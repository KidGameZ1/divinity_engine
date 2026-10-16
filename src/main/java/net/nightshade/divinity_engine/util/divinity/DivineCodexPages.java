package net.nightshade.divinity_engine.util.divinity;

import net.minecraft.network.chat.Component;
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

        // Keep one full entry per registration; the reader paginates for its current layout.
        titles.add(title);
        contents.add(content);
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
