package processing.webgpu;

import processing.core.PCompute;

public class PComputeWebGPU extends PUniformsWebGPU implements PCompute {

    private long id;

    PComputeWebGPU(long id) {
        this.id = id;
    }

    @Override
    long id() {
        return id;
    }

    @Override
    public void dispatch(int x, int y, int z) {
        PWebGPU.computeDispatch(id, x, y, z);
    }

    @Override
    public void destroy() {
        if (id != 0) {
            PWebGPU.computeDestroy(id);
            id = 0;
        }
    }
}
