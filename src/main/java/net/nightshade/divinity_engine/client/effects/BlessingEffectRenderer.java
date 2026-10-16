package net.nightshade.divinity_engine.client.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.nightshade.divinity_engine.util.divinity.effects.BlessingVisualProfiles;
import net.nightshade.renderlib.client.world.CustomWorldRendererRegistry;
import net.nightshade.renderlib.client.world.WorldRenderContext;
import net.nightshade.renderlib.config.RenderLibConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client-only composition dispatch and bounded geometry. Final effects belong to blessing composers. */
@OnlyIn(Dist.CLIENT)
public final class BlessingEffectRenderer {
    public static final ResourceLocation PRIMITIVE =
            new ResourceLocation("divinity_engine", "blessing_manifestation");
    private static final ResourceLocation POCKET_DIMENSION = new ResourceLocation("divinity_engine", "pocket_dimension");
    /** Includes vanilla LINES expansion (two physical vertices per submitted endpoint). */
    public static final int MAX_VERTICES = 1920;
    public static final double MAX_LINK_LENGTH = 48.0;
    public static final double MAX_RADIUS = 12.0;
    public static final double TAU = Math.PI * 2;
    public static final Vec3 UP = new Vec3(0, 1, 0);
    public static final Vec3 FORWARD = new Vec3(0, 0, 1);
    public static final Vec3 RIGHT = new Vec3(1, 0, 0);
    private static final Vec3 INVALID_POINT = new Vec3(Double.NaN, Double.NaN, Double.NaN);
    private static final Map<ResourceLocation, EffectComposer> COMPOSERS = new ConcurrentHashMap<>();
    private static boolean coreRegistered;
    private static boolean portalRegistered;

    private BlessingEffectRenderer() {}

    /** Call from normal client setup. Core registration and each built-in group run once. */
    public static synchronized void register() {
        if (!coreRegistered) {
            CustomWorldRendererRegistry.register(PRIMITIVE, BlessingEffectRenderer::render);
            coreRegistered = true;
        }
        registerBuiltinComposers();
    }

    /** Register a final composition by full blessing ID, never by god or cue. Duplicate IDs fail fast. */
    public static void register(ResourceLocation blessing, EffectComposer composer) {
        Objects.requireNonNull(blessing, "blessing");
        Objects.requireNonNull(composer, "composer");
        if (COMPOSERS.putIfAbsent(blessing, composer) != null) {
            throw new IllegalArgumentException("Duplicate blessing effect composer: " + blessing);
        }
    }

    /** Also usable by CPU diagnostics: registers authors without touching Core, Minecraft, or GL. */
    public static synchronized void registerBuiltinComposers() {
        if (!portalRegistered) {
            // Ability cues are vanilla sprites. A traversable portal still needs its surface.
            CelestialBlessingEffects.registerPortal();
            portalRegistered = true;
        }
    }

    /** Snapshot of currently registered mesh authors; particle-only definitions are intentionally absent. */
    public static Set<ResourceLocation> registeredBlessings() {
        return Set.copyOf(COMPOSERS.keySet());
    }

    @FunctionalInterface
    public interface EffectComposer {
        void compose(Mesh mesh, Frame frame);
    }

