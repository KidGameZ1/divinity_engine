package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Overworld-owned write-ahead recovery metadata. No inventories or story state are copied. */
public final class CommunionLedger extends SavedData {
    public enum State { PREPARING, ENTERING, ACTIVE, RETURNING, CLEANUP, CLOSED }
    public enum Exit { NORMAL, CANCELED, BODY_HIT, DAMAGE, UNSAFE, TIMEOUT, DISCONNECT, RECOVERY, MISSING_BODY, RESOURCE, EXTERNAL_TELEPORT, DEATH, ADMIN, SHUTDOWN }
    public static final String PLAYER_KEY="DivinityCommunionRecovery";
    public final Map<UUID,Record> records=new LinkedHashMap<>();
    public static CommunionLedger get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(CommunionLedger::load,CommunionLedger::new,"divinity_inner_worlds");}
    public static final class Record {
        public UUID id,owner,body;
        public ResourceLocation god,dimension;
        public Vec3 anchor; public float yaw,pitch;
        public int slot,width,height,depth,cursor,revision=1;
        public long started,activeAt;
        public boolean returned,cleaned;
        public State state=State.PREPARING;
        public Exit exit=Exit.NORMAL;
        public String variant="default",definition="";
        public BlockPos origin(){return new BlockPos((slot%128)*128,80,(slot/128)*128);}
        public int volume(){return width*height*depth;}
        public CompoundTag save(){
            var t=new CompoundTag();t.putUUID("Session",id);t.putUUID("Owner",owner);if(body!=null)t.putUUID("Body",body);
            t.putString("God",god.toString());t.putString("Dimension",dimension.toString());
            t.putDouble("X",anchor.x);t.putDouble("Y",anchor.y);t.putDouble("Z",anchor.z);t.putFloat("Yaw",yaw);t.putFloat("Pitch",pitch);
            t.putInt("Slot",slot);t.putInt("Width",width);t.putInt("Height",height);t.putInt("Depth",depth);t.putInt("Cursor",cursor);
            t.putInt("Revision",revision);t.putLong("Started",started);t.putLong("ActiveAt",activeAt);t.putBoolean("Returned",returned);t.putBoolean("Cleaned",cleaned);
            t.putString("State",state.name());t.putString("Exit",exit.name());t.putString("Variant",variant);t.putString("Definition",definition);return t;
        }
        public static Record load(CompoundTag t){
            var r=new Record();r.id=t.getUUID("Session");r.owner=t.getUUID("Owner");r.body=t.hasUUID("Body")?t.getUUID("Body"):null;
            r.god=new ResourceLocation(t.getString("God"));r.dimension=new ResourceLocation(t.getString("Dimension"));
            r.anchor=new Vec3(t.getDouble("X"),t.getDouble("Y"),t.getDouble("Z"));r.yaw=t.getFloat("Yaw");r.pitch=t.getFloat("Pitch");
            r.slot=t.getInt("Slot");r.width=t.getInt("Width");r.height=t.getInt("Height");r.depth=t.getInt("Depth");r.cursor=t.getInt("Cursor");
            r.revision=t.getInt("Revision");r.started=t.getLong("Started");r.activeAt=t.getLong("ActiveAt");r.returned=t.getBoolean("Returned");r.cleaned=t.getBoolean("Cleaned");
            r.state=State.valueOf(t.getString("State"));r.exit=Exit.valueOf(t.getString("Exit"));r.variant=t.getString("Variant");r.definition=t.getString("Definition");
            if(r.slot<0||r.slot>=16384||r.width<9||r.width>48||r.depth<9||r.depth>48||r.height<6||r.height>32||!Double.isFinite(r.anchor.lengthSqr()))throw new IllegalArgumentException("Invalid recorded bounds");
            r.cursor=Math.max(0,Math.min(r.volume(),r.cursor));return r;
        }
    }
    public Record byOwner(UUID owner){return records.values().stream().filter(r->r.owner.equals(owner)&&!r.returned).findFirst().orElse(null);}
    public int freeSlot(){Set<Integer> used=new HashSet<>();for(var r:records.values())if(!r.cleaned)used.add(r.slot);for(int i=0;i<16384;i++)if(!used.contains(i))return i;return -1;}
    public static CommunionLedger load(CompoundTag t){
        var data=new CommunionLedger();for(Tag tag:t.getList("Sessions",Tag.TAG_COMPOUND))try{var r=Record.load((CompoundTag)tag);data.records.put(r.id,r);}catch(RuntimeException e){net.nightshade.divinity_engine.DivinityEngineMod.LOGGER.error("Invalid communion recovery record; refusing unknown bounds",e);}
        return data;
    }
    @Override public CompoundTag save(CompoundTag t){var list=new ListTag();for(var r:records.values())list.add(r.save());t.putInt("Version",1);t.put("Sessions",list);return t;}
}
