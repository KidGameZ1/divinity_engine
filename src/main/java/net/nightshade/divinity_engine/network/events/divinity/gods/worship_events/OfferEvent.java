package net.nightshade.divinity_engine.network.events.divinity.gods.worship_events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.GodsEvent;

public class OfferEvent extends GodsEvent {
    private final LivingEntity offerer;

    public OfferEvent(BaseGodInstance instance, LivingEntity offerer) {
        super(instance);
        this.offerer = offerer;
    }

    public LivingEntity getOfferer() {
        return offerer;
    }

    public static class OfferItemEvent extends OfferEvent {
        private ItemStack offeredItem;

        public OfferItemEvent(BaseGodInstance instance, LivingEntity offerer, ItemStack offeredItem) {
            super(instance, offerer);
            this.offeredItem = offeredItem;
        }

        public ItemStack getOffer() {
            return offeredItem;
        }

        public void setOfferedItem(ItemStack offeredItem) {
            this.offeredItem = offeredItem;
        }
    }

    public static class OfferEntityEvent extends OfferEvent {
        private Entity offeredEntity;

        public OfferEntityEvent(BaseGodInstance instance, LivingEntity offerer, Entity offeredEntity) {
            super(instance, offerer);
            this.offeredEntity = offeredEntity;
        }

        public Entity getOffer() {
            return offeredEntity;
        }

        public void setOfferedItem(LivingEntity offeredEntity) {
            this.offeredEntity = offeredEntity;
        }
    }
}
