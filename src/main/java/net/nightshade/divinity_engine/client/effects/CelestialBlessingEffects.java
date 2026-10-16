package net.nightshade.divinity_engine.client.effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.Frame;
import net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.Mesh;

import static net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.block;
import static net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.smooth;
import static net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.tile;
import static net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.unit;

/** Twenty independent mechanic compositions; the ten particle-only blessings have no mesh entry. */
public final class CelestialBlessingEffects {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private CelestialBlessingEffects() { }

    public static void registerPortal() {
        BlessingEffectRenderer.register(id("pocket_dimension"), (mesh, frame) -> {
            if (frame.persistentGateway) pocketDimension(mesh, frame);
        });
    }

    public static void register() {
        BlessingEffectRenderer.register(id("radiant_strike"), CelestialBlessingEffects::radiantStrike);
        BlessingEffectRenderer.register(id("sunburst"), CelestialBlessingEffects::sunburst);
        BlessingEffectRenderer.register(id("solar_ward"), CelestialBlessingEffects::solarWard);
        BlessingEffectRenderer.register(id("corona_ascendant"), CelestialBlessingEffects::coronaAscendant);
        BlessingEffectRenderer.register(id("serpents_bite"), CelestialBlessingEffects::serpentsBite);
        BlessingEffectRenderer.register(id("coiled_retribution"), CelestialBlessingEffects::coiledRetribution);
        BlessingEffectRenderer.register(id("foresight"), CelestialBlessingEffects::foresight);
        BlessingEffectRenderer.register(id("mind_mirror"), CelestialBlessingEffects::mindMirror);
        BlessingEffectRenderer.register(id("arcane_recall"), CelestialBlessingEffects::arcaneRecall);
        BlessingEffectRenderer.register(id("phase_blink"), CelestialBlessingEffects::phaseBlink);
        BlessingEffectRenderer.register(id("silver_mirror"), CelestialBlessingEffects::silverMirror);
        BlessingEffectRenderer.register(id("second_chance"), CelestialBlessingEffects::secondChance);
        BlessingEffectRenderer.register(id("echo_step"), CelestialBlessingEffects::echoStep);
        BlessingEffectRenderer.register(id("threadsever"), CelestialBlessingEffects::threadsever);
        BlessingEffectRenderer.register(id("fateful_draw"), CelestialBlessingEffects::fatefulDraw);
        BlessingEffectRenderer.register(id("veilpiercer"), CelestialBlessingEffects::veilpiercer);
        BlessingEffectRenderer.register(id("celestial_ward"), CelestialBlessingEffects::celestialWard);
        BlessingEffectRenderer.register(id("starlit_mirror"), CelestialBlessingEffects::starlitMirror);
        BlessingEffectRenderer.register(id("gravity_fold"), CelestialBlessingEffects::gravityFold);
        BlessingEffectRenderer.register(id("pocket_dimension"), CelestialBlessingEffects::pocketDimension);
    }

    private static void radiantStrike(Mesh m, Frame f) {
        double cut = smooth(f.progress) * 0.22;
        for (int i = 0; i < f.count(2, 3); i++) {
            Vec3 a = new Vec3(-0.46 + i * 0.12, 0.57 - cut, i * 0.025);
            Vec3 b = new Vec3(0.24 + i * 0.12, -0.43 - cut, i * 0.025);
            m.ribbon(a, b, i == 0 ? 0.07 : 0.035, i == 0 ? f.highlight : f.primary);
            block(m, b.add(0.1, -0.08, 0.05), 0.045, 0.055, 0.025, f.accent, f.primary, f.highlight);
        }
    }