    private static void render(WorldRenderContext context) {
        ResourceLocation blessing = fullId(context.effect().spec().parameters().getString("BlessingId"));
        if (blessing == null || !COMPOSERS.containsKey(blessing)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !worldPosition(context.center()) || !worldPosition(context.camera().getPosition())) return;
        CompoundTag params = context.effect().spec().parameters();
        double age = (double) mc.level.getGameTime() - context.effect().startGameTime()
                + clamp(mc.getFrameTime(), 0, 1);
        double distance = context.camera().getPosition().distanceTo(context.center());
        int detail = switch (RenderLibConfig.CLIENT.quality.get()) {
            case LOW -> 0;
            case MEDIUM -> 1;
            case HIGH, ULTRA -> 2;
        };
        ParticleStatus particles = mc.options.particles().get();
        if (particles == ParticleStatus.DECREASED) detail = Math.min(detail, 1);
        if (particles == ParticleStatus.MINIMAL) detail = 0;
        if (distance > 24) detail = Math.min(detail, 1);
        if (distance > 48) detail = 0;
        double motion = clamp(mc.options.screenEffectScale().get(), 0, 1)
                * clamp(RenderLibConfig.CLIENT.animationStrength.get(), 0, 1);
        boolean reducedFlashing = RenderLibConfig.CLIENT.reducedFlashing.get();
        if (reducedFlashing) motion *= 0.4;
        int primary = context.color();
        double shapeRadius = params.contains("ShapeRadius", Tag.TAG_ANY_NUMERIC)
                ? params.getDouble("ShapeRadius") : context.radius();
        Frame frame = new Frame(blessing, params.getString("Cue"), shapeRadius, age,
                context.effect().spec().durationTicks(), motion, detail,
                vector(params, "Direction", FORWARD), vector(params, "Link", null),
                primary, color(params, "Accent", primary), color(params, "Highlight", primary),
                context.effect().spec().seed());
        Mesh mesh = build(frame);
        if (mesh.cost == 0) return;
        Vec3 camera = context.camera().getPosition().subtract(context.center());
        double surfaceScale = (0.3 + 0.7 * smooth(distance / (frame.radius + 1)))
                * (reducedFlashing ? 0.65 : 1);
        PoseStack pose = context.pose();
        pose.pushPose();
        try {
            // Core already subtracts the camera. All stored vertices are anchor-relative offsets.
            pose.translate(context.center().x, context.center().y, context.center().z);
            if (!mesh.quads.isEmpty()) {
                VertexConsumer buffer = context.buffers().getBuffer(context.quadRenderType());
                for (int i = 0; i < mesh.quads.size(); i += 4) {
                    double visibility = proximity(mesh.quads, i, 4, camera) * surfaceScale;
                    for (int j = 0; j < 4; j++) {
                        Vertex vertex = mesh.quads.get(i + j);
                        Vec3 p = vertex.position;
                        buffer.vertex(pose.last().pose(), (float) p.x, (float) p.y, (float) p.z)
                                .color(withAlpha(vertex.color, visibility)).endVertex();
                    }
                }
            }
            // A different getBuffer may flush the preceding consumer. Core owns all flushing.
            if (!mesh.lines.isEmpty()) {
                VertexConsumer buffer = context.buffers().getBuffer(context.lineRenderType());
                for (int i = 0; i < mesh.lines.size(); i += 2) {
                    Vertex a = mesh.lines.get(i), b = mesh.lines.get(i + 1);
                    Vec3 normal = unit(b.position.subtract(a.position), UP);
                    double visibility = proximity(mesh.lines, i, 2, camera) * (reducedFlashing ? 0.7 : 1);
                    lineVertex(buffer, pose, a, normal, visibility);
                    lineVertex(buffer, pose, b, normal, visibility);
                }
            }
        } finally {
            pose.popPose();
        }
    }

