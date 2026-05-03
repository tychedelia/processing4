package processing.webgpu;

public class Compute {

    private long id;

    public Compute(Shader shader) {
        this.id = PWebGPU.computeCreate(shader.id());
    }

    Compute(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    public void set(String name, float value) {
        PWebGPU.computeSetFloat(id, name, value);
    }

    public void set(String name, float x, float y, float z) {
        PWebGPU.computeSetFloat3(id, name, x, y, z);
    }

    public void set(String name, Buffer buffer) {
        PWebGPU.computeSetBuffer(id, name, buffer.id());
    }

    public void dispatch(int x, int y, int z) {
        PWebGPU.computeDispatch(id, x, y, z);
    }

    public void destroy() {
        if (id != 0) {
            PWebGPU.computeDestroy(id);
            id = 0;
        }
    }
}
