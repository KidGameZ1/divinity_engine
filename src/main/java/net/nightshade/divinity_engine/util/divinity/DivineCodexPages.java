package net.nightshade.divinity_engine.util.divinity;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record DivineCodexPages() {
    public static List<Component> titles = new ArrayList<>();
    public static List<Component> contents = new ArrayList<>();

    public void add(@Nullable Component title, @Nullable Component content) {
        if (title == null || content == null) {
            content = Component.literal("");
            title = Component.literal("");
        }

        titles.add(title);

        // Use a working variable to process overflow repeatedly
        Component workingContent = content;

        while (DivinityEngineHelper.doesTextOverflow(Minecraft.getInstance().font, workingContent, 118, 30, 140, 0.95f)) {
            titles.add(title); // Add title again for each line split

            DivinityEngineHelper.LineSplitResult result = DivinityEngineHelper.splitOverflowingLines(
                    Minecraft.getInstance().font, workingContent, 118, 30, 140, 0.95f
            );

            contents.add(result.visible());
            workingContent = result.overflow(); // Continue with overflow
        }

        // Add the last remaining visible content (not overflowing anymore)
        contents.add(workingContent);
    }

        public int sizeTitles() {
            return titles.size();
        }

        public int sizeContents() {
            return contents.size();
        }

        public Component getTitle(int index) {
            return titles.get(index);
        }

        public Component getContent(int index) {
            return contents.get(index);
        }

        public boolean contains(String title) {
            for(Component titleComponent : titles) {
                if(titleComponent.getString().equalsIgnoreCase(title)) {
                    return true;
                }
            }
            return false;
        }

        public int getMaxPages(){
            return contents.size() - 1;
        }
}