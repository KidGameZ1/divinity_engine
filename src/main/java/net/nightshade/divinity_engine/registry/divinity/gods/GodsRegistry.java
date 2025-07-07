package net.nightshade.divinity_engine.registry.divinity.gods;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.block.StatueBlock;
import net.nightshade.divinity_engine.block.entity.StatueBlockEntity;
import net.nightshade.divinity_engine.core.mixin.RegistryBuilderAccessor;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.aethon.Aethon;
import net.nightshade.divinity_engine.divinity.gods.cryonis.Cryonis;
import net.nightshade.divinity_engine.divinity.gods.dravak.Dravak;
import net.nightshade.divinity_engine.divinity.gods.elaris.Elaris;
import net.nightshade.divinity_engine.divinity.gods.grond.Grond;
import net.nightshade.divinity_engine.divinity.gods.ignar.Ignar;
import net.nightshade.divinity_engine.divinity.gods.kairoth.Kairoth;
import net.nightshade.divinity_engine.divinity.gods.lumen.Lumen;
import net.nightshade.divinity_engine.divinity.gods.mechanos.Mechanos;
import net.nightshade.divinity_engine.divinity.gods.nashara.Nashara;
import net.nightshade.divinity_engine.divinity.gods.nerai.Nerai;
import net.nightshade.divinity_engine.divinity.gods.nythea.Nythea;
import net.nightshade.divinity_engine.divinity.gods.solarius.Solarius;
import net.nightshade.divinity_engine.divinity.gods.terra.Terra;
import net.nightshade.divinity_engine.divinity.gods.varun.Varun;
import net.nightshade.divinity_engine.divinity.gods.vokar.Vokar;
import net.nightshade.divinity_engine.divinity.gods.voltira.Voltira;
import net.nightshade.divinity_engine.divinity.gods.zephra.Zephra;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.registry.blocks.BlocksRegistry;
import net.nightshade.divinity_engine.registry.blocks.entity.BlockEntitesRegistry;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.curse.CurseRegistry;
import net.nightshade.divinity_engine.registry.item.ItemsRegistry;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry.*;
import static net.nightshade.divinity_engine.registry.divinity.curse.CurseRegistry.*;

/**
 * Registry for all gods in the Divinity Engine mod
 */
public class GodsRegistry {
    // Registry key for the gods registry
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation(DivinityEngineMod.MODID, "gods");

    // Deferred register for BaseGod class
    private static final DeferredRegister<BaseGod> registry = DeferredRegister.create(REGISTRY_KEY, DivinityEngineMod.MODID);

    // Forge registry instance for gods
    public static final Supplier<IForgeRegistry<BaseGod>> GODS_REGISTRY = registry.makeRegistry(() -> {
        RegistryBuilder<BaseGod> builder = new RegistryBuilder();
        ((RegistryBuilderAccessor) builder).setHasWrapper(true);
        return builder;
    });

