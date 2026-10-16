package net.nightshade.divinity_engine.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.nightshade.divinity_engine.client.ui.DivinityUi;
import net.nightshade.divinity_engine.client.ui.ConstellationButton;
import net.nightshade.divinity_engine.client.ui.GalaxyUi;
import net.nightshade.divinity_engine.client.ui.NightshadeButton;
import net.nightshade.divinity_engine.client.ui.NightshadeEditBox;
import net.nightshade.divinity_engine.client.ui.ShrineUi;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.network.messages.ModMessages;
import net.nightshade.divinity_engine.network.messages.gui.EquipBlessingMessage;
import net.nightshade.divinity_engine.network.messages.gui.TrialButtonMessage;
import net.nightshade.divinity_engine.registry.divinity.trial.GodlyTrialsRegistry;
import net.nightshade.divinity_engine.registry.gui.DivinityUiTypes;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import net.nightshade.divinity_engine.world.inventory.BlessingsMenu;
import net.nightshade.renderlib.client.ui.RenderLibContainerScreen;
import net.nightshade.renderlib.ui.UiNode;
import net.nightshade.renderlib.ui.UiRenderLayer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Local browsing, server-owned loadout; a shrine menu is permanently bound to its opening god. */
public class BlessingsScreen extends RenderLibContainerScreen<BlessingsMenu> {
    private static final int GOD_ROW_STEP = 22;
    private static final int BLESSING_ROW_STEP = 22;
    private final List<NightshadeButton> godTabs = new ArrayList<>();
    private final List<NightshadeButton> blessingButtons = new ArrayList<>();
    private final List<NightshadeButton> slotButtons = new ArrayList<>();
    private List<BaseGod> gods = List.of();
    private List<BlessingsInstance> blessings = List.of();
    private BaseGod selectedGod;
    private ResourceLocation selectedBlessing;
    private NightshadeEditBox search;
    private NightshadeButton trial;
    private NightshadeButton communion;
    private NightshadeButton previousBlessings, nextBlessings;
    private String query = "";
    private int godOffset, blessingOffset, descriptionOffset;
    private int tabsX, tabsY, tabsWidth, rightX, rightWidth, gridY, detailsY, slotsY;
    private int gridRows, pagingY;
    private boolean revealBlessing;
    private enum View { BLESSINGS, QUESTS, FAVOR }
    private View view = View.BLESSINGS;
    private final List<NightshadeButton> viewButtons = new ArrayList<>();
    private NightshadeButton previousQuest, nextQuest, questAction, scrollUp, scrollDown;
    private List<net.minecraft.nbt.CompoundTag> quests = List.of();
    private CompoundTag selectedRelationship = new CompoundTag();
    private int questIndex, journalOffset;

    public BlessingsScreen(BlessingsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        selectedGod = menu.getGod();
    }

