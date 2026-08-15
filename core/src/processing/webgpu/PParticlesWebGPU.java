package processing.webgpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import processing.core.PBuffer;
import processing.core.PCompute;
import processing.core.PMaterial;
import processing.core.PShape;

import static processing.core.PConstants.*;

public class PParticlesWebGPU implements processing.core.PParticles {

    // ── Bounds modes ────────────────────────────────────────────────────
    public static final int CLAMP   = 0;
    public static final int REFLECT = 1;
    public static final int WRAP    = 2;
    public static final int SOFT    = 3;

    // ── Falloff modes (Attract/Vortex/Impulse/Field falloff) ────────────
    public static final int CONSTANT   = 0;
    public static final int LINEAR     = 1;
    public static final int SMOOTHSTEP = 2;
    public static final int QUADRATIC  = 3;
    public static final int CUBIC      = 4;
    public static final int INVERSE    = 5;

    // ── Combine ops (ATTR_COMBINE) ──────────────────────────────────────
    public static final int ADD      = 0;
    public static final int SUBTRACT = 1;
    public static final int MULTIPLY = 2;
    public static final int DIVIDE   = 3;
    public static final int MIN      = 4;
    public static final int MAX      = 5;
    public static final int POW      = 6;

    private long id;

    private float dt = 1.0f / 60.0f;
    private final Random rng = new Random();
    private PMaterial defaultMaterial;
    private PShape defaultGeometry;

    private final Map<Integer, PComputeWebGPU> builtins = new HashMap<>();
    private PComputeWebGPU boxBounds;
    private float noiseTime = 0;

    private PComputeWebGPU builtin(int kind) {
        return builtins.computeIfAbsent(kind, k -> newKernel(k));
    }

    private static PComputeWebGPU newKernel(int kind) {
        PComputeWebGPU c = new PComputeWebGPU(kernelFactory(kind));
        c.flockKernel = kind == FLOCK;
        return c;
    }

    private static long kernelFactory(int kind) {
        return switch (kind) {
            case NOISE, CURL_NOISE -> PWebGPU.particlesKernelNoise();
            case FLOCK -> PWebGPU.particlesKernelFlock();
            case ATTRACT, REPEL -> PWebGPU.particlesKernelAttract();
            case VORTEX -> PWebGPU.particlesKernelVortex();
            case DRAG -> PWebGPU.particlesKernelDrag();
            case AGE -> PWebGPU.particlesKernelAge();
            case FORCE, GRAVITY -> PWebGPU.particlesKernelForce();
            case UPDATE, INTEGRATE -> PWebGPU.particlesKernelIntegrate();
            case BOUNDS -> PWebGPU.particlesKernelBoundsSphere();
            case TRANSFORM -> PWebGPU.particlesKernelTransform();
            case IMPULSE -> PWebGPU.particlesKernelImpulse();
            case ORIENT -> PWebGPU.particlesKernelOrient();
            case FIELD -> PWebGPU.particlesKernelField();
            case ATTR_LINEAR -> PWebGPU.particlesKernelAttrLinear();
            case ATTR_COMBINE -> PWebGPU.particlesKernelAttrCombine();
            case ATTR_MIX -> PWebGPU.particlesKernelAttrMix();
            case ATTR_LOOKUP1D -> PWebGPU.particlesKernelAttrLookup1D();
            case ATTR_LOOKUP2D -> PWebGPU.particlesKernelAttrLookup2D();
            default -> throw new IllegalArgumentException("Unknown kernel: " + kind);
        };
    }

    public PCompute createKernel(int kind) {
        return newKernel(kind);
    }

    PParticlesWebGPU(int capacity, Attribute... attributes) {
        long[] attrIds = new long[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attrIds[i] = attributes[i].id();
        }
        this.id = PWebGPU.particlesCreate(capacity, attrIds);
    }

    private PParticlesWebGPU(long id) {
        this.id = id;
    }

