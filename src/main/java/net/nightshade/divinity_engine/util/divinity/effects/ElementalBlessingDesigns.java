package net.nightshade.divinity_engine.util.divinity.effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisualDesign.Context;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.renderlib.api.RenderPalette;
import net.nightshade.renderlib.particle.ParticleBehaviorSpec;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;
import net.nightshade.renderlib.particle.ParticleMotion;

import java.util.List;

/** Vanilla-sprite elemental recipes. The facade alone owns admission and burst counts. */
public final class ElementalBlessingDesigns {
    private static final RenderPalette ASH = new RenderPalette(List.of(0xFF77736E, 0xFFAAA49A), 40);
    private static boolean registered;

    private ElementalBlessingDesigns() { }

    public static void register() {
        if (registered) return;
        BlessingVisualDesigns.register(id("stormstep"), BlessingVisualDesign.builder().duration(12)
                .particles(10, 0.06, ElementalBlessingDesigns::stormstep).build());
        BlessingVisualDesigns.register(id("shockshield"), BlessingVisualDesign.builder().duration(12)
                .particles(8, 0.065, ElementalBlessingDesigns::shockshield).build());
        BlessingVisualDesigns.register(id("tempest_wrath"), BlessingVisualDesign.builder().duration(18)
                .particles(14, 0.075, ElementalBlessingDesigns::tempestWrath).build());
        BlessingVisualDesigns.register(id("charge_relay"), BlessingVisualDesign.builder().duration(16)
                .particles(6, 0.055, ElementalBlessingDesigns::chargeRelay).build());
        BlessingVisualDesigns.register(id("lightning_conduit"), BlessingVisualDesign.builder().duration(12)
                .particles(8, 0.065, ElementalBlessingDesigns::lightningConduit).build());

        BlessingVisualDesigns.register(id("frozen_heart"), BlessingVisualDesign.builder().duration(12)
                .particles(8, 0.065, ElementalBlessingDesigns::frozenHeart).build());
        BlessingVisualDesigns.register(id("icy_pulse"), BlessingVisualDesign.builder().duration(18)
                .particles(10, 0.065, ElementalBlessingDesigns::icyPulse).build());
        BlessingVisualDesigns.register(id("snowveil"), BlessingVisualDesign.builder().duration(10)
                .particles(8, 0.085, ElementalBlessingDesigns::snowveil).build());
        BlessingVisualDesigns.register(id("frostbite_mantle"), BlessingVisualDesign.builder().duration(20)
                .particles(10, 0.065, ElementalBlessingDesigns::frostbiteMantle).build());
        BlessingVisualDesigns.register(id("permafrost_surge"), BlessingVisualDesign.builder().duration(28)
                .particles(16, 0.08, ElementalBlessingDesigns::permafrostSurge).build());

        BlessingVisualDesigns.register(id("aquatic_grace"), BlessingVisualDesign.builder().duration(16)
                .particles(6, 0.075, ElementalBlessingDesigns::aquaticGrace).build());
        BlessingVisualDesigns.register(id("rains_favor"), BlessingVisualDesign.builder().duration(20)
                .particles(6, 0.065, ElementalBlessingDesigns::rainsFavor).build());
        BlessingVisualDesigns.register(id("wavebound"), BlessingVisualDesign.builder().duration(16)
                .particles(12, 0.08, ElementalBlessingDesigns::wavebound).build());
        BlessingVisualDesigns.register(id("undercurrent_grip"), BlessingVisualDesign.builder().duration(20)
                .particles(12, 0.075, ElementalBlessingDesigns::undercurrentGrip).build());
        BlessingVisualDesigns.register(id("tidecallers_wrath"), BlessingVisualDesign.builder().duration(20)
                .particles(16, 0.09, ElementalBlessingDesigns::tidecallersWrath).build());

        BlessingVisualDesigns.register(id("gale_step"), BlessingVisualDesign.builder().duration(12)
                .particles(6, 0.06, ElementalBlessingDesigns::galeStep).build());
        BlessingVisualDesigns.register(id("windcallers_reflex"), BlessingVisualDesign.builder().duration(10)
                .particles(4, 0.12, ElementalBlessingDesigns::windcallersReflex).build());
        BlessingVisualDesigns.register(id("cyclone_leap"), BlessingVisualDesign.builder().duration(24)
                .particles(10, 0.075, ElementalBlessingDesigns::cycloneLeap).build());
        BlessingVisualDesigns.register(id("shearwind_dash"), BlessingVisualDesign.builder().duration(14)
                .particles(8, 0.085, ElementalBlessingDesigns::shearwindDash).build());
        BlessingVisualDesigns.register(id("tempest_spiral"), BlessingVisualDesign.builder().duration(22)
                .particles(14, 0.075, ElementalBlessingDesigns::tempestSpiral).build());

        BlessingVisualDesigns.register(id("beacons_grace"), BlessingVisualDesign.builder().duration(18)
                .particles(6, 0.06, ElementalBlessingDesigns::beaconsGrace).build());
        BlessingVisualDesigns.register(id("cleansing_flame"), BlessingVisualDesign.builder().duration(18)
                .particles(8, 0.085, ElementalBlessingDesigns::cleansingFlame).build());
        BlessingVisualDesigns.register(id("divine_radiance"), BlessingVisualDesign.builder().duration(24)
                .particles(12, 0.09, ElementalBlessingDesigns::divineRadiance).build());
        BlessingVisualDesigns.register(id("lightbearers_oath"), BlessingVisualDesign.builder().duration(10)
                .particles(5, 0.10, ElementalBlessingDesigns::lightbearersOath).build());
        BlessingVisualDesigns.register(id("dawnshatter"), BlessingVisualDesign.builder().duration(20)
                .particles(14, 0.08, ElementalBlessingDesigns::dawnshatter).build());

        BlessingVisualDesigns.register(id("blazing_hands"), BlessingVisualDesign.builder().duration(18)
                .particles(8, 0.085, ElementalBlessingDesigns::blazingHands).build());
        BlessingVisualDesigns.register(id("fireproof"), BlessingVisualDesign.builder().duration(14)
                .particles(8, 0.065, ElementalBlessingDesigns::fireproof).build());
        BlessingVisualDesigns.register(id("flash_flare"), BlessingVisualDesign.builder().duration(22)
                .particles(16, 0.11, ElementalBlessingDesigns::flashFlare).build());
        BlessingVisualDesigns.register(id("kindling_fury"), BlessingVisualDesign.builder().duration(10)
                .particles(8, 0.065, ElementalBlessingDesigns::kindlingFury).build());
        BlessingVisualDesigns.register(id("inferno_mantle"), BlessingVisualDesign.builder().duration(20)
                .particles(10, 0.085, ElementalBlessingDesigns::infernoMantle).build());
        registered = true;
    }