    private static void lineVertex(VertexConsumer buffer, PoseStack pose, Vertex vertex,
                                   Vec3 normal, double visibility) {
        Vec3 p = vertex.position;
        buffer.vertex(pose.last().pose(), (float) p.x, (float) p.y, (float) p.z)
                .color(withAlpha(vertex.color, visibility))
                .normal(pose.last().normal(), (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    /**
     * Exact CPU composition probe. Registers built-in authors, not the Core renderer.
     * Radius is ShapeRadius; link is a full world-space segment about the midpoint anchor.
     * Unknown and particle-only IDs return a valid zero-vertex result.
     */
    public static Diagnostics diagnose(ResourceLocation blessing, String cue, double radius,
                                       double ageTicks, long durationTicks, double motion, int detail,
                                       Vec3 direction, Vec3 link) {
        registerBuiltinComposers();
        return build(new Frame(blessing, cue, radius, ageTicks, durationTicks, motion, detail,
                direction, link, 0xFFFFFFFF, 0xFF90C8E0, 0xFFFFD980, 0)).diagnostics();
    }

    public static Diagnostics diagnose(String blessingId, String cue, double radius,
                                       double ageTicks, long durationTicks, double motion, int detail,
                                       Vec3 direction, Vec3 link) {
        return diagnose(fullId(blessingId), cue, radius, ageTicks, durationTicks, motion, detail, direction, link);
    }

    /** Legacy gallery adapter only. Variant locates a registered blessing; it never generates geometry. */
    public static Diagnostics diagnose(String god, String cue, int variant, double radius,
                                       double ageTicks, long durationTicks, double motion, int detail,
                                       Vec3 direction, Vec3 link) {
        ResourceLocation godId = god == null ? null
                : fullId(god.indexOf(':') >= 0 ? god : "divinity_engine:" + god);
        ResourceLocation blessing = null;
        if (godId != null && variant >= 0 && variant <= 4) {
            blessing = BlessingVisualProfiles.styles().entrySet().stream()
                    .filter(entry -> entry.getValue().god().equals(godId) && entry.getValue().variant() == variant)
                    .map(Map.Entry::getKey).sorted((a, b) -> a.toString().compareTo(b.toString()))
                    .findFirst().orElse(null);
        }
        return diagnose(blessing, cue, radius, ageTicks, durationTicks, motion, detail, direction, link);
    }

    public record Diagnostics(int generatedVertices, int quadVertices, int lineEndpoints,
                              int rejectedPrimitives, int budgetDrops, double maximumAnchorDistance,
                              long geometryFingerprint) {}

    private static Mesh build(Frame frame) {
        Mesh mesh = new Mesh(frame);
        if (!frame.visible || frame.blessing == null) return mesh;
        EffectComposer composer = COMPOSERS.get(frame.blessing);
        if (composer == null) return mesh;
        composer.compose(mesh, frame);
        mesh.fitBody();
        return mesh;
    }

    public enum Cue {
        CAST, IMPACT, WARD, HEAL, TRAIL, AURA, CONTROL, TELEPORT, MARK, RECALL;

        public static Cue parse(String value) {
            try {
                return value == null ? CAST : valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return CAST;
            }
        }
    }

    /** Immutable composer inputs. Cue is metadata: it performs no automatic placement or animation. */
    public static final class Frame {
        public final ResourceLocation blessing;
        public final Cue cue;
        public final int detail, primary, accent, highlight;
        public final double radius, phase, progress, fade, seedPhase, motion, growth;
        public final Vec3 forward, right, up, link;
        public final boolean visible, persistentGateway;

        public Frame(ResourceLocation blessing, String cue, double radius, double age, long duration,
                     double motion, int detail, Vec3 direction, Vec3 link,
                     int primary, int accent, int highlight, long seed) {
            this.blessing = blessing;
            this.cue = Cue.parse(cue);
            this.detail = Math.max(0, Math.min(2, detail));
            this.radius = clamp(radius, 0.15, MAX_RADIUS);
            this.primary = primary;
            this.accent = accent;
            this.highlight = highlight;
            boolean gateway = POCKET_DIMENSION.equals(blessing) && this.cue == Cue.MARK;
            boolean persistent = gateway && duration == Long.MAX_VALUE;
            persistentGateway = persistent;
            double lifetime = Math.max(1, Math.min(60, duration));
            // Anchored gateways live until removed; the short spell-animation cap must not hide them.
            visible = Double.isFinite(age) && (persistent ? age >= 0 : age > 0 && age < lifetime) && Double.isFinite(radius);
            double ageProgress = persistent ? 0.45 : visible ? age / lifetime : 0;
            this.motion = clamp(motion, 0, 1);
            progress = 0.45 + (ageProgress - 0.45) * this.motion;
            seedPhase = (seed & 1023L) / 1024.0 * TAU;
            phase = (visible ? age : 0) * 0.1 * this.motion + seedPhase;
            growth = 0.65 + 0.35 * smooth(progress);
            fade = gateway ? 1 : smooth(ageProgress / 0.15) * smooth((1 - ageProgress) / 0.3);
            forward = unit(direction, FORWARD);
            right = unit(UP.cross(forward), RIGHT);
            up = unit(forward.cross(right), UP);
            this.link = boundedLink(link);
        }

        public int count(int low, int high) {
            int lo = Math.max(0, Math.min(128, low));
            int hi = Math.max(lo, Math.min(128, high));
            return lo + (hi - lo) * detail / 2;
        }

        /** Normalized local XYZ to world-oriented anchor offset. No lift, flattening, or cue scale. */
        public Vec3 point(Vec3 local) {
            if (!finite(local)) return INVALID_POINT;
            return right.scale(local.x).add(up.scale(local.y)).add(forward.scale(local.z)).scale(radius);
        }
    }

    /**
     * One effect's bounded writer. Local methods are radius-scaled/facing-oriented and fitted as a body.
     * Raw methods take world-oriented block offsets from the anchor and are NEVER body-fitted.
     */
    public static final class Mesh {
        public final Frame frame;
        public final int budget;
        private final List<Vertex> quads = new ArrayList<>();
        private final List<Vertex> lines = new ArrayList<>();
        private int cost, rejected, dropped;

        public Mesh(Frame frame) {
            this.frame = Objects.requireNonNull(frame, "frame");
            budget = switch (frame.detail) {
                case 0 -> 640;
                case 1 -> 1152;
                default -> MAX_VERTICES;
            };
        }

        /** A readable local quad stroke, not a hairline. Charged exactly four vertices. */
        public void line(Vec3 a, Vec3 b, int color) {
            if (!finite(a) || !finite(b)) { rejected++; return; }
            if (a.distanceToSqr(b) < 1.0E-12) return;
            Vec3 side = strokeSide(a, b).scale(0.024);
            emitQuad(frame.point(a.subtract(side)), frame.point(a.add(side)),
                    frame.point(b.add(side)), frame.point(b.subtract(side)), color, 0.42, true);
        }

        public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
            emitQuad(frame.point(a), frame.point(b), frame.point(c), frame.point(d), color, 0.24, true);
        }

        public void triangle(Vec3 a, Vec3 b, Vec3 c, int color) {
            quad(a, b, c, c, color);
        }

        public void ribbon(Vec3 a, Vec3 b, double width, int color) {
            if (!finite(a) || !finite(b) || !Double.isFinite(width)) { rejected++; return; }
            if (a.distanceToSqr(b) < 1.0E-12) return;
            Vec3 side = strokeSide(a, b).scale(clamp(width, 0.018, 2));
            quad(a.subtract(side), a.add(side), b.add(side), b.subtract(side), color);
        }

        private Vec3 strokeSide(Vec3 a, Vec3 b) {
            Vec3 direction = unit(b.subtract(a), UP);
            Vec3 side = direction.cross(FORWARD);
            return unit(side.lengthSqr() < 0.01 ? direction.cross(UP) : side, RIGHT);
        }

        public boolean room(int vertices) {
            return vertices >= 0 && vertices <= budget - cost;
        }

        /** World-oriented offsets, in blocks. These points bypass Frame.point and body fitting. */
        public void rawLine(Vec3 a, Vec3 b, int color) {
            if (!valid(a) || !valid(b)) { rejected++; return; }
            if (a.distanceToSqr(b) < 1.0E-12) return;
            int ink = withAlpha(color, frame.fade * 0.62);
            if ((ink >>> 24) == 0 || !reserve()) return;
            lines.add(new Vertex(a, ink, false));
            lines.add(new Vertex(b, ink, false));
        }

        /** World-oriented offsets, in blocks. These points bypass Frame.point and body fitting. */
        public void rawQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
            emitQuad(a, b, c, d, color, 0.24, false);
        }

        private void emitQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, double opacity, boolean local) {
            if (!valid(a) || !valid(b) || !valid(c) || !valid(d)) { rejected++; return; }
            int ink = withAlpha(color, frame.fade * opacity);
            if ((ink >>> 24) == 0 || !reserve()) return;
            quads.add(new Vertex(a, ink, local));
            quads.add(new Vertex(b, ink, local));
            quads.add(new Vertex(c, ink, local));
            quads.add(new Vertex(d, ink, local));
        }

        private boolean reserve() {
            // Quads and vanilla expanded lines both cost four; never submit a partial primitive.
            if (!room(4)) { dropped++; return false; }
            cost += 4;
            return true;
        }

        private void fitBody() {
            double maximum = frame.radius;
            for (Vertex vertex : quads) {
                if (vertex.local) maximum = Math.max(maximum, vertex.position.length());
            }
            for (Vertex vertex : lines) {
                if (vertex.local) maximum = Math.max(maximum, vertex.position.length());
            }
            if (maximum <= frame.radius) return;
            double scale = frame.radius / maximum;
            // Provenance, not list position, makes interleaved local/raw authoring safe.
            for (List<Vertex> vertices : List.of(quads, lines)) {
                for (int i = 0; i < vertices.size(); i++) {
                    Vertex vertex = vertices.get(i);
                    if (vertex.local) {
                        vertices.set(i, new Vertex(vertex.position.scale(scale), vertex.color, true));
                    }
                }
            }
        }

        public Diagnostics diagnostics() {
            long fingerprint = 0xcbf29ce484222325L;
            double maximumDistance = 0;
            for (List<Vertex> vertices : List.of(quads, lines)) {
                fingerprint = hash(fingerprint, vertices.size());
                for (Vertex vertex : vertices) {
                    maximumDistance = Math.max(maximumDistance, vertex.position.length());
                    fingerprint = hash(fingerprint, Double.doubleToLongBits(vertex.position.x));
                    fingerprint = hash(fingerprint, Double.doubleToLongBits(vertex.position.y));
                    fingerprint = hash(fingerprint, Double.doubleToLongBits(vertex.position.z));
                }
            }
            // Alpha is omitted so reduced-motion geometry can be compared across interior fade samples.
            return new Diagnostics(cost, quads.size(), lines.size(), rejected, dropped, maximumDistance, fingerprint);
        }
    }