    @Override
    protected void init() {
        imageWidth = Math.min(480, width - 16);
        imageHeight = Math.min(300, height - 16);
        super.init();
        tabsX = leftPos + 10;
        tabsWidth = Math.max(82, Math.min(108, imageWidth / 4));
        tabsY = topPos + 54;
        rightX = tabsX + tabsWidth + 10;
        rightWidth = leftPos + imageWidth - 10 - rightX;
        gridY = topPos + 60;
        int rows = imageHeight < 270 ? 2 : 3;
        gridRows = rows;
        detailsY = gridY + rows * BLESSING_ROW_STEP + 4;
        pagingY = gridY + rows * BLESSING_ROW_STEP + 1;
        revealBlessing = true;
        slotsY = topPos + imageHeight - 48;
        godTabs.clear();
        blessingButtons.clear();
        slotButtons.clear();
        viewButtons.clear();
        search = null;
        if (!menu.isOpenedFromStatue()) {
            search = new NightshadeEditBox(font, tabsX, topPos + 30, tabsWidth, 18, tr("search"));
            search.setThemeResolver(GalaxyUi::search);
            search.setHint(tr("search"));
            search.setMaxLength(80);
            search.setValue(query);
            search.setResponder(value -> {
                query = value;
                godOffset = 0;
                refresh();
                if (value.isBlank()) revealSelectedGod();
            });
            addRenderableWidget(search);
            int godRows = Math.max(1, (imageHeight - 88) / GOD_ROW_STEP);
            for (int i = 0; i < godRows; i++) {
                int row = i;
                godTabs.add(button(tabsX, tabsY + i * GOD_ROW_STEP, tabsWidth, 20, Component.empty(), "god", () -> {
                    int index = godOffset + row;
                    if (index < gods.size()) selectGod(gods.get(index));
                }));
            }
        }
        int viewWidth = (rightWidth - 28) / 3;
        for (View target : View.values()) {
            var tab = button(rightX + target.ordinal() * (viewWidth + 3), topPos + 9, viewWidth, 17,
                    tr("view_" + target.name().toLowerCase(Locale.ROOT)), "blessing", () -> {
                        view = target; journalOffset = 0; refresh();
                    });
            tab.setTooltip(Tooltip.create(tab.getMessage())); viewButtons.add(tab);
        }
        previousQuest = button(rightX, gridY, 16, 16, Component.literal("<"), "blessing", () -> { questIndex--; journalOffset = 0; refresh(); });
        nextQuest = button(rightX + rightWidth - 16, gridY, 16, 16, Component.literal(">"), "blessing", () -> { questIndex++; journalOffset = 0; refresh(); });
        scrollUp = button(rightX, slotsY, (rightWidth - 4) / 2, 20, tr("read_up"), "seal", () -> { journalOffset -= 4; refresh(); });
        scrollDown = button(rightX + (rightWidth - 4) / 2 + 4, slotsY, (rightWidth - 4) / 2, 20, tr("read_down"), "seal", () -> { journalOffset += 4; refresh(); });
        questAction = button(rightX, slotsY + 24, rightWidth, 16, tr("quest_status"), "trial", () -> {
            var quest = quest(); if (quest == null || !questAction.active) return;
            var id = ResourceLocation.tryParse(quest.getString("story"));
            if (id != null) net.nightshade.divinity_engine.network.messages.narrative.NarrativeNetwork.request(
                    new net.nightshade.divinity_engine.network.messages.narrative.QuestMenuAction(menu.containerId, id, quest.getString("action")));
        });
        int cellWidth = (rightWidth - 4) / 2;
        for (int i = 0; i < rows * 2; i++) {
            int cell = i;
            blessingButtons.add(button(rightX + (i % 2) * (cellWidth + 4), gridY + (i / 2) * BLESSING_ROW_STEP,
                    cellWidth, 20, Component.empty(), "blessing", () -> {
                        int index = blessingOffset + cell;
                        if (index < blessings.size()) {
                            selectedBlessing = blessings.get(index).getBlessing().getRegistryName();
                            descriptionOffset = 0;
                            refresh();
                        }
                    }));
        }
        previousBlessings = button(rightX, pagingY, 18, 12, Component.literal("<"), "blessing", () -> pageBlessings(-1));
        previousBlessings.setTooltip(Tooltip.create(tr("previous_blessings")));
        nextBlessings = button(rightX + rightWidth - 18, pagingY, 18, 12, Component.literal(">"), "blessing", () -> pageBlessings(1));
        nextBlessings.setTooltip(Tooltip.create(tr("next_blessings")));
        int slotWidth = (rightWidth - 8) / 3;
        for (int i = 0; i < 3; i++) {
            int slot = i;
            slotButtons.add(button(rightX + i * (slotWidth + 4), slotsY, slotWidth, 20, Component.empty(), "seal", () -> equip(slot)));
        }
        trial = button(rightX, slotsY + 24, rightWidth, 16, tr("trial"), "trial", () -> {
            if (!canStartTrial()) return;
            var pos = menu.getOpeningPos();
            ModMessages.sendToServer(new TrialButtonMessage(1, pos.getX(), pos.getY(), pos.getZ()));
        });
        // A god-specific action belongs in the navigation rail, not over the view tabs.
        communion = button(tabsX, topPos + imageHeight - 28, tabsWidth, 20, Component.translatable("gui.divinity_engine.communion"), "communion", () -> {
            if (selectedGod != null) net.nightshade.divinity_engine.client.innerworld.CommunionClient.open(selectedGod.getRegistryName());
        });
        button(leftPos + imageWidth - 28, topPos + 10, 18, 16, Component.literal("X"), "close", this::onClose)
                .setTooltip(Tooltip.create(tr("close")));
        refresh();
        revealSelectedGod();
        setInitialFocus(search != null ? search : blessingButtons.get(0));
    }

    private void revealSelectedGod() {
        if (menu.isOpenedFromStatue()) return;
        int selectedIndex = selectedGod == null ? -1 : gods.indexOf(selectedGod);
        if (selectedIndex >= 0 && (selectedIndex < godOffset || selectedIndex >= godOffset + godTabs.size())) {
            godOffset = selectedIndex;
            refresh();
        }
    }

