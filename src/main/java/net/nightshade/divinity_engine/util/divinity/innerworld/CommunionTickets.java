package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.nightshade.divinity_engine.DivinityEngineMod;
import java.util.*;

/** Only this feature's UUID prefix is reclaimed. Pocket/trial/admin tickets remain untouched. */
public final class CommunionTickets {
    public static final long PREFIX=0x444956434F4D4000L;
    private static final TicketType<UUID> PREPARE=TicketType.create("divinity_communion_prepare",UUID::compareTo,60);
    public static UUID newSession(){return new UUID(PREFIX,UUID.randomUUID().getLeastSignificantBits());}
    public static void register(){ForgeChunkManager.setForcedChunkLoadingCallback(DivinityEngineMod.MODID,(level,helper)->{
        for(UUID id:helper.getEntityTickets().keySet())if(id.getMostSignificantBits()==PREFIX)helper.removeAllTickets(id);
    });}
    public static List<ChunkPos> source(CommunionLedger.Record r){
        var result=new ArrayList<ChunkPos>();var center=new ChunkPos(BlockPos.containing(r.anchor));
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)result.add(new ChunkPos(center.x+x,center.z+z));return result;
    }
    public static List<ChunkPos> destination(CommunionLedger.Record r){
        var result=new ArrayList<ChunkPos>();var o=r.origin();
        for(int x=o.getX()>>4;x<=(o.getX()+r.width-1)>>4;x++)for(int z=o.getZ()>>4;z<=(o.getZ()+r.depth-1)>>4;z++)result.add(new ChunkPos(x,z));return result;
    }
    public static boolean prepare(ServerLevel level,UUID id,List<ChunkPos> chunks){
        boolean ready=true;for(var c:chunks){level.getChunkSource().addRegionTicket(PREPARE,c,0,id);if(level.getChunkSource().getChunkNow(c.x,c.z)==null)ready=false;}return ready;
    }
    public static boolean forceSource(ServerLevel level,CommunionLedger.Record r){
        if(source(r).size()>CommunionConfig.SOURCE_CHUNKS.get())return false;
        // forceChunk calls getChunk internally. Preflight every chunk to avoid a blocking load.
        if(!prepare(level,r.id,source(r)))return false;
        for(var c:source(r))ForgeChunkManager.forceChunk(level,"divinity_engine",r.id,c.x,c.z,true,true);
        return true;
    }
    public static void releaseSource(ServerLevel level,CommunionLedger.Record r){if(level!=null)for(var c:source(r)){ForgeChunkManager.forceChunk(level,"divinity_engine",r.id,c.x,c.z,false,true);level.getChunkSource().removeRegionTicket(PREPARE,c,0,r.id);}}
    public static void releaseDestination(ServerLevel level,CommunionLedger.Record r){if(level!=null)for(var c:destination(r))level.getChunkSource().removeRegionTicket(PREPARE,c,0,r.id);}
    private CommunionTickets(){}
}
