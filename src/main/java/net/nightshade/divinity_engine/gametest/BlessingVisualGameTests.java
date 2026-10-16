package net.nightshade.divinity_engine.gametest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.messages.effects.PrivateBlessingEffectsPacket;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingEffectBudget;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisualDesign;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisualDesigns;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisualProfiles;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.Cue;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisuals.VisualRecipe;
import net.nightshade.divinity_engine.util.divinity.narrative.CorePresenceEffects;
import net.nightshade.divinity_engine.util.divinity.narrative.DivineNarrativeRegistry;
import net.nightshade.renderlib.api.RenderAnchor;
import net.nightshade.renderlib.api.RenderEffectSpec;
import net.nightshade.renderlib.api.RenderEffectInstance;
import net.nightshade.renderlib.api.RenderLibRegistries;
import net.nightshade.renderlib.api.RenderVisibility;
import net.nightshade.renderlib.particle.ParticleBloomMode;
import net.nightshade.renderlib.particle.ParticleCollision;
import net.nightshade.renderlib.particle.ParticleEmitterSpec;
import net.nightshade.renderlib.particle.ParticleRegistries;
import net.nightshade.renderlib.server.RenderEffectSavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Effects-only checks; lifecycle tests create inert owners, never invoke gameplay callbacks. */
@Mod.EventBusSubscriber(modid = "divinity_engine", bus = Mod.EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public final class BlessingVisualGameTests {
    private static final String MODID = "divinity_engine";
    private static final String BATCH = "divinity_effects";
    private static final String TEMPLATE = "pocket_dimension";
    private static final RenderAnchor ANCHOR = RenderAnchor.world(new Vec3(3, 8, -4));
    private static final Vec3 FORWARD = new Vec3(2, 3, -6);

    private BlessingVisualGameTests() { }

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void registerEffectsTests(RegisterGameTestsEvent event) {
        boolean enabled = Arrays.stream(System.getProperty("forge.enabledGameTestNamespaces", "").split(","))
                .map(String::trim).anyMatch(BATCH::equals);
        if (!enabled) return;
        // Forge 47 filters the template namespace, not the suite namespace. Opt in only this class,
        // then use its supported templateNamespace annotation to reuse the existing structure.
        for (var method : BlessingVisualGameTests.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GameTest.class)) GameTestRegistry.register(method, Set.of(MODID));
        }
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_registry_bindings(GameTestHelper h) {
        Map<ResourceLocation, ResourceLocation> bindings = actualBindings(h);
        var styles = BlessingVisualProfiles.styles();
        var themes = BlessingVisualProfiles.themes();
        h.assertTrue(builtinKeys(styles.keySet()).equals(bindings.keySet()),
                "Built-in visual styles must exactly match all 90 actual registry bindings");
        h.assertTrue(builtinKeys(themes.keySet()).equals(new HashSet<>(bindings.values())),
                "Built-in themes must exactly match the 18 actual gods");

        Map<ResourceLocation, Set<Integer>> variants = new HashMap<>();
        for (var entry : bindings.entrySet()) {
            var style = styles.get(entry.getKey());
            h.assertTrue(style != null, "Missing style for " + entry.getKey());
            h.assertTrue(style.god().equals(entry.getValue()), "Wrong god on style " + entry.getKey());
            h.assertTrue(style.variant() >= 0 && style.variant() <= 4, "Out-of-range variant on " + entry.getKey());
            h.assertTrue(variants.computeIfAbsent(entry.getValue(), ignored -> new HashSet<>()).add(style.variant()),
                    "Repeated variant for " + entry.getValue());
        }
        for (ResourceLocation god : variants.keySet()) {
            h.assertTrue(variants.get(god).equals(Set.of(0, 1, 2, 3, 4)), "Missing visual variant for " + god);
            var theme = themes.get(god);
            h.assertTrue(theme != null && !theme.motif().isBlank(), "Missing theme motif for " + god);
            h.assertTrue(theme.texture() != null, "Missing particle texture for " + god);
            h.assertTrue((theme.primary() >>> 24) > 0 && (theme.accent() >>> 24) > 0
                    && (theme.highlight() >>> 24) > 0, "Invisible theme palette for " + god);
            h.assertTrue(theme.primary() != theme.accent() && theme.primary() != theme.highlight(),
                    "Theme lacks primary/accent/highlight contrast for " + god);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_common_registration(GameTestHelper h) {
        h.assertTrue(RenderLibRegistries.effect(BlessingVisuals.TYPE).isPresent(),
                "Common setup must register the manifestation effect before tests run");
        h.assertTrue(RenderLibRegistries.primitive(BlessingVisuals.TYPE).isPresent(),
                "Common setup must register the manifestation primitive without client initialization");
        h.assertTrue(ParticleRegistries.material(BlessingVisuals.PIXEL_MATERIAL).isPresent(),
                "Common setup must register the pixel particle material without client initialization");
        h.assertTrue(RenderLibRegistries.material(BlessingVisuals.PIXEL_MATERIAL).isPresent(),
                "Common setup must also register the pixel material for particle effect carriers");
        var effects = RenderLibRegistries.effectIds();
        var primitives = RenderLibRegistries.primitiveIds();
        var materials = ParticleRegistries.materialIds();
        var effectMaterials = RenderLibRegistries.materialIds();
        BlessingVisuals.register();
        BlessingVisuals.register();
        h.assertTrue(effects.equals(RenderLibRegistries.effectIds()), "Repeated registration changed effect IDs");
        h.assertTrue(primitives.equals(RenderLibRegistries.primitiveIds()), "Repeated registration changed primitive IDs");
        h.assertTrue(materials.equals(ParticleRegistries.materialIds()), "Repeated registration changed particle material IDs");
        h.assertTrue(effectMaterials.equals(RenderLibRegistries.materialIds()), "Repeated registration changed effect material IDs");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_all_900_recipes_and_codecs(GameTestHelper h) {
        int checked = 0;
        for (var binding : actualBindings(h).entrySet()) {
            ResourceLocation blessing = binding.getKey();
            ResourceLocation god = binding.getValue();
            var theme = BlessingVisualProfiles.themes().get(god);
            var style = BlessingVisualProfiles.styles().get(blessing);
            for (Cue cue : Cue.values()) {
                String context = blessing + "/" + cue;
                VisualRecipe recipe = BlessingVisuals.recipe(god, blessing, cue, ANCHOR, FORWARD, null, 4);
                var visual = recipe.visual();
                var particles = recipe.particles();
                CompoundTag data = visual.parameters();
                h.assertTrue(god.toString().equals(data.getString("GodId")), context + ": lost actual god ID");
                h.assertTrue(blessing.toString().equals(data.getString("BlessingId")), context + ": lost blessing ID");
                h.assertTrue(cue.name().equals(data.getString("Cue")), context + ": wrong cue");
                h.assertTrue(theme.motif().equals(data.getString("God")), context + ": wrong motif");
                h.assertTrue(data.getInt("Variant") == style.variant(), context + ": wrong variant");
                h.assertTrue(data.getInt("Accent") == theme.accent() && data.getInt("Highlight") == theme.highlight(),
                        context + ": wrong theme detail colors");
                List<Integer> palette = List.of(theme.primary(), theme.accent(), theme.highlight());
                h.assertTrue(visual.palette().colors().equals(palette), context + ": wrong visual palette");
                h.assertTrue(particles.palette().colors().equals(BlessingParticleExpectations.palette(blessing, cue, palette)),
                        context + ": wrong god palette or material color");
                h.assertTrue(particles.appearance().texture().orElseThrow().getNamespace().equals("minecraft"),
                        context + ": expected an authored vanilla particle sprite");
                h.assertTrue(visual.anchor().equals(ANCHOR) && particles.anchor().equals(ANCHOR),
                        context + ": unlinked recipe changed the requested anchor");
                close(h, direction(data).length(), 1, context + ": forward must be normalized");
                h.assertFalse(data.contains("LinkX") || data.contains("LinkY") || data.contains("LinkZ"),
                        context + ": null link must not manufacture a segment");
                bounds(h, recipe, context);
                roundTrip(h, RenderEffectSpec.CODEC, visual, context + " visual");
                roundTrip(h, ParticleEmitterSpec.CODEC, particles, context + " particles");
                RenderEffectSpec carrier = roundTrip(h, RenderEffectSpec.CODEC, particles.toEffectSpec(),
                        context + " particle carrier");
                h.assertTrue(ParticleEmitterSpec.fromEffect(carrier).orElseThrow().equals(particles),
                        context + ": particle carrier lost emitter data");
                checked++;
            }
        }
        h.assertTrue(Cue.values().length == 10 && checked == 900, "Expected all 90 bindings times all 10 cues");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_shrine_particles(GameTestHelper h) {
        var gods = new HashSet<>(actualBindings(h).values());
        for (var god : gods) {
            var presence = DivineNarrativeRegistry.presence(god);
            h.assertTrue(presence != null, "Missing shrine presentation for " + god);
            var particles = CorePresenceEffects.recipe(BlockPos.ZERO, presence);
            var emission = particles.emission();
            h.assertTrue(emission.ratePerTick() == 0 && emission.maximumAlive() == 6
                    && emission.bursts().size() == 1 && emission.bursts().get(0).count() == 6,
                    "Shrine must emit one small burst, not a continuing halo: " + god);
            h.assertTrue(particles.durationTicks() == 32 && emission.maximumLifetime() == 30,
                    "Shrine response must expire promptly: " + god);
            h.assertTrue(particles.appearance().bloom() == ParticleBloomMode.OFF
                    && particles.appearance().glowStrength() == 0, "Shrine must not glow/bloom: " + god);
            h.assertTrue(particles.appearance().texture().orElseThrow().equals(presence.texture())
                    && presence.texture().getNamespace().equals("minecraft"), "Shrine must retain its vanilla god sprite: " + god);
            ParticleRegistries.validate(particles);
            roundTrip(h, ParticleEmitterSpec.CODEC, particles, "Shrine particle recipe " + god);
        }
        h.assertTrue(gods.size() == 18, "Expected all 18 shrine presentations");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_per_blessing_designs(GameTestHelper h) {
        var bindings = actualBindings(h);
        h.assertTrue(builtinKeys(BlessingVisualDesigns.all().keySet()).equals(bindings.keySet()),
                "Every blessing must have its own explicit design, with no god fallback");
        Set<BlessingVisualDesign> unique = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        int particlesOnly = 0, meshOnly = 0, combined = 0;
        for (var binding : bindings.entrySet()) {
            var design = BlessingVisualDesigns.get(binding.getKey());
            h.assertTrue(unique.add(design), binding.getKey() + ": reused another blessing's finished design");
            if (!design.mesh()) particlesOnly++;
            else if (!design.particles()) meshOnly++;
            else combined++;
            for (Cue cue : Cue.values()) {
                for (Vec3 link : new Vec3[]{null, new Vec3(8, -6, 4)}) {
                    VisualRecipe recipe = BlessingVisuals.recipe(binding.getValue(), binding.getKey(), cue,
                            ANCHOR, FORWARD, link, 4);
                    h.assertTrue(recipe.renderMesh() == design.mesh() && recipe.emitParticles() == design.particles(),
                            binding.getKey() + ": recipe ignored authored channels");
                    h.assertTrue(recipe.handleCount() == (design.mesh() ? 1 : 0) + (design.particles() ? 1 : 0),
                            binding.getKey() + ": incorrect handle accounting");
                    bounds(h, recipe, binding.getKey() + "/" + cue);
                    var shape = recipe.particles().emission();
                    if (shape.shape().equals(coreId("line")) && shape.shapeData().getDouble("length") > 1) {
                        h.assertTrue(shape.shapeData().getDouble("radius") >= shape.shapeData().getDouble("length"),
                                binding.getKey() + ": line culling bounds do not enclose the authored path");
                    }
                    roundTrip(h, ParticleEmitterSpec.CODEC, recipe.particles(), "Authored particle recipe " + binding.getKey() + "/" + cue);
                }
            }
        }
        h.assertTrue(particlesOnly == 90 && meshOnly == 0 && combined == 0,
                "All built-in blessing cues must be vanilla-sprite particles, not combat meshes");
        rejects(h, () -> BlessingVisualDesign.builder().build(), "Empty design accepted");
        rejects(h, () -> BlessingVisualDesign.builder().mesh().duration(33).build(), "Unbounded duration accepted");
        rejects(h, () -> BlessingVisualDesigns.register(bindings.keySet().iterator().next(),
                BlessingVisualDesign.builder().mesh().build()), "Duplicate design accepted");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_no_recolored_templates(GameTestHelper h) {
        Map<List<Object>, ResourceLocation> presentations = new HashMap<>();
        for (var binding : actualBindings(h).entrySet()) {
            List<Object> presentation = new java.util.ArrayList<>();
            for (Cue cue : Cue.values()) {
                for (Vec3 link : new Vec3[]{null, new Vec3(3, 1, -2)}) {
                    var particles = BlessingVisuals.recipe(binding.getValue(), binding.getKey(), cue,
                            ANCHOR, FORWARD, link, 3).particles();
                    // Ignore identity, palette, seed, count and duration: those alone are not a new effect.
                    presentation.add(List.of(particles.emission().shape(), particles.emission().shapeData(),
                            particles.motion(), particles.appearance().texture(), particles.appearance().size(),
                            particles.appearance().alpha(), particles.behaviors(), particles.collision()));
                }
            }
            ResourceLocation duplicate = presentations.putIfAbsent(List.copyOf(presentation), binding.getKey());
            h.assertTrue(duplicate == null,
                    binding.getKey() + " reuses " + duplicate + " with only cosmetic identifiers/counts changed");
        }
        h.assertTrue(presentations.size() == 90, "Expected 90 independently authored particle presentations");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_transfers_and_impacts_follow_mechanics(GameTestHelper h) {
        Map<String, Cue> transfers = Map.of(
                "soul_leech", Cue.HEAL, "dread_consumption", Cue.HEAL,
                "charge_relay", Cue.RECALL, "undercurrent_grip", Cue.CONTROL,
                "tempest_spiral", Cue.IMPACT, "mind_mirror", Cue.IMPACT,
                "starlit_mirror", Cue.WARD);
        var bindings = actualBindings(h);
        for (var transfer : transfers.entrySet()) {
            for (Vec3 delta : List.of(new Vec3(4, 1, -2), new Vec3(-4, -1, 2))) {
                var particles = BlessingVisuals.recipe(bindings.get(id(transfer.getKey())), id(transfer.getKey()),
                        transfer.getValue(), ANCHOR, FORWARD, delta, 1).particles();
                h.assertTrue(particles.motion().velocity().dot(delta) > 0,
                        transfer.getKey() + ": particles must move from the supplied source toward the recipient");
                vector(h, particles.anchor().position(), ANCHOR.position().subtract(delta.scale(0.5)),
                        transfer.getKey() + ": transfer begins at donor/contact, not a generic owner aura");
                h.assertFalse(particles.collision().authoritative(), "Transfer particles cannot affect gameplay");
            }
        }
        for (String dash : List.of("stormstep", "wavebound", "shearwind_dash", "phase_blink")) {
            Vec3 movement = new Vec3(-3, 0, 2);
            Cue cue = dash.equals("phase_blink") ? Cue.TELEPORT : Cue.TRAIL;
            var particles = BlessingVisuals.recipe(bindings.get(id(dash)), id(dash), cue,
                    ANCHOR, FORWARD, movement, 1).particles();
            h.assertTrue(particles.motion().velocity().dot(movement) < 0,
                    dash + ": wake must drift behind the actual travel vector, not the player's look direction");
        }
        Vec3 incoming = new Vec3(0, 0, -1);
        var ward = BlessingVisuals.recipe(id("aethon"), id("celestial_ward"), Cue.WARD,
                ANCHOR, FORWARD, incoming, 1).particles();
        h.assertTrue(ward.motion().velocity().dot(incoming) < 0,
                "Celestial Ward's blocked-contact fragments must recoil against incoming motion");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_rejects_every_wrong_god(GameTestHelper h) {
        Map<ResourceLocation, ResourceLocation> bindings = actualBindings(h);
        Set<ResourceLocation> gods = new HashSet<>(bindings.values());
        int rejected = 0;
        for (var binding : bindings.entrySet()) {
            for (ResourceLocation otherGod : gods) {
                if (otherGod.equals(binding.getValue())) continue;
                rejects(h, () -> BlessingVisuals.recipe(otherGod, binding.getKey(), Cue.CAST, ANCHOR, FORWARD, null, 1),
                        binding.getKey() + " accepted wrong god " + otherGod);
                rejected++;
            }
            ResourceLocation samePathOtherNamespace = new ResourceLocation(BATCH, binding.getValue().getPath());
            rejects(h, () -> BlessingVisuals.recipe(samePathOtherNamespace, binding.getKey(), Cue.CAST,
                    ANCHOR, FORWARD, null, 1), "God namespace was ignored for " + binding.getKey());
        }
        h.assertTrue(rejected == 1530, "Expected all 90 bindings against the other 17 actual gods");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_radius_and_link_bounds(GameTestHelper h) {
        for (Cue cue : Cue.values()) {
            for (double input : new double[]{-Double.MAX_VALUE, -1, 0, 0.25, 1, 12, 13, Double.MAX_VALUE}) {
                VisualRecipe recipe = fixture(cue, ANCHOR, FORWARD, null, input);
                double expected = Math.max(0.25, Math.min(12, input));
                close(h, recipe.visual().parameters().getDouble("ShapeRadius"), expected, "Radius clamp: " + input);
                bounds(h, recipe, cue + " radius " + input);
            }
            for (Vec3 input : List.of(new Vec3(8, -6, 4), new Vec3(-80, 60, -40),
                    new Vec3(-0.03, 0.02, 0.01), Vec3.ZERO)) {
                Vec3 expected = input.length() > 48 ? input.normalize().scale(48) : input;
                VisualRecipe recipe = fixture(cue, ANCHOR, FORWARD, input, 2);
                Vec3 stored = link(recipe.visual().parameters());
                vector(h, stored, expected, "Full signed link vector must be preserved or length-clamped");
                h.assertTrue(stored.length() <= 48 + 1.0E-8, "Link exceeded its 48-block cap");
                close(h, recipe.visual().parameters().getDouble("Radius"), Math.max(3.6, expected.length() * 0.5 + 1),
                        "Mesh bounds must include both segment ends");
                vector(h, recipe.particles().anchor().position(), ANCHOR.position().subtract(expected.scale(0.5)),
                        "Particle line must start at the segment origin, including short contact segments");
                bounds(h, recipe, cue + " link " + input);
                roundTrip(h, RenderEffectSpec.CODEC, recipe.visual(), "Linked visual");
                roundTrip(h, ParticleEmitterSpec.CODEC, recipe.particles(), "Linked particles");
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_anchor_and_direction_round_trip(GameTestHelper h) {
        List<RenderAnchor> anchors = List.of(ANCHOR, RenderAnchor.block(new BlockPos(-7, 12, 4)),
                RenderAnchor.entity(new UUID(17, 42), new Vec3(0.25, 0.8, -0.5)));
        for (RenderAnchor anchor : anchors) {
            for (Vec3 forward : List.of(FORWARD, Vec3.ZERO, new Vec3(0, -8, 0))) {
                VisualRecipe recipe = fixture(Cue.WARD, anchor, forward, null, 1);
                h.assertTrue(recipe.visual().anchor().equals(anchor) && recipe.particles().anchor().equals(anchor),
                        "World, block, and UUID/offset anchors must survive recipe construction");
                Vec3 expected = forward.lengthSqr() < 0.0001 ? new Vec3(0, 0, 1) : forward.normalize();
                vector(h, direction(recipe.visual().parameters()), expected, "Direction normalization/fallback");
                roundTrip(h, RenderEffectSpec.CODEC, recipe.visual(), "Anchored visual");
                roundTrip(h, ParticleEmitterSpec.CODEC, recipe.particles(), "Anchored particles");
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_invalid_recipe_parameters(GameTestHelper h) {
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            rejects(h, () -> fixture(Cue.CAST, ANCHOR, FORWARD, null, invalid), "Non-finite radius accepted");
            for (Vec3 invalidVector : List.of(new Vec3(invalid, 0, 1), new Vec3(0, invalid, 1), new Vec3(0, 1, invalid))) {
                rejects(h, () -> fixture(Cue.CAST, ANCHOR, invalidVector, null, 1), "Non-finite forward accepted");
                rejects(h, () -> fixture(Cue.CAST, ANCHOR, FORWARD, invalidVector, 1), "Non-finite link accepted");
            }
        }
        rejects(h, () -> fixture(Cue.CAST, ANCHOR, null, null, 1), "Null forward accepted");
        rejects(h, () -> fixture(null, ANCHOR, FORWARD, null, 1), "Null cue must be an invalid argument");
        rejects(h, () -> fixture(Cue.CAST, null, FORWARD, null, 1), "Null anchor must be an invalid argument");
        rejects(h, () -> BlessingVisuals.recipe(null, id("blazing_hands"), Cue.CAST, ANCHOR, FORWARD, null, 1),
                "Null god must be an invalid argument");
        rejects(h, () -> BlessingVisuals.recipe(id("ignar"), null, Cue.CAST, ANCHOR, FORWARD, null, 1),
                "Null blessing must be an invalid argument");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_malformed_codecs(GameTestHelper h) {
        VisualRecipe recipe = fixture(Cue.CAST, ANCHOR, FORWARD, null, 1);
        CompoundTag particleTag = (CompoundTag) result(ParticleEmitterSpec.CODEC.encodeStart(NbtOps.INSTANCE, recipe.particles()),
                "Encode particle fixture");
        h.assertTrue(particleTag.contains("emission", Tag.TAG_COMPOUND), "Fixture must encode the nested emission fields");
        CompoundTag invalidDuration = particleTag.copy();
        invalidDuration.putLong("duration", 0);
        h.assertTrue(ParticleEmitterSpec.CODEC.parse(NbtOps.INSTANCE, invalidDuration).error().isPresent(),
                "Particle codec accepted zero duration");
        CompoundTag invalidCap = particleTag.copy();
        invalidCap.getCompound("emission").putInt("maximum_alive", 0);
        var capResult = ParticleEmitterSpec.CODEC.parse(NbtOps.INSTANCE, invalidCap);
        // DFU optionalFieldOf may recover malformed nested values using the declared default.
        h.assertTrue(capResult.error().isPresent() || capResult.result().map(value ->
                        value.emission().maximumAlive() >= 1 && value.emission().maximumAlive() <= 65_536).orElse(false),
                "Particle codec retained an unsafe maximum-alive cap");
        CompoundTag invalidRate = particleTag.copy();
        invalidRate.getCompound("emission").putDouble("rate", Double.NaN);
        var rateResult = ParticleEmitterSpec.CODEC.parse(NbtOps.INSTANCE, invalidRate);
        h.assertTrue(rateResult.error().isPresent() || rateResult.result().map(value ->
                        Double.isFinite(value.emission().ratePerTick()) && value.emission().ratePerTick() >= 0
                                && value.emission().ratePerTick() <= 16_384).orElse(false),
                "Particle codec retained an unsafe emission rate");
        CompoundTag visualTag = (CompoundTag) result(RenderEffectSpec.CODEC.encodeStart(NbtOps.INSTANCE, recipe.visual()),
                "Encode visual fixture");
        visualTag.putInt("priority", 1_000_001);
        var priorityResult = RenderEffectSpec.CODEC.parse(NbtOps.INSTANCE, visualTag);
        h.assertTrue(priorityResult.error().isPresent() || priorityResult.result().map(value ->
                        value.priority() >= -1_000_000 && value.priority() <= 1_000_000).orElse(false),
                "Visual codec retained an out-of-range priority");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_private_packet_round_trip(GameTestHelper h) {
        ResourceLocation dimension = h.getLevel().dimension().location();
        int checked = 0;
        for (var binding : actualBindings(h).entrySet()) {
            VisualRecipe recipe = BlessingVisuals.recipe(binding.getValue(), binding.getKey(), Cue.MARK,
                    ANCHOR, FORWARD, null, 1);
            List<RenderEffectInstance> pair = privatePair(recipe, 100, 100 + recipe.visual().durationTicks());
            privateRoundTrip(h, PrivateBlessingEffectsPacket.spawn(dimension, pair.get(0), pair.get(1)),
                    "Legacy private MARK pair " + binding.getKey());
            privateRoundTrip(h, PrivateBlessingEffectsPacket.spawn(dimension, pair.get(1)),
                    "Particle-only private MARK " + binding.getKey());
            checked++;
        }
        h.assertTrue(checked == 90, "Expected all 90 private emitters and legacy pairs on the wire format");
        UUID first = new UUID(83, 1), second = new UUID(83, 2);
        privateRoundTrip(h, PrivateBlessingEffectsPacket.remove(dimension, first), "One private removal");
        privateRoundTrip(h, new PrivateBlessingEffectsPacket(dimension, List.of(), List.of(first, second)), "Two private removals");
        VisualRecipe recipe = BlessingVisuals.recipe(id("aethon"), id("veilpiercer"), Cue.MARK, ANCHOR, FORWARD, null, 1);
        for (int duration : new int[]{1, 32}) {
            VisualRecipe bounded = new VisualRecipe(recipe.visual().toBuilder().duration(duration).build(),
                    recipe.particles().toBuilder().duration(duration).burst(6, 1).maximumAlive(6).build());
            List<RenderEffectInstance> pair = privatePair(bounded, Long.MAX_VALUE - 32, Long.MAX_VALUE - 32 + duration);
            privateRoundTrip(h, PrivateBlessingEffectsPacket.spawn(dimension, pair.get(0), pair.get(1)),
                    "Private TTL boundary " + duration);
            privateRoundTrip(h, PrivateBlessingEffectsPacket.spawn(dimension, pair.get(1)),
                    "Private emitter TTL boundary " + duration);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_private_packet_rejections(GameTestHelper h) {
        h.assertTrue(PrivateBlessingEffectsPacket.MAX_LIFETIME == 32 && PrivateBlessingEffectsPacket.MAX_SPEC_BYTES == 16_384,
                "Private wire limits must remain 32 ticks and 16 KiB per spec");
        VisualRecipe recipe = BlessingVisuals.recipe(id("aethon"), id("veilpiercer"), Cue.MARK, ANCHOR, FORWARD, null, 1);
        List<RenderEffectInstance> pair = privatePair(recipe, 100, 124);
        RenderEffectInstance mesh = pair.get(0), particles = pair.get(1);
        ResourceLocation dimension = h.getLevel().dimension().location();
        for (int count : new int[]{3, 255}) {
            rejectsPrivateWire(h, privateBytes(buffer -> {
                buffer.writeResourceLocation(dimension);
                buffer.writeByte(count);
            }), "Invalid spawn count " + count);
        }
        for (int count : new int[]{0, 3, 255}) {
            rejectsPrivateWire(h, privateBytes(buffer -> {
                buffer.writeResourceLocation(dimension);
                buffer.writeByte(0);
                buffer.writeByte(count);
            }), "Invalid removal count " + count);
        }
        rejectsPrivateWire(h, privateWire(dimension, pair, List.of(mesh.id())), "Mixed spawn/removal payload");
        rejectsPrivatePair(h, List.of(mesh), "Single private mesh is not a particle emitter");
        rejectsPrivateWire(h, privateWire(dimension, List.of(particles), List.of(particles.id())), "Mixed emitter/removal payload");
        rejectsPrivatePair(h, List.of(new RenderEffectInstance(particles.id(), 0, particles.spec(), -1, 23)),
                "Single emitter with negative private start time");
        CompoundTag oversizedParameters = mesh.spec().parameters().copy();
        oversizedParameters.putByteArray("TestPadding", new byte[PrivateBlessingEffectsPacket.MAX_SPEC_BYTES + 1]);
        RenderEffectSpec oversized = mesh.spec().toBuilder().parameters(oversizedParameters).build();
        rejectsPrivateWire(h, privateWire(dimension, List.of(
                new RenderEffectInstance(mesh.id(), 0, oversized, 100, 124), particles), List.of()), "Oversized valid private spec");
        byte[] validNbt = privateBytes(buffer -> buffer.writeNbt((CompoundTag)
                result(RenderEffectSpec.CODEC.encodeStart(NbtOps.INSTANCE, mesh.spec()), "Private NBT fixture")));
        for (byte[] invalidNbt : List.of(new byte[]{0}, new byte[]{10, 0, 0, 0}, Arrays.copyOf(validNbt, validNbt.length + 1))) {
            rejectsPrivateWire(h, privateWire(dimension, pair, List.of(), invalidNbt), "Null/missing-spec/trailing private NBT");
        }
        byte[] valid = privateBytes(buffer -> PrivateBlessingEffectsPacket.spawn(dimension, mesh, particles).encode(buffer));
        rejectsPrivateWire(h, Arrays.copyOf(valid, valid.length - 1), "Truncated private frame", true);
        rejectsPrivateWire(h, Arrays.copyOf(valid, valid.length + 1), "Trailing private frame data");

        rejectsPrivatePair(h, List.of(particles, mesh), "Reversed manifestation/emitter order");
        rejectsPrivatePair(h, List.of(mesh, new RenderEffectInstance(mesh.id(), 0, particles.spec(), 100, 124)), "Duplicate effect IDs");
        rejectsPrivatePair(h, List.of(mesh, new RenderEffectInstance(particles.id(), 0, particles.spec(), 101, 125)), "Different pair times");
        rejectsPrivatePair(h, privatePair(fixture(Cue.CAST, ANCHOR, FORWARD, null, 1), 100, 128), "Non-MARK private effect");
        rejectsPrivatePair(h, privatePair(recipe, -1, 23), "Negative private start time");
        rejectsPrivatePair(h, privatePair(recipe, 100, 100), "Zero private TTL");
        rejectsPrivatePair(h, privatePair(recipe, 100, 99), "Reversed private TTL");
        rejectsPrivatePair(h, privatePair(recipe, 100, 123), "TTL/spec duration mismatch");
        rejectsPrivatePair(h, privatePair(recipe, Long.MAX_VALUE - 31, Long.MAX_VALUE), "Overflow-risk private start time");
        VisualRecipe tooLong = new VisualRecipe(recipe.visual().toBuilder().duration(33).build(),
                recipe.particles().toBuilder().duration(33).build());
        rejectsPrivatePair(h, privatePair(tooLong, 100, 133), "Private TTL exceeds 32");
        for (RenderAnchor anchor : List.of(RenderAnchor.entity(new UUID(84, 1), Vec3.ZERO),
                RenderAnchor.block(BlockPos.ZERO), RenderAnchor.world(new Vec3(30_000_001, 0, 0)),
                RenderAnchor.world(new Vec3(0, -4097, 0)), RenderAnchor.world(new Vec3(0, 0, -30_000_001)))) {
            VisualRecipe moved = new VisualRecipe(recipe.visual().toBuilder().anchor(anchor).build(),
                    recipe.particles().toBuilder().anchor(anchor).build());
            rejectsPrivatePair(h, privatePair(moved, 100, 124), "Private anchor out of contract " + anchor);
        }
        VisualRecipe mismatchedAnchor = new VisualRecipe(recipe.visual(),
                recipe.particles().toBuilder().anchor(RenderAnchor.world(Vec3.ZERO)).build());
        rejectsPrivatePair(h, privatePair(mismatchedAnchor, 100, 124), "Private pair anchor mismatch");
        RenderEffectSpec nestedAnchor = mismatchedAnchor.particles().toEffectSpec().toBuilder().anchor(ANCHOR).build();
        rejectsPrivatePair(h, List.of(mesh, new RenderEffectInstance(particles.id(), 0, nestedAnchor, 100, 124)),
                "Nested particle anchor differs from its valid carrier");
        rejectsPrivatePair(h, List.of(new RenderEffectInstance(particles.id(), 0, nestedAnchor, 100, 124)),
                "Single emitter anchor differs from its carrier");
        RenderEffectSpec nestedDuration = recipe.particles().toBuilder().duration(23).build().toEffectSpec()
                .toBuilder().duration(24).build();
        rejectsPrivatePair(h, List.of(mesh, new RenderEffectInstance(particles.id(), 0, nestedDuration, 100, 124)),
                "Nested particle duration differs from its valid carrier");
        VisualRecipe distant = new VisualRecipe(recipe.visual().toBuilder()
                .visibility(new RenderVisibility(65, false, false, true)).build(), recipe.particles());
        rejectsPrivatePair(h, privatePair(distant, 100, 124), "Private visibility exceeds 64");
        for (ParticleEmitterSpec invalid : List.of(recipe.particles().toBuilder().maximumAlive(17).build(),
                recipe.particles().toBuilder().point(17, 22).build(),
                recipe.particles().toBuilder().burst(6, 33).maximumAlive(6).build())) {
            rejectsPrivatePair(h, privatePair(new VisualRecipe(recipe.visual(), invalid), 100, 124),
                    "Private particle cap/rate/lifetime exceeds contract");
            rejectsPrivatePair(h, List.of(new RenderEffectInstance(particles.id(), 0, invalid.toEffectSpec(), 100, 124)),
                    "Single emitter cap/rate/lifetime exceeds contract");
        }
        ParticleCollision authoritative = new ParticleCollision(ParticleCollision.WorldResponse.NONE,
                ParticleCollision.EntityResponse.IGNORE, 0.05, 0.6, 0.15, Optional.empty(),
                Optional.of(id("forbidden_private_test_collision")), true);
        rejectsPrivatePair(h, privatePair(new VisualRecipe(recipe.visual(),
                recipe.particles().toBuilder().collision(authoritative).build()), 100, 124),
                "Private particles must not request authoritative collision");
        rejects(h, () -> PrivateBlessingEffectsPacket.spawn(dimension,
                new RenderEffectInstance(mesh.id(), 1, mesh.spec(), 100, 124), particles), "Private revisions must remain zero");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_budget_intervals_and_independence(GameTestHelper h) {
        BlessingEffectBudget owner = new BlessingEffectBudget();
        BlessingEffectBudget otherOwner = new BlessingEffectBudget();
        String aura = "divinity_engine:inferno_mantle/aura";
        h.assertTrue(owner.acquire(aura, 100, 10, 2), "First cue in a burst must be admitted");
        h.assertTrue(owner.acquire(aura, 100, 10, 2), "Second cue in a burst must be admitted");
        h.assertFalse(owner.acquire(aura, 100, 10, 2), "Burst overflow must be rejected");
        h.assertTrue(otherOwner.acquire(aura, 100, 10, 2), "Owners must have independent cue budgets");
        h.assertTrue(owner.acquire("divinity_engine:beacons_grace/aura", 100, 10, 1),
                "One blessing must not consume another blessing's per-key interval");
        h.assertTrue(owner.acquire("divinity_engine:inferno_mantle/impact", 100, 10, 1),
                "Distinct cue keys must have independent intervals");
        h.assertFalse(owner.acquire(aura, 109, 10, 2), "Interval must remain closed one tick before its boundary");
        h.assertTrue(owner.acquire(aura, 110, 10, 2), "Interval must reopen at its exact boundary");
        h.assertTrue(owner.acquire(aura, 110, 10, 2), "Renewed interval must reset the burst count");
        h.assertFalse(owner.acquire(aura, 110, 10, 2), "Renewed interval still enforces its burst cap");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_budget_global_window(GameTestHelper h) {
        BlessingEffectBudget budget = new BlessingEffectBudget();
        for (int i = 0; i < 12; i++) {
            h.assertTrue(budget.acquire("first/" + i, 100, 40, 2), "Global allowance lost admission " + i);
        }
        h.assertFalse(budget.acquire("overflow", 100, 40, 2), "A thirteenth effect must be rejected");
        h.assertFalse(budget.acquire("first/0", 100, 40, 2), "Existing keys cannot bypass the owner cap");
        h.assertFalse(budget.acquire("overflow", 119, 40, 2), "Owner cap must last the full 20 ticks");
        for (int i = 0; i < 12; i++) {
            h.assertTrue(budget.acquire("second/" + i, 120, 40, 2), "Window did not reset at exactly 20 ticks: " + i);
        }
        h.assertFalse(budget.acquire("overflow", 120, 40, 2), "Reset window must retain the cap of 12");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_budget_rejections_are_free(GameTestHelper h) {
        BlessingEffectBudget budget = new BlessingEffectBudget();
        h.assertTrue(budget.acquire("blocked", 0, 100, 1), "Initial admission failed");
        for (int i = 0; i < 64; i++) {
            h.assertFalse(budget.acquire("blocked", 0, 100, 1), "Repeated interval overflow was accepted");
        }
        for (int i = 0; i < 11; i++) {
            h.assertTrue(budget.acquire("accepted/" + i, 0, 100, 1), "Rejected requests consumed owner allowance");
        }
        h.assertFalse(budget.acquire("last", 0, 100, 1), "Accepted requests must still exhaust the cap");
        h.assertTrue(budget.acquire("last", 20, 100, 1), "A rejected key must not acquire an interval reservation");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_budget_key_capacity_and_expiry(GameTestHelper h) {
        BlessingEffectBudget budget = new BlessingEffectBudget();
        for (int i = 0; i < 64; i++) {
            long tick = (i / 12) * 20L;
            h.assertTrue(budget.acquire("key/" + i, tick, 1, 1), "Key capacity rejected entry " + i);
        }
        h.assertFalse(budget.acquire("key/64", 100, 1, 1), "A 65th retained key must be rejected");
        h.assertTrue(budget.acquire("key/63", 101, 1, 1), "Existing keys must remain usable at capacity");
        h.assertFalse(budget.acquire("key/64", 200, 1, 1), "Age exactly 200 must not prematurely evict keys");
        h.assertTrue(budget.acquire("key/64", 220, 1, 1), "Expired keys must release capacity on a window reset");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_budget_clock_rollback(GameTestHelper h) {
        BlessingEffectBudget budget = new BlessingEffectBudget();
        for (int i = 0; i < 12; i++) {
            h.assertTrue(budget.acquire("key/" + i, 100, 40, 1), "Initial rollback fixture admission failed");
        }
        h.assertFalse(budget.acquire("key/0", 100, 40, 1), "Fixture did not fill the budget");
        h.assertTrue(budget.acquire("key/0", 90, 40, 1), "Clock rollback must reset future owner/key windows");
        h.assertFalse(budget.acquire("key/0", 90, 40, 1), "Rollback must not disable the renewed interval");
        h.succeed();
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_server_lifecycle(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        RenderEffectSavedData data = RenderEffectSavedData.get(level);
        Pig owner = new Pig(EntityType.PIG, level);
        Pig other = new Pig(EntityType.PIG, level);
        Runnable cleanup = () -> {
            for (Pig pig : List.of(owner, other)) {
                BlessingVisuals.death(new LivingDeathEvent(pig, level.damageSources().generic()));
                pig.discard();
            }
        };
        try {
            for (Pig pig : List.of(owner, other)) {
                pig.setNoAi(true);
                pig.setNoGravity(true);
                pig.setInvulnerable(true);
                pig.setPos(h.absoluteVec(new Vec3(2, 2, 2)));
                h.assertTrue(level.addFreshEntity(pig), "Lifecycle owner could not be added to the test level");
            }
            BlessingsInstance blessing = bound("ignar", "blazing_hands");
            int handles = channels("blazing_hands");
            CompoundTag originalBlessing = blessing.toNBT().copy();
            float health = owner.getHealth();
            Vec3 velocity = owner.getDeltaMovement();
            Set<UUID> baseline = effectIds(data, level);
            Set<UUID> ownerIds = new HashSet<>();
            Set<UUID> quietIds = admit(h, data, level, () -> BlessingVisuals.pulse(blessing, owner, Cue.AURA), handles, "Owner aura");
            ownerIds.addAll(quietIds);
            admit(h, data, level, () -> BlessingVisuals.pulse(blessing, owner, Cue.AURA), 0, "Duplicate aura suppression");
            ownerIds.addAll(admit(h, data, level, () -> BlessingVisuals.at(blessing, owner, owner.position(), Cue.IMPACT, 2),
                    handles, "Targeted impact"));
            Vec3 from = owner.position();
            Vec3 to = from.add(3, 1, -2);
            Set<UUID> trailIds = admit(h, data, level, () -> BlessingVisuals.link(blessing, owner, from, to, Cue.TRAIL), handles, "Linked trail");
            ownerIds.addAll(trailIds);
            quietIds.addAll(trailIds);
            BlessingsInstance wrongGod = bound("lumen", "blazing_hands");
            admit(h, data, level, () -> BlessingVisuals.pulse(wrongGod, owner, Cue.WARD), 0, "Wrong-god rejection");
            BlessingsInstance unbound = BlessingsRegistry.BLAZING_HANDS.get().createDefaultInstance();
            admit(h, data, level, () -> BlessingVisuals.pulse(unbound, owner, Cue.WARD), 0, "Unbound rejection");
            other.setInvisible(true);
            admit(h, data, level, () -> BlessingVisuals.pulse(blessing, other, Cue.AURA), 0, "Invisible owner aura suppression");
            admit(h, data, level, () -> BlessingVisuals.link(blessing, other, from, to, Cue.TRAIL), 0, "Invisible owner trail suppression");
            other.setInvisible(false);
            Set<UUID> otherIds = admit(h, data, level, () -> BlessingVisuals.pulse(blessing, other, Cue.AURA),
                    handles, "Same blessing on an independent owner");

            // Three admissions already occurred; fill the owner's remaining nine without reusing a key.
            for (Cue cue : new Cue[]{Cue.CAST, Cue.WARD, Cue.HEAL, Cue.CONTROL, Cue.TELEPORT, Cue.MARK, Cue.RECALL}) {
                ownerIds.addAll(admit(h, data, level, () -> BlessingVisuals.pulse(blessing, owner, cue), handles, "Distinct cue " + cue));
            }
            BlessingsInstance second = bound("ignar", "fireproof");
            ownerIds.addAll(admit(h, data, level, () -> BlessingVisuals.pulse(second, owner, Cue.CAST), channels("fireproof"), "Owner admission eleven"));
            ownerIds.addAll(admit(h, data, level, () -> BlessingVisuals.pulse(second, owner, Cue.WARD), channels("fireproof"), "Owner admission twelve"));
            admit(h, data, level, () -> BlessingVisuals.pulse(second, owner, Cue.HEAL), 0, "Owner thirteenth admission suppressed");
            h.assertTrue(ownerIds.size() == 10 * handles + 2 * channels("fireproof"), "Admissions must spawn only authored channels");
            h.assertTrue(blessing.toNBT().equals(originalBlessing), "Rendering changed blessing state");
            close(h, owner.getHealth(), health, "Rendering changed owner health");
            vector(h, owner.getDeltaMovement(), velocity, "Rendering changed owner velocity");

            Set<UUID> expectedConcealed = effectIds(data, level);
            expectedConcealed.removeAll(quietIds);
            owner.setInvisible(true);
            BlessingVisuals.pulse(blessing, owner, Cue.AURA);
            h.assertTrue(quietIds.stream().allMatch(id -> data.get(id).isEmpty()),
                    "Entering invisibility left previously active aura/trail effects tracking the owner");
            h.assertTrue(effectIds(data, level).equals(expectedConcealed),
                    "Concealment must preserve non-quiet effects, other owners, and pre-existing effects");
            admit(h, data, level, () -> BlessingVisuals.link(blessing, owner, from, to, Cue.TRAIL), 0,
                    "Invisible owner must not restore removed trails");
            owner.setInvisible(false);

            BlessingVisuals.death(new LivingDeathEvent(owner, level.damageSources().generic()));
            h.assertTrue(ownerIds.stream().allMatch(id -> data.get(id).isEmpty()), "Cleanup left owner effects behind");
            h.assertTrue(otherIds.stream().allMatch(id -> data.get(id).isPresent()), "Cleanup removed another owner's effects");
            h.assertTrue(effectIds(data, level).containsAll(baseline), "Cleanup removed pre-existing effects");
            long expires = otherIds.stream().map(id -> data.get(id).orElseThrow())
                    .mapToLong(RenderEffectInstance::endGameTime).max().orElseThrow();
            h.runAfterDelay(50, () -> {
                try {
                    h.assertTrue(level.getGameTime() >= expires, "Expiry check ran before the real effect deadline");
                    // get() does not prune: this checks Core's server lifecycle, not a test-triggered snapshot cleanup.
                    h.assertTrue(otherIds.stream().allMatch(id -> data.get(id).isEmpty()), "Core did not expire the remaining pair");
                    admit(h, data, level, () -> BlessingVisuals.pulse(blessing, other, Cue.AURA), handles,
                            "Owner/cue admission must recover after its interval and expiry");
                    h.succeed();
                } finally {
                    cleanup.run();
                }
            });
        } catch (RuntimeException | Error failure) {
            cleanup.run();
            throw failure;
        }
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_independent_concealment_cleanup(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        RenderEffectSavedData data = RenderEffectSavedData.get(level);
        Pig owner = new Pig(EntityType.PIG, level);
        Pig other = new Pig(EntityType.PIG, level);
        Runnable cleanup = () -> {
            for (Pig pig : List.of(owner, other)) {
                BlessingVisuals.death(new LivingDeathEvent(pig, level.damageSources().generic()));
                pig.discard();
            }
        };
        try {
            for (Pig pig : List.of(owner, other)) {
                pig.setNoAi(true);
                pig.setNoGravity(true);
                pig.setInvulnerable(true);
                pig.setPos(h.absoluteVec(new Vec3(2, 2, 2)));
                h.assertTrue(level.addFreshEntity(pig), "Concealment owner could not be added to the level");
            }
            BlessingsInstance blessing = bound("ignar", "blazing_hands");
            int handles = channels("blazing_hands");
            Set<UUID> quietIds = admit(h, data, level, () -> BlessingVisuals.pulse(blessing, owner, Cue.AURA), handles,
                    "Independent cleanup aura");
            quietIds.addAll(admit(h, data, level,
                    () -> BlessingVisuals.link(blessing, owner, owner.position(), owner.position().add(2, 1, 0), Cue.TRAIL),
                    handles, "Independent cleanup trail"));
            Set<UUID> retained = admit(h, data, level, () -> BlessingVisuals.pulse(blessing, owner, Cue.WARD), handles,
                    "Concealment must retain the owner's non-quiet ward");
            retained.addAll(admit(h, data, level, () -> BlessingVisuals.pulse(blessing, other, Cue.AURA), handles,
                    "Concealment must retain the other owner's aura"));
            long now = level.getGameTime();
            long expires = quietIds.stream().map(id -> data.get(id).orElseThrow())
                    .mapToLong(RenderEffectInstance::endGameTime).min().orElseThrow();
            owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
            owner.setInvisible(true);
            h.assertTrue(quietIds.stream().allMatch(id -> data.get(id).isPresent()),
                    "Fixture must still contain live effects before the server tick");
            // No further requests or direct tick-handler calls: the real END event must conceal them.
            h.runAfterDelay(2, () -> {
                try {
                    h.assertTrue(level.getGameTime() > now && level.getGameTime() < expires,
                            "Concealment check must occur after a tick but before natural TTL expiry");
                    h.assertTrue(owner.isInvisible(), "Fixture owner unexpectedly became visible");
                    h.assertTrue(quietIds.stream().allMatch(id -> data.get(id).isEmpty()),
                            "Quiet effects survived invisibility without another visual request");
                    h.assertTrue(retained.stream().allMatch(id -> data.get(id).isPresent()),
                            "Independent concealment removed non-quiet or other-owner effects");
                    h.succeed();
                } finally {
                    cleanup.run();
                }
            });
        } catch (RuntimeException | Error failure) {
            cleanup.run();
            throw failure;
        }
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_authored_channels_spawn(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        RenderEffectSavedData data = RenderEffectSavedData.get(level);
        Pig owner = new Pig(EntityType.PIG, level);
        owner.setNoAi(true);
        owner.setNoGravity(true);
        owner.setPos(h.absoluteVec(new Vec3(2, 2, 2)));
        h.assertTrue(level.addFreshEntity(owner), "Channel test owner could not be added");
        try {
            for (var binding : actualBindings(h).entrySet()) {
                var design = BlessingVisualDesigns.get(binding.getKey());
                var blessing = bound(binding.getValue().getPath(), binding.getKey().getPath());
                Set<UUID> added = admit(h, data, level,
                        () -> BlessingVisuals.at(blessing, owner, owner.position(), Cue.CAST, 1),
                        channels(binding.getKey().getPath()), "Authored channels for " + binding.getKey());
                long emitters = added.stream().map(id -> data.get(id).orElseThrow())
                        .filter(effect -> ParticleEmitterSpec.fromEffect(effect.spec()).isPresent()).count();
                h.assertTrue(emitters == (design.particles() ? 1 : 0), "Unexpected emitter for " + binding.getKey());
                h.assertTrue(added.size() - emitters == (design.mesh() ? 1 : 0), "Unexpected mesh for " + binding.getKey());
                BlessingVisuals.death(new LivingDeathEvent(owner, level.damageSources().generic()));
                h.assertTrue(added.stream().allMatch(id -> data.get(id).isEmpty()), "Channel cleanup failed");
            }
            h.succeed();
        } finally {
            BlessingVisuals.death(new LivingDeathEvent(owner, level.damageSources().generic()));
            owner.discard();
        }
    }

    @GameTest(templateNamespace = MODID, template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void divinity_effects_directed_contact(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        RenderEffectSavedData data = RenderEffectSavedData.get(level);
        Pig owner = new Pig(EntityType.PIG, level);
        owner.setNoAi(true);
        owner.setPos(h.absoluteVec(new Vec3(2, 2, 2)));
        h.assertTrue(level.addFreshEntity(owner), "Directed effect owner could not be added");
        try {
            var blessing = bound("zephra", "gale_step");
            Vec3 contact = owner.position().add(.2, .08, -.1);
            Vec3 movement = new Vec3(-1, 0, 0);
            Set<UUID> added = admit(h, data, level,
                    () -> BlessingVisuals.at(blessing, owner, contact, Cue.TRAIL, 1, movement), 1,
                    "Directed contact effect");
            var actual = ParticleEmitterSpec.fromEffect(data.get(added.iterator().next()).orElseThrow().spec()).orElseThrow();
            var expected = BlessingVisuals.recipe(id("zephra"), id("gale_step"), Cue.TRAIL,
                    RenderAnchor.world(contact), movement, null, 1).particles();
            h.assertTrue(actual.equals(expected), "Directed contact ignored movement or invented a link");
            admit(h, data, level, () -> BlessingVisuals.at(blessing, owner, contact, Cue.IMPACT, 1,
                    new Vec3(Double.NaN, 0, 0)), 0, "Non-finite contact direction");
            h.succeed();
        } finally {
            BlessingVisuals.death(new LivingDeathEvent(owner, level.damageSources().generic()));
            owner.discard();
        }
    }

    private static int channels(String blessing) {
        var design = BlessingVisualDesigns.get(id(blessing));
        return (design.mesh() ? 1 : 0) + (design.particles() ? 1 : 0);
    }

    private static List<RenderEffectInstance> privatePair(VisualRecipe recipe, long start, long end) {
        return List.of(new RenderEffectInstance(new UUID(82, 1), 0, recipe.visual(), start, end),
                new RenderEffectInstance(new UUID(82, 2), 0, recipe.particles().toEffectSpec(), start, end));
    }

    private static void privateRoundTrip(GameTestHelper h, PrivateBlessingEffectsPacket original, String context) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            original.encode(buffer);
            PrivateBlessingEffectsPacket decoded = PrivateBlessingEffectsPacket.decode(buffer);
            h.assertTrue(original.equals(decoded), context + ": private wire round trip changed data");
            h.assertFalse(buffer.isReadable(), context + ": private decoder did not consume the frame");
        } finally {
            buffer.release();
        }
    }

    private static byte[] privateBytes(Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writer.accept(buffer);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    private static byte[] privateWire(ResourceLocation dimension, List<RenderEffectInstance> effects, List<UUID> removals) {
        return privateWire(dimension, effects, removals, null);
    }

    private static byte[] privateWire(ResourceLocation dimension, List<RenderEffectInstance> effects, List<UUID> removals,
                                      byte[] firstSpecOverride) {
        // Deliberately bypass packet construction so malformed pairs actually reach its decoder.
        return privateBytes(buffer -> {
            buffer.writeResourceLocation(dimension);
            buffer.writeByte(effects.size());
            for (int index = 0; index < effects.size(); index++) {
                RenderEffectInstance effect = effects.get(index);
                buffer.writeUUID(effect.id());
                buffer.writeLong(effect.startGameTime());
                buffer.writeLong(effect.endGameTime());
                CompoundTag tag = (CompoundTag) result(RenderEffectSpec.CODEC.encodeStart(NbtOps.INSTANCE, effect.spec()),
                        "Private wire fixture");
                buffer.writeByteArray(index == 0 && firstSpecOverride != null ? firstSpecOverride
                        : privateBytes(encoded -> encoded.writeNbt(tag)));
            }
            buffer.writeByte(removals.size());
            removals.forEach(buffer::writeUUID);
        });
    }

    private static void rejectsPrivatePair(GameTestHelper h, List<RenderEffectInstance> effects, String context) {
        ResourceLocation dimension = h.getLevel().dimension().location();
        rejects(h, () -> new PrivateBlessingEffectsPacket(dimension, effects, List.of()), context + " constructor");
        rejectsPrivateWire(h, privateWire(dimension, effects, List.of()), context + " wire");
    }

    private static void rejectsPrivateWire(GameTestHelper h, byte[] bytes, String context) {
        rejectsPrivateWire(h, bytes, context, false);
    }

    private static void rejectsPrivateWire(GameTestHelper h, byte[] bytes, String context, boolean allowUnderflow) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            PrivateBlessingEffectsPacket.decode(buffer);
        } catch (DecoderException expected) {
            return;
        } catch (IndexOutOfBoundsException truncated) {
            if (!allowUnderflow) throw truncated;
            return;
        } finally {
            buffer.release();
        }
        h.assertTrue(false, context + ": malformed private packet was accepted");
    }

    private static VisualRecipe fixture(Cue cue, RenderAnchor anchor, Vec3 forward, Vec3 link, double radius) {
        return BlessingVisuals.recipe(id("ignar"), id("blazing_hands"), cue, anchor, forward, link, radius);
    }

    private static void bounds(GameTestHelper h, VisualRecipe recipe, String context) {
        RenderEffectSpec visual = recipe.visual();
        ParticleEmitterSpec particles = recipe.particles();
        h.assertTrue(visual.type().equals(BlessingVisuals.TYPE) && visual.primitive().equals(BlessingVisuals.TYPE),
                context + ": wrong manifestation type/primitive");
        h.assertTrue(visual.durationTicks() > 0 && visual.durationTicks() <= 32, context + ": unbounded visual duration");
        h.assertTrue(particles.durationTicks() == visual.durationTicks(), context + ": mismatched emitter duration");
        Cue cue = Cue.valueOf(visual.parameters().getString("Cue"));
        boolean quiet = cue == Cue.AURA || cue == Cue.TRAIL || cue == Cue.MARK;
        var design = BlessingVisualDesigns.get(new ResourceLocation(visual.parameters().getString("BlessingId")));
        int expectedDuration = design.duration();
        h.assertTrue(visual.durationTicks() == expectedDuration, context + ": cue duration changed");
        double radius = visual.parameters().getDouble("ShapeRadius");
        double bounds = visual.parameters().getDouble("Radius");
        h.assertTrue(Double.isFinite(radius) && radius >= 0.25 && radius <= 12, context + ": unbounded shape radius");
        h.assertTrue(Double.isFinite(bounds) && bounds >= radius && bounds <= 25 + 1.0E-8,
                context + ": unbounded mesh radius");
        h.assertTrue(visual.visibility().maximumDistance() == 48 && particles.visibility().maximumDistance() == 48,
                context + ": visibility must remain within 48 blocks");
        h.assertFalse(visual.visibility().throughWalls() || particles.visibility().throughWalls(), context + ": through-wall rendering");
        var appearance = particles.appearance();
        h.assertTrue(appearance.bloom() == ParticleBloomMode.OFF, context + ": unexpected particle bloom");
        h.assertTrue(appearance.glowStrength() == 0, context + ": halo-producing particle glow");
        h.assertTrue(appearance.material().equals(id("particle_alpha")), context + ": wrong pixel material");
        h.assertTrue(appearance.renderer().equals(coreId("textured_billboard")), context + ": particles must use textured billboards");
        ResourceLocation texture = appearance.texture().orElseThrow();
        h.assertTrue(texture.getNamespace().equals("minecraft") && texture.getPath().startsWith("textures/particle/")
                && texture.getPath().endsWith(".png"), context + ": particles must use vanilla particle textures");
        close(h, appearance.size().sample(0), design.particleSize(), context + ": initial particle size");
        for (var key : appearance.size().keys()) {
            h.assertTrue(Double.isFinite(key.value()) && key.value() >= 0.015 && key.value() <= 0.3,
                    context + ": particle size must remain within small vanilla-sprite bounds");
        }
        h.assertTrue(appearance.alpha().sample(0) > 0 && appearance.alpha().sample(0) <= 0.92
                && appearance.alpha().sample(1) == 0, context + ": particles must remain translucent and fade out");
        var emission = particles.emission();
        h.assertTrue(emission.ratePerTick() == 0, context + ": emission must be a bounded burst");
        int total = emission.bursts().stream().mapToInt(burst -> burst.count()).sum();
        h.assertTrue(total > 0 && total <= 16 && emission.maximumAlive() == total, context + ": unbounded particle count");
        int expectedCount = design.particleCount();
        h.assertTrue(total == expectedCount && emission.bursts().size() == 1 && emission.bursts().get(0).tick() == 0,
                context + ": cue must use its authored single bounded particle burst");
        h.assertTrue(emission.minimumLifetime() > 0 && emission.minimumLifetime() <= emission.maximumLifetime()
                && emission.maximumLifetime() < particles.durationTicks(), context + ": particle lifetime exceeds emitter");
        h.assertTrue(emission.minimumLifetime() == expectedDuration - 2 && emission.maximumLifetime() == expectedDuration - 2,
                context + ": particle lifetime changed");
        h.assertTrue(Double.isFinite(particles.estimatedBoundsRadius()) && particles.estimatedBoundsRadius() <= 2048,
                context + ": invalid particle bounds");
        particleShapeBounds(h, recipe, context);
        var motion = particles.motion();
        h.assertTrue(Double.isFinite(motion.velocity().length()) && motion.velocity().length() <= 4,
                context + ": excessive or invalid particle velocity");
        Vec3 spread = motion.velocitySpread();
        h.assertTrue(Double.isFinite(spread.length()) && spread.length() <= 1
                && spread.x >= 0 && spread.y >= 0 && spread.z >= 0, context + ": invalid particle spread");
        h.assertTrue(Double.isFinite(motion.gravity()) && Math.abs(motion.gravity()) <= 0.1,
                context + ": invalid particle gravity");
        h.assertTrue(Double.isFinite(motion.drag()) && motion.drag() >= 0 && motion.drag() <= 1,
                context + ": invalid particle drag");
        vector(h, motion.acceleration(), Vec3.ZERO, context + ": unexpected acceleration");
        close(h, motion.inheritedAnchorVelocity(), 0, context + ": unexpected inherited velocity");
        double spin = texture.getPath().equals("textures/particle/cherry_6.png") ? 0.08 : 0;
        close(h, motion.angularVelocity(), spin, context + ": authored petal rotation");
        h.assertTrue(particles.behaviors().size() <= 2, context + ": excessive behavior stack");
        for (var behavior : particles.behaviors()) {
            h.assertTrue(Set.of(coreId("orbit"), coreId("attractor"), coreId("curl_noise")).contains(behavior.type()),
                    context + ": unsupported behavior or child emitter");
            for (String key : behavior.data().getAllKeys()) {
                h.assertTrue(Double.isFinite(behavior.data().getDouble(key)), context + ": nonfinite behavior parameter");
            }
        }
        ParticleRegistries.validate(particles);
    }

    private static void particleShapeBounds(GameTestHelper h, VisualRecipe recipe, String context) {
        var emission = recipe.particles().emission();
        CompoundTag data = emission.shapeData();
        h.assertTrue(emission.shape().getNamespace().equals("nightshade_core"), context + ": unknown shape namespace");
        h.assertTrue(Set.of("point", "box", "sphere", "ring", "cylinder", "cone", "line").contains(emission.shape().getPath()),
                context + ": unsupported primitive shape");
        for (String key : data.getAllKeys()) {
            if (!data.contains(key, Tag.TAG_ANY_NUMERIC)) continue;
            double value = data.getDouble(key);
            h.assertTrue(Double.isFinite(value) && Math.abs(value) <= 96, context + ": invalid shape parameter " + key);
        }
    }

    private static ResourceLocation coreId(String path) {
        return new ResourceLocation("nightshade_core", path);
    }

    private static <T> T roundTrip(GameTestHelper h, Codec<T> codec, T original, String context) {
        Tag encoded = result(codec.encodeStart(NbtOps.INSTANCE, original), context + " encoding");
        T decoded = result(codec.parse(NbtOps.INSTANCE, encoded), context + " decoding");
        h.assertTrue(original.equals(decoded), context + ": codec round trip changed data: " + original + " -> " + decoded);
        return decoded;
    }

    private static <T> T result(DataResult<T> result, String context) {
        return result.result().orElseThrow(() -> new IllegalStateException(context + ": "
                + result.error().map(DataResult.PartialResult::message).orElse("missing codec result")));
    }

    private static void rejects(GameTestHelper h, Runnable operation, String message) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        h.assertTrue(false, message);
    }

    private static void close(GameTestHelper h, double actual, double expected, String context) {
        h.assertTrue(Double.isFinite(actual) && Math.abs(actual - expected) < 1.0E-8,
                context + ": expected " + expected + ", got " + actual);
    }

    private static void vector(GameTestHelper h, Vec3 actual, Vec3 expected, String context) {
        close(h, actual.x, expected.x, context + " x");
        close(h, actual.y, expected.y, context + " y");
        close(h, actual.z, expected.z, context + " z");
    }

    private static Vec3 direction(CompoundTag tag) {
        return new Vec3(tag.getDouble("DirectionX"), tag.getDouble("DirectionY"), tag.getDouble("DirectionZ"));
    }

    private static Vec3 link(CompoundTag tag) {
        return new Vec3(tag.getDouble("LinkX"), tag.getDouble("LinkY"), tag.getDouble("LinkZ"));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    private static BlessingsInstance bound(String god, String blessing) {
        BlessingsInstance instance = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(id(blessing)).createDefaultInstance();
        instance.setBoundGod(GodsRegistry.GODS_REGISTRY.get().getValue(id(god)).createGodDefaultInstance());
        return instance;
    }

    private static Set<UUID> effectIds(RenderEffectSavedData data, ServerLevel level) {
        return data.snapshot(level.getGameTime()).stream().map(RenderEffectInstance::id).collect(Collectors.toSet());
    }

    private static Set<UUID> admit(GameTestHelper h, RenderEffectSavedData data, ServerLevel level,
                                   Runnable operation, int expected, String context) {
        Set<UUID> before = effectIds(data, level);
        operation.run();
        Set<UUID> after = effectIds(data, level);
        h.assertTrue(after.containsAll(before), context + ": emission removed existing effects");
        after.removeAll(before);
        h.assertTrue(after.size() == expected, context + ": expected " + expected + " new effect records, got " + after.size());
        if (expected == 2) {
            long visuals = after.stream().map(id -> data.get(id).orElseThrow())
                    .filter(effect -> effect.spec().type().equals(BlessingVisuals.TYPE)).count();
            long particles = after.stream().map(id -> data.get(id).orElseThrow())
                    .filter(effect -> ParticleEmitterSpec.fromEffect(effect.spec()).isPresent()).count();
            h.assertTrue(visuals == 1 && particles == 1, context + ": admission was not a complete visual/emitter pair");
        }
        return after;
    }

    private static Map<ResourceLocation, ResourceLocation> actualBindings(GameTestHelper h) {
        var gods = GodsRegistry.GODS_REGISTRY.get();
        var blessings = BlessingsRegistry.BLESSINGS_REGISTRY.get();
        Set<ResourceLocation> godIds = builtinKeys(gods.getKeys());
        h.assertTrue(godIds.size() == 18, "Expected 18 built-in gods in the actual Forge registry");
        Map<ResourceLocation, ResourceLocation> bindings = new HashMap<>();
        for (ResourceLocation godId : godIds) {
            var god = gods.getValue(godId);
            h.assertTrue(god != null && god.getBlessings().size() == 5, "Expected five real bindings for " + godId);
            for (var reference : god.getBlessings()) {
                ResourceLocation blessingId = reference.getId();
                h.assertTrue(reference.isPresent() && blessings.getValue(blessingId) == reference.get(),
                        "God binding must resolve to the actual registered blessing: " + blessingId);
                h.assertTrue(bindings.put(blessingId, godId) == null, "Blessing belongs to multiple gods: " + blessingId);
            }
        }
        h.assertTrue(bindings.size() == 90, "Expected 90 distinct actual god-to-blessing bindings");
        h.assertTrue(bindings.keySet().equals(builtinKeys(blessings.getKeys())),
                "Every built-in registered blessing must have exactly one actual god binding");
        return bindings;
    }

    private static Set<ResourceLocation> builtinKeys(Set<ResourceLocation> keys) {
        return keys.stream().filter(id -> MODID.equals(id.getNamespace())).collect(Collectors.toSet());
    }
}