    private NightshadeButton button(int x, int y, int width, int height, Component label, String role, Runnable action) {
        NightshadeButton button = menu.isOpenedFromStatue()
                ? new ShrineUi.StoneButton(x, y, width, height, label, b -> action.run())
                : new ConstellationButton(x, y, width, height, label, role, b -> action.run());
        return addRenderableWidget(button);
    }

    private void selectGod(BaseGod god) {
        if (god == null || !menu.allowsGod(god.getRegistryName())) return;
        selectedGod = god;
        selectedBlessing = null;
        questIndex = journalOffset = 0;
        blessingOffset = descriptionOffset = 0;
        refresh();
    }

    private void refresh() {
        if (minecraft.player == null) return;
        List<BaseGod> contacted = menu.getVisibleGods().stream()
                .sorted(Comparator.comparing(god -> god.getRegistryName().toString())).toList();
        if (menu.isOpenedFromStatue()) selectedGod = menu.getGod();
        else if (!contacted.contains(selectedGod)) selectFirst(contacted);
        selectedRelationship = net.nightshade.divinity_engine.client.ui.BlessingJournal.bond(journal(),
                selectedGod == null ? null : selectedGod.getRegistryName()).getCompound("relationship");
        communion.active = selectedGod != null;
        communion.setTooltip(Tooltip.create(selectedGod == null ? tr("no_contact")
                : Component.translatable("gui.divinity_engine.communion.selected_god", name(selectedGod))));
        String needle = query.strip().toLowerCase(Locale.ROOT);
        gods = contacted.stream().filter(god -> menu.isOpenedFromStatue() || name(god).getString().toLowerCase(Locale.ROOT).contains(needle)
                || god.getRegistryName().toString().contains(needle)).toList();
        godOffset = clamp(godOffset, gods.size() - godTabs.size());
        var updated = selectedGod == null ? List.<BlessingsInstance>of() : GodHelper.getAllBlessingsInstancesOfGod(minecraft.player, selectedGod)
                .stream().sorted(Comparator.comparingInt(DivineRelationship::order)
                        .thenComparing(value -> value.getBlessing().getRegistryName().toString())).toList();
        boolean listChanged = !sameOrder(blessings, updated);
        blessings = updated;
        if (selected() == null) {
            selectedBlessing = blessings.isEmpty() ? null : blessings.get(0).getBlessing().getRegistryName();
            descriptionOffset = 0;
            revealBlessing = true;
        }
        blessingOffset = clamp(blessingOffset, Math.max(0, (blessings.size() + 1) / 2 - blessingButtons.size() / 2) * 2);
        if (listChanged || revealBlessing) {
            var selected = selected();
            int selectedIndex = selected == null ? -1 : blessings.indexOf(selected);
            if (selectedIndex >= 0 && selectedIndex < blessingOffset) blessingOffset = selectedIndex / 2 * 2;
            else if (selectedIndex >= blessingOffset + blessingButtons.size())
                blessingOffset = (selectedIndex / 2 - blessingButtons.size() / 2 + 1) * 2;
            revealBlessing = false;
        }
        boolean overflow = blessings.size() > blessingButtons.size();
        previousBlessings.visible = nextBlessings.visible = overflow;
        previousBlessings.active = blessingOffset > 0;
        nextBlessings.active = blessingOffset + blessingButtons.size() < blessings.size();
        detailsY = overflow ? pagingY + 17 : gridY + gridRows * BLESSING_ROW_STEP + 4;
        for (int i = 0; i < godTabs.size(); i++) {
            var button = godTabs.get(i);
            button.visible = godOffset + i < gods.size();
            if (!button.visible) continue;
            BaseGod god = gods.get(godOffset + i);
            button.setMessage(name(god));
            button.setSelected(god == selectedGod);
            button.setTooltip(Tooltip.create(name(god)));
        }
        for (int i = 0; i < blessingButtons.size(); i++) {
            var button = blessingButtons.get(i);
            button.visible = blessingOffset + i < blessings.size();
            if (!button.visible) continue;
            var blessing = blessings.get(blessingOffset + i);
            button.setMessage(Component.translatable(blessing.getBlessing().getNameTranslationKey()));
            button.setSelected(Objects.equals(blessing.getBlessing().getRegistryName(), selectedBlessing));
            button.setTooltip(Tooltip.create(button.getMessage().copy().append("\n")
                    .append(tr(availabilityKey(blessing))).append("\n")
                    .append(requirement(blessing))));
        }
        var selected = selected();
        boolean eligible = unlocked(selected);
        boolean equipped = false;
        for (int i = 0; i < 3; i++) {
            var current = DivinityEngineHelper.getBlessingInSlot(minecraft.player, i);
            equipped |= current != null && selected != null && current.getBlessing() == selected.getBlessing();
        }
        for (int i = 0; i < 3; i++) {
            var current = DivinityEngineHelper.getBlessingInSlot(minecraft.player, i);
            boolean bound = same(current, selected);
            var button = slotButtons.get(i);
            button.setMessage(Component.literal((i + 1) + ": ").append(tr(bound ? "remove" : "equip")));
            button.setSelected(bound);
            button.active = selected != null && (bound || (eligible && !equipped));
            button.setTooltip(Tooltip.create(tr("slot", i + 1).append(": ")
                    .append(current == null ? tr("empty") : !menu.allowsGod(current.getBoundGod().getRegistryName())
                            ? tr("bound_elsewhere") : Component.translatable(current.getBlessing().getNameTranslationKey()))));
        }
        trial.visible = selectedGod != null;
        trial.active = canStartTrial();
        trial.setTooltip(Tooltip.create(tr(trial.active ? "trial_ready" : !menu.isOpenedFromStatue() ? "trial_statue"
                : relationship().isEmpty() ? "bond_loading" : GodlyTrialsRegistry.get(selectedGod) == null ? "trial_missing"
                : needsReconciliation() ? "reconcile_required" : "trial_deed")));
        quests = net.nightshade.divinity_engine.client.ui.BlessingJournal.quests(journal(), selectedGod == null ? null : selectedGod.getRegistryName());
        questIndex = clamp(questIndex, quests.size() - 1);
        for (int i = 0; i < viewButtons.size(); i++) viewButtons.get(i).setSelected(i == view.ordinal());
        if (view != View.BLESSINGS) {
            blessingButtons.forEach(button -> button.visible = false);
            slotButtons.forEach(button -> button.visible = false);
            previousBlessings.visible = nextBlessings.visible = trial.visible = false;
        } else slotButtons.forEach(button -> button.visible = true);
        previousQuest.visible = nextQuest.visible = view == View.QUESTS && quests.size() > 1;
        previousQuest.active = questIndex > 0; nextQuest.active = questIndex + 1 < quests.size();
        scrollUp.visible = scrollDown.visible = view != View.BLESSINGS;
        questAction.visible = view == View.QUESTS && quest() != null;
        questAction.active = questAction.visible && menu.isOpenedFromStatue() && quest().getBoolean("contacted")
                && (!quest().getString("stage").equals("DORMANT") || quest().getBoolean("can_start"));
        if (quest() != null) questAction.setMessage(tr("quest_" + quest().getString("action")));
        questAction.setTooltip(Tooltip.create(tr(menu.isOpenedFromStatue() ? "quest_action_hint" : "quest_statue")));
        updatePresentation();
    }

