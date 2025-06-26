package net.nightshade.divinity_engine.util.divinity.gods;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.GodsStorage;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.events.divinity.gods.ContactGodEvent;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GodHelper {

    public static @NotNull GodsStorage getContactedGodsFrom(Entity entity) {
        return PlayerGodsCapability.load(entity);
    }

    @Nullable
    public static BaseGodInstance getGodOrNull(@Nullable Entity entity, BaseGod god) {
        if (entity == null) {
            return null;
        } else {
            Optional<BaseGodInstance> instance = getContactedGodsFrom(entity).getContactedGod(god);
            return (BaseGodInstance)instance.orElse((BaseGodInstance) null);
        }
    }

    public static Set<BlessingsInstance> getBlessingsInstances(Entity entity, BaseGod god) {
        return getGodOrNull(entity, god).getBlessingsInstances();
    }

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
                .collect(Collectors.toList());
    }




    public static BlessingsInstance getBlessingInstance(Entity entity, BaseGod god, Blessings blessings) {
        BaseGodInstance godInstance = getGodOrNull(entity, god);
        if (godInstance == null) {
            return null;
        }
        for (int i = 0; i<godInstance.getBlessingsInstances().size(); i++) {
            BlessingsInstance instance = godInstance.getBlessingsInstances().stream().toList().get(i);
            if (instance.equals(blessings)) {
                return instance;
            }
        }
        return null;
    }

    public static void addBlessing(Entity entity, BaseGod god, BlessingsInstance instance) {
        getGodOrNull(entity,god).addBlessing(instance);
    }

    public static BlessingsInstance getBlessing(Entity entity, BaseGod god, Blessings blessings) {
        return getGodOrNull(entity,god).getBlessing(blessings);
    }

    public static void removeBlessing(Entity entity, BaseGod god, Blessings blessings) {
        getGodOrNull(entity,god).removeBlessing(blessings);
    }

    public static void removeBlessing(Entity entity, BaseGod god, BlessingsInstance instance) {
        getGodOrNull(entity,god).removeBlessing(instance);
    }

    public static boolean hasContactedGod(Entity entity, BaseGod god) {
        BaseGodInstance instance = getGodOrNull(entity, god);
        if (instance == null) {
            return false;
        } else {
            return true;
        }
    }
    

    public static boolean contactGod(BaseGod god, LivingEntity entity) {
        GodsStorage storage = getContactedGodsFrom(entity);
        Optional<BaseGodInstance> optional = storage.getContactedGod(god);
        if (optional.isEmpty()) {
            BaseGodInstance instance = new BaseGodInstance(god);
            return storage.contactGod(instance);
        } else {
            BaseGodInstance instance = (BaseGodInstance)optional.get();
             if (MinecraftForge.EVENT_BUS.post(new ContactGodEvent(instance, entity))) {
                return false;
            } else {
                 instance.setFavor(0);
                storage.syncChanges();
                return true;
            }
        }
    }

    public static void contactAllGods(LivingEntity entity) {
        for(int i = 0; i < GodsRegistry.GODS_REGISTRY.get().getEntries().size(); i++) {
            BaseGod god = GodsRegistry.GODS_REGISTRY.get().getEntries().stream().toList().get(i).getValue();
            BaseGodInstance instance = god.createGodDefaultInstance();
            contactGod(instance, entity);
        }
    }

    public static boolean contactGod(BaseGodInstance godInstance, LivingEntity entity) {
        GodsStorage storage = getContactedGodsFrom(entity);
        Optional<BaseGodInstance> optional = storage.getContactedGod(godInstance.getBaseGod());
        if (optional.isEmpty()) {
            return storage.contactGod(godInstance);
        } else {
            BaseGodInstance instance = (BaseGodInstance)optional.get();
           if (MinecraftForge.EVENT_BUS.post(new ContactGodEvent(instance, entity))) {
                return false;
           } else {
               instance.setFavor(0);
               storage.syncChanges();
               return true;
           }
        }
    }
    
}
