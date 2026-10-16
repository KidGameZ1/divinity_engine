package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.*;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.entity.TranceBodyEntity;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.util.divinity.innerworld.*;
import java.util.*;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid="divinity_engine",bus=Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class CommunionGameTests {
    private static final String SUITE="divinity_inner_world";
    @SubscribeEvent public static void register(RegisterGameTestsEvent e){if(Arrays.asList(System.getProperty("forge.enabledGameTestNamespaces","").split(",")).contains(SUITE))for(var method:CommunionGameTests.class.getDeclaredMethods())if(method.isAnnotationPresent(GameTest.class))GameTestRegistry.register(method,Set.of("divinity_engine"));}
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_definitions")
    public static void allEighteenDefinitionsAreSafeStatueOnlyVoids(GameTestHelper h){
        h.assertTrue(InnerWorldsRegistry.values().size()==18,"Missing a built-in Inner World");
        for(var d:InnerWorldsRegistry.values()){
            h.assertTrue(d.visuals.motif().equals("void")&&d.template==null&&d.variants.isEmpty(),"Decorative scene still enabled in "+d.id);
            h.assertTrue(d.width*d.height*d.depth<=1352,"Built-in preparation unnecessarily large: "+d.id);
            for(int y=0;y<d.height;y++)for(int x=0;x<d.width;x++)for(int z=0;z<d.depth;z++){
                var state=d.scene.apply(new BlockPos(x,y,z));
                h.assertTrue(state!=null&&(state.isAir()||state.is(Blocks.BARRIER)),"Visible scenery in "+d.id);
            }
            for(var at:List.of(BlockPos.containing(d.spawn),BlockPos.containing(d.manifestation))){
                h.assertTrue(d.scene.apply(at).isAir()&&d.scene.apply(at.above()).isAir()&&d.scene.apply(at.below()).is(Blocks.BARRIER),"Unsafe arrival/statue in "+d.id);
            }
            for(int z=(int)d.spawn.z;z<=d.manifestation.z;z++){
                var at=new BlockPos((int)d.spawn.x,1,z);
                h.assertTrue(d.scene.apply(at).isAir()&&d.scene.apply(at.above()).isAir(),"Obstructed statue approach: "+d.id);
            }
        }h.succeed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_definitions")
    public static void ledgerRoundTripKeepsOwnershipAndCleanupCursor(GameTestHelper h){
        var d=new CommunionLedger();var r=new CommunionLedger.Record();r.id=CommunionTickets.newSession();r.owner=UUID.randomUUID();r.body=UUID.randomUUID();r.god=GodsRegistry.AETHON.getId();r.dimension=h.getLevel().dimension().location();r.anchor=new Vec3(1,2,3);r.slot=5;r.width=25;r.height=16;r.depth=25;r.cursor=271;r.state=CommunionLedger.State.CLEANUP;r.revision=8;d.records.put(r.id,r);
        var copy=CommunionLedger.load(d.save(new CompoundTag()));var restored=copy.records.get(r.id);
        h.assertTrue(restored.cursor==271&&restored.body.equals(r.body)&&copy.freeSlot()==0,"Recovery data lost");
        h.assertTrue(restored.width==25&&restored.height==16&&restored.depth==25&&restored.volume()==10000,"Old scenery cleanup was resized to new void bounds");h.succeed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_start",timeoutTicks=500)
    public static void rejectedAndCanceledWindupLeaveNoBody(GameTestHelper h){
        var p=player(h);var id=GodsRegistry.AETHON.getId();
        h.assertFalse(CommunionManager.begin(p,GodsRegistry.IGNAR.getId()),"Uncontacted god accepted");
        h.assertTrue(CommunionManager.begin(p,id),"Cannot begin: "+p.lastMessage);var r=CommunionManager.session(p);p.setPos(p.position().add(1,0,0));
        h.startSequence().thenWaitUntil(()->h.assertTrue(CommunionManager.session(p)==null,"Movement did not cancel"))
            .thenExecute(()->{h.assertTrue(r.body==null&&!p.getPersistentData().contains(CommunionLedger.PLAYER_KEY),"Windup leaked body/recovery");p.dispose();}).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_normal",timeoutTicks=900)
    public static void normalVisitReturnsOnePlayerAndKeepsInventory(GameTestHelper h){
        var p=player(h);var anchor=p.position();p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));begin(h,p);
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var r=CommunionManager.session(p);var b=body(h,p);h.assertTrue(b.owner().equals(p.getUUID()),"Wrong owner");
            var statues=p.serverLevel().getEntitiesOfClass(net.nightshade.divinity_engine.entity.GodManifestationEntity.class,CommunionManager.bounds(r));
            h.assertTrue(statues.size()==1&&statues.get(0).god().equals(GodsRegistry.AETHON.getId().toString()),"Missing or wrong statue");
            h.assertFalse(statues.get(0).hasCustomName(),"Void statue has a floating nameplate");
            h.assertTrue(p.serverLevel().getBlockState(p.blockPosition().below()).is(Blocks.BARRIER),"Void visitor lacks invisible support");
            h.assertFalse(b.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent(),"Body exposes inventory");
            h.assertFalse(CommunionManager.begin(p,GodsRegistry.AETHON.getId()),"Nested session accepted");
            CommunionManager.end(p,CommunionLedger.Exit.NORMAL);
            h.assertTrue(p.serverLevel()==h.getLevel()&&p.position().distanceToSqr(anchor)<1&&p.getInventory().getItem(0).getCount()==7,"Return changed inventory/location");
            h.assertTrue(b.isRemoved()&&CommunionManager.session(p)==null,"Body/session survived return");p.dispose();
            h.assertTrue(r.state==CommunionLedger.State.CLEANUP,"Cleanup not queued");
        }).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_damage",timeoutTicks=900)
    public static void bodyHitReturnsBeforeMitigationAndOnlyOnce(GameTestHelper h){
        var p=player(h);begin(h,p);
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var b=body(h,p);p.setAbsorptionAmount(8);float health=p.getHealth();var source=h.getLevel().damageSources().generic();
            h.assertTrue(b.hurt(source,5),"Body hit rejected");
            h.assertTrue(p.serverLevel()==h.getLevel()&&p.getHealth()==health&&p.getAbsorptionAmount()==3,"Armor/absorption/return ordering failed");
            h.assertFalse(b.hurt(source,5),"Repeated callback reapplied damage");h.assertFalse(p.hurt(source,5),"Same AoE hit both representations");p.dispose();
        }).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_damage",timeoutTicks=900)
    public static void canceledBodyAndOwnerAttacksDoNotInterrupt(GameTestHelper h){
        var p=player(h);begin(h,p);
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var body=body(h,p);Consumer<LivingAttackEvent> cancel=e->{if(e.getEntity()==body)e.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(cancel);
            try{h.assertFalse(body.hurt(h.getLevel().damageSources().generic(),4),"Canceled body hit forwarded");}finally{MinecraftForge.EVENT_BUS.unregister(cancel);}
            Consumer<LivingAttackEvent> cancelOwner=e->{if(e.getEntity()==p)e.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(cancelOwner);
            try{h.assertFalse(body.hurt(h.getLevel().damageSources().generic(),4),"Canceled owner hit applied");}finally{MinecraftForge.EVENT_BUS.unregister(cancelOwner);}
            h.assertTrue(CommunionManager.session(p).state==CommunionLedger.State.ACTIVE,"Canceled hit interrupted");CommunionManager.end(p,CommunionLedger.Exit.NORMAL);p.dispose();
        }).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_damage",timeoutTicks=900)
    public static void lethalHitDiesInOriginAndTotemUsesRealInventory(GameTestHelper h){
        var p=player(h);begin(h,p);
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
            body(h,p).hurt(h.getLevel().damageSources().generic(),100);
            h.assertTrue(p.serverLevel()==h.getLevel()&&p.isAlive()&&p.getOffhandItem().isEmpty(),"Totem not consumed on real returned player");
            p.invulnerableTime=0;p.hurt(h.getLevel().damageSources().genericKill(),100);
            h.assertTrue(!p.isAlive()&&p.serverLevel()==h.getLevel()&&CommunionManager.session(p)==null,"Death in realm or stale session");p.dispose();
        }).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_damage",timeoutTicks=900)
    public static void realPlayerDamageCannotBeEscaped(GameTestHelper h){
        var p=player(h);begin(h,p);h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{float health=p.getHealth();p.hurt(p.damageSources().magic(),3);h.assertTrue(p.serverLevel()==h.getLevel()&&p.getHealth()==health-3,"Ongoing harm escaped");p.dispose();}).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_environment",timeoutTicks=900)
    public static void supportLossReturnsAndInvalidatesRequests(GameTestHelper h){
        var p=player(h);begin(h,p);final UUID[] id={null};final int[] revision={0};
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{var r=CommunionManager.session(p);id[0]=r.id;revision[0]=r.revision;var b=body(h,p);h.getLevel().setBlock(b.blockPosition().below(),Blocks.AIR.defaultBlockState(),3);})
            .thenWaitUntil(()->h.assertTrue(CommunionManager.session(p)==null,"Lost support did not interrupt"))
            .thenExecute(()->{h.assertFalse(CommunionManager.validRequest(p,id[0],revision[0]),"Stale dialogue accepted");p.dispose();}).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_hostiles",timeoutTicks=1200)
    public static void zombieCanThreatenBodyWithoutOwnerNearby(GameTestHelper h){
        var p=player(h);var anchor=p.position();begin(h,p);final Zombie[] zombie={null};
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var z=EntityType.ZOMBIE.create(h.getLevel());z.moveTo(anchor.x+2,anchor.y,anchor.z,0,0);z.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));z.setPersistenceRequired();h.getLevel().addFreshEntity(z);zombie[0]=z;
        }).thenWaitUntil(()->h.assertTrue(CommunionManager.session(p)==null,"Zombie has not attacked the remote body"))
            .thenExecute(()->{h.assertTrue(p.serverLevel()==h.getLevel()&&p.getHealth()<20,"Zombie did not cause physical damage");zombie[0].discard();p.dispose();}).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_recovery",timeoutTicks=900)
    public static void sameGodSessionsAndMissingBodyRemainIsolated(GameTestHelper h){
        var a=player(h);var b=player(h);b.setPos(b.position().add(3,0,0));begin(h,a);begin(h,b);
        h.startSequence().thenWaitUntil(()->{active(h,a);active(h,b);}).thenExecute(()->{
            h.assertTrue(CommunionManager.session(a).slot!=CommunionManager.session(b).slot,"Overlapping instances");body(h,a).discard();
        }).thenWaitUntil(()->h.assertTrue(CommunionManager.session(a)==null,"Missing body stranded owner"))
            .thenExecute(()->{h.assertTrue(CommunionManager.session(b).state==CommunionLedger.State.ACTIVE,"Other session was closed");CommunionManager.end(b,CommunionLedger.Exit.NORMAL);a.dispose();b.dispose();}).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_recovery",timeoutTicks=900)
    public static void loginReconcilesOrphanedRecoveryWithoutResuming(GameTestHelper h){
        var p=player(h);begin(h,p);h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var saved=p.getPersistentData().getCompound(CommunionLedger.PLAYER_KEY).copy();CommunionManager.end(p,CommunionLedger.Exit.NORMAL);
            p.getPersistentData().put(CommunionLedger.PLAYER_KEY,saved);CommunionManager.recover(p);
            h.assertTrue(!p.getPersistentData().contains(CommunionLedger.PLAYER_KEY)&&CommunionManager.session(p)==null&&!CommunionManager.inRealm(p),"Recovery reactivated projection");p.dispose();
        }).thenSucceed();
    }
    private static void begin(GameTestHelper h,TestPlayer p){h.assertTrue(CommunionManager.begin(p,GodsRegistry.AETHON.getId()),"Entry refused: "+p.lastMessage);}
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_ranged",timeoutTicks=1000)
    public static void arrowHitsBodyAndPreservesAttribution(GameTestHelper h){
        var p=player(h);begin(h,p);final Arrow[] arrow={null};
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var body=body(h,p);var a=new Arrow(h.getLevel(),body.getX(),body.getY()+1,body.getZ()-4);a.setBaseDamage(3);a.shoot(0,0,1,1.4F,0);arrow[0]=a;h.getLevel().addFreshEntity(a);
        }).thenWaitUntil(()->h.assertTrue(CommunionManager.session(p)==null,"Arrow did not interrupt"))
            .thenExecute(()->{h.assertTrue(p.serverLevel()==h.getLevel()&&p.getHealth()<20,"Arrow damage missing");arrow[0].discard();p.dispose();}).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_ranged",timeoutTicks=1200)
    public static void skeletonTargetsAndShootsBody(GameTestHelper h){
        var p=player(h);begin(h,p);final net.minecraft.world.entity.monster.Skeleton[] skeleton={null};
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var b=body(h,p);var s=EntityType.SKELETON.create(h.getLevel());s.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));s.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));s.moveTo(b.getX()+4,b.getY(),b.getZ(),90,0);s.setPersistenceRequired();skeleton[0]=s;h.getLevel().addFreshEntity(s);
        }).thenWaitUntil(()->h.assertTrue(CommunionManager.session(p)==null,"Skeleton did not threaten body"))
            .thenExecute(()->{h.assertTrue(p.getHealth()<20,"Skeleton hit caused no damage");skeleton[0].discard();p.dispose();}).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_combat_rules",timeoutTicks=1000)
    public static void armorUsesOnlyRealPlayerAndPvpTeamIsRespected(GameTestHelper h){
        var p=player(h);var control=player(h);control.setPos(control.position().add(4,0,0));begin(h,p);
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var team=h.getLevel().getScoreboard().addPlayerTeam("c"+p.getUUID().toString().substring(0,8));team.setAllowFriendlyFire(false);
            h.getLevel().getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);h.getLevel().getScoreboard().addPlayerToTeam(control.getScoreboardName(),team);
            h.assertFalse(body(h,p).hurt(h.getLevel().damageSources().playerAttack(control),4),"Friendly fire bypassed via proxy");
            h.assertTrue(CommunionManager.session(p)!=null,"Blocked PvP interrupted communion");h.getLevel().getScoreboard().removePlayerTeam(team);
            p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));control.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
            var damage=h.getLevel().damageSources().generic();control.hurt(damage,8);body(h,p).hurt(damage,8);
            h.assertTrue(Math.abs(p.getHealth()-control.getHealth())<.001&&p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==control.getItemBySlot(EquipmentSlot.CHEST).getDamageValue(),"Double mitigation or armor wear");p.dispose();control.dispose();
        }).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_aoe",timeoutTicks=1000)
    public static void explosionOnlyProcessesOneOwnerAttack(GameTestHelper h){
        var p=player(h);begin(h,p);
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var b=body(h,p);int[] attacks={0};Consumer<LivingAttackEvent> count=e->{if(e.getEntity()==p)attacks[0]++;};MinecraftForge.EVENT_BUS.addListener(count);
            try{h.getLevel().explode(null,b.getX()+2,b.getY()+1,b.getZ(),1.5F,net.minecraft.world.level.Level.ExplosionInteraction.NONE);}finally{MinecraftForge.EVENT_BUS.unregister(count);}
            h.assertTrue(attacks[0]==1&&p.serverLevel()==h.getLevel(),"Explosion duplicated or lost owner damage: "+attacks[0]);p.dispose();
        }).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_aoe",timeoutTicks=1000)
    public static void sweepingAttackCannotHitReturnedOwnerAgain(GameTestHelper h){
        var p=player(h);var attacker=player(h);attacker.setPos(attacker.position().add(2,0,0));begin(h,p);
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            int[] attacks={0};Consumer<LivingAttackEvent> count=e->{if(e.getEntity()==p)attacks[0]++;};MinecraftForge.EVENT_BUS.addListener(count);
            attacker.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));attacker.setOnGround(true);
            attacker.prepareSweep();
            boolean pvp=p.server.isPvpAllowed();p.server.setPvpAllowed(true);
            try{attacker.attack(body(h,p));}finally{p.server.setPvpAllowed(pvp);MinecraftForge.EVENT_BUS.unregister(count);}
            h.assertTrue(attacker.swept&&attacks[0]==1&&p.serverLevel()==h.getLevel(),"Sweep duplicated or lost damage: "+attacks[0]+", swept="+attacker.swept);p.dispose();attacker.dispose();
        }).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_cleanup",timeoutTicks=1000)
    public static void cleanupReleasesOnlyAssignedBoundsAndSlot(GameTestHelper h){
        var p=player(h);begin(h,p);final CommunionLedger.Record[] record={null};
        h.startSequence().thenWaitUntil(()->active(h,p)).thenExecute(()->{
            var r=CommunionManager.session(p);record[0]=r;var world=p.serverLevel();world.setBlock(r.origin().offset(r.width+2,1,2),Blocks.DIAMOND_BLOCK.defaultBlockState(),2);CommunionManager.end(p,CommunionLedger.Exit.NORMAL);
        }).thenWaitUntil(()->h.assertTrue(record[0].cleaned,"Waiting for bounded cleanup"))
            .thenExecute(()->{var r=record[0];var world=p.server.getLevel(DimensionRegistry.INNER_WORLD);h.assertTrue(world.getBlockState(r.origin().offset(r.width+2,1,2)).is(Blocks.DIAMOND_BLOCK),"Cleanup escaped bounds");h.assertTrue(world.getBlockState(r.origin()).isAir(),"Floor not deleted");h.assertFalse(CommunionLedger.get(p.server).records.containsKey(r.id),"Released session remains recorded");p.dispose();}).thenSucceed();
    }
    @GameTest(templateNamespace="divinity_engine",template="pocket_dimension",batch="communion_legacy_cleanup",timeoutTicks=1000)
    public static void olderLargerSceneCleansWithoutRegisteredGod(GameTestHelper h){
        var server=h.getLevel().getServer();var ledger=CommunionLedger.get(server);
        var world=server.getLevel(DimensionRegistry.INNER_WORLD);
        var saved=new CommunionLedger.Record();saved.id=CommunionTickets.newSession();saved.owner=UUID.randomUUID();
        saved.god=new net.minecraft.resources.ResourceLocation("divinity_engine","aureth");
        saved.dimension=h.getLevel().dimension().location();saved.anchor=Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO));
        saved.slot=ledger.freeSlot();saved.width=25;saved.height=16;saved.depth=25;
        saved.state=CommunionLedger.State.CLEANUP;saved.returned=true;saved.definition="divinity_engine:aureth_inner_world@1";
        var restored=CommunionLedger.Record.load(saved.save());
        var oldCorner=restored.origin().offset(24,15,24);var outside=restored.origin().offset(27,1,24);
        world.setBlock(oldCorner,Blocks.GOLD_BLOCK.defaultBlockState(),2);
        world.setBlock(outside,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        ledger.records.put(restored.id,restored);ledger.setDirty();
        h.startSequence().thenWaitUntil(()->h.assertTrue(restored.cleaned,"Waiting for old scene cleanup"))
            .thenExecute(()->{
                h.assertTrue(world.getBlockState(oldCorner).isAir(),"Old outer scenery survived smaller replacement");
                h.assertTrue(world.getBlockState(outside).is(Blocks.DIAMOND_BLOCK),"Recovery deleted outside saved bounds");
                h.assertFalse(ledger.records.containsKey(restored.id),"Recovered slot was not released");
                world.setBlock(outside,Blocks.AIR.defaultBlockState(),2);
            }).thenSucceed();
    }
    private static void active(GameTestHelper h,TestPlayer p){var r=CommunionManager.session(p);h.assertTrue(r!=null&&r.state==CommunionLedger.State.ACTIVE,"Waiting for realm; "+p.lastMessage);}
    private static TranceBodyEntity body(GameTestHelper h,TestPlayer p){var r=CommunionManager.session(p);return (TranceBodyEntity)h.getLevel().getEntity(r.body);}
    private static TestPlayer player(GameTestHelper h){
        var p=new TestPlayer(h.getLevel());var floor=h.absolutePos(new BlockPos(14,1,14));
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){h.getLevel().setBlock(floor.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);for(int y=1;y<=4;y++)h.getLevel().setBlock(floor.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        p.setPos(Vec3.atBottomCenterOf(floor.above()));p.setOnGround(true);h.getLevel().addNewPlayer(p);return p;
    }
    private static final class TestPlayer extends ServerPlayer {
        String lastMessage="";
        boolean swept;
        void prepareSweep(){attackStrengthTicker=100;setSpeed(.1F);walkDist=walkDistO;}
        @Override public void sweepAttack(){swept=true;super.sweepAttack();}
        TestPlayer(ServerLevel level){super(level.getServer(),level,new GameProfile(UUID.randomUUID(),"communion-test"));connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),this){@Override public void send(Packet<?> packet){}};
            var god=GodsRegistry.AETHON.get().createGodDefaultInstance();god.setFavor(100);PlayerGodsCapability.load(this).updateGodInstance(god,false);
        }
        @Override public void sendSystemMessage(Component message){lastMessage=message.getString();}
        @Override public boolean hasDisconnected(){return false;}
        void dispose(){CommunionManager.end(this,CommunionLedger.Exit.ADMIN);serverLevel().removePlayerImmediately(this,Entity.RemovalReason.DISCARDED);}
    }
}
