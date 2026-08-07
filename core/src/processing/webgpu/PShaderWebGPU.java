package processing.webgpu;

import processing.core.PShader;

public class PShaderWebGPU extends PUniformsWebGPU implements PShader {

    private final PMaterialWebGPU material;

    PShaderWebGPU(long shaderId) {
        this.material = (PMaterialWebGPU) PMaterialWebGPU.custom(shaderId);
    }

    static PShaderWebGPU load(String fragFilename) {
        return new PShaderWebGPU(PWebGPU.shaderLoad(fragFilename));
    }

    PMaterialWebGPU material() {
        return material;
    }

    @Override
    long id() {
        return material.id();
    }
}
