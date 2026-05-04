package processing.webgpu.kernels;

import processing.webgpu.Buffer;
import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * In-place per-particle 3-input lerp:
 * {@code target = mix(target, rhs, weight * weightScale + weightOffset)}.
 * The three buffers must be distinct (WebGPU buffer-aliasing rule).
 */
public class AttrMixKernel extends Kernel {
    public AttrMixKernel() { super(PWebGPU.particlesKernelAttrMix()); }

    /** Destination + lerp-from value (read_write). */
    public AttrMixKernel target(Buffer buf) { compute.set("op_a", buf); return this; }
    /** Lerp-toward value (read-only). */
    public AttrMixKernel rhs(Buffer buf)    { compute.set("op_b", buf); return this; }
    /** Per-particle weight in [0, 1] (read-only). */
    public AttrMixKernel weight(Buffer buf) { compute.set("op_t", buf); return this; }

    public AttrMixKernel weightScale(float v)   { compute.set("t_scale", v); return this; }
    public AttrMixKernel weightOffset(float v)  { compute.set("t_offset", v); return this; }
    /** When true (default), weight is clamped to [0, 1]. Disable for additive blends. */
    public AttrMixKernel weightClamp(boolean c) { compute.set("t_clamp", c ? 1 : 0); return this; }
}
