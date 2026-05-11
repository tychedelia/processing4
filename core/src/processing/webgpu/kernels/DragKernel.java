package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Velocity damping: each dispatch, {@code velocity *= (1 - damping)}.
 * {@code maxSpeed > 0} additionally clamps the resulting speed.
 */
public class DragKernel extends Kernel {
    public DragKernel() { super(PWebGPU.particlesKernelDrag()); }

    public DragKernel damping(float v)  { compute.set("damping", v);   return this; }
    public DragKernel maxSpeed(float v) { compute.set("max_speed", v); return this; }
}
