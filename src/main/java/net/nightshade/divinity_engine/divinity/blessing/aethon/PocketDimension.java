package net.nightshade.divinity_engine.divinity.blessing.aethon;

import io.netty.buffer.Unpooled;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.InputEvent;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.registry.dimensions.DimensionRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.blessings.aethon.PocketDimensionStructureSpawner;
import net.nightshade.divinity_engine.world.inventory.PocketDimensionInvMenu;

import java.awt.*;
import java.security.KeyPair;

public class PocketDimension extends Blessings {

    private static final int UNINITIALIZED = -999999999;

    public PocketDimension(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();
        CompoundTag tag = instance.getOrCreateTag();

        if (!tag.contains("pocket_x")) tag.putInt("pocket_x", UNINITIALIZED);
        if (!tag.contains("pocket_y")) tag.putInt("pocket_y", UNINITIALIZED);
        if (!tag.contains("pocket_z")) tag.putInt("pocket_z", UNINITIALIZED);

        if (!tag.contains("return_x")) tag.putInt("return_x", UNINITIALIZED);
        if (!tag.contains("return_y")) tag.putInt("return_y", UNINITIALIZED);
        if (!tag.contains("return_z")) tag.putInt("return_z", UNINITIALIZED);

        if (!tag.contains("return_dimension")) tag.putString("return_dimension", "none");
        if (!tag.contains("was_in_pocket")) tag.putBoolean("was_in_pocket", false);

        return instance;
    }

