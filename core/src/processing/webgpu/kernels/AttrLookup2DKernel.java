package processing.webgpu.kernels;

import processing.core.PBuffer;
import processing.webgpu.Kernel;
import processing.webgpu.PImageWebGPU;
import processing.webgpu.PWebGPU;

/**
 * Per-particle 2D texture lookup. Samples {@code ramp} using two
 * scalar input attributes as u/v coords and writes the sampled color
 * (multiplied by {@code colorScale}) into a {@code vec4} output
 * attribute.
 */
public class AttrLookup2DKernel extends Kernel {
    public AttrLookup2DKernel() { super(PWebGPU.particlesKernelAttrLookup2D()); }

    /** u-coordinate input (scalar). */
    public AttrLookup2DKernel u(PBuffer buf) { compute.set("op_in_u", buf); return this; }
    /** v-coordinate input (scalar). */
    public AttrLookup2DKernel v(PBuffer buf) { compute.set("op_in_v", buf); return this; }
    /** Output buffer (vec4 destination). */
    public AttrLookup2DKernel out(PBuffer buf) { compute.set("op_out", buf); return this; }

    /** Bind both the texture and its sampler from the same image. */
    public AttrLookup2DKernel ramp(PImageWebGPU img) {
        compute.set("ramp", img);
        compute.set("ramp_sampler", img);
        return this;
    }

    public AttrLookup2DKernel uScale(float v)     { compute.set("u_scale", v); return this; }
    public AttrLookup2DKernel uOffset(float v)    { compute.set("u_offset", v); return this; }
    public AttrLookup2DKernel vScale(float v)     { compute.set("v_scale", v); return this; }
    public AttrLookup2DKernel vOffset(float v)    { compute.set("v_offset", v); return this; }
    public AttrLookup2DKernel colorScale(float v) { compute.set("color_scale", v); return this; }
}
