package processing.webgpu;

import processing.core.PBuffer;
import processing.core.PMaterial;

public class PMaterialWebGPU extends PUniformsWebGPU implements PMaterial {

    private long id;

    private PMaterialWebGPU(long id) {
        this.id = id;
    }

    static PMaterial fromId(long id) {
        return new PMaterialWebGPU(id);
    }

    static PMaterial pbr() {
        return new PMaterialWebGPU(PWebGPU.materialCreatePbr());
    }

    static PMaterial unlit() {
        PMaterial mat = pbr();
        mat.unlit(true);
        return mat;
    }

    static PMaterial custom(long shaderId) {
        return new PMaterialWebGPU(PWebGPU.materialCreateCustom(shaderId));
    }

    @Override
    long id() {
        return id;
    }

    @Override
    public void albedo(float r, float g, float b, float a) {
        PWebGPU.materialSetFloat4(id, "color", r, g, b, a);
    }

    @Override
    public void metalness(float value) {
        PWebGPU.materialSetFloat(id, "metallic", value);
    }

    @Override
    public void roughness(float value) {
        PWebGPU.materialSetFloat(id, "roughness", value);
    }

    @Override
    public void reflectance(float value) {
        PWebGPU.materialSetFloat(id, "reflectance", value);
    }

    @Override
    public void emissive(float r, float g, float b, float a) {
        PWebGPU.materialSetFloat4(id, "emissive", r, g, b, a);
    }

    public void albedo(PBuffer colorBuffer) {
        PWebGPU.materialSetAlbedoBuffer(id, ((PBufferWebGPU) colorBuffer).id());
    }

    public void emissive(PBuffer emissiveBuffer) {
        PWebGPU.materialSetEmissiveBuffer(id, ((PBufferWebGPU) emissiveBuffer).id());
    }

    @Override
    public void opaque() {
        PWebGPU.materialSetAlphaMode(id, 0, 0f);
    }

    @Override
    public void transparent() {
        PWebGPU.materialSetAlphaMode(id, 2, 0f);
    }

    @Override
    public void mask(float cutoff) {
        PWebGPU.materialSetAlphaMode(id, 1, cutoff);
    }

    @Override
    public void doubleSided(boolean value) {
        PWebGPU.materialSetDoubleSided(id, value);
    }

    @Override
    public void unlit(boolean value) {
        PWebGPU.materialSetUnlit(id, value);
    }

    @Override
    public void depthWrite(boolean value) {
        PWebGPU.materialSetDepthWrite(id, value);
    }

    public void customBlend(int colorSrc, int colorDst, int colorOp,
                            int alphaSrc, int alphaDst, int alphaOp) {
        PWebGPU.materialSetCustomBlendMode(id, colorSrc, colorDst, colorOp, alphaSrc, alphaDst, alphaOp);
    }

    @Override
    public void destroy() {
        if (id != 0) {
            PWebGPU.materialDestroy(id);
            id = 0;
        }
    }
}
