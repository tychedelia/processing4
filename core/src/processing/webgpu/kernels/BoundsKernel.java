package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Sphere region constraint. Keeps particles within {@code radius} of
 * {@code center} via the chosen {@link Mode}.
 */
public class BoundsKernel extends Kernel {
    public enum Mode { CLAMP, REFLECT, WRAP, SOFT_PULL }

    public BoundsKernel() { super(PWebGPU.particlesKernelBounds()); }

    public BoundsKernel center(float x, float y, float z) { compute.set("center", x, y, z); return this; }
    public BoundsKernel radius(float r)        { compute.set("radius", r); return this; }
    public BoundsKernel mode(Mode m)           { compute.set("mode", m.ordinal()); return this; }
    public BoundsKernel softStrength(float v)  { compute.set("soft_strength", v); return this; }
    public BoundsKernel velocityCap(float v)   { compute.set("velocity_cap", v); return this; }
}
