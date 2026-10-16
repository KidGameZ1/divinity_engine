package net.nightshade.divinity_engine.divinity.blessing.aethon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDomainManager;

import java.awt.Color;

public class PocketDimension extends Blessings {
    public PocketDimension(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        CompoundTag tag = instance.getOrCreateTag();
        // Keep the legacy schema for old saves and addons. Migration reads it exactly once.
        for (String key : new String[]{"pocket_x", "pocket_y", "pocket_z", "return_x", "return_y", "return_z"})
            if (!tag.contains(key)) tag.putInt(key, -999999999);
        if (!tag.contains("return_dimension")) tag.putString("return_dimension", "none");
        return instance;
    }

    @Override public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer player)) return false;
        if (PocketDomainManager.inPocket(player) && player.isShiftKeyDown()) return PocketDomainManager.exit(player);
        if (!PocketDomainManager.takeAction(player)) return false;
        if (!PocketDomainManager.inPocket(player) && player.isShiftKeyDown()) PocketDomainManager.openStorage(player);
        else PocketDomainManager.openControl(player);
        return false; // Opening a menu is not a combat activation or a favor reward.
    }
}
