package example.pantheon;

import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.api.narrative.*;

/**
 * Call register() from your addon constructor after creating your own god registry.
 * This example assumes example_pantheon:vesper and example_pantheon:evening_ward exist.
 */
public final class ExamplePantheonAddon {
    private static final ResourceLocation GOD = new ResourceLocation("example_pantheon", "vesper");
    private static final DivineNarrativeDeferredRegister DEFINITIONS = new DivineNarrativeDeferredRegister("example_pantheon");
    public static final DivineNarrativeRegistryObject<DivineStoryDefinition> STORY = DEFINITIONS.story("vespers_promise", () ->
        DivineStoryDefinition.builder(GOD).title("Vesper's Promise").request("Bring a gift to the evening, then choose what you will keep.")
            .branch("a", path("Shelter", "lantern", 4, "Your Evening Ward lasts two seconds longer", "You kept a small light for those returning late."))
            .branch("b", path("Remembrance", "paper", 8, "Your Evening Ward also grants a brief Speed effect", "You preserved the names that the day forgot.")).build());
    public static final DivineNarrativeRegistryObject<GodPresenceDefinition> PRESENCE = DEFINITIONS.presence("vesper", () ->
        GodPresenceDefinition.builder(GOD).color(0x8AAFC4)
            .texture(new ResourceLocation("minecraft", "textures/particle/glitter_0.png"))
            .lines("greeting", "The evening has room for you.", "Welcome back beneath the first star.", "What has the day left in your hands?")
            .lines("prayer", "Rest your hurried thoughts.", "Let the quiet answer.", "No honest word is too small.")
            .lines("offering", "A kindness kept for evening.", "I accept the care within your gift.", "This will not be forgotten.").build());

    public static void register() { DEFINITIONS.submit(); }
    private static DivineStoryDefinition.Branch path(String name, String item, int count, String reward, String ending) {
        String branch = name.equals("Shelter") ? "a" : "b";
        return new DivineStoryDefinition.Branch(name, StoryObjectiveType.DONATION, "", count,
            new ResourceLocation("minecraft", item), "Contribute " + count + " " + item,
            BlessingSpecialization.builder(new ResourceLocation("example_pantheon", "vesper_" + branch))
                .blessing(new ResourceLocation("example_pantheon", "evening_ward")).description(reward).build(), ending);
    }
}
