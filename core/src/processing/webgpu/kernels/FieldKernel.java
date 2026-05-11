package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Spatial weight field. Writes a per-particle scalar to the
 * {@code weight} attribute based on distance to a sphere
 * (1 inside, 0 outside, with the chosen {@link Falloff} curve in between).
 * Used as input to other kernels — typically as the {@code t} of an
 * {@link AttrMixKernel} for conditional writes, or as a charge value
 * accumulated via max with {@link AttrCombineKernel}.
 *
 * <p>Requires the particle system to have a {@code Float} attribute
 * named {@code "weight"}.
 */
public class FieldKernel extends Kernel {
    public FieldKernel() { super(PWebGPU.particlesKernelField()); }

    public FieldKernel center(float x, float y, float z) { compute.set("center", x, y, z); return this; }
    public FieldKernel radius(float r)   { compute.set("radius", r);            return this; }
    public FieldKernel falloff(int mode) { compute.set("falloff_mode", mode);   return this; }
}
