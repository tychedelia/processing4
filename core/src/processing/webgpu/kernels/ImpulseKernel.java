package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * One-shot click-burst impulse: displaces position outward and adds an
 * outward velocity component within {@code radius} of {@code center}.
 */
public class ImpulseKernel extends Kernel {
    public ImpulseKernel() { super(PWebGPU.particlesKernelImpulse()); }

    public ImpulseKernel center(float x, float y, float z) { compute.set("center", x, y, z); return this; }
    public ImpulseKernel radius(float r)       { compute.set("radius", r);            return this; }
    public ImpulseKernel positionKick(float k) { compute.set("position_kick", k);     return this; }
    public ImpulseKernel velocityKick(float k) { compute.set("velocity_kick", k);     return this; }
    public ImpulseKernel falloff(int mode)     { compute.set("falloff_mode", mode);   return this; }
}
