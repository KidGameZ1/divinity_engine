package net.nightshade.divinity_engine.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import org.jetbrains.annotations.Nullable;

public class MainPlayerCapabilityHelper {
    public static MainPlayerCapability.PlayerVariables getMainPlayerVariables(Entity entity){
        return (MainPlayerCapability.PlayerVariables) entity.getCapability(MainPlayerCapability.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MainPlayerCapability.PlayerVariables());
    }

    public static @Nullable BlessingsInstance getBlessingSlot1(Entity entity) {
        return getMainPlayerVariables(entity).getBlessingSlot1();
    }
    public static void setBlessingSlot1(Entity entity, @Nullable BlessingsInstance blessingSlot1) {
        getMainPlayerVariables(entity).setBlessingSlot1(blessingSlot1);
    }
    public static @Nullable BlessingsInstance getBlessingSlot2(Entity entity) {
        return getMainPlayerVariables(entity).getBlessingSlot2();
    }
    public static void setBlessingSlot2(Entity entity, @Nullable BlessingsInstance blessingSlot2) {
        getMainPlayerVariables(entity).setBlessingSlot2(blessingSlot2);
    }
    public static @Nullable BlessingsInstance getBlessingSlot3(Entity entity) {
        return getMainPlayerVariables(entity).getBlessingSlot3();
    }
    public static void setBlessingSlot3(Entity entity, @Nullable BlessingsInstance blessingSlot3) {
        getMainPlayerVariables(entity).setBlessingSlot3(blessingSlot3);
    }

    public static int getBlessingsPageNum(Entity entity) {
        return getMainPlayerVariables(entity).getBlessingsPageNum();
    }
    public static void setBlessingsPageNum(Entity entity, int num) {
        getMainPlayerVariables(entity).setBlessingsPageNum(num);
    }
    public static void increaseBlessingsPageNum(Entity entity, int num) {
        getMainPlayerVariables(entity).increaseBlessingsPageNum(num);
    }
    public static void decreaseBlessingsPageNum(Entity entity, int num) {
        getMainPlayerVariables(entity).decreaseBlessingsPageNum(num);
    }

    public static void setBlessingSlot(Player player, int slot, BlessingsInstance blessing) {
        switch(slot) {
            case 0:
                setBlessingSlot1(player, blessing);
                break;
            case 1:
                setBlessingSlot2(player, blessing);
                break;
            case 2:
                setBlessingSlot3(player, blessing);
                break;
        }
    }


}
