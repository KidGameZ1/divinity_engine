package net.nightshade.divinity_engine.util.divinity.narrative;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.api.narrative.GodPresenceDefinition;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.renderlib.api.RenderAnchor;
import net.nightshade.renderlib.api.RenderPalette;
import net.nightshade.renderlib.api.RenderVisibility;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;
import net.nightshade.renderlib.particle.ParticleBloomMode;
import net.nightshade.renderlib.particle.RenderParticles;
import java.util.*;

/** Brief vanilla-sprite responses, with one six-particle burst per shrine per five seconds. */
public final class CorePresenceEffects {
    private static final Map<ServerLevel, Map<BlockPos, Long>> ACTIVE = new WeakHashMap<>();
    public static void pulse(ServerLevel level, BlockPos pos, GodPresenceDefinition presence) {
        if (!level.getServer().isSameThread() || !level.hasChunkAt(pos)) return;
        long now = level.getGameTime();
        Map<BlockPos, Long> recent = ACTIVE.computeIfAbsent(level, ignored -> new HashMap<>());
        recent.values().removeIf(expiry -> expiry <= now);
        if (recent.containsKey(pos) || recent.size() >= 64) return;
        recent.put(pos.immutable(), now + 100);
        RenderParticles.spawn(level, recipe(pos, presence));
    }

    public static ParticleEmitterSpec recipe(BlockPos pos, GodPresenceDefinition presence) {
        return ParticleEmitterSpec.builder()
            .anchor(RenderAnchor.block(pos)).palette(new RenderPalette(List.of(0xFF000000 | presence.color()), 40))
            .visibility(new RenderVisibility(24, false, false, true)).duration(32).priority(-10)
            .sphere(0.45, 0, 30).burst(6, 30).maximumAlive(6).texture(presence.texture())
            .size(0.11, 0.06).alpha(0.85, 0)
            .glow(0, ParticleBloomMode.OFF).material(BlessingVisuals.PIXEL_MATERIAL)
            .velocity(new Vec3(0, 0.018, 0), new Vec3(0.006, 0.003, 0.006)).build();
    }
}
