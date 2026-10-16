package net.nightshade.divinity_engine.api.gods;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Low-priority, in-memory defaults; addon assets/datapacks can replace any individual file. */
public final class StatueResources extends AbstractPackResources {
    private final PackType type;
    private final Map<ResourceLocation, byte[]> files = new LinkedHashMap<>();

    public StatueResources(String id, PackType type, Collection<? extends GodStatueRegistration<?>> statues) {
        super(id, true);
        this.type = type;
        for (var statue : statues) {
            var location = statue.id();
            String path = location.getPath();
            if (type == PackType.CLIENT_RESOURCES) {
                JsonObject variants = new JsonObject();
                String[] directions = {"north", "east", "south", "west"};
                for (int i = 0; i < directions.length; i++) {
                    JsonObject variant = new JsonObject();
                    variant.addProperty("model", location.getNamespace() + ":block/" + path);
                    if (i > 0) variant.addProperty("y", i * 90);
                    variants.add("facing=" + directions[i], variant);
                }
                JsonObject state = new JsonObject();
                state.add("variants", variants);
                put(location, "blockstates/" + path + ".json", state.toString());
                put(location, "models/block/" + path + ".json", "{\"parent\":\"divinity_engine:block/statue\"}");
                put(location, "models/item/" + path + ".json",
                        "{\"parent\":\"" + location.getNamespace() + ":block/" + path + "\"}");
            } else {
                put(location, "loot_tables/blocks/" + path + ".json", """
                        {"type":"minecraft:block","pools":[{"rolls":1,"entries":[
                          {"type":"minecraft:item","name":"%s"}],
                          "conditions":[{"condition":"minecraft:survives_explosion"}]}]}
                        """.formatted(location));
            }
        }
    }

    static void addPack(AddPackFindersEvent event, String namespace, Collection<GodStatueRegistration<?>> statues) {
        if (statues.isEmpty()) return;
        var snapshot = java.util.List.copyOf(statues);
        event.addRepositorySource(consumer -> {
            String id = "divinity_statues/" + namespace;
            Pack pack = Pack.readMetaAndCreate(id, Component.literal("Automatic statues: " + namespace), true,
                    name -> new StatueResources(name, event.getPackType(), snapshot), event.getPackType(),
                    // Required-pack insertion honors BOTTOM; automatic discovery appends at the top.
                    Pack.Position.BOTTOM, PackSource.create(PackSource.NO_DECORATION, false));
            if (pack != null) consumer.accept(pack);
        });
    }

    private void put(ResourceLocation statue, String path, String json) {
        // Never shadow a mod's shipped asset, even if a pack selector reorders built-in packs.
        if (ModList.get().getModFiles().stream().anyMatch(mod -> Files.isRegularFile(
                mod.getFile().findResource(type.getDirectory(), statue.getNamespace(), path)))) return;
        files.put(ResourceLocation.fromNamespaceAndPath(statue.getNamespace(), path), json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length != 1 || !"pack.mcmeta".equals(elements[0])) return null;
        byte[] metadata = ("{\"pack\":{\"description\":\"Automatic statue defaults\",\"pack_format\":"
                + SharedConstants.getCurrentVersion().getPackVersion(type) + "}}").getBytes(StandardCharsets.UTF_8);
        return () -> new ByteArrayInputStream(metadata);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType requested, ResourceLocation location) {
        byte[] bytes = requested == type ? files.get(location) : null;
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType requested, String namespace, String path, ResourceOutput output) {
        if (requested != type) return;
        String prefix = path.isEmpty() ? "" : path + "/";
        files.forEach((location, bytes) -> {
            if (location.getNamespace().equals(namespace) && location.getPath().startsWith(prefix))
                output.accept(location, () -> new ByteArrayInputStream(bytes));
        });
    }

    @Override
    public Set<String> getNamespaces(PackType requested) {
        return requested != type ? Set.of() : files.keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toSet());
    }

    @Override public boolean isHidden() { return true; }
    @Override public void close() { }
}
