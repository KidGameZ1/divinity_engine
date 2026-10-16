package example.pantheon.connected;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.api.narrative.BlessingSpecialization;
import net.nightshade.divinity_engine.api.narrative.DivineNarrativeDeferredRegister;
import net.nightshade.divinity_engine.api.narrative.DivineNarrativeRegistryObject;
import net.nightshade.divinity_engine.api.narrative.DivineStoryDefinition;
import net.nightshade.divinity_engine.api.narrative.GodRelationshipDefinition;
import net.nightshade.divinity_engine.api.narrative.NarrativeText;
import net.nightshade.divinity_engine.api.narrative.StoryChapterDefinition;
import net.nightshade.divinity_engine.api.narrative.StoryChronicle;
import net.nightshade.divinity_engine.api.narrative.StoryObjective;
import net.nightshade.divinity_engine.api.narrative.StoryObjectiveType;

/** Narrative module for an addon that supplies Vesper, her statue, offerings and trial. */
public final class ConnectedPantheonAddon {
    public static final String MOD_ID = "example_pantheon";
    public static final ResourceLocation VESPER = id("vesper");
    public static final ResourceLocation FIRST_STAR = id("first_star");
    public static final ResourceLocation LAST_LANTERN = id("last_lantern");
    public static final ResourceLocation SOLARIUS = engine("solarius");
    public static final ResourceLocation SOLAR_WARD = engine("solar_ward");
    public static final ResourceLocation TWILIGHT_VEIL = engine("twilight_veil");

    // Class initialization happens from register(), before common setup or JSON decoding.
    public static final StoryObjectiveType STAR_CHART = StoryObjectiveType.register(
        new StoryObjectiveType(id("star_chart"), false, 0));
    public static final String CHART_CONDITION = "craft_compass";

    private static final DivineNarrativeDeferredRegister DEFINITIONS = new DivineNarrativeDeferredRegister(MOD_ID);
    public static final DivineNarrativeRegistryObject<DivineStoryDefinition> FIRST_STAR_STORY =
        DEFINITIONS.story("first_star", ConnectedPantheonAddon::firstStar);
    public static final DivineNarrativeRegistryObject<DivineStoryDefinition> LAST_LANTERN_STORY =
        DEFINITIONS.story("last_lantern", ConnectedPantheonAddon::lastLantern);
    public static final DivineNarrativeRegistryObject<GodRelationshipDefinition> DAWN_AND_EVENING =
        DEFINITIONS.relationship("dawn_and_evening", ConnectedPantheonAddon::dawnAndEvening);
    private static boolean registered;

    private ConnectedPantheonAddon() {}

    /** Call once in the addon constructor, after attaching its ordinary god registries. */
    public static synchronized void register() {
        if (registered) return;
        DEFINITIONS.submit();
        StarChartEvents.register();
        registered = true;
    }

