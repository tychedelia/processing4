package processing.webgpu;

import processing.core.PMaterial;

/** WEBGPU implementation of {@link PMaterial}, wrapping a native material. */
public class PMaterialWebGPU implements PMaterial {

    private long id;

    private PMaterialWebGPU(long id) {
        this.id = id;
    }

    public static PMaterial pbr() {
        return new PMaterialWebGPU(PWebGPU.materialCreatePbr());
    }

    public static PMaterial unlit() {
        PMaterial mat = pbr();
        mat.set("unlit", 1.0f);
        return mat;
    }

    long id() {
        return id;
    }

    @Override
    public void set(String name, float value) {
        PWebGPU.materialSetFloat(id, name, value);
    }

    @Override
    public void set(String name, float r, float g, float b, float a) {
        PWebGPU.materialSetFloat4(id, name, r, g, b, a);
    }

    @Override
    public void setAlbedo(float r, float g, float b, float a) {
        PWebGPU.materialSetAlbedoColor(id, r, g, b, a);
    }

    /** Bind a per-particle color buffer as the albedo source (WEBGPU-specific). */
    public void setAlbedo(Buffer colorBuffer) {
        PWebGPU.materialSetAlbedoBuffer(id, colorBuffer.id());
    }

    /** Bind a per-particle emissive buffer (WEBGPU-specific). */
    public void setEmissive(Buffer emissiveBuffer) {
        PWebGPU.materialSetEmissiveBuffer(id, emissiveBuffer.id());
    }

    @Override
    public void destroy() {
        if (id != 0) {
            PWebGPU.materialDestroy(id);
            id = 0;
        }
    }
}
