package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.api.innerworld.InnerWorldDefinition;
import net.nightshade.divinity_engine.entity.*;
import net.nightshade.divinity_engine.network.cap.player.narrative.DivineNarrativeCapability;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.util.divinity.narrative.*;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionManager;
import java.util.*;
import static net.nightshade.divinity_engine.util.divinity.innerworld.CommunionLedger.*;
import net.nightshade.divinity_engine.util.divinity.innerworld.CommunionLedger.Record;

/** The only authority for projection ownership, transport and cleanup. Never runs on a worker. */
public final class CommunionManager {
    private static final Map<UUID,Runtime> LIVE=new LinkedHashMap<>();
    private static final Map<UUID,UUID> OWNERS=new HashMap<>();
    private static final Map<UUID,Long> REQUESTS=new HashMap<>();
    private static MinecraftServer current;
    private static boolean transporting;
    private static long maximumJobNanos;
    private static int lastBlockOperations;
    private static final class Runtime {
        final Record r;final InnerWorldDefinition definition;final InnerWorldDefinition.Visuals visual;
        Map<BlockPos,BlockState> template;UUID manifestation;boolean sourceForced,forwarding;
        final LinkedHashMap<BlockPos,BlockState> edits=new LinkedHashMap<>();
        final Deque<PendingHit> pendingHits=new ArrayDeque<>();
        Runtime(Record r,InnerWorldDefinition d,InnerWorldDefinition.Visuals v){this.r=r;definition=d;visual=v;}
    }
    private record PendingHit(DamageSource source,float amount){}
    public static boolean inRealm(Entity player){return player.level().dimension().equals(DimensionRegistry.INNER_WORLD);}
    public static boolean busy(Entity player){return OWNERS.containsKey(player.getUUID())||player.getPersistentData().contains(PLAYER_KEY)||inRealm(player);}
    public static Record session(ServerPlayer p){Runtime rt=LIVE.get(OWNERS.get(p.getUUID()));return rt==null?null:rt.r;}
    public static boolean transporting(){return transporting;}
    private static long now(MinecraftServer s){return s.overworld().getGameTime();}
    private static ServerLevel source(MinecraftServer server,Record r){return server.getLevel(ResourceKey.create(Registries.DIMENSION,r.dimension));}
    public static boolean rateLimit(ServerPlayer p){long now=now(p.server);Long last=REQUESTS.put(p.getUUID(),now);return last==null||now-last>=10;}
    public static String eligibility(ServerPlayer p,ResourceLocation god){
        var d=InnerWorldsRegistry.get(god);
        if(d==null)return "This god has no registered Inner World.";
        if(!DivineNarrativeManager.contacted(p,god))return "Make contact with this god at their statue first.";
        if(busy(p))return "Finish your current communion first.";
        if(TrialSessionManager.getSession(p)!=null||p.level().dimension().equals(DimensionRegistry.GODLY_TRIALS)||p.level().dimension().equals(DimensionRegistry.POCKET_DIMENSION)
            ||net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketChunkLoading.pending(p.getUUID())
            ||net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager.isTransferring(p))return "Leave your trial or pocket-domain journey first.";
        long cooldown=p.getPersistentData().getLong("DivinityCommunionCooldown");
        if(cooldown>now(p.server))return "Meditation is recovering: "+((cooldown-now(p.server)+19)/20)+"s.";
        if(!safeToBegin(p))return "Stand still on safe, solid ground, away from fire or water, without riding or sleeping.";
        if(LIVE.size()>=CommunionConfig.CAPACITY.get())return "All communion places are occupied. Try again shortly.";
        if(d.width>CommunionConfig.MAX_SIDE.get()||d.depth>CommunionConfig.MAX_SIDE.get())return "This realm exceeds this server's configured bounds.";
        if(CommunionConfig.SOURCE_CHUNKS.get()<9)return "This server cannot maintain the required nine source chunks.";
        if(p.server.getLevel(DimensionRegistry.INNER_WORLD)==null)return "The Inner World dimension is unavailable.";
        try{if(!d.requirement.test(p))return d.requirementText;}catch(RuntimeException ex){DivinityEngineMod.LOGGER.error("{} entry requirement failed",d.id,ex);return "This realm's entry condition failed.";}
        return "";
    }
    private static boolean safeToBegin(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&!p.isSleeping()&&!p.isPassenger()&&!p.isVehicle()&&!p.isFallFlying()&&!p.getAbilities().flying
        &&p.onGround()&&p.fallDistance<.1F&&!p.isOnFire()&&!p.isInWaterOrBubble()&&!p.isInLava()&&!p.isInWall()&&p.getDeltaMovement().lengthSqr()<.04
        &&p.serverLevel().noCollision(p,p.getBoundingBox())&&!p.serverLevel().noCollision(p,p.getBoundingBox().move(0,-.1,0));}
    public static boolean begin(ServerPlayer p,ResourceLocation god){
        ensure(p.server);String error=eligibility(p,god);if(!error.isEmpty()){message(p,error);return false;}
        var d=InnerWorldsRegistry.get(god);var data=get(p.server);int slot=data.freeSlot();if(slot<0)return false;
        var r=new Record();r.id=CommunionTickets.newSession();r.owner=p.getUUID();r.god=god;r.dimension=p.level().dimension().location();r.anchor=p.position();r.yaw=p.getYRot();r.pitch=p.getXRot();
        r.slot=slot;r.width=d.width;r.height=d.height;r.depth=d.depth;r.started=now(p.server);r.definition=d.id+"@"+d.revision;
        try{
            var variant=d.resolve(p);r.variant=variant.id();var rt=new Runtime(r,d,variant.visuals());
            if(d.template!=null)rt.template=template(p.server.getLevel(DimensionRegistry.INNER_WORLD),d);
            LIVE.put(r.id,rt);OWNERS.put(r.owner,r.id);data.records.put(r.id,r);persist(p,r);data.setDirty();
            hook(rt,p,InnerWorldDefinition.Moment.REQUEST);if(session(p)!=r)return false;
            p.closeContainer();send(p,r,true);return true;
        }catch(RuntimeException ex){DivinityEngineMod.LOGGER.error("{}: invalid Inner World preparation",d.id,ex);if(LIVE.containsKey(r.id))end(p,Exit.RESOURCE);message(p,"The realm could not be prepared. No entry was made.");return false;}
    }
    private static Map<BlockPos,BlockState> template(ServerLevel level,InnerWorldDefinition d){
        var template=level.getStructureManager().get(d.template).orElseThrow(()->new IllegalArgumentException(d.id+": missing template "+d.template));
        var size=template.getSize();if(size.getX()>d.width||size.getY()>d.height||size.getZ()>d.depth)throw new IllegalArgumentException(d.id+": template exceeds instance bounds");
        var tag=template.save(new CompoundTag());var palette=tag.getList("palette",Tag.TAG_COMPOUND);
        if(palette.isEmpty())throw new IllegalArgumentException(d.id+": template requires one nonempty palette");
        var blocks=new HashMap<BlockPos,BlockState>();
        for(Tag value:tag.getList("blocks",Tag.TAG_COMPOUND)){
            var block=(CompoundTag)value;var pos=block.getList("pos",Tag.TAG_INT);int state=block.getInt("state");
            if(state<0||state>=palette.size()||pos.size()!=3)throw new IllegalArgumentException(d.id+": malformed template block");
            var at=new BlockPos(pos.getInt(0),pos.getInt(1),pos.getInt(2));
            if(at.getX()<0||at.getY()<0||at.getZ()<0||at.getX()>=d.width||at.getY()>=d.height||at.getZ()>=d.depth)throw new IllegalArgumentException(d.id+": template block outside bounds");
            var bs=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),palette.getCompound(state));
            if(bs.hasBlockEntity()||!bs.getFluidState().isEmpty())throw new IllegalArgumentException(d.id+": template must use inert scenery, not containers, block entities or fluids");blocks.put(at,bs);
        }
        return blocks;
    }
    private static void ensure(MinecraftServer server){if(current!=server){LIVE.clear();OWNERS.clear();REQUESTS.clear();maximumJobNanos=0;current=server;}}
    public static void started(MinecraftServer server){
        ensure(server);var data=get(server);
        for(var r:data.records.values()){r.state=State.CLEANUP;r.exit=Exit.RECOVERY;r.cursor=0;r.revision++;}data.setDirty();
    }
    public static void tick(MinecraftServer server){
        ensure(server);long now=now(server);var world=server.getLevel(DimensionRegistry.INNER_WORLD);var data=get(server);
        CommunionDamage.prune(now);
        if(world!=null){world.getChunkSource().setViewDistance(Math.min(3,server.getPlayerList().getViewDistance()));world.getChunkSource().setSimulationDistance(Math.min(2,server.getPlayerList().getSimulationDistance()));}
        for(var rt:List.copyOf(LIVE.values())){
            var r=rt.r;var p=server.getPlayerList().getPlayer(r.owner);
            // GameTests may have connected players outside PlayerList; recover their reference from the origin/realm.
            if(p==null)p=findPlayer(server,r.owner);
            if(p==null){retire(server,rt,Exit.DISCONNECT);continue;}
            if(r.state==State.PREPARING||r.state==State.ENTERING){
                if(!safeToBegin(p)||!p.level().dimension().location().equals(r.dimension)||p.position().distanceToSqr(r.anchor)>.01||!eligibleStill(p,rt))end(p,Exit.CANCELED);
                else if(now-r.started>1200)end(p,Exit.TIMEOUT);
            }else if(r.state==State.RETURNING){
                if(returnToAnchor(p,r)){
                    r.returned=true;rt.forwarding=true;
                    try{while(!rt.pendingHits.isEmpty()&&p.isAlive()){var hit=rt.pendingHits.removeFirst();p.hurt(hit.source,hit.amount);CommunionDamage.record(p,hit.source);}}
                    finally{rt.forwarding=false;finish(p,rt);}
                }
            }else if(r.state==State.ACTIVE){
                if(!p.isAlive())end(p,Exit.DEATH);
                else if(!inRealm(p))end(p,Exit.EXTERNAL_TELEPORT);
                else if(!bounds(r).contains(p.position()))end(p,Exit.UNSAFE);
                else if(now-r.activeAt>=CommunionConfig.DURATION.get())end(p,Exit.TIMEOUT);
                else {
                    var src=source(server,r);var body=src==null?null:src.getEntity(r.body);
                    if(!(body instanceof TranceBodyEntity))end(p,Exit.MISSING_BODY);
                    else if(!src.getChunkSource().isPositionTicking(body.chunkPosition().toLong()))end(p,Exit.RESOURCE);
                    else if(now%20==0)targetThreats(p,(TranceBodyEntity)body);
                }
            }
            if(session(p)==r&&now%20==0)send(p,r,false);
        }
        if(world==null)return;
        long jobStart=System.nanoTime(),until=jobStart+2_000_000;int remaining=CommunionConfig.BLOCK_BUDGET.get();
        for(var rt:LIVE.values())if(rt.r.state==State.ACTIVE)while(!rt.edits.isEmpty()&&remaining>0&&System.nanoTime()<until){
            var entry=rt.edits.entrySet().iterator().next();var pos=rt.r.origin().offset(entry.getKey());
            if(!world.hasChunkAt(pos))break;
            world.setBlock(pos,entry.getValue(),2|16);rt.edits.remove(entry.getKey());remaining--;
        }
        for(var r:List.copyOf(data.records.values())){
            if(remaining<=0||System.nanoTime()>=until)break;
            Runtime rt=LIVE.get(r.id);
            boolean clearing=r.state==State.CLEANUP;
            if(r.cleaned || !(clearing||r.state==State.PREPARING||r.state==State.ENTERING))continue;
            if(clearing&&world.players().stream().anyMatch(p->bounds(r).contains(p.position())))continue;
            if(!clearing&&rt==null)continue;
            if(!CommunionTickets.prepare(world,r.id,CommunionTickets.destination(r)))continue;
            if(!clearing)r.state=State.ENTERING;
            try{
                while(r.cursor<r.volume()&&remaining>0&&System.nanoTime()<until){
                    int n=r.cursor++;int x=n%r.width,z=(n/r.width)%r.depth,y=n/(r.width*r.depth);var local=new BlockPos(x,y,z);
                    BlockState block=clearing?Blocks.AIR.defaultBlockState():rt.template!=null?rt.template.getOrDefault(local,Blocks.AIR.defaultBlockState()):rt.definition.scene.apply(local);
                    if(block==null||block.hasBlockEntity()||!block.getFluidState().isEmpty())throw new IllegalArgumentException(r.definition+": unsafe scene block at "+local);
                    if(!world.getBlockState(r.origin().offset(local)).equals(block))world.setBlock(r.origin().offset(local),block,2|16);
                    remaining--;
                }
                data.setDirty();
                if(r.cursor==r.volume()){
                    if(clearing){r.cleaned=true;CommunionTickets.releaseDestination(world,r);if(r.returned)data.records.remove(r.id);}
                    else if(now-r.started>=CommunionConfig.WINDUP.get())enter(server,rt);
                }
            }catch(RuntimeException ex){DivinityEngineMod.LOGGER.error("{} instance job failed",r.definition,ex);if(!clearing){var p=findPlayer(server,r.owner);if(p!=null)end(p,Exit.RESOURCE);else retire(server,rt,Exit.RESOURCE);}}
        }
        for(var p:server.getPlayerList().getPlayers())if(!OWNERS.containsKey(p.getUUID())&&(p.getPersistentData().contains(PLAYER_KEY)||data.byOwner(p.getUUID())!=null||inRealm(p)))recover(p);
        maximumJobNanos=Math.max(maximumJobNanos,System.nanoTime()-jobStart);lastBlockOperations=CommunionConfig.BLOCK_BUDGET.get()-remaining;
    }
    private static ServerPlayer findPlayer(MinecraftServer server,UUID id){var listed=server.getPlayerList().getPlayer(id);if(listed!=null)return listed;for(var level:server.getAllLevels())for(var p:level.players())if(p.getUUID().equals(id))return p;return null;}
    private static boolean eligibleStill(ServerPlayer p,Runtime rt){try{return DivineNarrativeManager.contacted(p,rt.r.god)&&rt.definition.requirement.test(p)&&TrialSessionManager.getSession(p)==null;}catch(RuntimeException ex){return false;}}
    private static void enter(MinecraftServer server,Runtime rt){
        var r=rt.r;var p=findPlayer(server,r.owner);if(p==null||session(p)!=r||!safeToBegin(p)||!eligibleStill(p,rt)){if(p!=null)end(p,Exit.CANCELED);return;}
        var level=server.getLevel(DimensionRegistry.INNER_WORLD);var src=source(server,r);
        if(src==null||!CommunionTickets.forceSource(src,r))return;
        rt.sourceForced=true;Vec3 arrival=rt.definition.spawn.add(Vec3.atLowerCornerOf(r.origin()));
        AABB box=p.getBoundingBox().move(arrival.subtract(p.position()));
        if(!level.noCollision(p,box)||level.noCollision(p,box.move(0,-.1,0))){end(p,Exit.RESOURCE);return;}
        var body=CommunionBootstrap.BODY.get().create(src);if(body==null){end(p,Exit.RESOURCE);return;}
        body.moveTo(r.anchor.x,r.anchor.y,r.anchor.z,r.yaw,r.pitch);body.bind(p,r.id,r.god.toString());r.body=body.getUUID();persist(p,r);
        if(!src.addFreshEntity(body)){end(p,Exit.RESOURCE);return;}
        var god=CommunionBootstrap.GOD.get().create(level);if(god==null){end(p,Exit.RESOURCE);return;}
        Vec3 at=rt.definition.manifestation.add(Vec3.atLowerCornerOf(r.origin()));god.moveTo(at.x,at.y,at.z,180,0);god.bind(p,r.id,r.god.toString());
        for(var slot:EquipmentSlot.values())god.setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);
        if(!rt.visual.motif().equals("void")){god.setCustomName(Component.literal(rt.definition.name));god.setCustomNameVisible(true);}
        else {god.setCustomName(null);god.setCustomNameVisible(false);}
        rt.manifestation=god.getUUID();
        if(!level.addFreshEntity(god)){end(p,Exit.RESOURCE);return;}
        move(p,level,arrival,0,0);r.state=State.ACTIVE;r.activeAt=now(server);r.revision++;persist(p,r);get(server).setDirty();
        hook(rt,p,InnerWorldDefinition.Moment.ENTER);if(session(p)==r)send(p,r,true);
        if(CommunionConfig.DIAGNOSTICS.get())DivinityEngineMod.LOGGER.info("Communion {} prepared in {} ticks",r.id,now(server)-r.started);
    }
    public static AABB bounds(Record r){return new AABB(r.origin(),r.origin().offset(r.width,r.height,r.depth));}
    private static void move(ServerPlayer p,ServerLevel level,Vec3 pos,float yaw,float pitch){
        boolean old=transporting;transporting=true;try{p.closeContainer();p.teleportTo(level,pos.x,pos.y,pos.z,yaw,pitch);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;}finally{transporting=old;}
    }
    private static void persist(ServerPlayer p,Record r){p.getPersistentData().put(PLAYER_KEY,r.save());get(p.server).setDirty();}
    public static void end(ServerPlayer p,Exit reason){
        Runtime rt=LIVE.get(OWNERS.get(p.getUUID()));if(rt==null){if(inRealm(p)||p.getPersistentData().contains(PLAYER_KEY))recover(p);return;}
        if(rt.r.state==State.RETURNING||rt.r.state==State.CLEANUP)return;
        if(returnPlayer(p,rt,reason))finish(p,rt);
    }
    private static boolean returnPlayer(ServerPlayer p,Runtime rt,Exit reason){
        var r=rt.r;r.state=State.RETURNING;r.exit=reason;r.revision++;invalidateChoices(p);persist(p,r);
        send(p,r,false);
        if(reason!=Exit.EXTERNAL_TELEPORT&&reason!=Exit.DEATH){if(!returnToAnchor(p,r))return false;}
        r.returned=true;persist(p,r);return true;
    }
    private static boolean returnToAnchor(ServerPlayer p,Record r){
        var level=source(p.server,r);boolean fallback=level==null||level.dimension().equals(DimensionRegistry.INNER_WORLD);
        if(fallback)level=p.server.overworld();Vec3 anchor=fallback?Vec3.atBottomCenterOf(level.getSharedSpawnPos()):r.anchor;
        var chunk=new net.minecraft.world.level.ChunkPos(BlockPos.containing(anchor));
        if(!CommunionTickets.prepare(level,r.id,List.of(chunk)))return false;
        Vec3 safe=collisionSafe(p,level,anchor);
        // If every nearby cell is obstructed, remain near the body rather than teleport away from an attacker.
        if(safe==null)safe=anchor;
        move(p,level,safe,r.yaw,r.pitch);return true;
    }
    public static Vec3 collisionSafe(ServerPlayer p,ServerLevel level,Vec3 anchor){
        for(int radius=0;radius<=3;radius++)for(int dy=0;dy<=3;dy++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;Vec3 v=anchor.add(dx,dy,dz);
            if(!level.hasChunkAt(BlockPos.containing(v))||!level.getWorldBorder().isWithinBounds(BlockPos.containing(v)))continue;
            if(level.noCollision(p,p.getBoundingBox().move(v.subtract(p.position()))))return v;
        }return null;
    }
    private static void finish(ServerPlayer p,Runtime rt){
        var r=rt.r;if(!r.returned)return;
        p.getPersistentData().remove(PLAYER_KEY);p.getPersistentData().putLong("DivinityCommunionCooldown",now(p.server)+CommunionConfig.COOLDOWN.get());
        if(r.exit==Exit.BODY_HIT||r.exit==Exit.DAMAGE){message(p,"Your communion has been broken.");hook(rt,p,InnerWorldDefinition.Moment.INTERRUPT);}
        hook(rt,p,InnerWorldDefinition.Moment.RETURN);retire(p.server,rt,r.exit);CommunionNetwork.clear(p);
    }
    private static void retire(MinecraftServer server,Runtime rt,Exit reason){
        if(rt==null)return;var r=rt.r;r.exit=reason;r.state=State.CLEANUP;r.cursor=0;
        OWNERS.remove(r.owner,r.id);LIVE.remove(r.id);var src=source(server,r);var world=server.getLevel(DimensionRegistry.INNER_WORLD);
        if(src!=null&&r.body!=null){var b=src.getEntity(r.body);if(b instanceof TranceBodyEntity body&&r.id.equals(body.session()))b.discard();}
        if(world!=null&&rt.manifestation!=null){var e=world.getEntity(rt.manifestation);if(e!=null)e.discard();}
        CommunionTickets.releaseSource(src,r);get(server).setDirty();
        var p=findPlayer(server,r.owner);hook(rt,p,InnerWorldDefinition.Moment.CLEANUP);
    }
    public static void recover(ServerPlayer p){
        ensure(p.server);var data=get(p.server);var r=data.byOwner(p.getUUID());
        if(r==null&&p.getPersistentData().contains(PLAYER_KEY))try{r=Record.load(p.getPersistentData().getCompound(PLAYER_KEY));}catch(RuntimeException ex){DivinityEngineMod.LOGGER.error("Invalid player communion return metadata",ex);}
        if(r==null){if(inRealm(p)){var spawn=p.server.overworld().getSharedSpawnPos();if(!CommunionTickets.prepare(p.server.overworld(),p.getUUID(),List.of(new net.minecraft.world.level.ChunkPos(spawn))))return;move(p,p.server.overworld(),Vec3.atBottomCenterOf(spawn),0,0);}p.getPersistentData().remove(PLAYER_KEY);CommunionNetwork.clear(p);return;}
        if(!returnToAnchor(p,r))return;r.returned=true;r.state=State.CLEANUP;r.exit=Exit.RECOVERY;r.cursor=0;
        var src=source(p.server,r);if(src!=null&&r.body!=null&&src.getEntity(r.body) instanceof TranceBodyEntity body&&r.id.equals(body.session()))body.discard();
        CommunionTickets.releaseSource(src,r);p.getPersistentData().remove(PLAYER_KEY);invalidateChoices(p);CommunionNetwork.clear(p);
        p.getPersistentData().putLong("DivinityCommunionCooldown",now(p.server)+CommunionConfig.COOLDOWN.get());
        // A player-only crash record may refer to an already reused slot: never insert its old deletion bounds.
        if(data.records.containsKey(r.id)){if(r.cleaned)data.records.remove(r.id);data.setDirty();}
    }
    public static void bodyTick(TranceBodyEntity body){
        var rt=LIVE.get(body.session());if(rt==null||!rt.r.owner.equals(body.owner())){body.discard();return;}
        if(body instanceof GodManifestationEntity)return;
        if(rt.r.state!=State.ACTIVE)return;var p=findPlayer(body.getServer(),body.owner());if(p==null)return;
        if(!body.level().noCollision(body,body.getBoundingBox())||body.level().noCollision(body,body.getBoundingBox().move(0,-.15,0)))end(p,Exit.UNSAFE);
    }
    public static boolean bodyHit(TranceBodyEntity body,DamageSource source,float amount){
        var rt=LIVE.get(body.session());if(rt==null||rt.forwarding||rt.r.state!=State.ACTIVE||!rt.r.owner.equals(body.owner()))return false;
        var p=findPlayer(body.getServer(),body.owner());if(p==null||!p.isAlive()||p.isInvulnerableTo(source))return false;
        if(source.getEntity() instanceof Player attacker&&(!p.server.isPvpAllowed()||!p.canHarmPlayer(attacker)||!attacker.canHarmPlayer(p)))return false;
        rt.forwarding=true;
        try {
            boolean hit=p.hurt(source,amount);
            if(hit){
                body.acceptedHit(p);
                if(source.getEntity() instanceof LivingEntity attacker
                    &&!source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                    &&(source.getDirectEntity() instanceof LivingEntity||source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile))
                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostHurtEffects(p,attacker);
            }
            return hit;
        } finally {rt.forwarding=false;if(rt.r.state==State.RETURNING&&rt.r.returned)finish(p,rt);}
    }
    /** Called AFTER Forge's cancelable player attack event has fully completed, before mitigation. */
    public static boolean authorizedPlayerAttack(ServerPlayer p,DamageSource source,float amount){
        Runtime rt=LIVE.get(OWNERS.get(p.getUUID()));if(rt==null||amount<=0||p.isInvulnerableTo(source))return true;
        if(rt.r.state==State.RETURNING&&rt.r.returned)return true;
        if(rt.r.state!=State.RETURNING&&returnPlayer(p,rt,rt.forwarding?Exit.BODY_HIT:Exit.DAMAGE)){
            CommunionDamage.record(p,source);return true;
        }
        // Exceptional unloaded/deleted-source fallback: never resolve a lethal hit in the realm.
        // Retry through normal Forge cancellation/mitigation only after the return chunks are ready.
        if(rt.pendingHits.size()<16)rt.pendingHits.addLast(new PendingHit(source,amount));
        return false;
    }
    public static void damageFinished(ServerPlayer p){var rt=LIVE.get(OWNERS.get(p.getUUID()));if(rt!=null&&!rt.forwarding&&rt.r.state==State.RETURNING&&rt.r.returned)finish(p,rt);}
    private static void targetThreats(ServerPlayer p,TranceBodyEntity body){
        if(p.isCreative()||p.isSpectator())return;double radius=CommunionConfig.TARGET_RADIUS.get();int considered=0;
        for(Monster mob:body.level().getEntitiesOfClass(Monster.class,body.getBoundingBox().inflate(radius),m->m.isAlive()&&!m.isNoAi())){
            if(++considered>24)break;
            if(mob.getTarget()!=null&&mob.getTarget()!=p&&mob.getTarget().isAlive())continue;
            if(mob instanceof net.minecraft.world.entity.monster.EnderMan||mob instanceof net.minecraft.world.entity.monster.ZombifiedPiglin)continue;
            double range=mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
            if(mob.distanceToSqr(body)>range*range||mob.isAlliedTo(p)||!mob.getSensing().hasLineOfSight(body))continue;
            mob.setTarget(body);
        }
    }
    public static void interact(ServerPlayer p,UUID session,UUID entity){var rt=LIVE.get(session);if(rt!=null&&rt.r.owner.equals(p.getUUID())&&rt.r.state==State.ACTIVE&&Objects.equals(rt.manifestation,entity)&&inRealm(p)){
        var god=p.serverLevel().getEntity(entity);if(god==null||p.distanceToSqr(god)>36)return;hook(rt,p,InnerWorldDefinition.Moment.INTERACT);if(LIVE.containsKey(session))send(p,rt.r,true);
    }}
    public static boolean validRequest(ServerPlayer p,UUID id,int revision){var r=session(p);return r!=null&&r.id.equals(id)&&r.revision==revision&&r.state==State.ACTIVE&&inRealm(p)&&bounds(r).contains(p.position());}
    public static boolean permitsStory(ServerPlayer p,ResourceLocation story){var rt=LIVE.get(OWNERS.get(p.getUUID()));var d=DivineNarrativeRegistry.byId(story);return rt!=null&&rt.r.state==State.ACTIVE&&inRealm(p)&&d!=null&&d.godId().equals(rt.r.god)&&rt.definition.compatibleStories.contains(story);}
    public static void storyAction(ServerPlayer p,UUID id,int revision,ResourceLocation story,String action,String branch,String token){
        if(!validRequest(p,id,revision)||!permitsStory(p,story)||!Set.of("request","preview","confirm","status","lore").contains(action))return;
        var r=session(p);int result=DivineNarrativeManager.communionAction(p,story,action,branch,token);if(session(p)==r){r.revision++;if(result>0)hook(LIVE.get(r.id),p,InnerWorldDefinition.Moment.STORY);if(session(p)==r)send(p,r,true);}
    }
    /** Addon scene changes use local coordinates and the same global budget as construction. */
    public static boolean sceneBlock(ServerPlayer p,UUID session,BlockPos local,BlockState state){
        if(!p.server.isSameThread())throw new IllegalStateException("Communion scenery must be requested on the server thread");
        Runtime rt=LIVE.get(session);if(rt==null||!rt.r.owner.equals(p.getUUID())||rt.r.state!=State.ACTIVE||rt.edits.size()>=128)return false;
        if(local.getX()<1||local.getZ()<1||local.getY()<1||local.getX()>=rt.r.width-1||local.getZ()>=rt.r.depth-1||local.getY()>=rt.r.height-1||state.hasBlockEntity()||!state.getFluidState().isEmpty())return false;
        var world=p.serverLevel();var pos=rt.r.origin().offset(local);if(p.getBoundingBox().intersects(new AABB(pos)))return false;
        rt.edits.put(local.immutable(),state);return true;
    }
    public static void invalidateChoices(ServerPlayer p){var data=DivineNarrativeCapability.get(p);if(data!=null)for(var def:DivineNarrativeRegistry.stories()){var m=data.findStory(def);if(m!=null){m.invalidatePending();m.preview="";m.previewExpires=0;}}}
    private static void hook(Runtime rt,ServerPlayer p,InnerWorldDefinition.Moment moment){try{rt.definition.hook.accept(new InnerWorldDefinition.Context(p,rt.r.id,rt.r.god,rt.r.variant,rt.r.origin()),moment);}catch(RuntimeException ex){DivinityEngineMod.LOGGER.error("{} {} hook failed; safety exit remains enabled",rt.definition.id,moment,ex);}}
    private static void send(ServerPlayer p,Record r,boolean open){var rt=LIVE.get(r.id);if(rt!=null)CommunionNetwork.state(p,r,rt.definition,rt.visual,open);}
    public static void inspect(ServerPlayer p,ResourceLocation god){String error=eligibility(p,god);CommunionNetwork.offer(p,god,error);}
    private static void message(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));}
    public static void logout(ServerPlayer p){end(p,Exit.DISCONNECT);REQUESTS.remove(p.getUUID());}
    public static void stop(MinecraftServer server){for(var rt:List.copyOf(LIVE.values())){var p=findPlayer(server,rt.r.owner);if(p!=null)end(p,Exit.SHUTDOWN);if(LIVE.containsKey(rt.r.id))retire(server,rt,Exit.SHUTDOWN);}LIVE.clear();OWNERS.clear();REQUESTS.clear();current=null;}
    public static String diagnostics(MinecraftServer server){var d=get(server);return "Communions="+LIVE.size()+", recorded="+d.records.size()+", cleanup="+d.records.values().stream().filter(r->!r.cleaned&&r.state==State.CLEANUP).count()+", source tickets<="+(LIVE.size()*9)+", last block ops="+lastBlockOperations+", max job ms="+(maximumJobNanos/1_000_000.0);}
    private CommunionManager(){}
}
