//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.nightshade.divinity_engine.network.cap.player.gods.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.network.events.divinity.gods.ContactGodEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;

@EventBusSubscriber(
    modid = DivinityEngineMod.MODID,
    bus = Bus.FORGE
)
public class GodsServerEventListener {
    @SubscribeEvent
    public static void onContactGod(ContactGodEvent e) {
        Entity entity = e.getEntity();
        if (!entity.level().isClientSide()) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;
                e.getInstance().onContact(living, e);
            }

        }
    }
    @SubscribeEvent
    public static void onPrayedToGod(PrayEvent e) {
        Entity entity = e.getLiving();
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            e.getInstance().onPrayedTo(e);
        }
    }
    @SubscribeEvent
    public static void onOfferItemToGod(OfferEvent.OfferItemEvent e) {
        Entity entity = e.getOfferer();
        ItemStack stack = e.getOffer();
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            if (!stack.isEmpty())
                e.getInstance().onOfferedItems(e);
        }
    }
    @SubscribeEvent
    public static void onOfferEntityToGod(OfferEvent.OfferEntityEvent e) {
        Entity offerer = e.getOfferer();
        Entity offer = e.getOffer();
        if (offerer instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)offerer;
            if (offer != null)
                if (offer instanceof LivingEntity liv){
                    liv.kill();
                }
                e.getInstance().onOfferedEntity(e);
        }
    }

}