    // Solarius – God of the Sun
    public static final RegistryObject<BaseGod> SOLARIUS = register("solarius", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(RADIANT_STRIKE);
        blessings.add(SUNSKIN);
        blessings.add(SUNBURST);
        return new Solarius(
                Set.of("light", "justice", "day"),
                blessings,
                ECLIPSE.get(),
                Color.red
        );
    });

    // Nashara – Goddess of Venom
    public static final RegistryObject<BaseGod> NASHARA = register("nashara", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(TOXIC_SKIN);
        blessings.add(SERPENTS_BITE);
        blessings.add(SHADOWSTEP);
        return new Nashara(
                Set.of("poison", "deception", "shadow"),
                blessings,
                VENOMS_SPITE.get(),
                Color.green
        );
    });

    // Grond – God of Stone & Forge
    public static final RegistryObject<BaseGod> GROND = register("grond", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(HAMMERHAND);
        blessings.add(STONEFORM);
        blessings.add(ECHOING_STRIKE);
        return new Grond(
                Set.of("mountains", "crafting", "endurance"),
                blessings,
                CRACKED_FOUNDATION.get(),
                Color.orange
        );
    });

    // Varun – God of Beasts
    public static final RegistryObject<BaseGod> VARUN = register("varun", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(FERAL_STEP);
        blessings.add(TRACKERS_MARK);
        blessings.add(PREDATORS_FOCUS);
        return new Varun(
                Set.of("animals", "instinct", "wilderness"),
                blessings,
                SCENT_OF_PREY.get(),
                Color.green
        );
    });

    // Dravak – God of War
    public static final RegistryObject<BaseGod> DRAVAK = register("dravak", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(BATTLE_RUSH);
        blessings.add(UNYIELDING);
        blessings.add(CRUSHING_BLOW);
        return new Dravak(
                Set.of("blood", "valor", "strength"),
                blessings,
                COWARDS_BRAND.get(),
                new Color(235, 70, 52)
        );
    });

    // Elaris – Goddess of Wisdom
    public static final RegistryObject<BaseGod> ELARIS = register("elaris", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(INSIGHTFUL_KILL);
        blessings.add(FORESIGHT);
        blessings.add(SCHOLARS_EDGE);
        return new Elaris(
                Set.of("knowledge", "magic", "insight"),
                blessings,
                LOST_WISDOM.get(),
                Color.white
        );
    });

    // Voltira – Goddess of Storms
    public static final RegistryObject<BaseGod> VOLTIRA = register("voltira", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(STORMSTEP);
        blessings.add(SHOCKSHIELD);
        blessings.add(TEMPEST_WRATH);
        return new Voltira(
                Set.of("thunder", "chaos", "sky"),
                blessings,
                STATIC_WRATH.get(),
                new Color(0, 255, 251)
        );
    });

    // Vokar – God of Death
    public static final RegistryObject<BaseGod> VOKAR = register("vokar", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(SOUL_LEECH);
        blessings.add(WRAITHWALK);
        blessings.add(GHASTLY_AURA);
        return new Vokar(
                Set.of("spirits", "shadows", "finality"),
                blessings,
                WRAITHS_MARK.get(),
                new Color(50, 18, 82)
        );
    });

    // Nythea – Goddess of the Moon
    public static final RegistryObject<BaseGod> NYTHEA = register("nythea", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(TWILIGHT_VEIL);
        blessings.add(LULL_OF_THE_LOST);
        blessings.add(PHASE_BLINK);
        return new Nythea(
                Set.of("night", "illusion", "calm"),
                blessings,
                TWILIGHT_REVERSAL.get(),
                new Color(163, 231, 240)
        );
    });

    // Kairoth – God of Fate
    public static final RegistryObject<BaseGod> KAIROTH = register("kairoth", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(SECOND_CHANCE);
        blessings.add(ECHO_STEP);
        blessings.add(PREDICTED_PATH);
        return new Kairoth(
                Set.of("time", "destiny", "choice"),
                blessings,
                FRACTURED_FATE.get(),
                new Color(248, 250, 152)
        );
    });

    // Cryonis – God of Frost
    public static final RegistryObject<BaseGod> CRYONIS = register("cryonis", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(FROZEN_HEART);
        blessings.add(ICY_PULSE);
        blessings.add(SNOWVEIL);
        return new Cryonis(
                Set.of("ice", "winter", "stillness"),
                blessings,
                FROSTBITE.get(),
                new Color(152, 234, 250)
        );
    });

    // Nerai – Goddess of Tides
    public static final RegistryObject<BaseGod> NERAI = register("nerai", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(AQUATIC_GRACE);
        blessings.add(RAINS_FAVOR);
        blessings.add(WAVEBOUND);
        return new Nerai(
                Set.of("water", "rain", "weather"),
                blessings,
                DROWNEDS_PULL.get(),
                Color.BLUE
        );
    });

    // Zephra – Goddess of Wind
    public static final RegistryObject<BaseGod> ZEPHRA = register("zephra", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(GALE_STEP);
        blessings.add(WINDCALLERS_REFLEX);
        blessings.add(CYCLONE_LEAP);
        return new Zephra(
                Set.of("air", "movement", "speed"),
                blessings,
                WINDLOCKED.get(),
                new Color(138, 255, 171)
        );
    });

    // Terra – Goddess of Growth
    public static final RegistryObject<BaseGod> TERRA = register("terra", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(VERDANT_TOUCH);
        blessings.add(THORNSKIN);
        blessings.add(STONEWARDENS_EMBRACE);
        return new Terra(
                Set.of("nature", "plants", "earth"),
                blessings,
                WITHERED_ROOTS.get(),
                new Color(172, 255, 94)
        );
    });

    // Lumen – Goddess of Light
    public static final RegistryObject<BaseGod> LUMEN = register("lumen", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(BEACONS_GRACE);
        blessings.add(CLEANSING_FLAME);
        blessings.add(DIVINE_RADIANCE);
        return new Lumen(
                Set.of("light", "order", "purity"),
                blessings,
                BLINDED_GRACE.get(),
                new Color(255, 221, 0)
        );
    });

    // Mechanos – God of Craft & Industry
    public static final RegistryObject<BaseGod> MECHANOS = register("mechanos", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(TINKERS_GRACE);
        blessings.add(AUTO_SMELTER);
        blessings.add(CLOCKWORK_PUNCH);
        return new Mechanos(
                Set.of("metal", "crafting", "golems"),
                blessings,
                NULL_PROCESS.get(),
                Color.gray
        );
    });

    // Ignar – God of Flame
    public static final RegistryObject<BaseGod> IGNAR = register("ignar", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(BLAZING_HANDS);
        blessings.add(FIREPROOF);
        blessings.add(FLASHFLARE);
        return new Ignar(
                Set.of("fire", "combat"),
                blessings,
                FLAME_REVERSAL.get(),
                Color.orange
        );
    });

    // Aethon – God of Stars
    public static final RegistryObject<BaseGod> AETHON = register("aethon", () -> {
        Set<RegistryObject<Blessings>> blessings = new HashSet<>();
        blessings.add(VAILPIERCER);
        blessings.add(CELESTIAL_WARD);
        blessings.add(STARLIT_MIRROR);
        return new Aethon(
                Set.of("stars", "vision", "cosmic"),
                blessings,
                VOIDMARKED.get(),
                new Color(55, 23, 115)
        );
    });

    /**
     * Default constructor
     */
    public GodsRegistry() {
    }

    /**
     * Register the gods capability and registry
     *
     * @param modEventBus The mod event bus to register to
     */
    public static void register(IEventBus modEventBus) {
        PlayerGodsCapability.init(modEventBus);
        registry.register(modEventBus);
    }

    /**
     * Initialize the gods registry
     *
     * @param modEventBus The mod event bus to initialize with
     */
    public static void init(IEventBus modEventBus) {
        register(modEventBus);
    }

    /**
     * Register a new god
     *
     * @param name     The name of the god
     * @param supplier The supplier that creates the god
     * @return RegistryObject containing the registered god
     */
    public static RegistryObject<BaseGod> register(String name, Supplier<? extends BaseGod> supplier) {
        RegistryObject<BaseGod> reg = registry.register(name, supplier);

        RegistryObject<Block> statue = BlocksRegistry.REGISTRY.register(name + "_statue", () -> new StatueBlock(reg));
        ItemsRegistry.REGISTRY.register(statue.getId().getPath(), () -> new BlockItem(statue.get(), new Item.Properties()));
        BlockEntitesRegistry.register(name+"_statue", statue, StatueBlockEntity::new);


        return reg;
    }

    static {

    }
}