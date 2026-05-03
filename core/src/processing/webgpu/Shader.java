package processing.webgpu;

public class Shader {

    private long id;

    public Shader(String wgslSource) {
        this.id = PWebGPU.shaderCreate(wgslSource);
    }

    private Shader(long id) {
        this.id = id;
    }

    public static Shader load(String path) {
        return new Shader(PWebGPU.shaderLoad(path));
    }

    public long id() {
        return id;
    }

    public void destroy() {
        if (id != 0) {
            PWebGPU.shaderDestroy(id);
            id = 0;
        }
    }
}
