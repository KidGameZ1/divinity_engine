package net.nightshade.divinity_engine.util.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.api.content.ContentBehaviorContext;
import net.nightshade.divinity_engine.api.content.DivinityContent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

public final class ContentBlessing extends Blessings {
    private static final ThreadLocal<Boolean> EXECUTING = ThreadLocal.withInitial(() -> false);
    private final ResourceLocation id;
    private final ContentSchema.Blessing definition;

    public ContentBlessing(ResourceLocation id, ContentSchema.Blessing definition) {
        super(definition.favor(), definition.cooldown(), definition.mode().equals("passive"),
                !definition.mode().equals("passive"), definition.mode().equals("toggle"), new Color(definition.color()));
        this.id = id;
        this.definition = definition;
    }

    @Override public boolean onPressed(BlessingsInstance instance, LivingEntity owner) {
        return definition.mode().equals("active") && run("pressed", instance, owner, null, null);
    }
    @Override public boolean onTick(BlessingsInstance instance, LivingEntity owner) {
        if (definition.mode().equals("active") || canToggle() && !instance.isToggled()) return false;
        if (Math.floorMod(owner.level().getGameTime() + owner.getId(), definition.interval()) != 0) return false;
        return run("tick", instance, owner, null, null);
    }
    @Override public void onToggleOn(BlessingsInstance instance, LivingEntity owner) {
        if (run("toggle_on", instance, owner, null, null)) instance.startCooldown(owner);
    }
    @Override public boolean onToggleOff(BlessingsInstance instance, LivingEntity owner) {
        return run("toggle_off", instance, owner, null, null);
    }
    @Override public boolean onDamageEntity(BlessingsInstance instance, LivingEntity owner, LivingHurtEvent event) {
        return !event.isCanceled() && event.getAmount() > 0 && run("damage_entity", instance, owner, event.getEntity(), event);
    }
    @Override public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        return !event.isCanceled() && event.getAmount() > 0 && run("taken_damage", instance, event.getEntity(),
                event.getSource().getEntity() instanceof LivingEntity living ? living : null, event);
    }
    @Override public boolean onKillEntity(BlessingsInstance instance, LivingEntity owner, LivingDeathEvent event) {
        return !event.isCanceled() && run("kill_entity", instance, owner, event.getEntity(), event);
    }

    private boolean run(String hook, BlessingsInstance instance, LivingEntity owner, LivingEntity target, Object nativeEvent) {
        if (!(owner.level() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || instance.isOnCooldown() || EXECUTING.get() || !owner.isAlive()) return false;
        if (canToggle() && !hook.startsWith("toggle_") && !instance.isToggled()) return false;
        EXECUTING.set(true);
        try {
            ContentBehaviorContext context = new ContentBehaviorContext(id, hook, owner, target, instance, nativeEvent);
            for (var action : definition.hooks().getOrDefault(hook, List.of())) {
                for (LivingEntity recipient : targets(action, context)) {
                    if (apply(action, context, recipient)) context.markSuccessful();
                }
            }
            DivinityContent.dispatch(context);
            if (context.isSuccessful() && definition.texture() != null)
                BlessingVisuals.pulse(instance, owner, hook.equals("tick") ? BlessingVisuals.Cue.AURA : BlessingVisuals.Cue.CAST);
            return context.isSuccessful();
        } finally { EXECUTING.remove(); }
    }

    private static List<LivingEntity> targets(ContentSchema.Action action, ContentBehaviorContext context) {
        LivingEntity owner = context.getOwner();
        return switch (action.target()) {
            case "self" -> List.of(owner);
            case "target" -> context.getTarget() != null && context.getTarget().level() == owner.level()
                    && context.getTarget().isAlive() ? List.of(context.getTarget()) : List.of();
            case "nearby_hostiles" -> context.getLevel().getEntitiesOfClass(LivingEntity.class,
                            owner.getBoundingBox().inflate(action.radius()),
                            entity -> entity instanceof Enemy && !(entity instanceof Player) && entity.isAlive()
                                    && entity != owner && !entity.isAlliedTo(owner)
                                    && !(entity instanceof TamableAnimal pet && pet.isTame())
                                    && owner.distanceToSqr(entity) <= action.radius() * action.radius() && owner.hasLineOfSight(entity))
                    .stream().sorted(Comparator.comparingDouble(owner::distanceToSqr)).limit(16).toList();
            default -> List.of();
        };
    }

    private static boolean apply(ContentSchema.Action action, ContentBehaviorContext context, LivingEntity target) {
        switch (action.type()) {
            case "heal": {
                float before = target.getHealth();
                target.heal((float) action.amount());
                return target.getHealth() > before;
            }
            case "effect":
                return target.addEffect(new MobEffectInstance(ForgeRegistries.MOB_EFFECTS.getValue(action.effect()),
                        action.ticks(), action.amplifier()), context.getOwner());
            case "ignite":
                if (target.fireImmune()) return false;
                int before = target.getRemainingFireTicks();
                target.setSecondsOnFire((action.ticks() + 19) / 20);
                return target.getRemainingFireTicks() > before;
            case "damage":
                if (target == context.getOwner() || target.isAlliedTo(context.getOwner())) return false;
                return target.hurt(context.getOwner() instanceof Player player ? context.getLevel().damageSources().playerAttack(player)
                        : context.getLevel().damageSources().mobAttack(context.getOwner()), (float) action.amount());
            case "dash":
                Vec3 direction = context.getOwner().getLookAngle();
                Vec3 horizontal = new Vec3(direction.x, 0, direction.z).normalize().scale(action.amount());
                if (horizontal.lengthSqr() == 0) return false;
                target.setDeltaMovement(horizontal.x, target.getDeltaMovement().y, horizontal.z);
                target.hurtMarked = true;
                return true;
            default: throw new IllegalStateException("Unvalidated action " + action.type());
        }
    }
}
