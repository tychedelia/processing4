package processing.webgpu;

/**
 * A dynamic-topology target: kernels add points, lines, or triangles to it
 * via the WESL {@code processing::prims} module, and
 * {@link PGraphicsWebGPU#particles(PPrimitives)} draws whatever was added
 * this frame. Create with {@link PParticlesWebGPU#primitives}; fill with
 * {@link PParticlesWebGPU#apply(processing.core.PCompute, PPrimitives)}.
 */
public class PPrimitives {

    private final long target;
    private final long field;
    private final int topology;
    private final int capacity;

    PPrimitives(long target, long field, int topology, int capacity) {
        this.target = target;
        this.field = field;
        this.topology = topology;
        this.capacity = capacity;
    }

    long target() {
        return target;
    }

    long field() {
        return field;
    }

    int topology() {
        return topology;
    }

    /** Capacity in primitives. */
    public int capacity() {
        return capacity;
    }

    /**
     * Primitives the kernels tried to add this frame, including any dropped
     * because the target was full. Reads back a small GPU stat; call it when
     * you want the number rather than every frame.
     */
    public int emitted() {
        return PWebGPU.particlesPrimitivesAttempted(target);
    }

    /**
     * True when kernels tried to add more primitives than the capacity this
     * frame (the excess was dropped).
     */
    public boolean overflowed() {
        return emitted() > capacity;
    }
}
