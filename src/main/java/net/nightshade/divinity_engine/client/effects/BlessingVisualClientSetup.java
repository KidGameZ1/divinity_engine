package net.nightshade.divinity_engine.client.effects;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.renderlib.client.RenderLibShaders;

@Mod.EventBusSubscriber(modid = DivinityEngineMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlessingVisualClientSetup {
    private BlessingVisualClientSetup() { }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> { BlessingEffectRenderer.register(); });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shaders(RegisterShadersEvent event) {
        // Alpha blending preserves god colors against bright skies; the shader samples pixel centers.
        RenderLibShaders.registerParticleMaterialShader(event, BlessingVisuals.PIXEL_MATERIAL,
                new ResourceLocation(DivinityEngineMod.MODID, "blessing_pixel"));
    }
}
