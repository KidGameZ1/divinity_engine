package net.nightshade.divinity_engine.client.effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.Frame;
import net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.Mesh;

import static net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.block;
import static net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.smooth;
import static net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.tile;
import static net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.unit;

/** Eighteen independent mechanic compositions; particle-only blessings deliberately have no mesh. */
@OnlyIn(Dist.CLIENT)
public final class ElementalBlessingEffects {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 RIGHT = new Vec3(1, 0, 0);
    private static final double TAU = Math.PI * 2;
    private static boolean registered;

    private ElementalBlessingEffects() { }

    public static void register() {
        if (registered) return;
        BlessingEffectRenderer.register(id("shockshield"), ElementalBlessingEffects::shockshield);
        BlessingEffectRenderer.register(id("tempest_wrath"), ElementalBlessingEffects::tempestWrath);
        BlessingEffectRenderer.register(id("icy_pulse"), ElementalBlessingEffects::icyPulse);
        BlessingEffectRenderer.register(id("frostbite_mantle"), ElementalBlessingEffects::frostbiteMantle);
        BlessingEffectRenderer.register(id("permafrost_surge"), ElementalBlessingEffects::permafrostSurge);
        BlessingEffectRenderer.register(id("wavebound"), ElementalBlessingEffects::wavebound);
        BlessingEffectRenderer.register(id("undercurrent_grip"), ElementalBlessingEffects::undercurrentGrip);
        BlessingEffectRenderer.register(id("tidecallers_wrath"), ElementalBlessingEffects::tidecallersWrath);
        BlessingEffectRenderer.register(id("windcallers_reflex"), ElementalBlessingEffects::windcallersReflex);
        BlessingEffectRenderer.register(id("cyclone_leap"), ElementalBlessingEffects::cycloneLeap);
        BlessingEffectRenderer.register(id("shearwind_dash"), ElementalBlessingEffects::shearwindDash);
        BlessingEffectRenderer.register(id("tempest_spiral"), ElementalBlessingEffects::tempestSpiral);
        BlessingEffectRenderer.register(id("beacons_grace"), ElementalBlessingEffects::beaconsGrace);
        BlessingEffectRenderer.register(id("divine_radiance"), ElementalBlessingEffects::divineRadiance);
        BlessingEffectRenderer.register(id("lightbearers_oath"), ElementalBlessingEffects::lightbearersOath);
        BlessingEffectRenderer.register(id("dawnshatter"), ElementalBlessingEffects::dawnshatter);
        BlessingEffectRenderer.register(id("fireproof"), ElementalBlessingEffects::fireproof);
        BlessingEffectRenderer.register(id("flash_flare"), ElementalBlessingEffects::flashFlare);
        registered = true;
    }