    private static void sunburst(Mesh m, Frame f) {
        if (!cue(f, "CAST")) {
            for (int i = -1; i <= 1; i++) {
                Vec3 a = new Vec3(i * 0.13, -0.12, 0);
                m.ribbon(a, a.add(i * 0.24, 0.5, 0.26 + f.progress * 0.2), 0.055, f.highlight);
            }
            return;
        }
        // The gameplay query is a box, not a circular ground-only radius.
        double r = 0.12 + 0.84 * smooth(f.progress);
        for (int side : new int[]{-1, 1}) {
            worldQuad(m, f, new Vec3(-r, 0.04, side * r), new Vec3(r, 0.04, side * r),
                    new Vec3(r, 0.12, side * (r + 0.04)), new Vec3(-r, 0.12, side * (r + 0.04)), f.primary);
            worldQuad(m, f, new Vec3(side * r, 0.04, -r), new Vec3(side * r, 0.04, r),
                    new Vec3(side * (r + 0.04), 0.12, r), new Vec3(side * (r + 0.04), 0.12, -r), f.primary);
            for (int i = -1; i <= 1; i++) {
                worldBlock(m, f, new Vec3(i * r * 0.65, 0.12, side * r), 0.035, 0.065, 0.025,
                        f.accent, f.primary, f.highlight);
            }
        }
    }

    private static void solarWard(Mesh m, Frame f) {
        double seat = (1 - smooth(f.progress)) * 0.16;
        block(m, new Vec3(0, 0.08, 0.34 + seat), 0.24, 0.28, 0.035, f.primary, f.accent, f.highlight);
        for (int side : new int[]{-1, 1}) {
            block(m, new Vec3(side * (0.38 + seat), 0.34, 0.12), 0.15, 0.10, 0.08,
                    f.primary, f.accent, f.highlight);
            block(m, new Vec3(side * 0.16, -0.29, 0.32), 0.12, 0.075, 0.035,
                    f.accent, f.primary, f.highlight);
        }
        m.line(new Vec3(0, -0.12, 0.385 + seat), new Vec3(0, 0.28, 0.385 + seat), f.highlight);
    }

    private static void coronaAscendant(Mesh m, Frame f) {
        if (cue(f, "IMPACT")) {
            for (int side : new int[]{-1, 1}) {
                tile(m, new Vec3(side * 0.2, 0, 0), 0.15, 0.055, f.highlight);
                m.ribbon(new Vec3(side * 0.36, 0.03, 0), new Vec3(side * 0.6, 0.13, 0), 0.025, f.primary);
            }
        } else {
            for (int i = 0; i < f.count(3, 5); i++) {
                double x = (i - 2) * 0.16;
                double h = 0.2 + (i % 2) * 0.14 + f.progress * 0.18;
                m.quad(new Vec3(x, 0, 0), new Vec3(x + 0.09, 0, 0),
                        new Vec3(x + 0.09, h, 0), new Vec3(x, h, 0), f.primary);
                tile(m, new Vec3(x + 0.06, h + 0.065, 0), 0.03, 0.05, f.highlight);
            }
        }
    }

    private static void serpentsBite(Mesh m, Frame f) {
        double close = 0.14 * (1 - smooth(f.progress));
        for (int side : new int[]{-1, 1}) {
            double x = side * (0.14 + close);
            m.quad(new Vec3(x - 0.055, 0.34, 0), new Vec3(x + 0.055, 0.34, 0),
                    new Vec3(x + 0.055, 0.02, 0.07), new Vec3(x - 0.055, 0.02, 0.07), f.highlight);
            m.triangle(new Vec3(x - 0.055, 0.02, 0.07), new Vec3(x + 0.055, 0.02, 0.07),
                    new Vec3(x, -0.17, 0.09), f.primary);
            m.ribbon(new Vec3(x, -0.23, 0.09), new Vec3(x, -0.32 - f.progress * 0.12, 0.09), 0.025, f.accent);
        }
    }

