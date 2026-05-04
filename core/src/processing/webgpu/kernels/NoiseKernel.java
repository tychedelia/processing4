package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Procedural noise field. Perturbs {@code velocity} by sampled noise
 * (curl-noise mode toggles divergence-free turbulence).
 */
public class NoiseKernel extends Kernel {
    public NoiseKernel() { super(PWebGPU.particlesKernelNoise()); }

    public NoiseKernel scale(float v)    { compute.set("scale", v); return this; }
    public NoiseKernel strength(float v) { compute.set("strength", v); return this; }
    public NoiseKernel time(float v)     { compute.set("time", v); return this; }
    public NoiseKernel curl(boolean c)   { compute.set("curl", c ? 1 : 0); return this; }
}