    private static void stormstep(ParticleEmitterSpec.Builder p, Context c) {
        if (!segment(p, c)) box(p, c, 0.1, 0.08, 0.1);
        motion(p, travel(c).scale(-0.04), new Vec3(0.018, 0.01, 0.018), 0.001, 0.84);
        texture(p, "spark_1");
    }

    private static void shockshield(ParticleEmitterSpec.Builder p, Context c) {
        boolean linked = segment(p, c);
        if (!linked) box(p, c, 0.16, 0.2, 0.12);
        motion(p, travel(c).scale(linked ? 0.12 : 0.025), new Vec3(0.025, 0.018, 0.025), 0.002, 0.86);
        texture(p, "spark_2");
    }

    private static void tempestWrath(ParticleEmitterSpec.Builder p, Context c) {
        p.cylinder(Math.min(c.radius(), 0.65), 0.05, 0, c.lifetime());
        motion(p, new Vec3(0, 0.055, 0), new Vec3(0.16, 0.04, 0.16), 0.01, 0.92);
        behavior(p, "attractor", "strength", -0.014);
        texture(p, "spark_1");
    }

    private static void chargeRelay(ParticleEmitterSpec.Builder p, Context c) {
        Vec3 delta = c.link();
        if (delta != null && delta.lengthSqr() > 0.0001) {
            line(p, c, Math.min(0.18, delta.length() * 0.08), delta.normalize());
            motion(p, delta.scale(0.92 / c.lifetime()), new Vec3(0.005, 0.005, 0.005), 0, 1);
        } else {
            box(p, c, 0.1, 0.12, 0.1);
            motion(p, new Vec3(0, 0.035, 0), new Vec3(0.006, 0.006, 0.006), 0, 0.95);
        }
        texture(p, "spark_0");
    }

