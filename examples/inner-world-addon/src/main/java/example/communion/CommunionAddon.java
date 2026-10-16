package example.communion;

import java.awt.Color;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.nightshade.divinity_engine.api.gods.GodsDeferredRegister;
import net.nightshade.divinity_engine.api.innerworld.*;
import net.nightshade.divinity_engine.api.narrative.*;
import net.nightshade.divinity_engine.api.trials.*;
import net.nightshade.divinity_engine.divinity.gods.*;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;

/** Standalone test addon: custom god, automatic statue, branching story, trial and realm. */
@Mod(CommunionAddon.ID)
public final class CommunionAddon {
    public static final String ID = "communion_example";
    private static final ResourceLocation GOD = id("vesper");
    private static final ResourceLocation STORY = id("evening_promise");
    private static final GodsDeferredRegister GODS = GodsDeferredRegister.create(ID);
    private static final net.minecraftforge.registries.RegistryObject<BaseGod> VESPER = GODS.register("vesper", () ->
        new BaseGod(Set.of("evening"), Set.of(), new Color(0x779EB8)) {
            @Override public boolean onOfferedItems(BaseGodInstance god, OfferEvent.OfferItemEvent e) {
                return e.getOffer().is(Items.AMETHYST_SHARD) || e.getOffer().is(Items.PAPER);
            }
        });
    private static final InnerWorldsDeferredRegister WORLDS = new InnerWorldsDeferredRegister(ID);
    private static final DivineNarrativeDeferredRegister STORIES = new DivineNarrativeDeferredRegister(ID);
    private static final GodTrialsDeferredRegister TRIALS = new GodTrialsDeferredRegister(ID);

    public CommunionAddon() {
        GODS.register(FMLJavaModLoadingContext.get().getModEventBus());
        STORIES.story("evening_promise", () -> DivineStoryDefinition.builder(GOD).id(STORY)
            .title("A Promise Kept at Evening")
            .request("Bring paper or amethyst to my statue, then choose a light or a record to keep.")
            .branch("a", branch("Shelter", "lantern", "Keep a light for those who return late."))
            .branch("b", branch("Remembrance", "paper", "Keep a record without pretending to know every road."))
            .build());
        STORIES.submit();
        TRIALS.register("evening", () -> new GodTrial(VESPER.get(), EveningTrial::new));
        GodlyTrialsRegistry.addDeferredRegister(TRIALS);

        WORLDS.register("evening_garden", () -> InnerWorldDefinition.builder(id("evening_garden"), GOD)
            .name("The Quiet Between Stars")
            .story(STORY)
            .visuals(visuals(0x779EB8))
            .variant(new InnerWorldDefinition.Variant("trusted",
                p -> { var god = net.nightshade.divinity_engine.util.divinity.gods.GodHelper.getGodOrNull(p, VESPER.get());
                    return god != null && god.getFavor() >= 50; }, visuals(0xAADDE9)))
            .scene(pos -> {
                if (pos.getY() == 0) return Blocks.SMOOTH_STONE.defaultBlockState();
                if (pos.getX() == 0 || pos.getZ() == 0 || pos.getX() == 24 || pos.getZ() == 24)
                    return Blocks.BARRIER.defaultBlockState();
                if (pos.getY() < 5 && (pos.getX() == 4 || pos.getX() == 20)
                        && (pos.getZ() == 4 || pos.getZ() == 20))
                    return Blocks.AMETHYST_BLOCK.defaultBlockState();
                return Blocks.AIR.defaultBlockState();
            })
            .hook((context, moment) -> {
                ServerPlayer player = context.player();
                if (player == null) return; // Offline cleanup has no player.
                if (moment == InnerWorldDefinition.Moment.ENTER)
                    player.sendSystemMessage(Component.literal("Vesper: An unfinished promise need not be a broken one."));
                if (moment == InnerWorldDefinition.Moment.STORY)
                    CommunionManager.sceneBlock(player, context.session(), new BlockPos(12, 1, 22), Blocks.SEA_LANTERN.defaultBlockState());
                if (moment == InnerWorldDefinition.Moment.INTERRUPT)
                    player.sendSystemMessage(Component.literal("Vesper: Attend to the living world. We can speak again."));
            }).build());
        WORLDS.register();
    }

    private static InnerWorldDefinition.Visuals visuals(int color) {
        return new InnerWorldDefinition.Visuals(color, vanilla("block.amethyst_block.chime"), vanilla("end_rod"), "stars");
    }

    private static DivineStoryDefinition.Branch branch(String name, String item, String ending) {
        // Deliberately a lore marker, not a claim that a new numerical blessing effect exists.
        return new DivineStoryDefinition.Branch(name, StoryObjectiveType.DONATION, "", 2,
            vanilla(item), "Contribute two " + item + " at Vesper's physical statue",
            BlessingSpecialization.builder(id("evening_" + item))
                .blessing(ResourceLocation.fromNamespaceAndPath("divinity_engine", "celestial_ward"))
                .description("Records the " + name + " ending; this sample adds no combat modifier").build(), ending);
    }

    private static final class EveningTrial extends GodlyTrial {
        EveningTrial(ServerPlayer player) { super(player); }
        @Override protected void onStart() { player.sendSystemMessage(Component.literal("Vesper: Keep watch for five seconds.")); }
        @Override protected void runPhase(int phase) { phaseDuration = 200; setPhaseInfo("Vigil", "Keep watch", 0, 100); }
        @Override public void tick() { setObjectiveProgress(getTicksElapsed()); if (getTicksElapsed() >= 100) completeTrial(); }
        @Override protected void onComplete() { player.sendSystemMessage(Component.literal("Vesper: Your vigil is remembered.")); }
        @Override protected int getMaxPhases() { return 1; }
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
    private static ResourceLocation vanilla(String path) { return ResourceLocation.withDefaultNamespace(path); }
}
