package net.nightshade.divinity_engine.util.divinity.effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.nightshade.renderlib.api.RenderPalette;
import net.nightshade.renderlib.particle.ParticleCollision;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;
import net.nightshade.renderlib.particle.ParticleMotion;

import java.util.List;
import java.util.Optional;

import static net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

/** Vanilla-sprite, mechanic-specific Nightshade emitters; no built-in blessing requests a mesh. */
public final class EarthSpiritBlessingDesigns {
    private static final ResourceLocation GRIT = texture("generic_0");
    private static final ResourceLocation DUST = texture("generic_2");
    private static final ResourceLocation SMOKE = texture("generic_5");
    private static final ResourceLocation CRIT = texture("critical_hit");
    private static final ResourceLocation EMBER = texture("flame");
    private static final ResourceLocation SOUL = texture("sculk_soul_3");
    private static final ResourceLocation SOUL_FADE = texture("sculk_soul_8");
    private static final ResourceLocation LEAF = texture("cherry_1");
    private static final ResourceLocation PETAL = texture("cherry_6");
    private static final ResourceLocation GROWTH = texture("glint");
    private static final ResourceLocation HEART = texture("heart");
    private static final ResourceLocation SPARK = texture("spark_0");
    private static final ResourceLocation BUFF = texture("spell_0");
    private static final RenderPalette SOIL = new RenderPalette(List.of(0xFF806247, 0xFFB99A70), 40);
    private static final RenderPalette STONE = new RenderPalette(List.of(0xFF888780, 0xFFB2ACA0), 40);
    private static final RenderPalette ASH = new RenderPalette(List.of(0xFF77736E, 0xFFAAA49A), 40);
    private static final ResourceLocation BOX = new ResourceLocation("nightshade_core", "box");
    // Visual-only contact: debris stops at blocks, never queries or affects entities.
    private static final ParticleCollision SETTLE = new ParticleCollision(
            ParticleCollision.WorldResponse.STOP, ParticleCollision.EntityResponse.IGNORE,
            .025, 0, .8, Optional.empty(), Optional.empty(), false);

    private EarthSpiritBlessingDesigns() { }

