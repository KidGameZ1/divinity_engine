package net.nightshade.divinity_engine.client.innerworld;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.client.ui.*;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionNetwork;
import net.nightshade.renderlib.client.ui.RetainedUiTree;
import java.util.*;

/** A non-pausing, compact stone/sigil conversation, with an always-visible safety exit. */
public final class CommunionScreen extends Screen {
    private CompoundTag data;
    private int storyIndex,scroll,left,top,w,h;
    private final RetainedUiTree tree=new RetainedUiTree(DivinityUi.canvas("communion").build());
    public CommunionScreen(CompoundTag data){super(Component.translatable("gui.divinity_engine.communion"));this.data=data.copy();}
    public void update(CompoundTag tag){boolean changed=!data.getString("State").equals(tag.getString("State"))||data.getInt("Revision")!=tag.getInt("Revision");data=tag.copy();if(changed&&minecraft!=null)rebuildWidgets();}
    @Override protected void init(){
        boolean offer=data.getString("State").equals("OFFER");
        w=Math.min(384,width-16);h=Math.min(offer?180:268,height-16);left=(width-w)/2;top=(height-h)/2;
        var god=ResourceLocation.tryParse(data.getString("God"));
        addRenderableWidget(new ShrineUi.StoneButton(left+12,top+h-30,w-24,20,Component.translatable(offer?"gui.divinity_engine.communion.cancel":"gui.divinity_engine.communion.return"),b->{if(!offer)send(2,"","","");onClose();}));
        if(offer){var begin=addRenderableWidget(new ShrineUi.StoneButton(left+12,top+h-55,w-24,20,Component.translatable("gui.divinity_engine.communion.begin"),b->{if(god!=null)ModMessages.sendToServer(CommunionNetwork.Request.entry(god,1));}));begin.active=data.getString("Eligibility").isEmpty();}
        else if(data.getString("State").equals("ACTIVE")){
            var stories=data.getList("Stories",10);storyIndex=Math.max(0,Math.min(storyIndex,stories.size()-1));
            if(stories.size()>1){button(left+12,top+38,22,"<",()->{storyIndex=(storyIndex+stories.size()-1)%stories.size();scroll=0;rebuildWidgets();});button(left+w-34,top+38,22,">",()->{storyIndex=(storyIndex+1)%stories.size();scroll=0;rebuildWidgets();});}
            if(!stories.isEmpty()){
                var story=stories.getCompound(storyIndex);boolean allowed=story.getBoolean("Compatible");String stage=story.getString("Stage");
                if(allowed&&stage.equals("DORMANT"))button(left+12,top+h-55,w-24,"Hear the request",()->send(3,"request","",""));
                else if(allowed&&stage.equals("CHOICE")){
                    if(!story.getString("Token").isEmpty())button(left+12,top+h-55,w-24,"Confirm "+story.getString("Name_"+story.getString("Preview"))+" (permanent)",()->send(3,"confirm",story.getString("Preview"),story.getString("Token")));
                    else {button(left+12,top+h-55,(w-28)/2,story.getString("Name_a"),()->send(3,"preview","a",""));button(left+w/2+2,top+h-55,(w-28)/2,story.getString("Name_b"),()->send(3,"preview","b",""));}
                }
            }
        }
    }
    private void button(int x,int y,int width,String text,Runnable action){addRenderableWidget(new ShrineUi.StoneButton(x,y,width,20,Component.literal(text),b->action.run()));}
    private void send(int action,String verb,String branch,String token){
        if(!data.hasUUID("Session"))return;var god=new ResourceLocation(data.getString("God"));var stories=data.getList("Stories",10);
        ResourceLocation story=stories.isEmpty()?god:new ResourceLocation(stories.getCompound(Math.min(storyIndex,stories.size()-1)).getString("Id"));
        ModMessages.sendToServer(new CommunionNetwork.Request(action,god,data.getUUID("Session"),data.getInt("Revision"),story,verb,branch,token));
    }
    private String text(){
        String state=data.getString("State");if(state.equals("OFFER"))return Component.translatable("gui.divinity_engine.communion.warning").getString()+"\n\n"+(data.getString("Eligibility").isEmpty()?"Ready. Remain still during meditation.":data.getString("Eligibility"));
        if(!state.equals("ACTIVE"))return "Meditating... Keep still while the realm is prepared. You may cancel at any time.";
        var stories=data.getList("Stories",10);if(stories.isEmpty())return "Your god acknowledges your presence. Read the Divine Codex for guidance. Worship and trials still require your physical shrine.";
        var story=stories.getCompound(Math.min(storyIndex,stories.size()-1));String text=story.getString("Text");
        if(story.getString("Stage").equals("CHOICE"))text+="\n\n"+story.getString("Name_a")+": "+story.getString("Reward_a")+"\n\n"+story.getString("Name_b")+": "+story.getString("Reward_b");
        return text;
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        var godId=ResourceLocation.tryParse(data.getString("God"));
        var god=godId==null?null:net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeManager.god(godId);
        String heading=(god==null?"":god.getName().getString()+" - ")+data.getString("Name");
        var root=DivinityUi.canvas("communion").child(ShrineUi.background(left,top,w,h,10,data.getInt("Color")))
            .child(DivinityUi.text("title",font.plainSubstrByWidth(heading,w-28),left+14,top+10,w-28,12,ShrineUi.GOLD));
        var stories=data.getList("Stories",10);if(!stories.isEmpty()){var s=stories.getCompound(Math.min(storyIndex,stories.size()-1));root.child(DivinityUi.text("chapter",font.plainSubstrByWidth(s.getString("Title"),w-90),left+44,top+43,w-88,12,ShrineUi.TEXT));}
        int bodyTop=data.getString("State").equals("OFFER")?42:68;
        var lines=font.split(Component.literal(text()),w-32);int rows=Math.max(1,(h-bodyTop-75)/11);scroll=Math.max(0,Math.min(scroll,lines.size()-rows));
        tree.setRoot(root.build());tree.layout(0,0,width,height);tree.render(g,mouseX,mouseY,partial);
        for(int i=0;i<rows&&i+scroll<lines.size();i++)g.drawString(font,lines.get(i+scroll),left+16,top+bodyTop+i*11,ShrineUi.TEXT,false);
        if(lines.size()>rows)g.drawString(font,"Scroll to read",left+16,top+h-69,ShrineUi.MUTED,false);
        super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){scroll+=delta>0?-2:2;return true;}
    @Override public boolean isPauseScreen(){return false;}
}
