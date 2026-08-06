package processing.webgpu;

import processing.core.PLight;
import processing.core.PMaterial;
import processing.core.PShape;

/**
 * A loaded glTF / GLB scene. Pull out its named meshes and materials, its
 * lights, and apply its cameras. WebGPU-specific (advanced).
 */
public class Gltf {

    private final long id;
    private final PGraphicsWebGPU pg;

    Gltf(long id, PGraphicsWebGPU pg) {
        this.id = id;
        this.pg = pg;
    }

    /** The named mesh as a {@link PShape}. */
    public PShape shape(String name) {
        return PShapeWebGPU.fromGeometry(pg, PWebGPU.gltfGeometry(id, name));
    }

    /** The named material. */
    public PMaterial material(String name) {
        return PMaterialWebGPU.fromId(PWebGPU.gltfMaterial(id, name));
    }

    /** The light at {@code index}. */
    public PLight light(int index) {
        return new PLightWebGPU(PWebGPU.gltfLight(id, index));
    }

    /** Apply the camera at {@code index}. */
    public void camera(int index) {
        PWebGPU.gltfCamera(id, index);
    }
}
