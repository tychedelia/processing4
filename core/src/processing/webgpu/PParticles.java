package processing.webgpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Random;

import processing.core.PMaterial;
import processing.core.PShape;

import processing.webgpu.kernels.AgeKernel;
import processing.webgpu.kernels.AttrCombineKernel;
import processing.webgpu.kernels.AttrLinearKernel;
import processing.webgpu.kernels.AttrLookup1DKernel;
import processing.webgpu.kernels.AttrLookup2DKernel;
import processing.webgpu.kernels.AttrMixKernel;
import processing.webgpu.kernels.AttractKernel;
import processing.webgpu.kernels.BoundsKernel;
import processing.webgpu.kernels.DragKernel;
import processing.webgpu.kernels.FieldKernel;
import processing.webgpu.kernels.FlockKernel;
import processing.webgpu.kernels.ForceKernel;
import processing.webgpu.kernels.ImpulseKernel;
import processing.webgpu.kernels.IntegrateKernel;
import processing.webgpu.kernels.NoiseKernel;
import processing.webgpu.kernels.OrientKernel;
import processing.webgpu.kernels.TransformKernel;
import processing.webgpu.kernels.VortexKernel;

public class PParticles {

    // ── Bounds modes (BoundsKernel.mode) ────────────────────────────────
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

    // ── Combine ops (AttrCombineKernel.op) ──────────────────────────────
    public static final int ADD      = 0;
    public static final int SUBTRACT = 1;
    public static final int MULTIPLY = 2;
    public static final int DIVIDE   = 3;
    public static final int MIN      = 4;
    public static final int MAX      = 5;
    public static final int POW      = 6;

    private long id;

    // ── Beginner-layer convenience state ────────────────────────────────
    // A PParticles built by createParticles(n) starts with only `position`;
    // motion/lifecycle attributes materialize on the GPU as the kernels that
    // need them are applied (see PParticles.apply). These fields back the
    // verb helpers (scatter/update/applyForce) and the zero-config draw path.
    private float dt = 1.0f / 60.0f;
    private final Random rng = new Random();
    private PMaterial defaultMaterial;   // lazily built for particles(p)
    private PShape defaultGeometry;     // lazily built point sprite

    // Verbs (noise/flock/attract/…) cache their kernel on the system: built
    // once, re-parameterized and dispatched each call — so a verb in draw()
    // doesn't create a fresh compute pipeline every frame.
    private NoiseKernel noiseKernel;
    private FlockKernel flockKernel;
    private AttractKernel attractKernel;
    private VortexKernel vortexKernel;
    private DragKernel dragKernel;
    private AgeKernel ageKernel;
    private ForceKernel forceKernel;
    private IntegrateKernel integrateKernel;
    private BoundsKernel.Sphere boundsKernel;
    private float noiseTime = 0;

    public PParticles(int capacity, Attribute... attributes) {
        long[] attrIds = new long[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attrIds[i] = attributes[i].id();
        }
        this.id = PWebGPU.particlesCreate(capacity, attrIds);
    }

    private PParticles(long id) {
        this.id = id;
    }

    static PParticles fromGeometryId(long geometryId, Attribute... attributes) {
        long[] attrIds = new long[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attrIds[i] = attributes[i].id();
        }
        return new PParticles(PWebGPU.particlesCreateFromGeometry(geometryId, attrIds));
    }

    public long id() {
        return id;
    }

    public int capacity() {
        return PWebGPU.particlesCapacity(id);
    }

    /**
     * Declare a custom attribute on this system, allocating its per-particle
     * buffer. Built-in attributes materialize on demand, so this is only needed
     * for custom attributes referenced by your own WGSL.
     */
    public void attribute(Attribute attribute) {
        PWebGPU.particlesAttributeAdd(id, attribute.id());
    }

    public Buffer buffer(Attribute attribute) {
        long bufferId = PWebGPU.particlesBuffer(id, attribute.id());
        if (bufferId == 0) return null;
        return new Buffer(bufferId, true);
    }

