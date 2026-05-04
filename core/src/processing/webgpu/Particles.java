package processing.webgpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

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
import processing.webgpu.kernels.ImpulseKernel;
import processing.webgpu.kernels.NoiseKernel;
import processing.webgpu.kernels.OrientKernel;
import processing.webgpu.kernels.TransformKernel;
import processing.webgpu.kernels.VortexKernel;

public class Particles {

    private long id;

    public Particles(int capacity, Attribute... attributes) {
        long[] attrIds = new long[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attrIds[i] = attributes[i].id();
        }
        this.id = PWebGPU.particlesCreate(capacity, attrIds);
    }

    private Particles(long id) {
        this.id = id;
    }

    public static Particles fromGeometry(Geometry geometry, Attribute... attributes) {
        long[] attrIds = new long[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attrIds[i] = attributes[i].id();
        }
        return new Particles(PWebGPU.particlesCreateFromGeometry(geometry.id(), attrIds));
    }

    public long id() {
        return id;
    }

    public int capacity() {
        return PWebGPU.particlesCapacity(id);
    }

    public Buffer buffer(Attribute attribute) {
        long bufferId = PWebGPU.particlesBuffer(id, attribute.id());
        if (bufferId == 0) return null;
        return new Buffer(bufferId, true);
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
    public static BoundsKernel bounds()       { return new BoundsKernel(); }
    public static ImpulseKernel impulse()     { return new ImpulseKernel(); }
    public static FlockKernel flock()         { return new FlockKernel(); }
    public static OrientKernel orient()       { return new OrientKernel(); }
    public static FieldKernel field()         { return new FieldKernel(); }
    public static AttrLinearKernel attrLinear()       { return new AttrLinearKernel(); }
    public static AttrCombineKernel attrCombine()     { return new AttrCombineKernel(); }
    public static AttrMixKernel attrMix()             { return new AttrMixKernel(); }
    public static AttrLookup1DKernel attrLookup1D()   { return new AttrLookup1DKernel(); }
    public static AttrLookup2DKernel attrLookup2D()   { return new AttrLookup2DKernel(); }

    public void destroy() {
        if (id != 0) {
            PWebGPU.particlesDestroy(id);
            id = 0;
        }
    }
}
