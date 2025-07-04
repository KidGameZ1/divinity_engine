package net.nightshade.divinity_engine.registry.keys;

import net.nightshade.divinity_engine.network.messages.key.*;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import org.lwjgl.glfw.GLFW;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

public class DivinityEngineKeyMappings {
    private static class BlessingKeyMapping extends KeyMapping {
        private final BlessingState state;

        public BlessingKeyMapping(String name, int keyCode, String category) {
            super(name, keyCode, category);
            this.state = new BlessingState();
        }

        public BlessingState getState() {
            return state;
        }
    }

    private static class BlessingState {
        private boolean isDownOld = false;
        private long lastTickTime = 0;
        private long pressStartTime = 0;

        public boolean wasDown() {
            return isDownOld;
        }

        public void setWasDown(boolean down) {
            this.isDownOld = down;
        }

        public long getLastTickTime() {
            return lastTickTime;
        }

        public void updateLastTickTime(long time) {
            this.lastTickTime = time;
        }

        public void setPressStartTime(long time) {
            this.pressStartTime = time;
        }

        public int getHeldTime() {
            return (int)(System.currentTimeMillis() - pressStartTime);
        }
    }

    public static final BlessingKeyMapping BLESSING_SLOT_1 = new BlessingKeyMapping(
            "key.divinity_engine.blessing_slot_1", 
            GLFW.GLFW_KEY_R, 
            "key.categories.divinity_engine"
    ) {
        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            BlessingState state = getState();
            if (state.wasDown() != isDown && isDown) {
                ModMessages.INSTANCE.sendToServer(new BlessingSlot1Message(0, 0));
                BlessingSlot1Message.pressAction(Minecraft.getInstance().player, 0, 0);
                state.setPressStartTime(System.currentTimeMillis());
                state.updateLastTickTime(System.currentTimeMillis());
            } else if (state.wasDown() != isDown && !isDown) {
                ModMessages.INSTANCE.sendToServer(new BlessingSlot1Message(1, state.getHeldTime()));
                BlessingSlot1Message.pressAction(Minecraft.getInstance().player, 1, state.getHeldTime());
            }
            state.setWasDown(isDown);
        }
    };

    public static final BlessingKeyMapping BLESSING_SLOT_2 = new BlessingKeyMapping(
            "key.divinity_engine.blessing_slot_2", 
            GLFW.GLFW_KEY_G, 
            "key.categories.divinity_engine"
    ) {
        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            BlessingState state = getState();
            if (state.wasDown() != isDown && isDown) {
                ModMessages.INSTANCE.sendToServer(new BlessingSlot2Message(0, 0));
                BlessingSlot2Message.pressAction(Minecraft.getInstance().player, 0, 0);
                state.setPressStartTime(System.currentTimeMillis());
                state.updateLastTickTime(System.currentTimeMillis());
            } else if (state.wasDown() != isDown && !isDown) {
                ModMessages.INSTANCE.sendToServer(new BlessingSlot2Message(1, state.getHeldTime()));
                BlessingSlot2Message.pressAction(Minecraft.getInstance().player, 1, state.getHeldTime());
            }
            state.setWasDown(isDown);
        }
    };

    public static final BlessingKeyMapping BLESSING_SLOT_3 = new BlessingKeyMapping(
            "key.divinity_engine.blessing_slot_3", 
            GLFW.GLFW_KEY_V, 
            "key.categories.divinity_engine"
    ) {
        @Override
        public void setDown(boolean isDown) {
            super.setDown(isDown);
            BlessingState state = getState();
            if (state.wasDown() != isDown && isDown) {
                ModMessages.INSTANCE.sendToServer(new BlessingSlot3Message(0, 0));
                BlessingSlot3Message.pressAction(Minecraft.getInstance().player, 0, 0);
                state.setPressStartTime(System.currentTimeMillis());
                state.updateLastTickTime(System.currentTimeMillis());
            } else if (state.wasDown() != isDown && !isDown) {
                ModMessages.INSTANCE.sendToServer(new BlessingSlot3Message(1, state.getHeldTime()));
                BlessingSlot3Message.pressAction(Minecraft.getInstance().player, 1, state.getHeldTime());
            }
            state.setWasDown(isDown);
        }
    };

    @Mod.EventBusSubscriber({Dist.CLIENT})
    public static class KeyEventListener {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) {
                long currentTime = System.currentTimeMillis();

                // Handle BLESSING_SLOT_1
                if (BLESSING_SLOT_1.isDown()) {
                    BlessingState state = BLESSING_SLOT_1.getState();
                    if (currentTime - state.getLastTickTime() >= 250) { // More frequent updates (every 250ms)
                        int heldTime = state.getHeldTime();
                        // Show debug message
                        if (Minecraft.getInstance().player != null) {
                            ModMessages.INSTANCE.sendToServer(new BlessingSlot1Message(3, heldTime));
                            // Directly call pressAction to see effect on client too
                            BlessingSlot1Message.pressAction(Minecraft.getInstance().player, 3, heldTime);
                        }
                        state.updateLastTickTime(currentTime);
                    }
                }

                // Handle BLESSING_SLOT_2
                if (BLESSING_SLOT_2.isDown()) {
                    BlessingState state = BLESSING_SLOT_2.getState();
                    if (currentTime - state.getLastTickTime() >= 250) {
                        int heldTime = state.getHeldTime();
                        if (Minecraft.getInstance().player != null) {
                            ModMessages.INSTANCE.sendToServer(new BlessingSlot2Message(3, heldTime));
                            BlessingSlot2Message.pressAction(Minecraft.getInstance().player, 3, heldTime);
                        }
                        state.updateLastTickTime(currentTime);
                    }
                }

                // Handle BLESSING_SLOT_3
                if (BLESSING_SLOT_3.isDown()) {
                    BlessingState state = BLESSING_SLOT_3.getState();
                    if (currentTime - state.getLastTickTime() >= 250) {
                        int heldTime = state.getHeldTime();
                        if (Minecraft.getInstance().player != null) {
                            ModMessages.INSTANCE.sendToServer(new BlessingSlot3Message(3, heldTime));
                            BlessingSlot3Message.pressAction(Minecraft.getInstance().player, 3, heldTime);
                        }
                        state.updateLastTickTime(currentTime);
                    }
                }

                // Consume clicks
                BLESSING_SLOT_1.consumeClick();
                BLESSING_SLOT_2.consumeClick();
                BLESSING_SLOT_3.consumeClick();
            }
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KeyMappingRegistry {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(BLESSING_SLOT_1);
            event.register(BLESSING_SLOT_2);
            event.register(BLESSING_SLOT_3);
        }
    }
}