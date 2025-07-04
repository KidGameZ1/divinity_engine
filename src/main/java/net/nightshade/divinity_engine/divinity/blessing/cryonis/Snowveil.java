package net.nightshade.divinity_engine.divinity.blessing.cryonis;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

import java.awt.*;

/**
 * A blessing that provides invisibility abilities in cold or rainy conditions.
 * The blessing allows players to become invisible by crouching when in appropriate weather conditions.
 */
public class Snowveil extends Blessings {
    /**
     * Creates a new Snowveil blessing.
     *
     * @param neededFavor Amount of favor required to activate this blessing
     * @param cooldown    Cooldown period between activations in seconds
     * @param isActive    Whether this blessing starts active
     * @param canToggle   Whether this blessing can be toggled on/off
     * @param textColor   Color used for blessing text in UI
     */
    public Snowveil(int neededFavor, int cooldown, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isActive, canToggle, textColor);
    }

    /**
     * Called when the blessed entity takes damage.
     * Removes invisibility effect if the entity has it.
     *
     * @param instance The blessing instance
     * @param event    The damage event
     * @return True if the event was handled
     */
    @Override
    public boolean onTakenDamage(BlessingsInstance instance, LivingDamageEvent event) {
        if (event.getEntity().hasEffect(MobEffects.INVISIBILITY) || !(event.getEntity() instanceof Player && ((Player) event.getEntity()).isSpectator()))
            event.getEntity().setInvisible(false);
        return super.onTakenDamage(instance, event);
    }

    /**
     * Called when the blessed entity deals damage.
     * Removes invisibility effect if the entity has it.
     *
     * @param instance The blessing instance
     * @param player   The entity dealing damage
     * @param event    The hurt event
     * @return True if the event was handled
     */
    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        if (player.hasEffect(MobEffects.INVISIBILITY) || !(player instanceof Player && ((Player) player).isSpectator()))
            player.setInvisible(false);
        return super.onDamageEntity(instance, player, event);
    }

    /**
     * Called every tick while the blessing is active.
     * Manages invisibility based on weather conditions and player crouching state.
     *
     * @param instance The blessing instance
     * @param living   The affected living entity
     * @return True if the tick was handled
     */
    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        Level world = living.level();
        System.out.println(world.getBiome(BlockPos.containing(living.getX(), living.getY(), living.getZ())).value().getBaseTemperature()*100);
        if (world.isRaining() || world.getBiome(BlockPos.containing(living.getX(), living.getY(), living.getZ())).value().getBaseTemperature()*100 < 0) {
            if (living instanceof Player player){
                if (player.hasEffect(MobEffects.INVISIBILITY) || !(player instanceof Player && ((Player) player).isSpectator())) {
                    if (player.isShiftKeyDown()) {
                        living.setInvisible(true);
                    }else {
                        living.setInvisible(false);
                    }
                }
            }
        }else {
            if (living.hasEffect(MobEffects.INVISIBILITY) || !(living instanceof Player && ((Player) living).isSpectator()))
                living.setInvisible(false);
        }
        return super.onTick(instance, living);
    }
}