    private static void shockshield(Mesh m, Frame f) {
        if (linked(f)) {
            Vec3 side = side(f.link);
            int steps = f.count(5, 9);
            Vec3 previous = along(f, 0);
            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps;
                double kink = i == steps ? 0 : (i % 2 == 0 ? -0.12 : 0.12);
                Vec3 next = along(f, t).add(side.scale(kink));
                stroke(m, previous, next, 0.025, f.primary);
                m.rawLine(previous, next, f.highlight);
                previous = next;
            }
            Vec3 hit = along(f, 1);
            stroke(m, hit.subtract(side.scale(0.18)), hit.add(side.scale(0.18)), 0.035, f.highlight);
        } else {
            m.ribbon(new Vec3(-0.45, 0.15, 0), new Vec3(-0.08, -0.12, 0), 0.035, f.highlight);
            m.ribbon(new Vec3(-0.08, -0.12, 0), new Vec3(0.2, 0.18, 0), 0.035, f.primary);
            m.ribbon(new Vec3(0.2, 0.18, 0), new Vec3(0.4, -0.08, 0), 0.025, f.accent);
        }
    }

    private static void tempestWrath(Mesh m, Frame f) {
        int forks = f.count(5, 8);
        double reach = 0.15 + 0.8 * smooth(f.progress);
        for (int i = 0; i < forks; i++) {
            double angle = TAU * i / forks + 0.13;
            Vec3 axis = radial(angle, 1), tangent = radial(angle + Math.PI / 2, 1);
            Vec3 root = axis.scale(0.06).add(0, 0.025, 0);
            Vec3 elbow = axis.scale(reach * 0.52).add(tangent.scale(i % 2 == 0 ? 0.08 : -0.08)).add(0, 0.03, 0);
            Vec3 tip = axis.scale(reach).add(0, 0.05, 0);
            groundStroke(m, f, root, elbow, 0.018, f.primary);
            groundStroke(m, f, elbow, tip, 0.012, f.highlight);
            groundStroke(m, f, elbow, elbow.add(axis.scale(0.15)).add(tangent.scale(0.11)), 0.009, f.accent);
        }
    }

    private static void icyPulse(Mesh m, Frame f) {
        if (linked(f)) {
            Vec3 sideways = side(f.link);
            Vec3 previous = along(f, 0);
            int steps = f.count(4, 7);
            double reach = 0.2 + 0.8 * smooth(f.progress);
            for (int i = 1; i <= steps; i++) {
                double t = reach * i / steps;
                Vec3 next = along(f, t).add(sideways.scale(i == steps ? 0 : (i % 2 - 0.5) * 0.15));
                stroke(m, previous, next, 0.028, f.primary);
                if (i % 2 == 0) stroke(m, next, next.add(sideways.scale(0.15)).add(0, 0.12, 0), 0.02, f.highlight);
                previous = next;
            }
        } else {
            for (int i = 0; i < 3; i++) {
                Vec3 tip = radial(i * TAU / 3, 0.6 * smooth(f.progress)).add(0, 0.05, 0);
                groundStroke(m, f, new Vec3(0, 0.02, 0), tip, 0.025, f.primary);
            }
        }
    }

    private static void frostbiteMantle(Mesh m, Frame f) {
        double closing = 0.7 - 0.25 * smooth(f.progress);
        for (int sign : new int[]{-1, 1}) {
            Vec3 foot = new Vec3(sign * closing, 0.02, -0.22);
            groundQuad(m, f, foot, foot.add(0, 0.55, 0), foot.add(-sign * 0.12, 0.43, 0.06), foot.add(-sign * 0.12, 0, 0.06), f.primary);
            groundStroke(m, f, foot.add(0, 0.34, 0), foot.add(-sign * 0.3, 0.34, 0), 0.04, f.highlight);
            groundQuad(m, f, foot.add(0, 0.03, 0), foot.add(0, 0.03, 0.5), foot.add(-sign * 0.13, 0.1, 0.5),
                    foot.add(-sign * 0.13, 0.1, 0), f.accent);
        }
    }

    private static void permafrostSurge(Mesh m, Frame f) {
        double spread = 0.18 + 0.6 * smooth(f.progress);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0 || f.detail == 0 && x * z != 0) continue;
                Vec3 base = new Vec3(x * spread, 0.02, z * spread).scale(f.radius);
                double width = f.radius * 0.11;
                double height = f.radius * (0.05 + 0.08 * (1 - f.progress));
                Vec3 a = base.add(-width, 0, -width), b = base.add(width, 0, -width);
                Vec3 tip = base.add(width * 0.4, height, width * 0.1);
                m.rawQuad(a, b, tip, tip, f.primary);
                m.rawQuad(b, base.add(width, 0, width), tip, tip, f.accent);
                m.rawLine(a, tip, f.highlight);
            }
        }
    }

    private static void wavebound(Mesh m, Frame f) {
        if (!linked(f)) {
            m.quad(new Vec3(-0.35, 0, -0.3), new Vec3(0, 0.18, 0.7), new Vec3(0.35, 0, -0.3), Vec3.ZERO, f.primary);
            return;
        }
        Vec3 lateral = side(f.link), normal = unit(f.link.cross(lateral), UP);
        double head = 0.35 + 0.65 * smooth(f.progress);
        Vec3 bow = along(f, head), tail = along(f, Math.max(0, head - 0.65));
        for (int sign : new int[]{-1, 1}) {
            Vec3 wing = tail.add(lateral.scale(sign * 0.34));
            m.rawQuad(bow, wing, wing.add(normal.scale(0.09)), bow.add(normal.scale(0.06)), f.primary);
            stroke(m, wing, along(f, 0).add(lateral.scale(sign * 0.43)), 0.025, f.highlight);
        }
    }

    private static void undercurrentGrip(Mesh m, Frame f) {
        if (linked(f)) {
            Vec3 lateral = side(f.link);
            double head = 0.15 + 0.85 * smooth(f.progress);
            for (int sign : new int[]{-1, 1}) {
                Vec3 a = along(f, Math.max(0, head - 0.4)).add(lateral.scale(sign * 0.13));
                Vec3 b = along(f, head);
                stroke(m, a, b, 0.04, sign > 0 ? f.primary : f.accent);
                m.rawLine(a.add(0, 0.035, 0), b.add(0, 0.035, 0), f.highlight);
            }
        } else {
            double edge = 0.65 * (1 - 0.6 * smooth(f.progress));
            for (int sign : new int[]{-1, 1}) {
                m.quad(new Vec3(sign * edge, 0.12, -0.3), new Vec3(sign * edge, 0.12, 0.3),
                        new Vec3(0, -0.1, 0.16), new Vec3(0, -0.1, -0.16), f.primary);
            }
        }
    }

    private static void tidecallersWrath(Mesh m, Frame f) {
        if (linked(f)) {
            Vec3 lateral = side(f.link);
            Vec3 head = along(f, 0.2 + 0.8 * smooth(f.progress));
            m.rawQuad(head.add(lateral.scale(-0.3)), head.add(lateral.scale(0.3)),
                    head.add(lateral.scale(0.22)).add(0, 0.3, 0), head.add(lateral.scale(-0.22)).add(0, 0.3, 0), f.primary);
            return;
        }
        double edge = 0.12 + 0.78 * smooth(f.progress);
        for (int i = 0; i < 4; i++) {
            Vec3 axis = radial(i * Math.PI / 2, 1), tangent = radial(i * Math.PI / 2 + Math.PI / 2, 1);
            for (int step = -1; step <= 1; step++) {
                Vec3 base = axis.scale(edge - Math.abs(step) * 0.055).add(tangent.scale(step * 0.22)).scale(f.radius);
                Vec3 half = tangent.scale(f.radius * 0.085);
                Vec3 crest = axis.scale(-f.radius * 0.04).add(0, f.radius * (0.035 + 0.035 * f.progress), 0);
                m.rawQuad(base.subtract(half), base.add(half), base.add(half).add(crest), base.subtract(half).add(crest), f.primary);
                m.rawLine(base.add(half).add(crest), base.subtract(half).add(crest), f.highlight);
            }
        }
    }

    private static void windcallersReflex(Mesh m, Frame f) {
        if (linked(f)) {
            Vec3 contact = along(f, 1), lateral = side(f.link), vertical = unit(f.link.cross(lateral), UP);
            double opening = 0.08 + 0.23 * smooth(f.progress);
            for (int sign : new int[]{-1, 1}) {
                Vec3 a = contact.add(lateral.scale(sign * opening)).subtract(vertical.scale(0.42));
                Vec3 b = contact.add(lateral.scale(sign * (opening + 0.13))).add(vertical.scale(0.42));
                stroke(m, a, b, 0.035, f.highlight);
                stroke(m, b, b.add(lateral.scale(sign * 0.16)).subtract(vertical.scale(0.09)), 0.024, f.primary);
            }
        } else {
            double gap = 0.08 + 0.2 * smooth(f.progress);
            m.ribbon(new Vec3(-gap, -0.48, 0), new Vec3(-gap - 0.14, 0.48, 0), 0.035, f.highlight);
            m.ribbon(new Vec3(gap, -0.48, 0), new Vec3(gap + 0.14, 0.48, 0), 0.035, f.primary);
        }
    }

    private static void cycloneLeap(Mesh m, Frame f) {
        Vec3 root = linked(f) ? along(f, 0) : Vec3.ZERO;
        Vec3 rise = linked(f) ? f.link : new Vec3(0, f.radius * 0.8, 0);
        Vec3 lateral = side(rise);
        for (int i = 0; i < f.count(3, 5); i++) {
            double t = (i + smooth(f.progress)) / 5;
            Vec3 center = root.add(rise.scale(t));
            double width = 0.3 * (1 - t * 0.55);
            Vec3 dip = center.add(0, -0.07, 0);
            stroke(m, center.subtract(lateral.scale(width)), dip, 0.028, f.primary);
            stroke(m, dip, center.add(lateral.scale(width)), 0.028, f.highlight);
        }
    }

    private static void shearwindDash(Mesh m, Frame f) {
        if (linked(f) && cue(f, "TRAIL")) {
            Vec3 lateral = side(f.link), axis = unit(f.link, UP);
            for (int sign : new int[]{-1, 1}) {
                Vec3 tail = along(f, 0).add(lateral.scale(sign * 0.2));
                Vec3 tip = along(f, 0.35 + 0.65 * smooth(f.progress)).add(lateral.scale(sign * 0.13));
                m.rawQuad(tail, tail.add(0, 0.13, 0), tip.add(axis.scale(0.08)), tip, f.primary);
                m.rawLine(tail, tip, f.highlight);
            }
        } else {
            double length = 0.2 + 0.4 * smooth(f.progress);
            m.ribbon(new Vec3(-length, length * 0.65, 0), new Vec3(length, -length * 0.65, 0), 0.035, f.highlight);
            m.ribbon(new Vec3(-length, -0.15, 0.06), new Vec3(length * 0.65, 0.4, 0.06), 0.025, f.primary);
        }
    }

    private static void tempestSpiral(Mesh m, Frame f) {
        if (linked(f)) {
            Vec3 head = along(f, smooth(f.progress));
            stroke(m, along(f, Math.max(0, f.progress - 0.32)), head, 0.035, f.primary);
            stroke(m, head, head.add(0, 0.16, 0), 0.02, f.highlight);
            return;
        }
        int steps = f.count(8, 14);
        double contraction = 1 - 0.68 * smooth(f.progress);
        Vec3 previous = null;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 next = radial(t * TAU * 1.3 + f.phase * 0.4, (0.8 - t * 0.6) * contraction)
                    .add(0, 0.03 + t * 0.22, 0);
            if (previous != null) groundStroke(m, f, previous, next, 0.018, i % 3 == 0 ? f.highlight : f.primary);
            previous = next;
        }
    }

    private static void beaconsGrace(Mesh m, Frame f) {
        if (cue(f, "WARD")) {
            double split = 0.12 + 0.25 * smooth(f.progress);
            tile(m, new Vec3(-split, 0.02, 0), 0.09, 0.43, f.primary);
            tile(m, new Vec3(split, 0.02, 0), 0.09, 0.43, f.highlight);
            m.line(new Vec3(-split, 0.46, 0), new Vec3(split, 0.46, 0), f.accent);
        } else {
            for (int i = 0; i < 3; i++) {
                double height = 0.08 + (i + 1) * 0.055 + f.progress * 0.08;
                Vec3 base = new Vec3((i - 1) * 0.24, 0, i % 2 * 0.16);
                groundQuad(m, f, base.add(-0.025, 0, 0), base.add(0.025, 0, 0),
                        base.add(0.025, height * 2, 0), base.add(-0.025, height * 2, 0), i == 1 ? f.highlight : f.primary);
            }
        }
    }

    private static void divineRadiance(Mesh m, Frame f) {
        double expansion = 0.12 + 0.7 * smooth(f.progress);
        int shutters = f.count(4, 8);
        for (int i = 0; i < shutters; i++) {
            double angle = TAU * i / shutters;
            Vec3 center = radial(angle, expansion).scale(f.radius), tangent = radial(angle + Math.PI / 2, 0.07 * f.radius);
            m.rawQuad(center.subtract(tangent).add(0, -0.18, 0), center.add(tangent).add(0, -0.18, 0),
                    center.add(tangent).add(0, 0.22, 0), center.subtract(tangent).add(0, 0.22, 0), f.highlight);
        }
        double body = 1 / f.radius;
        for (int sign : new int[]{-1, 1}) {
            tile(m, new Vec3(sign * 0.34 * body, 0, 0), 0.07 * body, 0.38 * body, f.primary);
        }
    }

    private static void lightbearersOath(Mesh m, Frame f) {
        double cut = 0.22 + 0.4 * smooth(f.progress);
        Vec3 a = new Vec3(-cut * 0.5, cut, 0), b = new Vec3(0.05, 0.02, 0.02), c = new Vec3(cut * 0.55, -cut, 0);
        m.ribbon(a, b, 0.045, f.highlight);
        m.ribbon(b, c, 0.035, f.primary);
        m.triangle(b, b.add(-0.18, -0.025, 0), b.add(-0.28, 0.16, 0), f.accent);
        m.triangle(b, b.add(0.18, 0.025, 0), b.add(0.25, -0.18, 0), f.highlight);
    }

    private static void dawnshatter(Mesh m, Frame f) {
        if (cue(f, "IMPACT")) {
            for (int i = 0; i < 3; i++) {
                Vec3 base = new Vec3((i - 1) * 0.16, -0.22 + i * 0.07, 0);
                m.quad(base, base.add(0.07, 0, 0), base.add(0.1, 0.48 * (1 - f.progress), 0.06), base.add(0.03, 0.56, 0.06), f.highlight);
            }
            return;
        }
        int rays = f.count(5, 9);
        double reach = 0.2 + 0.78 * smooth(f.progress);
        for (int i = 0; i < rays; i++) {
            double angle = -Math.PI / 6 + Math.PI / 3 * i / (rays - 1);
            Vec3 axis = new Vec3(Math.sin(angle), 0, Math.cos(angle));
            Vec3 tail = axis.scale(reach * 0.18), tip = axis.scale(reach);
            Vec3 edge = new Vec3(Math.cos(angle), 0, -Math.sin(angle)).scale(0.024);
            m.quad(tail.subtract(edge), tip.subtract(edge.scale(0.3)), tip.add(edge.scale(0.3)), tail.add(edge), f.primary);
            m.line(tail.add(0, 0.018, 0), tip.add(0, 0.018, 0), f.highlight);
        }
    }

    private static void fireproof(Mesh m, Frame f) {
        double shed = 0.04 + f.progress * 0.18;
        for (int sign : new int[]{-1, 1}) {
            Vec3 base = new Vec3(sign * (0.28 + shed), -0.12, -0.16);
            m.quad(base, base.add(0, 0.36, 0), base.add(-sign * 0.1, 0.42, 0.08), base.add(-sign * 0.1, 0, 0.08), f.primary);
            block(m, base.add(sign * 0.08, -0.03 - f.progress * 0.12, 0), 0.025, 0.035, 0.02,
                    f.accent, f.primary, f.highlight);
        }
        m.quad(new Vec3(-0.3, -0.15, -0.2), new Vec3(0.3, -0.15, -0.2),
                new Vec3(0.24, -0.09, 0.15), new Vec3(-0.24, -0.09, 0.15), f.accent);
    }

    private static void flashFlare(Mesh m, Frame f) {
        double expansion = 0.1 + 0.82 * smooth(f.progress);
        for (int i = 0; i < 4; i++) {
            Vec3 axis = radial(Math.PI / 4 + i * Math.PI / 2, 1);
            Vec3 tangent = radial(Math.PI / 4 + i * Math.PI / 2 + Math.PI / 2, 0.2);
            Vec3 base = axis.scale(expansion).add(0, 0.06, 0);
            Vec3 crest = base.add(0, 0.18 * (1 - f.progress), 0).subtract(axis.scale(0.07));
            m.rawQuad(base.subtract(tangent).scale(f.radius), base.add(tangent).scale(f.radius),
                    crest.add(tangent.scale(0.6)).scale(f.radius), crest.subtract(tangent.scale(0.6)).scale(f.radius), f.highlight);
            groundStroke(m, f, axis.scale(0.1).add(0, 0.02, 0), base, 0.014, f.primary);
        }
    }

    private static boolean cue(Frame f, String cue) { return cue.equals(String.valueOf(f.cue)); }

    private static boolean linked(Frame f) { return f.link != null && f.link.lengthSqr() > 0.0001; }

    private static Vec3 along(Frame f, double progress) { return f.link.scale(progress - 0.5); }

    private static Vec3 side(Vec3 direction) { return unit(UP.cross(unit(direction, UP)), RIGHT); }

    private static Vec3 radial(double angle, double radius) {
        return new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    private static void groundStroke(Mesh m, Frame f, Vec3 a, Vec3 b, double width, int color) {
        stroke(m, a.scale(f.radius), b.scale(f.radius), width * f.radius, color);
    }

    private static void groundQuad(Mesh m, Frame f, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        m.rawQuad(a.scale(f.radius), b.scale(f.radius), c.scale(f.radius), d.scale(f.radius), color);
    }

    private static void stroke(Mesh m, Vec3 a, Vec3 b, double width, int color) {
        if (a.distanceToSqr(b) < 1.0E-10 || !m.room(4)) return;
        Vec3 half = side(b.subtract(a)).scale(width);
        m.rawQuad(a.subtract(half), a.add(half), b.add(half), b.subtract(half), color);
    }

    private static ResourceLocation id(String path) { return new ResourceLocation("divinity_engine", path); }
}
