package net.nightshade.divinity_engine.registry.divinity.trial;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.api.trials.GodTrial;
import net.nightshade.divinity_engine.api.trials.GodTrialsDeferredRegister;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.divinity.trial.aethon.TrialOfStars;
import net.nightshade.divinity_engine.divinity.trial.cryonis.TrialOfFrost;
import net.nightshade.divinity_engine.divinity.trial.dravak.TrialOfWar;
import net.nightshade.divinity_engine.divinity.trial.elaris.TrialOfKnowledge;
import net.nightshade.divinity_engine.divinity.trial.grond.TrialOfStone;
import net.nightshade.divinity_engine.divinity.trial.ignar.TrialOfFire;
import net.nightshade.divinity_engine.divinity.trial.kairoth.TrialOfTime;
import net.nightshade.divinity_engine.divinity.trial.lumen.TrialOfOrder;
import net.nightshade.divinity_engine.divinity.trial.mechanos.TrialOfInvention;
import net.nightshade.divinity_engine.divinity.trial.nashara.TrialOfShadows;
import net.nightshade.divinity_engine.divinity.trial.nerai.TrialOfTheSea;
import net.nightshade.divinity_engine.divinity.trial.nythea.TrialOfDreams;
import net.nightshade.divinity_engine.divinity.trial.solarius.TrialOfLight;
import net.nightshade.divinity_engine.divinity.trial.terra.TrialOfNature;
import net.nightshade.divinity_engine.divinity.trial.varun.TrialOfBeasts;
import net.nightshade.divinity_engine.divinity.trial.vokar.TrialOfDeath;
import net.nightshade.divinity_engine.divinity.trial.voltira.TrialOfStorms;
import net.nightshade.divinity_engine.divinity.trial.zephra.TrialOfTheSky;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class GodlyTrialsRegistry {
    private static boolean initialized = false;
    private static final Map<ResourceLocation, GodTrial> TRIALS_BY_ID = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> GOD_TO_TRIAL_ID = new LinkedHashMap<>();
    private static final List<GodTrialsDeferredRegister> DEFERRED_REGISTERS = new ArrayList<>();

    public static void register(ResourceLocation id, GodTrial definition) {
        TRIALS_BY_ID.put(id, definition);
        GOD_TO_TRIAL_ID.put(Objects.requireNonNull(definition.getGod().getRegistryName()), id);
    }

    public static void addDeferredRegister(GodTrialsDeferredRegister register) {
        if (initialized) {
            register.registerAll();
            return;
        }
        DEFERRED_REGISTERS.add(register);
    }

    public static @Nullable GodTrial get(BaseGod god) {
        if (god == null || god.getRegistryName() == null) {
            return null;
        }
        ResourceLocation trialId = GOD_TO_TRIAL_ID.get(god.getRegistryName());
        return trialId == null ? null : TRIALS_BY_ID.get(trialId);
    }

    public static @Nullable GodTrial get(ResourceLocation id) {
        return TRIALS_BY_ID.get(id);
    }

    public static @Nullable ResourceLocation getId(BaseGod god) {
        if (god == null || god.getRegistryName() == null) {
            return null;
        }
        return GOD_TO_TRIAL_ID.get(god.getRegistryName());
    }

    public static GodlyTrial createTrial(BaseGod god, ServerPlayer player) {
        ResourceLocation id = getId(god);
        if (id == null) {
            throw new IllegalArgumentException("No trial registered for god: " + god);
        }
        return createTrial(id, player);
    }

    public static GodlyTrial createTrial(ResourceLocation id, ServerPlayer player) {
        GodTrial definition = TRIALS_BY_ID.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("No trial registered with id: " + id);
        }
        return definition.getFactory().apply(player);
    }

    public static String getDisplayName(ResourceLocation id) {
        String path = id.getPath().replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String token : path.split(" ")) {
            if (token.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(token.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(token.substring(1));
        }
        return builder.toString();
    }

    public static void initializeTrials() {
        if (initialized) {
            return;
        }
        initialized = true;

        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_storms"), new GodTrial(GodsRegistry.VOLTIRA.get(), TrialOfStorms::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_light"), new GodTrial(GodsRegistry.SOLARIUS.get(), TrialOfLight::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_shadows"), new GodTrial(GodsRegistry.NASHARA.get(), TrialOfShadows::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_stone"), new GodTrial(GodsRegistry.GROND.get(), TrialOfStone::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_beasts"), new GodTrial(GodsRegistry.VARUN.get(), TrialOfBeasts::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_war"), new GodTrial(GodsRegistry.DRAVAK.get(), TrialOfWar::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_knowledge"), new GodTrial(GodsRegistry.ELARIS.get(), TrialOfKnowledge::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_death"), new GodTrial(GodsRegistry.VOKAR.get(), TrialOfDeath::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_dreams"), new GodTrial(GodsRegistry.NYTHEA.get(), TrialOfDreams::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_time"), new GodTrial(GodsRegistry.KAIROTH.get(), TrialOfTime::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_frost"), new GodTrial(GodsRegistry.CRYONIS.get(), TrialOfFrost::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_the_sea"), new GodTrial(GodsRegistry.NERAI.get(), TrialOfTheSea::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_the_sky"), new GodTrial(GodsRegistry.ZEPHRA.get(), TrialOfTheSky::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_nature"), new GodTrial(GodsRegistry.TERRA.get(), TrialOfNature::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_order"), new GodTrial(GodsRegistry.LUMEN.get(), TrialOfOrder::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_invention"), new GodTrial(GodsRegistry.MECHANOS.get(), TrialOfInvention::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_fire"), new GodTrial(GodsRegistry.IGNAR.get(), TrialOfFire::new));
        register(new ResourceLocation(DivinityEngineMod.MODID, "trial_of_stars"), new GodTrial(GodsRegistry.AETHON.get(), TrialOfStars::new));

        for (GodTrialsDeferredRegister register : DEFERRED_REGISTERS) {
            register.registerAll();
        }

        DivinityEngineMod.LOGGER.info("Trials Registered");
    }
}
