package net.nightshade.divinity_engine.util.content;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.nightshade.divinity_engine.divinity.trial.GodlyTrial;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** The normal session engine owns transport, terminal states, protection and cleanup. */
public final class ContentTrial extends GodlyTrial {
    private final ContentSchema.Trial definition;
    private final Set<UUID> remaining = new HashSet<>();
    private int killed;

    public ContentTrial(ServerPlayer player, ContentSchema.Trial definition) { super(player); this.definition = definition; }

    @Override protected void onStart() { player.sendSystemMessage(Component.literal(definition.start())); }
    @Override protected void runPhase(int phase) {
        // Surviving a phase must not leave enemies contributing to the next objective.
        for (UUID id : remaining) {
            var entity = trialLevel.getEntity(id);
            if (entity != null) entity.discard();
        }
        remaining.clear();
        killed = 0;
        var current = definition.phases().get(phase - 1);
        phaseDuration = current.ticks();
        for (var spawn : current.spawns()) for (int i = 0; i < spawn.count(); i++) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(spawn.entity());
            var entity = type.spawn(trialLevel, getArenaCenter().offset(player.getRandom().nextInt(7) - 3, 1,
                    player.getRandom().nextInt(7) - 3), MobSpawnType.MOB_SUMMONED);
            if (!(entity instanceof Mob mob)) {
                if (entity != null) entity.discard();
                failTrial();
                return;
            }
            mob.setPersistenceRequired();
            mob.setTarget(player);
            remaining.add(mob.getUUID());
            trackEntity(mob);
        }
        setPhaseInfo(current.name(), current.objective(), 0,
                current.goal().equals("survive") ? current.ticks() : remaining.size());
    }

    @Override public void tick() {
        var current = definition.phases().get(phase - 1);
        if (current.goal().equals("survive")) {
            if (getTicksElapsed() % 20 == 0) setObjectiveProgress(getTicksElapsed());
            if (getTicksElapsed() + 1 >= current.ticks()) nextPhase();
        } else if (remaining.isEmpty()) nextPhase();
    }

    @Override public void onKillEntity(Player source, LivingDeathEvent event) {
        if (source.getUUID().equals(player.getUUID()) && remaining.remove(event.getEntity().getUUID())) {
            killed++;
            if (!definition.phases().get(phase - 1).goal().equals("survive")) setObjectiveProgress(killed);
        }
    }
    @Override protected void onComplete() {
        GodHelper.increaseFavor(GodsRegistry.GODS_REGISTRY.get().getValue(definition.god()), player, definition.favor());
        player.sendSystemMessage(Component.literal(definition.complete()));
    }
    @Override protected int getMaxPhases() { return definition.phases().size(); }
}
