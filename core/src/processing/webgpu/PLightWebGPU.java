package processing.webgpu;

import processing.core.PLight;

/** WEBGPU implementation of {@link PLight}, wrapping a native light entity. */
public class PLightWebGPU implements PLight {

    private final long id;

    PLightWebGPU(long id) {
        this.id = id;
    }

    long id() {
        return id;
    }

    @Override
    public PLight position(float x, float y, float z) {
        PWebGPU.transformSetPosition(id, x, y, z);
        return this;
    }

    @Override
    public PLight direction(float x, float y, float z) {
        PWebGPU.transformSetRotation(id, x, y, z);
        return this;
    }

    @Override
    public PLight lookAt(float x, float y, float z) {
        PWebGPU.transformLookAt(id, x, y, z);
        return this;
    }
}
