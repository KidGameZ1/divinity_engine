package net.nightshade.divinity_engine.util.divinity.effects;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.effects.PrivateBlessingEffectsPacket;
import net.nightshade.renderlib.api.*;
import net.nightshade.renderlib.particle.ParticleBloomMode;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;
import net.nightshade.renderlib.particle.ParticleRegistries;
import net.nightshade.renderlib.particle.RenderParticles;

import java.util.*;

/** Server-owned, cosmetic-only manifestations. Core owns transport, visibility and expiration. */
@Mod.EventBusSubscriber(modid = DivinityEngineMod.MODID)
public final class BlessingVisuals {
    public static final ResourceLocation TYPE = new ResourceLocation(DivinityEngineMod.MODID, "blessing_manifestation");
    public static final ResourceLocation PIXEL_MATERIAL = new ResourceLocation(DivinityEngineMod.MODID, "particle_alpha");
    public enum Cue { CAST, IMPACT, WARD, HEAL, TRAIL, AURA, CONTROL, TELEPORT, MARK, RECALL }
    public record VisualRecipe(RenderEffectSpec visual, ParticleEmitterSpec particles,
                               boolean renderMesh, boolean emitParticles) {
        public VisualRecipe(RenderEffectSpec visual, ParticleEmitterSpec particles) {
            this(visual, particles, true, true);
        }
        public int handleCount() { return (renderMesh ? 1 : 0) + (emitParticles ? 1 : 0); }
    }
    private record Active(RenderHandle handle, long expires, boolean concealWhenInvisible,
                          boolean detachedTeleport, UUID privateRecipient) { }
    private static final class OwnerEffects {
        final BlessingEffectBudget budget = new BlessingEffectBudget();
        final List<Active> active = new ArrayList<>();
        long lastUsed;
    }
    private static final Map<ServerLevel, Map<UUID, OwnerEffects>> ACTIVE = new WeakHashMap<>();
    private static boolean registered;
    private static long nextWarning;

    private BlessingVisuals() { }

    public static void register() {
        if (registered) return;
        RenderLibRegistries.registerEffect(TYPE, () -> MapCodec.unit("blessing_manifestation"));
        RenderLibRegistries.registerPrimitive(TYPE, () -> MapCodec.unit("blessing_manifestation"));
        RenderLibRegistries.registerMaterial(PIXEL_MATERIAL, () -> MapCodec.unit("blessing_pixels"));
        ParticleRegistries.registerMaterial(PIXEL_MATERIAL, () -> MapCodec.unit("blessing_pixels"));
        registered = true;
    }

    public static void pulse(BlessingsInstance instance, LivingEntity owner, Cue cue) {
        if (owner == null) return;
        Vec3 offset = new Vec3(0, Math.min(0.8, owner.getBbHeight() * 0.4), 0);
        boolean follows = cue == Cue.AURA || cue == Cue.WARD || cue == Cue.HEAL;
        emit(instance, owner, follows ? RenderAnchor.entity(owner.getUUID(), offset)
                : RenderAnchor.world(owner.position().add(offset)), owner.position(), cue, null,
                cue == Cue.CAST || cue == Cue.CONTROL ? 1.5 : 0.9);
    }

    public static void at(BlessingsInstance instance, LivingEntity owner, Vec3 position, Cue cue, double radius) {
        if (!finite(position)) return;
        emit(instance, owner, RenderAnchor.world(position), position, cue, null, radius);
    }

    /** Local feedback aligned to applied movement or contact, without inventing a source/destination link. */
    public static void at(BlessingsInstance instance, LivingEntity owner, Vec3 position, Cue cue,
                          double radius, Vec3 direction) {
        if (!finite(position) || !finite(direction)) return;
        emit(instance, owner, RenderAnchor.world(position), position, cue, null, radius, false, direction);
    }

    public static void link(BlessingsInstance instance, LivingEntity owner, Vec3 from, Vec3 to, Cue cue) {
        if (!finite(from) || !finite(to)) return;
        Vec3 delta = to.subtract(from);
        delta = boundedLink(delta);
        Vec3 midpoint = from.add(delta.scale(0.5));
        emit(instance, owner, RenderAnchor.world(midpoint), midpoint, cue, delta, 0.7);
    }

