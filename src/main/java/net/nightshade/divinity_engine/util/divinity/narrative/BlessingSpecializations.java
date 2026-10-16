package net.nightshade.divinity_engine.util.divinity.narrative;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Resolves earned specializations without changing saved blessing parameters. */
@Mod.EventBusSubscriber(modid = DivinityEngineMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlessingSpecializations {
    private static final Map<String, String> BLESSINGS = Map.ofEntries(
            Map.entry("solarius", "solar_ward"), Map.entry("nashara", "serpents_bite"),
            Map.entry("grond", "stoneform"), Map.entry("varun", "savage_hunger"),
            Map.entry("dravak", "battle_rush"), Map.entry("elaris", "foresight"),
            Map.entry("voltira", "stormstep"), Map.entry("vokar", "soul_leech"),
            Map.entry("nythea", "twilight_veil"), Map.entry("kairoth", "second_chance"),
            Map.entry("cryonis", "icy_pulse"), Map.entry("nerai", "aquatic_grace"),
            Map.entry("zephra", "gale_step"), Map.entry("terra", "thornskin"),
            Map.entry("lumen", "beacons_grace"), Map.entry("mechanos", "clockwork_punch"),
            Map.entry("ignar", "blazing_hands"), Map.entry("aethon", "celestial_ward"));
    public static final UUID TWILIGHT_SPEED = UUID.fromString("7e0e1e5e-8c0d-4f9a-b8a1-f9a9e9b9e9e9");
    public static final UUID AQUATIC_SPEED = UUID.fromString("05a697d2-64af-4ba5-81fd-63957012ed58");
    public static final UUID GALE_SPEED = UUID.fromString("227ee8f8-b8b2-462f-b54d-f51e4d544ece");
    private static final Map<LivingEntity, Map<MobEffect, EffectLease>> EFFECTS = new WeakHashMap<>();

    private BlessingSpecializations() {}

    public static boolean isActive(LivingEntity player, String godPath, String branch) {
        if (!(player instanceof ServerPlayer serverPlayer) || !("a".equals(branch) || "b".equals(branch))) return false;
        String blessingPath = BLESSINGS.get(godPath);
        if (blessingPath == null) return false;
        ResourceLocation godId = new ResourceLocation(DivinityEngineMod.MODID, godPath);
        return hasEligibleBlessing(player, godId, new ResourceLocation(DivinityEngineMod.MODID, blessingPath))
                && DivineNarrativeManager.hasSpecialization(serverPlayer, godId, branch);
    }

    public static boolean isEligible(LivingEntity player, BlessingsInstance instance) {
        if (player == null || player.level().isClientSide || instance == null || instance.getBoundGodInstance() == null) return false;
        for (BaseGodInstance god : GodHelper.getContactedGodsFrom(player).getContactedGods()) {
            if (god.getBaseGodId().equals(instance.getBoundGodInstance().getBaseGodId())) {
                return net.nightshade.divinity_engine.util.divinity.gods.DivineRelationship.canUse(player, instance)
                        && DivinityEngineHelper.hasBlessingInSlot(instance.getBlessing(), player);
            }
        }
        return false;
    }

    public static boolean hasEligibleBlessing(LivingEntity player, ResourceLocation godId, ResourceLocation blessingId) {
        for (int slot = 0; slot < 3; slot++) {
            BlessingsInstance instance = switch (slot) {
                case 0 -> DivinityEngineHelper.getBlessingSlot1(player);
                case 1 -> DivinityEngineHelper.getBlessingSlot2(player);
                default -> DivinityEngineHelper.getBlessingSlot3(player);
            };
            if (instance != null && instance.getBlessingId().equals(blessingId)
                    && instance.getBoundGodInstance() != null
                    && godId.equals(instance.getBoundGodInstance().getBaseGodId()) && isEligible(player, instance)) return true;
        }
        return false;
    }

    /** Call only when starting a cooldown, never while decrementing or deserializing it. */
    public static int cooldownTicks(LivingEntity player, BlessingsInstance instance) {
        if (!DivinityEngineMod.MODID.equals(instance.getBlessingId().getNamespace())) return instance.getBlessing().getCooldown();
        return switch (instance.getBlessingId().getPath()) {
            case "stoneform" -> isActive(player, "grond", "b") ? 20 * 20 : instance.getBlessing().getCooldown();
            case "foresight" -> isActive(player, "elaris", "b") ? 16 * 20 : instance.getBlessing().getCooldown();
            case "celestial_ward" -> (isActive(player, "aethon", "a") ? 24 : 30) * 20;
            default -> instance.getBlessing().getCooldown();
        };
    }

    public static void modifier(LivingEntity player, Attribute attribute, UUID id, String name, double amount) {
        AttributeInstance value = player.getAttribute(attribute);
        if (value == null) return;
        AttributeModifier old = value.getModifier(id);
        if (old != null && old.getAmount() == amount && amount != 0) return;
        if (old != null) value.removeModifier(id);
        if (amount != 0) value.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    /** Do not overwrite a potion supplied by another source; lease only our own refreshes. */
    public static void effect(LivingEntity player, MobEffect effect, boolean active) {
        if (player.level().isClientSide) return;
        Map<MobEffect, EffectLease> leases = EFFECTS.get(player);
        if (leases == null) {
            if (!active) return;
            leases = new HashMap<>();
            EFFECTS.put(player, leases);
        }
        EffectLease lease = leases.get(effect);
        MobEffectInstance current = player.getEffect(effect);
        boolean owned = lease != null && lease.matches(current, player.tickCount);
        if (!active) {
            if (owned) player.removeEffect(effect);
            leases.remove(effect);
            if (leases.isEmpty()) EFFECTS.remove(player);
            return;
        }
        if (current != null && !owned) {
            leases.remove(effect);
            if (leases.isEmpty()) EFFECTS.remove(player);
            return;
        }
        if (owned && current.getDuration() > 220) return;
        player.addEffect(new MobEffectInstance(effect, 300, 0, true, false, true));
        current = player.getEffect(effect);
        if (current != null && current.getAmplifier() == 0 && current.getDuration() == 300) {
            leases.put(effect, new EffectLease(current, player.tickCount + 300));
        }
    }

    private record EffectLease(MobEffectInstance instance, int expires) {
        boolean matches(MobEffectInstance current, int tick) {
            return current == instance && current != null && current.getAmplifier() == 0
                    && Math.abs(current.getDuration() - (expires - tick)) <= 1;
        }
    }

    public static boolean isBright(LivingEntity player, int threshold) {
        return player.level().getBrightness(LightLayer.BLOCK, player.blockPosition()) >= threshold
                || player.level().isDay() && player.level().canSeeSkyFromBelowWater(player.blockPosition());
    }

    public static void clearTwilightInvisibility(LivingEntity player) {
        if (!player.getPersistentData().getBoolean("divinity_engine.twilight_invisible")) return;
        if (!player.hasEffect(MobEffects.INVISIBILITY)) player.setInvisible(false);
        player.getPersistentData().remove("divinity_engine.twilight_invisible");
    }

    // Runs even after a slot is removed, when its blessing can no longer receive onTick.
    @SubscribeEvent
    public static void cleanupIneligible(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        boolean twilight = hasEligibleBlessing(player, new ResourceLocation(DivinityEngineMod.MODID, "nythea"),
                new ResourceLocation(DivinityEngineMod.MODID, "twilight_veil")) && !player.level().isDay();
        if (!twilight) {
            modifier(player, Attributes.MOVEMENT_SPEED, TWILIGHT_SPEED, "Twilight Speed", 0);
            clearTwilightInvisibility(player);
        }
        if (!isActive(player, "nerai", "b") || !player.isInWaterOrBubble())
            modifier(player, ForgeMod.SWIM_SPEED.get(), AQUATIC_SPEED, "Tidal protection", 0);
        if (!isActive(player, "zephra", "a")) modifier(player, Attributes.MOVEMENT_SPEED, GALE_SPEED, "Unbound journey", 0);
        if (!twilight || !isActive(player, "nythea", "a")) effect(player, MobEffects.NIGHT_VISION, false);
        if (!isActive(player, "lumen", "b") || !isBright(player, 12)) effect(player, MobEffects.DAMAGE_RESISTANCE, false);
        if (!isActive(player, "nerai", "a") || !player.isEyeInFluid(FluidTags.WATER))
            player.getPersistentData().remove("divinity_engine.aquatic_heal_at");
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LivingEntity player = event.getEntity();
        if (player.level().isClientSide) return;
        effect(player, MobEffects.NIGHT_VISION, false);
        effect(player, MobEffects.DAMAGE_RESISTANCE, false);
        modifier(player, Attributes.MOVEMENT_SPEED, TWILIGHT_SPEED, "Twilight Speed", 0);
        modifier(player, Attributes.MOVEMENT_SPEED, GALE_SPEED, "Unbound journey", 0);
        modifier(player, ForgeMod.SWIM_SPEED.get(), AQUATIC_SPEED, "Tidal protection", 0);
        player.getPersistentData().remove("divinity_engine.aquatic_heal_at");
        clearTwilightInvisibility(player);
    }
}
