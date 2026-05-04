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
    public enum Op {
        ADD(0), SUB(1), MUL(2), DIV(3), MIN(4), MAX(5), POW(6);
        final int code;
        Op(int code) { this.code = code; }
    }

    public AttrCombineKernel() { super(PWebGPU.particlesKernelAttrCombine()); }

    /** Destination + first operand (read_write). */
    public AttrCombineKernel target(Buffer buf) { compute.set("op_a", buf); return this; }
    /** Second operand (read-only). Must be a different buffer than {@link #target}. */
    public AttrCombineKernel rhs(Buffer buf)    { compute.set("op_b", buf); return this; }

    public AttrCombineKernel op(Op op)              { compute.set("op", op.code); return this; }
    public AttrCombineKernel rhsScale(float v)      { compute.set("b_scale", v); return this; }
    public AttrCombineKernel rhsOffset(float v)     { compute.set("b_offset", v); return this; }
}
