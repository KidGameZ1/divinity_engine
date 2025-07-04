package net.nightshade.divinity_engine.divinity.curse.elaris;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

import java.util.Random;

public class LostWisdom extends Curse {
    @Override
    public void onEXPDrop(CurseInstance instance, LivingExperienceDropEvent event) {
        Random random = new Random();

        if (random.nextFloat() < 0.25f){
            if (event.getEntity() instanceof Player player){
//                player.giveExperiencePoints(-event.getDroppedExperience());
                event.setDroppedExperience(event.getDroppedExperience() * 2);
            }
        }

        super.onEXPDrop(instance, event);
    }
}