    static PParticlesWebGPU fromGeometryId(long geometryId, Attribute... attributes) {
        long[] attrIds = new long[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attrIds[i] = attributes[i].id();
        }
        return new PParticlesWebGPU(PWebGPU.particlesCreateFromGeometry(geometryId, attrIds));
    }

    long id() {
        return id;
    }

    public int capacity() {
        return PWebGPU.particlesCapacity(id);
    }

    public void attribute(Attribute attribute) {
        PWebGPU.particlesAttributeAdd(id, attribute.id());
    }

    public PBuffer buffer(Attribute attribute) {
        long bufferId = PWebGPU.particlesBuffer(id, attribute.id());
        if (bufferId == 0) return null;
        return new PBufferWebGPU(bufferId, true);
    }

    public PBuffer buffer(String name) {
        return buffer(attributeByName(name));
    }

    private static Attribute attributeByName(String name) {
        switch (name) {
            case "position": return Attribute.position();
            case "velocity": return Attribute.velocity();
            case "color":    return Attribute.color();
            case "scale":    return Attribute.scale();
            case "life":     return Attribute.life();
            case "age":      return Attribute.age();
            case "normal":   return Attribute.normal();
            case "uv":       return Attribute.uv();
            case "rotation": return Attribute.rotation();
            default:
                throw new IllegalArgumentException(
                    "\"" + name + "\" is not a built-in attribute; pass its Attribute to buffer(Attribute)");
        }
    }

    // ── Beginner verbs ──────────────────────────────────────────────────

    public void timeStep(float seconds) {
        this.dt = seconds;
    }

    public void scatter(float radius) {
        PBuffer positions = buffer(Attribute.position());
        if (positions == null) return;
        int cap = capacity();
        float[] data = new float[cap * 3];
        for (int i = 0; i < cap; i++) {
            double x, y, z, d2;
            do {
                x = rng.nextDouble() * 2 - 1;
                y = rng.nextDouble() * 2 - 1;
                z = rng.nextDouble() * 2 - 1;
                d2 = x * x + y * y + z * z;
            } while (d2 > 1.0 || d2 == 0.0);
            float r = (float) (radius * Math.cbrt(rng.nextDouble()) / Math.sqrt(d2));
            data[i * 3]     = (float) x * r;
            data[i * 3 + 1] = (float) y * r;
            data[i * 3 + 2] = (float) z * r;
        }
        positions.write(data);
    }

    public void update() {
        PComputeWebGPU c = builtin(INTEGRATE);
        c.set("dt", dt);
        apply(c);
    }

    public void applyForce(float x, float y, float z) {
        float mag = (float) Math.sqrt(x * x + y * y + z * z);
        if (mag == 0) return;
        PComputeWebGPU c = builtin(FORCE);
        c.set("direction", x, y, z);
        c.set("strength", mag * dt);
        apply(c);
    }

    public void applyForce(float x, float y) {
        applyForce(x, y, 0);
    }

    public void gravity(float x, float y, float z) {
        applyForce(x, y, z);
    }

    public void gravity(float x, float y) {
        applyForce(x, y, 0);
    }

    public void noise(float scale, float strength) {
        noiseTime += dt;
        applyNoise(scale, strength, noiseTime, false);
    }

    public void noise(float scale, float strength, float time) {
        noiseTime = time;
        applyNoise(scale, strength, time, false);
    }

    public void curlNoise(float scale, float strength) {
        noiseTime += dt;
        applyNoise(scale, strength, noiseTime, true);
    }

    private void applyNoise(float scale, float strength, float time, boolean curl) {
        PComputeWebGPU c = builtin(curl ? CURL_NOISE : NOISE);
        c.set("scale", scale);
        c.set("strength", strength);
        c.set("time", time);
        c.set("divergence_free", curl);
        apply(c);
    }

    public void flock() {
        apply(builtin(FLOCK));
    }

    public void flock(float neighborDistance, float separationDistance) {
        PComputeWebGPU c = builtin(FLOCK);
        c.set("neighbor_distance", neighborDistance);
        c.set("sep_distance", separationDistance);
        apply(c);
    }

