package processing.webgpu.kernels;

import processing.webgpu.Kernel;
import processing.webgpu.PWebGPU;

/**
 * Affine transform on {@code position}: scale → axis-angle rotate →
 * translate. Defaults are identity (any unset component is a no-op).
 */
public class TransformKernel extends Kernel {
    public TransformKernel() {
        super(PWebGPU.particlesKernelTransform());
    }

    public TransformKernel translate(float x, float y, float z) {
        compute.set("translate", x, y, z);
        return this;
    }

    public TransformKernel rotationAxis(float x, float y, float z) {
        compute.set("rotation_axis", x, y, z);
        return this;
    }

    public TransformKernel rotationAngle(float radians) {
        compute.set("rotation_angle", radians);
        return this;
    }

    public TransformKernel scale(float x, float y, float z) {
        compute.set("scale", x, y, z);
        return this;
    }

    public TransformKernel scale(float uniform) {
        compute.set("scale", uniform, uniform, uniform);
        return this;
    }
}
