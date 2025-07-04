package net.nightshade.divinity_engine.registry.divinity.curse;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.core.mixin.RegistryBuilderAccessor;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.aethon.CelestialWard;
import net.nightshade.divinity_engine.divinity.blessing.aethon.StarlitMirror;
import net.nightshade.divinity_engine.divinity.blessing.aethon.Veilpiercer;
import net.nightshade.divinity_engine.divinity.blessing.cryonis.FrozenHeart;
import net.nightshade.divinity_engine.divinity.blessing.cryonis.IcyPulse;
import net.nightshade.divinity_engine.divinity.blessing.cryonis.Snowveil;
import net.nightshade.divinity_engine.divinity.blessing.dravak.BattleRush;
import net.nightshade.divinity_engine.divinity.blessing.dravak.CrushingBlow;
import net.nightshade.divinity_engine.divinity.blessing.dravak.Unyielding;
import net.nightshade.divinity_engine.divinity.blessing.elaris.Foresight;
import net.nightshade.divinity_engine.divinity.blessing.elaris.InsightfulKill;
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
import net.nightshade.divinity_engine.divinity.blessing.nashara.Shadowstep;
import net.nightshade.divinity_engine.divinity.blessing.nashara.ToxicSkin;
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
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.aethon.Voidmarked;
import net.nightshade.divinity_engine.divinity.curse.cryonis.Frostbite;
import net.nightshade.divinity_engine.divinity.curse.dravak.CowardsBrand;
import net.nightshade.divinity_engine.divinity.curse.elaris.LostWisdom;
import net.nightshade.divinity_engine.divinity.curse.grond.CrackedFoundation;
import net.nightshade.divinity_engine.divinity.curse.ignar.FlameReversal;
import net.nightshade.divinity_engine.divinity.curse.kairoth.FracturedFate;
import net.nightshade.divinity_engine.divinity.curse.lumen.BlindedGrace;
import net.nightshade.divinity_engine.divinity.curse.mechanos.NullProcess;
import net.nightshade.divinity_engine.divinity.curse.nashara.VenomsSpite;
import net.nightshade.divinity_engine.divinity.curse.nerai.DrownedsPull;
import net.nightshade.divinity_engine.divinity.curse.nythea.TwilightReversal;
import net.nightshade.divinity_engine.divinity.curse.solarius.Eclipse;
import net.nightshade.divinity_engine.divinity.curse.terra.WitheredRoots;
import net.nightshade.divinity_engine.divinity.curse.varun.ScentOfPrey;
import net.nightshade.divinity_engine.divinity.curse.vokar.WraithsMark;
import net.nightshade.divinity_engine.divinity.curse.voltira.StaticWrath;
import net.nightshade.divinity_engine.divinity.curse.zephra.Windlocked;

import java.awt.*;
import java.util.function.Supplier;

/**
 * Registry for all curse in the mod
 * Manages registration of blessing types and their properties
 */
public class CurseRegistry {
    // Registry key for curse
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation(DivinityEngineMod.MODID, "curses");

    // Deferred register for curse
    private static final DeferredRegister<Curse> registry = DeferredRegister.create(REGISTRY_KEY, DivinityEngineMod.MODID);

    // Registry supplier for curse
    public static final Supplier<IForgeRegistry<Curse>> CURSES_REGISTRY = registry.makeRegistry(() -> {
        RegistryBuilder<Curse> builder = new RegistryBuilder();
        ((RegistryBuilderAccessor) builder).setHasWrapper(true);
        return builder;
    });

    public static final RegistryObject<Curse> ECLIPSE = registry.register("eclipse", Eclipse::new);
    public static final RegistryObject<Curse> VENOMS_SPITE = registry.register("venoms_spite", VenomsSpite::new);
    public static final RegistryObject<Curse> CRACKED_FOUNDATION = registry.register("cracked_foundation", CrackedFoundation::new);
    public static final RegistryObject<Curse> SCENT_OF_PREY = registry.register("scent_of_prey", ScentOfPrey::new);
    public static final RegistryObject<Curse> COWARDS_BRAND = registry.register("cowards_brand", CowardsBrand::new);
    public static final RegistryObject<Curse> LOST_WISDOM = registry.register("lost_wisdom", LostWisdom::new);
    public static final RegistryObject<Curse> STATIC_WRATH = registry.register("static_wrath", StaticWrath::new);
    public static final RegistryObject<Curse> WRAITHS_MARK = registry.register("wraiths_mark", WraithsMark::new);
    public static final RegistryObject<Curse> TWILIGHT_REVERSAL = registry.register("twilght_reversal", TwilightReversal::new);
    public static final RegistryObject<Curse> FRACTURED_FATE = registry.register("fractured_fate", FracturedFate::new);
    public static final RegistryObject<Curse> FROSTBITE = registry.register("frostbite", Frostbite::new);
    public static final RegistryObject<Curse> DROWNEDS_PULL = registry.register("drowneds_pull", DrownedsPull::new);
    public static final RegistryObject<Curse> WINDLOCKED = registry.register("windlocked", Windlocked::new);
    public static final RegistryObject<Curse> WITHERED_ROOTS = registry.register("withered_roots", WitheredRoots::new);
    public static final RegistryObject<Curse> BLINDED_GRACE = registry.register("blinded_grace", BlindedGrace::new);
    public static final RegistryObject<Curse> NULL_PROCESS = registry.register("null_process", NullProcess::new);
    public static final RegistryObject<Curse> FLAME_REVERSAL = registry.register("flame_reversal", FlameReversal::new);
    public static final RegistryObject<Curse> VOIDMARKED = registry.register("voidmarked", Voidmarked::new);

    /**
     * Default constructor
     */
    public CurseRegistry() {
    }

    /**
     * Registers curse with the event bus
     *
     * @param modEventBus The mod event bus to register with
     */
    public static void register(IEventBus modEventBus) {
        registry.register(modEventBus);
    }

    /**
     * Initializes the curse registry
     *
     * @param modEventBus The mod event bus to initialize with
     */
    public static void init(IEventBus modEventBus) {
        register(modEventBus);
    }
}