    private record Vertex(Vec3 position, int color, boolean local) {}

    @FunctionalInterface
    public interface Path {
        Vec3 at(double t);
    }

    public static void arc(Mesh m, Vec3 center, Vec3 u, Vec3 v, double radius,
                           double start, double sweep, int segments, double width, int color) {
        curve(m, t -> center.add(u.scale(Math.cos(start + sweep * t) * radius))
                .add(v.scale(Math.sin(start + sweep * t) * radius)), Math.min(8, segments), width, color);
    }

    public static void tile(Mesh m, Vec3 center, double halfWidth, double halfHeight, int color) {
        m.quad(center.add(-halfWidth, -halfHeight, 0), center.add(halfWidth, -halfHeight, 0),
                center.add(halfWidth, halfHeight, 0), center.add(-halfWidth, halfHeight, 0), color);
    }

    public static void block(Mesh m, Vec3 p, double x, double y, double z,
                             int front, int side, int top) {
        // Open three-face relief, not a closed shell.
        tile(m, p.add(0, 0, -z), x, y, front);
        m.quad(p.add(x, -y, -z), p.add(x, -y, z), p.add(x, y, z), p.add(x, y, -z), side);
        m.quad(p.add(-x, y, -z), p.add(x, y, -z), p.add(x, y, z), p.add(-x, y, z), top);
    }

