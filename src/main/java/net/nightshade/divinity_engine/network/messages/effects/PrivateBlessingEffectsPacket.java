package net.nightshade.divinity_engine.network.messages.effects;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.client.effects.PrivateBlessingEffectsClient;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.renderlib.api.RenderAnchor;
import net.nightshade.renderlib.api.RenderEffectInstance;
import net.nightshade.renderlib.api.RenderEffectSpec;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;
import net.nightshade.renderlib.particle.ParticleRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Owner-only transient visuals; never inserted into Core's public snapshot state. */
public record PrivateBlessingEffectsPacket(ResourceLocation dimension,
                                          List<RenderEffectInstance> effects, List<UUID> removals) {
    public static final int MAX_LIFETIME = 32;
    public static final int MAX_SPEC_BYTES = 16_384;
    private static final ResourceLocation PARTICLES = new ResourceLocation("nightshade_core", "particle_emitter");

    public PrivateBlessingEffectsPacket {
        Objects.requireNonNull(dimension);
        effects = List.copyOf(effects);
        removals = List.copyOf(removals);
        if (effects.size() == 1 && removals.isEmpty()) {
            validateParticles(effects.get(0));
        } else if (effects.size() == 2 && removals.isEmpty()) {
            validatePair(effects.get(0), effects.get(1));
        } else if (!effects.isEmpty() || removals.isEmpty() || removals.size() > 2) {
            throw new IllegalArgumentException("Private visuals require one emitter, a legacy pair, or one/two removals");
        }
    }

    public static PrivateBlessingEffectsPacket spawn(ResourceLocation dimension,
                                                     RenderEffectInstance mesh, RenderEffectInstance particles) {
        return new PrivateBlessingEffectsPacket(dimension, List.of(mesh, particles), List.of());
    }

    public static PrivateBlessingEffectsPacket spawn(ResourceLocation dimension, RenderEffectInstance particles) {
        return new PrivateBlessingEffectsPacket(dimension, List.of(particles), List.of());
    }

    public static PrivateBlessingEffectsPacket remove(ResourceLocation dimension, UUID id) {
        return new PrivateBlessingEffectsPacket(dimension, List.of(), List.of(id));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeByte(effects.size());
        for (RenderEffectInstance effect : effects) {
            buffer.writeUUID(effect.id());
            buffer.writeLong(effect.startGameTime());
            buffer.writeLong(effect.endGameTime());
            FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer(256, MAX_SPEC_BYTES));
            try {
                var tag = RenderEffectSpec.CODEC.encodeStart(NbtOps.INSTANCE, effect.spec()).result()
                        .filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid private effect specification"));
                encoded.writeNbt(tag);
                byte[] bytes = new byte[encoded.readableBytes()];
                encoded.readBytes(bytes);
                buffer.writeByteArray(bytes);
            } finally {
                encoded.release();
            }
        }
        buffer.writeByte(removals.size());
        removals.forEach(buffer::writeUUID);
    }

    public static PrivateBlessingEffectsPacket decode(FriendlyByteBuf buffer) {
        try {
            ResourceLocation dimension = buffer.readResourceLocation();
            int count = buffer.readUnsignedByte();
            if (count > 2) throw new DecoderException("Private effect count must be zero, one or two");
            List<RenderEffectInstance> effects = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                UUID id = buffer.readUUID();
                long start = buffer.readLong(), end = buffer.readLong();
                FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer.readByteArray(MAX_SPEC_BYTES)));
                try {
                    CompoundTag tag = encoded.readNbt();
                    if (tag == null || encoded.isReadable()) throw new DecoderException("Invalid private effect NBT");
                    RenderEffectSpec spec = RenderEffectSpec.CODEC.parse(NbtOps.INSTANCE, tag).result()
                            .orElseThrow(() -> new DecoderException("Invalid private effect codec"));
                    effects.add(new RenderEffectInstance(id, 0, spec, start, end));
                } finally {
                    encoded.release();
                }
            }
            int removed = buffer.readUnsignedByte();
            if (removed > 2) throw new DecoderException("Too many private effect removals");
            List<UUID> removals = new ArrayList<>(removed);
            for (int i = 0; i < removed; i++) removals.add(buffer.readUUID());
            if (buffer.isReadable()) throw new DecoderException("Trailing private effect data");
            return new PrivateBlessingEffectsPacket(dimension, effects, removals);
        } catch (IllegalArgumentException | IndexOutOfBoundsException failure) {
            throw new DecoderException("Invalid private blessing effects", failure);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> PrivateBlessingEffectsClient.apply(this)));
        }
        context.setPacketHandled(true);
    }

    private static void validatePair(RenderEffectInstance mesh, RenderEffectInstance particles) {
        validateEffect(mesh);
        validateParticles(particles);
        if (mesh.id().equals(particles.id()) || !mesh.spec().type().equals(BlessingVisuals.TYPE)
                || !mesh.spec().primitive().equals(BlessingVisuals.TYPE)
                || !"MARK".equals(mesh.spec().parameters().getString("Cue"))
                || !particles.spec().type().equals(PARTICLES) || !particles.spec().primitive().equals(PARTICLES)
                || !mesh.spec().anchor().equals(particles.spec().anchor())
                || mesh.startGameTime() != particles.startGameTime() || mesh.endGameTime() != particles.endGameTime()) {
            throw new IllegalArgumentException("Private visuals must be a matching MARK mesh/particle pair");
        }
    }

    private static void validateParticles(RenderEffectInstance particles) {
        validateEffect(particles);
        if (!particles.spec().type().equals(PARTICLES) || !particles.spec().primitive().equals(PARTICLES)) {
            throw new IllegalArgumentException("Private single effects must be particle emitters");
        }
        ParticleEmitterSpec emitter = ParticleEmitterSpec.fromEffect(particles.spec())
                .orElseThrow(() -> new IllegalArgumentException("Missing private particle specification"));
        if (!emitter.anchor().equals(particles.spec().anchor())
                || emitter.durationTicks() != particles.spec().durationTicks()
                || emitter.collision().authoritative() || emitter.emission().maximumAlive() > 16
                || emitter.emission().ratePerTick() > 16 || emitter.emission().maximumLifetime() > MAX_LIFETIME) {
            throw new IllegalArgumentException("Private particles exceed cosmetic bounds");
        }
        ParticleRegistries.validate(emitter);
    }

    private static void validateEffect(RenderEffectInstance effect) {
        long start = effect.startGameTime(), end = effect.endGameTime();
        RenderEffectSpec spec = effect.spec();
        if (effect.revision() != 0 || start < 0 || start > Long.MAX_VALUE - MAX_LIFETIME
                || end <= start || end - start > MAX_LIFETIME || spec.durationTicks() != end - start
                || spec.anchor().kind() != RenderAnchor.Kind.WORLD
                || spec.anchor().entityId().isPresent() || spec.anchor().blockPosition().isPresent()
                || !spec.anchor().offset().equals(Vec3.ZERO) || !worldPosition(spec.anchor().position())
                || !Double.isFinite(spec.visibility().maximumDistance()) || spec.visibility().maximumDistance() > 64) {
            throw new IllegalArgumentException("Invalid private effect lifetime or location");
        }
    }

    private static boolean worldPosition(Vec3 position) {
        return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z)
                && Math.abs(position.x) <= 30_000_000 && Math.abs(position.z) <= 30_000_000
                && Math.abs(position.y) <= 4096;
    }
}
