package net.nightshade.divinity_engine.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import net.nightshade.divinity_engine.network.cap.player.gods.PlayerGodsCapability;
import net.nightshade.divinity_engine.network.messages.gui.EquipBlessingMessage;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;

import java.util.UUID;

@GameTestHolder("divinity_engine")
@PrefixGameTestTemplate(false)
public final class BlessingsUiGameTests {
    @GameTest(template = "pocket_dimension", timeoutTicks = 40)
    public static void stableIdsAndIdempotentEquip(GameTestHelper h) {
        TestPlayer p = player(h);
        try {
            var first = GodHelper.getAllBlessingsInstances(p).get(0);
            var request = new EquipBlessingMessage(17, first.getBoundGod().getRegistryName(), first.getBlessing().getRegistryName(), 0, true);
            DivinityEngineHelper.setBlessingsPageNum(p, 999);
            h.assertTrue(request.apply(p), "Stable-ID request must not depend on the old page number");
            h.assertFalse(request.apply(p), "Repeated equip must not toggle the slot off");
            h.assertTrue(DivinityEngineHelper.getBlessingInSlot(p, 0) == first, "Wrong blessing was equipped");
            h.assertFalse(new EquipBlessingMessage(17, request.godId(), request.blessingId(), 1, true).apply(p), "Duplicate slot binding accepted");
            var remove = new EquipBlessingMessage(17, request.godId(), request.blessingId(), 0, false);
            h.assertTrue(remove.apply(p), "Unequip failed");
            h.assertFalse(remove.apply(p), "Duplicate unequip changed state");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 40)
    public static void invalidOrStaleRequestsAreRejected(GameTestHelper h) {
        TestPlayer p = player(h);
        try {
            var entries = GodHelper.getAllBlessingsInstances(p);
            var first = entries.get(0);
            var second = entries.get(1);
            var request = new EquipBlessingMessage(17, first.getBoundGod().getRegistryName(), first.getBlessing().getRegistryName(), 0, true);
            h.assertFalse(request.apply(null), "Null sender accepted");
            h.assertFalse(new EquipBlessingMessage(18, request.godId(), request.blessingId(), 0, true).apply(p), "Stale menu accepted");
            h.assertFalse(new EquipBlessingMessage(17, request.godId(), request.blessingId(), 3, true).apply(p), "Invalid slot accepted");
            h.assertFalse(new EquipBlessingMessage(17, new ResourceLocation("missing", "god"), request.blessingId(), 0, true).apply(p), "Uncontacted god accepted");
            first.getBoundGodInstance().setFavor(first.getNeededFavor() - 1);
            h.assertFalse(request.apply(p), "Insufficient favor accepted");
            first.getBoundGodInstance().setFavor(250);
            h.assertTrue(request.apply(p), "Valid equip failed");
            h.assertTrue(new EquipBlessingMessage(17, second.getBoundGod().getRegistryName(), second.getBlessing().getRegistryName(), 0, true).apply(p), "Replacement failed");
            h.assertFalse(new EquipBlessingMessage(17, request.godId(), request.blessingId(), 0, false).apply(p), "Stale unequip removed a replacement");
            p.containerMenu = p.inventoryMenu;
            h.assertFalse(request.apply(p), "Equip outside the blessings menu accepted");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(template = "pocket_dimension", timeoutTicks = 40)
    public static void packetRoundTripAndPlayerIsolation(GameTestHelper h) {
        TestPlayer p = player(h), other = player(h);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var first = GodHelper.getAllBlessingsInstances(p).get(0);
            var request = new EquipBlessingMessage(17, first.getBoundGod().getRegistryName(), first.getBlessing().getRegistryName(), 2, true);
            EquipBlessingMessage.encode(request, buffer);
            var decoded = new EquipBlessingMessage(buffer);
            h.assertTrue(decoded.equals(request), "Packet lost its stable selection");
            h.assertTrue(decoded.apply(p), "Decoded request failed");
            h.assertTrue(DivinityEngineHelper.getBlessingInSlot(other, 2) == null, "Another player's loadout changed");
            h.succeed();
        } finally { buffer.release(); p.discard(); other.discard(); }
    }

    private static TestPlayer player(GameTestHelper h) {
        TestPlayer player = new TestPlayer(h.getLevel());
        var god = GodsRegistry.SOLARIUS.get().createGodDefaultInstance();
        god.setFavor(250);
        PlayerGodsCapability.load(player).updateGodInstance(god, false);
        player.containerMenu = new BlessingsMenu(17, player.getInventory(), null);
        return player;
    }

    private static final class TestPlayer extends FakePlayer {
        private final MainPlayerCapability.PlayerVariables variables = new MainPlayerCapability.PlayerVariables();
        private final LazyOptional<MainPlayerCapability.PlayerVariables> cap = LazyOptional.of(() -> variables);

        private TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "BlessingsUiTest")); }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (capability == MainPlayerCapability.PLAYER_VARIABLES_CAPABILITY && cap != null) return cap.cast();
            return super.getCapability(capability, side);
        }
    }
}
