
package net.nightshade.divinity_engine.network.cap.player.gods.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.network.events.divinity.curse.CursePlayerEvent;
import net.nightshade.divinity_engine.network.events.divinity.curse.UncursePlayerEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.ContactGodEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.LoseFaithEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.util.MainPlayerCapabilityHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.Objects;

@EventBusSubscriber(
    modid = DivinityEngineMod.MODID,
    bus = Bus.FORGE
)
public class GodsServerEventListener {

    @SubscribeEvent
    public static void onGodTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Player player = event.player;
            if (!GodHelper.getContactedGodsFrom(player).getContactedGods().isEmpty()){
                for (BaseGodInstance instance : GodHelper.getContactedGodsFrom(player).getContactedGods()) {
                    if (instance.getFavor() > 100){
                        instance.setFavor(100);
                        GodHelper.getContactedGodsFrom(player).updateGodInstance(instance);
                        break;
                    } else if (instance.getFavor() < -100) {
                        instance.setFavor(-100);
                        GodHelper.getContactedGodsFrom(player).updateGodInstance(instance);
                        break;
                    }
                }
                for (BaseGodInstance godInstance : GodHelper.getContactedGodsFrom(player).getContactedGods()) {
                    if (FavorTiers.getByFavor(godInstance.getFavor()) == FavorTiers.Enemy) {
                        if (godInstance.getBaseGod().getCurse() != null) {
                            if (!(GodHelper.getContactedGodsFrom(player).getCurse(godInstance.getBaseGod().getCurse()).isPresent())) {
                                GodHelper.curse(godInstance.getBaseGod().getCurse(), godInstance.getBaseGod(), player);
                            } else {
                                break;
                            }
                        }
                    } else {
                        if (!GodHelper.getContactedGodsFrom(player).getCurses().isEmpty()) {
                            for (CurseInstance curseInstance : GodHelper.getContactedGodsFrom(player).getCurses()) {
                                if (GodHelper.getGodOrNull(player, curseInstance.getBoundGod()) != null){
                                    if (FavorTiers.getByFavor(GodHelper.getGodOrNull(player, curseInstance.getBoundGod()).getFavor()) != FavorTiers.Enemy) {
                                        GodHelper.getContactedGodsFrom(player).uncurse(curseInstance);
                                        break;
                                    } else {
                                        break;
                                    }
                                }else {
                                    GodHelper.getContactedGodsFrom(player).uncurse(curseInstance);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onContactGod(ContactGodEvent e) {
        Entity entity = e.getEntity();
        if (!entity.level().isClientSide()) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;
                if (living instanceof Player player && !GodHelper.getContactedGodsFrom(entity).getContactedGods().contains(e.getInstance())) {
                    player.displayClientMessage(Component.translatable(e.getInstance().getBaseGod().getContactMessageTranslationKey()).withStyle(ChatFormatting.GRAY), false);
                }
                e.getInstance().onContact(living, e);
            }

        }
    }

    @SubscribeEvent
    public static void onLoseFaith(LoseFaithEvent e) {
        Entity entity = e.getEntity();
        if (!entity.level().isClientSide()) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;
                if (living instanceof Player player && !GodHelper.getContactedGodsFrom(entity).getContactedGods().contains(e.getInstance())) {
                    player.displayClientMessage(Component.translatable(e.getInstance().getBaseGod().getLossContactMessageTranslationKey()).withStyle(ChatFormatting.GRAY), false);
                }
                e.getInstance().onLoseFaith(living, e);
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
                if (e.getInstance().onOfferedItems(e)){
                    e.getOfferedItemEntity().kill();
                }
        }
    }
    @SubscribeEvent
    public static void onOfferEntityToGod(OfferEvent.OfferEntityEvent e) {
        Entity offerer = e.getOfferer();
        Entity offer = e.getOffer();
        if (offerer instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)offerer;
            if (offer != null) {
                if (e.getInstance().onOfferedEntity(e)) {
                    if (offer instanceof LivingEntity liv) {
                        if (liv.getHealth() <= liv.getMaxHealth() * 0.3)
                            liv.kill();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void whileNearShrine(WhileNearStatueEvent e) {
        Entity entity = e.getLiving();
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            e.getInstance().whileNearShrine(e);
        }
    }

}
