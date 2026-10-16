package net.nightshade.divinity_engine.client.innerworld;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionConfig;
import org.joml.Matrix4f;

/** Client-local backdrop only. No shared time/weather changes and no shader asset dependency. */
public final class InnerWorldSky extends DimensionSpecialEffects {
    public InnerWorldSky(){super(Float.NaN,false,SkyType.NONE,true,false);}
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float brightness){return !CommunionClient.active()||CommunionClient.motif().equals("void")?Vec3.ZERO:color;}
    @Override public boolean isFoggyAt(int x,int z){return false;}
    @Override public boolean renderSky(ClientLevel level,int ticks,float partial,PoseStack poses,Camera camera,Matrix4f projection,boolean foggy,Runnable setupFog){
        if(!CommunionClient.active()||CommunionClient.motif().equals("void"))return true;
        setupFog.run();int color=CommunionClient.color();String motif=CommunionClient.motif();
        float r=((color>>16)&255)/255F,g=((color>>8)&255)/255F,b=(color&255)/255F;
        RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var mesh=Tesselator.getInstance().getBuilder();var matrix=poses.last().pose();
        try{
            mesh.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(int i=0;i<32;i++){
                double a=i*Math.PI/16,a2=(i+1)*Math.PI/16;float x=(float)Math.cos(a)*110,z=(float)Math.sin(a)*110,x2=(float)Math.cos(a2)*110,z2=(float)Math.sin(a2)*110;
                mesh.vertex(matrix,x,-25,z).color(r*.48F,g*.48F,b*.48F,1).endVertex();mesh.vertex(matrix,x2,-25,z2).color(r*.48F,g*.48F,b*.48F,1).endVertex();mesh.vertex(matrix,x2,75,z2).color(r*.08F,g*.08F,b*.08F,1).endVertex();mesh.vertex(matrix,x,75,z).color(r*.08F,g*.08F,b*.08F,1).endVertex();
            }
            boolean stars=motif.equals("stars")||motif.equals("dream")||motif.equals("time")||motif.equals("aurora");
            if(stars){
                double rotation=CommunionConfig.MOTION.get()?(ticks+partial)*.0001:0;
                for(int i=0;i<64;i++){double a=i*2.399963+rotation;float x=(float)Math.cos(a)*(24+i%8*6),z=(float)Math.sin(a)*(24+i%8*6);float y=40+i%17;
                    quad(mesh,matrix,x-.3F,y,z-.3F,x+.3F,y,z+.3F,.85F,.9F,1,1);}
            }
            if(motif.equals("sun")||motif.equals("light"))quad(mesh,matrix,-13,65,-65,13,65,-39,1,.88F,.45F,1);
            if(motif.equals("storm")||motif.equals("sea")||motif.equals("wind"))for(int i=0;i<12;i++){
                float x=-90+i*16,z=-50+(i%3)*35;quad(mesh,matrix,x,26+i%3,z,x+26,26+i%3,z+12,.5F,.62F,.72F,.65F);
            }
            if(motif.equals("aurora"))for(int i=0;i<20;i++)quad(mesh,matrix,-75+i*7,40+i%4,-30,-65+i*7,40+i%4,-22,.25F,.8F,1,.45F);
            var frame=new InnerWorldPresentationRegistry.Frame(ticks+partial,color,CommunionConfig.FLASHING.get(),CommunionConfig.MOTION.get());
            for(var mark:InnerWorldPresentationRegistry.marks(motif,frame)){
                var c=mark.center();float half=mark.size()/2;int rgb=mark.color();
                quad(mesh,matrix,(float)c.x-half,(float)c.y,(float)c.z-half,(float)c.x+half,(float)c.y,(float)c.z+half,
                    ((rgb>>16)&255)/255F,((rgb>>8)&255)/255F,(rgb&255)/255F,mark.alpha());
            }
            // Optional distant lightning, never a full-screen flash.
            if(motif.equals("storm")&&!CommunionConfig.FLASHING.get()&&ticks%300<3)for(int i=0;i<5;i++){
                float x=45+i%2*2,y=8+i*5;mesh.vertex(matrix,x,y,-70).color(.7F,.85F,1,1).endVertex();mesh.vertex(matrix,x+1,y,-70).color(.7F,.85F,1,1).endVertex();mesh.vertex(matrix,x+3,y+5,-70).color(.7F,.85F,1,1).endVertex();mesh.vertex(matrix,x+2,y+5,-70).color(.7F,.85F,1,1).endVertex();
            }
            BufferUploader.drawWithShader(mesh.end());
        }finally{RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.depthMask(true);RenderSystem.setShaderColor(1,1,1,1);}
        return true;
    }
    private static void quad(BufferBuilder b,Matrix4f m,float x,float y,float z,float x2,float y2,float z2,float r,float g,float blue,float a){b.vertex(m,x,y,z).color(r,g,blue,a).endVertex();b.vertex(m,x2,y,z).color(r,g,blue,a).endVertex();b.vertex(m,x2,y2,z2).color(r,g,blue,a).endVertex();b.vertex(m,x,y2,z2).color(r,g,blue,a).endVertex();}
}
