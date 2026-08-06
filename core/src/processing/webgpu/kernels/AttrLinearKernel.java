package processing.webgpu.kernels;

import processing.core.PBuffer;
import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * In-place per-particle scalar linear transform: {@code op = op * scale + offset}.
 * With scale=0, offset=k it fills {@code op} with the constant {@code k}.
 */
public class AttrLinearKernel extends Kernel {
    public AttrLinearKernel() { super(PWebGPU.particlesKernelAttrLinear()); }

    /** Destination buffer (read_write). */
    public AttrLinearKernel target(PBuffer buf) { compute.set("op", buf); return this; }

    public AttrLinearKernel scale(float v)  { compute.set("scale", v); return this; }
    public AttrLinearKernel offset(float v) { compute.set("offset", v); return this; }
}