    public static DivineStoryDefinition firstStar() {
        StoryChronicle chronicle = StoryChronicle.builder()
            .premise("Vesper keeps the routes used by those who return after sunset. A missing mark on an old chart asks whether guidance should promise safety or admit uncertainty. This is an addon tale, not a new account of the Sundering.")
            .offering(vanilla("paper")).offering(vanilla("amethyst_shard"))
            .witness(StoryObjective.of(StoryObjectiveType.OBSERVE, "night_sky", 200,
                "Observe the open night sky for 200 consecutive ticks"))
            .reckoning(new StoryObjective(STAR_CHART, CHART_CONDITION, 1, vanilla("compass"),
                "Craft a vanilla compass once as a star chart after this chapter begins"))
            .chapter(chapter("call", NarrativeText.translatable("example_pantheon.story.first_star.call.title"), NarrativeText.translatable("example_pantheon.story.first_star.call.dialogue"),
                NarrativeText.translatable("example_pantheon.story.first_star.call.paragraph"),
                "Make an ordinary offering she accepts. Paper and amethyst are examples only if her offering rules accept them; this story does not change those rules. The old village belongs to her account, not a hidden structure you must find."))
            .chapter(chapter("witness", "2. A Witness", "Watch long enough to notice what the stars cannot tell you.",
                "Stand beneath an open night sky for two hundred uninterrupted ticks. A star can steady your bearing without showing the ground beneath your feet. Clouds do not create another objective, and no special moon is required.",
                "Vesper admits that she once mistook a confident direction for a safe journey. The sky witnessed her confidence, not the safety of those who followed it."))
            .chapter(chapter("testimony", "3. Divided Testimony", "Will you keep a shelter, or leave an honest record?",
                "A keeper wanted lanterns at the last known refuge. A surveyor wanted paper for a record of hazards beyond it. Neither asked you to condemn the other. Vesper offers two permanent paths: Shelter and Testimony.",
                "Shelter contributes four lanterns and records a Solar Ward specialization marker. Testimony contributes eight paper and records a Twilight Veil marker. These examples add no numerical blessing effect. Preview both rewards at her statue before confirming."))
            .chapter(chapter("deed_a", "4. The Chosen Deed", "A light may welcome without ordering anyone to stay.",
                "Contribute four lanterns through the story donation action at Vesper's statue. They stand for a refuge whose door remains open in both directions. You are not asked to place blocks or locate the remembered village.",
                "Vesper accepts Shelter without pretending that shelter finishes a journey. A resting place is a promise of care, not ownership of the traveler."))
            .chapter(chapter("deed_b", "4. The Chosen Deed", "Leave room on the page for an answer you do not have.",
                "Contribute eight paper through the story donation action at Vesper's statue. The record will distinguish a known road from a guessed one. No named book or collectible document is required.",
                "Vesper accepts Testimony without treating a careful record as rescue. The page can warn another traveler, but it cannot take that traveler's steps."))
            .chapter(chapter("reckoning_a", "5. The Reckoning", "Carry a bearing beyond the shelter you kept.",
                "Now craft a vanilla compass. Vesper calls this practical instrument a star chart: no custom item, name or recipe is needed. The new crafting event, not possession of an old compass, completes the reckoning.",
                "The needle is not a promise that every road is safe. Shelter becomes more honest when those leaving it receive guidance instead of a command to remain."))
            .chapter(chapter("reckoning_b", "5. The Reckoning", "Test the habit of making, not the beauty of a record.",
                "Now craft a vanilla compass. It serves as Vesper's star chart, using the normal item and recipe. Receiving a compass, taking one from a chest or renaming one is not this deed.",
                "You have made a tool without claiming that it knows every hazard. Testimony becomes useful when it equips a person to act rather than merely preserving your own account."))
            .chapter(chapter("trial", "6. The Trial", "Begin my trial anew; yesterday's victory cannot answer today.",
                "Return to Vesper's statue and start a new instance of her registered trial from its blessings screen. Win that fresh trial, then return to the statue. A victory earned before this chapter does not count.",
                "Failure does not undo your chosen path, donation or completed reckoning. Vesper asks you to try again, not to buy the same promise twice."))
            .chapter(chapter("ending_a", "7. The Return", "You kept a door, not a boundary around another life.",
                "Vesper places a lantern beside the unfinished chart. The blank road remains blank, but the refuge is no longer concealed by the cartographer's embarrassment. Those who rest there may leave without owing their keeper certainty.",
                "Your Shelter ending and its marker are recorded once. Vesper grants the ordinary story favor reward, not authority over another god or an automatic new blessing power."))
            .chapter(chapter("ending_b", "7. The Return", "You left a useful truth, including the edge of what you know.",
                "Vesper keeps the chart with its last road unfinished. Beside it she records what can be followed, what must be checked and what no witness can promise. The omission is no longer passed off as a complete map.",
                "Your Testimony ending and its marker are recorded once. The story's favor reward does not purchase another god's agreement, and the marker alone changes no blessing statistics."))
            .build();
        return DivineStoryDefinition.builder(VESPER).id(FIRST_STAR)
            .title(NarrativeText.translatable("example_pantheon.story.first_star.title"))
            .request("Offer something Vesper accepts, then learn what an honest guide can promise.")
            .branch("a", donation("Shelter", "lantern", 4, "Contribute four lanterns at Vesper's statue",
                "first_star_shelter", SOLAR_WARD, "Records the Shelter marker for Solar Ward; no numeric modifier is installed by this example",
                "You kept a refuge without claiming the road beyond it."))
            .branch("b", donation("Testimony", "paper", 8, "Contribute eight paper at Vesper's statue",
                "first_star_testimony", TWILIGHT_VEIL, "Records the Testimony marker for Twilight Veil; no numeric modifier is installed by this example",
                "You recorded a useful truth without inventing certainty."))
            .chronicle(chronicle).build();
    }