    public static void curve(Mesh m, Path path, int segments, double width, int color) {
        int steps = Math.max(1, Math.min(64, segments));
        Vec3 previous = path.at(0);
        for (int i = 1; i <= steps; i++) {
            Vec3 next = path.at(i / (double) steps);
            if (width > 0) {
                double taper = 0.18 + Math.sin(Math.PI * (i - 0.5) / steps) * 0.82;
                m.ribbon(previous, next, width * taper, color);
            }
            m.line(previous, next, color);
            previous = next;
        }
    }

    public static void crystal(Mesh m, Vec3 center, double width, double height, int sides,
                               int surface, int edge) {
        int faces = Math.max(3, Math.min(8, sides));
        Vec3 top = center.add(0, height, 0), bottom = center.add(0, -height * 0.5, 0);
        for (int i = 0; i < faces; i++) {
            Vec3 a = center.add(xz(TAU * i / faces, width));
            Vec3 b = center.add(xz(TAU * (i + 1) / faces, width));
            m.triangle(a, b, top, surface);
            m.triangle(b, a, bottom, surface);
            m.line(a, top, edge);
            m.line(a, b, edge);
            if (m.frame.detail > 0) m.line(a, bottom, edge);
        }
    }

    public static void star(Mesh m, Vec3 center, double radius, int points, double rotation, int color) {
        int arms = Math.max(3, Math.min(8, points));
        tile(m, center, radius * 0.25, radius * 0.25, color);
        for (int i = 0; i < arms; i++) {
            double a = TAU * i / arms + rotation;
            Vec3 axis = xy(a, 1), side = xy(a + Math.PI / 2, radius * 0.18);
            Vec3 root = center.add(axis.scale(radius * 0.25)), tip = center.add(axis.scale(radius));
            m.quad(root.subtract(side), tip.subtract(side), tip.add(side), root.add(side), color);
        }
    }

