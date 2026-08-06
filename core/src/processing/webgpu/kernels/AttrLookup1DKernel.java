package processing.webgpu.kernels;

import processing.core.PBuffer;
import processing.webgpu.Kernel;
import processing.webgpu.PImageWebGPU;
import processing.webgpu.PWebGPU;

/**
 * Per-particle 1D ramp lookup. Samples {@code ramp} (a 1×N gradient
 * texture) using a scalar input attribute as the lookup coord and
 * writes the four sampled channels (r/g/b/a) into separate scalar
 * output attributes.
 */
public class AttrLookup1DKernel extends Kernel {
    public AttrLookup1DKernel() { super(PWebGPU.particlesKernelAttrLookup1D()); }

    /** Lookup coord input (scalar). */
    public AttrLookup1DKernel input(PBuffer buf) { compute.set("op_in", buf); return this; }

    /** Sampled-color output channel buffers. */
    public AttrLookup1DKernel outR(PBuffer buf) { compute.set("op_out_r", buf); return this; }
    public AttrLookup1DKernel outG(PBuffer buf) { compute.set("op_out_g", buf); return this; }
    public AttrLookup1DKernel outB(PBuffer buf) { compute.set("op_out_b", buf); return this; }
    public AttrLookup1DKernel outA(PBuffer buf) { compute.set("op_out_a", buf); return this; }

    /** Bind both the texture and its sampler from the same image. */
    public AttrLookup1DKernel ramp(PImageWebGPU img) {
        compute.set("ramp", img);
        compute.set("ramp_sampler", img);
        return this;
    }

    public AttrLookup1DKernel scale(float v)  { compute.set("scale", v); return this; }
    public AttrLookup1DKernel offset(float v) { compute.set("offset", v); return this; }
}
