package net.nightshade.divinity_engine.client.innerworld;

import java.util.*;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.DivinityEngineMod;

/** Client-only addon sky marks. Register on the client setup work queue, never from common code. */
public final class InnerWorldPresentationRegistry {
    public record Frame(float ticks, int godColor, boolean reducedFlashing, boolean motion) {}
    public record Mark(Vec3 center, float size, int color, float alpha) {
        public Mark {
            Objects.requireNonNull(center);
            if (!Double.isFinite(center.lengthSqr()) || center.length() > 160 || center.length() < 16
                    || !Float.isFinite(size) || size <= 0 || size > 16
                    || !Float.isFinite(alpha) || alpha < 0 || alpha > 1)
                throw new IllegalArgumentException("Invalid Inner World sky mark");
        }
    }

    private static final Map<ResourceLocation, Function<Frame, List<Mark>>> PROVIDERS = new HashMap<>();
    private static final Set<ResourceLocation> FAILED = new HashSet<>();

    public static void register(ResourceLocation id, Function<Frame, List<Mark>> provider) {
        if (PROVIDERS.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(provider)) != null)
            throw new IllegalArgumentException("Duplicate Inner World presentation: " + id);
    }

    static List<Mark> marks(String motif, Frame frame) {
        ResourceLocation id = ResourceLocation.tryParse(motif);
        var provider = PROVIDERS.get(id);
        if (provider == null || FAILED.contains(id)) return List.of();
        try {
            var marks = List.copyOf(provider.apply(frame));
            if (marks.size() > 128) throw new IllegalArgumentException("Maximum 128 sky marks per frame");
            return marks;
        } catch (RuntimeException exception) {
            FAILED.add(id);
            DivinityEngineMod.LOGGER.error("{} sky provider failed; using the ordinary realm background", id, exception);
            return List.of();
        }
    }

    private InnerWorldPresentationRegistry() {}
}