    /** Private scouting information never enters Core's dimension-wide saved/snapshot state. */
    public static void privateMarks(BlessingsInstance instance, LivingEntity owner, List<? extends LivingEntity> targets) {
        if (!(owner instanceof ServerPlayer player) || !player.serverLevel().getServer().isSameThread()
                || owner.tickCount % 20 != 0 || targets == null || targets.isEmpty()) return;
        long second = owner.level().getGameTime() / 20;
        int windows = (targets.size() - 1) / 256 + 1;
        int offset = (int) Math.floorMod(second, (long) windows) * 256;
        // Rotate bounded windows; advance within each window on its next visit.
        List<? extends LivingEntity> valid = targets.subList(offset, offset + Math.min(256, targets.size() - offset)).stream()
                .filter(target -> target != null && target != owner && target.level() == owner.level()
                        && target.isAlive() && !target.isRemoved())
                .distinct().sorted(Comparator.comparing(LivingEntity::getUUID)).toList();
        if (valid.isEmpty()) return;
        int start = (int) Math.floorMod(Math.floorDiv(second, windows) * 4, (long) valid.size());
        for (int i = 0; i < Math.min(4, valid.size()); i++) {
            LivingEntity target = valid.get((start + i) % valid.size());
            Vec3 position = target.getBoundingBox().getCenter();
            emit(instance, owner, RenderAnchor.world(position), position, Cue.MARK, null,
                    Math.max(0.5, target.getBbWidth()), true);
        }
    }

    private static void emit(BlessingsInstance instance, LivingEntity owner, RenderAnchor anchor, Vec3 position,
                             Cue cue, Vec3 link, double radius) {
        emit(instance, owner, anchor, position, cue, link, radius, false);
    }

    private static void emit(BlessingsInstance instance, LivingEntity owner, RenderAnchor anchor, Vec3 position,
                             Cue cue, Vec3 link, double radius, boolean personal) {
        emit(instance, owner, anchor, position, cue, link, radius, personal,
                owner == null ? Vec3.ZERO : owner.getLookAngle());
    }

