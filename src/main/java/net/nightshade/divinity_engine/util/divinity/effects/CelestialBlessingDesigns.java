package net.nightshade.divinity_engine.util.divinity.effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.nightshade.renderlib.api.RenderPalette;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;
import net.nightshade.renderlib.particle.ParticleMotion;

import java.util.List;

/** Vanilla particle sprites through Nightshade; no built-in celestial trigger requests a mesh. */
public final class CelestialBlessingDesigns {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private CelestialBlessingDesigns() { }

    public static void register() {
        BlessingVisualDesigns.register(id("radiant_strike"), BlessingVisualDesign.builder().duration(12)
                .particles(6, 0.10, (p, c) -> {
                    if (c.cue() == BlessingVisuals.Cue.AURA) {
                        p.cylinder(0.18, 0.25, 0, c.lifetime());
                        motion(p, new Vec3(0, 0.045, 0), 0.008, 0, 0.94);
                        nativeTexture(p, "flame");
                    } else {
                        // IMPACT is the undead-only bonus damage; ordinary ignition is AURA.
                        line(p, c, 0.42, right(c).add(0, -1, 0));
                        motion(p, c.forward().scale(0.045).add(0, 0.012, 0), 0.015, 0.003, 0.88);
                        p.texture(texture("critical_hit"));
                    }
                }).build());
        BlessingVisualDesigns.register(id("sunskin"), BlessingVisualDesign.builder().duration(10)
                .particles(2, 0.045, (p, c) -> {
                    box(p, c, 0.16, 0.18, 0.12);
                    motion(p, new Vec3(0, 0.015, 0), 0.002, 0, 0.92);
                    nativeTexture(p, "heart");
                    p.alpha(0.55, 0);
                }).build());
        BlessingVisualDesigns.register(id("sunburst"), BlessingVisualDesign.builder().duration(16)
                .particles(12, 0.12, (p, c) -> {
                    if (c.cue() == BlessingVisuals.Cue.CAST) {
                        box(p, c, c.radius() * 0.72, 0.045, c.radius() * 0.72);
                        motion(p, new Vec3(0, 0.055, 0), 0.045, 0.002, 0.88);
                        nativeTexture(p, "flame");
                    } else if (c.cue() == BlessingVisuals.Cue.CONTROL) {
                        line(p, c, 0.28, right(c));
                        motion(p, Vec3.ZERO, 0, 0, 1);
                        p.texture(texture("glint"));
                    } else {
                        line(p, c, 0.18, direction(c));
                        motion(p, direction(c).scale(0.08), 0.012, 0.003, 0.9);
                        nativeTexture(p, "flame");
                    }
                    p.alpha(0.65, 0);
                }).build());
        BlessingVisualDesigns.register(id("solar_ward"), BlessingVisualDesign.builder().duration(18)
                .particles(5, 0.09, (p, c) -> {
                    // Absorption covers the body; this is not a directional projectile shield.
                    p.cylinder(0.32, 0.6, 0, c.lifetime());
                    motion(p, new Vec3(0, 0.012, 0), 0.003, 0, 0.94);
                    p.texture(texture("enchanted_hit")).alpha(0.6, 0);
                }).build());
        BlessingVisualDesigns.register(id("corona_ascendant"), BlessingVisualDesign.builder().duration(10)
                .particles(5, 0.08, (p, c) -> {
                    if (c.cue() == BlessingVisuals.Cue.IMPACT) {
                        line(p, c, 0.2, right(c));
                        motion(p, Vec3.ZERO, 0.003, 0, 0.9);
                        p.texture(texture("glint")).alpha(0.5, 0);
                    } else {
                        p.cylinder(0.23, 0.03, 0, c.lifetime());
                        motion(p, new Vec3(0, 0.055, 0), 0.008, 0, 0.94);
                        nativeTexture(p, "flame");
                    }
                }).build());

        BlessingVisualDesigns.register(id("toxic_skin"), BlessingVisualDesign.builder().duration(12)
                .particles(5, 0.07, (p, c) -> {
                    p.cylinder(0.25, 0.45, 0, c.lifetime());
                    motion(p, new Vec3(0, -0.025, 0), 0.009, 0.004, 0.94);
                    p.texture(texture("drip_fall"));
                }).build());
        BlessingVisualDesigns.register(id("serpents_bite"), BlessingVisualDesign.builder().duration(12)
                .particles(4, 0.07, (p, c) -> {
                    line(p, c, 0.24, right(c));
                    motion(p, new Vec3(0, -0.05, 0), 0.002, 0.004, 0.95);
                    p.texture(texture("drip_hang")).size(0.07, 0.04);
                }).build());
        BlessingVisualDesigns.register(id("shadowstep"), BlessingVisualDesign.builder().duration(10)
                .particles(5, 0.09, (p, c) -> {
                    if (c.cue() == BlessingVisuals.Cue.TELEPORT) {
                        // Departure only: do not manufacture an arrival or reveal the invisible escape.
                        box(p, c, 0.22, 0.025, 0.22);
                        motion(p, new Vec3(0, 0.018, 0), 0.008, 0, 0.86);
                        p.texture(texture("generic_2")).size(0.09, 0.14).alpha(0.32, 0);
                    } else {
                        box(p, c, 0.13, 0.10, 0.13);
                        motion(p, new Vec3(0, 0.028, 0), 0.03, 0.006, 0.93);
                        p.texture(texture("drip_land"));
                    }
                }).build());
        BlessingVisualDesigns.register(id("venomblood"), BlessingVisualDesign.builder().duration(12)
                .particles(7, 0.075, (p, c) -> {
                    p.cone(0.16, 0.75, 0, c.lifetime());
                    motion(p, new Vec3(0, 0.065, 0), 0.04, 0.012, 0.96);
                    p.texture(texture("drip_fall")).size(0.075, 0.045);
                }).build());
        BlessingVisualDesigns.register(id("coiled_retribution"), BlessingVisualDesign.builder().duration(14)
                .particles(5, 0.065, (p, c) -> {
                    if (c.cue() == BlessingVisuals.Cue.WARD) {
                        p.cylinder(0.32, 0.1, 0, c.lifetime());
                        motion(p, new Vec3(0, -0.014, 0), 0.004, 0, 0.87);
                        p.texture(texture("critical_hit"));
                    } else {
                        line(p, c, 0.3, UP);
                        motion(p, new Vec3(0, -0.03, 0), 0.01, 0.003, 0.92);
                        p.texture(texture("drip_fall"));
                    }
                }).build());

        BlessingVisualDesigns.register(id("insightful_kill"), BlessingVisualDesign.builder().duration(16)
                .particles(3, 0.09, (p, c) -> {
                    box(p, c, 0.14, 0.035, 0.14);
                    motion(p, new Vec3(0, 0.025, 0), 0.005, 0, 0.96);
                    p.texture(texture("sga_e")).alpha(0.6, 0);
                }).build());
        BlessingVisualDesigns.register(id("foresight"), BlessingVisualDesign.builder().duration(10)
                .particles(4, 0.08, (p, c) -> {
                    if (c.cue() == BlessingVisuals.Cue.CONTROL) {
                        facingBox(p, c, 0.18, 0.18, 0.015);
                    } else {
                        line(p, c, 0.38, right(c));
                    }
                    // Foresight cancels damage/stops a projectile; it does not move the owner or reflect it.
                    motion(p, Vec3.ZERO, 0, 0, 1);
                    p.texture(texture("enchanted_hit")).alpha(0.5, 0);
                }).build());
        BlessingVisualDesigns.register(id("scholars_edge"), BlessingVisualDesign.builder().duration(8)
                .particles(3, 0.065, (p, c) -> {
                    line(p, c, 0.35, right(c).add(0, -0.7, 0));
                    motion(p, c.forward().scale(0.025), 0, 0, 0.86);
                    p.texture(texture("enchanted_hit"));
                }).build());
        BlessingVisualDesigns.register(id("mind_mirror"), BlessingVisualDesign.builder().duration(18)
                .particles(5, 0.075, (p, c) -> {
                    if (c.link() != null && c.link().lengthSqr() > 0.0001) {
                        // Parent anchors linked particles at the source; transfer toward the struck recipient.
                        line(p, c, Math.min(0.12, c.link().length()), c.link());
                        motion(p, c.link().scale(1.0 / c.lifetime()), 0, 0, 1);
                    } else {
                        facingBox(p, c, 0.14, 0.20, 0.045);
                        motion(p, new Vec3(0, c.cue() == BlessingVisuals.Cue.WARD ? 0.025 : -0.025, 0), 0.006, 0, 0.94);
                    }
                    p.texture(texture("spell_4")).alpha(0.6, 0);
                }).build());
        BlessingVisualDesigns.register(id("arcane_recall"), BlessingVisualDesign.builder().duration(20)
                .particles(6, 0.085, (p, c) -> {
                    if (c.link() != null) {
                        line(p, c, Math.min(0.12, c.link().length()), direction(c));
                        motion(p, c.link().scale(1.0 / c.lifetime()), 0, 0, 1);
                    } else {
                        box(p, c, 0.18, 0.012, 0.24);
                        motion(p, Vec3.ZERO, 0, 0, 1);
                    }
                    p.texture(texture("generic_0")).size(0.085, 0.03).alpha(0.5, 0);
                }).build());

        BlessingVisualDesigns.register(id("twilight_veil"), BlessingVisualDesign.builder().duration(8)
                .particles(2, 0.045, (p, c) -> {
                    box(p, c, 0.13, 0.012, 0.09);
                    motion(p, horizontal(c).scale(-0.008), 0, 0, 0.91);
                    p.texture(texture("generic_3")).alpha(0.25, 0);
                }).build());
        BlessingVisualDesigns.register(id("lull_of_the_lost"), BlessingVisualDesign.builder().duration(18)
                .particles(3, 0.08, (p, c) -> {
                    box(p, c, 0.12, 0.045, 0.12);
                    motion(p, right(c).scale(0.008).add(0, 0.012, 0), 0.002, 0, 0.94);
                    p.texture(texture("generic_5")).size(0.08, 0.12).alpha(0.32, 0);
                }).build());
        BlessingVisualDesigns.register(id("phase_blink"), BlessingVisualDesign.builder().duration(10)
                .particles(8, 0.09, (p, c) -> {
                    if (c.link() != null) linkedLine(p, c);
                    else line(p, c, 0.6, UP);
                    motion(p, direction(c).scale(-0.025), 0.003, 0, 0.88);
                    p.texture(texture("generic_0")).alpha(0.55, 0);
                }).build());
        BlessingVisualDesigns.register(id("silver_mirror"), BlessingVisualDesign.builder().duration(12)
                .particles(5, 0.085, (p, c) -> {
                    boolean impact = c.cue() == BlessingVisuals.Cue.IMPACT;
                    if (impact) {
                        box(p, c, 0.14, 0.19, 0.14);
                        motion(p, Vec3.ZERO, 0.025, 0.004, 0.92);
                    } else if (c.cue() == BlessingVisuals.Cue.WARD) {
                        p.cylinder(0.23, 0.38, 0, c.lifetime());
                        motion(p, new Vec3(0, -0.018, 0), 0.003, 0, 0.92);
                    } else {
                        facingBox(p, c, 0.23, 0.3, 0.035);
                        motion(p, new Vec3(0, 0.012, 0), 0.003, 0, 0.92);
                    }
                    p.texture(texture("glint"));
                }).build());
        BlessingVisualDesigns.register(id("moondrinker"), BlessingVisualDesign.builder().duration(16)
                .particles(3, 0.055, (p, c) -> {
                    box(p, c, 0.09, 0.06, 0.09);
                    motion(p, new Vec3(0, -0.022, 0), 0.001, 0, 1);
                    nativeTexture(p, "heart");
                    p.size(0.055, 0.025).alpha(0.55, 0);
                }).build());

        BlessingVisualDesigns.register(id("second_chance"), BlessingVisualDesign.builder().duration(28)
                .particles(10, 0.10, (p, c) -> {
                    if (c.cue() == BlessingVisuals.Cue.WARD) {
                        p.cylinder(0.32, 0.6, 0, c.lifetime());
                        motion(p, new Vec3(0, 0.012, 0), 0.003, 0, 0.94);
                        p.texture(texture("enchanted_hit")).alpha(0.5, 0);
                    } else {
                        p.cylinder(0.28, 0.9, 0, c.lifetime());
                        motion(p, new Vec3(0, 0.025, 0), 0.018, 0, 0.93);
                        p.texture(texture("glitter_5")).alpha(0.65, 0);
                    }
                }).build());
        BlessingVisualDesigns.register(id("echo_step"), BlessingVisualDesign.builder().duration(20)
                .particles(6, 0.07, (p, c) -> {
                    if (c.link() != null) linkedLine(p, c);
                    else box(p, c, 0.18, 0.012, 0.28);
                    motion(p, Vec3.ZERO, 0, 0, 1);
                    p.texture(texture("generic_1")).size(0.07, 0.035).alpha(0.45, 0);
                }).build());
        BlessingVisualDesigns.register(id("predicted_path"), BlessingVisualDesign.builder().duration(8)
                .particles(3, 0.055, (p, c) -> {
                    line(p, c, 0.4, horizontal(c).scale(-1));
                    motion(p, horizontal(c).scale(-0.015).add(0, 0.002, 0), 0, 0, 0.9);
                    p.texture(texture("generic_0")).alpha(0.4, 0);
                }).build());
        BlessingVisualDesigns.register(id("threadsever"), BlessingVisualDesign.builder().duration(12)
                .particles(6, 0.065, (p, c) -> {
                    line(p, c, 0.5, right(c).add(0, -0.4, 0));
                    motion(p, new Vec3(0, -0.035, 0), 0.004, 0, 0.9);
                    p.texture(texture("spell_4"))
                            .palette(new RenderPalette(List.of(0xFF000000 | MobEffects.WEAKNESS.getColor()), 40));
                }).build());
        BlessingVisualDesigns.register(id("fateful_draw"), BlessingVisualDesign.builder().duration(20)
                .particles(5, 0.075, (p, c) -> {
                    if (c.cue() == BlessingVisuals.Cue.TRAIL) {
                        line(p, c, 0.34, horizontal(c).scale(-1));
                        motion(p, horizontal(c).scale(-0.03), 0.003, 0, 0.9);
                        p.texture(texture("generic_0"));
                    } else if (c.cue() == BlessingVisuals.Cue.WARD) {
                        p.cylinder(0.23, 0.38, 0, c.lifetime());
                        motion(p, new Vec3(0, -0.012, 0), 0.002, 0, 0.94);
                        p.texture(texture("enchanted_hit"));
                    } else {
                        box(p, c, 0.12, 0.08, 0.12);
                        motion(p, new Vec3(0, 0.028, 0), 0.018, 0.002, 0.9);
                        p.texture(texture("critical_hit"));
                    }
                }).build());

        // Remains owner-only MARK feedback; the parent owns the particle-only private packet contract.
        BlessingVisualDesigns.register(id("veilpiercer"), BlessingVisualDesign.builder().duration(24)
                .particles(3, 0.08, (p, c) -> {
                    box(p, c, c.radius() * 0.4, c.radius() * 0.3, 0.025);
                    motion(p, Vec3.ZERO, 0, 0, 1);
                    p.texture(texture("glitter_0")).alpha(0.65, 0);
                }).build());
        BlessingVisualDesigns.register(id("celestial_ward"), BlessingVisualDesign.builder().duration(14)
                .particles(5, 0.08, (p, c) -> {
                    if (c.cue() == BlessingVisuals.Cue.HEAL) {
                        box(p, c, 0.14, 0.18, 0.08);
                        motion(p, new Vec3(0, 0.012, 0), 0.002, 0, 0.93);
                        nativeTexture(p, "heart");
                        p.alpha(0.55, 0);
                    } else {
                        // Only the fragments recoil; the hook stops the projectile at this contact.
                        facingBox(p, c, 0.22, 0.22, 0.015);
                        motion(p, direction(c).scale(-0.035), 0.008, 0.001, 0.9);
                        p.texture(texture("enchanted_hit"));
                    }
                }).build());
        BlessingVisualDesigns.register(id("starlit_mirror"), BlessingVisualDesign.builder().duration(14)
                .particles(5, 0.075, (p, c) -> {
                    if (c.link() != null) {
                        line(p, c, Math.min(0.16, c.link().length()), direction(c));
                        motion(p, direction(c).scale(0.055), 0.003, 0, 0.95);
                    } else {
                        facingBox(p, c, 0.14, 0.26, 0.035);
                        motion(p, new Vec3(0, -0.012, 0), 0.002, 0, 0.92);
                    }
                    p.texture(texture("glint"));
                }).build());
        BlessingVisualDesigns.register(id("gravity_fold"), BlessingVisualDesign.builder().duration(8)
                .particles(6, 0.065, (p, c) -> {
                    p.cylinder(0.25, 0.012, 0, c.lifetime());
                    motion(p, Vec3.ZERO, 0, 0, 1);
                    p.texture(texture("generic_2")).alpha(0.4, 0);
                }).build());
        // Only the short blessing cue; persistent MARK gateways are authored separately by the parent.
        BlessingVisualDesigns.register(id("pocket_dimension"), BlessingVisualDesign.builder().duration(24)
                .particles(6, 0.065, (p, c) -> {
                    // Current hooks only emit TELEPORT at each endpoint, not a menu CAST or a cross-world link.
                    line(p, c, 1.4, UP);
                    motion(p, new Vec3(0, 0.012, 0), 0.003, 0, 0.96);
                    p.texture(texture("generic_0")).alpha(0.5, 0);
                }).build());
    }

