package net.nightshade.divinity_engine.api.narrative;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

/** Permanent reward identity, separate from the original blessing's mutable parameters. */
public record BlessingSpecialization(ResourceLocation id, ResourceLocation blessingId, String description) {
    public BlessingSpecialization {
        Objects.requireNonNull(id); Objects.requireNonNull(blessingId); Objects.requireNonNull(description);
    }
    public static Builder builder(ResourceLocation id) { return new Builder(id); }
    public static final class Builder {
        private final ResourceLocation id;
        private ResourceLocation blessing;
        private String description = "";
        private Builder(ResourceLocation id) { this.id = id; }
        public Builder blessing(ResourceLocation value) { blessing = value; return this; }
        public Builder description(String value) { description = value; return this; }
        public BlessingSpecialization build() { return new BlessingSpecialization(id, blessing, description); }
    }
}
