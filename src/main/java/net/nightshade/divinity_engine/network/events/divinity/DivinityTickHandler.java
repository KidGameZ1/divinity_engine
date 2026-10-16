package net.nightshade.divinity_engine.network.events.divinity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.divinity.gods.FavorTiers;
import net.nightshade.divinity_engine.network.cap.player.gods.GodsStorage;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.List;

@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DivinityTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.isAlive()) return;
        processPlayerTicks(player);
    }

    private static void processPlayerTicks(Player player) {
        int tickCount = player.tickCount;

        GodsStorage godData = GodHelper.getContactedGodsFrom(player);
        // ===== Curse Tick (every 10 ticks) =====
        if (tickCount % 10 == 0) {
            for (CurseInstance curse : godData.getCurses()) {
                curse.onTick(player);
            }
        }

        // ===== God Tick (every 20 ticks ~1 second) =====
        if (tickCount % 20 == 0) {
            List<BaseGodInstance> contactedGods = godData.getContactedGods().stream().toList();
            if (!contactedGods.isEmpty()) {
                // Clamp favor
                for (BaseGodInstance instance : contactedGods) {
                    int favor = instance.getFavor();
                    if (favor > FavorTiers.Champion.getMaxFavor()) {
                        instance.setFavor(FavorTiers.Champion.getMaxFavor());
                        godData.updateGodInstance(instance);
                    } else if (favor < FavorTiers.Enemy.getMinFavor()) {
                        instance.setFavor(FavorTiers.Enemy.getMinFavor());
                        godData.updateGodInstance(instance);
                    }
                }

                // Curse/Uncurse logic
                for (BaseGodInstance godInstance : contactedGods) {
                    FavorTiers tier = FavorTiers.getByFavor(godInstance.getFavor());
                    BaseGod baseGod = godInstance.getBaseGod();
                    if (!net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.inRealm(player)) godInstance.onTick(player);

                    if (tier == FavorTiers.Enemy && baseGod.getCurse() != null) {
                        boolean alreadyCursed = godData.getCurse(baseGod.getCurse()).isPresent();
                        if (!alreadyCursed) {
                            GodHelper.curse(baseGod.getCurse(), baseGod, player);
                        }
                    } else if (!godData.getCurses().isEmpty()) {
                        for (CurseInstance curse : godData.getCurses()) {
                            BaseGod boundGod = curse.getBoundGod();
                            BaseGodInstance boundInstance = GodHelper.getGodOrNull(player, boundGod);

                            boolean invalid = boundInstance == null
                                    || FavorTiers.getByFavor(boundInstance.getFavor()) != FavorTiers.Enemy;

                            if (invalid) {
                                godData.uncurse(curse);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // ===== Blessing Tick (every 2 ticks) =====
        if (tickCount % 2 == 0) {
            BlessingsInstance[] slots = {
                    DivinityEngineHelper.getBlessingSlot1(player),
                    DivinityEngineHelper.getBlessingSlot2(player),
                    DivinityEngineHelper.getBlessingSlot3(player)
            };

            boolean updatedBlessing = false;
            for (BlessingsInstance slot : slots) {
                if (slot != null) {
                    GodHelper.validateBlessingSlot(player, slot);
                    if (!DivinityEngineHelper.hasBlessingInSlot(slot.getBlessing(), player)) continue;

                    if (!net.nightshade.divinity_engine.util.divinity.innerworld.CommunionManager.busy(player)
                            && (slot.getBlessing().isPassive() || slot.getBlessing().canToggle())) {
                        slot.onTick(player);
                    } if (slot.getCooldown() != 0){
                        slot.decreaseCooldown(2);
                    }

                    GodHelper.updateBlessingInstance(player, slot, false);
                    updatedBlessing = true;
                }
            }
            // Preserve the two-tick cadence, but serialize once for the whole loadout.
            if (updatedBlessing) godData.syncChanges();
        }

    }

}
