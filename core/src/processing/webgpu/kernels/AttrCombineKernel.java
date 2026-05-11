package processing.webgpu.kernels;

import processing.webgpu.Buffer;
import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * In-place per-particle binary combine:
 * {@code target = op(target, rhs * rhsScale + rhsOffset)}. The two
 * buffers must be distinct (WebGPU buffer-aliasing rule).
 */
public class AttrCombineKernel extends Kernel {
    public AttrCombineKernel() { super(PWebGPU.particlesKernelAttrCombine()); }

    /** Destination + first operand (read_write). */
    public AttrCombineKernel target(Buffer buf) { compute.set("op_a", buf); return this; }
    /** Second operand (read-only). Must be a different buffer than {@link #target}. */
    public AttrCombineKernel rhs(Buffer buf)    { compute.set("op_b", buf); return this; }

    public AttrCombineKernel op(int op)         { compute.set("op", op);       return this; }
    public AttrCombineKernel rhsScale(float v)  { compute.set("b_scale", v);   return this; }
    public AttrCombineKernel rhsOffset(float v) { compute.set("b_offset", v);  return this; }
}
