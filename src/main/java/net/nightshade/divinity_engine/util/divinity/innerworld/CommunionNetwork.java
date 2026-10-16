package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.nightshade.divinity_engine.api.innerworld.InnerWorldDefinition;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.util.divinity.narrative.*;
import java.util.*;
import java.util.function.Supplier;

public final class CommunionNetwork {
    public record Request(int action,ResourceLocation god,UUID session,int revision,ResourceLocation story,String verb,String branch,String token){
        public Request(FriendlyByteBuf b){this(b.readUnsignedByte(),b.readResourceLocation(),b.readUUID(),b.readVarInt(),b.readResourceLocation(),b.readUtf(16),b.readUtf(1),b.readUtf(36));}
        public void encode(FriendlyByteBuf b){b.writeByte(action);b.writeResourceLocation(god);b.writeUUID(session);b.writeVarInt(revision);b.writeResourceLocation(story);b.writeUtf(verb,16);b.writeUtf(branch,1);b.writeUtf(token,36);}
        public static Request entry(ResourceLocation god,int action){return new Request(action,god,new UUID(0,0),0,god,"","","");}
        public static void handle(Request r,Supplier<NetworkEvent.Context> supplier){var ctx=supplier.get();if(ctx.getDirection().getReceptionSide().isServer())ctx.enqueueWork(()->{
            var p=ctx.getSender();if(p==null)return;
            // Safety returns are not rate-limited behind dialogue spam.
            if(r.action==2){var s=CommunionManager.session(p);if(s!=null&&s.id.equals(r.session))CommunionManager.end(p,CommunionLedger.Exit.NORMAL);return;}
            if(!CommunionManager.rateLimit(p))return;
            switch(r.action){case 0->CommunionManager.inspect(p,r.god);case 1->CommunionManager.begin(p,r.god);case 3->CommunionManager.storyAction(p,r.session,r.revision,r.story,r.verb,r.branch,r.token);default->{}}
        });ctx.setPacketHandled(true);}
    }
    public record StatePacket(CompoundTag state,boolean open){
        public StatePacket(FriendlyByteBuf b){this(Objects.requireNonNullElseGet(b.readNbt(),CompoundTag::new),b.readBoolean());}
        public void encode(FriendlyByteBuf b){b.writeNbt(state);b.writeBoolean(open);}
        public static void handle(StatePacket p,Supplier<NetworkEvent.Context> supplier){var ctx=supplier.get();if(ctx.getDirection().getReceptionSide().isClient())ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->net.nightshade.divinity_engine.client.innerworld.CommunionClient.receive(p.state,p.open)));ctx.setPacketHandled(true);}
    }
    public static void register(){ModMessages.addNetworkMessage(Request.class,Request::encode,Request::new,Request::handle);ModMessages.addNetworkMessage(StatePacket.class,StatePacket::encode,StatePacket::new,StatePacket::handle);}
    public static void clear(ServerPlayer p){ModMessages.sendToPlayer(new StatePacket(new CompoundTag(),false),p);}
    public static void offer(ServerPlayer p,ResourceLocation god,String error){
        var t=new CompoundTag();var d=InnerWorldsRegistry.get(god);t.putString("God",god.toString());t.putString("Name",d==null?god.toString():d.name);t.putString("State","OFFER");t.putString("Eligibility",error);
        t.putInt("Color",d==null?0xAACCEE:d.visuals.color());ModMessages.sendToPlayer(new StatePacket(t,true),p);
    }
    public static void state(ServerPlayer p,CommunionLedger.Record r,InnerWorldDefinition d,InnerWorldDefinition.Visuals visual,boolean open){
        var t=new CompoundTag();t.putUUID("Session",r.id);t.putInt("Revision",r.revision);t.putString("State",r.state.name());t.putString("God",r.god.toString());t.putString("Name",d.name);
        t.putString("Variant",r.variant);t.putString("Exit",r.exit.name());t.putInt("Color",visual.color());t.putString("Sound",visual.sound().toString());t.putString("Particle",visual.particle().toString());t.putString("Motif",visual.motif());
        t.putInt("Remaining",(int)Math.max(0,CommunionConfig.DURATION.get()-(p.server.overworld().getGameTime()-r.activeAt)));
        if(open&&r.state==CommunionLedger.State.ACTIVE){
            var data=DivineNarrativeCapability.get(p);var list=new ListTag();int count=0;
            for(var registered:DivineNarrativeRegistry.forGod(r.god)){
                if(++count>32||data==null)break;var m=data.story(registered);var def=m.definition(registered);if(def==null)continue;
                var s=new CompoundTag();s.putString("Id",def.id().toString());s.putString("Title",def.title());s.putString("Stage",m.story.stage().name());
                s.putBoolean("Compatible",d.compatibleStories.contains(def.id()));s.putString("Branch",m.story.branch());
                String text=def.request();var passage=def.chronicle().passages().get(m.story.chapter());if(passage!=null)text=passage.dialogue();
                var path=DivineNarrativeManager.path(def,m);if(path!=null&&m.story.activeObjective())text+="\n\n"+path.deed()+" ("+m.story.progress()+"/"+path.target()+")";
                text+="\n\nOfferings, prayer, trials and final shrine turn-ins require a physical statue.";
                s.putString("Text",text.substring(0,Math.min(text.length(),4000)));
                for(String branch:List.of("a","b")){var b=def.branches().get(branch);s.putString("Name_"+branch,b.name());s.putString("Reward_"+branch,b.reward().description());}
                if(m.previewExpires>=p.server.overworld().getGameTime()&&m.preview.length()>=38){s.putString("Token",m.preview.substring(0,36));s.putString("Preview",m.preview.substring(37));}
                list.add(s);
            }t.put("Stories",list);
        }
        ModMessages.sendToPlayer(new StatePacket(t,open),p);
    }
    private CommunionNetwork(){}
}
