package net.nightshade.divinity_engine.client.effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static net.nightshade.divinity_engine.client.effects.BlessingEffectRenderer.*;

/** Seventeen mechanic-specific meshes; the other thirteen blessings deliberately have no mesh. */
@OnlyIn(Dist.CLIENT)
public final class EarthSpiritBlessingEffects {
    private static final double TAU = Math.PI * 2;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private EarthSpiritBlessingEffects() { }

    public static void register() {
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "stoneform"), EarthSpiritBlessingEffects::stoneform);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "echoing_strike"), EarthSpiritBlessingEffects::echoingStrike);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "earthbind_grip"), EarthSpiritBlessingEffects::earthbindGrip);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "trackers_mark"), EarthSpiritBlessingEffects::trackersMark);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "predators_focus"), EarthSpiritBlessingEffects::predatorsFocus);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "alphas_call"), EarthSpiritBlessingEffects::alphasCall);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "unyielding"), EarthSpiritBlessingEffects::unyielding);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "crushing_blow"), EarthSpiritBlessingEffects::crushingBlow);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "warlords_will"), EarthSpiritBlessingEffects::warlordsWill);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "fury_unbound"), EarthSpiritBlessingEffects::furyUnbound);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "dread_consumption"), EarthSpiritBlessingEffects::dreadConsumption);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "thornskin"), EarthSpiritBlessingEffects::thornskin);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "stonewardens_embrace"), EarthSpiritBlessingEffects::stonewardensEmbrace);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "rootgrip"), EarthSpiritBlessingEffects::rootgrip);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "wildheart_bloom"), EarthSpiritBlessingEffects::wildheartBloom);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "clockwork_punch"), EarthSpiritBlessingEffects::clockworkPunch);
        BlessingEffectRenderer.register(new ResourceLocation("divinity_engine", "precision_impact"), EarthSpiritBlessingEffects::precisionImpact);
    }

    private static void stoneform(Mesh m, Frame f) {
        double settle = .16 * (1 - smooth(f.progress));
        for (int row = 0; row < 3; row++) {
            for (int sign : new int[]{-1, 1}) {
                Vec3 p = new Vec3(sign * (.25 + settle), .38 - row * .30, .10 - row * .045);
                block(m, p, .14, .125, .07, f.primary, f.accent, f.highlight);
                m.line(p.add(-.09, .015, -.073), p.add(.015, -.04, -.073), f.highlight);
            }
        }
        block(m, new Vec3(0, .06, -.15), .13, .18, .06, f.accent, f.primary, f.highlight);
    }

    private static void echoingStrike(Mesh m, Frame f) {
        for (int echo = 0; echo < 3; echo++) {
            double extent = clamp(f.progress * 1.15 - echo * .18, .04, .88);
            for (int side = 0; side < 4; side++) {
                double a = side * Math.PI / 2;
                Vec3 center = xz(a, extent);
                Vec3 tangent = xz(a + Math.PI / 2, extent * .58);
                floorStroke(m, f, center.subtract(tangent).add(0, .025, 0), center.add(0, .045, 0),
                        .017, echo == 0 ? f.highlight : f.primary);
                floorStroke(m, f, center.add(0, .045, 0), center.add(tangent).add(0, .025, 0),
                        .017, f.accent);
            }
        }
        for (int i = 0; i < f.count(3, 6); i++) {
            Vec3 a = xz(i * 2.4, .09), b = xz(i * 2.4 + .13, .29);
            floorStroke(m, f, a.add(0, .012, 0), b.add(0, .016, 0), .009, f.highlight);
        }
    }

    private static void earthbindGrip(Mesh m, Frame f) {
        int claws = f.count(5, 8);
        double lift = .05 + .12 * Math.sin(Math.PI * f.progress);
        for (int i = 0; i < claws; i++) {
            double a = TAU * i / claws;
            Vec3 root = xz(a, .66), side = xz(a + Math.PI / 2, .055);
            Vec3 tip = xz(a, .49).add(0, lift, 0);
            floorQuad(m, f, root.subtract(side), root.add(side), tip.add(side.scale(.4)), tip, f.primary);
            floorQuad(m, f, root.add(side), root.add(side).add(0, .055, 0), tip, tip, f.accent);
            floorStroke(m, f, root, tip, .012, f.highlight);
            floorStroke(m, f, xz(a, .30).add(0, .01, 0), root, .013, f.primary);
        }
    }

    private static void trackersMark(Mesh m, Frame f) {
        double inset = .035 * Math.sin(f.phase);
        for (int sx : new int[]{-1, 1}) {
            for (int sy : new int[]{-1, 1}) {
                Vec3 corner = new Vec3(sx * (.55 - inset), sy * .68, -.03);
                m.ribbon(corner, corner.add(-sx * .22, 0, 0), .028, f.primary);
                m.ribbon(corner, corner.add(0, -sy * .21, 0), .028, f.highlight);
            }
        }
        // A short, private scan notch, never an owner-visible broadcast halo.
        double scan = -.45 + .9 * f.progress;
        m.line(new Vec3(-.52, scan, -.025), new Vec3(-.39, scan, -.025), f.accent);
        m.line(new Vec3(.39, scan, -.025), new Vec3(.52, scan, -.025), f.accent);
    }

    private static void predatorsFocus(Mesh m, Frame f) {
        double close = .12 + .28 * (1 - smooth(f.progress));
        for (int sign : new int[]{-1, 1}) {
            Vec3 point = new Vec3(sign * close, 0, .12);
            m.ribbon(point.add(sign * .19, .13, 0), point, .022, f.primary);
            m.ribbon(point, point.add(sign * .19, -.13, 0), .022, f.highlight);
            tile(m, point.add(sign * .24, 0, 0), .015, .045, f.accent);
        }
        m.line(new Vec3(0, -.065, .12), new Vec3(0, .065, .12), f.highlight);
    }

    private static void alphasCall(Mesh m, Frame f) {
        for (int front = 0; front < 2; front++) {
            double radius = clamp(.18 + f.progress * .69 - front * .18, .10, .86);
            double y = .13 + front * .16;
            for (int i = 0; i < f.count(8, 12); i++) {
                double a = TAU * i / f.count(8, 12);
                Vec3 p = xz(a, radius).add(0, y, 0);
                Vec3 q = xz(a + .32, radius).add(0, y, 0);
                floorQuad(m, f, p, q, q.add(0, .022, 0), p.add(0, .022, 0), f.primary);
                floorStroke(m, f, p.add(0, -.07, 0), p.add(0, .065, 0), .013, f.highlight);
            }
        }
    }

    private static void unyielding(Mesh m, Frame f) {
        double brace = f.cue == Cue.WARD ? .14 : .05;
        for (int sign : new int[]{-1, 1}) {
            Vec3 heel = new Vec3(sign * .27, .045, .16);
            Vec3 anchor = heel.add(sign * .15, 0, -.38 - brace * (1 - f.progress));
            floorQuad(m, f, heel.add(-.065, 0, 0), anchor.add(-.10, 0, 0),
                    anchor.add(.10, .02, 0), heel.add(.065, .27, 0), f.primary);
            floorStroke(m, f, heel.add(0, .22, 0), anchor, .025, f.highlight);
            floorStroke(m, f, anchor.add(-.12, 0, 0), anchor.add(.12, 0, 0), .02, f.accent);
        }
    }

    private static void crushingBlow(Mesh m, Frame f) {
        double squeeze = .14 + .36 * (1 - smooth(Math.min(1, f.progress * 1.8)));
        for (int sign : new int[]{-1, 1}) {
            block(m, new Vec3(sign * squeeze, 0, -.10), .09, .32, .10, f.primary, f.accent, f.highlight);
        }
        int fractures = f.count(5, 8);
        for (int i = 0; i < fractures; i++) {
            double a = TAU * i / fractures;
            Vec3 joint = xy(a + .16, .30 + .15 * f.progress).add(0, 0, -.23);
            m.ribbon(Vec3.ZERO.add(0, 0, -.23), joint, .025, f.highlight);
            m.line(joint, xy(a, .68).add(0, 0, -.17), f.accent);
        }
    }

    private static void warlordsWill(Mesh m, Frame f) {
        // Blindfold and descending lock bars mark the actual debuffed body, not a ten-block owner emblem.
        for (int sign : new int[]{-1, 1}) {
            Vec3 eye = new Vec3(sign * .19, .40, -.14);
            m.ribbon(eye.add(-.16, .055, 0), eye.add(.16, -.055, 0), .045, f.primary);
            m.ribbon(eye.add(-.16, -.055, 0), eye.add(.16, .055, 0), .018, f.highlight);
            Vec3 ankle = new Vec3(sign * .23, -.57, -.12);
            m.line(eye.add(0, -.10, 0), ankle, f.accent);
            for (int i = 0; i < 3; i++) {
                tile(m, ankle.add(0, i * .18 + .05 * (1 - f.progress), 0), .065, .025, f.primary);
            }
        }
    }

    private static void furyUnbound(Mesh m, Frame f) {
        double flare = .06 + .12 * Math.sin(Math.PI * f.progress);
        for (int sign : new int[]{-1, 1}) {
            Vec3 hand = new Vec3(sign * .38, -.20, .09);
            block(m, hand, .10, .16, .10, f.primary, f.accent, f.highlight);
            for (int finger = 0; finger < 3; finger++) {
                Vec3 base = hand.add((finger - 1) * .065, .17, -.04);
                m.triangle(base.add(-.026, 0, 0), base.add(.026, 0, 0), base.add(0, flare, .08), f.highlight);
            }
            block(m, new Vec3(sign * .23, .18, -.10), .13, .18, .045, f.accent, f.primary, f.highlight);
        }
    }

    private static void dreadConsumption(Mesh m, Frame f) {
        if (f.link == null) {
            for (int i = 0; i < 4; i++) {
                Vec3 corner = xy(i * Math.PI / 2, .38 * (1 - f.progress));
                m.triangle(corner, corner.add(.13, .08, .08), Vec3.ZERO, f.primary);
            }
            return;
        }
        Vec3 from = f.link.scale(-.5), axis = unit(f.link, UP);
        Vec3 side = unit(axis.cross(UP), f.right), lift = unit(axis.cross(side), f.up);
        double shrink = .23 * (1 - f.progress) + .025;
        Vec3 head = from.add(f.link.scale(f.progress));
        for (int i = 0; i < 4; i++) {
            Vec3 fold = side.scale(Math.cos(i * Math.PI / 2) * shrink)
                    .add(lift.scale(Math.sin(i * Math.PI / 2) * shrink));
            m.rawQuad(head.add(fold), head.add(fold.scale(.5)).add(axis.scale(.13)),
                    head.add(axis.scale(.24)), head, i % 2 == 0 ? f.primary : f.accent);
        }
        for (int strand = 0; strand < 3; strand++) {
            double a = Math.max(0, f.progress - .16 - strand * .035);
            Vec3 tail = from.add(f.link.scale(a)).add(lift.scale((strand - 1) * .065));
            worldStroke(m, tail, head, side, .018, f.highlight);
        }
    }

    private static void thornskin(Mesh m, Frame f) {
        if (f.link == null) {
            for (int i = 0; i < 5; i++) {
                double a = TAU * i / 5;
                Vec3 p = xy(a, .24), tip = xy(a, .55);
                m.triangle(p.add(xy(a + Math.PI / 2, .07)), p.subtract(xy(a + Math.PI / 2, .07)), tip, f.primary);
            }
            return;
        }
        Vec3 from = f.link.scale(-.5), axis = unit(f.link, UP);
        Vec3 side = unit(axis.cross(UP), f.right);
        for (int i = 0; i < 4; i++) {
            double progress = clamp(f.progress - i * .12, 0, 1);
            Vec3 tip = from.add(f.link.scale(progress));
            Vec3 root = tip.subtract(axis.scale(.23));
            m.rawQuad(root.subtract(side.scale(.065)), root.add(side.scale(.065)), tip, tip, f.primary);
            m.rawLine(root, tip, f.highlight);
        }
    }

    private static void stonewardensEmbrace(Mesh m, Frame f) {
        for (int sign : new int[]{-1, 1}) {
            Vec3 foot = new Vec3(sign * .23, .025, 0);
            floorQuad(m, f, foot.add(-.17, 0, -.26), foot.add(.17, 0, -.26),
                    foot.add(.13, 0, .24), foot.add(-.13, 0, .24), f.primary);
            for (int side : new int[]{-1, 1}) {
                Vec3 ground = foot.add(side * .18, 0, .07);
                Vec3 cuff = foot.add(side * .10, .22, -.05);
                floorQuad(m, f, ground.add(0, 0, -.13), ground.add(0, 0, .13),
                        cuff.add(0, 0, .11), cuff.add(0, 0, -.11), f.accent);
                floorStroke(m, f, ground, cuff, .024, f.highlight);
            }
        }
    }

    private static void rootgrip(Mesh m, Frame f) {
        int roots = f.count(4, 6);
        for (int root = 0; root < roots; root++) {
            double start = TAU * root / roots;
            Vec3 previous = xz(start, .65).add(0, .025, 0);
            for (int step = 1; step <= 5; step++) {
                double t = step / 5.0;
                Vec3 next = xz(start + t * 1.2 * smooth(f.progress), .65 - .40 * t)
                        .add(0, .03 + t * .43 * smooth(f.progress), 0);
                floorStroke(m, f, previous, next, .028 * (1 - t * .45), f.primary);
                if (step == 3) {
                    floorStroke(m, f, next, next.add(xz(start + .7, .15)).add(0, .09, 0), .015, f.accent);
                }
                previous = next;
            }
        }
    }

    private static void wildheartBloom(Mesh m, Frame f) {
        if (f.link != null) {
            Vec3 from = f.link.scale(-.5), axis = unit(f.link, UP);
            Vec3 side = unit(axis.cross(UP), f.right);
            for (int i = 0; i < 5; i++) {
                double t = clamp(f.progress - i * .08, 0, 1);
                Vec3 p = from.add(f.link.scale(t));
                double direction = i % 2 == 0 ? 1 : -1;
                m.rawQuad(p, p.add(side.scale(.09 * direction)).subtract(axis.scale(.07)),
                        p.subtract(axis.scale(.17)), p.subtract(axis.scale(.11)).subtract(side.scale(.025 * direction)), f.primary);
            }
        } else if (f.cue == Cue.HEAL) {
            Vec3 center = new Vec3(0, .12 * f.progress, 0);
            m.ribbon(center.add(0, -.33, 0), center, .018, f.accent);
            for (int i = 0; i < 5; i++) leaf(m, center, TAU * i / 5, .09, i % 2 == 0 ? f.primary : f.highlight);
        } else {
            for (int i = 0; i < f.count(5, 8); i++) {
                double a = i * 2.4;
                Vec3 p = xz(a, .18 + i * .077).add(0, .012, 0);
                for (int petal = 0; petal < 4; petal++) {
                    Vec3 tip = p.add(xz(petal * Math.PI / 2, .047));
                    Vec3 side = xz(petal * Math.PI / 2 + Math.PI / 2, .018);
                    floorQuad(m, f, p, tip.add(side), tip.add(0, .015, 0), tip.subtract(side),
                            petal % 2 == 0 ? f.primary : f.highlight);
                }
                floorStroke(m, f, p, p.add(xz(a + .8, .075)), .008, f.accent);
            }
        }
    }

    private static void clockworkPunch(Mesh m, Frame f) {
        double stroke = -.30 + .52 * Math.sin(Math.PI * Math.min(1, f.progress * 1.3));
        block(m, new Vec3(0, 0, -.52), .19, .19, .07, f.primary, f.accent, f.highlight);
        block(m, new Vec3(0, 0, (stroke - .45) / 2), .055, .055, (stroke + .52) / 2,
                f.highlight, f.primary, f.accent);
        block(m, new Vec3(0, 0, stroke), .24, .24, .045, f.accent, f.primary, f.highlight);
        for (int sign : new int[]{-1, 1}) {
            m.line(new Vec3(sign * .29, -.14, -.48), new Vec3(sign * .29, -.14, stroke), f.highlight);
        }
    }

    private static void precisionImpact(Mesh m, Frame f) {
        for (int slot = 0; slot < 3; slot++) {
            double y = -.24 + slot * .22;
            m.ribbon(new Vec3(-.42, y, -.04), new Vec3(.42, y, -.04), .035, f.primary);
            for (int tooth = 0; tooth < 5; tooth++) {
                tile(m, new Vec3(-.32 + tooth * .16, y + .055, -.035), .027, .035, f.accent);
            }
            double travel = Math.floor(clamp(f.progress + slot * .09, 0, .999) * 5) / 4.0;
            block(m, new Vec3(-.32 + .64 * travel, y + .035, -.10), .06, .065, .025,
                    f.highlight, f.accent, f.primary);
        }
    }

    /** Ground mechanics stay world-up even while the caster looks vertically. */
    private static Vec3 floorPoint(Frame f, Vec3 p) {
        Vec3 ahead = unit(new Vec3(f.forward.x, 0, f.forward.z), new Vec3(0, 0, 1));
        Vec3 right = UP.cross(ahead);
        return right.scale(p.x).add(0, p.y, 0).add(ahead.scale(p.z)).scale(f.radius);
    }

    private static void floorQuad(Mesh m, Frame f, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        m.rawQuad(floorPoint(f, a), floorPoint(f, b), floorPoint(f, c), floorPoint(f, d), color);
    }

    private static void floorStroke(Mesh m, Frame f, Vec3 a, Vec3 b, double width, int color) {
        Vec3 from = floorPoint(f, a), to = floorPoint(f, b);
        Vec3 side = unit(to.subtract(from).cross(UP), f.right);
        worldStroke(m, from, to, side, width * f.radius, color);
    }

    private static void worldStroke(Mesh m, Vec3 from, Vec3 to, Vec3 side, double width, int color) {
        Vec3 edge = side.scale(width);
        m.rawQuad(from.subtract(edge), from.add(edge), to.add(edge), to.subtract(edge), color);
    }
}