    private static void coiledRetribution(Mesh m, Frame f) {
        if (cue(f, "IMPACT")) {
            for (int side : new int[]{-1, 1}) {
                Vec3 base = new Vec3(side * 0.29, -0.1, 0);
                m.triangle(base.add(-0.09, -0.12, 0), base.add(0.09, -0.12, 0),
                        base.add(-side * 0.16, 0.36, 0.07), f.primary);
            }
            return;
        }
        double cinch = 0.1 * (1 - smooth(f.progress));
        for (int i = -2; i <= 2; i++) {
            double x = i * (0.2 + cinch * 0.2);
            block(m, new Vec3(x, Math.abs(i) * 0.045, 0.28 - Math.abs(i) * 0.08 + cinch),
                    0.095, 0.13, 0.045, f.primary, f.accent, f.highlight);
        }
    }

    private static void foresight(Mesh m, Frame f) {
        for (int i = 0; i < 3; i++) {
            double offset = (2 - i) * (0.14 + (1 - smooth(f.progress)) * 0.06);
            rectangle(m, new Vec3(offset, offset * 0.5, i * 0.10), 0.23, 0.32,
                    i == 2 ? f.highlight : f.accent);
            if (i == 2) m.line(new Vec3(-0.11, -0.05, 0.2), new Vec3(0.14, 0.14, 0.2), f.primary);
        }
    }

    private static void mindMirror(Mesh m, Frame f) {
        if (f.link != null) {
            Vec3 from = f.link.scale(-0.5), to = f.link.scale(0.5);
            Vec3 transfer = from.add(f.link.scale(smooth(f.progress)));
            rawPane(m, transfer, f.right, f.up, 0.12, 0.16, f.primary);
            rawPane(m, from.add(f.up.scale(0.2)), f.right, f.up, 0.10, 0.035, f.accent);
            rawPane(m, to.add(f.up.scale(-0.2)), f.right, f.up, 0.10, 0.035, f.highlight);
            return;
        }
        double split = cue(f, "WARD") ? f.progress * 0.3 : (1 - f.progress) * 0.3;
        for (int side : new int[]{-1, 1}) {
            tile(m, new Vec3(side * (0.1 + split), side * split * 0.5, 0), 0.08, 0.19, f.primary);
            m.line(new Vec3(side * (0.1 + split), -0.1, -0.02),
                    new Vec3(side * (0.1 + split), 0.1, -0.02), f.highlight);
        }
    }

    private static void arcaneRecall(Mesh m, Frame f) {
        if (cue(f, "MARK")) {
            worldQuad(m, f, new Vec3(-0.25, 0.025, -0.3), new Vec3(0.18, 0.025, -0.3),
                    new Vec3(0.18, 0.025, 0.24), new Vec3(-0.25, 0.025, 0.24), f.accent);
            worldQuad(m, f, new Vec3(0.18, 0.03, 0.24), new Vec3(0.18, 0.15, 0.05),
                    new Vec3(0.02, 0.03, 0.24), new Vec3(0.02, 0.03, 0.24), f.highlight);
            m.rawLine(new Vec3(-0.13, 0.04, -0.15).scale(f.radius), new Vec3(0.06, 0.04, -0.15).scale(f.radius), f.primary);
            return;
        }
        Vec3 segment = f.link == null ? Vec3.ZERO : f.link;
        for (int end : new int[]{-1, 1}) {
            Vec3 center = segment.scale(end * 0.5).add(0, 0.65, 0);
            double fold = end < 0 ? 1 - smooth(f.progress) : smooth(f.progress);
            Vec3 edge = f.right.scale(0.1 + fold * 0.3).add(f.forward.scale((1 - fold) * 0.24));
            m.rawQuad(center.add(0, -0.55, 0), center.add(edge).add(0, -0.55, 0),
                    center.add(edge).add(0, 0.55, 0), center.add(0, 0.55, 0), f.primary);
            m.rawLine(center.add(0, -0.55, 0), center.add(0, 0.55, 0), f.highlight);
        }
    }

