package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Constant directional acceleration on {@code velocity} — wind, gravity, or
 * any uniform external force. {@code direction} is auto-normalized.
 * Required attributes: {@code velocity}.
 */
public class ForceKernel extends Kernel {
    public ForceKernel() { super(PWebGPU.particlesKernelForce()); }

    public ForceKernel direction(float x, float y, float z) { compute.set("direction", x, y, z); return this; }
    public ForceKernel strength(float v)                    { compute.set("strength", v);        return this; }
}
