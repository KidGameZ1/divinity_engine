package net.nightshade.divinity_engine.client.trial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.nightshade.divinity_engine.util.divinity.trials.TrialSessionState;
import net.nightshade.divinity_engine.client.ui.DivinityUi;
import net.nightshade.divinity_engine.client.ui.GalaxyUi;
import net.nightshade.renderlib.client.ui.RetainedUiTree;
import net.nightshade.renderlib.ui.UiNode;

import java.util.Locale;

public final class TrialHudOverlay {
    public static final IGuiOverlay OVERLAY = (forgeGui, guiGraphics, partialTick, screenWidth, screenHeight) ->
            render(guiGraphics, screenWidth, screenHeight);

    private static final int PANEL_WIDTH = 188;
    private static final int PANEL_HEIGHT = 44;
    private static final int BAR_HEIGHT = 4;
    private static final RetainedUiTree TREE = new RetainedUiTree(DivinityUi.canvas("trial_hud").build());

    private TrialHudOverlay() {
    }

    private static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (!ClientTrialState.shouldRender()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        int x = (screenWidth - PANEL_WIDTH) / 2;
        int y = 8;

        var root = DivinityUi.canvas("trial_hud")
                .child(DivinityUi.panel("panel", x, y, PANEL_WIDTH, PANEL_HEIGHT, 0xD80B1421))
                .child(DivinityUi.panel("accent", x, y, PANEL_WIDTH, 1, GalaxyUi.GOLD));

        String title = ClientTrialState.getGodName().isEmpty()
                ? ClientTrialState.getTrialName()
                : ClientTrialState.getGodName() + ": " + ClientTrialState.getTrialName();
        String heading = trimToWidth(font, title, PANEL_WIDTH - 16);
        root.child(DivinityUi.text("title", heading, x + (PANEL_WIDTH - font.width(heading)) / 2 - 2,
                y + 4, font.width(heading) + 4, 11, 0xFFFFFFFF));

        TrialSessionState state = ClientTrialState.getSessionState();
        String phaseText = ClientTrialState.getPhaseName().isEmpty()
                ? "Phase " + Math.max(ClientTrialState.getPhase(), 1)
                : ClientTrialState.getPhaseName();
        String statusText = ClientTrialState.getStatusText();
        int detailColor = 0xFFFFFF;
        String lineOneLeft = phaseText;
        String lineOneRight = "";
        String lineTwoLeft = ClientTrialState.getObjectiveText();
        String lineTwoRight = "";

        if (state == TrialSessionState.PREPARING) {
            lineOneLeft = "Preparing arena";
            lineTwoLeft = "You will enter when ready.";
        } else if (state == TrialSessionState.COUNTDOWN) {
            lineOneLeft = "Preparation";
            lineOneRight = formatTicks(ClientTrialState.getCountdownTicksRemaining());
            lineTwoLeft = statusText.isEmpty() ? "Prepare yourself." : statusText;
        } else if (state == TrialSessionState.ACTIVE) {
            lineOneRight = formatTicks(ClientTrialState.getPhaseTicksRemaining());
            if (ClientTrialState.getObjectiveTarget() > 0) {
                lineTwoRight = ClientTrialState.getObjectiveCurrent() + "/" + ClientTrialState.getObjectiveTarget();
            }
        } else if (state == TrialSessionState.RETURNING) {
            boolean completed = statusText.toLowerCase(Locale.ROOT).contains("completed");
            lineOneLeft = completed ? "Complete" : "Failed";
            lineOneRight = formatTicks(ClientTrialState.getReturnTicksRemaining());
            lineTwoLeft = statusText.isEmpty()
                    ? (completed ? "Trial completed." : "Trial failed.")
                    : statusText;
            detailColor = completed ? 0xC4F4A5 : 0xFFD58E;
        }

        drawAlignedRow(root, "phase", font, x + 8, y + 17, PANEL_WIDTH - 16, lineOneLeft, lineOneRight, 0xD7E9FF);
        drawAlignedRow(root, "objective", font, x + 8, y + 28, PANEL_WIDTH - 16, lineTwoLeft, lineTwoRight, detailColor);

        int barX = x + 8;
        int barY = y + PANEL_HEIGHT - 7;
        int barWidth = PANEL_WIDTH - 16;
        root.child(DivinityUi.panel("track", barX, barY, barWidth, BAR_HEIGHT, 0xFF1E2632));
        root.child(DivinityUi.panel("progress", barX, barY, progressWidth(barWidth), BAR_HEIGHT, GalaxyUi.GOLD));
        TREE.setRoot(root.build());
        TREE.layout(0, 0, screenWidth, screenHeight);
        TREE.render(guiGraphics, -1, -1, 0);
    }

    private static int progressWidth(int barWidth) {
        TrialSessionState state = ClientTrialState.getSessionState();
        float progress;
        if (state == TrialSessionState.COUNTDOWN) {
            int remaining = ClientTrialState.getCountdownTicksRemaining();
            progress = 1.0F - (remaining / 100.0F);
        } else if (state == TrialSessionState.RETURNING) {
            int remaining = ClientTrialState.getReturnTicksRemaining();
            progress = 1.0F - (remaining / 100.0F);
        } else if (ClientTrialState.getPhaseDuration() > 0) {
            progress = 1.0F - ((float) ClientTrialState.getPhaseTicksRemaining() / (float) ClientTrialState.getPhaseDuration());
        } else {
            progress = 0.0F;
        }
        return Mth.floor(Mth.clamp(progress, 0.0F, 1.0F) * barWidth);
    }

    private static void drawAlignedRow(UiNode.Builder root, String key, Font font, int x, int y, int width,
                                       String leftText, String rightText, int color) {
        String right = rightText == null ? "" : rightText;
        int rightWidth = right.isEmpty() ? 0 : font.width(right);
        int leftMaxWidth = Math.max(width - rightWidth - (right.isEmpty() ? 0 : 6), 0);
        String left = trimToWidth(font, leftText == null ? "" : leftText, leftMaxWidth);

        if (!left.isEmpty()) {
            root.child(DivinityUi.text(key + "_left", left, x - 2, y - 1, leftMaxWidth + 4, 11, 0xFF000000 | color));
        }

        if (!right.isEmpty()) {
            root.child(DivinityUi.text(key + "_right", right, x + width - rightWidth - 2, y - 1, rightWidth + 4, 11, 0xFFFFFFFF));
        }
    }

    private static String trimToWidth(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ellipsis;
    }

    private static String formatTicks(int ticks) {
        return String.format("%.1fs", Math.max(ticks, 0) / 20.0F);
    }
}
