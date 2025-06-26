package net.nightshade.divinity_engine.divinity.blessing.solarius;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;

public class RadiantStrike extends Blessings {
    public RadiantStrike(int neededFavor, int cooldown, boolean hasTickCooldown) {
        super(neededFavor, cooldown, hasTickCooldown);
    }

    @Override
    public void onJump(BlessingsInstance instance, LivingEvent.LivingJumpEvent event) {

        event.getEntity().kill();

        super.onJump(instance, event);
    }
}
