package net.nightshade.divinity_engine.util.divinity.blessings;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Controls only affected mobs, without scanning the world or changing their AI goals. */
@Mod.EventBusSubscriber(modid = "divinity_engine")
public final class WarlordConfusion {
    private record Control(UUID opponent, UUID previous, long expires) {}
    private static final Map<Mob, Control> CONTROLLED = new WeakHashMap<>();

    private WarlordConfusion() {}

    public static void apply(Mob mob, LivingEntity opponent, int duration) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        var prior = CONTROLLED.get(mob);
        UUID previous = prior == null ? (mob.getTarget() == null ? null : mob.getTarget().getUUID()) : prior.previous;
        CONTROLLED.put(mob, new Control(opponent == null ? null : opponent.getUUID(), previous, level.getGameTime() + duration));
        mob.setTarget(opponent);
        mob.getNavigation().stop();
    }

    private static LivingEntity resolve(Mob mob, UUID id) {
        if (id == null || !(mob.level() instanceof ServerLevel level)) return null;
        return level.getEntity(id) instanceof LivingEntity target && target.isAlive() && !mob.isAlliedTo(target)
                ? target : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void target(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        var control = CONTROLLED.get(mob);
        if (control != null && mob.level().getGameTime() < control.expires)
            event.setNewTarget(resolve(mob, control.opponent));
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;
        var control = CONTROLLED.get(mob);
        if (control == null) return;
        if (mob.level().getGameTime() >= control.expires || !mob.isAlive()) {
            CONTROLLED.remove(mob);
            mob.setTarget(resolve(mob, control.previous));
        } else if (mob.tickCount % 5 == 0) {
            var target = resolve(mob, control.opponent);
            if (mob.getTarget() != target) mob.setTarget(target);
        }
    }

    @SubscribeEvent public static void stop(ServerStoppedEvent event) { CONTROLLED.clear(); }
}