    /**
     * The buffer for a well-known attribute by name — {@code "position"},
     * {@code "velocity"}, {@code "color"}, {@code "scale"}, {@code "life"},
     * {@code "age"}, {@code "normal"}, {@code "uv"}, {@code "rotation"}.
     * Returns {@code null} if the system hasn't grown that attribute yet.
     * For a custom attribute, pass its {@link Attribute} to {@link #buffer(Attribute)}.
     */
    public Buffer buffer(String name) {
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
    //
    // Thin, readable wrappers over the built-in kernels and buffers. Each one
    // "does a thing" to the whole system — the noun ambiguity of a single
    // particle never comes up because these read as verbs on the field.

    /** Seconds-per-step used by {@link #update()} and {@link #applyForce}. */
    public PParticles timeStep(float seconds) {
        this.dt = seconds;
        return this;
    }

    /**
     * Seed every slot with a random position inside a ball of {@code radius}
     * (centered on the origin), bringing the whole field to life at once — the
     * natural starting point for a swarm or flock. Writes only {@code position};
     * with no {@code life} attribute present, every slot renders.
     */
    public void scatter(float radius) {
        Buffer positions = buffer(Attribute.position());
        if (positions == null) return;
        int cap = capacity();
        float[] data = new float[cap * 3];
        for (int i = 0; i < cap; i++) {
            // uniform in a ball: random direction * radius * cbrt(u)
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

    /**
     * Advance the field one step: {@code position += velocity * dt}. Pair with
     * {@link #applyForce} (or any velocity-writing verb) applied earlier in the
     * frame. {@code velocity} materializes on first use.
     */
    public void update() {
        if (integrateKernel == null) integrateKernel = new IntegrateKernel();
        apply(integrateKernel.dt(dt));
    }

    /**
     * Add a constant acceleration to every particle's velocity this step —
     * gravity, wind, any uniform push. {@code velocity} materializes on first
     * use; follow with {@link #update()} to integrate it into position.
     */
    public void applyForce(float x, float y, float z) {
        float mag = (float) Math.sqrt(x * x + y * y + z * z);
        if (mag == 0) return;
        if (forceKernel == null) forceKernel = new ForceKernel();
        apply(forceKernel.direction(x, y, z).strength(mag * dt));
    }

    /** 2D convenience — {@code applyForce(x, y, 0)}. */
    public void applyForce(float x, float y) {
        applyForce(x, y, 0);
    }

    /** Constant downward-or-any acceleration — a readable alias for {@link #applyForce}. */
    public void gravity(float x, float y, float z) {
        applyForce(x, y, z);
    }

    /** 2D gravity — {@code gravity(x, y, 0)}. */
    public void gravity(float x, float y) {
        applyForce(x, y, 0);
    }

    /**
     * Displace every particle by a value-noise field — smooth, organic drift.
     * {@code scale} is the spatial frequency (smaller = larger features),
     * {@code strength} the displacement amount. Time advances automatically so
     * the field animates; use {@link #noise(float, float, float)} to drive it
     * yourself.
     */
    public void noise(float scale, float strength) {
        noiseTime += dt;
        applyNoise(scale, strength, noiseTime, false);
    }

    /** {@link #noise(float, float)} with an explicit time (no auto-advance). */
    public void noise(float scale, float strength, float time) {
        noiseTime = time;
        applyNoise(scale, strength, time, false);
    }

    /**
     * Divergence-free (curl) variant of {@link #noise} — particles flow along
     * streamlines without piling up. A little more expensive; usually worth it.
     */
    public void curlNoise(float scale, float strength) {
        noiseTime += dt;
        applyNoise(scale, strength, noiseTime, true);
    }

    private void applyNoise(float scale, float strength, float time, boolean curl) {
        if (noiseKernel == null) noiseKernel = new NoiseKernel();
        apply(noiseKernel.scale(scale).strength(strength).time(time).curl(curl));
    }

    /** Reynolds flocking with default weights — reads {@code position}, grows and writes {@code velocity}. */
    public void flock() {
        if (flockKernel == null) flockKernel = new FlockKernel();
        apply(flockKernel);
    }

    /**
     * Flocking tuned by its two most impactful knobs: {@code neighborDistance}
     * (how far a boid sees) and {@code separationDistance} (personal space).
     * For the full weight/speed set, drop to {@code apply(PParticles.flock()…)}.
     */
    public void flock(float neighborDistance, float separationDistance) {
        if (flockKernel == null) flockKernel = new FlockKernel();
        apply(flockKernel.nbrDistance(neighborDistance).sepDistance(separationDistance));
    }

    /** Pull particles toward a point (positive {@code strength}). */
    public void attract(float x, float y, float z, float strength) {
        if (attractKernel == null) attractKernel = new AttractKernel();
        apply(attractKernel.center(x, y, z).strength(strength));
    }

    /** {@link #attract} limited to particles within {@code radius}. */
    public void attract(float x, float y, float z, float strength, float radius) {
        if (attractKernel == null) attractKernel = new AttractKernel();
        apply(attractKernel.center(x, y, z).strength(strength).radius(radius));
    }

    /** Push particles away from a point — {@link #attract} with negated strength. */
    public void repel(float x, float y, float z, float strength) {
        attract(x, y, z, -strength);
    }

    /** {@link #repel} limited to particles within {@code radius}. */
    public void repel(float x, float y, float z, float strength, float radius) {
        attract(x, y, z, -strength, radius);
    }

    /** Swirl particles around a vertical axis through the given point. */
    public void vortex(float x, float y, float z, float strength) {
        if (vortexKernel == null) vortexKernel = new VortexKernel();
        apply(vortexKernel.center(x, y, z).strength(strength));
    }

    /** {@link #vortex} limited to particles within {@code radius}. */
    public void vortex(float x, float y, float z, float strength, float radius) {
        if (vortexKernel == null) vortexKernel = new VortexKernel();
        apply(vortexKernel.center(x, y, z).strength(strength).radius(radius));
    }

    /** Damp velocity by {@code amount} in [0, 1] each step — 0 = none, 1 = full stop. */
    public void drag(float amount) {
        if (dragKernel == null) dragKernel = new DragKernel();
        apply(dragKernel.damping(amount));
    }

    /** Advance per-particle {@code age}; particles past their {@code life} are culled. */
    public void age() {
        if (ageKernel == null) ageKernel = new AgeKernel();
        apply(ageKernel.dt(dt));
    }

    /** Keep particles within a sphere of {@code radius} around the origin. */
    public void bounds(float radius) {
        if (boundsKernel == null) boundsKernel = new BoundsKernel.Sphere();
        apply(boundsKernel.radius(radius));
    }

    /** Keep particles within a shape's bounding box. */
    public void bounds(PShape shape) {
        apply(new BoundsKernel.Box(PWebGPU.particlesKernelBoundsGeometry(PShapeWebGPU.geometryId(shape))));
    }

    // ── Zero-config draw ────────────────────────────────────────────────

    /** A plain unlit material for {@link #particles(PParticles)}-style drawing. */
    PMaterial defaultMaterial() {
        if (defaultMaterial == null) {
            defaultMaterial = PMaterialWebGPU.unlit();
        }
        return defaultMaterial;
    }

    /** A small sphere instanced over each particle when no shape is given. */
    PShape defaultGeometry() {
        if (defaultGeometry == null) {
            defaultGeometry = PShapeWebGPU.createSphere(null, 2.0f, 8, 6);
        }
        return defaultGeometry;
    }

    /** Dispatch a typed built-in kernel against this particle system. */
    public void apply(Kernel kernel) {
        PWebGPU.particlesApply(id, kernel.id());
    }

    /** Dispatch a raw {@link Compute} (custom WGSL) against this particle system. */
    public void apply(Compute compute) {
        PWebGPU.particlesApply(id, compute.id());
    }

    /**
     * CPU-driven emission. Writes per-attribute float data into the next {@code n}
     * ring-buffer slots.
     *
     * @param n    number of particles to emit
     * @param data map from attribute to flat float array (n * attribute.floatCount() elements)
     */
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

    public void emitGpu(int n, Compute compute) {
        PWebGPU.particlesEmitGpu(id, n, compute.id());
    }

    // ── Built-in kernel factories ───────────────────────────────────────
    //
    // Each factory returns a typed Kernel subclass with chainable, typed
    // setters for that kernel's uniforms and slot bindings — so users
    // configure with `flock().maxSpeed(0.1).maxForce(0.003)` rather than
    // stringly-typed `compute.set("max_speed", 0.1)`. Every Kernel can
    // be passed to {@link #apply(Kernel)}; the underlying {@link Compute}
    // is reachable via {@link Kernel#compute()} as an escape hatch.

    public static NoiseKernel noise()         { return new NoiseKernel(); }
    public static TransformKernel transform() { return new TransformKernel(); }
    public static AttractKernel attract()     { return new AttractKernel(); }
    public static DragKernel drag()           { return new DragKernel(); }
    public static VortexKernel vortex()       { return new VortexKernel(); }
    public static ForceKernel force()         { return new ForceKernel(); }
    public static IntegrateKernel integrate() { return new IntegrateKernel(); }
    public static ImpulseKernel impulse()     { return new ImpulseKernel(); }
    public static OrientKernel orient()       { return new OrientKernel(); }
    public static FieldKernel field()         { return new FieldKernel(); }
    public static AttrLinearKernel attrLinear()       { return new AttrLinearKernel(); }
    public static AttrCombineKernel attrCombine()     { return new AttrCombineKernel(); }
    public static AttrMixKernel attrMix()             { return new AttrMixKernel(); }
    public static AttrLookup1DKernel attrLookup1D()   { return new AttrLookup1DKernel(); }
    public static AttrLookup2DKernel attrLookup2D()   { return new AttrLookup2DKernel(); }

    public static BoundsKernel.Sphere boundsSphere() { return new BoundsKernel.Sphere(); }
    public static BoundsKernel.Box boundsBox()       { return new BoundsKernel.Box(); }

    /** Box bounds preloaded with a shape's AABB. */
    public static BoundsKernel.Box boundsShape(PShape shape) {
        return new BoundsKernel.Box(PWebGPU.particlesKernelBoundsGeometry(PShapeWebGPU.geometryId(shape)));
    }

    // ── GPU emitters ────────────────────────────────────────────────────
    //
    // Use with {@link #emitGpu(int, Compute)}. `seed` is a u32 uniform on
    // both — bump it across frames (or off `frameCount`) to avoid emitting
    // the same sample sequence each dispatch.

    /** GPU emitter that scatters particles across a shape's surface (triangles). */
    public static Compute scatterSurface(PShape shape) {
        return new Compute(PWebGPU.particlesScatterCreate(PShapeWebGPU.geometryId(shape)));
    }

    /** GPU emitter that scatters particles through a closed shape's volume. */
    public static Compute scatterVolume(PShape shape) {
        return new Compute(PWebGPU.particlesScatterVolumeCreate(PShapeWebGPU.geometryId(shape)));
    }

    public void destroy() {
        if (id != 0) {
            PWebGPU.particlesDestroy(id);
            id = 0;
        }
    }
}
