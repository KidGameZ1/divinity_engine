package net.nightshade.divinity_engine.util.divinity.gods;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.GodsStorage;
import net.nightshade.divinity_engine.network.cap.player.gods.IMainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.events.divinity.curse.CursePlayerEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.ContactGodEvent;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for managing god-related functionality and interactions between players/entities and gods.
 * Provides methods for managing blessings, favor, god contact and other divine interactions.
 */
public class GodHelper {

    /**
     * Gets the GodsStorage instance for an entity
     *
     * @param entity The entity to get storage for
     * @return The GodsStorage for the entity
     */
    public static @NotNull GodsStorage getContactedGodsFrom(Entity entity) {
        return PlayerGodsCapability.load(entity);
    }

    /**
     * Gets a god instance for an entity, or null if not found
     *
     * @param entity The entity to get god instance for
     * @param god    The god to get instance of
     * @return The god instance if found, null otherwise
     */
    @Nullable
    public static BaseGodInstance getGodOrNull(@Nullable Entity entity, BaseGod god) {
        if (entity == null) {
            return null;
        } else {
            Optional<BaseGodInstance> instance = getContactedGodsFrom(entity).getContactedGod(god);
            return (BaseGodInstance) instance.orElse((BaseGodInstance) null);
        }
    }

    @Nullable
    public static CurseInstance getCurseOrNull(@Nullable Entity entity, Curse curse) {
        if (entity == null) {
            return null;
        } else {
            Optional<CurseInstance> instance = getContactedGodsFrom(entity).getCurse(curse);
            return (CurseInstance) instance.orElse((CurseInstance) null);
        }
    }

    /**
     * Gets all blessings instances for a god and entity
     *
     * @param entity The entity to get blessings for
     * @param god    The god to get blessings from
     * @return Set of blessing instances
     */
    public static Set<BlessingsInstance> getBlessingsInstances(Entity entity, BaseGod god) {
        return getGodOrNull(entity, god).getBlessingsInstances();
    }

    /**
     * Validates and potentially clears a blessing slot based on god contact and favor level.
     * Will remove blessing if player has lost contact with god or insufficient favor.
     *
     * @param player       The player to validate slot for
     * @param slotInstance The blessing slot instance to validate
     */
    public static void validateBlessingSlot(Player player, BlessingsInstance slotInstance) {
        if (player == null || slotInstance == null || slotInstance.getBoundGodInstance() == null) {
            return;
        }
        IMainPlayerCapability capability = DivinityEngineHelper.getMainPlayerVariables(player);

        BaseGod god = slotInstance.getBoundGod();
        if (!hasContactedGod(player, god)) {
            // Clear the slot if player doesn't have contact with the god
            if (slotInstance.equals(capability.getBlessingSlot1())) {
                capability.setBlessingSlot1(null);
            } else if (slotInstance.equals(capability.getBlessingSlot2())) {
                capability.setBlessingSlot2(null);
            } else if (slotInstance.equals(capability.getBlessingSlot3())) {
                capability.setBlessingSlot3(null);
            }
        } else {
            if (getGodOrNull(player, god).getFavor() < slotInstance.getNeededFavor()) {
                if (slotInstance.equals(capability.getBlessingSlot1())) {
                    capability.setBlessingSlot1(null);
                } else if (slotInstance.equals(capability.getBlessingSlot2())) {
                    capability.setBlessingSlot2(null);
                } else if (slotInstance.equals(capability.getBlessingSlot3())) {
                    capability.setBlessingSlot3(null);
                }
            }
        }
    }

    /**
     * Updates a blessing instance for a player by finding and replacing the matching blessing
     *
     * @param player       The player to update blessing for
     * @param slotInstance The new blessing instance
     */
    public static void updateBlessingInstance(Player player, BlessingsInstance slotInstance) {
        if (player == null || slotInstance == null || slotInstance.getBoundGodInstance() == null) {
            return;
        }

        BaseGod god = slotInstance.getBoundGod();
        BaseGodInstance godInstance = getGodOrNull(player, god);

        if (godInstance != null) {
            getContactedGodsFrom(player).updateBlessingInstance(godInstance, slotInstance, true);
        }
    }

    /**
     * Gets all blessing instances for an entity across all contacted gods
     *
     * @param entity The entity to get blessings for
     * @return List of all blessing instances sorted by god registry name and favor cost
     */
    public static List<BlessingsInstance> getAllBlessingsInstances(Entity entity) {
        if (entity == null) {
            return new ArrayList<>();
        }

        GodsStorage storage = getContactedGodsFrom(entity);
        if (storage == null) {
            return new ArrayList<>();
        }

        return storage.getContactedGods().stream()
                .filter(Objects::nonNull)
                .flatMap(god -> god.getBlessingsInstances().stream())
                .filter(Objects::nonNull)
                .filter(b -> b.getBoundGod() != null && b.getBoundGod().getRegistryName() != null)
                .sorted(Comparator.comparing((BlessingsInstance b) -> b.getBoundGod().getRegistryName().toString())
                        .thenComparing(BlessingsInstance::getNeededFavor))
                .collect(Collectors.toList());
    }