    public void attract(float x, float y, float z, float strength) {
        PComputeWebGPU c = builtin(ATTRACT);
        c.set("center", x, y, z);
        c.set("strength", strength);
        apply(c);
    }

    public void attract(float x, float y, float z, float strength, float radius) {
        PComputeWebGPU c = builtin(ATTRACT);
        c.set("center", x, y, z);
        c.set("strength", strength);
        c.set("radius", radius);
        apply(c);
    }

    public void repel(float x, float y, float z, float strength) {
        attract(x, y, z, -strength);
    }

    public void repel(float x, float y, float z, float strength, float radius) {
        attract(x, y, z, -strength, radius);
    }

    public void vortex(float x, float y, float z, float strength) {
        PComputeWebGPU c = builtin(VORTEX);
        c.set("center", x, y, z);
        c.set("strength", strength);
        apply(c);
    }

    public void vortex(float x, float y, float z, float strength, float radius) {
        PComputeWebGPU c = builtin(VORTEX);
        c.set("center", x, y, z);
        c.set("strength", strength);
        c.set("radius", radius);
        apply(c);
    }

    public void drag(float amount) {
        PComputeWebGPU c = builtin(DRAG);
        c.set("damping", amount);
        apply(c);
    }

    public void age() {
        PComputeWebGPU c = builtin(AGE);
        c.set("dt", dt);
        apply(c);
    }

    public void bounds(float radius) {
        PComputeWebGPU c = builtin(BOUNDS);
        c.set("radius", radius);
        apply(c);
    }

    public void bounds(float width, float height, float depth) {
        if (boxBounds == null) {
            boxBounds = new PComputeWebGPU(PWebGPU.particlesKernelBoundsBox());
        }
        boxBounds.set("aabb_min", -width / 2, -height / 2, -depth / 2);
        boxBounds.set("aabb_max", width / 2, height / 2, depth / 2);
        apply(boxBounds);
    }

    public void bounds(PShape shape) {
        apply(new PComputeWebGPU(PWebGPU.particlesKernelBoundsGeometry(PShapeWebGPU.geometryId(shape))));
    }

    // ── Zero-config draw ────────────────────────────────────────────────

    PMaterial defaultMaterial() {
        if (defaultMaterial == null) {
            defaultMaterial = PMaterialWebGPU.unlit();
        }
        return defaultMaterial;
    }

    PShape defaultGeometry() {
        if (defaultGeometry == null) {
            defaultGeometry = PShapeWebGPU.createSphere(null, 2.0f, 8, 6);
        }
        return defaultGeometry;
    }

    public void apply(int kind, float... p) {
        switch (kind) {
            case NOISE      -> { need(kind, p, 2); noise(p[0], p[1]); }
            case CURL_NOISE -> { need(kind, p, 2); curlNoise(p[0], p[1]); }
            case FLOCK      -> { if (p.length >= 2) flock(p[0], p[1]); else flock(); }
            case ATTRACT    -> { need(kind, p, 4); if (p.length >= 5) attract(p[0], p[1], p[2], p[3], p[4]); else attract(p[0], p[1], p[2], p[3]); }
            case REPEL      -> { need(kind, p, 4); if (p.length >= 5) repel(p[0], p[1], p[2], p[3], p[4]); else repel(p[0], p[1], p[2], p[3]); }
            case VORTEX     -> { need(kind, p, 4); if (p.length >= 5) vortex(p[0], p[1], p[2], p[3], p[4]); else vortex(p[0], p[1], p[2], p[3]); }
            case DRAG       -> { need(kind, p, 1); drag(p[0]); }
            case AGE        -> age();
            case FORCE, GRAVITY -> { need(kind, p, 2); applyForce(p[0], p[1], p.length >= 3 ? p[2] : 0); }
            case UPDATE, INTEGRATE -> update();
            case BOUNDS     -> { need(kind, p, 1); bounds(p[0]); }
            default         -> apply(builtin(kind));
        }
    }