    private static void lightningConduit(ParticleEmitterSpec.Builder p, Context c) {
        p.cone(0.22, 0.35, 0, c.lifetime());
        motion(p, new Vec3(0, 0.11, 0), new Vec3(0.035, 0.04, 0.035), 0.014, 0.91);
        texture(p, "spark_3");
    }

    private static void frozenHeart(ParticleEmitterSpec.Builder p, Context c) {
        box(p, c, 0.28, 0.45, 0.22);
        motion(p, new Vec3(0, -0.018, 0), new Vec3(0.02, 0.01, 0.02), 0.006, 0.94);
        texture(p, "generic_0");
    }

    private static void icyPulse(ParticleEmitterSpec.Builder p, Context c) {
        if (segment(p, c)) {
            motion(p, travel(c).scale(0.065).add(0, 0.025, 0), new Vec3(0.012, 0.01, 0.012), 0.004, 0.95);
        } else {
            box(p, c, Math.min(c.radius(), 0.65), 0.08, Math.min(c.radius(), 0.65));
            motion(p, new Vec3(0, 0.035, 0), new Vec3(0.035, 0.01, 0.035), 0.004, 0.93);
        }
        texture(p, "generic_2");
    }

    private static void snowveil(ParticleEmitterSpec.Builder p, Context c) {
        box(p, c, 0.28, 0.6, 0.22);
        motion(p, new Vec3(0, -0.008, 0), new Vec3(0.018, 0.012, 0.018), 0.0008, 0.94);
        nativeTexture(p, "generic_4");
    }

    private static void frostbiteMantle(ParticleEmitterSpec.Builder p, Context c) {
        p.cylinder(Math.min(c.radius(), 0.48), 0.22, 0, c.lifetime());
        motion(p, new Vec3(0, -0.018, 0), new Vec3(0.01, 0.006, 0.01), 0.003, 0.95);
        behavior(p, "attractor", "strength", 0.002);
        texture(p, "generic_0");
    }

