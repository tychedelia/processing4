package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Increments per-particle {@code age} by {@code dt} and zeroes {@code life}
 * once {@code age >= life}. Combine with {@link AttrLookup1DKernel} driven by
 * {@code age} to fade scale or color over a particle's lifetime. Required
 * attributes: {@code age}, {@code life}.
 */
public class AgeKernel extends Kernel {
    public AgeKernel() { super(PWebGPU.particlesKernelAge()); }

    public AgeKernel dt(float v) { compute.set("dt", v); return this; }
}