    public static DivineStoryDefinition lastLantern() {
        StoryChronicle chronicle = StoryChronicle.builder()
            .premise("A separate Vesper story asks who tends a light when its keeper leaves. It needs no ending from The First Star's Unfinished Map and stores its own permanent choice.")
            .offering(vanilla("paper")).offering(vanilla("amethyst_shard"))
            .witness(StoryObjective.of(StoryObjectiveType.OBSERVE, "day_sky", 100,
                "Observe the open daytime sky for 100 consecutive ticks"))
            .reckoning(StoryObjective.of(StoryObjectiveType.OBSERVE, "night_sky", 100,
                "Observe the open night sky for 100 consecutive ticks"))
            .chapter(chapter("call", "1. The Call", "A light is not its keeper.",
                "Vesper recalls a lantern tended until its keeper was too tired to lift it. Make an ordinary accepted offering to hear why gratitude became an obligation no one had agreed to carry."))
            .chapter(chapter("witness", "2. A Witness", "See the work that daylight lets you overlook.",
                "Observe the open daytime sky for one hundred consecutive ticks. A lantern can seem unnecessary at noon; the hands cleaning it still need rest. This memory introduces no new location to discover."))
            .chapter(chapter("testimony", "3. Divided Testimony", "Renew the light, or provide for the keeper's departure.",
                "Choose Renewal, a contribution of two lanterns, or Release, a contribution of four bread. Renewal records a Solar Ward marker; Release records a Twilight Veil marker. Neither marker implements numerical effects. This choice is independent of your other Vesper story."))
            .chapter(chapter("deed_a", "4. The Chosen Deed", "A replacement must not become another chain.",
                "Donate two lanterns at the statue using the story action. Vesper remembers the keeper teaching a willing successor, not assigning the task to the nearest grateful stranger."))
            .chapter(chapter("deed_b", "4. The Chosen Deed", "A farewell can be an act of care.",
                "Donate four bread at the statue using the story action. Vesper remembers preparing food for a departure without demanding a date of return."))
            .chapter(chapter("reckoning_a", "5. The Reckoning", "The night does not require the same hands forever.",
                "Observe the open night sky for one hundred consecutive ticks. Consider a light that endures because its care can be shared, not because a keeper is forbidden to stop."))
            .chapter(chapter("reckoning_b", "5. The Reckoning", "Let a departure remain a departure.",
                "Observe the open night sky for one hundred consecutive ticks. Do not turn the keeper's absence into a promise that someone else must repay."))
            .chapter(chapter("trial", "6. The Trial", "Meet this question in a fresh trial.",
                "Start and win a new Vesper trial from her statue's blessings screen after this reckoning. Earlier victories do not count. A failed attempt may be retried without repeating your donation."))
            .chapter(chapter("ending_a", "7. The Return", "You renewed the work without claiming its workers.",
                "Return to Vesper's statue to record Renewal. The last lantern has another keeper by consent, and the first keeper may rest. Your ending and marker are granted once, independently of the unfinished map."))
            .chapter(chapter("ending_b", "7. The Return", "You did not make kindness a debt.",
                "Return to Vesper's statue to record Release. The departing keeper receives food rather than a summons. Your ending and marker are granted once, and another god's story remains open."))
            .build();
        return DivineStoryDefinition.builder(VESPER).id(LAST_LANTERN)
            .title("The Last Lantern's Keeper")
            .request("Offer something Vesper accepts, then consider a keeper's right to leave.")
            .branch("a", donation("Renewal", "lantern", 2, "Contribute two lanterns at Vesper's statue",
                "last_lantern_renewal", SOLAR_WARD, "Records the Renewal marker for Solar Ward; no numeric modifier is installed by this example",
                "You renewed a shared task without binding its keeper."))
            .branch("b", donation("Release", "bread", 4, "Contribute four bread at Vesper's statue",
                "last_lantern_release", TWILIGHT_VEIL, "Records the Release marker for Twilight Veil; no numeric modifier is installed by this example",
                "You offered a farewell without creating a debt."))
            .chronicle(chronicle).build();
    }

    /** Same two-god/four-combination shape as the nine built-in relationships. */
    public static GodRelationshipDefinition dawnAndEvening() {
        return new GodRelationshipDefinition(id("dawn_and_evening"), List.of(SOLARIUS, VESPER),
            List.of(SOLARIUS, FIRST_STAR), "Between Dawn and Evening", "A difference in how guidance is offered",
            "Solarius and Vesper ask how a warning can protect without pretending to know every road. Their conversation is optional and changes neither independent story.",
            Map.of(
                "aa", List.of("Judgment and Shelter meet at an open door. Solarius will name a danger; Vesper will keep a place to recover. Neither calls a warning permission to own the traveler."),
                "ab", List.of("Judgment and Testimony compare the limits of their evidence. Solarius accepts that a clear verdict must not invent a missing witness; Vesper leaves the unknown road unfilled."),
                "ba", List.of("Sanctuary and Shelter offer two welcomes without making either exclusive. Bread and light sustain the traveler, but neither host demands allegiance as payment."),
                "bb", List.of("Sanctuary and Testimony refuse a comforting falsehood. Solarius offers care before agreement, while Vesper records uncertainty without using it as a reason to withhold help.")));
    }

    private static StoryChapterDefinition chapter(String key, String title, String dialogue, String... paragraphs) {
        return new StoryChapterDefinition(key, title, List.of(paragraphs), dialogue);
    }

    private static DivineStoryDefinition.Branch donation(String name, String item, int count, String deed,
            String reward, ResourceLocation blessing, String description, String ending) {
        return new DivineStoryDefinition.Branch(name, StoryObjectiveType.DONATION, "", count, vanilla(item), deed,
            BlessingSpecialization.builder(id(reward)).blessing(blessing).description(description).build(), ending);
    }

    private static ResourceLocation id(String path) { return new ResourceLocation(MOD_ID, path); }
    private static ResourceLocation engine(String path) { return new ResourceLocation("divinity_engine", path); }
    private static ResourceLocation vanilla(String path) { return new ResourceLocation("minecraft", path); }
}
