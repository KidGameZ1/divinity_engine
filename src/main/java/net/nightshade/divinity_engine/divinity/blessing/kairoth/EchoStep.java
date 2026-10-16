package net.nightshade.divinity_engine.divinity.blessing.kairoth;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.blessings.RewindHistory;

import java.awt.Color;

public class EchoStep extends Blessings {
    public EchoStep(int favor, int cooldown, boolean passive, boolean active, boolean toggle, Color color) {
        super(favor, cooldown, passive, active, toggle, color);
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        RewindHistory.record(instance, living, 60);
        return false;
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        boolean moved = RewindHistory.rewind(instance, living, 60);
        if (!moved && !living.level().isClientSide)
            living.sendSystemMessage(Component.literal("Echo Step needs three seconds of recent, safe movement in this dimension."));
        return moved;
    }
}