    public static void leaf(Mesh m, Vec3 p, double angle, double size, int color) {
        Vec3 axis = xz(angle, size).add(0, size * 0.7, 0), side = xz(angle + Math.PI / 2, size * 0.42);
        m.quad(p, p.add(axis.scale(0.4)).add(side), p.add(axis.scale(1.5)).add(side), p.add(axis.scale(2)), color);
        m.quad(p, p.add(axis.scale(2)), p.add(axis.scale(1.5)).subtract(side), p.add(axis.scale(0.4)).subtract(side), color);
        m.line(p, p.add(axis.scale(2)), color);
    }

    /** A single chunky glyph component; glyph is an ornament selector, never a blessing variant. */
    public static void rune(Mesh m, Vec3 p, double size, int glyph, int color) {
        tile(m, p.add(-size * 0.6, 0, 0), size * 0.2, size, color);
        tile(m, p.add(size * 0.2, size * 0.8, 0), size * 0.6, size * 0.2, color);
        tile(m, p.add(size * 0.2, glyph % 2 == 0 ? 0 : -size * 0.8, 0), size * 0.6, size * 0.2, color);
        tile(m, p.add(size * 0.6, glyph % 3 == 0 ? -size * 0.4 : size * 0.4, 0), size * 0.2, size * 0.2, color);
    }

    private static ResourceLocation fullId(String value) {
        return value == null || value.indexOf(':') <= 0 ? null : ResourceLocation.tryParse(value);
    }

    private static Vec3 vector(CompoundTag tag, String prefix, Vec3 fallback) {
        if (!tag.contains(prefix + "X", Tag.TAG_ANY_NUMERIC)
                || !tag.contains(prefix + "Y", Tag.TAG_ANY_NUMERIC)
                || !tag.contains(prefix + "Z", Tag.TAG_ANY_NUMERIC)) return fallback;
        Vec3 value = new Vec3(tag.getDouble(prefix + "X"), tag.getDouble(prefix + "Y"), tag.getDouble(prefix + "Z"));
        return finite(value) ? value : fallback;
    }

    private static int color(CompoundTag tag, String name, int fallback) {
        return tag.contains(name, Tag.TAG_ANY_NUMERIC) ? tag.getInt(name) : fallback;
    }

    private static Vec3 boundedLink(Vec3 value) {
        if (!finite(value)) return null;
        double largest = Math.max(Math.abs(value.x), Math.max(Math.abs(value.y), Math.abs(value.z)));
        if (largest < 1.0E-6) return null;
        Vec3 scaled = value.scale(1 / largest);
        double scaledLength = scaled.length();
        return largest > MAX_LINK_LENGTH / scaledLength
                ? scaled.scale(MAX_LINK_LENGTH / scaledLength) : value;
    }

    public static Vec3 unit(Vec3 value, Vec3 fallback) {
        if (!finite(value)) return fallback;
        double largest = Math.max(Math.abs(value.x), Math.max(Math.abs(value.y), Math.abs(value.z)));
        if (largest < 1.0E-9) return fallback;
        Vec3 scaled = value.scale(1 / largest);
        return scaled.scale(1 / scaled.length());
    }

    public static Vec3 xy(double angle, double radius) {
        return new Vec3(Math.cos(angle) * radius, Math.sin(angle) * radius, 0);
    }

    public static Vec3 xz(double angle, double radius) {
        return new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    public static Vec3 square(double angle, double radius) {
        double t = fraction(angle / TAU) * 4;
        if (t < 1) return new Vec3(radius, 0, radius * (t * 2 - 1));
        if (t < 2) return new Vec3(radius * (3 - t * 2), 0, radius);
        if (t < 3) return new Vec3(-radius, 0, radius * (5 - t * 2));
        return new Vec3(radius * (t * 2 - 7), 0, -radius);
    }

    private static boolean finite(Vec3 p) {
        return p != null && Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z);
    }

    private static boolean valid(Vec3 p) {
        return finite(p) && Math.abs(p.x) <= 64 && Math.abs(p.y) <= 64 && Math.abs(p.z) <= 64;
    }

    private static boolean worldPosition(Vec3 p) {
        return finite(p) && Math.abs(p.x) <= 3.2E7 && Math.abs(p.y) <= 3.2E7 && Math.abs(p.z) <= 3.2E7;
    }

    public static double clamp(double value, double low, double high) {
        return Double.isFinite(value) ? Math.max(low, Math.min(high, value)) : low;
    }

