package net.nightshade.divinity_engine.divinity.blessing.terra;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;

import java.awt.*;
import java.util.List;

public class WildheartBloom extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public WildheartBloom(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();

        if (!instance.getOrCreateTag().contains("allies_list"))
            instance.getOrCreateTag().put("allies_list", new CompoundTag());
        if (!instance.getOrCreateTag().contains("time"))
            instance.getOrCreateTag().putInt("time", 0);
        if (!instance.getOrCreateTag().contains("time_limit"))
            instance.getOrCreateTag().putInt("time_limit", 600);

        return instance;
    }

    @Override
    public boolean onRightClickEntity(BlessingsInstance instance, PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().isShiftKeyDown()){
            if (event.getTarget() instanceof Player) {
                if (instance.getOrCreateTag().getCompound("allies_list").getUUID(event.getTarget().getDisplayName().getString()) != event.getTarget().getUUID() || !instance.getOrCreateTag().getCompound("allies_list").contains(event.getTarget().getDisplayName().getString())) {
                    instance.getOrCreateTag().getCompound("allies_list").putUUID(event.getEntity().getDisplayName().getString(), event.getTarget().getUUID());
                    event.getEntity().displayClientMessage(Component.literal("You have added " + event.getTarget().getDisplayName().getString() + " to your allies list!").withStyle(style -> style.withColor(Color.GREEN.getRGB())), true);
                    if (!event.isCanceled()) {
                        BlessingVisuals.link(instance, event.getEntity(), event.getEntity().getBoundingBox().getCenter(),
                                event.getTarget().getBoundingBox().getCenter(), Cue.MARK);
                    }
                }
            }
        }
        return super.onRightClickEntity(instance, event);
    }

    @Override
    public boolean onToggleOff(BlessingsInstance instance, LivingEntity entity) {
        instance.getOrCreateTag().putInt("time", 0);
        return super.onToggleOff(instance, entity);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {

        if (instance.getOrCreateTag().getInt("time") >= instance.getOrCreateTag().getInt("time_limit")) {
            instance.onToggleOff(living);
            return true;
        }

        if(instance.isToggled()) {
            if (living instanceof Player player) System.out.println(instance.getOrCreateTag().getInt("time")+"/"+instance.getOrCreateTag().getInt("time_limit"));
            if (living.level() instanceof ServerLevel serverLevel) {
                Vec3 position = living.position();
                AABB affectedArea = new AABB(
                        position.x - 6,
                        position.y - 6,
                        position.z - 6,
                        position.x + 6,
                        position.y + 6,
                        position.z + 6
                );
                List<LivingEntity> livingEntityList = serverLevel.getEntitiesOfClass(LivingEntity.class, affectedArea);
                livingEntityList.remove(living);

                if (livingEntityList.size() > 1) {
                    for (LivingEntity entity : livingEntityList) {
                        if (!livingEntityList.isEmpty()) {
                            if (!instance.getOrCreateTag().getCompound("allies_list").contains(entity.getDisplayName().getString()) || entity instanceof TamableAnimal animal && animal.getOwner() != living && entity != living) {
                                livingEntityList.remove(entity);
                            } else {
                                DivinityEngineMod.waitInSeconds(2, () -> {
                                    if (entity.getHealth() < entity.getMaxHealth()) {
                                        float healthBefore = entity.getHealth();
                                        entity.heal(0.05f);
                                        if (entity.getHealth() > healthBefore && entity.level() == living.level()) {
                                            BlessingVisuals.at(instance, living, entity.getBoundingBox().getCenter(), Cue.HEAL, 1.0);
                                        }
                                    }
                                });
                            }
                        }else break;
                    }
                }else if (livingEntityList.size() == 1) {

                }

                DivinityEngineMod.waitInSeconds(2, ()->{
                    if (living.getHealth() < living.getMaxHealth()) {
                        float healthBefore = living.getHealth();
                        living.heal(0.05f);
                        if (living.getHealth() > healthBefore) {
                            BlessingVisuals.pulse(instance, living, Cue.HEAL);
                        }
                    }
                });
                if (instance.getOrCreateTag().getInt("time") < instance.getOrCreateTag().getInt("time_limit")) {
                    instance.getOrCreateTag().putInt("time", instance.getOrCreateTag().getInt("time") + 1);
                }
                BlessingVisuals.at(instance, living, position.add(0, 0.08, 0), Cue.AURA, 6.0);

            }
        }

        return super.onTick(instance, living);
    }
}
