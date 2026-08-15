package processing.webgpu;

import processing.core.PShader;

public class PShaderWebGPU extends PUniformsWebGPU implements PShader {

    private final long shaderId;
    private final PMaterialWebGPU material;
    private long filterId = 0;

    PShaderWebGPU(long shaderId) {
        this.shaderId = shaderId;
        this.material = (PMaterialWebGPU) PMaterialWebGPU.custom(shaderId);
    }

    static PShaderWebGPU load(String fragFilename) {
        return new PShaderWebGPU(PWebGPU.shaderLoad(fragFilename));
    }

    PMaterialWebGPU material() {
        return material;
    }

    long filterId() {
        if (filterId == 0) {
            filterId = PWebGPU.filterCreate(shaderId);
        }
        return filterId;
    }

    @Override
    long id() {
        return material.id();
    }
}