    private static void phaseBlink(Mesh m, Frame f) {
        Vec3 segment = f.link == null ? f.forward.scale(0.8) : f.link;
        for (int end : new int[]{-1, 1}) {
            Vec3 feet = segment.scale(end * 0.5);
            rawPane(m, feet.add(0, 0.75, 0), f.right, UP, 0.035, 0.66, f.highlight);
            rawPane(m, feet.add(f.right.scale(end * 0.12)).add(0, 0.55, 0), f.right, UP, 0.025, 0.4, f.accent);
        }
        for (int i = 0; i < f.count(2, 4); i++) {
            double t = (f.progress + i * 0.19) % 1;
            Vec3 p = segment.scale(t - 0.5).add(0, 0.3, 0);
            m.rawLine(p, p.add(unit(segment, f.forward).scale(0.18)), f.primary);
        }
    }

    private static void silverMirror(Mesh m, Frame f) {
        if (cue(f, "IMPACT")) {
            for (int i = 0; i < 3; i++) {
                double x = (i - 1) * (0.16 + f.progress * 0.2);
                m.quad(new Vec3(x, -0.15, 0), new Vec3(x + 0.10, -0.05, 0),
                        new Vec3(x + 0.16, 0.3, 0), new Vec3(x + 0.06, 0.2, 0), f.highlight);
            }
            return;
        }
        double dent = cue(f, "CAST") ? 0 : Math.sin(f.progress * Math.PI) * 0.12;
        rectangle(m, new Vec3(0, 0, 0.31), 0.35, 0.42, f.highlight);
        m.quad(new Vec3(-0.31, -0.38, 0.3), new Vec3(0, -0.38, 0.3 - dent),
                new Vec3(0, 0.38, 0.3 - dent), new Vec3(-0.31, 0.38, 0.3), f.primary);
        m.quad(new Vec3(0, -0.38, 0.3 - dent), new Vec3(0.31, -0.38, 0.3),
                new Vec3(0.31, 0.38, 0.3), new Vec3(0, 0.38, 0.3 - dent), f.accent);
    }

    private static void secondChance(Mesh m, Frame f) {
        double gap = 0.3 * (1 - smooth(f.progress));
        rectangle(m, new Vec3(0, 0.52 + gap, 0), 0.16, 0.17, f.highlight);
        for (int side : new int[]{-1, 1}) {
            Vec3 shoulder = new Vec3(side * (0.3 + gap), 0.24, 0);
            m.ribbon(shoulder, shoulder.add(0, -0.48, 0), 0.06, f.primary);
            m.ribbon(new Vec3(side * (0.14 + gap), -0.18, 0),
                    new Vec3(side * (0.14 + gap), -0.68, 0), 0.055, f.primary);
            m.ribbon(new Vec3(side * gap, 0.3, 0), new Vec3(side * (0.26 + gap), 0.3, 0), 0.04, f.accent);
        }
        m.line(new Vec3(-0.22, -0.1, 0), new Vec3(0.22, -0.1, 0), f.highlight);
    }

    private static void echoStep(Mesh m, Frame f) {
        if (f.link == null) {
            for (int side : new int[]{-1, 1}) {
                double x = side * 0.18;
                worldQuad(m, f, new Vec3(x - 0.08, 0.03, -0.2), new Vec3(x + 0.08, 0.03, -0.2),
                        new Vec3(x + 0.08, 0.03, 0.13), new Vec3(x - 0.08, 0.03, 0.13), f.primary);
                m.rawLine(new Vec3(x - 0.08, 0.035, 0.03).scale(f.radius),
                        new Vec3(x + 0.08, 0.035, 0.03).scale(f.radius), f.highlight);
            }
            return;
        }
        Vec3 source = f.link.scale(-0.5), target = f.link.scale(0.5);
        double collapse = 0.04 + (1 - smooth(f.progress)) * 0.24;
        rawPane(m, source.add(0, 0.7, 0), f.right, UP, collapse, 0.65, f.accent);
        Vec3 along = unit(new Vec3(f.forward.x, 0, f.forward.z), new Vec3(0, 0, 1));
        for (int side : new int[]{-1, 1}) {
            Vec3 p = target.add(f.right.scale(side * 0.16)).add(0, 0.035, 0);
            rawPane(m, p, f.right, along, 0.085, 0.18, f.highlight);
        }
        m.rawLine(source.add(0, 0.08, 0), source.add(f.link.scale(smooth(f.progress))).add(0, 0.08, 0), f.primary);
    }