    private static void permafrostSurge(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.IMPACT) {
            p.cylinder(Math.min(c.radius(), 0.4), 0.3, 0, c.lifetime());
            motion(p, new Vec3(0, -0.012, 0), new Vec3(0.008, 0.004, 0.008), 0.002, 0.96);
            behavior(p, "attractor", "strength", 0.003);
        } else {
            p.cylinder(Math.min(c.radius() * 0.4, 2.4), 0.045, 0, c.lifetime());
            motion(p, new Vec3(0, 0.055, 0), new Vec3(0.055, 0.015, 0.055), 0.003, 0.97);
            behavior(p, "attractor", "strength", -0.006);
        }
        texture(p, "generic_2");
    }

    private static void aquaticGrace(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.HEAL) {
            box(p, c, 0.12, 0.18, 0.12);
            motion(p, new Vec3(0, 0.035, 0), new Vec3(0.009, 0.008, 0.009), 0, 0.97);
            texture(p, "bubble_pop_0");
        } else {
            box(p, c, 0.22, 0.06, 0.18);
            motion(p, c.forward().scale(-0.012).add(0, 0.045, 0), new Vec3(0.012, 0.008, 0.012), 0, 0.99);
            texture(p, "bubble");
        }
    }

    private static void rainsFavor(ParticleEmitterSpec.Builder p, Context c) {
        boolean restoring = c.cue() == Cue.HEAL;
        box(p, c, 0.3, restoring ? 0.12 : 0.18, 0.26);
        motion(p, new Vec3(0, restoring ? 0.045 : -0.075, 0),
                new Vec3(0.009, 0.008, 0.009), 0.006, 0.99);
        texture(p, restoring ? "splash_0" : "drip_fall");
    }

    private static void wavebound(ParticleEmitterSpec.Builder p, Context c) {
        if (!segment(p, c)) box(p, c, 0.32, 0.12, 0.18);
        motion(p, travel(c).scale(-0.055).add(0, 0.03, 0), new Vec3(0.025, 0.015, 0.025), 0.006, 0.95);
        texture(p, "splash_1");
    }

    private static void undercurrentGrip(ParticleEmitterSpec.Builder p, Context c) {
        if (c.link() != null && c.link().lengthSqr() > 0.0001) {
            line(p, c, c.link().length() * 0.12, travel(c));
            motion(p, c.link().scale(0.78 / c.lifetime()), new Vec3(0.009, 0.006, 0.009), 0, 1);
            texture(p, "bubble");
        } else {
            p.cylinder(Math.min(c.radius(), 0.6), 0.18, 0, c.lifetime());
            motion(p, new Vec3(0, 0.018, 0), new Vec3(0.008, 0.005, 0.008), 0, 0.95);
            behavior(p, "attractor", "strength", 0.005);
            texture(p, "bubble_pop_1");
        }
    }

    private static void tidecallersWrath(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.IMPACT) {
            box(p, c, 0.2, 0.12, 0.2);
            // The hook supplies the actual outward/upward knockback, not caster-to-victim elevation.
            motion(p, c.forward().scale(0.14), new Vec3(0.015, 0.012, 0.015), 0.003, 0.96);
        } else {
            p.cylinder(Math.min(c.radius() * 0.4, 2.4), 0.08, 0, c.lifetime());
            motion(p, new Vec3(0, 0.09, 0), new Vec3(0.08, 0.02, 0.08), 0.009, 0.96);
            behavior(p, "attractor", "strength", -0.01);
        }
        texture(p, "splash_2");
    }

    private static void galeStep(ParticleEmitterSpec.Builder p, Context c) {
        box(p, c, 0.2, 0.025, 0.16);
        boolean landing = c.cue() == Cue.WARD;
        Vec3 horizontal = new Vec3(c.forward().x, 0, c.forward().z).normalize();
        Vec3 drift = landing ? new Vec3(0, 0.065, 0) : horizontal.scale(-0.05).add(0, 0.02, 0);
        motion(p, drift, new Vec3(0.025, 0.004, 0.025), landing ? 0.008 : 0.003, 0.9);
        texture(p, "generic_2");
    }

    private static void windcallersReflex(ParticleEmitterSpec.Builder p, Context c) {
        if (!segment(p, c)) box(p, c, 0.12, 0.18, 0.06);
        // Follow the incoming contact segment; cancellation does not reflect the projectile.
        motion(p, travel(c).scale(0.02), new Vec3(0.055, 0.035, 0.055), 0, 0.78);
        texture(p, "sweep_2");
    }

    private static void cycloneLeap(ParticleEmitterSpec.Builder p, Context c) {
        boolean linked = segment(p, c);
        if (!linked) p.cylinder(0.3, 0.12, 0, c.lifetime());
        Vec3 lift = linked ? travel(c).scale(0.045).add(0, 0.025, 0) : new Vec3(0, 0.07, 0);
        motion(p, lift, new Vec3(0.018, 0.012, 0.018), 0.003, 0.94);
        texture(p, "generic_5");
    }

    private static void shearwindDash(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.TRAIL && segment(p, c)) {
            motion(p, travel(c).scale(-0.07), new Vec3(0.012, 0.008, 0.012), 0.001, 0.91);
            texture(p, "generic_1");
        } else {
            box(p, c, 0.06, 0.28, 0.08);
            motion(p, c.forward().scale(0.07), new Vec3(0.025, 0.015, 0.025), 0.002, 0.88);
            texture(p, "sweep_1");
        }
    }

    private static void tempestSpiral(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.IMPACT) {
            box(p, c, 0.18, 0.08, 0.18);
            // Victims are pulled horizontally with fixed lift; only the origin dust rotates.
            motion(p, travel(c).scale(0.065), new Vec3(0.009, 0.004, 0.009), 0, 0.96);
        } else {
            p.cylinder(Math.min(c.radius() * 0.35, 1.3), 0.35, 0, c.lifetime());
            motion(p, new Vec3(0, 0.018, 0), new Vec3(0.012, 0.008, 0.012), 0, 0.94);
            behavior(p, "orbit", "speed", 0.11);
            behavior(p, "attractor", "strength", 0.004);
        }
        texture(p, "generic_3");
    }

    private static void beaconsGrace(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.WARD) {
            p.cylinder(0.33, 0.6, 0, c.lifetime());
            motion(p, new Vec3(0, 0.035, 0), new Vec3(0.008, 0.006, 0.008), 0, 0.95);
            texture(p, "enchanted_hit");
        } else {
            box(p, c, 0.18, 0.035, 0.14);
            motion(p, new Vec3(0, 0.012, 0), new Vec3(0.006, 0.004, 0.006), 0.002, 0.95);
            texture(p, "generic_0");
        }
    }

    private static void cleansingFlame(ParticleEmitterSpec.Builder p, Context c) {
        box(p, c, 0.22, 0.4, 0.18);
        motion(p, new Vec3(0, 0.04, 0), new Vec3(0.016, 0.008, 0.016), -0.0005, 0.96);
        // This proc removes status effects; it neither ignites anything nor restores health.
        texture(p, "generic_5");
    }

    private static void divineRadiance(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.CONTROL) {
            box(p, c, 0.18, 0.08, 0.18);
            motion(p, new Vec3(0, 0.006, 0), new Vec3(0.008, 0.004, 0.008), 0, 0.9);
        } else {
            p.cylinder(0.33, 1.2, 0, c.lifetime());
            motion(p, new Vec3(0, 0.018, 0), new Vec3(0.008, 0.006, 0.008), 0, 0.95);
        }
        texture(p, "enchanted_hit");
    }

    private static void lightbearersOath(ParticleEmitterSpec.Builder p, Context c) {
        box(p, c, 0.12, 0.2, 0.12);
        motion(p, new Vec3(0, 0.018, 0), new Vec3(0.025, 0.02, 0.025), 0.002, 0.9);
        texture(p, "enchanted_hit");
    }

    private static void dawnshatter(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.CAST) {
            cone(p, c, Math.min(c.radius(), 6), Math.tan(Math.PI / 6), c.forward());
            motion(p, c.forward().scale(0.12), new Vec3(0.02, 0.02, 0.02), 0, 0.96);
            texture(p, "enchanted_hit");
        } else {
            box(p, c, 0.12, 0.2, 0.1);
            motion(p, new Vec3(0, 0.045, 0), new Vec3(0.015, 0.018, 0.015), -0.0003, 0.95);
            nativeTexture(p, "flame");
        }
    }

    private static void blazingHands(ParticleEmitterSpec.Builder p, Context c) {
        p.cone(Math.min(c.radius() * 0.4, 0.5), 0.18, 0, c.lifetime());
        motion(p, new Vec3(0, 0.045, 0), new Vec3(0.015, 0.02, 0.015), -0.0008, 0.96);
        nativeTexture(p, "flame");
    }

    private static void fireproof(ParticleEmitterSpec.Builder p, Context c) {
        box(p, c, 0.32, 0.12, 0.25);
        motion(p, new Vec3(0, -0.018, 0), new Vec3(0.03, 0.01, 0.03), 0.005, 0.92);
        p.palette(ASH);
        texture(p, "generic_0");
    }

    private static void flashFlare(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.CONTROL) {
            box(p, c, 0.16, 0.07, 0.16);
            motion(p, new Vec3(0, 0.008, 0), new Vec3(0.012, 0.006, 0.012), 0, 0.88);
            texture(p, "generic_5");
        } else {
            p.cylinder(Math.min(c.radius() * 0.4, 2), 0.14, 0, c.lifetime());
            motion(p, new Vec3(0, 0.12, 0), new Vec3(0.18, 0.035, 0.18), 0.012, 0.94);
            behavior(p, "attractor", "strength", -0.012);
            nativeTexture(p, "flame");
        }
    }

    private static void kindlingFury(ParticleEmitterSpec.Builder p, Context c) {
        box(p, c, 0.08, 0.12, 0.08);
        motion(p, new Vec3(0, 0.055, 0), new Vec3(0.055, 0.035, 0.055), 0.009, 0.9);
        nativeTexture(p, "lava");
    }

    private static void infernoMantle(ParticleEmitterSpec.Builder p, Context c) {
        if (c.cue() == Cue.IMPACT) {
            p.cone(0.2, 0.18, 0, c.lifetime());
            motion(p, new Vec3(0, 0.055, 0), new Vec3(0.012, 0.015, 0.012), 0, 0.97);
        } else {
            p.cylinder(Math.min(c.radius(), 0.7), 0.16, 0, c.lifetime());
            motion(p, new Vec3(0, 0.025, 0), new Vec3(0.012, 0.008, 0.012), -0.0004, 0.96);
        }
        nativeTexture(p, "flame");
    }

    private static boolean segment(ParticleEmitterSpec.Builder p, Context c) {
        if (c.link() == null || c.link().lengthSqr() < 0.0001) return false;
        line(p, c, c.link().length(), c.link().normalize());
        return true;
    }

    private static void line(ParticleEmitterSpec.Builder p, Context c, double length, Vec3 direction) {
        CompoundTag data = new CompoundTag();
        data.putDouble("length", length);
        data.putDouble("direction_x", direction.x);
        data.putDouble("direction_y", direction.y);
        data.putDouble("direction_z", direction.z);
        // Core's culling estimate reads radius, even for a line emitted from its start anchor.
        data.putDouble("radius", length);
        p.shape(new ResourceLocation("nightshade_core", "line"), data, 0, c.lifetime());
    }

    private static Vec3 travel(Context c) {
        return c.link() != null && c.link().lengthSqr() > 0.0001 ? c.link().normalize() : c.forward();
    }

    private static void cone(ParticleEmitterSpec.Builder p, Context c, double length, double spread, Vec3 forward) {
        Vec3 right = new Vec3(0, 1, 0).cross(forward);
        right = right.lengthSqr() < 0.0001 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = forward.cross(right).normalize();
        CompoundTag data = new CompoundTag();
        data.putDouble("radius", length);
        data.putDouble("spread", spread);
        data.putBoolean("oriented", true);
        basis(data, "right", right);
        basis(data, "up", up);
        basis(data, "forward", forward);
        p.shape(new ResourceLocation("nightshade_core", "cone"), data, 0, c.lifetime());
    }

    private static void basis(CompoundTag data, String axis, Vec3 direction) {
        data.putDouble(axis + "_x", direction.x);
        data.putDouble(axis + "_y", direction.y);
        data.putDouble(axis + "_z", direction.z);
    }

    private static void box(ParticleEmitterSpec.Builder p, Context c, double x, double y, double z) {
        CompoundTag data = new CompoundTag();
        data.putDouble("x", x);
        data.putDouble("y", y);
        data.putDouble("z", z);
        data.putDouble("radius", Math.sqrt(x * x + y * y + z * z));
        p.shape(new ResourceLocation("nightshade_core", "box"), data, 0, c.lifetime());
    }

    private static void motion(ParticleEmitterSpec.Builder p, Vec3 velocity, Vec3 spread, double gravity, double drag) {
        p.motion(new ParticleMotion(velocity, spread, Vec3.ZERO, gravity, drag, 0, 0));
    }

    private static void behavior(ParticleEmitterSpec.Builder p, String type, String parameter, double value) {
        CompoundTag data = new CompoundTag();
        data.putDouble(parameter, value);
        p.behavior(new ParticleBehaviorSpec(new ResourceLocation("nightshade_core", type), data));
    }

    private static void nativeTexture(ParticleEmitterSpec.Builder p, String name) {
        // Keep the vanilla fire colors and white snow instead of multiplying in a god tint.
        p.palette(RenderPalette.WHITE);
        texture(p, name);
    }

    private static void texture(ParticleEmitterSpec.Builder p, String name) {
        p.texture(new ResourceLocation("minecraft", "textures/particle/" + name + ".png"));
    }

    private static ResourceLocation id(String path) { return new ResourceLocation("divinity_engine", path); }
}
