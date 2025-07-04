package net.nightshade.divinity_engine.registry.divinity.blessing;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.core.mixin.RegistryBuilderAccessor;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.aethon.Veilpiercer;
import net.nightshade.divinity_engine.divinity.blessing.aethon.CelestialWard;
import net.nightshade.divinity_engine.divinity.blessing.aethon.StarlitMirror;
import net.nightshade.divinity_engine.divinity.blessing.cryonis.FrozenHeart;
import net.nightshade.divinity_engine.divinity.blessing.cryonis.IcyPulse;
import net.nightshade.divinity_engine.divinity.blessing.cryonis.Snowveil;
import net.nightshade.divinity_engine.divinity.blessing.dravak.BattleRush;
import net.nightshade.divinity_engine.divinity.blessing.dravak.CrushingBlow;
import net.nightshade.divinity_engine.divinity.blessing.dravak.Unyielding;
import net.nightshade.divinity_engine.divinity.blessing.elaris.InsightfulKill;
import net.nightshade.divinity_engine.divinity.blessing.elaris.Foresight;
import net.nightshade.divinity_engine.divinity.blessing.elaris.ScholarsEdge;
import net.nightshade.divinity_engine.divinity.blessing.grond.EchoingStrike;
import net.nightshade.divinity_engine.divinity.blessing.grond.Hammerhand;
import net.nightshade.divinity_engine.divinity.blessing.grond.Stoneform;
import net.nightshade.divinity_engine.divinity.blessing.ignar.BlazingHands;
import net.nightshade.divinity_engine.divinity.blessing.ignar.Fireproof;
import net.nightshade.divinity_engine.divinity.blessing.ignar.FlashFlare;
import net.nightshade.divinity_engine.divinity.blessing.kairoth.EchoStep;
import net.nightshade.divinity_engine.divinity.blessing.kairoth.PredictedPath;
import net.nightshade.divinity_engine.divinity.blessing.kairoth.SecondChance;
import net.nightshade.divinity_engine.divinity.blessing.lumen.BeaconsGrace;
import net.nightshade.divinity_engine.divinity.blessing.lumen.CleansingFlame;
import net.nightshade.divinity_engine.divinity.blessing.lumen.DivineRadiance;
import net.nightshade.divinity_engine.divinity.blessing.mechanos.AutoSmelter;
import net.nightshade.divinity_engine.divinity.blessing.mechanos.ClockworkPunch;
import net.nightshade.divinity_engine.divinity.blessing.mechanos.TinkersGrace;
import net.nightshade.divinity_engine.divinity.blessing.nashara.SerpentsBite;
import net.nightshade.divinity_engine.divinity.blessing.nashara.ToxicSkin;
import net.nightshade.divinity_engine.divinity.blessing.nashara.Shadowstep;
import net.nightshade.divinity_engine.divinity.blessing.nerai.AquaticGrace;
import net.nightshade.divinity_engine.divinity.blessing.nerai.RainsFavor;
import net.nightshade.divinity_engine.divinity.blessing.nerai.Wavebound;
import net.nightshade.divinity_engine.divinity.blessing.nythea.LullOfTheLost;
import net.nightshade.divinity_engine.divinity.blessing.nythea.PhaseBlink;
import net.nightshade.divinity_engine.divinity.blessing.nythea.TwilightVeil;
import net.nightshade.divinity_engine.divinity.blessing.solarius.RadiantStrike;
import net.nightshade.divinity_engine.divinity.blessing.solarius.Sunburst;
import net.nightshade.divinity_engine.divinity.blessing.solarius.Sunskin;
import net.nightshade.divinity_engine.divinity.blessing.terra.StonewardensEmbrace;
import net.nightshade.divinity_engine.divinity.blessing.terra.Thornskin;
import net.nightshade.divinity_engine.divinity.blessing.terra.VerdantTouch;
import net.nightshade.divinity_engine.divinity.blessing.varun.FeralStep;
import net.nightshade.divinity_engine.divinity.blessing.varun.PredatorsFocus;
import net.nightshade.divinity_engine.divinity.blessing.varun.TrackersMark;
import net.nightshade.divinity_engine.divinity.blessing.vokar.GhastlyAura;
import net.nightshade.divinity_engine.divinity.blessing.vokar.SoulLeech;
import net.nightshade.divinity_engine.divinity.blessing.vokar.Wraithwalk;
import net.nightshade.divinity_engine.divinity.blessing.voltira.Shockshield;
import net.nightshade.divinity_engine.divinity.blessing.voltira.Stormstep;
import net.nightshade.divinity_engine.divinity.blessing.voltira.TempestWrath;
import net.nightshade.divinity_engine.divinity.blessing.zephra.CycloneLeap;
import net.nightshade.divinity_engine.divinity.blessing.zephra.GaleStep;
import net.nightshade.divinity_engine.divinity.blessing.zephra.WindcallersReflex;