    public static List<BlessingsInstance> getAllBlessingsInstancesOfGod(Entity entity, BaseGod targetGod) {
        if (entity == null || targetGod == null) {
            return new ArrayList<>();
        }

        GodsStorage storage = getContactedGodsFrom(entity);
        if (storage == null) {
            return new ArrayList<>();
        }

        // Find the ContactedGod object for the specific god
        Optional<BaseGodInstance> matchedGod = storage.getContactedGods().stream()
                .filter(cg -> cg.getBaseGod() == targetGod)
                .findFirst();

        if (matchedGod.isEmpty()) {
            return new ArrayList<>();
        }

        // Return only blessings bound to the specified god
        return matchedGod.get().getBlessingsInstances().stream()
                .filter(Objects::nonNull)
                .filter(b -> b.getBoundGod() == targetGod)
                .sorted(Comparator.comparing(BlessingsInstance::getNeededFavor))
                .collect(Collectors.toList());
    }

    /**
     * Gets a specific blessing instance for an entity and god
     *
     * @param entity    The entity to get blessing for
     * @param god       The god to get blessing from
     * @param blessings The blessing type to get
     * @return The blessing instance if found, null otherwise
     */
    public static BlessingsInstance getBlessingInstance(Entity entity, BaseGod god, Blessings blessings) {
        BaseGodInstance godInstance = getGodOrNull(entity, god);
        if (godInstance == null) {
            return null;
        }
        for (int i = 0; i < godInstance.getBlessingsInstances().size(); i++) {
            BlessingsInstance instance = godInstance.getBlessingsInstances().stream().toList().get(i);
            if (instance.equals(blessings)) {
                return instance;
            }
        }
        return null;
    }

    /**
     * Adds a blessing to an entity's god
     *
     * @param entity   The entity to add blessing for
     * @param god      The god to add blessing to
     * @param instance The blessing instance to add
     */
    public static void addBlessing(Entity entity, BaseGod god, BlessingsInstance instance) {
        getGodOrNull(entity, god).addBlessing(instance);
    }

