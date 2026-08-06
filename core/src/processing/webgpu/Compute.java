package processing.webgpu;

import processing.core.PBuffer;

public class Compute {

    private long id;

    public Compute(Shader shader) {
        this.id = PWebGPU.computeCreate(shader.id());
    }

    Compute(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    public void set(String name, float value) {
        PWebGPU.computeSetFloat(id, name, value);
    }

    public void set(String name, float x, float y) {
        PWebGPU.computeSetFloat2(id, name, x, y);
    }

    public void set(String name, float x, float y, float z) {
        PWebGPU.computeSetFloat3(id, name, x, y, z);
    }

    public void set(String name, float x, float y, float z, float w) {
        PWebGPU.computeSetFloat4(id, name, x, y, z, w);
    }

    public void set(String name, float[] matrix) {
        PWebGPU.computeSetMat4(id, name, matrix);
    }

    /**
     * Set a {@code u32} uniform. Use this for shader fields like enum
     * selectors / mode flags. Java's {@code int} literals (e.g. {@code 3})
     * resolve to this overload before being promoted to float.
     */
    public void set(String name, int value) {
        PWebGPU.computeSetUInt(id, name, value);
    }

    public void set(String name, PBuffer buffer) {
        PWebGPU.computeSetBuffer(id, name, ((PBufferWebGPU) buffer).id());
    }

    public void set(String name, PImageWebGPU image) {
        PWebGPU.computeSetTexture(id, name, image.getId());
    }

    public void dispatch(int x, int y, int z) {
        PWebGPU.computeDispatch(id, x, y, z);
    }

    public void destroy() {
        if (id != 0) {
            PWebGPU.computeDestroy(id);
            id = 0;
        }
    }
}