import java.awt.*;
import java.util.function.Supplier;

/**
 * Registry for all blessings in the mod
 * Manages registration of blessing types and their properties
 */
public class BlessingsRegistry {
    // Registry key for blessings
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation(DivinityEngineMod.MODID, "blessings");

    // Deferred register for blessings
    private static final DeferredRegister<Blessings> registry = DeferredRegister.create(REGISTRY_KEY, DivinityEngineMod.MODID);

    // Registry supplier for blessings
    public static final Supplier<IForgeRegistry<Blessings>> BLESSINGS_REGISTRY = registry.makeRegistry(() -> {
        RegistryBuilder<Blessings> builder = new RegistryBuilder();
        ((RegistryBuilderAccessor) builder).setHasWrapper(true);
        return builder;
    });

    // Solarius Curse
    public static final RegistryObject<Blessings> RADIANT_STRIKE = registry.register("radiant_strike",
            () -> new RadiantStrike(25, 0, true,false, Color.red) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SUNSKIN = registry.register("sunskin",
            () -> new Sunskin(50, 0, true,false, Color.red) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SUNBURST = registry.register("sunburst",
            () -> new Sunburst(80, 45, false,false, Color.red) // neededFavor: 80, cooldown: 45 in second
    );

    // Nashara Curse
    public static final RegistryObject<Blessings> TOXIC_SKIN = registry.register("toxic_skin",
            () -> new ToxicSkin(50, 0, true,false, Color.green) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SERPENTS_BITE = registry.register("serpents_bite",
            () -> new SerpentsBite(25, 0, true,false, Color.green) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SHADOWSTEP = registry.register("shadowstep",
            () -> new Shadowstep(80, 25, true,false, Color.darkGray) // neededFavor: 80, cooldown: 25 in second
    );

    // Grond Curse
    public static final RegistryObject<Blessings> HAMMERHAND = registry.register("hammerhand",
            () -> new Hammerhand(25, 0, true,false, Color.orange) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> STONEFORM = registry.register("stoneform",
            () -> new Stoneform(50, 25, true,false, Color.gray) // neededFavor: 50, cooldown: 25 in second
    );
    public static final RegistryObject<Blessings> ECHOING_STRIKE = registry.register("echoing_strike",
            () -> new EchoingStrike(50, 45, true,false, Color.gray) // neededFavor: 50, cooldown: 45 in second
    );

    // Varun Curse
    public static final RegistryObject<Blessings> FERAL_STEP = registry.register("feral_step",
            () -> new FeralStep(25, 0, true,false, Color.green) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> TRACKERS_MARK = registry.register("trackers_mark",
            () -> new TrackersMark(50, 0, true,false, Color.green) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> PREDATORS_FOCUS = registry.register("predators_focus",
            () -> new PredatorsFocus(80, 45, true,false, Color.green) // neededFavor: 80, cooldown: 45 in second
    );

    // Dravak Curse
    public static final RegistryObject<Blessings> BATTLE_RUSH = registry.register("battle_rush",
            () -> new BattleRush(25, 10, true,false, new Color(235, 70, 52)) // neededFavor: 25, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> UNYIELDING = registry.register("unyielding",
            () -> new Unyielding(50, 0, true,false, new Color(235, 70, 52)) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> CRUSHING_BLOW = registry.register("crushing_blow",
            () -> new CrushingBlow(80, 30, true,false, new Color(235, 70, 52)) // neededFavor: 80, cooldown: 30 in second
    );

    // Elaris Curse
    public static final RegistryObject<Blessings> INSIGHTFUL_KILL = registry.register("insightful_kill",
            () -> new InsightfulKill(25, 10, true,false, Color.white) // neededFavor: 25, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> FORESIGHT = registry.register("foresight",
            () -> new Foresight(50, 20, true,false, Color.white) // neededFavor: 50, cooldown: 20 in second
    );
    public static final RegistryObject<Blessings> SCHOLARS_EDGE = registry.register("scholars_edge",
            () -> new ScholarsEdge(80, 0, true,false, Color.white) // neededFavor: 80, cooldown: 0 in second
    );

    // Voltira Curse
    public static final RegistryObject<Blessings> STORMSTEP = registry.register("stormstep",
            () -> new Stormstep(25, 15, false, false, new Color(0, 255, 251)) // neededFavor: 25, cooldown: 15 in second
    );
    public static final RegistryObject<Blessings> SHOCKSHIELD = registry.register("shockshield",
            () -> new Shockshield(50, 5, true,false, new Color(0, 255, 251)) // neededFavor: 50, cooldown: 5 in second
    );
    public static final RegistryObject<Blessings> TEMPEST_WRATH = registry.register("tempest_wrath",
            () -> new TempestWrath(80, 30, true,false, new Color(0, 255, 251)) // neededFavor: 80, cooldown: 30 in second
    );

    // Vokar Curse
    public static final RegistryObject<Blessings> SOUL_LEECH = registry.register("soul_leech",
            () -> new SoulLeech(25, 0, true,false, new Color(50, 18, 82)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> WRAITHWALK = registry.register("wraithwalk",
            () -> new Wraithwalk(50, 15, true,false, new Color(50, 18, 82)) // neededFavor: 50, cooldown: 15 in second
    );
    public static final RegistryObject<Blessings> GHASTLY_AURA = registry.register("ghastly_aura",
            () -> new GhastlyAura(80, 0, true,false, new Color(50, 18, 82)) // neededFavor: 80, cooldown: 0 in second
    );

    // Nythea Curse
    public static final RegistryObject<Blessings> TWILIGHT_VEIL = registry.register("twilight_veil",
            () -> new TwilightVeil(25, 0, true,false, new Color(163, 231, 240)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> LULL_OF_THE_LOST = registry.register("lull_of_the_lost",
            () -> new LullOfTheLost(50, 0, true,false, new Color(163, 231, 240)) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> PHASE_BLINK = registry.register("phase_blink",
            () -> new PhaseBlink(80, 10, false,false, new Color(163, 231, 240)) // neededFavor: 80, cooldown: 10 in second
    );

    // Kairoth Curse
    public static final RegistryObject<Blessings> SECOND_CHANCE = registry.register("second_chance",
            () -> new SecondChance(25, 10800, true,false, new Color(248, 250, 152)) // neededFavor: 25, cooldown: 10800 in second
    );
    public static final RegistryObject<Blessings> ECHO_STEP = registry.register("echo_step",
            () -> new EchoStep(50, 30, false,false, new Color(248, 250, 152)) // neededFavor: 50, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> PREDICTED_PATH = registry.register("predicted_path",
            () -> new PredictedPath(80, 15, true,false, new Color(248, 250, 152)) // neededFavor: 80, cooldown: 15 in second
    );

    // Cryonis Curse
    public static final RegistryObject<Blessings> FROZEN_HEART = registry.register("frozen_heart",
            () -> new FrozenHeart(25, 0,true ,false, new Color(152, 234, 250)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> ICY_PULSE = registry.register("icy_pulse",
            () -> new IcyPulse(50, 0, true,false, new Color(152, 234, 250)) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SNOWVEIL = registry.register("snowveil",
            () -> new Snowveil(80, 0, true,false, new Color(152, 234, 250)) // neededFavor: 80, cooldown: 0 in second
    );

    // Nerai Curse
    public static final RegistryObject<Blessings> AQUATIC_GRACE = registry.register("aquatic_grace",
            () -> new AquaticGrace(25, 0, true,false, Color.BLUE) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> RAINS_FAVOR = registry.register("rains_favor",
            () -> new RainsFavor(50, 0, true,false, Color.BLUE) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> WAVEBOUND = registry.register("wavebound",
            () -> new Wavebound(80, 10, false,false, Color.BLUE) // neededFavor: 80, cooldown: 10 in second
    );

    // Zephra Curse
    public static final RegistryObject<Blessings> GALE_STEP = registry.register("gale_step",
            () -> new GaleStep(25, 0, true,false, new Color(138, 255, 171)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> WINDCALLERS_REFLEX = registry.register("windcallers_reflex",
            () -> new WindcallersReflex(50, 20, true,false, new Color(138, 255, 171)) // neededFavor: 50, cooldown: 20 in second
    );
    public static final RegistryObject<Blessings> CYCLONE_LEAP = registry.register("cyclone_leap",
            () -> new CycloneLeap(80, 10, false,false, new Color(138, 255, 171)) // neededFavor: 80, cooldown: 10 in second
    );

    // Terra Curse
    public static final RegistryObject<Blessings> VERDANT_TOUCH = registry.register("verdant_touch",
            () -> new VerdantTouch(25, 0, true,false, new Color(172, 255, 94)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> THORNSKIN = registry.register("thornskin",
            () -> new Thornskin(50, 2, true,false, new Color(172, 255, 94)) // neededFavor: 50, cooldown: 2 in second
    );
    public static final RegistryObject<Blessings> STONEWARDENS_EMBRACE = registry.register("stonewardens_embrace",
            () -> new StonewardensEmbrace(80, 0, true,false, Color.gray) // neededFavor: 80, cooldown: 0 in second
    );

    // Lumen Curse
    public static final RegistryObject<Blessings> BEACONS_GRACE = registry.register("beacons_grace",
            () -> new BeaconsGrace(25, 0, true,false, new Color(255, 221, 0)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> CLEANSING_FLAME = registry.register("cleansing_flame",
            () -> new CleansingFlame(50, 30, true,false, new Color(255, 221, 0)) // neededFavor: 50, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> DIVINE_RADIANCE = registry.register("divine_radiance",
            () -> new DivineRadiance(80, 30, false,false, new Color(255, 221, 0)) // neededFavor: 80, cooldown: 10 in second
    );

    // Mechanos Curse
    public static final RegistryObject<Blessings> TINKERS_GRACE = registry.register("tinkers_grace",
            () -> new TinkersGrace(25, 0, true,false, Color.gray) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> AUTO_SMELTER = registry.register("auto_smelter",
            () -> new AutoSmelter(50, 30, false,true, Color.orange) // neededFavor: 50, cooldown: 20 in second
    );
    public static final RegistryObject<Blessings> CLOCKWORK_PUNCH = registry.register("clockwork_punch",
            () -> new ClockworkPunch(80, 5, true,false, Color.gray) // neededFavor: 80, cooldown: 5 in second
    );

    // Ignar Curse
    public static final RegistryObject<Blessings> BLAZING_HANDS = registry.register("blazing_hands",
            () -> new BlazingHands(25, 2, true,false, Color.orange) // neededFavor: 25, cooldown: 2 in second
    );
    public static final RegistryObject<Blessings> FIREPROOF = registry.register("fireproof",
            () -> new Fireproof(50, 0, true,false, Color.orange) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> FLASHFLARE = registry.register("flash_flare",
            () -> new FlashFlare(80, 30, false,false, Color.orange) // neededFavor: 80, cooldown: 30 in second
    );

    // Aethon Curse
    public static final RegistryObject<Blessings> VAILPIERCER = registry.register("veilpiercer",
            () -> new Veilpiercer(25, 0, true,false, new Color(55, 23, 115)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> CELESTIAL_WARD = registry.register("celestial_ward",
            () -> new CelestialWard(50, 10, true,false, new Color(55, 23, 115)) // neededFavor: 50, cooldown: 20 in second
    );
    public static final RegistryObject<Blessings> STARLIT_MIRROR = registry.register("starlit_mirror",
            () -> new StarlitMirror(80, 0, false,true, new Color(55, 23, 115)) // neededFavor: 80, cooldown: 30 in second
    );

    /**
     * Default constructor
     */
    public BlessingsRegistry() {
    }

    /**
     * Registers blessings with the event bus
     *
     * @param modEventBus The mod event bus to register with
     */
    public static void register(IEventBus modEventBus) {
        registry.register(modEventBus);
    }

    /**
     * Initializes the blessings registry
     *
     * @param modEventBus The mod event bus to initialize with
     */
    public static void init(IEventBus modEventBus) {
        register(modEventBus);
    }
}