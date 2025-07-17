package net.nightshade.divinity_engine.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Helper class for managing player capabilities related to blessings and their slots.
 * Provides utility methods to access and modify blessing-related data for entities.
 */
public class DivinityEngineHelper {
    /**
     * Retrieves the player variables capability for the given entity.
     *
     * @param entity The entity to get variables for
     * @return PlayerVariables capability instance, creates new if none exists
     */
    public static MainPlayerCapability.PlayerVariables getMainPlayerVariables(Entity entity){
        return (MainPlayerCapability.PlayerVariables) entity.getCapability(MainPlayerCapability.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MainPlayerCapability.PlayerVariables());
    }

    /**
     * Gets the blessing in slot 1 for the given entity.
     *
     * @param entity The entity to check
     * @return The blessing instance in slot 1, or null if empty
     */
    public static @Nullable BlessingsInstance getBlessingSlot1(Entity entity) {
        return getMainPlayerVariables(entity).getBlessingSlot1();
    }

    /**
     * Sets the blessing in slot 1 for the given entity.
     *
     * @param entity        The entity to modify
     * @param blessingSlot1 The blessing instance to set, or null to clear
     */
    public static void setBlessingSlot1(Entity entity, @Nullable BlessingsInstance blessingSlot1) {
        getMainPlayerVariables(entity).setBlessingSlot1(blessingSlot1);
    }

    /**
     * Gets the blessing in slot 2 for the given entity.
     *
     * @param entity The entity to check
     * @return The blessing instance in slot 2, or null if empty
     */
    public static @Nullable BlessingsInstance getBlessingSlot2(Entity entity) {
        return getMainPlayerVariables(entity).getBlessingSlot2();
    }

    /**
     * Sets the blessing in slot 2 for the given entity.
     *
     * @param entity        The entity to modify
     * @param blessingSlot2 The blessing instance to set, or null to clear
     */
    public static void setBlessingSlot2(Entity entity, @Nullable BlessingsInstance blessingSlot2) {
        getMainPlayerVariables(entity).setBlessingSlot2(blessingSlot2);
    }

    /**
     * Gets the blessing in slot 3 for the given entity.
     *
     * @param entity The entity to check
     * @return The blessing instance in slot 3, or null if empty
     */
    public static @Nullable BlessingsInstance getBlessingSlot3(Entity entity) {
        return getMainPlayerVariables(entity).getBlessingSlot3();
    }

    /**
     * Sets the blessing in slot 3 for the given entity.
     *
     * @param entity        The entity to modify
     * @param blessingSlot3 The blessing instance to set, or null to clear
     */
    public static void setBlessingSlot3(Entity entity, @Nullable BlessingsInstance blessingSlot3) {
        getMainPlayerVariables(entity).setBlessingSlot3(blessingSlot3);
        getMainPlayerVariables(entity).syncPlayerVariables(entity);
    }

    /**
     * Checks if the entity has the specified blessing in any slot.
     *
     * @param blessings The blessing to check for
     * @param entity    The entity to check
     * @return true if the blessing is found in any slot, false otherwise
     */
    public static boolean hasBlessingInSlot(Blessings blessings, Entity entity) {
        if (getBlessingSlot1(entity) != null && getBlessingSlot1(entity).getBlessing() == blessings) return true;
        if (getBlessingSlot2(entity) != null && getBlessingSlot2(entity).getBlessing() == blessings) return true;
        if (getBlessingSlot3(entity) != null && getBlessingSlot3(entity).getBlessing() == blessings) return true;
        return false;
    }

    /**
     * Gets the slot number (0-2) containing the specified blessing.
     *
     * @param entity    The entity to check
     * @param blessings The blessing to find
     * @return The slot number (0-2) or -1 if not found
     */
    public static int getBlessingsSlot(Entity entity, Blessings blessings) {
        if (getBlessingSlot1(entity) != null && getBlessingSlot1(entity).getBlessing() == blessings) return 0;
        if (getBlessingSlot2(entity) != null && getBlessingSlot2(entity).getBlessing() == blessings) return 1;
        if (getBlessingSlot3(entity) != null && getBlessingSlot3(entity).getBlessing() == blessings) return 2;

        return -1;
    }

    /**
     * Gets the current blessings page number for the entity.
     *
     * @param entity The entity to check
     * @return The current page number
     */
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

    /**
     * Sets a blessing in the specified slot for a player.
     *
     * @param player   The player to modify
     * @param slot     The slot number (0-2, or -1 to skip)
     * @param blessing The blessing instance to set
     */
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
            case -1:
                break;
        }
    }

    public static BlessingsInstance getBlessingInSlot(Player player, int slot) {
        switch(slot) {
            case 0:
                return getBlessingSlot1(player);
            case 1:
                return getBlessingSlot2(player);
            case 2:
                return getBlessingSlot3(player);
            case -1:
                return null;
        }
        return null;
    }

    public record PlayerData(UUID uuid, BlockPos pos, boolean isSneaking) {
        /**
         * Calculate squared distance to another BlockPos. Safe for async.
         */
        public double distSqr(BlockPos other) {
            double dx = pos.getX() + 0.5 - other.getX() - 0.5;
            double dy = pos.getY() + 0.5 - other.getY() - 0.5;
            double dz = pos.getZ() + 0.5 - other.getZ() - 0.5;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    public static boolean isCtrlPressed() {
        Minecraft mc = Minecraft.getInstance();
        // Check if left or right control key is down
        boolean leftCtrl = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL);
        boolean rightCtrl = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
        return leftCtrl || rightCtrl;
    }

    public static boolean isAltPressed() {
        Minecraft mc = Minecraft.getInstance();
        // Check if left or right control key is down
        boolean leftCtrl = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT);
        boolean rightCtrl = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
        return leftCtrl || rightCtrl;
    }

    public static boolean isShiftPressed() {
        Minecraft mc = Minecraft.getInstance();
        // Check if left or right control key is down
        boolean leftCtrl = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
        boolean rightCtrl = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        return leftCtrl || rightCtrl;
    }

    public static boolean isKeyPressed(int key) {
        Minecraft mc = Minecraft.getInstance();
        boolean KEY = InputConstants.isKeyDown(mc.getWindow().getWindow(), key);
        return KEY;
    }

}
