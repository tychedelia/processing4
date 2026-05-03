package processing.webgpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

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

    public static Compute kernelNoise() {
        return new Compute(PWebGPU.particlesKernelNoise());
    }

    public static Compute kernelTransform() {
        return new Compute(PWebGPU.particlesKernelTransform());
    }

    public void destroy() {
        if (id != 0) {
            PWebGPU.particlesDestroy(id);
            id = 0;
        }
    }
}
