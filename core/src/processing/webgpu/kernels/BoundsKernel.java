package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Region constraint that keeps particles inside a {@link Sphere} or
 * axis-aligned {@link Box}. Constructed via {@link processing.webgpu.PParticles}
 * factories — {@code PParticles.boundsSphere()}, {@code PParticles.boundsBox()},
 * or {@code PParticles.boundsGeometry(geo)}.
 *
 * <p>{@link #mode(int)} accepts {@code PParticles.CLAMP}, {@code REFLECT},
 * {@code WRAP}, or {@code SOFT}. {@code WRAP} is toroidal on a box and a
 * no-op on a sphere (sphere wrap is geometrically meaningless).
 */
public abstract class BoundsKernel extends Kernel {
    protected BoundsKernel(long id) { super(id); }

    public BoundsKernel mode(int m)           { compute.set("mode", m); return this; }
    public BoundsKernel softStrength(float v) { compute.set("soft_strength", v); return this; }
    public BoundsKernel maxSpeed(float v)     { compute.set("max_speed", v); return this; }

    public static class Sphere extends BoundsKernel {
        public Sphere() { super(PWebGPU.particlesKernelBoundsSphere()); }

        public Sphere center(float x, float y, float z) { compute.set("center", x, y, z); return this; }
        public Sphere radius(float r)                   { compute.set("radius", r);       return this; }

        @Override public Sphere mode(int m)           { super.mode(m);          return this; }
        @Override public Sphere softStrength(float v) { super.softStrength(v); return this; }
        @Override public Sphere maxSpeed(float v)     { super.maxSpeed(v);     return this; }
    }

    public static class Box extends BoundsKernel {
        public Box() { super(PWebGPU.particlesKernelBoundsBox()); }
        public Box(long id) { super(id); }

        public Box min(float x, float y, float z) { compute.set("aabb_min", x, y, z); return this; }
        public Box max(float x, float y, float z) { compute.set("aabb_max", x, y, z); return this; }

        @Override public Box mode(int m)           { super.mode(m);          return this; }
        @Override public Box softStrength(float v) { super.softStrength(v); return this; }
        @Override public Box maxSpeed(float v)     { super.maxSpeed(v);     return this; }
    }
}
