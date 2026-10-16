package net.nightshade.divinity_engine.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.api.content.ContentBehaviorContext;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import org.jetbrains.annotations.Nullable;

/** A synchronous server hook; markSuccessful participates in the ordinary blessing cooldown. */
public final class DivinityBlessingEventJS extends EventJS {
    private final ContentBehaviorContext context;
    private boolean open = true;

    DivinityBlessingEventJS(ContentBehaviorContext context) {
        this.context = context;
    }

    public ResourceLocation getId() {
        return context.getId();
    }

    public String getHook() {
        return context.getHook();
    }

    public LivingEntity getOwner() {
        return context.getOwner();
    }

    public @Nullable LivingEntity getTarget() {
        return context.getTarget();
    }

    public @Nullable BlessingsInstance getInstance() {
        return context.getInstance();
    }

    public ServerLevel getLevel() {
        return context.getLevel();
    }

    public @Nullable Object getNativeEvent() {
        return context.getNativeEvent();
    }

    public boolean isSuccessful() {
        return context.isSuccessful();
    }

    public void markSuccessful() {
        if (!context.getLevel().getServer().isSameThread() || !open) {
            throw new IllegalStateException("markSuccessful must run synchronously inside DivinityEvents.blessing");
        }
        context.markSuccessful();
    }

    void close() {
        open = false;
    }
}
