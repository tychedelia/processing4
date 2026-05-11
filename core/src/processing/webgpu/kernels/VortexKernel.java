package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Vortex / swirl around an axis through {@code center}. Adds a
 * tangential impulse to {@code velocity}.
 */
public class VortexKernel extends Kernel {
    public VortexKernel() { super(PWebGPU.particlesKernelVortex()); }

    public VortexKernel center(float x, float y, float z) { compute.set("center", x, y, z); return this; }
    public VortexKernel axis(float x, float y, float z)   { compute.set("axis", x, y, z);   return this; }
    public VortexKernel strength(float v) { compute.set("strength", v);     return this; }
    public VortexKernel radius(float r)   { compute.set("radius", r);       return this; }
    public VortexKernel falloff(int mode) { compute.set("falloff_mode", mode); return this; }
}
