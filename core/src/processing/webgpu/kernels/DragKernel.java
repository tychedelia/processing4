package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Velocity damping: each dispatch, {@code velocity *= (1 - coefficient)}.
 * {@code velocityCap > 0} clamps the resulting speed.
 */
public class DragKernel extends Kernel {
    public DragKernel() { super(PWebGPU.particlesKernelDrag()); }

    public DragKernel coefficient(float v) { compute.set("coefficient", v); return this; }
    public DragKernel velocityCap(float v) { compute.set("velocity_cap", v); return this; }
}
