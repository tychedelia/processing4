package processing.webgpu;

import processing.core.PBuffer;
import processing.core.PCompute;

/**
 * A uniform spatial hash grid over a particle field, for GPU neighbor
 * queries. Create with {@link PParticlesWebGPU#createGrid}; each frame,
 * {@link #build} re-sorts particles into cells from a position buffer, and
 * {@link #bind} exposes the cell index to a compute kernel as the
 * {@code offsets}/{@code sorted} storage buffers plus the
 * {@code grid_min}/{@code cell_size}/{@code dims_*} uniforms.
 */
public class PGrid {

    private long handle;
    private final float cellSize;

    PGrid(long handle, float cellSize) {
        this.handle = handle;
        this.cellSize = cellSize;
    }

    /** Rebuild the cell index from the given position buffer. */
    public void build(PBuffer positions) {
        PWebGPU.particlesGridBuild(handle, ((PBufferWebGPU) positions).id());
    }

    /** Bind the grid's buffers and domain uniforms onto a compute kernel. */
    public void bind(PCompute compute) {
        PWebGPU.particlesGridBind(handle, ((PComputeWebGPU) compute).id());
    }

    public float cellSize() {
        return cellSize;
    }

    public void destroy() {
        if (handle != 0) {
            PWebGPU.particlesGridDestroy(handle);
            handle = 0;
        }
    }
}