    @Override
    public boolean onPressed(BlessingsInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer player)) return false;

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        ServerLevel pocketLevel = server.getLevel(DimensionRegistry.POCKET_DIMENSION);
        if (pocketLevel == null) return false;

        CompoundTag tag = instance.getOrCreateTag();
        ResourceKey<Level> currentDim = player.level().dimension();

        System.out.println("[PocketDimension] onPressed by: " + player.getName().getString() +
                ", Dim: " + currentDim.location() +
                ", was_in_pocket: " + tag.getBoolean("was_in_pocket"));

        boolean inPocketNow = currentDim.equals(DimensionRegistry.POCKET_DIMENSION);
        tag.putBoolean("was_in_pocket", inPocketNow);
        instance.setTag(tag);
        instance.markDirty();

        if (tag.getInt("teleport_cooldown") > 0) {
            System.out.println("[PocketDimension] Blocked onPressed due to cooldown");
            return false;
        }


        // Leaving pocket realm
        if (currentDim.equals(DimensionRegistry.POCKET_DIMENSION) && tag.getBoolean("was_in_pocket")) {
            if (player.isShiftKeyDown()) {
                System.out.println("[PocketDimension] Attempting to return to saved location...");

                String dimId = tag.getString("return_dimension");
                if (!dimId.equals("none")) {
                    ResourceKey<Level> returnKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimId));
                    ServerLevel returnLevel = server.getLevel(returnKey);

                    if (returnLevel != null) {
                        BlockPos returnPos = new BlockPos(
                                tag.getInt("return_x"),
                                tag.getInt("return_y"),
                                tag.getInt("return_z")
                        );
                        System.out.println("[PocketDimension] Returning to: " + returnPos + " in " + dimId);
                        teleportToDim(player, instance, returnLevel, returnPos);
                        player.displayClientMessage(Component.literal("You return from the pocket realm."), true);
                        instance.markDirty();
                        return true;
                    }
                }

                BlockPos fallback = player.getRespawnPosition() != null ? player.getRespawnPosition() : new BlockPos(0, 100, 0);
                ServerLevel fallbackLevel = player.getRespawnPosition() != null ? server.getLevel(player.getRespawnDimension()) : server.overworld();
                System.out.println("[PocketDimension] Fallback teleport to spawn.");
                teleportToDim(player, instance, fallbackLevel, fallback);
                player.displayClientMessage(Component.literal("No return point saved."), true);
                instance.markDirty();
                return false;
            } else {
                if (living instanceof Player _player) {
                    BlockPos _blockPos = BlockPos.containing(living.getX(), living.getY(), living.getZ());
                    _player.openMenu(new SimpleMenuProvider((_containerID, _inventory, _entity) -> {
                        return new PocketDimensionInvMenu(_containerID, _inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_blockPos)) {
                            @Override
                            public boolean stillValid(Player player) {
                                return true;
                            }
                        };
                    }, Component.literal("Pocket Inv")));
                }
                player.displayClientMessage(Component.literal("Hold Shift to return from the pocket realm."), true);
            }
            return false;
        }else {
            if (player.isShiftKeyDown()) {
                if (living instanceof Player _player) {
                    BlockPos _blockPos = BlockPos.containing(living.getX(), living.getY(), living.getZ());
                    _player.openMenu(new SimpleMenuProvider((_containerID, _inventory, _entity) -> {
                        return new PocketDimensionInvMenu(_containerID, _inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_blockPos)) {
                            @Override
                            public boolean stillValid(Player player) {
                                return true;
                            }
                        };
                    }, Component.literal("Pocket Inv")));
                }
                return true;
            }
        }

        // Save current position
        System.out.println("[PocketDimension] Saving return point: " + player.blockPosition() + ", Dim: " + currentDim.location());
        tag.putInt("return_x", player.getBlockX());
        tag.putInt("return_y", player.getBlockY());
        tag.putInt("return_z", player.getBlockZ());
        tag.putString("return_dimension", currentDim.location().toString());

        // Already have pocket structure
        if (tag.getInt("pocket_x") != UNINITIALIZED) {
            BlockPos pos = new BlockPos(
                    tag.getInt("pocket_x"),
                    tag.getInt("pocket_y"),
                    tag.getInt("pocket_z")
            );
            System.out.println("[PocketDimension] Teleporting to existing pocket pos: " + pos);
            teleportToDim(player, instance, pocketLevel, pos);
            tag.putBoolean("was_in_pocket", true);
            instance.markDirty();
            return true;
        }

        // Try to place structure
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = pocketLevel.getRandom().nextIntBetweenInclusive(-512, 512);
            int z = pocketLevel.getRandom().nextIntBetweenInclusive(-512, 512);
            if (x == 0 && z == 0) continue;

            int y = 80;
            BlockPos pos = new BlockPos(x, y, z);

            if (!PocketDimensionStructureSpawner.isAreaOccupied(pocketLevel, pos)) {
                System.out.println("[PocketDimension] Placing new pocket structure at: " + pos);
                PocketDimensionStructureSpawner.placeStructure(pocketLevel, pos);
                tag.putInt("pocket_x", x);
                tag.putInt("pocket_y", y);
                tag.putInt("pocket_z", z);
                teleportToDim(player, instance, pocketLevel, pos);
                tag.putBoolean("was_in_pocket", true);
                instance.markDirty();
                return true;
            }
        }

        System.out.println("[PocketDimension] No space found for pocket.");
        player.displayClientMessage(Component.literal("No suitable space found."), true);
        return false;
    }


    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return false;

        CompoundTag tag = instance.getOrCreateTag();
        boolean inPocket = player.level().dimension().equals(DimensionRegistry.POCKET_DIMENSION);
        if (tag.contains("teleport_cooldown")) {
            int ticks = tag.getInt("teleport_cooldown");
            if (ticks > 0) {
                tag.putInt("teleport_cooldown", ticks - 2);
                System.out.println("[PocketDimension] TP Cooldown: " + ticks);
                instance.setTag(tag);
                instance.markDirty();
            }
        }

        if (!inPocket) {
            tag.putBoolean("was_in_pocket", false);
            tag.putInt("return_x", player.getBlockX());
            tag.putInt("return_y", player.getBlockY());
            tag.putInt("return_z", player.getBlockZ());
            tag.putString("return_dimension", player.level().dimension().location().toString());

            instance.setTag(tag);
            instance.markDirty();


        } else {
            tag.putBoolean("was_in_pocket", true);
            instance.setTag(tag);
            instance.markDirty();


        }

        return false;
    }

    private void teleportToDim(ServerPlayer player, BlessingsInstance instance, ServerLevel level, BlockPos origin) {
        double cx = origin.getX();
        double cy = origin.getY();
        double cz = origin.getZ();
        if (level.dimension() == DimensionRegistry.POCKET_DIMENSION) {
             cx = origin.getX() + 14.5;
             cy = origin.getY() + 1.0;
             cz = origin.getZ() + 15.5;
        }

        if (level != null) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
            player.teleportTo(level, cx, cy, cz, player.getYRot(), player.getXRot());
            player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
            for (MobEffectInstance effect : player.getActiveEffects()) {
                player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect));
            }
            player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
            instance.getOrCreateTag().putInt("teleport_cooldown", 10); // ~1 second lockout
            System.out.println("[PocketDimension] Teleported to: " + origin + " in " + level.dimension().location());
        }
    }
}
