package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Reynolds-style flocking kernel. Reads {@code position} + {@code velocity},
 * integrates separation/alignment/cohesion forces from a tiled neighbor
 * scan, and advances {@code position}. Required attributes: {@code position},
 * {@code velocity}.
 */
public class FlockKernel extends Kernel {
    public FlockKernel() { super(PWebGPU.particlesKernelFlock()); }

    public FlockKernel sepDistance(float v)      { compute.set("sep_distance", v); return this; }
    public FlockKernel nbrDistance(float v)      { compute.set("nbr_distance", v); return this; }
    public FlockKernel weightSeparation(float v) { compute.set("weight_separation", v); return this; }
    public FlockKernel weightAlignment(float v)  { compute.set("weight_alignment", v); return this; }
    public FlockKernel weightCohesion(float v)   { compute.set("weight_cohesion", v); return this; }
    public FlockKernel maxSpeed(float v)         { compute.set("max_speed", v); return this; }
    public FlockKernel maxForce(float v)         { compute.set("max_force", v); return this; }
    public FlockKernel minSpeed(float v)         { compute.set("min_speed", v); return this; }
}
