package net.nightshade.divinity_engine.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.api.narrative.StoryObjectiveType;
import net.nightshade.divinity_engine.util.divinity.narrative.*;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@GameTestHolder("divinity_engine")
@PrefixGameTestTemplate(false)
public final class NarrativeReloadGameTests {
    @GameTest(template = "pocket_dimension")
    public static void malformedPackRetainsEntireCatalog(GameTestHelper h) throws Exception {
        DivineNarrativeRegistry.initialize();
        var before = DivineNarrativeRegistry.byId(DivineNarrativeRegistry.id("solarius"));
        long revision = DivineNarrativeRegistry.revision();
        var listener = new NarrativeReloadListener();
        var prepare = NarrativeReloadListener.class.getDeclaredMethod("prepare", ResourceManager.class, ProfilerFiller.class);
        var apply = NarrativeReloadListener.class.getDeclaredMethod("apply", NarrativeReloadListener.Prepared.class, ResourceManager.class, ProfilerFiller.class);
        prepare.setAccessible(true); apply.setAccessible(true);
        for (String invalid : new String[]{"{", "[]", "{\"version\":2,\"version\":1}", "{\"version\":2,\"kind\":\"chronicle\",\"story\":\"divinity_engine:solarius\"}"}) {
            ResourceManager manager = resources(invalid);
            var prepared = (NarrativeReloadListener.Prepared) prepare.invoke(listener, manager, InactiveProfiler.INSTANCE);
            apply.invoke(listener, prepared, manager, InactiveProfiler.INSTANCE);
            h.assertTrue(DivineNarrativeRegistry.revision() == revision, "Rejected reload publishes no partial revision");
            h.assertTrue(DivineNarrativeRegistry.byId(before.id()) == before, "Rejected reload keeps exact last valid definition");
        }
        h.succeed();
    }

    @GameTest(template = "pocket_dimension")
    public static void chapterTitlesAndLocalizedProseSurviveSnapshots(GameTestHelper h) {
        var original = DivineNarrativeRegistry.byId(DivineNarrativeRegistry.id("solarius"));
        var json = com.google.gson.JsonParser.parseString(NarrativeDefinitionJson.encodeStory(original)).getAsJsonObject();
        json.getAsJsonObject("chronicle").getAsJsonObject("chapter_titles").addProperty("call", "A custom chapter title");
        var title = new com.google.gson.JsonObject(); title.addProperty("translate", "addon.story.title"); json.add("title", title);
        var decoded = NarrativeDefinitionJson.story(json);
        var restored = NarrativeDefinitionJson.decodeStory(NarrativeDefinitionJson.encodeStory(decoded));
        h.assertTrue(restored.equals(decoded), "Full accepted story including titles round-trips");
        h.assertTrue(restored.chronicle().passages().get("call").title().equals("A custom chapter title"), "Custom chapter title preserved");
        h.assertTrue(restored.title().equals("{{lang:addon.story.title}}"), "Translation key remains unresolved on server");
        for (StoryObjectiveType type : new StoryObjectiveType[]{StoryObjectiveType.KILL, StoryObjectiveType.PRAY, StoryObjectiveType.OBSERVE}) {
            boolean rejected = false;
            try { type.validateCondition("unsupported_condition"); } catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected, "Impossible built-in condition is rejected for " + type.id());
        }
        h.succeed();
    }

    private static ResourceManager resources(String json) {
        Resource resource = new Resource(null, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        return (ResourceManager) Proxy.newProxyInstance(ResourceManager.class.getClassLoader(), new Class<?>[]{ResourceManager.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("listResources")) return Map.of(new ResourceLocation("connected_test", "divine_narratives/broken.json"), resource);
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
