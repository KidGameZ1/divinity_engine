package net.nightshade.divinity_engine.api.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import org.jetbrains.annotations.Nullable;

/** A short-lived server callback. Success starts the ordinary blessing cooldown. */
public final class ContentBehaviorContext {
    private final ResourceLocation id;
    private final String hook;
    private final LivingEntity owner;
    private final @Nullable LivingEntity target;
    private final @Nullable BlessingsInstance instance;
    private final @Nullable Object nativeEvent;
    private boolean successful;

    public ContentBehaviorContext(ResourceLocation id, String hook, LivingEntity owner,
                                  @Nullable LivingEntity target, @Nullable BlessingsInstance instance,
                                  @Nullable Object nativeEvent) {
        if (!(owner.level() instanceof ServerLevel level) || !level.getServer().isSameThread())
            throw new IllegalStateException("Content behaviors must run on the server thread");
        this.id = id;
        this.hook = hook;
        this.owner = owner;
        this.target = target;
        this.instance = instance;
        this.nativeEvent = nativeEvent;
    }

    public ResourceLocation getId() { return id; }
    public String getHook() { return hook; }
    public LivingEntity getOwner() { return owner; }
    public @Nullable LivingEntity getTarget() { return target; }
    public @Nullable BlessingsInstance getInstance() { return instance; }
    public ServerLevel getLevel() { return (ServerLevel) owner.level(); }
    public @Nullable Object getNativeEvent() { return nativeEvent; }
    public void markSuccessful() { successful = true; }
    public boolean isSuccessful() { return successful; }
}
