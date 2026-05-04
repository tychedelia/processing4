package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Per-particle orientation kernel: writes a {@code rotation} quaternion
 * that aligns the configured forward axis with each particle's velocity.
 * Default forward is {@code (0, 0, 1)}, matching {@code Geometry.box}'s
 * long axis.
 */
public class OrientKernel extends Kernel {
    public OrientKernel() { super(PWebGPU.particlesKernelOrient()); }

    public OrientKernel forward(float x, float y, float z) { compute.set("forward", x, y, z); return this; }
}