    private static void emit(BlessingsInstance instance, LivingEntity owner, RenderAnchor anchor, Vec3 position,
                             Cue cue, Vec3 link, double radius, boolean personal, Vec3 direction) {
        if (instance == null || owner == null || owner.isRemoved() || cue == null || !Double.isFinite(radius)
                || !(owner.level() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || instance.getBoundGodInstance() == null || !level.hasChunkAt(BlockPos.containing(position))) return;
        if (owner.isInvisible() && (cue == Cue.AURA || cue == Cue.TRAIL)) {
            Map<UUID, OwnerEffects> existing = ACTIVE.get(level);
            OwnerEffects previous = existing == null ? null : existing.get(owner.getUUID());
            if (previous != null) previous.active.removeIf(active -> {
                if (active.concealWhenInvisible) remove(level, active);
                return active.concealWhenInvisible;
            });
            return;
        }
        ResourceLocation god = instance.getBoundGodInstance().getBaseGodId();
        ResourceLocation blessing = instance.getBlessingId();
        BlessingVisualProfiles.Style style = BlessingVisualProfiles.style(blessing);
        if (style != null && !style.god().equals(god)) return;
        BlessingVisualDesign design = BlessingVisualDesigns.get(blessing);
        if (design == null || personal && !design.particles()) return;
        Map<UUID, OwnerEffects> owners = ACTIVE.computeIfAbsent(level, ignored -> new HashMap<>());
        if (!owners.containsKey(owner.getUUID()) && owners.size() >= 256) return;
        OwnerEffects effects = owners.computeIfAbsent(owner.getUUID(), ignored -> new OwnerEffects());
        long now = level.getGameTime();
        int interval = switch (cue) {
            case AURA -> 30;
            case TRAIL, WARD -> 8;
            case IMPACT, TELEPORT -> 4;
            case MARK, RECALL -> 20;
            default -> 10;
        };
        int burst = cue == Cue.TELEPORT || personal && cue == Cue.MARK ? 4 : 1;
        if (!effects.budget.acquire(blessing + "/" + cue + (personal ? "/private" : ""), now, interval, burst)) return;
        effects.lastUsed = now;
        effects.active.removeIf(active -> active.expires <= now);
        // Bound our custom mesh work as well as Core's independent particle/effect budgets.
        int needed = (design.mesh() ? 1 : 0) + (design.particles() ? 1 : 0);
        if (owners.values().stream().mapToInt(value -> value.active.size()).sum() + needed > 128) return;
        RenderHandle visual = null;
        RenderHandle particles = null;
        try {
            VisualRecipe recipe = recipe(god, blessing, cue, anchor, direction, link, radius);
            if (personal) {
                if (!(owner instanceof ServerPlayer player)) return;
                RenderEffectInstance privateParticles = new RenderEffectInstance(UUID.randomUUID(), 0, recipe.particles().toEffectSpec(), now,
                        now + recipe.particles().durationTicks());
                List<RenderEffectInstance> privateEffects = new ArrayList<>();
                if (recipe.renderMesh()) {
                    RenderEffectInstance mesh = new RenderEffectInstance(UUID.randomUUID(), 0, recipe.visual(), now,
                            now + recipe.visual().durationTicks());
                    privateEffects.add(mesh);
                    ModMessages.sendToPlayer(PrivateBlessingEffectsPacket.spawn(level.dimension().location(), mesh, privateParticles), player);
                } else {
                    ModMessages.sendToPlayer(PrivateBlessingEffectsPacket.spawn(level.dimension().location(), privateParticles), player);
                }
                privateEffects.add(privateParticles);
                for (RenderEffectInstance effect : privateEffects) {
                    effects.active.add(new Active(new RenderHandle(effect.id(), level.dimension(), true),
                            effect.endGameTime(), false, false, owner.getUUID()));
                }
                return;
            }
            if (recipe.renderMesh()) visual = RenderEffects.spawn(level, recipe.visual());
            if (recipe.emitParticles()) particles = RenderParticles.spawn(level, recipe.particles());
            boolean conceal = cue == Cue.AURA || cue == Cue.TRAIL;
            boolean detached = cue == Cue.TELEPORT && anchor.kind() == RenderAnchor.Kind.WORLD;
            if (visual != null) effects.active.add(new Active(visual, now + recipe.visual().durationTicks(), conceal, detached, null));
            if (particles != null) effects.active.add(new Active(particles, now + recipe.particles().durationTicks(), conceal, detached, null));
        } catch (RuntimeException unavailable) {
            // A saturated cosmetic budget must never prevent a blessing from doing its gameplay work.
            if (visual != null) RenderEffects.remove(level, visual);
            if (particles != null) RenderEffects.remove(level, particles);
            if (now >= nextWarning) {
                nextWarning = now + 200;
                DivinityEngineMod.LOGGER.warn("Skipped blessing visuals for {}: {}", blessing, unavailable.toString());
            }
        }
    }

    /** Same bounded recipe used by synced gameplay and local previews; never changes gameplay state. */
    public static VisualRecipe recipe(ResourceLocation god, ResourceLocation blessing, Cue cue, RenderAnchor anchor,
                                      Vec3 forward, Vec3 link, double radius) {
        if (god == null || blessing == null || cue == null || anchor == null)
            throw new IllegalArgumentException("Blessing visual god, blessing, cue and anchor are required");
        if (!Double.isFinite(radius) || !finite(forward) || (link != null && !finite(link)))
            throw new IllegalArgumentException("Blessing visual coordinates and radius must be finite");
        BlessingVisualProfiles.Style style = BlessingVisualProfiles.style(blessing);
        if (style != null && !style.god().equals(god))
            throw new IllegalArgumentException("Blessing " + blessing + " is not bound to " + god);
        BlessingVisualProfiles.Theme theme = BlessingVisualProfiles.theme(god);
        BlessingVisualDesign design = BlessingVisualDesigns.get(blessing);
        if (design == null) throw new IllegalArgumentException("No visual design registered for " + blessing);
        int variant = style == null ? Math.floorMod(blessing.hashCode(), 5) : style.variant();
        double shapeRadius = Math.max(0.25, Math.min(12, radius));
        if (link != null) link = boundedLink(link);
        forward = unit(forward);
        boolean quiet = cue == Cue.AURA || cue == Cue.TRAIL || cue == Cue.MARK;
        int duration = design.duration();
        int count = design.particleCount();
        CompoundTag parameters = new CompoundTag();
        parameters.putString("God", theme.motif());
        parameters.putString("GodId", god.toString());
        parameters.putString("BlessingId", blessing.toString());
        parameters.putString("Cue", cue.name());
        parameters.putInt("Variant", variant);
        parameters.putDouble("ShapeRadius", shapeRadius);
        parameters.putDouble("Radius", Math.max(shapeRadius * 1.8, link == null ? 0 : link.length() * 0.5 + 1));
        parameters.putInt("Accent", theme.accent());
        parameters.putInt("Highlight", theme.highlight());
        parameters.putDouble("DirectionX", forward.x);
        parameters.putDouble("DirectionY", forward.y);
        parameters.putDouble("DirectionZ", forward.z);
        if (link != null) {
            parameters.putDouble("LinkX", link.x);
            parameters.putDouble("LinkY", link.y);
            parameters.putDouble("LinkZ", link.z);
        }
        RenderPalette palette = new RenderPalette(List.of(theme.primary(), theme.accent(), theme.highlight()), 40);
        RenderVisibility visibility = new RenderVisibility(48, false, false, true);
        RenderEffectSpec visual = RenderEffectSpec.builder(TYPE, TYPE).anchor(anchor).palette(palette)
                .visibility(visibility).duration(duration).priority(quiet ? -8 : 0)
                .seed(blessing.hashCode() * 31L + cue.ordinal()).parameters(parameters).build();
        var particles = ParticleEmitterSpec.builder().anchor(anchor).palette(palette).visibility(visibility)
                .duration(duration).priority(-10).seed(blessing.hashCode() * 31L + cue.ordinal())
                .texture(theme.texture()).renderer(new ResourceLocation("nightshade_core", "textured_billboard"))
                .point(0, duration - 2)
                .size(design.particleSize(), Math.max(0.025, design.particleSize() * 0.55))
                .alpha(quiet ? 0.72 : 0.92, 0)
                .glow(0, ParticleBloomMode.OFF).material(PIXEL_MATERIAL);
        if (link != null) {
            // A design owns its trajectory; particle coordinates start at the supplied source.
            RenderAnchor particleAnchor = anchor.kind() == RenderAnchor.Kind.WORLD
                    ? RenderAnchor.world(anchor.position().subtract(link.scale(0.5))) : anchor;
            particles.anchor(particleAnchor);
        }
        if (design.particles()) design.particleAuthor().configure(particles,
                new BlessingVisualDesign.Context(cue, shapeRadius, forward, link, duration - 2));
        particles.burst(count, duration - 2).maximumAlive(count);
        ParticleEmitterSpec emitter = particles.build();
        var motion = emitter.motion();
        // NBT canonicalizes -0.0; match it before syncing so equivalent motion has stable identity.
        emitter = emitter.toBuilder().motion(new net.nightshade.renderlib.particle.ParticleMotion(
                canonical(motion.velocity()), canonical(motion.velocitySpread()), canonical(motion.acceleration()),
                canonical(motion.gravity()), canonical(motion.drag()), canonical(motion.inheritedAnchorVelocity()),
                canonical(motion.angularVelocity()))).build();
        return new VisualRecipe(visual, emitter, design.mesh(), design.particles());
    }

    private static double canonical(double value) { return value == 0 ? 0 : value; }

    private static Vec3 canonical(Vec3 value) {
        return new Vec3(canonical(value.x), canonical(value.y), canonical(value.z));
    }

    private static boolean finite(Vec3 value) {
        return value != null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private static Vec3 unit(Vec3 value) {
        double largest = Math.max(Math.abs(value.x), Math.max(Math.abs(value.y), Math.abs(value.z)));
        if (largest < 1.0E-9) return new Vec3(0, 0, 1);
        Vec3 scaled = value.scale(1 / largest);
        return scaled.scale(1 / scaled.length());
    }

    private static Vec3 boundedLink(Vec3 value) {
        double largest = Math.max(Math.abs(value.x), Math.max(Math.abs(value.y), Math.abs(value.z)));
        return largest > 48 || value.lengthSqr() > 48 * 48 ? unit(value).scale(48) : value;
    }

    private static void remove(ServerLevel level, Active active) {
        if (active.privateRecipient == null) {
            RenderEffects.remove(level, active.handle);
        } else {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(active.privateRecipient);
            if (player != null) ModMessages.sendToPlayer(
                    PrivateBlessingEffectsPacket.remove(level.dimension().location(), active.handle.id()), player);
        }
    }

    private static void clear(LivingEntity owner, boolean dimensionChange) {
        if (!(owner.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) return;
        for (var entry : ACTIVE.entrySet()) {
            OwnerEffects effects = entry.getValue().get(owner.getUUID());
            if (effects == null) continue;
            effects.active.removeIf(active -> {
                if (dimensionChange && active.detachedTeleport) return false;
                remove(entry.getKey(), active);
                return true;
            });
            if (effects.active.isEmpty()) entry.getValue().remove(owner.getUUID());
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity(), false); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { clear(event.getEntity(), true); }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel) clear(event.getEntity(), false);
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ACTIVE.remove(level);
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        Map<UUID, OwnerEffects> owners = ACTIVE.get(level);
        if (owners == null) return;
        long now = level.getGameTime();
        boolean prune = now % 20 == 0;
        owners.entrySet().removeIf(entry -> {
            OwnerEffects effects = entry.getValue();
            var owner = effects.active.isEmpty() ? null : level.getEntity(entry.getKey());
            boolean invisible = owner != null && owner.isInvisible();
            // Conceal existing effects even when their blessing no longer emits a cue.
            effects.active.removeIf(active -> {
                if (invisible && active.concealWhenInvisible) {
                    remove(level, active);
                    return true;
                }
                return prune && active.expires <= now;
            });
            return prune && effects.active.isEmpty() && now - effects.lastUsed > 200;
        });
    }
}
