package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Euler step: {@code position += velocity * dt}. Pair with force kernels
 * ({@link ForceKernel}, {@link AttractKernel}, {@link VortexKernel}, …) which
 * only write {@code velocity}. Required attributes: {@code position},
 * {@code velocity}.
 */
public class IntegrateKernel extends Kernel {
    public IntegrateKernel() { super(PWebGPU.particlesKernelIntegrate()); }

    public IntegrateKernel dt(float v) { compute.set("dt", v); return this; }
}