    private static void threadsever(Mesh m, Frame f) {
        double recoil = 0.08 + smooth(f.progress) * 0.3;
        for (int row : new int[]{-1, 1}) {
            double y = row * 0.14;
            m.ribbon(new Vec3(-0.65, y, 0), new Vec3(-recoil, y + row * 0.09, 0), 0.025, f.primary);
            m.ribbon(new Vec3(recoil, y - row * 0.09, 0), new Vec3(0.65, y, 0), 0.025, f.primary);
            m.line(new Vec3(-recoil, y + row * 0.09, 0), new Vec3(-recoil - 0.06, y + row * 0.2, 0), f.highlight);
            m.line(new Vec3(recoil, y - row * 0.09, 0), new Vec3(recoil + 0.06, y - row * 0.2, 0), f.accent);
        }
    }

    private static void fatefulDraw(Mesh m, Frame f) {
        int selected = cue(f, "TRAIL") ? 1 : cue(f, "WARD") ? 2 : 0;
        double settle = smooth(f.progress);
        for (int i = 0; i < 3; i++) {
            double x = (i - 1) * 0.32 * (i == selected ? 1 - settle : 1 + settle * 0.4);
            double y = i == selected ? settle * 0.16 : -settle * 0.16;
            Vec3 center = new Vec3(x, y, 0.3);
            rectangle(m, center, 0.115, 0.16, i == selected ? f.highlight : f.accent);
            if (i == 0) block(m, center, 0.065, 0.07, 0.018, f.primary, f.accent, f.highlight);
            if (i == 1) {
                m.line(center.add(-0.06, -0.06, -0.01), center.add(0.05, 0, -0.01), f.primary);
                m.line(center.add(0.05, 0, -0.01), center.add(-0.06, 0.06, -0.01), f.primary);
            }
            if (i == 2) m.quad(center.add(-0.06, 0.08, -0.01), center.add(0.06, 0.08, -0.01),
                    center.add(0.04, -0.06, -0.01), center.add(-0.04, -0.06, -0.01), f.primary);
        }
    }

    private static void veilpiercer(Mesh m, Frame f) {
        for (int x : new int[]{-1, 1}) for (int y : new int[]{-1, 1}) {
            Vec3 corner = new Vec3(x * 0.62, y * 0.7, 0);
            m.line(corner, corner.add(-x * 0.2, 0, 0), f.highlight);
            m.line(corner, corner.add(0, -y * 0.22, 0), f.primary);
            if (f.detail > 0) m.line(corner, corner.add(0, 0, 0.16), f.accent);
        }
    }

    private static void celestialWard(Mesh m, Frame f) {
        if (cue(f, "HEAL")) {
            for (int side : new int[]{-1, 1}) {
                tile(m, new Vec3(side * (0.25 - f.progress * 0.16), 0, 0), 0.045, 0.09, f.highlight);
            }
            return;
        }
        Vec3 normal = f.link == null ? f.forward : unit(f.link, f.forward);
        Vec3 right = unit(UP.cross(normal), f.right), up = unit(normal.cross(right), UP);
        Vec3 hit = f.link == null ? Vec3.ZERO : f.link.scale(0.5);
        rawPane(m, hit, right, up, 0.28, 0.38, f.primary);
        rawPane(m, hit.subtract(normal.scale(0.07)), right, up, 0.37, 0.25, f.accent);
        m.rawLine(hit.add(right.scale(-0.24)), hit.add(right.scale(0.24)), f.highlight);
        m.rawLine(hit.add(up.scale(-0.23)), hit.add(up.scale(0.23)), f.highlight);
    }

