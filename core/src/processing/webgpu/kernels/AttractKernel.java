package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Point attractor / repeller. Adds a radial impulse to {@code velocity}
 * within {@code radius} of {@code center}. Positive {@code strength}
 * attracts; negative repels.
 */
public class AttractKernel extends Kernel {
    public AttractKernel() { super(PWebGPU.particlesKernelAttract()); }

    public AttractKernel center(float x, float y, float z) { compute.set("center", x, y, z); return this; }
    public AttractKernel strength(float v) { compute.set("strength", v);     return this; }
    public AttractKernel radius(float r)   { compute.set("radius", r);       return this; }
    public AttractKernel falloff(int mode) { compute.set("falloff_mode", mode); return this; }
}
