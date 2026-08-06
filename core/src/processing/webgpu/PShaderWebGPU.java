package processing.webgpu;

import processing.core.PImage;
import processing.core.PMatrix2D;
import processing.core.PMatrix3D;
import processing.core.PShader;
import processing.core.PVector;

/**
 * WebGPU implementation of {@link PShader}: a custom WGSL shader surfaced as a
 * material. {@code loadShader()} builds one; {@code shader(sh)} applies it to
 * subsequent drawing; {@code set(...)} feeds its uniforms.
 */
public class PShaderWebGPU implements PShader {

    private final Shader shader;
    private final PMaterialWebGPU material;

    PShaderWebGPU(Shader shader) {
        this.shader = shader;
        this.material = (PMaterialWebGPU) PMaterialWebGPU.custom(shader);
    }

    static PShaderWebGPU load(String fragFilename) {
        return new PShaderWebGPU(Shader.load(fragFilename));
    }

    /** The material this shader drives (used by {@code shader()}). */
    PMaterialWebGPU material() {
        return material;
    }

    private long id() {
        return material.id();
    }

    private static UnsupportedOperationException unsupported(String what) {
        return new UnsupportedOperationException("WebGPU shaders do not support " + what);
    }

    // ── Uniform setters ─────────────────────────────────────────────────

    @Override
    public void set(String name, int x) {
        PWebGPU.computeSetInt(id(), name, x);
    }

    @Override
    public void set(String name, int x, int y) {
        PWebGPU.computeSetFloat2(id(), name, x, y);
    }

    @Override
    public void set(String name, int x, int y, int z) {
        PWebGPU.computeSetFloat3(id(), name, x, y, z);
    }

    @Override
    public void set(String name, int x, int y, int z, int w) {
        PWebGPU.computeSetFloat4(id(), name, x, y, z, w);
    }

    @Override
    public void set(String name, float x) {
        PWebGPU.computeSetFloat(id(), name, x);
    }

    @Override
    public void set(String name, float x, float y) {
        PWebGPU.computeSetFloat2(id(), name, x, y);
    }

    @Override
    public void set(String name, float x, float y, float z) {
        PWebGPU.computeSetFloat3(id(), name, x, y, z);
    }

    @Override
    public void set(String name, float x, float y, float z, float w) {
        PWebGPU.computeSetFloat4(id(), name, x, y, z, w);
    }

    @Override
    public void set(String name, PVector vec) {
        PWebGPU.computeSetFloat3(id(), name, vec.x, vec.y, vec.z);
    }

    @Override
    public void set(String name, boolean x) {
        PWebGPU.computeSetUInt(id(), name, x ? 1 : 0);
    }

    @Override
    public void set(String name, boolean x, boolean y) {
        PWebGPU.computeSetFloat2(id(), name, x ? 1 : 0, y ? 1 : 0);
    }

    @Override
    public void set(String name, boolean x, boolean y, boolean z) {
        PWebGPU.computeSetFloat3(id(), name, x ? 1 : 0, y ? 1 : 0, z ? 1 : 0);
    }

    @Override
    public void set(String name, boolean x, boolean y, boolean z, boolean w) {
        PWebGPU.computeSetFloat4(id(), name, x ? 1 : 0, y ? 1 : 0, z ? 1 : 0, w ? 1 : 0);
    }

    @Override
    public void set(String name, int[] vec) {
        set(name, vec, vec.length);
    }

    @Override
    public void set(String name, int[] vec, int ncoords) {
        switch (ncoords) {
            case 1 -> PWebGPU.computeSetInt(id(), name, vec[0]);
            case 2 -> PWebGPU.computeSetFloat2(id(), name, vec[0], vec[1]);
            case 3 -> PWebGPU.computeSetFloat3(id(), name, vec[0], vec[1], vec[2]);
            case 4 -> PWebGPU.computeSetFloat4(id(), name, vec[0], vec[1], vec[2], vec[3]);
            default -> throw unsupported("int arrays longer than 4");
        }
    }

    @Override
    public void set(String name, float[] vec) {
        set(name, vec, vec.length);
    }

    @Override
    public void set(String name, float[] vec, int ncoords) {
        switch (ncoords) {
            case 1 -> PWebGPU.computeSetFloat(id(), name, vec[0]);
            case 2 -> PWebGPU.computeSetFloat2(id(), name, vec[0], vec[1]);
            case 3 -> PWebGPU.computeSetFloat3(id(), name, vec[0], vec[1], vec[2]);
            case 4 -> PWebGPU.computeSetFloat4(id(), name, vec[0], vec[1], vec[2], vec[3]);
            case 16 -> PWebGPU.computeSetMat4(id(), name, vec);
            default -> throw unsupported("float arrays of length " + ncoords);
        }
    }

    @Override
    public void set(String name, boolean[] vec) {
        set(name, vec, vec.length);
    }

    @Override
    public void set(String name, boolean[] boolvec, int ncoords) {
        float[] f = new float[ncoords];
        for (int i = 0; i < ncoords; i++) {
            f[i] = boolvec[i] ? 1 : 0;
        }
        set(name, f, ncoords);
    }

    @Override
    public void set(String name, PMatrix2D mat) {
        throw unsupported("2x3 matrices; use a PMatrix3D");
    }

    @Override
    public void set(String name, PMatrix3D mat) {
        // Processing PMatrix3D is row-major (m{row}{col}); WebGPU wants column-major.
        PWebGPU.computeSetMat4(id(), name, new float[] {
            mat.m00, mat.m10, mat.m20, mat.m30,
            mat.m01, mat.m11, mat.m21, mat.m31,
            mat.m02, mat.m12, mat.m22, mat.m32,
            mat.m03, mat.m13, mat.m23, mat.m33,
        });
    }

    @Override
    public void set(String name, PMatrix3D mat, boolean use3x3) {
        set(name, mat);
    }

    @Override
    public void set(String name, PImage tex) {
        if (!(tex instanceof PImageWebGPU img)) {
            throw new RuntimeException("WebGPU shaders require a PImageWebGPU texture.");
        }
        PWebGPU.computeSetTexture(id(), name, img.getId());
    }
}