    private static void need(int kind, float[] p, int n) {
        if (p.length < n) {
            throw new IllegalArgumentException(
                "apply(kind " + kind + ", …) needs " + n + " parameters, got " + p.length);
        }
    }

    public void apply(PCompute compute) {
        PComputeWebGPU c = (PComputeWebGPU) compute;
        if (c.flockKernel) {
            // Flock reads the neighbor grid; this builds and binds it first.
            PWebGPU.particlesFlock(id, c.id());
        } else {
            PWebGPU.particlesApply(id, c.id());
        }
    }

    /**
     * Apply a compute kernel over this field with {@code primitives}' output
     * buffers bound under the WESL {@code processing::prims} reserved names,
     * so the kernel can add points/lines/triangles to the target.
     */
    public void apply(PCompute compute, PPrimitives primitives) {
        PWebGPU.particlesPrimitivesApply(primitives.target(), ((PComputeWebGPU) compute).id());
    }

    /**
     * Create a dynamic-topology target over this field. {@code topology} is
     * {@code "points"}, {@code "lines"}, or {@code "triangles"}; capacity is
     * counted in primitives. Kernels applied with
     * {@link #apply(PCompute, PPrimitives)} add primitives to it, and
     * {@link PGraphicsWebGPU#particles(PPrimitives)} draws them.
     */
    public PPrimitives primitives(String topology, int capacity) {
        int topo = switch (topology.toLowerCase()) {
            case "points" -> 0;
            case "lines" -> 1;
            case "triangles" -> 3;
            default -> throw new IllegalArgumentException(
                "primitives(): unknown topology \"" + topology + "\" (points, lines, or triangles)");
        };
        long target = PWebGPU.particlesPrimitivesCreate(id, topo, capacity);
        long field = PWebGPU.particlesPrimitivesField(target);
        return new PPrimitives(target, field, topo, capacity);
    }

    /**
     * Create a uniform spatial hash grid over this field for GPU neighbor
     * queries, sized to the field's capacity. The grid spans
     * {@code dims * cellSize} from {@code (minX, minY, minZ)}.
     */
    public PGrid createGrid(float minX, float minY, float minZ,
                            float cellSize, int dimsX, int dimsY, int dimsZ) {
        return new PGrid(
            PWebGPU.particlesGridCreate(id, minX, minY, minZ, cellSize, dimsX, dimsY, dimsZ),
            cellSize);
    }

    public void emit(int n, Map<Attribute, float[]> data) {
        if (data.isEmpty()) {
            PWebGPU.particlesEmit(id, n, new long[0], new byte[0], new long[0]);
            return;
        }

        long[] attrIds = new long[data.size()];
        long[] attrByteLengths = new long[data.size()];

        int totalBytes = 0;
        int i = 0;
        for (var entry : data.entrySet()) {
            attrIds[i] = entry.getKey().id();
            int byteLen = entry.getValue().length * 4;
            attrByteLengths[i] = byteLen;
            totalBytes += byteLen;
            i++;
        }

        ByteBuffer buf = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN);
        for (var entry : data.entrySet()) {
            buf.asFloatBuffer().put(entry.getValue());
            buf.position(buf.position() + entry.getValue().length * 4);
        }

        PWebGPU.particlesEmit(id, n, attrIds, buf.array(), attrByteLengths);
    }

    public void emit(int n, PCompute compute) {
        PWebGPU.particlesEmitGpu(id, n, ((PComputeWebGPU) compute).id());
    }

    // ── GPU emitters ────────────────────────────────────────────────────

    public static PCompute scatterSurface(PShape shape) {
        return new PComputeWebGPU(PWebGPU.particlesScatterCreate(PShapeWebGPU.geometryId(shape)));
    }

    public static PCompute scatterVolume(PShape shape) {
        return new PComputeWebGPU(PWebGPU.particlesScatterVolumeCreate(PShapeWebGPU.geometryId(shape)));
    }

    public void destroy() {
        if (id != 0) {
            PWebGPU.particlesDestroy(id);
            id = 0;
        }
    }
}
