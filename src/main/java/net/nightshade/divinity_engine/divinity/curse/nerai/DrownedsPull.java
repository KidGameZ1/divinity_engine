package net.nightshade.divinity_engine.divinity.curse.nerai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

import java.util.UUID;

public class DrownedsPull extends Curse {
    @Override
    public void onTick(CurseInstance instance, LivingEntity living) {
        if (living.isInWaterOrBubble()){
            living.setDeltaMovement(living.getDeltaMovement().x, living.getDeltaMovement().y-0.01, living.getDeltaMovement().z);
        }

        super.onTick(instance, living);
    }
}