    private static void linkedLine(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        line(p, c, c.link().length(), direction(c));
    }

    private static void line(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c,
                             double length, Vec3 direction) {
        // Core treats a zero line length as a fallback radius; use a point for coincident endpoints.
        if (length < 1.0E-8) {
            p.point(0, c.lifetime());
            return;
        }
        CompoundTag shape = new CompoundTag();
        shape.putDouble("length", length);
        shape.putDouble("radius", length);
        shape.putDouble("direction_x", direction.x);
        shape.putDouble("direction_y", direction.y);
        shape.putDouble("direction_z", direction.z);
        p.shape(new ResourceLocation("nightshade_core", "line"), shape, 0, c.lifetime());
    }

    private static ResourceLocation id(String path) { return new ResourceLocation("divinity_engine", path); }
    private static ResourceLocation texture(String path) {
        return new ResourceLocation("minecraft", "textures/particle/" + path + ".png");
    }

    private static void nativeTexture(ParticleEmitterSpec.Builder p, String path) {
        p.texture(texture(path)).palette(RenderPalette.WHITE);
    }

    private static void box(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c,
                            double x, double y, double z) {
        p.shape(new ResourceLocation("nightshade_core", "box"), boxData(x, y, z), 0, c.lifetime());
    }

    private static void facingBox(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c,
                                  double x, double y, double z) {
        CompoundTag data = boxData(x, y, z);
        data.putBoolean("oriented", true);
        Vec3 forward = direction(c);
        Vec3 right = UP.cross(forward);
        right = right.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, -1).cross(forward).normalize() : right.normalize();
        basis(data, "right", right);
        basis(data, "up", forward.cross(right).normalize());
        basis(data, "forward", forward);
        p.shape(new ResourceLocation("nightshade_core", "box"), data, 0, c.lifetime());
    }

    private static CompoundTag boxData(double x, double y, double z) {
        CompoundTag data = new CompoundTag();
        data.putDouble("x", x); data.putDouble("y", y); data.putDouble("z", z);
        data.putDouble("radius", Math.sqrt(x * x + y * y + z * z));
        return data;
    }

    private static void basis(CompoundTag data, String name, Vec3 direction) {
        data.putDouble(name + "_x", direction.x);
        data.putDouble(name + "_y", direction.y);
        data.putDouble(name + "_z", direction.z);
    }

    private static Vec3 horizontal(BlessingVisualDesign.Context c) {
        Vec3 travel = direction(c);
        Vec3 direction = new Vec3(travel.x, 0, travel.z);
        return direction.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : direction.normalize();
    }

    private static Vec3 right(BlessingVisualDesign.Context c) {
        return UP.cross(horizontal(c));
    }

    private static Vec3 direction(BlessingVisualDesign.Context c) {
        return c.link() == null || c.link().lengthSqr() < 1.0E-8 ? c.forward() : c.link().normalize();
    }

    private static void motion(ParticleEmitterSpec.Builder p, Vec3 velocity, double spread,
                               double gravity, double drag) {
        p.motion(new ParticleMotion(velocity, new Vec3(spread, spread * 0.5, spread),
                Vec3.ZERO, gravity, drag, 0, 0));
    }
}
