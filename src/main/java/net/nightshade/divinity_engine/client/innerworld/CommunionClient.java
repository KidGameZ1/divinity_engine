package net.nightshade.divinity_engine.client.innerworld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.util.divinity.innerworld.*;
import net.nightshade.divinity_engine.client.ui.ShrineUi;
import net.nightshade.divinity_engine.network.messages.ModMessages;

@Mod.EventBusSubscriber(modid="divinity_engine",value=Dist.CLIENT)
public final class CommunionClient {
    private static CompoundTag state=new CompoundTag();
    private static boolean pendingOpen;
    private static int transition;
    public static void receive(CompoundTag tag,boolean open){
        var oldStories=state.getList("Stories",10);boolean same=state.hasUUID("Session")&&tag.hasUUID("Session")&&state.getUUID("Session").equals(tag.getUUID("Session"));
        if(!state.getString("State").equals("ACTIVE")&&tag.getString("State").equals("ACTIVE"))transition=8;
        state=tag.copy();if(same&&!tag.contains("Stories"))state.put("Stories",oldStories.copy());pendingOpen|=open;
        var mc=Minecraft.getInstance();String status=tag.getString("State");
        if(tag.isEmpty()||status.equals("RETURNING")||status.equals("CLEANUP")){
            pendingOpen=false;transition=0;if(mc.screen instanceof CommunionScreen)mc.setScreen(null);
            if(status.equals("RETURNING"))state=new CompoundTag();
        }else if(mc.screen instanceof CommunionScreen screen){
            screen.update(state);
            // An ACTIVE update can arrive before the dimension switch finishes.
            if(!status.equals("ACTIVE")||active())pendingOpen=false;
        }
    }
    public static void open(ResourceLocation god){ModMessages.sendToServer(CommunionNetwork.Request.entry(god,0));}
    public static boolean active(){var mc=Minecraft.getInstance();return state.getString("State").equals("ACTIVE")&&mc.level!=null&&mc.level.dimension().equals(DimensionRegistry.INNER_WORLD);}
    public static int color(){return state.getInt("Color");}
    public static String motif(){return state.getString("Motif");}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){state=new CompoundTag();pendingOpen=false;transition=0;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        // The receiving-level screen closes itself on arrival; do not let it consume the dialogue.
        if(pendingOpen&&mc.getOverlay()==null
                &&!(mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)
                &&(!state.getString("State").equals("ACTIVE")||active())){
            pendingOpen=false;mc.setScreen(new CommunionScreen(state));
        }
        if(!active())return;
        if(transition>0)transition--;
        if(!motif().equals("void")&&mc.player.tickCount%20==0&&!CommunionConfig.PARTICLES.get()){
            var type=ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation.tryParse(state.getString("Particle")));
            if(type instanceof SimpleParticleType particle)for(int i=0;i<4;i++){double x=mc.player.getX()+mc.level.random.nextDouble()*14-7,z=mc.player.getZ()+mc.level.random.nextDouble()*14-7;mc.level.addParticle(particle,x,mc.player.getY()+2+mc.level.random.nextDouble()*5,z,0,.01,0);}
        }
        if(mc.player.tickCount%120==0){var sound=ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse(state.getString("Sound")));if(sound!=null)mc.level.playLocalSound(mc.player.getX(),mc.player.getY(),mc.player.getZ(),sound,SoundSource.AMBIENT,CommunionConfig.VOLUME.get().floatValue(),1,false);}
    }
    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor e){
        var level=Minecraft.getInstance().level;
        if(level==null||!level.dimension().equals(DimensionRegistry.INNER_WORLD))return;
        int c=active()&&!motif().equals("void")?color():0;
        e.setRed(((c>>16)&255)/510F);e.setGreen(((c>>8)&255)/510F);e.setBlue((c&255)/510F);
    }
    @SubscribeEvent public static void overlay(RenderGuiEvent.Post e){if(active()){var mc=Minecraft.getInstance();if(!motif().equals("void")&&transition>0&&CommunionConfig.TRANSITIONS.get())e.getGuiGraphics().fill(0,0,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),(transition*3<<24)|(color()&0xFFFFFF));e.getGuiGraphics().drawCenteredString(mc.font,net.minecraft.network.chat.Component.translatable("gui.divinity_engine.communion.return_hint",state.getString("Name")),mc.getWindow().getGuiScaledWidth()/2,8,ShrineUi.GOLD);}}
    @SubscribeEvent public static void pauseMenu(ScreenEvent.Init.Post e){if(active()&&e.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen){Screen screen=e.getScreen();e.addListener(new ShrineUi.StoneButton(screen.width/2-100,screen.height-28,200,20,net.minecraft.network.chat.Component.literal("Return to Body"),b->ModMessages.sendToServer(new CommunionNetwork.Request(2,new ResourceLocation(state.getString("God")),state.getUUID("Session"),state.getInt("Revision"),new ResourceLocation(state.getString("God")),"","",""))));}}
    private CommunionClient(){}
}