    private static void starlitMirror(Mesh m, Frame f) {
        if (f.link != null) {
            Vec3 normal = unit(f.link, f.forward), right = unit(UP.cross(normal), f.right);
            Vec3 up = unit(normal.cross(right), UP), hit = f.link.scale(-0.5);
            Vec3 hinge = right.scale(0.28 * Math.cos(f.progress * 0.8))
                    .add(normal.scale(0.28 * Math.sin(f.progress * 0.8)));
            m.rawQuad(hit.subtract(up.scale(0.4)), hit.add(hinge).subtract(up.scale(0.4)),
                    hit.add(hinge).add(up.scale(0.4)), hit.add(up.scale(0.4)), f.primary);
            m.rawLine(hit, hit.add(f.link.scale(0.8)), f.highlight);
            rawPane(m, hit.add(f.link.scale(0.75)), right, up, 0.065, 0.065, f.accent);
            return;
        }
        rectangle(m, new Vec3(0, 0.12, 0.3), 0.2, 0.36, f.accent);
        m.quad(new Vec3(-0.17, -0.2, 0.3), new Vec3(0.08, -0.2, 0.43),
                new Vec3(0.08, 0.44, 0.43), new Vec3(-0.17, 0.44, 0.3), f.primary);
    }

    private static void gravityFold(Mesh m, Frame f) {
        double lift = 0.06 * smooth(f.progress);
        for (int layer = 0; layer < 2; layer++) {
            double y = -0.1 - layer * 0.16, width = 0.42 - layer * 0.1;
            for (int side : new int[]{-1, 1}) {
                m.rawQuad(new Vec3(side * width, y + lift, -width), new Vec3(0, y, -width),
                        new Vec3(0, y, width), new Vec3(side * width, y + lift, width),
                        layer == 0 ? f.primary : f.accent);
                m.rawLine(new Vec3(side * width, y + lift, -width), new Vec3(side * width, y + lift, width), f.highlight);
            }
        }
    }

