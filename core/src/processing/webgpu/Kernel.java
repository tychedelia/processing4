package processing.webgpu;

/**
 * Base class for typed wrappers around built-in compute kernels. Each
 * subclass exposes the kernel's uniforms and slot bindings as typed
 * methods (no stringly-typed parameter names) and chains via
 * {@code return this} so callers can configure inline.
 *
 * <p>{@link Particles#apply(Kernel)} dispatches a kernel against the
 * particle system. For custom WGSL kernels (no typed wrapper), use
 * {@link Compute} directly via {@link Particles#apply(Compute)}.
 */
public abstract class Kernel {

    protected final Compute compute;

    protected Kernel(long computeId) {
        this.compute = new Compute(computeId);
    }

    /** The underlying {@link Compute} — escape hatch for any uniform
     *  or slot the typed wrapper doesn't expose. */
    public Compute compute() {
        return compute;
    }

    public long id() {
        return compute.id();
    }

    public void destroy() {
        compute.destroy();
    }
}
