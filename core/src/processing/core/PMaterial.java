package processing.core;

/**
 * A surface material — how a shape or particle system catches light. Bind one
 * as the active material with {@code material(m)}, the same way {@code shader(s)}
 * binds a shader. Renderer-provided (e.g. {@code PMaterialWebGPU}); create one
 * with {@code createMaterial()}.
 */
public interface PMaterial {

    /** Set a named scalar parameter. */
    void set(String name, float value);

    /** Set a named vec4 parameter. */
    void set(String name, float r, float g, float b, float a);

    /** Base (albedo) color. */
    void setAlbedo(float r, float g, float b, float a);

    /** Release native resources. */
    void destroy();
}
