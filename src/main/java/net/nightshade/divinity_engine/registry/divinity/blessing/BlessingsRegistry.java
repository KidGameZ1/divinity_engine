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
import net.nightshade.divinity_engine.divinity.blessing.aethon.*;
import net.nightshade.divinity_engine.divinity.blessing.cryonis.*;
import net.nightshade.divinity_engine.divinity.blessing.dravak.*;
import net.nightshade.divinity_engine.divinity.blessing.elaris.*;
import net.nightshade.divinity_engine.divinity.blessing.grond.*;
import net.nightshade.divinity_engine.divinity.blessing.ignar.*;
import net.nightshade.divinity_engine.divinity.blessing.kairoth.*;
import net.nightshade.divinity_engine.divinity.blessing.lumen.*;
import net.nightshade.divinity_engine.divinity.blessing.mechanos.*;
import net.nightshade.divinity_engine.divinity.blessing.nashara.*;
import net.nightshade.divinity_engine.divinity.blessing.nerai.*;
import net.nightshade.divinity_engine.divinity.blessing.nythea.*;
import net.nightshade.divinity_engine.divinity.blessing.solarius.*;
import net.nightshade.divinity_engine.divinity.blessing.terra.*;
import net.nightshade.divinity_engine.divinity.blessing.varun.*;
import net.nightshade.divinity_engine.divinity.blessing.vokar.*;
import net.nightshade.divinity_engine.divinity.blessing.voltira.*;
import net.nightshade.divinity_engine.divinity.blessing.zephra.*;

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

    // Solarius Blessings
    public static final RegistryObject<Blessings> RADIANT_STRIKE = registry.register("radiant_strike",
            () -> new RadiantStrike(25, 0, true,false, false, Color.red) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SUNSKIN = registry.register("sunskin",
            () -> new Sunskin(90, 0, true,false, false, Color.red) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SUNBURST = registry.register("sunburst",
            () -> new Sunburst(140, 45, false,false, false, Color.red) // neededFavor: 80, cooldown: 45 in second
    );
    public static final RegistryObject<Blessings> SOLAR_WARD = registry.register("solar_ward",
            () -> new SolarWard(180, 30, false,true, false, Color.red) // neededFavor: 80, cooldown: 45 in second
    );
    public static final RegistryObject<Blessings> CORONA_ASCENDANT = registry.register("corona_ascendant",
            () -> new CoronaAscendant(240, 30, false,true, true, Color.red) // neededFavor: 80, cooldown: 45 in second
    );

    // Nashara Blessings
    public static final RegistryObject<Blessings> TOXIC_SKIN = registry.register("toxic_skin",
            () -> new ToxicSkin(30, 0, true,false, false, Color.green) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SERPENTS_BITE = registry.register("serpents_bite",
            () -> new SerpentsBite(85, 0, true,false, false, Color.green) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SHADOWSTEP = registry.register("shadowstep",
            () -> new Shadowstep(135, 25, true,false, false, Color.darkGray) // neededFavor: 80, cooldown: 25 in second
    );
    public static final RegistryObject<Blessings> VENOMBLOOD = registry.register("venomblood",
            () -> new Venomblood(175, 0, true,false, false, Color.green) // neededFavor: 80, cooldown: 25 in second
    );
    public static final RegistryObject<Blessings> COIDED_RETRIBUTION = registry.register("coiled_retribution",
            () -> new CoiledRetribution(230, 0, true,false, false, Color.green) // neededFavor: 80, cooldown: 25 in second
    );

    // Grond Blessings
    public static final RegistryObject<Blessings> HAMMERHAND = registry.register("hammerhand",
            () -> new Hammerhand(40, 0, true,false, false, Color.orange) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> STONEFORM = registry.register("stoneform",
            () -> new Stoneform(95, 25, true,false, false, Color.gray) // neededFavor: 50, cooldown: 25 in second
    );
    public static final RegistryObject<Blessings> ECHOING_STRIKE = registry.register("echoing_strike",
            () -> new EchoingStrike(145, 45, true,false, false, Color.gray) // neededFavor: 50, cooldown: 45 in second
    );
    public static final RegistryObject<Blessings> EARTHBIND_GRIP = registry.register("earthbind_grip",
            () -> new EarthbindGrip(175, 6, true,false, false, Color.gray) // neededFavor: 50, cooldown: 45 in second
    );
    public static final RegistryObject<Blessings> FORGEHEART = registry.register("forgeheart",
            () -> new Forgeheart(235, 6, true,false, false, Color.gray) // neededFavor: 50, cooldown: 45 in second
    );

    // Varun Blessings
    public static final RegistryObject<Blessings> FERAL_STEP = registry.register("feral_step",
            () -> new FeralStep(25, 0, true,false, false, Color.green) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> TRACKERS_MARK = registry.register("trackers_mark",
            () -> new TrackersMark(90, 0, true,false, false, Color.green) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> PREDATORS_FOCUS = registry.register("predators_focus",
            () -> new PredatorsFocus(135, 45, true,false, false, Color.green) // neededFavor: 80, cooldown: 45 in second
    );
    public static final RegistryObject<Blessings> SAVAGE_HUNGER = registry.register("savage_hunger",
            () -> new SavageHunger(170, 10, true,false, false, Color.green) // neededFavor: 80, cooldown: 45 in second
    );
    public static final RegistryObject<Blessings> ALPHAS_CALL = registry.register("alphas_call",
            () -> new AlphasCall(225, 30, false,true, false, Color.red) // neededFavor: 80, cooldown: 45 in second
    );

    // Dravak Blessings
    public static final RegistryObject<Blessings> BATTLE_RUSH = registry.register("battle_rush",
            () -> new BattleRush(45, 10, true,false, false, new Color(235, 70, 52)) // neededFavor: 25, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> UNYIELDING = registry.register("unyielding",
            () -> new Unyielding(100, 0, true,false, false, new Color(235, 70, 52)) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> CRUSHING_BLOW = registry.register("crushing_blow",
            () -> new CrushingBlow(145, 30, true,false, false, new Color(235, 70, 52)) // neededFavor: 80, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> WARLORDS_WILL = registry.register("warlords_will",
            () -> new WarlordsWill(185, 30, false,true, false, new Color(235, 70, 52)) // neededFavor: 80, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> FURY_UNBOUND = registry.register("fury_unbound",
            () -> new FuryUnbound(240, 35, false,true, false, new Color(235, 70, 52)) // neededFavor: 80, cooldown: 30 in second
    );

    // Elaris Blessings
    public static final RegistryObject<Blessings> INSIGHTFUL_KILL = registry.register("insightful_kill",
            () -> new InsightfulKill(30, 10, true,false, false, Color.white) // neededFavor: 25, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> FORESIGHT = registry.register("foresight",
            () -> new Foresight(95, 20, true,false, false, Color.white) // neededFavor: 50, cooldown: 20 in second
    );
    public static final RegistryObject<Blessings> SCHOLARS_EDGE = registry.register("scholars_edge",
            () -> new ScholarsEdge(140, 0, true,false, false, Color.white) // neededFavor: 80, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> MIND_MIRROR = registry.register("mind_mirror",
            () -> new MindMirror(180, 0, true,false, false, Color.white) // neededFavor: 80, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> ARCANE_RECALL = registry.register("arcane_recall",
            () -> new ArcaneRecall(235, 0, false,true, false, Color.white) // neededFavor: 80, cooldown: 0 in second
    );

    // Voltira Blessings
    public static final RegistryObject<Blessings> STORMSTEP = registry.register("stormstep",
            () -> new Stormstep(20, 15, false, true, false, new Color(0, 255, 251)) // neededFavor: 25, cooldown: 15 in second
    );
    public static final RegistryObject<Blessings> SHOCKSHIELD = registry.register("shockshield",
            () -> new Shockshield(105, 5, true,false, false, new Color(0, 255, 251)) // neededFavor: 50, cooldown: 5 in second
    );
    public static final RegistryObject<Blessings> TEMPEST_WRATH = registry.register("tempest_wrath",
            () -> new TempestWrath(160, 30, true,false, false, new Color(0, 255, 251)) // neededFavor: 80, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> CHARGE_RELAY = registry.register("charge_relay",
            () -> new ChargeRelay(195, 0, true,false, false, new Color(0, 255, 251)) // neededFavor: 80, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> LIGHTNING_CONDUIT = registry.register("lightning_conduit",
            () -> new LightningConduit(245, 0, true,false, false, new Color(0, 255, 251)) // neededFavor: 80, cooldown: 30 in second
    );

    // Vokar Blessings
    public static final RegistryObject<Blessings> SOUL_LEECH = registry.register("soul_leech",
            () -> new SoulLeech(35, 0, true,false, false, new Color(50, 18, 82)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> WRAITHWALK = registry.register("wraithwalk",
            () -> new Wraithwalk(100, 15, true,false, false, new Color(50, 18, 82)) // neededFavor: 50, cooldown: 15 in second
    );
    public static final RegistryObject<Blessings> GHASTLY_AURA = registry.register("ghastly_aura",
            () -> new GhastlyAura(145, 0, true,false, false, new Color(50, 18, 82)) // neededFavor: 80, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> DREAD_COMSUMPTION = registry.register("dread_consumption",
            () -> new DreadConsumption(185, 6, true,false, false, new Color(50, 18, 82)) // neededFavor: 80, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> WITHERBOUND = registry.register("witherbound",
            () -> new Witherbound(240, 6, false,true, false, new Color(50, 18, 82)) // neededFavor: 80, cooldown: 0 in second
    );

    // Nythea Blessings
    public static final RegistryObject<Blessings> TWILIGHT_VEIL = registry.register("twilight_veil",
            () -> new TwilightVeil(30, 0, true,false, false, new Color(163, 231, 240)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> LULL_OF_THE_LOST = registry.register("lull_of_the_lost",
            () -> new LullOfTheLost(90, 0, true,false, false, new Color(163, 231, 240)) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> PHASE_BLINK = registry.register("phase_blink",
            () -> new PhaseBlink(140, 10, false,true, false, new Color(163, 231, 240)) // neededFavor: 80, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> SILVER_MIRROR = registry.register("silver_mirror",
            () -> new SilverMirror(175, 25, false,true, true, new Color(163, 231, 240)) // neededFavor: 80, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> MOONDRINKER = registry.register("moondrinker",
            () -> new Moondrinker(230, 0, true,false, false, new Color(163, 231, 240)) // neededFavor: 80, cooldown: 10 in second
    );

    // Kairoth Blessings
    public static final RegistryObject<Blessings> SECOND_CHANCE = registry.register("second_chance",
            () -> new SecondChance(50, 10800, true,false, false, new Color(248, 250, 152)) // neededFavor: 25, cooldown: 10800 in second
    );
    public static final RegistryObject<Blessings> ECHO_STEP = registry.register("echo_step",
            () -> new EchoStep(125, 30, false,true, false, new Color(248, 250, 152)) // neededFavor: 50, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> PREDICTED_PATH = registry.register("predicted_path",
            () -> new PredictedPath(180, 15, true,false, false, new Color(248, 250, 152)) // neededFavor: 80, cooldown: 15 in second
    );
    public static final RegistryObject<Blessings> THREADSEVER = registry.register("threadsever",
            () -> new Threadsever(210, 30, true,false, false, new Color(248, 250, 152)) // neededFavor: 80, cooldown: 15 in second
    );
    public static final RegistryObject<Blessings> FATEFUL_DRAW = registry.register("fateful_draw",
            () -> new FatefulDraw(235, 30, true,false, false, new Color(248, 250, 152)) // neededFavor: 80, cooldown: 15 in second
    );

    // Cryonis Blessings
    public static final RegistryObject<Blessings> FROZEN_HEART = registry.register("frozen_heart",
            () -> new FrozenHeart(25, 0,true ,false,false, new Color(152, 234, 250)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> ICY_PULSE = registry.register("icy_pulse",
            () -> new IcyPulse(90, 0, true,false,false, new Color(152, 234, 250)) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> SNOWVEIL = registry.register("snowveil",
            () -> new Snowveil(145, 0, true,false,false, new Color(152, 234, 250)) // neededFavor: 80, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> FROSTBITE_MANTLE = registry.register("frostbite_mantle",
            () -> new FrostbiteMantle(180, 0, true,false,false, new Color(152, 234, 250)) // neededFavor: 80, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> PERMAFROST_SURGE = registry.register("permafrost_surge",
            () -> new PermafrostSurge(240, 25, false,true,false, new Color(152, 234, 250)) // neededFavor: 80, cooldown: 0 in second
    );

    // Nerai Blessings
    public static final RegistryObject<Blessings> AQUATIC_GRACE = registry.register("aquatic_grace",
            () -> new AquaticGrace(20, 0, true,false,false, Color.BLUE) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> RAINS_FAVOR = registry.register("rains_favor",
            () -> new RainsFavor(85, 0, true,false, false, Color.BLUE) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> WAVEBOUND = registry.register("wavebound",
            () -> new Wavebound(140, 10, false,true, false, Color.BLUE) // neededFavor: 80, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> UNDERCURRENT_GRIP = registry.register("undercurrent_grip",
            () -> new UndercurrentGrip(175, 10, false,true,false, Color.BLUE) // neededFavor: 80, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> TIDECALLERS_WRATH = registry.register("tidecallers_wrath",
            () -> new TidecallersWrath(235, 30, false,true,false, Color.BLUE) // neededFavor: 80, cooldown: 10 in second
    );

    // Zephra Blessings
    public static final RegistryObject<Blessings> GALE_STEP = registry.register("gale_step",
            () -> new GaleStep(25, 0, true,false,false, new Color(138, 255, 171)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> WINDCALLERS_REFLEX = registry.register("windcallers_reflex",
            () -> new WindcallersReflex(95, 20, true,false, false, new Color(138, 255, 171)) // neededFavor: 50, cooldown: 20 in second
    );
    public static final RegistryObject<Blessings> CYCLONE_LEAP = registry.register("cyclone_leap",
            () -> new CycloneLeap(150, 10, false,true, false, new Color(138, 255, 171)) // neededFavor: 80, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> SHEARWIND_DASH = registry.register("shearwind_dash",
            () -> new ShearwindDash(185, 6, false,true, false, new Color(138, 255, 171)) // neededFavor: 80, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> TEMPEST_SPIRAL = registry.register("tempest_spiral",
            () -> new TempestSpiral(240, 25, false,true,false, new Color(138, 255, 171)) // neededFavor: 80, cooldown: 10 in second
    );

    // Terra Blessings
    public static final RegistryObject<Blessings> VERDANT_TOUCH = registry.register("verdant_touch",
            () -> new VerdantTouch(30, 0, true,false,false, new Color(172, 255, 94)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> THORNSKIN = registry.register("thornskin",
            () -> new Thornskin(90, 2, true,false, false, new Color(172, 255, 94)) // neededFavor: 50, cooldown: 2 in second
    );
    public static final RegistryObject<Blessings> STONEWARDENS_EMBRACE = registry.register("stonewardens_embrace",
            () -> new StonewardensEmbrace(145, 0, true, false,false, Color.gray) // neededFavor: 80, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> ROOTGRIP = registry.register("rootgrip",
            () -> new Rootgrip(175, 10, true,false,false, new Color(172, 255, 94)) // neededFavor: 80, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> WILDHEART_BLOOM = registry.register("wildheart_bloom",
            () -> new WildheartBloom(230, 30, false,true, true, new Color(172, 255, 94)) // neededFavor: 80, cooldown: 0 in second
    );

    // Lumen Blessings
    public static final RegistryObject<Blessings> BEACONS_GRACE = registry.register("beacons_grace",
            () -> new BeaconsGrace(40, 0, true,false, false, new Color(255, 221, 0)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> CLEANSING_FLAME = registry.register("cleansing_flame",
            () -> new CleansingFlame(100, 30, true,false, false, new Color(255, 221, 0)) // neededFavor: 50, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> DIVINE_RADIANCE = registry.register("divine_radiance",
            () -> new DivineRadiance(150, 30, false,true,false, new Color(255, 221, 0)) // neededFavor: 80, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> LIGHTBEARERS_OATH = registry.register("lightbearers_oath",
            () -> new LightbearersOath(185, 0, true,false, false, new Color(255, 221, 0)) // neededFavor: 80, cooldown: 10 in second
    );
    public static final RegistryObject<Blessings> DAWNSHATTER = registry.register("dawnshatter",
            () -> new Dawnshatter(240, 0, false,true,false, new Color(255, 221, 0)) // neededFavor: 80, cooldown: 10 in second
    );

    // Mechanos Blessings
    public static final RegistryObject<Blessings> TINKERS_GRACE = registry.register("tinkers_grace",
            () -> new TinkersGrace(25, 0, true,false,false, Color.gray) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> AUTO_SMELTER = registry.register("auto_smelter",
            () -> new AutoSmelter(90, 30, false,true,true, Color.orange) // neededFavor: 50, cooldown: 20 in second
    );
    public static final RegistryObject<Blessings> CLOCKWORK_PUNCH = registry.register("clockwork_punch",
            () -> new ClockworkPunch(150, 5, true,false,false, Color.gray) // neededFavor: 80, cooldown: 5 in second
    );
    public static final RegistryObject<Blessings> PRECISION_IMPACT = registry.register("precision_impact",
            () -> new PrecisionImpact(185, 5, true,false,false, Color.gray) // neededFavor: 80, cooldown: 5 in second
    );
    public static final RegistryObject<Blessings> OVERDRIVE_LOOP = registry.register("overdrive_loop",
            () -> new OverdriveLoop(240, 5, true,false,false, Color.gray) // neededFavor: 80, cooldown: 5 in second
    );

    // Ignar Blessings
    public static final RegistryObject<Blessings> BLAZING_HANDS = registry.register("blazing_hands",
            () -> new BlazingHands(35, 2, true,false,false, Color.orange) // neededFavor: 25, cooldown: 2 in second
    );
    public static final RegistryObject<Blessings> FIREPROOF = registry.register("fireproof",
            () -> new Fireproof(100, 0, true,false,false, Color.orange) // neededFavor: 50, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> FLASHFLARE = registry.register("flash_flare",
            () -> new FlashFlare(150, 30, false,true,false, Color.orange) // neededFavor: 80, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> KINDING_FURY = registry.register("kinding_fury",
            () -> new KindlingFury(185, 30, true,false, false, Color.orange) // neededFavor: 80, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> INFERNO_MANTLE = registry.register("inferno_mantle",
            () -> new InfernoMantle(245, 30, false,true, true, Color.orange) // neededFavor: 80, cooldown: 30 in second
    );

    // Aethon Blessings
    public static final RegistryObject<Blessings> VAILPIERCER = registry.register("veilpiercer",
            () -> new Veilpiercer(45, 0, true,false, false, new Color(55, 23, 115)) // neededFavor: 25, cooldown: 0 in second
    );
    public static final RegistryObject<Blessings> CELESTIAL_WARD = registry.register("celestial_ward",
            () -> new CelestialWard(125, 10, true,false,false, new Color(55, 23, 115)) // neededFavor: 50, cooldown: 20 in second
    );
    public static final RegistryObject<Blessings> STARLIT_MIRROR = registry.register("starlit_mirror",
            () -> new StarlitMirror(180, 0, false,true, true, new Color(55, 23, 115)) // neededFavor: 80, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> GRAVITY_FOLD = registry.register("gravity_fold",
            () -> new GravityFold(210, 8, true,false, false, new Color(55, 23, 115)) // neededFavor: 80, cooldown: 30 in second
    );
    public static final RegistryObject<Blessings> POCKET_DIMENSION = registry.register("pocket_dimension",
            () -> new PocketDimension(250, 2, true,true, false, new Color(55, 23, 115)) // neededFavor: 80, cooldown: 30 in second
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