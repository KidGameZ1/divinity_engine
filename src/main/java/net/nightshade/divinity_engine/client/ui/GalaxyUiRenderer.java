package net.nightshade.divinity_engine.client.ui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.renderlib.client.RenderLibRenderTypes;
import net.nightshade.renderlib.client.RenderLibShaders;
import net.nightshade.renderlib.client.ui.CustomUiNodeRenderContext;
import net.nightshade.renderlib.client.ui.CustomUiNodeRendererRegistry;
import net.nightshade.renderlib.ui.UiRect;
import net.nightshade.renderlib.ui.UiValue;
import org.joml.Matrix4f;

import java.util.Random;

/** One screen-local Nightshade material pass, with a bounded mesh fallback. No world particles. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GalaxyUiRenderer {
    private static final ResourceLocation FALLBACK = new ResourceLocation("nightshade_core", "translucent");
    private static final float[][] STARS = new float[48][4];
    private static final long START = Util.getMillis();

    static {
        Random random = new Random(0xC05C05L);
        for (float[] star : STARS) {
            for (int i = 0; i < star.length; i++) star[i] = random.nextFloat();
        }
    }

    private GalaxyUiRenderer() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CustomUiNodeRendererRegistry.register(DivinityUiTypes.GALAXY, GalaxyUiRenderer::render);
            CelestialUiRenderer.register();
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shaders(RegisterShadersEvent event) {
        RenderLibShaders.registerMaterialShader(event, DivinityUiTypes.GALAXY, DivinityUiTypes.GALAXY);
    }

    private static void render(CustomUiNodeRenderContext context) {
        UiRect rect = context.bounds();
        if (rect.width() < 1 || rect.height() < 1) return;
        GuiGraphics graphics = context.graphics();
        // Follow Minecraft's accessibility preference: zero screen effects freezes all motion.
        boolean animate = Minecraft.getInstance().options.screenEffectScale().get() > 0.0
                && context.node().properties().getOrDefault("animate", UiValue.bool(true)).asBoolean().orElse(true);
        float seconds = animate ? (Util.getMillis() - START) / 1000.0F : 0;
        int tint = context.node().properties().getOrDefault("god_color", UiValue.integer(0x86C9DF)).asInteger().orElse(0x86C9DF);
        graphics.flush();
        if (RenderLibShaders.customAvailable(DivinityUiTypes.GALAXY)) {
            ShaderInstance shader = RenderLibShaders.forMaterial(DivinityUiTypes.GALAXY);
            if (shader.getUniform("UiTime") != null) shader.getUniform("UiTime").set(seconds);
            if (shader.getUniform("UiSize") != null) shader.getUniform("UiSize").set((float) rect.width(), (float) rect.height());
            if (shader.getUniform("GodTint") != null) shader.getUniform("GodTint").set(
                    ((tint >> 16) & 255) / 255.0F, ((tint >> 8) & 255) / 255.0F, (tint & 255) / 255.0F);
            VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderLibRenderTypes.quads(DivinityUiTypes.GALAXY, true));
            Matrix4f pose = graphics.pose().last().pose();
            // RG carry the normalized panel coordinates through Nightshade's POSITION_COLOR format.
            vertex(vertices, pose, rect.x(), rect.y(), 0xFF000000);
            vertex(vertices, pose, rect.x(), rect.y() + rect.height(), 0xFF00FF00);
            vertex(vertices, pose, rect.x() + rect.width(), rect.y() + rect.height(), 0xFFFFFF00);
            vertex(vertices, pose, rect.x() + rect.width(), rect.y(), 0xFFFF0000);
            graphics.flush();
        } else {
            fallback(graphics, rect, seconds);
        }
    }

    private static void fallback(GuiGraphics graphics, UiRect rect, float seconds) {
        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), 0xFF080D18);
        graphics.flush();
        var vertices = graphics.bufferSource().getBuffer(RenderLibRenderTypes.quads(FALLBACK, true));
        Matrix4f pose = graphics.pose().last().pose();
        // A bounded tiled texture fallback, never a second animated particle system.
        for (int y = 0; y < 16; y++) {
            for (int x = 8; x < 24; x++) {
                int px = rect.x() + x * rect.width() / 24;
                int py = rect.y() + y * rect.height() / 16;
                int nextX = rect.x() + (x + 1) * rect.width() / 24;
                int nextY = rect.y() + (y + 1) * rect.height() / 16;
                int shade = (int) ((Math.sin(x * .51 + Math.cos(y * .37)) + Math.cos(y * .45)) * 2 + 4);
                quad(vertices, pose, px, py, nextX - px, nextY - py, (shade * 3 << 24) | 0x355578);
            }
        }
        for (float[] star : STARS) {
            int x = rect.x() + 2 + (int) (star[0] * Math.max(1, rect.width() - 4));
            int y = rect.y() + 2 + (int) (star[1] * Math.max(1, rect.height() - 4));
            int alpha = 70 + (int) ((Math.sin(seconds * .25F + star[2] * 6.28F) + 1) * 12);
            int color = (alpha << 24) | 0x8CADCE;
            quad(vertices, pose, x, y, 1, 1, color);
            if (star[3] > .96F) {
                quad(vertices, pose, x - 1, y, 3, 1, 0x285E809E);
                quad(vertices, pose, x, y - 1, 1, 3, 0x285E809E);
            }
        }
        graphics.flush();
    }

    private static void quad(VertexConsumer vertices, Matrix4f pose, float x, float y, float width, float height, int color) {
        vertex(vertices, pose, x, y, color);
        vertex(vertices, pose, x, y + height, color);
        vertex(vertices, pose, x + width, y + height, color);
        vertex(vertices, pose, x + width, y, color);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, int color) {
        vertices.vertex(pose, x, y, 0).color((color >> 16) & 255, (color >> 8) & 255, color & 255, color >>> 24).endVertex();
    }
}
