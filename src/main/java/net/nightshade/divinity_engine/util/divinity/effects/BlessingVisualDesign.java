package net.nightshade.divinity_engine.util.divinity.effects;

import net.minecraft.world.phys.Vec3;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;

import java.util.Objects;

/** A blessing's authored presentation, independent of its god's color palette. */
public record BlessingVisualDesign(boolean mesh, int duration, int particleCount,
                                   double particleSize, ParticleAuthor particleAuthor) {
    public record Context(BlessingVisuals.Cue cue, double radius, Vec3 forward, Vec3 link, int lifetime) { }
    @FunctionalInterface
    public interface ParticleAuthor {
        void configure(ParticleEmitterSpec.Builder particles, Context context);
    }

    public BlessingVisualDesign {
        if (!mesh && particleAuthor == null) throw new IllegalArgumentException("A visual design needs a mesh or particles");
        if (duration < 8 || duration > 32) throw new IllegalArgumentException("Visual duration must be 8..32 ticks");
        if (particleCount < 1 || particleCount > 16) throw new IllegalArgumentException("Particle count must be 1..16");
        if (!Double.isFinite(particleSize) || particleSize < 0.025 || particleSize > 0.3)
            throw new IllegalArgumentException("Particle size must be 0.025..0.3");
    }

    public boolean particles() { return particleAuthor != null; }
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private boolean mesh;
        private int duration = 24;
        private int count = 6;
        private double size = 0.1;
        private ParticleAuthor particles;

        public Builder mesh() { mesh = true; return this; }
        public Builder duration(int ticks) { duration = ticks; return this; }
        public Builder particles(int count, double size, ParticleAuthor author) {
            this.count = count;
            this.size = size;
            particles = Objects.requireNonNull(author);
            return this;
        }
        public BlessingVisualDesign build() {
            return new BlessingVisualDesign(mesh, duration, count, size, particles);
        }
    }
}
