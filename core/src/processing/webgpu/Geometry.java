package processing.webgpu;

public class Geometry {

    private long id;

    Geometry(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    public static Geometry box(float width, float height, float depth) {
        return new Geometry(PWebGPU.geometryBox(width, height, depth));
    }

    public static Geometry sphere(float radius, int sectors, int stacks) {
        return new Geometry(PWebGPU.geometrySphere(radius, sectors, stacks));
    }

    public int vertexCount() {
        return PWebGPU.geometryVertexCount(id);
    }

    public void destroy() {
        if (id != 0) {
            PWebGPU.geometryDestroy(id);
            id = 0;
        }
    }
}
