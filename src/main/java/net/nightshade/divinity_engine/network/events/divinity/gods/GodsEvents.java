
package net.nightshade.divinity_engine.network.events.divinity.gods;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.network.cap.player.gods.GodsStorage;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.WhileNearStatueEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.List;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@EventBusSubscriber(
    modid = DivinityEngineMod.MODID,
    bus = Bus.FORGE
)
public class GodsEvents {

    /*private static final WeakHashMap<Player, Integer> playerTickCounters = new WeakHashMap<>();

    @SubscribeEvent
    public static void onGodTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        int tickCount = playerTickCounters.getOrDefault(player, 0);

        if (tickCount % 20 == 0) { // run every 20 ticks

            GodsStorage godData = GodHelper.getContactedGodsFrom(player);
            var contactedGods = godData.getContactedGods();

            if (contactedGods.isEmpty()) {
                playerTickCounters.put(player, tickCount + 1);
                return;
            }

            // Clamp favor values once per god instance
            for (BaseGodInstance instance : contactedGods) {
                int favor = instance.getFavor();
                boolean changed = false;
                if (favor > FavorTiers.Champion.getMaxFavor()) {
                    instance.setFavor(FavorTiers.Champion.getMaxFavor());
                    changed = true;
                } else if (favor < FavorTiers.Enemy.getMinFavor()) {
                    instance.setFavor(FavorTiers.Enemy.getMinFavor());
                    changed = true;
                }
                if (changed) {
                    godData.updateGodInstance(instance);
                }
            }

            // Curse and uncurse logic
            for (BaseGodInstance godInstance : contactedGods) {
                FavorTiers tier = FavorTiers.getByFavor(godInstance.getFavor());
                BaseGod baseGod = godInstance.getBaseGod();

                if (tier == FavorTiers.Enemy && baseGod.getCurse() != null) {
                    if (godData.getCurse(baseGod.getCurse()).isEmpty()) {
                        GodHelper.curse(baseGod.getCurse(), baseGod, player);
                    }
                } else {
                    var curses = godData.getCurses();
                    if (!curses.isEmpty()) {
                        for (CurseInstance curse : curses) {
                            BaseGod boundGod = curse.getBoundGod();
                            BaseGodInstance boundInstance = GodHelper.getGodOrNull(player, boundGod);

                            boolean shouldUncurse = boundInstance == null
                                    || FavorTiers.getByFavor(boundInstance.getFavor()) != FavorTiers.Enemy;

                            if (shouldUncurse) {
                                godData.uncurse(curse);
                                break; // uncurse once per tick cycle is enough
                            }
                        }
                    }
                }
            }
        }

        playerTickCounters.put(player, tickCount + 1);
    }*/

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