    /**
     * Increases favor with a god for an entity
     *
     * @param god    The god instance to increase favor with
     * @param entity The entity gaining favor
     * @param favor  Amount of favor to increase by
     */
    public static void increaseFavor(BaseGodInstance god, LivingEntity entity, int favor) {
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.literal("You've gain " + favor + " Favor with " + Component.translatable(god.getBaseGod().getNameTranslationKey()).getString()), true);
        }
        getGodOrNull(entity, god.getBaseGod()).increaseFavor(favor);
    }
    /**
     * Increases favor with a god for an entity
     *
     * @param god    The god to increase favor with
     * @param entity The entity gaining favor
     * @param favor  Amount of favor to increase by
     */
    public static void increaseFavor(BaseGod god, LivingEntity entity, int favor) {
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.literal("You've gain " + favor + " Favor with " + Component.translatable(god.getNameTranslationKey()).getString()), true);
        }
        getGodOrNull(entity, god).increaseFavor(favor);
    }

    /**
     * Decreases favor with a god for an entity
     *
     * @param god    The god instance to decrease favor with
     * @param entity The entity losing favor
     * @param favor  Amount of favor to decrease by
     */
    public static void decreaseFavor(BaseGodInstance god, LivingEntity entity, int favor) {
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.literal("You've lossed " + favor + " Favor with " + Component.translatable(god.getBaseGod().getNameTranslationKey()).getString()), true);
        }
        getGodOrNull(entity, god.getBaseGod()).decreaseFavor(favor);
    }
    /**
     * Decreases favor with a god for an entity
     *
     * @param god    The god to decrease favor with
     * @param entity The entity losing favor
     * @param favor  Amount of favor to decrease by
     */
    public static void decreaseFavor(BaseGod god, LivingEntity entity, int favor) {
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.literal("You've loss " + favor + " Favor with " + Component.translatable(god.getNameTranslationKey()).getString()), true);
        }
        getGodOrNull(entity, god).decreaseFavor(favor);
    }

    /**
     * Gets a blessing for an entity from a god
     *
     * @param entity    The entity to get blessing for
     * @param god       The god to get blessing from
     * @param blessings The blessing type to get
     * @return The blessing instance
     */
    public static BlessingsInstance getBlessing(Entity entity, BaseGod god, Blessings blessings) {
        return getGodOrNull(entity, god).getBlessing(blessings);
    }

    /**
     * Removes a blessing from an entity's god
     *
     * @param entity    The entity to remove blessing from
     * @param god       The god to remove blessing from
     * @param blessings The blessing type to remove
     */
    public static void removeBlessing(Entity entity, BaseGod god, Blessings blessings) {
        getGodOrNull(entity, god).removeBlessing(blessings);
    }

    /**
     * Removes a specific blessing instance from an entity's god
     *
     * @param entity   The entity to remove blessing from
     * @param god      The god to remove blessing from
     * @param instance The specific blessing instance to remove
     */
    public static void removeBlessing(Entity entity, BaseGod god, BlessingsInstance instance) {
        getGodOrNull(entity, god).removeBlessing(instance);
    }

    /**
     * Checks if an entity has contacted a specific god
     *
     * @param entity The entity to check
     * @param god    The god to check contact with
     * @return True if contacted, false otherwise
     */
    public static boolean hasContactedGod(Entity entity, BaseGod god) {
        BaseGodInstance instance = getGodOrNull(entity, god);
        if (instance == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Initiates contact between an entity and a god
     *
     * @param god    The god to contact
     * @param entity The entity initiating contact
     * @return True if contact successful, false otherwise
     */
    public static boolean contactGod(BaseGod god, LivingEntity entity) {
        GodsStorage storage = getContactedGodsFrom(entity);
        Optional<BaseGodInstance> optional = storage.getContactedGod(god);
        if (optional.isEmpty()) {
            BaseGodInstance instance = new BaseGodInstance(god);
            return storage.contactGod(instance);
        } else {
            BaseGodInstance instance = (BaseGodInstance) optional.get();
            if (MinecraftForge.EVENT_BUS.post(new ContactGodEvent(instance, entity))) {
                return false;
            } else {
                instance.setFavor(0);
                storage.syncChanges();
                return true;
            }
        }
    }

    public static boolean curse(Curse curse, BaseGod boundGod, LivingEntity entity) {
        GodsStorage storage = getContactedGodsFrom(entity);
        Optional<CurseInstance> optional = storage.getCurse(curse);
        if (optional.isEmpty()) {
            CurseInstance instance = new CurseInstance(curse);
            return storage.curse(instance, boundGod);
        } else {
            CurseInstance instance = (CurseInstance) optional.get();
            if (MinecraftForge.EVENT_BUS.post(new CursePlayerEvent(instance, entity))) {
                return false;
            } else {
                storage.syncChanges();
                return true;
            }
        }
    }

    /**
     * Initiates contact with all available gods for an entity
     *
     * @param entity The entity to contact all gods with
     */
    public static void contactAllGods(LivingEntity entity) {
        for (int i = 0; i < GodsRegistry.GODS_REGISTRY.get().getEntries().size(); i++) {
            BaseGod god = GodsRegistry.GODS_REGISTRY.get().getEntries().stream().toList().get(i).getValue();
            BaseGodInstance instance = god.createGodDefaultInstance();
            contactGod(instance, entity);
        }
    }

    public static List<BaseGodInstance> getAllContactedGods(LivingEntity entity) {
        List<BaseGodInstance> gods = new ArrayList<>();
        for (int i = 0; i < GodsRegistry.GODS_REGISTRY.get().getEntries().size(); i++) {
            BaseGod god = GodsRegistry.GODS_REGISTRY.get().getEntries().stream().toList().get(i).getValue();
            gods.add(getGodOrNull(entity, god));
        }
        return gods;
    }

    /**
     * Makes an entity lose faith in all gods
     *
     * @param entity The entity losing faith
     */
    public static void loseFaithAllGods(LivingEntity entity) {
        for (BaseGodInstance instance : getAllContactedGods(entity)) {
            getContactedGodsFrom(entity).loseContactedGod(instance);
        }
    }

    /**
     * Initiates contact with a specific god instance for an entity
     *
     * @param godInstance The specific god instance to contact
     * @param entity      The entity initiating contact
     * @return True if contact successful, false otherwise
     */
    public static boolean contactGod(BaseGodInstance godInstance, LivingEntity entity) {
        GodsStorage storage = getContactedGodsFrom(entity);
        Optional<BaseGodInstance> optional = storage.getContactedGod(godInstance.getBaseGod());
        if (optional.isEmpty()) {
            return storage.contactGod(godInstance);
        } else {
            BaseGodInstance instance = (BaseGodInstance) optional.get();
            if (MinecraftForge.EVENT_BUS.post(new ContactGodEvent(instance, entity))) {
                return false;
            } else {
                instance.setFavor(0);
                storage.syncChanges();
                return true;
            }
        }
    }

    public static boolean curse(CurseInstance curseInstance, BaseGod boundGod, LivingEntity entity) {
        GodsStorage storage = getContactedGodsFrom(entity);
        Optional<CurseInstance> optional = storage.getCurse(curseInstance.getCurse());
        if (optional.isEmpty()) {
            return storage.curse(curseInstance, boundGod);
        } else {
            CurseInstance instance = (CurseInstance) optional.get();
            if (MinecraftForge.EVENT_BUS.post(new CursePlayerEvent(instance, entity))) {
                return false;
            } else {
                storage.syncChanges();
                return true;
            }
        }
    }

    /**
     * Formats a god's domains into a readable string
     *
     * @param god The god to format domains for
     * @return Formatted string of domains
     */
    public static String getFormattedDomains(BaseGod god) {
        return god.getDomains().stream()
                .map(domain -> domain.substring(0, 1).toUpperCase() + domain.substring(1))
                .collect(Collectors.joining(", "));
    }

}