    private static void pocketDimension(Mesh m, Frame f) {
        if (cue(f, "MARK")) {
            // World-sized doorway, independent of body-fitting and one-shot teleport animations.
            Vec3 left = f.right.scale(-0.8), right = f.right.scale(0.8), top = new Vec3(0, 2.2, 0);
            m.rawQuad(left, right, right.add(top), left.add(top), f.primary);
            m.rawLine(left, right, f.highlight); m.rawLine(left.add(top), right.add(top), f.highlight);
            m.rawLine(left, left.add(top), f.accent); m.rawLine(right, right.add(top), f.accent);
            for (int i = 0; i < 7; i++) {
                double x = ((i * 37) % 13 - 6) * 0.1, y = 0.2 + ((i * 17) % 11) * 0.16;
                Vec3 star = f.right.scale(x).add(0, y, 0).add(f.forward.scale(0.01));
                m.rawLine(star.subtract(f.right.scale(0.04)), star.add(f.right.scale(0.04)), f.highlight);
                m.rawLine(star.add(0, -0.04, 0), star.add(0, 0.04, 0), f.highlight);
            }
            if (f.link != null && f.link.y != 0) {
                Vec3 center = new Vec3(0, 1.1, 0).add(f.forward.scale(0.02));
                Vec3 tip = center.add(0, Math.signum(f.link.y) * 0.3, 0);
                m.rawLine(center.subtract(f.right.scale(0.27)), tip, f.highlight);
                m.rawLine(center.add(f.right.scale(0.27)), tip, f.highlight);
            } else if (f.link != null && f.link.x > 0) {
                // A gold doorway/arrow distinguishes leaving the realm from changing floors.
                Vec3 center = new Vec3(0, 1.1, 0).add(f.forward.scale(0.02));
                Vec3 a = center.subtract(f.right.scale(0.24)).add(0, -0.3, 0);
                Vec3 b = a.add(0, 0.6, 0), c = b.add(f.right.scale(0.36));
                m.rawLine(a, b, f.highlight); m.rawLine(b, c, f.highlight);
                m.rawLine(c, c.add(0, -0.6, 0), f.highlight);
                Vec3 tip = center.add(f.right.scale(0.38));
                m.rawLine(center, tip, f.highlight);
                m.rawLine(tip, tip.subtract(f.right.scale(0.13)).add(0, 0.13, 0), f.highlight);
                m.rawLine(tip, tip.subtract(f.right.scale(0.13)).add(0, -0.13, 0), f.highlight);
            }
            return;
        }
        if (cue(f, "CAST")) {
            double lid = 0.08 + smooth(f.progress) * 0.17;
            block(m, new Vec3(0.23, 0, 0.36), 0.16, 0.12, 0.12, f.primary, f.accent, f.highlight);
            m.quad(new Vec3(0.07, 0.13, 0.24), new Vec3(0.39, 0.13, 0.24),
                    new Vec3(0.39, 0.13 + lid, 0.4), new Vec3(0.07, 0.13 + lid, 0.4), f.highlight);
            return;
        }
        double gap = 0.04 + 0.18 * Math.sin(f.progress * Math.PI);
        for (int side : new int[]{-1, 1}) {
            Vec3 a = new Vec3(side * gap, 0.04, 0), b = new Vec3(side * gap, 1.08, 0);
            m.ribbon(a, b, 0.035, f.highlight);
            m.quad(a, a.add(side * 0.12, 0, 0.13), b.add(side * 0.12, 0, 0.13), b, f.primary);
        }
        m.line(new Vec3(-gap, 0.04, 0), new Vec3(gap, 0.04, 0), f.accent);
        m.line(new Vec3(-gap, 1.08, 0), new Vec3(gap, 1.08, 0), f.accent);
    }

    private static ResourceLocation id(String path) { return new ResourceLocation("divinity_engine", path); }
    private static boolean cue(Frame f, String value) { return f.cue.name().equals(value); }

    private static void rectangle(Mesh m, Vec3 center, double x, double y, int color) {
        Vec3 a = center.add(-x, -y, 0), b = center.add(x, -y, 0);
        Vec3 c = center.add(x, y, 0), d = center.add(-x, y, 0);
        m.line(a, b, color); m.line(b, c, color); m.line(c, d, color); m.line(d, a, color);
    }

    private static void rawPane(Mesh m, Vec3 center, Vec3 right, Vec3 up, double x, double y, int color) {
        Vec3 u = right.scale(x), v = up.scale(y);
        m.rawQuad(center.subtract(u).subtract(v), center.add(u).subtract(v),
                center.add(u).add(v), center.subtract(u).add(v), color);
    }

    private static void worldBlock(Mesh m, Frame f, Vec3 center, double x, double y, double z,
                                   int front, int side, int top) {
        worldQuad(m, f, center.add(-x, -y, -z), center.add(x, -y, -z),
                center.add(x, y, -z), center.add(-x, y, -z), front);
        worldQuad(m, f, center.add(x, -y, -z), center.add(x, -y, z),
                center.add(x, y, z), center.add(x, y, -z), side);
        worldQuad(m, f, center.add(-x, y, -z), center.add(x, y, -z),
                center.add(x, y, z), center.add(-x, y, z), top);
    }

    // Ground marks stay world-horizontal even when the owner looks straight up or down.
    private static void worldQuad(Mesh m, Frame f, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        m.rawQuad(a.scale(f.radius), b.scale(f.radius), c.scale(f.radius), d.scale(f.radius), color);
    }
}
