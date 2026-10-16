package net.nightshade.divinity_engine.network.events.divinity.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.util.divinity.trials.*;

@Mod.EventBusSubscriber(modid = "divinity_engine")
public final class TrialArenaProtection {
    public static boolean protectedAt(LevelAccessor level, BlockPos pos) {
        return level instanceof ServerLevel server && server.dimension().equals(DimensionRegistry.GODLY_TRIALS)
                && TrialArenaJobs.get(server.getServer()).protects(pos);
    }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void breaking(BlockEvent.BreakEvent e) {
        if (protectedAt(e.getLevel(), e.getPos())) e.setCanceled(true);
    }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void placing(BlockEvent.EntityPlaceEvent e) {
        if (protectedAt(e.getLevel(), e.getPos()) || e instanceof BlockEvent.EntityMultiPlaceEvent multi
                && multi.getReplacedBlockSnapshots().stream().anyMatch(s -> protectedAt(e.getLevel(), s.getPos()))) e.setCanceled(true);
    }
    @SubscribeEvent public static void bucket(PlayerInteractEvent.RightClickBlock e) {
        if (e.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem
                && (protectedAt(e.getLevel(), e.getPos()) || e.getFace() != null && protectedAt(e.getLevel(), e.getPos().relative(e.getFace())))) e.setCanceled(true);
    }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void fillBucket(net.minecraftforge.event.entity.player.FillBucketEvent e) {
        if (e.getTarget() instanceof net.minecraft.world.phys.BlockHitResult hit
                && (protectedAt(e.getLevel(), hit.getBlockPos()) || protectedAt(e.getLevel(), hit.getBlockPos().relative(hit.getDirection())))) e.setCanceled(true);
    }
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate e) {
        e.getAffectedBlocks().removeIf(pos -> protectedAt(e.getLevel(), pos));
    }
    @SubscribeEvent public static void grief(EntityMobGriefingEvent e) {
        if (protectedAt(e.getEntity().level(), e.getEntity().blockPosition())) e.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }
    @SubscribeEvent public static void piston(PistonEvent.Pre e) {
        // A piston can push twelve blocks; also check side branches of slime/honey assemblies.
        if (e.getLevel() instanceof ServerLevel level && level.dimension().equals(DimensionRegistry.GODLY_TRIALS)
                && TrialArenaJobs.get(level.getServer()).intersects(new net.minecraft.world.phys.AABB(e.getPos()).inflate(13))) e.setCanceled(true);
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !level.dimension().equals(DimensionRegistry.GODLY_TRIALS)) return;
        Entity entity = e.getEntity();
        var tag = entity.getPersistentData();
        if (tag.hasUUID("divinity_engine.trial_session")) {
            var session = TrialSessionManager.getSessionById(tag.getUUID("divinity_engine.trial_session"));
            if (session == null || session.isTerminal()) { e.setCanceled(true); entity.discard(); }
            else session.trackEntity(entity);
            return;
        }
        Entity owner = entity instanceof Vex vex ? vex.getOwner()
                : entity instanceof Projectile projectile ? projectile.getOwner()
                : entity instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs ? fangs.getOwner() : null;
        if (owner != null) {
            var session = TrialSessionManager.getSession(owner);
            if (session != null && !session.isTerminal()) session.trackEntity(entity);
        }
    }
}
