package net.nightshade.divinity_engine.api.innerworld;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/** Common-side realm contract. All positions are LOCAL; allocation belongs to the server. */
public final class InnerWorldDefinition {
    public enum Moment { REQUEST, ENTER, INTERACT, STORY, INTERRUPT, RETURN, CLEANUP }
    public record Context(ServerPlayer player, UUID session, ResourceLocation god, String variant, BlockPos origin) {}
    @FunctionalInterface public interface Hook { void accept(Context context, Moment moment); }
    public record Visuals(int color, ResourceLocation sound, ResourceLocation particle, String motif) {
        public Visuals { Objects.requireNonNull(sound); Objects.requireNonNull(particle); Objects.requireNonNull(motif); }
    }
    public record Variant(String id, Predicate<ServerPlayer> condition, Visuals visuals) {
        public Variant { Objects.requireNonNull(id); Objects.requireNonNull(condition); Objects.requireNonNull(visuals); }
    }
    public final ResourceLocation id, god, template;
    public final String name, requirementText, revision;
    public final int width, height, depth;
    public final Vec3 spawn, manifestation;
    public final Visuals visuals;
    public final List<Variant> variants;
    public final Predicate<ServerPlayer> requirement;
    public final Function<BlockPos, BlockState> scene;
    public final Hook hook;
    public final Set<ResourceLocation> compatibleStories;

    private InnerWorldDefinition(Builder b) {
        id = Objects.requireNonNull(b.id); god = Objects.requireNonNull(b.god); name = b.name; revision=b.revision;
        width=b.width; height=b.height; depth=b.depth; spawn=b.spawn; manifestation=b.manifestation;
        template=b.template; visuals=b.visuals; variants=List.copyOf(b.variants); requirement=b.requirement;
        requirementText=b.requirementText; scene=b.scene; hook=b.hook; compatibleStories=Set.copyOf(b.stories);
        if (revision.isBlank() || revision.length()>64 || name.isBlank() || name.length()>128 || width<9 || depth<9 || height<6 || width>48 || depth>48 || height>32
                || !inside(spawn) || !inside(manifestation)) throw new IllegalArgumentException(id+": invalid realm name, bounds or local spawn/manifestation");
        Set<String> ids=new HashSet<>();
        for (Variant v:variants) if (!ids.add(v.id()) || v.id().length()>64) throw new IllegalArgumentException(id+": duplicate/invalid variant "+v.id());
    }
    private boolean inside(Vec3 v) { return v!=null && Double.isFinite(v.lengthSqr()) && v.x>=1 && v.z>=1 && v.y>=1 && v.x<width-1 && v.z<depth-1 && v.y<height-2; }
    public Variant resolve(ServerPlayer player) { for (Variant v:variants) if(v.condition.test(player)) return v; return new Variant("default",p->true,visuals); }
    public static Builder builder(ResourceLocation id, ResourceLocation god) { return new Builder(id,god); }
    public static final class Builder {
        private final ResourceLocation id,god;
        private String name="Inner World",requirementText="Contact this god at a physical statue.",revision="1";
        private int width=25,height=16,depth=25;
        private Vec3 spawn=new Vec3(12.5,1,5.5),manifestation=new Vec3(12.5,1,19.5);
        private ResourceLocation template;
        private Visuals visuals=new Visuals(0xAACCEE,new ResourceLocation("minecraft","block.amethyst_block.chime"),new ResourceLocation("minecraft","end_rod"),"stars");
        private final List<Variant> variants=new ArrayList<>();
        private Predicate<ServerPlayer> requirement=p->true;
        private Function<BlockPos,BlockState> scene=p->p.getY()==0?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState();
        private Hook hook=(c,m)->{};
        private final Set<ResourceLocation> stories=new HashSet<>();
        private Builder(ResourceLocation id,ResourceLocation god){this.id=id;this.god=god;}
        public Builder name(String value){name=value;return this;}
        public Builder revision(String value){revision=Objects.requireNonNull(value);return this;}
        public Builder bounds(int x,int y,int z){width=x;height=y;depth=z;return this;}
        public Builder spawn(Vec3 value){spawn=value;return this;}
        public Builder manifestation(Vec3 value){manifestation=value;return this;}
        public Builder template(ResourceLocation value){template=value;return this;}
        public Builder scene(Function<BlockPos,BlockState> value){scene=Objects.requireNonNull(value);return this;}
        public Builder visuals(Visuals value){visuals=Objects.requireNonNull(value);return this;}
        public Builder variant(Variant value){variants.add(value);return this;}
        public Builder requires(String explanation,Predicate<ServerPlayer> value){requirementText=explanation;requirement=value;return this;}
        public Builder hook(Hook value){hook=Objects.requireNonNull(value);return this;}
        public Builder story(ResourceLocation value){stories.add(value);return this;}
        public InnerWorldDefinition build(){return new InnerWorldDefinition(this);}
    }
}