    public static double smooth(double value) {
        double t = clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static double fraction(double value) {
        return value - Math.floor(value);
    }

    private static int withAlpha(int color, double multiplier) {
        int alpha = (int) Math.round((color >>> 24) * clamp(multiplier, 0, 1));
        return (color & 0xFFFFFF) | (alpha << 24);
    }

    private static long hash(long hash, long value) {
        return (hash ^ value) * 0x100000001b3L;
    }

    private static double proximity(List<Vertex> vertices, int first, int count, Vec3 camera) {
        if (!finite(camera)) return 0;
        Vec3 start = vertices.get(first).position;
        if (!valid(start)) return 0;
        double minX = start.x, maxX = start.x, minY = start.y, maxY = start.y, minZ = start.z, maxZ = start.z;
        for (int i = 1; i < count; i++) {
            Vec3 p = vertices.get(first + i).position;
            if (!valid(p)) return 0;
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
            minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
        }
        double dx = Math.max(0, Math.max(minX - camera.x, camera.x - maxX));
        double dy = Math.max(0, Math.max(minY - camera.y, camera.y - maxY));
        double dz = Math.max(0, Math.max(minZ - camera.z, camera.z - maxZ));
        double boundsDistanceSquared = dx * dx + dy * dy + dz * dz;
        if (!Double.isFinite(boundsDistanceSquared)) return 0;
        // Bounds are only a cheap lower bound: being inside them does not mean touching the geometry.
        if (boundsDistanceSquared >= 0.85 * 0.85) return 1;
        Vec3 b = vertices.get(first + 1).position;
        double distanceSquared;
        if (count == 2) {
            distanceSquared = segmentDistanceSquared(camera, start, b);
        } else {
            Vec3 c = vertices.get(first + 2).position, d = vertices.get(first + 3).position;
            // Match vanilla QUADS triangulation, including triangle quads whose last two points coincide.
            distanceSquared = Math.min(triangleDistanceSquared(camera, start, b, c),
                    triangleDistanceSquared(camera, start, c, d));
        }
        return Double.isFinite(distanceSquared)
                ? smooth((Math.sqrt(Math.max(0, distanceSquared)) - 0.35) / 0.5) : 0;
    }

    static double segmentDistanceSquared(Vec3 p, Vec3 a, Vec3 b) {
        double x = b.x - a.x, y = b.y - a.y, z = b.z - a.z;
        double px = p.x - a.x, py = p.y - a.y, pz = p.z - a.z;
        double lengthSquared = x * x + y * y + z * z;
        double t = lengthSquared > 0 ? clamp((px * x + py * y + pz * z) / lengthSquared, 0, 1) : 0;
        double dx = px - x * t, dy = py - y * t, dz = pz - z * t;
        return dx * dx + dy * dy + dz * dz;
    }

    static double triangleDistanceSquared(Vec3 p, Vec3 a, Vec3 b, Vec3 c) {
        double abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
        double acx = c.x - a.x, acy = c.y - a.y, acz = c.z - a.z;
        double px = p.x - a.x, py = p.y - a.y, pz = p.z - a.z;
        double nx = aby * acz - abz * acy, ny = abz * acx - abx * acz, nz = abx * acy - aby * acx;
        double areaSquared = nx * nx + ny * ny + nz * nz;
        double abSquared = abx * abx + aby * aby + abz * abz;
        double acSquared = acx * acx + acy * acy + acz * acz;
        if (areaSquared > 1.0E-24 * abSquared * acSquared) {
            // Unnormalized barycentrics of the plane projection avoid division for outside points.
            double u = (py * acz - pz * acy) * nx + (pz * acx - px * acz) * ny
                    + (px * acy - py * acx) * nz;
            double v = (aby * pz - abz * py) * nx + (abz * px - abx * pz) * ny
                    + (abx * py - aby * px) * nz;
            if (u >= 0 && v >= 0 && u + v <= areaSquared) {
                double normalDistance = px * nx + py * ny + pz * nz;
                return normalDistance * normalDistance / areaSquared;
            }
        }
        // Outside, collinear and repeated-point triangles reduce safely to their edges.
        return Math.min(segmentDistanceSquared(p, a, b),
                Math.min(segmentDistanceSquared(p, b, c), segmentDistanceSquared(p, c, a)));
    }
}
