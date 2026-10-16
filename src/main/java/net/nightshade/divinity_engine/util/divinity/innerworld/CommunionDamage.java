package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import java.util.*;

/** Scope an AoE to one hit per real player even if a sweep discovers the returned player. */
public final class CommunionDamage {
    private record Hit(long tick,Object scope,DamageSource source,UUID projectile){}
    private static final ThreadLocal<Deque<Object>> SCOPES=ThreadLocal.withInitial(ArrayDeque::new);
    private static final Map<UUID,Hit> HITS=new HashMap<>();
    public static void push(){SCOPES.get().push(new Object());}
    public static void pop(){if(!SCOPES.get().isEmpty())SCOPES.get().pop();}
    public static void record(ServerPlayer p,DamageSource s){HITS.put(p.getUUID(),new Hit(p.server.overworld().getGameTime(),SCOPES.get().peek(),s,s.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile?s.getDirectEntity().getUUID():null));}
    public static boolean duplicate(ServerPlayer p,DamageSource s){
        Hit hit=HITS.get(p.getUUID());if(hit==null)return false;
        if(hit.tick!=p.server.overworld().getGameTime()){HITS.remove(p.getUUID());return false;}
        return hit.source==s||hit.scope!=null&&hit.scope==SCOPES.get().peek()||hit.projectile!=null&&s.getDirectEntity()!=null&&hit.projectile.equals(s.getDirectEntity().getUUID());
    }
    public static void clear(){HITS.clear();SCOPES.remove();}
    public static void prune(long tick){HITS.values().removeIf(h->h.tick<tick);SCOPES.remove();}
    private CommunionDamage(){}
}
