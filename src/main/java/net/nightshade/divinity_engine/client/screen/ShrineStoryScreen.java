package net.nightshade.divinity_engine.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.nightshade.divinity_engine.client.ui.DivinityUi;
import net.nightshade.divinity_engine.client.ui.ShrineUi;
import net.nightshade.divinity_engine.network.messages.narrative.NarrativeNetwork;
import net.nightshade.divinity_engine.network.messages.narrative.QuestMenuAction;
import net.nightshade.renderlib.client.ui.RetainedUiTree;

/** A conversation over the still-open shrine container, never a remote quest or command console. */
public class ShrineStoryScreen extends Screen {
    private final BlessingsScreen parent;
    private CompoundTag data;
    private int left, top, panelWidth, panelHeight, scroll, actionPage, waitTicks;
    private final RetainedUiTree tree = new RetainedUiTree(DivinityUi.canvas("shrine_story").build());

    public ShrineStoryScreen(BlessingsScreen parent, CompoundTag data) {
        super(Component.translatable("gui.divinity_engine.story.conversation"));
        this.parent = parent;
        this.data = data.copy();
    }

    public static void receive(int containerId, CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.containerMenu.containerId != containerId || data.isEmpty()) return;
        if (mc.screen instanceof ShrineStoryScreen screen && screen.parent.getMenu() == mc.player.containerMenu
                && screen.data.getString("story").equals(data.getString("story"))) {
            screen.data = data.copy(); screen.waitTicks = 0; screen.rebuildWidgets();
        } else if (mc.screen instanceof BlessingsScreen parent && parent.getMenu() == mc.player.containerMenu
                && parent.getMenu().isOpenedFromStatue()) {
            mc.setScreen(new ShrineStoryScreen(parent, data));
        }
    }

    @Override protected void init() {
        panelWidth = Math.min(404, width - 16);
        panelHeight = Math.min(292, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        var actions = data.getList("actions", 10);
        int pages = Math.max(1, (actions.size() + 2) / 3);
        actionPage = Math.max(0, Math.min(actionPage, pages - 1));
        int visibleActions = Math.min(3, actions.size() - actionPage * 3);
        for (int i = 0; i < 3 && actionPage * 3 + i < actions.size(); i++) {
            var action = actions.getCompound(actionPage * 3 + i);
            var button = button(left + 12, top + panelHeight - 33 - visibleActions * 23 + i * 23, panelWidth - 24,
                    Component.literal(action.getString("label")), () -> send(action));
            button.active = waitTicks == 0;
        }
        button(left + 12, top + panelHeight - 29, panelWidth - 24,
                tr("back"), this::onClose);
        if (pages > 1) {
            button(left + 12, top + 30, 22, Component.literal("<"), () -> { actionPage--; rebuildWidgets(); }).active = actionPage > 0;
            button(left + panelWidth - 34, top + 30, 22, Component.literal(">"), () -> { actionPage++; rebuildWidgets(); }).active = actionPage + 1 < pages;
        }
    }

    private ShrineUi.StoneButton button(int x, int y, int w, Component label, Runnable action) {
        var button = new ShrineUi.StoneButton(x, y, w, 20, label, b -> action.run());
        button.setTooltip(Tooltip.create(label));
        return addRenderableWidget(button);
    }

    private void send(CompoundTag action) {
        var story = ResourceLocation.tryParse(data.getString("story"));
        if (story == null || minecraft.player == null || minecraft.player.containerMenu != parent.getMenu()) return;
        NarrativeNetwork.request(new QuestMenuAction(parent.getMenu().containerId, story,
                action.getString("action"), action.getString("branch"), action.getString("token")));
        waitTicks = 10;
        rebuildWidgets();
    }

    @Override public void tick() {
        if (minecraft.player == null || minecraft.player.containerMenu != parent.getMenu()) {
            minecraft.setScreen(null); return;
        }
        if (waitTicks > 0 && --waitTicks == 0) rebuildWidgets();
    }

    @Override public void onClose() {
        minecraft.setScreen(minecraft.player != null && minecraft.player.containerMenu == parent.getMenu() ? parent : null);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var root = DivinityUi.canvas("shrine_story")
                .child(ShrineUi.background(left, top, panelWidth, panelHeight, 10, ShrineUi.GOLD))
                .child(DivinityUi.text("title", font.plainSubstrByWidth(data.getString("title"), panelWidth - 28),
                        left + 14, top + 12, panelWidth - 28, 12, ShrineUi.GOLD));
        tree.setRoot(root.build()); tree.layout(0, 0, width, height); tree.render(graphics, mouseX, mouseY, partialTick);
        var lines = font.split(Component.literal(data.getString("text")), panelWidth - 32);
        int actionCount = data.getList("actions", 10).size();
        int visibleActions = Math.min(3, Math.max(0, actionCount - actionPage * 3));
        int bodyTop = actionCount > 3 ? 56 : 36;
        int textBottom = panelHeight - 49 - visibleActions * 23;
        int rows = Math.max(1, (textBottom - bodyTop) / 11);
        scroll = Math.max(0, Math.min(scroll, lines.size() - rows));
        for (int i = 0; i < rows && scroll + i < lines.size(); i++)
            graphics.drawString(font, lines.get(scroll + i), left + 16, top + bodyTop + i * 11, ShrineUi.TEXT, false);
        if (lines.size() > rows) graphics.drawString(font, tr("scroll"), left + 16, top + textBottom + 2, ShrineUi.MUTED, false);
        if (data.getList("actions", 10).size() > 3)
            graphics.drawCenteredString(font, tr("actions", actionPage + 1, (data.getList("actions", 10).size() + 2) / 3),
                    width / 2, top + 35, ShrineUi.MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseScrolled(double x, double y, double delta) { scroll += delta > 0 ? -3 : 3; return true; }
    @Override public boolean isPauseScreen() { return false; }
    private static Component tr(String key, Object... args) { return Component.translatable("gui.divinity_engine.story." + key, args); }
}
