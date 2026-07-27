package processing.core;

/**
 * A light in the scene, created by a renderer's {@code pointLight},
 * {@code directionalLight}, or {@code spotLight}.
 *
 * <p>Processing's built-in lights are stateless and immediate — you re-declare
 * them every frame inside {@code draw()}. A {@code PLight} is a small,
 * renderer-provided enhancement: the creating call hands back a handle you can
 * keep and re-place or re-aim each frame, rather than respecifying it from
 * scratch. Implemented per-renderer (e.g. {@code PLightWebGPU}).
 */
public interface PLight {

    /** Place the light in the world (point and spot lights). */
    PLight position(float x, float y, float z);

    /** Orient the light (directional and spot lights). */
    PLight direction(float x, float y, float z);

    /** Aim the light at a world-space target. */
    PLight lookAt(float x, float y, float z);
}