    private static boolean sameOrder(List<BlessingsInstance> before, List<BlessingsInstance> after) {
        if (before.size() != after.size()) return false;
        for (int i = 0; i < before.size(); i++) {
            if (!Objects.equals(before.get(i).getBlessing().getRegistryName(), after.get(i).getBlessing().getRegistryName())) return false;
        }
        return true;
    }

    private void pageBlessings(int direction) {
        blessingOffset += direction * blessingButtons.size();
        refresh();
    }

    private void selectFirst(List<BaseGod> contacted) {
        selectedGod = contacted.isEmpty() ? null : contacted.get(0);
        selectedBlessing = null;
        blessingOffset = descriptionOffset = 0;
    }

    private void updatePresentation() {
        String godId = selectedGod == null ? "" : selectedGod.getRegistryName().getPath();
        int textColor = menu.isOpenedFromStatue() ? ShrineUi.TEXT : GalaxyUi.TEXT;
        int muted = menu.isOpenedFromStatue() ? ShrineUi.MUTED : GalaxyUi.MUTED;
        int gold = menu.isOpenedFromStatue() ? ShrineUi.GOLD : GalaxyUi.GOLD;
        UiNode.Builder root = DivinityUi.canvas("blessings");
        if (menu.isOpenedFromStatue()) {
            int color = selectedGod == null ? ShrineUi.GOLD : selectedGod.getColor().getRGB();
            var inscription = font.split(tr(imageHeight < 280 ? "shrine_bound_short" : "shrine_bound"), tabsWidth - 20);
            int inscriptionHeight = Math.max(0, inscription.size() - 1) * 11 + font.lineHeight;
            int sigilSize = Math.max(32, Math.min(44, communion.getY() - 6 - (topPos + 54) - inscriptionHeight));
            root.child(ShrineUi.background(leftPos, topPos, imageWidth, imageHeight, rightX - leftPos - 5, color))
                    .child(DivinityUi.text("title", tr("shrine_title").getString(), tabsX + 3, topPos + 11, tabsWidth, 12, gold))
                    .child(ShrineUi.sigil(tabsX + (tabsWidth - sigilSize) / 2, topPos + 49, sigilSize, godId, color));
            centeredText(root, "sigil_label", tr("sigil").getString(), tabsX + 4, topPos + 35, tabsWidth - 8, muted);
            int inscriptionY = topPos + 54 + sigilSize;
            for (int i = 0; i < inscription.size() && inscriptionY + i * 11 + font.lineHeight <= communion.getY() - 6; i++) {
                final StringBuilder line = new StringBuilder();
                inscription.get(i).accept((index, style, codePoint) -> { line.appendCodePoint(codePoint); return true; });
                centeredText(root, "inscription_" + i, line.toString(), tabsX + 8, inscriptionY + i * 11, tabsWidth - 16, muted);
            }
        } else {
            root.child(GalaxyUi.background(leftPos, topPos, imageWidth, imageHeight,
                        selectedGod == null ? 0x86C9DF : selectedGod.getColor().getRGB()))
                .child(GalaxyUi.ornament("engraving", DivinityUiTypes.CELESTIAL_FRAME, leftPos, topPos, imageWidth, imageHeight,
                        godId, rightX - leftPos - 5, 0))
                .child(DivinityUi.text("title", tr("chart_title").getString(), tabsX, topPos + 10, tabsWidth, 16, GalaxyUi.GOLD));
        if (gods.isEmpty()) root.child(DivinityUi.text("no_gods", tr("no_matches").getString(), tabsX, tabsY, tabsWidth, 18, GalaxyUi.MUTED));
        scrollIndicator(root, "gods", tabsX + tabsWidth + 2, tabsY, godTabs.size() * GOD_ROW_STEP - 2, godTabs.size(), gods.size(), godOffset);
        }
        root.child(DivinityUi.panel("reading_surface", rightX - 2, gridY - 2, rightWidth + 4,
                slotsY - gridY - 6, 0x7008101D));
        root.child(DivinityUi.panel("binding_rule", rightX, slotsY - 6, rightWidth, 1, (gold & 0x00FFFFFF) | 0x50000000));
        if (view == View.BLESSINGS) scrollIndicator(root, "blessings", rightX + rightWidth + 2, gridY, gridRows * BLESSING_ROW_STEP - 2,
                blessingButtons.size() / 2, (blessings.size() + 1) / 2, blessingOffset / 2);
        if (previousBlessings.visible) {
            String range = tr("blessing_range", blessingOffset + 1,
                    Math.min(blessings.size(), blessingOffset + blessingButtons.size()), blessings.size()).getString();
            range = font.plainSubstrByWidth(range, rightWidth - 44);
            root.child(DivinityUi.text("blessing_range", range, rightX + (rightWidth - font.width(range)) / 2,
                    pagingY + 2, rightWidth - 44, 10, muted));
        }
        if (selectedGod == null) {
            root.child(DivinityUi.at("empty", "text", rightX, topPos + 36, rightWidth, imageHeight - 90)
                    .text(tr("no_contact").getString()).property("wrap", net.nightshade.renderlib.ui.UiValue.bool(true))
                    .style(net.nightshade.renderlib.ui.UiStyle.builder().size(rightWidth, imageHeight - 90).foreground(textColor).build()).build());
        } else {
            if (!menu.isOpenedFromStatue()) {
                root.child(GalaxyUi.ornament("divine_seal", DivinityUiTypes.DIVINE_SEAL,
                        rightX + rightWidth - 24, topPos + 30, 24, 24, godId, 0, 0));
            }
            int headingWidth = rightWidth - (menu.isOpenedFromStatue() ? 0 : 30);
            root.child(DivinityUi.text("god", font.plainSubstrByWidth(name(selectedGod).getString(), headingWidth), rightX, topPos + 30, headingWidth, 14, textColor));
            var relationship = relationship();
            String rank = relationship.isEmpty() ? tr("bond_loading").getString()
                    : tr("bond_rank", net.nightshade.divinity_engine.api.narrative.NarrativeText.resolve(relationship.getString("rank"))).getString();
            root.child(DivinityUi.text("bond", font.plainSubstrByWidth(rank, headingWidth),
                    rightX, topPos + 44, headingWidth, 14, muted));
            if (view != View.BLESSINGS) {
                presentJournal(root, textColor, gold, muted);
                setRenderlibRoot(UiRenderLayer.BACKGROUND, root.build());
                return;
            }
            var selected = selected();
            if (selected != null) {
                var blessing = selected.getBlessing();
                root.child(DivinityUi.text("blessing_name", Component.translatable(blessing.getNameTranslationKey()).getString(),
                        rightX, detailsY, rightWidth, 13, gold));
                String kind = tr(blessing.canToggle() ? "toggle" : blessing.isActive() ? "active" : "passive").getString();
                String availability = tr(availabilityKey(selected)).getString();
                root.child(DivinityUi.text("requirement", font.plainSubstrByWidth(tr("requires", availability, kind).getString(), rightWidth),
                        rightX, detailsY + 13, rightWidth, 13, unlocked(selected) ? muted : 0xFFF0A58B));
                String upgrade = net.nightshade.divinity_engine.client.ui.BlessingJournal.earnedUpgrade(quests, blessing.getRegistryName());
                var lines = font.split(tr("proof", requirement(selected)).append("\n\n")
                        .append(upgrade.isEmpty() ? Component.empty() : Component.literal(upgrade).append(tr("base_blessing")).append("\n"))
                        .append(Component.translatable(blessing.getDescriptionTranslationKey())), rightWidth - 4);
                int visibleLines = Math.max(1, (slotsY - detailsY - 32) / 11);
                descriptionOffset = clamp(descriptionOffset, lines.size() - visibleLines);
                scrollIndicator(root, "description", rightX + rightWidth + 2, detailsY + 28,
                        visibleLines * 11, visibleLines, lines.size(), descriptionOffset);
                for (int i = 0; i < visibleLines && descriptionOffset + i < lines.size(); i++) {
                    StringBuilder text = new StringBuilder();
                    lines.get(descriptionOffset + i).accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; });
                    root.child(DivinityUi.text("description_" + i, text.toString(), rightX, detailsY + 28 + i * 11, rightWidth, 11, textColor));
                }
            } else root.child(DivinityUi.text("no_blessings", tr("no_blessings").getString(),
                    rightX, detailsY, rightWidth, 13, muted));
        }
        setRenderlibRoot(UiRenderLayer.BACKGROUND, root.build());
    }

    private CompoundTag journal() {
        return minecraft.player == null ? new CompoundTag()
                : minecraft.player.getPersistentData().getCompound(net.nightshade.divinity_engine.client.codex.CodexJournal.DATA_KEY);
    }

    private CompoundTag relationship() {
        return selectedRelationship;
    }

    // Only the server journal can grant an unlock; missing/old snapshots fail closed.
    private boolean unlocked(BlessingsInstance blessing) {
        return blessing != null && relationship().getList("unlocked_blessings", Tag.TAG_STRING)
                .contains(StringTag.valueOf(blessing.getBlessing().getRegistryName().toString()));
    }

    private boolean needsReconciliation() {
        String rank = relationship().getString("rank");
        return rank.equals("Displeased") || rank.equals("Enemy");
    }

    private String availabilityKey(BlessingsInstance blessing) {
        if (relationship().isEmpty()) return "bond_loading";
        if (unlocked(blessing)) return "unlocked";
        return needsReconciliation() ? "reconcile_required" : "deed_required";
    }

    private String requirement(BlessingsInstance blessing) {
        if (blessing == null) return "";
        String requirement = relationship().getCompound("requirements").getString(blessing.getBlessing().getRegistryName().toString());
        return requirement.isEmpty() ? tr("bond_loading").getString()
                : net.nightshade.divinity_engine.api.narrative.NarrativeText.resolve(requirement);
    }

    private net.minecraft.nbt.CompoundTag quest() { return quests.isEmpty() ? null : quests.get(questIndex); }

    private void presentJournal(UiNode.Builder root, int textColor, int gold, int muted) {
        String heading, body; float progress = 0;
        if (view == View.QUESTS) {
            var quest = quest();
            heading = quest == null ? tr("view_quests").getString() : net.nightshade.divinity_engine.api.narrative.NarrativeText.resolve(quest.getString("quest_title"));
            if (quest == null) body = tr(journal().contains("entries") ? "no_quests" : "journal_loading").getString();
            else {
                body = quest.getString("status") + "\n\n" + quest.getString("next");
                if (quest.getString("stage").equals("DORMANT") || quest.getString("stage").equals("OFFERING"))
                    body += "\n\nAccepted offering examples: " + quest.getString("offering_examples") + ". Drop an accepted offering in front of the statue after beginning the quest.";
                if (!quest.getString("worship_requirement").isEmpty()) body += "\n\n" + quest.getString("worship_requirement");
                if (quest.getInt("target") > 0) {
                    body += "\n\nObjective: " + quest.getInt("progress") + " / " + quest.getInt("target");
                    progress = Math.min(1, (float) quest.getInt("progress") / quest.getInt("target"));
                }
                body += net.nightshade.divinity_engine.client.ui.BlessingJournal.rewards(quest)
                        + "\n" + tr("quest_finale").getString()
                        + "\n\n" + (menu.isOpenedFromStatue() ? "Use the action below. Speak about quest opens path previews, permanent confirmation and available epilogues." : "Visit this god's statue to start, contribute, choose a path or claim rewards.");
                if (quest.getString("stage").equals("COMPLETE")) progress = 1;
            }
        } else {
            heading = tr("divine_bond").getString();
            var bond = relationship();
            if (bond.isEmpty()) body = tr("bond_loading").getString();
            else {
                body = tr("bond_rank", bond.getString("rank")).getString()
                        + "\n\n" + tr("bond_next", bond.getString("next_action")).getString()
                        + "\n\n" + tr("bond_proofs").getString();
                for (var blessing : blessings) {
                    body += "\n\n" + Component.translatable(blessing.getBlessing().getNameTranslationKey()).getString()
                            + " | " + tr(availabilityKey(blessing)).getString()
                            + "\n" + requirement(blessing);
                }
                body += "\n\n" + tr(bond.getBoolean("trial_unlocked") ? "bond_trial_ready"
                        : needsReconciliation() ? "reconcile_required" : "trial_deed").getString();
                var memories = bond.getList("memories", Tag.TAG_STRING);
                if (!memories.isEmpty()) {
                    body += "\n\n" + tr("bond_memories").getString();
                    for (Tag memory : memories) body += "\n- " + memory.getAsString();
                }
                body += "\n\n" + tr("bond_help").getString();
            }
        }
        heading = font.plainSubstrByWidth(heading, rightWidth - 44);
        root.child(DivinityUi.text("journal_heading", heading, rightX + 20, gridY + 3, rightWidth - 40, 12, gold));
        root.child(DivinityUi.panel("journal_progress_track", rightX, gridY + 21, rightWidth, 3, 0xFF101719));
        if (view == View.QUESTS) root.child(DivinityUi.panel("journal_progress", rightX, gridY + 21, Math.round(rightWidth * Math.max(0, progress)), 3, gold));
        int y = gridY + 30;
        var lines = font.split(net.nightshade.divinity_engine.api.narrative.NarrativeText.component(body), rightWidth - 4);
        int visible = Math.max(1, (slotsY - 10 - y) / 11);
        journalOffset = clamp(journalOffset, lines.size() - visible);
        scrollUp.active = journalOffset > 0; scrollDown.active = journalOffset + visible < lines.size();
        scrollIndicator(root, "journal", rightX + rightWidth + 2, y, visible * 11, visible, lines.size(), journalOffset);
        for (int i = 0; i < visible && journalOffset + i < lines.size(); i++) {
            StringBuilder line = new StringBuilder();
            lines.get(journalOffset + i).accept((index, style, codePoint) -> { line.appendCodePoint(codePoint); return true; });
            root.child(DivinityUi.text("journal_line_" + i, line.toString(), rightX, y + i * 11, rightWidth, 11, textColor));
        }
    }

    private void centeredText(UiNode.Builder root, String key, String text, int x, int y, int width, int color) {
        String fitted = font.plainSubstrByWidth(text, Math.max(1, width - 4));
        root.child(DivinityUi.at(key, "text", x, y, width, 11).text(fitted)
                .style(net.nightshade.renderlib.ui.UiStyle.builder().size(width, 11).foreground(color)
                        .alignment(net.nightshade.renderlib.ui.UiStyle.Alignment.CENTER).build()).build());
    }

    private void scrollIndicator(UiNode.Builder root, String key, int x, int y, int height, int visible, int total, int offset) {
        if (total <= visible) return;
        int thumb = Math.max(5, height * visible / total);
        root.child(DivinityUi.panel(key + "_track", x, y, 1, height, 0x505E665D));
        root.child(DivinityUi.panel(key + "_thumb", x, y + (height - thumb) * offset / (total - visible), 1, thumb, GalaxyUi.GOLD));
    }

    private boolean canStartTrial() {
        return menu.isOpenedFromStatue() && selectedGod != null && selectedGod == menu.getGod()
                && relationship().getBoolean("trial_unlocked") && GodlyTrialsRegistry.get(selectedGod) != null;
    }

    private void equip(int slot) {
        if (view != View.BLESSINGS) return;
        var selected = selected();
        if (selected == null || !slotButtons.get(slot).active || minecraft.player == null) return;
        ModMessages.sendToServer(new EquipBlessingMessage(menu.containerId, selectedGod.getRegistryName(), selectedBlessing, slot,
                !same(DivinityEngineHelper.getBlessingInSlot(minecraft.player, slot), selected)));
    }

    private BlessingsInstance selected() {
        return blessings.stream().filter(value -> value.getBlessing().getRegistryName().equals(selectedBlessing)).findFirst().orElse(null);
    }

    private static boolean same(BlessingsInstance a, BlessingsInstance b) {
        return a != null && b != null && a.getBoundGod() == b.getBoundGod() && a.getBlessing() == b.getBlessing();
    }

    private static int clamp(int value, int max) { return Math.max(0, Math.min(value, Math.max(0, max))); }
    private static Component name(BaseGod god) { return Component.translatable(god.getNameTranslationKey()); }
    private static net.minecraft.network.chat.MutableComponent tr(String key, Object... args) {
        return Component.translatable("gui.divinity_engine.blessings.tabs." + key, args);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (search != null) search.tick();
        refresh();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0 || mouseY < topPos + 30 || mouseY >= slotsY) return false;
        int direction = delta > 0 ? -1 : 1;
        if (mouseX >= tabsX && mouseX < rightX) {
            if (menu.isOpenedFromStatue()) return false;
            godOffset += direction;
        }
        else if (mouseX >= rightX && mouseX < rightX + rightWidth) {
            if (view != View.BLESSINGS) journalOffset += direction * 3;
            else if (mouseY < detailsY) blessingOffset += direction * 2;
            else descriptionOffset += direction;
        } else return false;
        refresh();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (search != null && hasControlDown() && key == GLFW.GLFW_KEY_F) { setFocused(search); return true; }
        if (search != null && search.isFocused() && key != GLFW.GLFW_KEY_TAB) {
            search.keyPressed(key, scanCode, modifiers);
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_3) { equip(key - GLFW.GLFW_KEY_1); return true; }
        if (key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            int direction = key == GLFW.GLFW_KEY_PAGE_UP ? -1 : 1;
            if (view != View.BLESSINGS) { journalOffset += direction * 6; refresh(); }
            else if (menu.isOpenedFromStatue() || blessingNavigationFocused()) pageBlessings(direction);
            else {
                godOffset += direction * godTabs.size();
                refresh();
            }
            return true;
        }
        if ((key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END) && blessingNavigationFocused()) {
            blessingOffset = key == GLFW.GLFW_KEY_HOME ? 0 : blessings.size();
            refresh();
            return true;
        }
        if (minecraft.options.keyInventory.matches(key, scanCode)) return true;
        return super.keyPressed(key, scanCode, modifiers);
    }

    private boolean blessingNavigationFocused() {
        return blessingButtons.contains(getFocused()) || getFocused() == previousBlessings || getFocused() == nextBlessings;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public Component getNarrationMessage() {
        return tr("title").append(". ").append(selectedGod == null ? tr("no_contact") : name(selectedGod))
                .append(". ").append(tr("view_" + view.name().toLowerCase(Locale.ROOT)));
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
    @Override protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {}
    @Override protected boolean checkHotbarKeyPressed(int key, int scanCode) { return false; }
    @Override public void removed() { /* No client-side capability writes in a read-only menu. */ }
    @Override public boolean isPauseScreen() { return false; }
}
