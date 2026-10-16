package net.nightshade.divinity_engine.util.divinity.innerworld;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CommunionConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue WINDUP=B.defineInRange("windupTicks",60,20,1200);
    public static final ForgeConfigSpec.IntValue COOLDOWN=B.defineInRange("cooldownTicks",200,0,72000);
    public static final ForgeConfigSpec.IntValue DURATION=B.defineInRange("maximumSessionTicks",6000,200,72000);
    public static final ForgeConfigSpec.IntValue CAPACITY=B.defineInRange("concurrentSessions",8,1,32);
    public static final ForgeConfigSpec.IntValue BLOCK_BUDGET=B.defineInRange("blockOperationsPerTick",256,16,512);
    public static final ForgeConfigSpec.IntValue MAX_SIDE=B.defineInRange("maximumInstanceSide",48,25,48);
    public static final ForgeConfigSpec.IntValue SOURCE_CHUNKS=B.defineInRange("sourceChunkLimitPerSession",9,1,9);
    public static final ForgeConfigSpec.IntValue TARGET_RADIUS=B.defineInRange("hostileTargetRadius",16,4,24);
    public static final ForgeConfigSpec.BooleanValue DIAGNOSTICS=B.define("diagnostics",false);
    public static final ForgeConfigSpec SERVER=B.build();
    private static final ForgeConfigSpec.Builder C=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue PARTICLES=C.define("reducedParticles",false);
    public static final ForgeConfigSpec.BooleanValue FLASHING=C.define("reducedFlashing",true);
    public static final ForgeConfigSpec.BooleanValue TRANSITIONS=C.define("screenTransitions",false);
    public static final ForgeConfigSpec.BooleanValue MOTION=C.define("motionEffects",false);
    public static final ForgeConfigSpec.DoubleValue VOLUME=C.defineInRange("ambientVolume",0.3,0,1);
    public static final ForgeConfigSpec CLIENT=C.build();
    private CommunionConfig(){}
}
