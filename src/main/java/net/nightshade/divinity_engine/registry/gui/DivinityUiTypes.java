package net.nightshade.divinity_engine.registry.gui;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.renderlib.api.RenderLibRegistries;

/** Common descriptors only: safe to load on a dedicated server. */
public final class DivinityUiTypes {
    public static final ResourceLocation GALAXY = new ResourceLocation("divinity_engine", "blessings_galaxy");
    public static final ResourceLocation CELESTIAL_CONTROL = new ResourceLocation("divinity_engine", "celestial_control");
    public static final ResourceLocation CELESTIAL_FRAME = new ResourceLocation("divinity_engine", "celestial_frame");
    public static final ResourceLocation DIVINE_SEAL = new ResourceLocation("divinity_engine", "divine_seal");
    public static final ResourceLocation CELESTIAL_INVENTORY = new ResourceLocation("divinity_engine", "celestial_inventory");
    public static final ResourceLocation CELESTIAL_VAULT = new ResourceLocation("divinity_engine", "celestial_vault");
    public static final ResourceLocation SHRINE_STONE = new ResourceLocation("divinity_engine", "shrine_stone");
    public static final ResourceLocation SHRINE_SIGIL = new ResourceLocation("divinity_engine", "shrine_sigil");
    public static final ResourceLocation SHRINE_CONTROL = new ResourceLocation("divinity_engine", "shrine_control");

    private DivinityUiTypes() {}

    public static void register() {
        RenderLibRegistries.registerUiNode(GALAXY, () -> MapCodec.unit("galaxy"));
        RenderLibRegistries.registerMaterial(GALAXY, () -> MapCodec.unit("galaxy"));
        RenderLibRegistries.registerUiNode(CELESTIAL_CONTROL, () -> MapCodec.unit("celestial_control"));
        RenderLibRegistries.registerUiNode(CELESTIAL_FRAME, () -> MapCodec.unit("celestial_frame"));
        RenderLibRegistries.registerUiNode(DIVINE_SEAL, () -> MapCodec.unit("divine_seal"));
        RenderLibRegistries.registerUiNode(CELESTIAL_INVENTORY, () -> MapCodec.unit("celestial_inventory"));
        RenderLibRegistries.registerUiNode(CELESTIAL_VAULT, () -> MapCodec.unit("celestial_vault"));
        RenderLibRegistries.registerUiNode(SHRINE_STONE, () -> MapCodec.unit("shrine_stone"));
        RenderLibRegistries.registerUiNode(SHRINE_SIGIL, () -> MapCodec.unit("shrine_sigil"));
        RenderLibRegistries.registerUiNode(SHRINE_CONTROL, () -> MapCodec.unit("shrine_control"));
    }
}
