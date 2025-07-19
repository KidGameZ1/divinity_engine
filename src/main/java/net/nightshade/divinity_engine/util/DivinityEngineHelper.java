package net.nightshade.divinity_engine.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.cap.player.gods.MainPlayerCapability;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * Wraps and limits a text block to fit within given pixel width and line height constraints.
     *
     * @param fontRenderer Font instance to calculate width
     * @param text The full unwrapped text
     * @param maxWidth Max width in pixels for each line
     * @param maxHeight Max total height (i.e. number of lines * lineHeight)
     * @param lineHeight Height of one line (usually 9 or font.lineHeight)
     * @return A list of Component lines to render
     */
    public static List<Component> wrapText(Font fontRenderer, String text, int maxWidth, int maxHeight, int lineHeight) {
        List<Component> wrappedLines = new ArrayList<>();

        int maxLines = maxHeight / lineHeight;

        List<FormattedText> split = fontRenderer.getSplitter().splitLines(Component.literal(text), maxWidth, Component.literal("").getStyle());

        for (int i = 0; i < Math.min(split.size(), maxLines); i++) {
            wrappedLines.add(Component.literal(String.valueOf(split.get(i))));
        }

        return wrappedLines;
    }

    public static Component formattedTextToComponent(FormattedText formattedText) {
        // Simply wrap the string version into a Component.
        // Note: This will lose advanced formatting unless you handle styles manually.
        return Component.literal(formattedText.getString());
    }
    public static FormattedText componentToFormattedText(Component component) {
        // Component implements FormattedText, so we can safely return it
        return component;
    }
    /**
     * Wraps and centers text within a box of given width and height.
     *
     * @param font Font renderer
     * @param text Raw text to wrap and center
     * @param maxWidth Max pixel width of the text box
     * @param maxHeight Max pixel height of the text box
     * @param lineHeight Line height in pixels (usually font.lineHeight)
     * @return A list of lines (Components) to render
     */
    public static List<Component> wrapCentered(Font font, String text, int maxWidth, int maxHeight, int lineHeight) {
        List<Component> wrapped = new ArrayList<>();
        int maxLines = maxHeight / lineHeight;

        List<FormattedText> split = font.getSplitter().splitLines(Component.literal(text), maxWidth, Component.literal("").getStyle());

        for (int i = 0; i < Math.min(split.size(), maxLines); i++) {
            wrapped.add(Component.literal(String.valueOf(split.get(i))));
        }

        return wrapped;
    }

    /**
     * Renders wrapped and centered lines using drawString.
     *
     * @param guiGraphics The GuiGraphics to draw with
     * @param font Font renderer
     * @param lines Wrapped lines from wrapCentered()
     * @param x Left boundary of the box
     * @param y Top boundary of the box
     * @param boxWidth Width of the text box
     * @param color Text color (e.g., 0xFFFFFF)
     */
    public static void drawWrappedCentered(GuiGraphics guiGraphics, Font font, List<Component> lines, int x, int y, int boxWidth, int color) {
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int textWidth = font.width(line);
            int lineX = x + (boxWidth - textWidth) / 2;
            int lineY = y + i * font.lineHeight;
            guiGraphics.drawString(font, line, lineX, lineY, color, false);
        }
    }

    public static void drawWrappedWithScale(GuiGraphics graphics, Font font, Component text, int x, int y, int wrapWidth, float scale, int color) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        poseStack.translate(x, y, 0);

        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }

        int scaledWrapWidth = (int)(wrapWidth / scale);
        List<FormattedCharSequence> lines = font.split(text, scaledWrapWidth);

        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            graphics.drawString(font, line, 0, i * 9, color, false); // No centering
        }

        poseStack.popPose();
    }

    public record LineSplitResult(Component visible, Component overflow) {}

    /**
     * Splits text into visible and overflowing lines based on Y-position and scale.
     *
     * @param font      The Minecraft font renderer.
     * @param text      The text to wrap.
     * @param wrapWidth The max width for wrapping.
     * @param startY    The starting Y pixel coordinate.
     * @param maxY      The bottom pixel boundary.
     * @param scale     The text scale factor.
     * @return LineSplitResult with visible lines and overflow lines.
     */
    public static LineSplitResult splitOverflowingLines(Font font, Component text, int wrapWidth, int startY, int maxY, float scale) {
        List<FormattedCharSequence> allLines = font.split(text, (int)(wrapWidth / scale));
        List<Component> visibleLines = new ArrayList<>();
        List<Component> overflowLines = new ArrayList<>();

        int lineHeight = (int)(9 * scale);
        int currentY = startY;

        for (FormattedCharSequence line : allLines) {
            // Safely convert each line to Component while preserving formatting
            Component lineComponent = Component.literal("").append(formattedCharSequenceToComponent(line));

            if (currentY + lineHeight <= maxY) {
                visibleLines.add(lineComponent);
            } else {
                overflowLines.add(lineComponent);
            }

            currentY += lineHeight;
        }

        MutableComponent visibleComponent = Component.empty();
        for (int i = 0; i < visibleLines.size(); i++) {
            visibleComponent.append(visibleLines.get(i));
            if (i < visibleLines.size() - 1) visibleComponent.append(Component.literal("\n"));
        }

        MutableComponent overflowComponent = Component.empty();
        for (int i = 0; i < overflowLines.size(); i++) {
            overflowComponent.append(overflowLines.get(i));
            if (i < overflowLines.size() - 1) overflowComponent.append(Component.literal("\n"));
        }

        return new LineSplitResult(visibleComponent, overflowComponent);
    }

    private static Component formattedCharSequenceToComponent(FormattedCharSequence seq) {
        MutableComponent result = Component.empty();
        StringBuilder sb = new StringBuilder();
        final Style[] currentStyle = {Style.EMPTY};

        seq.accept((index, style, codePoint) -> {
            if (!style.equals(currentStyle[0]) && sb.length() > 0) {
                // Flush the previous group of characters with the old style
                result.append(Component.literal(sb.toString()).withStyle(currentStyle[0]));
                sb.setLength(0);
            }

            currentStyle[0] = style;
            sb.appendCodePoint(codePoint);
            return true;
        });

        // Append remaining characters
        if (sb.length() > 0) {
            result.append(Component.literal(sb.toString()).withStyle(currentStyle[0]));
        }

        return result;
    }

    /**
     * Checks whether the wrapped lines of text would overflow a given vertical space.
     *
     * @param font      The font renderer.
     * @param text      The component text to wrap.
     * @param wrapWidth The max width of the text wrapping.
     * @param startY    The starting Y position.
     * @param maxY      The maximum bottom Y position.
     * @param scale     The scale of the text.
     * @return true if the wrapped lines overflow the area, false otherwise.
     */
    public static boolean doesTextOverflow(Font font, Component text, int wrapWidth, int startY, int maxY, float scale) {
        List<FormattedCharSequence> lines = font.split(text, (int)(wrapWidth / scale));
        int lineHeight = (int)(9 * scale);
        int totalHeight = lines.size() * lineHeight;
        return (startY + totalHeight) > maxY;
    }

    /**
     * Renders wrapped and centered lines using drawString.
     *
     * @param guiGraphics The GuiGraphics to draw with
     * @param font Font renderer
     * @param line Wrapped lines from wrapCentered()
     * @param x Left boundary of the box
     * @param y Top boundary of the box
     * @param boxWidth Width of the text box
     * @param color Text color (e.g., 0xFFFFFF)
     */
    public static void drawWrappedCentered(GuiGraphics guiGraphics, Font font, Component line, int x, int y, int boxWidth, int color) {
        List<Component> lines = wrapCentered(font, line.getString(), boxWidth, font.lineHeight, font.lineHeight);
        for (int i = 0; i < lines.size(); i++) {
            Component newLine = lines.get(i);
            int textWidth = font.width(newLine);
            int lineX = x + (boxWidth - textWidth) / 2;
            int lineY = y + i * font.lineHeight;
            guiGraphics.drawString(font, newLine, lineX, lineY, color, false);
        }
    }

    public static void drawWrapped(GuiGraphics graphics, Font font, Component text, int x, int y, int wrapWidth, int color) {
        List<FormattedCharSequence> lines = font.split(text, wrapWidth);

        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            graphics.drawString(font, line, x, y + i * font.lineHeight, color, false);
        }
    }


    /**
     * Wraps and centers text within a given box size, with optional scaling.
     *
     * @param font       Font renderer
     * @param text       The full string to wrap and center
     * @param maxWidth   Width of the area (before scaling)
     * @param maxHeight  Height of the area (before scaling)
     * @param scale      Scale factor for text (e.g., 1.0f = normal size, 0.75f = smaller)
     * @return List of wrapped lines (Components)
     */
    public static List<Component> wrapCenteredWithScale(Font font, String text, int maxWidth, int maxHeight, float scale) {
        List<Component> lines = new ArrayList<>();
        int scaledLineHeight = (int) (font.lineHeight * scale);
        int maxLines = maxHeight / scaledLineHeight;

        List<FormattedText> split = font.getSplitter().splitLines(Component.literal(text), (int) (maxWidth / scale), Component.literal("").getStyle());

        for (int i = 0; i < Math.min(split.size(), maxLines); i++) {
            lines.add(Component.literal(String.valueOf(split.get(i))));
        }

        return lines;
    }

    /**
     * Renders the wrapped + centered text with scaling.
     *
     * @param guiGraphics GuiGraphics context
     * @param font        Font renderer
     * @param lines       Wrapped lines to render
     * @param x           X position of the box (top-left, unscaled)
     * @param y           Y position of the box (top-left, unscaled)
     * @param boxWidth    Width of the box (unscaled)
     * @param scale       Scale of the text (1.0 = normal size)
     * @param color       Text color
     */
    public static void drawWrappedCenteredWithScale(GuiGraphics guiGraphics, Font font, List<Component> lines, int x, int y, int boxWidth, float scale, int color) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        int scaledX = (int) (x / scale);
        int scaledY = (int) (y / scale);
        int lineHeight = font.lineHeight;

        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int textWidth = font.width(line);
            int centeredX = scaledX + (int) ((boxWidth / scale - textWidth) / 2.0f);
            int lineY = scaledY + (int) (i * lineHeight);
            guiGraphics.drawString(font, line, centeredX, lineY, color, false);
        }

        poseStack.popPose();
    }
    public static void drawWrappedCenteredWithScale(GuiGraphics graphics, Font font, Component text, int centerX, int startY, int wrapWidth, float scale, int color) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        // Scale-aware width
        int scaledWrapWidth = (int) (wrapWidth / scale);

        // Wrap text
        List<FormattedCharSequence> lines = font.split(text, scaledWrapWidth);

        // Total height of the text block
        int totalHeight = lines.size() * 9; // 9 is line height

        // Apply translation to center horizontally
        poseStack.translate(centerX, startY, 0);
        poseStack.scale(scale, scale, scale);

        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            int lineWidth = font.width(line);

            // Centered drawing (horizontally)
            graphics.drawString(font, line, -lineWidth / 2, i * 9, color, false);
        }

        poseStack.popPose();
    }


}
