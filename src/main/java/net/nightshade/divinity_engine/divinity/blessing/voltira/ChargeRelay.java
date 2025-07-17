package net.nightshade.divinity_engine.divinity.blessing.voltira;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.awt.*;

public class ChargeRelay extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public ChargeRelay(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public boolean onDamageEntity(BlessingsInstance instance, LivingEntity player, LivingHurtEvent event) {
        for (BlessingsInstance blessingsInstance : GodHelper.getAllBlessingsInstances(player)){
            if (blessingsInstance.getBlessing() instanceof Stormstep || blessingsInstance.getBlessing() instanceof TempestWrath){
                if(player.level().isThundering()){
                    if (DivinityEngineHelper.hasBlessingInSlot(blessingsInstance.getBlessing(), player)){
                        if (blessingsInstance.getCooldown() > 0){
                            BlessingsInstance newInstance = DivinityEngineHelper.getBlessingInSlot((Player) player, DivinityEngineHelper.getBlessingsSlot(player, blessingsInstance.getBlessing()));
                            assert newInstance != null;
                            newInstance.decreaseCooldown(2);
                            DivinityEngineHelper.setBlessingSlot((Player) player, DivinityEngineHelper.getBlessingsSlot(player, blessingsInstance.getBlessing()), newInstance);
                        }
                    }
                }else {
                    if (DivinityEngineHelper.hasBlessingInSlot(blessingsInstance.getBlessing(), player)){
                        if (blessingsInstance.getCooldown() > 0){
                            BlessingsInstance newInstance = DivinityEngineHelper.getBlessingInSlot((Player) player, DivinityEngineHelper.getBlessingsSlot(player, blessingsInstance.getBlessing()));
                            assert newInstance != null;
                            newInstance.decreaseCooldown(1);
                            DivinityEngineHelper.setBlessingSlot((Player) player, DivinityEngineHelper.getBlessingsSlot(player, blessingsInstance.getBlessing()), newInstance);
                        }
                    }
                }
            }
        }

        return super.onDamageEntity(instance, player, event);
    }
}