    public static void register() {
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "hammerhand"),
                BlessingVisualDesign.builder().duration(8).particles(9, .085, EarthSpiritBlessingDesigns::hammerhand).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "stoneform"),
                BlessingVisualDesign.builder().duration(20).particles(7, .09, EarthSpiritBlessingDesigns::stoneform).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "echoing_strike"),
                BlessingVisualDesign.builder().duration(14).particles(12, .08, EarthSpiritBlessingDesigns::echoingStrike).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "earthbind_grip"),
                BlessingVisualDesign.builder().duration(18).particles(14, .10, EarthSpiritBlessingDesigns::earthbindGrip).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "forgeheart"),
                BlessingVisualDesign.builder().duration(16).particles(6, .075, EarthSpiritBlessingDesigns::forgeheart).build());

        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "feral_step"),
                BlessingVisualDesign.builder().duration(12).particles(8, .07, EarthSpiritBlessingDesigns::feralStep).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "trackers_mark"),
                BlessingVisualDesign.builder().duration(24).particles(4, .045, EarthSpiritBlessingDesigns::trackersMark).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "predators_focus"),
                BlessingVisualDesign.builder().duration(12).particles(2, .035, EarthSpiritBlessingDesigns::predatorsFocus).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "savage_hunger"),
                BlessingVisualDesign.builder().duration(16).particles(5, .065, EarthSpiritBlessingDesigns::savageHunger).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "alphas_call"),
                BlessingVisualDesign.builder().duration(16).particles(12, .065, EarthSpiritBlessingDesigns::alphasCall).build());

        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "battle_rush"),
                BlessingVisualDesign.builder().duration(16).particles(9, .075, EarthSpiritBlessingDesigns::battleRush).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "unyielding"),
                BlessingVisualDesign.builder().duration(16).particles(6, .07, EarthSpiritBlessingDesigns::unyielding).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "crushing_blow"),
                BlessingVisualDesign.builder().duration(18).particles(14, .10, EarthSpiritBlessingDesigns::crushingBlow).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "warlords_will"),
                BlessingVisualDesign.builder().duration(20).particles(8, .07, EarthSpiritBlessingDesigns::warlordsWill).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "fury_unbound"),
                BlessingVisualDesign.builder().duration(20).particles(10, .09, EarthSpiritBlessingDesigns::furyUnbound).build());

        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "soul_leech"),
                BlessingVisualDesign.builder().duration(16).particles(8, .075, EarthSpiritBlessingDesigns::soulLeech).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "wraithwalk"),
                BlessingVisualDesign.builder().duration(8).particles(7, .09, EarthSpiritBlessingDesigns::wraithwalk).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "ghastly_aura"),
                BlessingVisualDesign.builder().duration(16).particles(14, .14, EarthSpiritBlessingDesigns::ghastlyAura).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "dread_consumption"),
                BlessingVisualDesign.builder().duration(18).particles(9, .085, EarthSpiritBlessingDesigns::dreadConsumption).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "witherbound"),
                BlessingVisualDesign.builder().duration(18).particles(11, .095, EarthSpiritBlessingDesigns::witherbound).build());

        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "verdant_touch"),
                BlessingVisualDesign.builder().duration(14).particles(7, .065, EarthSpiritBlessingDesigns::verdantTouch).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "thornskin"),
                BlessingVisualDesign.builder().duration(10).particles(8, .075, EarthSpiritBlessingDesigns::thornskin).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "stonewardens_embrace"),
                BlessingVisualDesign.builder().duration(20).particles(5, .08, EarthSpiritBlessingDesigns::stonewardensEmbrace).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "rootgrip"),
                BlessingVisualDesign.builder().duration(20).particles(8, .085, EarthSpiritBlessingDesigns::rootgrip).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "wildheart_bloom"),
                BlessingVisualDesign.builder().duration(20).particles(12, .075, EarthSpiritBlessingDesigns::wildheartBloom).build());

        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "tinkers_grace"),
                BlessingVisualDesign.builder().duration(8).particles(4, .045, EarthSpiritBlessingDesigns::tinkersGrace).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "auto_smelter"),
                BlessingVisualDesign.builder().duration(14).particles(10, .08, EarthSpiritBlessingDesigns::autoSmelter).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "clockwork_punch"),
                BlessingVisualDesign.builder().duration(10).particles(7, .06, EarthSpiritBlessingDesigns::clockworkPunch).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "precision_impact"),
                BlessingVisualDesign.builder().duration(20).particles(6, .05, EarthSpiritBlessingDesigns::precisionImpact).build());
        BlessingVisualDesigns.register(new ResourceLocation("divinity_engine", "overdrive_loop"),
                BlessingVisualDesign.builder().duration(12).particles(6, .055, EarthSpiritBlessingDesigns::overdriveLoop).build());
    }

    private static void hammerhand(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        box(p, c, .09, .12, .09);
        motion(p, new Vec3(0, .055, 0), new Vec3(.065, .045, .065), .022, .90);
        p.texture(CRIT).collision(SETTLE);
    }

    private static void stoneform(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        box(p, c, .32, .48, .24);
        motion(p, new Vec3(0, -.018, 0), new Vec3(.012, .012, .012), .009, .92);
        p.texture(GRIT).palette(STONE).collision(SETTLE);
    }

    private static void echoingStrike(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.cylinder(c.radius() * .75, .025, 0, c.lifetime());
        motion(p, new Vec3(0, .10, 0), new Vec3(.045, .055, .045), .020, .92);
        p.texture(DUST).palette(SOIL).collision(SETTLE);
    }

    private static void earthbindGrip(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.cylinder(c.radius() * .8, .02, 0, c.lifetime());
        motion(p, new Vec3(0, .17, 0), new Vec3(.009, .06, .009), .024, .95);
        p.texture(GRIT).palette(SOIL).collision(SETTLE);
    }

    private static void forgeheart(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        if (c.cue() == Cue.IMPACT) {
            p.point(0, c.lifetime());
            motion(p, new Vec3(0, .035, 0), new Vec3(.045, .04, .045), .016, .92);
            p.texture(SPARK).collision(SETTLE);
        } else {
            box(p, c, .10, .15, .08);
            motion(p, new Vec3(0, .022, 0), new Vec3(.005, .009, .005), 0, .96);
            p.texture(EMBER).palette(RenderPalette.WHITE);
        }
    }

    private static void feralStep(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        boolean landing = c.cue() == Cue.WARD;
        p.cylinder(landing ? .24 : .16, .02, 0, c.lifetime());
        motion(p, new Vec3(0, landing ? .07 : .045, 0),
                landing ? new Vec3(.09, .025, .09) : new Vec3(.018, .014, .018), .014, .90);
        p.texture(DUST).palette(SOIL).collision(SETTLE);
    }

    private static void trackersMark(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        // Owner-only transport remains parent-owned; no broadcast halo or secondary emitter.
        box(p, c, c.radius() * .50, c.radius() * .75, .035);
        motion(p, new Vec3(0, -.003, 0), Vec3.ZERO, 0, 1);
        p.texture(GROWTH).palette(RenderPalette.WHITE);
    }

    private static void predatorsFocus(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.line(.18, c.forward(), 0, c.lifetime());
        motion(p, c.forward().scale(.026), new Vec3(.002, .002, .002), 0, .93);
        p.texture(GROWTH).palette(RenderPalette.WHITE);
    }

    private static void savageHunger(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        if (c.cue() == Cue.HEAL) {
            box(p, c, .22, .24, .18);
            motion(p, new Vec3(0, .034, 0), new Vec3(.008, .012, .008), 0, .95);
            p.texture(HEART).palette(RenderPalette.WHITE);
        } else {
            box(p, c, .24, .025, .24);
            motion(p, new Vec3(0, .10, 0), new Vec3(.025, .025, .025), .015, .94);
            p.texture(GRIT).palette(SOIL).collision(SETTLE);
        }
    }

    private static void alphasCall(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.cylinder(c.radius() * .65, .04, 0, c.lifetime());
        motion(p, new Vec3(0, .11, 0), new Vec3(.11, .018, .11), .014, .88);
        p.texture(DUST).palette(SOIL).collision(SETTLE);
    }

    private static void battleRush(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        boolean ward = c.cue() == Cue.WARD;
        box(p, c, .30, ward ? .36 : .025, .18);
        motion(p, ward ? new Vec3(0, -.025, 0) : new Vec3(0, .065, 0),
                new Vec3(.03, .02, .03), ward ? .008 : .014, .91);
        p.texture(GRIT).palette(ward ? STONE : SOIL).collision(SETTLE);
    }

    private static void unyielding(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        boolean ward = c.cue() == Cue.WARD;
        box(p, c, .28, .015, .12);
        motion(p, new Vec3(0, ward ? .055 : .018, 0),
                new Vec3(ward ? .035 : .008, .008, ward ? .035 : .008), .012, .86);
        p.texture(GRIT).palette(STONE).collision(SETTLE);
    }

    private static void crushingBlow(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.point(0, c.lifetime());
        motion(p, new Vec3(0, .07, 0), new Vec3(.10, .09, .10), .025, .91);
        p.texture(CRIT).collision(SETTLE);
    }

    private static void warlordsWill(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        box(p, c, .24, .12, .24);
        motion(p, new Vec3(0, -.012, 0), new Vec3(.006, .004, .006), 0, .95);
        p.texture(SMOKE).palette(ASH);
    }

    private static void furyUnbound(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        box(p, c, .40, .26, .18);
        motion(p, new Vec3(0, .025, 0), new Vec3(.012, .008, .012), 0, .94);
        p.texture(BUFF);
    }

    private static void soulLeech(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        if (c.link() != null) {
            travel(p, c, .006);
            p.texture(SOUL).palette(RenderPalette.WHITE);
        } else {
            box(p, c, .20, .045, .20);
            motion(p, new Vec3(0, .018, 0), new Vec3(.01, .012, .01), 0, .96);
            p.texture(SOUL_FADE).palette(RenderPalette.WHITE);
        }
    }

    private static void wraithwalk(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        box(p, c, .24, .48, .22);
        motion(p, new Vec3(0, .035, 0), new Vec3(.065, .02, .065), 0, .86);
        p.texture(SMOKE).palette(ASH);
    }

    private static void ghastlyAura(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.cylinder(c.radius() * .8, .12, 0, c.lifetime());
        motion(p, new Vec3(0, .009, 0), new Vec3(.014, .004, .014), 0, .95);
        p.texture(SOUL_FADE).palette(RenderPalette.WHITE);
    }

    private static void dreadConsumption(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        travel(p, c, .012);
        p.texture(SOUL_FADE).palette(RenderPalette.WHITE);
    }

    private static void witherbound(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        box(p, c, .30, .56, .30);
        motion(p, new Vec3(0, -.018, 0), new Vec3(.012, .008, .012), .004, .94);
        p.texture(GRIT).palette(ASH).collision(SETTLE);
    }

    private static void verdantTouch(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.cylinder(.19, .22, 0, c.lifetime());
        motion(p, new Vec3(0, .028, 0), new Vec3(.015, .012, .015), .001, .93);
        p.texture(GROWTH).palette(RenderPalette.WHITE);
    }

    private static void thornskin(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        if (c.link() != null) travel(p, c, .010);
        else {
            box(p, c, .24, .20, .24);
            motion(p, c.forward().scale(.08), new Vec3(.035, .025, .035), .010, .94);
            p.collision(SETTLE);
        }
        p.texture(LEAF).palette(RenderPalette.WHITE);
    }

    private static void stonewardensEmbrace(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.cylinder(.33, .025, 0, c.lifetime());
        motion(p, new Vec3(0, .012, 0), new Vec3(.004, .005, .004), .006, .88);
        p.texture(DUST).palette(STONE).collision(SETTLE);
    }

    private static void rootgrip(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.cylinder(.32, .03, 0, c.lifetime());
        motion(p, new Vec3(0, .085, 0), new Vec3(.008, .022, .008), .012, .95);
        p.texture(GRIT).palette(SOIL).collision(SETTLE);
    }

    private static void wildheartBloom(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.palette(RenderPalette.WHITE);
        if (c.link() != null) {
            travel(p, c, .004);
            p.texture(LEAF);
        } else if (c.cue() == Cue.HEAL) {
            p.cylinder(.26, .35, 0, c.lifetime());
            motion(p, new Vec3(0, .026, 0), new Vec3(.008, .012, .008), .001, .96);
            p.texture(HEART);
        } else {
            p.cylinder(c.radius() * .78, .04, 0, c.lifetime());
            p.motion(new ParticleMotion(new Vec3(.008, .035, 0), new Vec3(.01, .012, .01),
                    Vec3.ZERO, .003, .95, 0, .08));
            p.texture(PETAL).collision(SETTLE);
        }
    }

    private static void tinkersGrace(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        p.point(0, c.lifetime());
        motion(p, c.forward().scale(.05).add(0, .025, 0), new Vec3(.025, .02, .025), .018, .88);
        p.texture(SPARK).collision(SETTLE);
    }

    private static void autoSmelter(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        box(p, c, .26, .16, .26);
        motion(p, new Vec3(0, .065, 0), new Vec3(.025, .025, .025), 0, .93);
        p.texture(EMBER).palette(RenderPalette.WHITE);
    }

    private static void clockworkPunch(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        box(p, c, .08, .08, .08);
        motion(p, new Vec3(0, .085, 0), new Vec3(.04, .025, .04), .012, .91);
        p.texture(SPARK).collision(SETTLE);
    }

    private static void precisionImpact(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        box(p, c, .16, .06, .16);
        motion(p, new Vec3(0, .045, 0), new Vec3(.006, .012, .006), 0, .97);
        p.texture(SMOKE).palette(ASH);
    }

    private static void overdriveLoop(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c) {
        boolean trail = c.cue() == Cue.TRAIL;
        box(p, c, .26, trail ? .025 : .12, .10);
        motion(p, new Vec3(0, trail ? .04 : .022, 0),
                new Vec3(.012, .009, .012), trail ? .010 : 0, .92);
        p.texture(trail ? GRIT : SMOKE).palette(trail ? SOIL : ASH);
        if (trail) p.collision(SETTLE);
    }

    private static void box(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c,
                            double x, double y, double z) {
        CompoundTag data = new CompoundTag();
        data.putDouble("x", x);
        data.putDouble("y", y);
        data.putDouble("z", z);
        data.putDouble("radius", Math.sqrt(x * x + y * y + z * z));
        p.shape(BOX, data, 0, c.lifetime());
    }

    private static void travel(ParticleEmitterSpec.Builder p, BlessingVisualDesign.Context c, double spread) {
        Vec3 segment = c.link() == null ? Vec3.ZERO : c.link();
        double length = segment.length();
        Vec3 direction = length > 1.0E-6 ? segment.scale(1 / length) : new Vec3(0, 1, 0);
        // Linked emitters start at the donor, not the midpoint. No drag or orbit can bend the transfer away.
        p.line(Math.min(.12, length), direction, 0, c.lifetime());
        motion(p, segment.scale(1.0 / Math.max(1, c.lifetime())),
                new Vec3(spread, spread, spread), 0, 1);
    }

    private static void motion(ParticleEmitterSpec.Builder p, Vec3 velocity, Vec3 spread, double gravity, double drag) {
        p.motion(new ParticleMotion(velocity, spread, Vec3.ZERO, gravity, drag, 0, 0));
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("minecraft", "textures/particle/" + name + ".png");
    }
}
