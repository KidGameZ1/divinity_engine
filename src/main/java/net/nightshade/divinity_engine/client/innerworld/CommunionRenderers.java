package net.nightshade.divinity_engine.client.innerworld;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.entity.*;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionBootstrap;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager;

@Mod.EventBusSubscriber(modid="divinity_engine",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CommunionRenderers {
    @SubscribeEvent public static void sky(RegisterDimensionSpecialEffectsEvent e){e.register(new ResourceLocation("divinity_engine","inner_world"),new InnerWorldSky());}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(CommunionBootstrap.BODY.get(),Body::new);e.registerEntityRenderer(CommunionBootstrap.GOD.get(),Manifestation::new);}
    private static final class Body extends MobRenderer<TranceBodyEntity,PlayerModel<TranceBodyEntity>> {
        private final PlayerModel<TranceBodyEntity> standard,slim;
        Body(EntityRendererProvider.Context c){
            super(c,new MeditatingModel(c.bakeLayer(ModelLayers.PLAYER),false),.35F);standard=model;slim=new MeditatingModel(c.bakeLayer(ModelLayers.PLAYER_SLIM),true);
            addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),c.getModelManager()));
        }
        private net.minecraft.client.multiplayer.PlayerInfo info(TranceBodyEntity e){var connection=Minecraft.getInstance().getConnection();return connection==null||e.owner()==null?null:connection.getPlayerInfo(e.owner());}
        @Override public ResourceLocation getTextureLocation(TranceBodyEntity e){var info=info(e);return info==null?DefaultPlayerSkin.getDefaultSkin(e.owner()==null?e.getUUID():e.owner()):info.getSkinLocation();}
        @Override public void render(TranceBodyEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){var info=info(e);model=info!=null&&info.getModelName().equals("slim")?slim:standard;super.render(e,yaw,partial,pose,buffers,light);}
    }
    private static final class MeditatingModel extends PlayerModel<TranceBodyEntity>{
        MeditatingModel(net.minecraft.client.model.geom.ModelPart root,boolean slim){super(root,slim);}
        @Override public void setupAnim(TranceBodyEntity entity,float swing,float amount,float age,float yaw,float pitch){riding=true;super.setupAnim(entity,0,0,age,0,10);rightArm.xRot=-.65F;leftArm.xRot=-.65F;rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);}
    }
    private static final class Manifestation extends EntityRenderer<GodManifestationEntity>{
        Manifestation(EntityRendererProvider.Context c){super(c);}
        @Override public ResourceLocation getTextureLocation(GodManifestationEntity e){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
        @Override public void render(GodManifestationEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            var id=ResourceLocation.tryParse(e.god());var god=id==null?null:DivineNarrativeManager.god(id);var block=god==null?Blocks.CHISELED_QUARTZ_BLOCK:god.getStatueBlock();if(block==null)block=Blocks.CHISELED_QUARTZ_BLOCK;
            pose.pushPose();pose.translate(-.5,0,-.5);Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
        }
    }
    private CommunionRenderers(){}
}
