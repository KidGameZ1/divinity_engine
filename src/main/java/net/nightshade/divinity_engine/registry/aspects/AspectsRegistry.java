package net.nightshade.divinity_engine.registry.aspects;

import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.aspects.armor.*;
import net.nightshade.divinity_engine.aspects.tool.BulwarkAspect;
import net.nightshade.divinity_engine.aspects.weapon.*;
import net.nightshade.vanguards_kit.api.battle_aspects.BattleAspectDeferredRegister;
import net.nightshade.vanguards_kit.api.battle_aspects.BattleAspectRegistryObject;
import net.nightshade.vanguards_kit.core.battle_aspects.BattleAspect;
import net.nightshade.vanguards_kit.registry.battle_aspects.BattleAspectsRegistry;


public class AspectsRegistry {
    public static final BattleAspectDeferredRegister BATTLE_ASPECTS_REGISTER = new BattleAspectDeferredRegister(DivinityEngineMod.MODID);

    public static final BattleAspectRegistryObject<BattleAspect> REND =
            BATTLE_ASPECTS_REGISTER.register(() -> new RendAspect("rend"));
    public static final BattleAspectRegistryObject<BattleAspect> MOMENTUM =
            BATTLE_ASPECTS_REGISTER.register(() -> new MomentumAspect("momentum"));
    public static final BattleAspectRegistryObject<BattleAspect> SUNDERER =
            BATTLE_ASPECTS_REGISTER.register(() -> new SundererAspect("sunderer"));
    public static final BattleAspectRegistryObject<BattleAspect> OVERWHELM =
            BATTLE_ASPECTS_REGISTER.register(() -> new OverwhelmAspect("overwhelm"));
    public static final BattleAspectRegistryObject<BattleAspect> BULWARK =
            BATTLE_ASPECTS_REGISTER.register(() -> new BulwarkAspect("bulwark"));
    public static final BattleAspectRegistryObject<BattleAspect> SECOND_SKIN =
            BATTLE_ASPECTS_REGISTER.register(() -> new SecondSkinAspect("second_skin"));
    public static final BattleAspectRegistryObject<BattleAspect> STOIC_VIGOR =
            BATTLE_ASPECTS_REGISTER.register(() -> new StoicVigorAspect("stoic_vigor"));
    public static final BattleAspectRegistryObject<BattleAspect> ENDURING_SPARK =
            BATTLE_ASPECTS_REGISTER.register(() -> new EnduringSparkAspect("enduring_spark"));
    public static final BattleAspectRegistryObject<BattleAspect> VENOMBRAND =
            BATTLE_ASPECTS_REGISTER.register(() -> new VenombrandAspect("venombrand"));
    public static final BattleAspectRegistryObject<BattleAspect> SOULFLARE =
            BATTLE_ASPECTS_REGISTER.register(() -> new SoulflareAspect("soulflare"));
    public static final BattleAspectRegistryObject<BattleAspect> FROSTBITE =
            BATTLE_ASPECTS_REGISTER.register(() -> new FrostbiteAspect("frostbite"));
    public static final BattleAspectRegistryObject<BattleAspect> THUNDERCLASH =
            BATTLE_ASPECTS_REGISTER.register(() -> new ThunderclashAspect("thunderclash"));


    public static void init() {
        BattleAspectsRegistry.registerDeferred(BATTLE_ASPECTS_REGISTER);
    }
}
