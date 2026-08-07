package processing.webgpu;

import processing.core.NativeLibrary;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static processing.ffi.processing_h.*;
import processing.ffi.Color;

public class PWebGPU {

    static {
        ensureLoaded();
    }

    public static void ensureLoaded() {
        NativeLibrary.ensureLoaded();
    }

    // ── Init / lifecycle ────────────────────────────────────────────────

    public static void init() {
        processing_init();
        checkError();
    }

    public static void exit() {
        processing_exit((byte) 0);
        checkError();
    }

    // ── Surface ─────────────────────────────────────────────────────────

    public static long createSurface(long windowHandle, long displayHandle, int width, int height, float scaleFactor) {
        long surfaceId = processing_surface_create(windowHandle, displayHandle, width, height, scaleFactor);
        checkError();
        return surfaceId;
    }

    public static void cursor(long surfaceId, byte kind) {
        processing_cursor(surfaceId, kind);
        checkError();
    }

    public static void noCursor(long surfaceId) {
        processing_no_cursor(surfaceId);
        checkError();
    }

    public static void destroySurface(long surfaceId) {
        processing_surface_destroy(surfaceId);
        checkError();
    }

    public static void windowResized(long surfaceId, int width, int height) {
        processing_surface_resize(surfaceId, width, height);
        checkError();
    }

    // ── Graphics context ────────────────────────────────────────────────

    public static long graphicsCreate(long surfaceId, int width, int height) {
        long graphicsId = processing_graphics_create(surfaceId, width, height);
        checkError();
        return graphicsId;
    }

    public static void graphicsDestroy(long graphicsId) {
        processing_graphics_destroy(graphicsId);
        checkError();
    }

    public static void beginDraw(long graphicsId) {
        processing_begin_draw(graphicsId);
        checkError();
    }

    public static void flush(long graphicsId) {
        processing_flush(graphicsId);
        checkError();
    }

    public static void endDraw(long graphicsId) {
        processing_end_draw(graphicsId);
        checkError();
    }

    // ── Background ──────────────────────────────────────────────────────

    public static void backgroundColor(long graphicsId, float r, float g, float b, float a) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment color = allocateColor(arena, r, g, b, a);
            processing_background_color(graphicsId, color);
            checkError();
        }
    }

    public static void backgroundImage(long graphicsId, long imageId) {
        processing_background_image(graphicsId, imageId);
        checkError();
    }

    // ── Color mode ──────────────────────────────────────────────────────

    public static final byte COLOR_SPACE_SRGB = 0;
    public static final byte COLOR_SPACE_HSB = 1;
    public static final byte COLOR_SPACE_LINEAR = 2;

    public static void colorMode(long graphicsId, byte space, float max1, float max2, float max3, float maxAlpha) {
        processing_color_mode(graphicsId, space, max1, max2, max3, maxAlpha);
        checkError();
    }

    // ── Fill / stroke ───────────────────────────────────────────────────

    public static void setFill(long graphicsId, float r, float g, float b, float a) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment color = allocateColor(arena, r, g, b, a);
            processing_set_fill(graphicsId, color);
            checkError();
        }
    }

    public static void setStrokeColor(long graphicsId, float r, float g, float b, float a) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment color = allocateColor(arena, r, g, b, a);
            processing_set_stroke_color(graphicsId, color);
            checkError();
        }
    }

    public static void setStrokeWeight(long graphicsId, float weight) {
        processing_set_stroke_weight(graphicsId, weight);
        checkError();
    }

    public static void noFill(long graphicsId) {
        processing_no_fill(graphicsId);
        checkError();
    }

    public static void noStroke(long graphicsId) {
        processing_no_stroke(graphicsId);
        checkError();
    }

    public static void tint(long graphicsId, float r, float g, float b, float a) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment color = allocateColor(arena, r, g, b, a);
            processing_tint(graphicsId, color);
            checkError();
        }
    }

    public static void noTint(long graphicsId) {
        processing_no_tint(graphicsId);
        checkError();
    }

    public static void clear(long graphicsId) {
        processing_clear(graphicsId);
        checkError();
    }

    // ── Stroke style ────────────────────────────────────────────────────

    public static final byte STROKE_CAP_ROUND = 0;
    public static final byte STROKE_CAP_SQUARE = 1;
    public static final byte STROKE_CAP_PROJECT = 2;

    public static final byte STROKE_JOIN_ROUND = 0;
    public static final byte STROKE_JOIN_MITER = 1;
    public static final byte STROKE_JOIN_BEVEL = 2;

    public static void setStrokeCap(long graphicsId, byte cap) {
        processing_set_stroke_cap(graphicsId, cap);
        checkError();
    }

    public static void setStrokeJoin(long graphicsId, byte join) {
        processing_set_stroke_join(graphicsId, join);
        checkError();
    }

    // ── Shape modes ─────────────────────────────────────────────────────

    public static void rectMode(long graphicsId, byte mode) {
        processing_rect_mode(graphicsId, mode);
        checkError();
    }

    public static void ellipseMode(long graphicsId, byte mode) {
        processing_ellipse_mode(graphicsId, mode);
        checkError();
    }

    // ── Blend modes ─────────────────────────────────────────────────────

    public static final byte BLEND_MODE_BLEND = 0;
    public static final byte BLEND_MODE_ADD = 1;
    public static final byte BLEND_MODE_SUBTRACT = 2;
    public static final byte BLEND_MODE_DARKEST = 3;
    public static final byte BLEND_MODE_LIGHTEST = 4;
    public static final byte BLEND_MODE_DIFFERENCE = 5;
    public static final byte BLEND_MODE_EXCLUSION = 6;
    public static final byte BLEND_MODE_MULTIPLY = 7;
    public static final byte BLEND_MODE_SCREEN = 8;
    public static final byte BLEND_MODE_REPLACE = 9;

    public static void setBlendMode(long graphicsId, byte mode) {
        processing_set_blend_mode(graphicsId, mode);
        checkError();
    }

    public static void setCustomBlendMode(long graphicsId, byte colorSrc, byte colorDst, byte colorOp,
                                          byte alphaSrc, byte alphaDst, byte alphaOp) {
        processing_set_custom_blend_mode(graphicsId, colorSrc, colorDst, colorOp, alphaSrc, alphaDst, alphaOp);
        checkError();
    }

    // ── 2D drawing matrix ───────────────────────────────────────────────

    public static void pushMatrix(long graphicsId) {
        processing_push_matrix(graphicsId);
        checkError();
    }

    public static void popMatrix(long graphicsId) {
        processing_pop_matrix(graphicsId);
        checkError();
    }

    public static void resetMatrix(long graphicsId) {
        processing_reset_matrix(graphicsId);
        checkError();
    }

    public static void push(long graphicsId) {
        processing_push(graphicsId);
        checkError();
    }

    public static void pop(long graphicsId) {
        processing_pop(graphicsId);
        checkError();
    }

    public static void pushStyle(long graphicsId) {
        processing_push_style(graphicsId);
        checkError();
    }

    public static void popStyle(long graphicsId) {
        processing_pop_style(graphicsId);
        checkError();
    }

    /** {@code colMajor16} is a 4x4 matrix in column-major order. */
    public static void applyMatrix(long graphicsId, float[] colMajor16) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment m = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_FLOAT, colMajor16);
            processing_apply_matrix(graphicsId, m);
            checkError();
        }
    }

    /** The current model matrix as 16 floats in column-major order. */
    public static float[] getMatrix(long graphicsId) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mat = processing_get_matrix(arena, graphicsId);
            checkError();
            return processing.ffi.Matrix.m(mat).toArray(java.lang.foreign.ValueLayout.JAVA_FLOAT);
        }
    }

    public static float screenX(long graphicsId, float x, float y, float z) {
        float r = processing_screen_x(graphicsId, x, y, z);
        checkError();
        return r;
    }

    public static float screenY(long graphicsId, float x, float y, float z) {
        float r = processing_screen_y(graphicsId, x, y, z);
        checkError();
        return r;
    }

    public static float screenZ(long graphicsId, float x, float y, float z) {
        float r = processing_screen_z(graphicsId, x, y, z);
        checkError();
        return r;
    }

    public static float modelX(long graphicsId, float x, float y, float z) {
        float r = processing_model_x(graphicsId, x, y, z);
        checkError();
        return r;
    }

    public static float modelY(long graphicsId, float x, float y, float z) {
        float r = processing_model_y(graphicsId, x, y, z);
        checkError();
        return r;
    }

    public static float modelZ(long graphicsId, float x, float y, float z) {
        float r = processing_model_z(graphicsId, x, y, z);
        checkError();
        return r;
    }

    public static void translate(long graphicsId, float x, float y) {
        processing_translate(graphicsId, x, y, 0f);
        checkError();
    }

    public static void rotate(long graphicsId, float angle) {
        processing_rotate(graphicsId, angle, 0f, 0f, 1f);
        checkError();
    }

    public static void scale(long graphicsId, float x, float y) {
        processing_scale(graphicsId, x, y, 1f);
        checkError();
    }

    public static void shearX(long graphicsId, float angle) {
        processing_shear_x(graphicsId, angle);
        checkError();
    }

    public static void shearY(long graphicsId, float angle) {
        processing_shear_y(graphicsId, angle);
        checkError();
    }

    // ── 2D primitives ───────────────────────────────────────────────────

    public static void rect(long graphicsId, float x, float y, float w, float h,
                           float tl, float tr, float br, float bl) {
        processing_rect(graphicsId, x, y, w, h, tl, tr, br, bl);
        checkError();
    }

    public static void ellipse(long graphicsId, float cx, float cy, float w, float h) {
        processing_ellipse(graphicsId, cx, cy, w, h);
        checkError();
    }

    public static void circle(long graphicsId, float cx, float cy, float d) {
        processing_circle(graphicsId, cx, cy, d);
        checkError();
    }

    public static void line(long graphicsId, float x1, float y1, float x2, float y2) {
        processing_line(graphicsId, x1, y1, x2, y2);
        checkError();
    }

    public static void triangle(long graphicsId, float x1, float y1, float x2, float y2,
                                float x3, float y3) {
        processing_triangle(graphicsId, x1, y1, x2, y2, x3, y3);
        checkError();
    }

    public static void quad(long graphicsId, float x1, float y1, float x2, float y2,
                            float x3, float y3, float x4, float y4) {
        processing_quad(graphicsId, x1, y1, x2, y2, x3, y3, x4, y4);
        checkError();
    }

    public static void point(long graphicsId, float x, float y) {
        processing_point(graphicsId, x, y);
        checkError();
    }

    public static void square(long graphicsId, float x, float y, float s) {
        processing_square(graphicsId, x, y, s);
        checkError();
    }

    public static void arc(long graphicsId, float cx, float cy, float w, float h,
                           float start, float stop, byte mode) {
        processing_arc(graphicsId, cx, cy, w, h, start, stop, mode);
        checkError();
    }

    public static void bezier(long graphicsId, float x1, float y1, float x2, float y2,
                              float x3, float y3, float x4, float y4) {
        processing_bezier(graphicsId, x1, y1, x2, y2, x3, y3, x4, y4);
        checkError();
    }

    public static void curve(long graphicsId, float x1, float y1, float x2, float y2,
                             float x3, float y3, float x4, float y4) {
        processing_curve(graphicsId, x1, y1, x2, y2, x3, y3, x4, y4);
        checkError();
    }

    // ── 3D primitives ───────────────────────────────────────────────────

    public static void cylinder(long graphicsId, float radius, float height, int detail) {
        processing_cylinder(graphicsId, radius, height, detail);
        checkError();
    }

    public static void cone(long graphicsId, float radius, float height, int detail) {
        processing_cone(graphicsId, radius, height, detail);
        checkError();
    }

    public static void torus(long graphicsId, float radius, float tubeRadius, int majorSegments, int minorSegments) {
        processing_torus(graphicsId, radius, tubeRadius, majorSegments, minorSegments);
        checkError();
    }

    public static void plane(long graphicsId, float width, float height) {
        processing_plane(graphicsId, width, height);
        checkError();
    }

    public static void capsule(long graphicsId, float radius, float length, int detail) {
        processing_capsule(graphicsId, radius, length, detail);
        checkError();
    }

    public static void conicalFrustum(long graphicsId, float radiusTop, float radiusBottom, float height, int detail) {
        processing_conical_frustum(graphicsId, radiusTop, radiusBottom, height, detail);
        checkError();
    }

    public static void tetrahedron(long graphicsId, float radius) {
        processing_tetrahedron(graphicsId, radius);
        checkError();
    }

    // ── Vertex shapes ───────────────────────────────────────────────────

    public static void beginShape(long graphicsId, byte kind) {
        processing_begin_shape(graphicsId, kind);
        checkError();
    }

    public static void endShape(long graphicsId, boolean close) {
        processing_end_shape(graphicsId, close);
        checkError();
    }

    public static void shapeVertex(long graphicsId, float x, float y) {
        processing_vertex(graphicsId, x, y);
        checkError();
    }

    public static void bezierVertex(long graphicsId, float cx1, float cy1, float cx2, float cy2, float x, float y) {
        processing_bezier_vertex(graphicsId, cx1, cy1, cx2, cy2, x, y);
        checkError();
    }

    public static void quadraticVertex(long graphicsId, float cx, float cy, float x, float y) {
        processing_quadratic_vertex(graphicsId, cx, cy, x, y);
        checkError();
    }

    public static void curveVertex(long graphicsId, float x, float y) {
        processing_curve_vertex(graphicsId, x, y);
        checkError();
    }

    public static void beginContour(long graphicsId) {
        processing_begin_contour(graphicsId);
        checkError();
    }

    public static void endContour(long graphicsId) {
        processing_end_contour(graphicsId);
        checkError();
    }

    // ── 3D mode / projection ────────────────────────────────────────────

    public static void mode3d(long graphicsId) {
        processing_mode_3d(graphicsId);
        checkError();
    }

    public static void mode2d(long graphicsId) {
        processing_mode_2d(graphicsId);
        checkError();
    }

    public static void perspective(long graphicsId, float fov, float aspect, float near, float far) {
        processing_perspective(graphicsId, fov, aspect, near, far);
        checkError();
    }

    public static void ortho(long graphicsId, float left, float right, float bottom, float top, float near, float far) {
        processing_ortho(graphicsId, left, right, bottom, top, near, far);
        checkError();
    }

    public static void camera(long graphicsId, float eyeX, float eyeY, float eyeZ,
                              float centerX, float centerY, float centerZ,
                              float upX, float upY, float upZ) {
        processing_camera(graphicsId, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        checkError();
    }

    public static void cameraReset(long graphicsId) {
        processing_camera_reset(graphicsId);
        checkError();
    }

    public static void cameraSetCenter(long graphicsId, float x, float y, float z) {
        processing_camera_set_center(graphicsId, x, y, z);
        checkError();
    }

    public static void cameraSetDistance(long graphicsId, float distance) {
        processing_camera_set_distance(graphicsId, distance);
        checkError();
    }

    public static void cameraSetMinDistance(long graphicsId, float min) {
        processing_camera_set_min_distance(graphicsId, min);
        checkError();
    }

    public static void cameraSetMaxDistance(long graphicsId, float max) {
        processing_camera_set_max_distance(graphicsId, max);
        checkError();
    }

    public static void cameraSetSpeed(long graphicsId, float speed) {
        processing_camera_set_speed(graphicsId, speed);
        checkError();
    }

    public static void orbitCamera(long graphicsId) {
        processing_orbit_camera(graphicsId);
        checkError();
    }

    public static void panCamera(long graphicsId) {
        processing_pan_camera(graphicsId);
        checkError();
    }

    public static void freeCamera(long graphicsId) {
        processing_free_camera(graphicsId);
        checkError();
    }

    public static void disableCameraController(long graphicsId) {
        processing_disable_camera_controller(graphicsId);
        checkError();
    }

    // ── Entity transforms (3D objects: lights, geometry, etc.) ──────────

    public static void transformSetPosition(long entityId, float x, float y, float z) {
        processing_transform_set_position(entityId, x, y, z);
        checkError();
    }

    public static void transformTranslate(long entityId, float x, float y, float z) {
        processing_transform_translate(entityId, x, y, z);
        checkError();
    }

    public static void transformSetRotation(long entityId, float x, float y, float z) {
        processing_transform_set_rotation(entityId, x, y, z);
        checkError();
    }

    public static void transformRotateX(long entityId, float angle) {
        processing_transform_rotate_x(entityId, angle);
        checkError();
    }

    public static void transformRotateY(long entityId, float angle) {
        processing_transform_rotate_y(entityId, angle);
        checkError();
    }

    public static void transformRotateZ(long entityId, float angle) {
        processing_transform_rotate_z(entityId, angle);
        checkError();
    }

    public static void transformRotateAxis(long entityId, float angle, float axisX, float axisY, float axisZ) {
        processing_transform_rotate_axis(entityId, angle, axisX, axisY, axisZ);
        checkError();
    }

    public static void transformSetScale(long entityId, float x, float y, float z) {
        processing_transform_set_scale(entityId, x, y, z);
        checkError();
    }

    public static void transformScale(long entityId, float x, float y, float z) {
        processing_transform_scale(entityId, x, y, z);
        checkError();
    }

    public static void transformLookAt(long entityId, float targetX, float targetY, float targetZ) {
        processing_transform_look_at(entityId, targetX, targetY, targetZ);
        checkError();
    }

    public static void transformReset(long entityId) {
        processing_transform_reset(entityId);
        checkError();
    }

    // ── Lights ──────────────────────────────────────────────────────────

    public static long lightCreateDirectional(long graphicsId, float r, float g, float b, float a, float illuminance) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment color = allocateColor(arena, r, g, b, a);
            long id = processing_light_create_directional(graphicsId, color, illuminance);
            checkError();
            return id;
        }
    }

    public static long lightCreatePoint(long graphicsId, float r, float g, float b, float a,
                                        float intensity, float range, float radius) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment color = allocateColor(arena, r, g, b, a);
            long id = processing_light_create_point(graphicsId, color, intensity, range, radius);
            checkError();
            return id;
        }
    }

    public static long lightCreateSpot(long graphicsId, float r, float g, float b, float a,
                                       float intensity, float range, float radius,
                                       float innerAngle, float outerAngle) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment color = allocateColor(arena, r, g, b, a);
            long id = processing_light_create_spot(graphicsId, color, intensity, range, radius, innerAngle, outerAngle);
            checkError();
            return id;
        }
    }

    // ── Materials ───────────────────────────────────────────────────────

    public static long materialCreatePbr() {
        long id = processing_material_create_pbr();
        checkError();
        return id;
    }

    public static long materialCreateCustom(long shaderId) {
        long id = processing_material_create_custom(shaderId);
        checkError();
        return id;
    }

    public static void materialSetFloat(long matId, String name, float value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_material_set_float(matId, nameSegment, value);
            checkError();
        }
    }

    public static void materialSetFloat4(long matId, String name, float r, float g, float b, float a) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_material_set_float4(matId, nameSegment, r, g, b, a);
            checkError();
        }
    }

    public static void materialDestroy(long matId) {
        processing_material_destroy(matId);
        checkError();
    }

    public static void material(long graphicsId, long matId) {
        processing_material(graphicsId, matId);
        checkError();
    }

    public static void materialSetAlbedoBuffer(long matId, long bufferId) {
        processing_material_set_albedo_buffer(matId, bufferId);
        checkError();
    }

    public static void materialSetEmissiveBuffer(long matId, long bufferId) {
        processing_material_set_emissive_buffer(matId, bufferId);
        checkError();
    }

    public static void materialSetAlphaMode(long matId, int mode, float cutoff) {
        processing_material_set_alpha_mode(matId, (byte) mode, cutoff);
        checkError();
    }

    public static void materialSetDoubleSided(long matId, boolean value) {
        processing_material_set_double_sided(matId, value);
        checkError();
    }

    public static void materialSetUnlit(long matId, boolean value) {
        processing_material_set_unlit(matId, value);
        checkError();
    }

    public static void materialSetDepthWrite(long matId, boolean value) {
        processing_material_set_depth_write(matId, value);
        checkError();
    }

    public static void materialSetCustomBlendMode(long matId, int colorSrc, int colorDst, int colorOp,
                                                  int alphaSrc, int alphaDst, int alphaOp) {
        processing_material_set_custom_blend_mode(matId, (byte) colorSrc, (byte) colorDst, (byte) colorOp,
                (byte) alphaSrc, (byte) alphaDst, (byte) alphaOp);
        checkError();
    }

    // ── Bloom ───────────────────────────────────────────────────────────

    public static void graphicsSetBloom(long graphicsId, float intensity, float threshold) {
        processing_graphics_set_bloom(graphicsId, intensity, threshold);
        checkError();
    }

    public static void graphicsRemoveBloom(long graphicsId) {
        processing_graphics_remove_bloom(graphicsId);
        checkError();
    }

    /**
     * Unproject a screen coordinate to world space. `depth` is in `[0, 1]`
     * where 0 = near plane, 1 = far plane. Returns `{ x, y, z }`.
     */
    public static float[] graphicsWorldFromScreen(long graphicsId, float sx, float sy, float depth) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outX = arena.allocate(4);
            MemorySegment outY = arena.allocate(4);
            MemorySegment outZ = arena.allocate(4);
            processing_graphics_world_from_screen(graphicsId, sx, sy, depth, outX, outY, outZ);
            checkError();
            return new float[] { outX.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, 0),
                                 outY.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, 0),
                                 outZ.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, 0) };
        }
    }

    // ── Shaders ─────────────────────────────────────────────────────────

    public static long shaderCreate(String source) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sourceSegment = arena.allocateFrom(source);
            long id = processing_shader_create(sourceSegment);
            checkError();
            return id;
        }
    }

    public static long shaderLoad(String path) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSegment = arena.allocateFrom(path);
            long id = processing_shader_load(pathSegment);
            checkError();
            return id;
        }
    }

    public static void shaderDestroy(long shaderId) {
        processing_shader_destroy(shaderId);
        checkError();
    }

    // ── Buffers ─────────────────────────────────────────────────────────

    public static long bufferCreate(long size) {
        long id = processing_buffer_create(size);
        checkError();
        return id;
    }

    public static long bufferCreateWithData(byte[] data) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dataSegment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_BYTE, data);
            long id = processing_buffer_create_with_data(dataSegment, data.length);
            checkError();
            return id;
        }
    }

    public static void bufferWrite(long bufferId, byte[] data) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dataSegment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_BYTE, data);
            processing_buffer_write(bufferId, dataSegment, data.length);
            checkError();
        }
    }

    public static long bufferSize(long bufferId) {
        long size = processing_buffer_size(bufferId);
        checkError();
        return size;
    }

    public static byte[] bufferRead(long bufferId) {
        long size = processing_buffer_size(bufferId);
        checkError();
        if (size == 0) return new byte[0];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(java.lang.foreign.ValueLayout.JAVA_BYTE, size);
            long actual = processing_buffer_read(bufferId, out, size);
            checkError();
            return out.asSlice(0, actual).toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
        }
    }

    public static void bufferDestroy(long bufferId) {
        processing_buffer_destroy(bufferId);
        checkError();
    }

    // ── Compute ─────────────────────────────────────────────────────────

    public static long computeCreate(long shaderId) {
        long id = processing_compute_create(shaderId);
        checkError();
        return id;
    }

    public static void computeSetFloat(long computeId, String name, float value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_shader_set_float(computeId, nameSegment, value);
            checkError();
        }
    }

    public static void computeSetFloat3(long computeId, String name, float x, float y, float z) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_shader_set_vec3(computeId, nameSegment, x, y, z);
            checkError();
        }
    }

    public static void computeSetUInt(long computeId, String name, int value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_shader_set_uint(computeId, nameSegment, value);
            checkError();
        }
    }

    public static void computeSetInt(long computeId, String name, int value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_shader_set_int(computeId, nameSegment, value);
            checkError();
        }
    }

    public static void computeSetBuffer(long computeId, String name, long bufferId) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_shader_set_buffer(computeId, nameSegment, bufferId);
            checkError();
        }
    }

    public static void computeSetTexture(long computeId, String name, long imageId) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_shader_set_texture(computeId, nameSegment, imageId);
            checkError();
        }
    }

    public static void computeSetFloat2(long computeId, String name, float x, float y) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_shader_set_vec2(computeId, nameSegment, x, y);
            checkError();
        }
    }

    public static void computeSetFloat4(long computeId, String name, float x, float y, float z, float w) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            processing_shader_set_vec4(computeId, nameSegment, x, y, z, w);
            checkError();
        }
    }

    public static void computeSetMat4(long computeId, String name, float[] matrix) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            MemorySegment matSegment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_FLOAT, matrix);
            processing_shader_set_mat4(computeId, nameSegment, matSegment);
            checkError();
        }
    }

    public static void computeDispatch(long computeId, int x, int y, int z) {
        processing_compute_dispatch(computeId, x, y, z);
        checkError();
    }

    public static void computeDestroy(long computeId) {
        processing_compute_destroy(computeId);
        checkError();
    }

    // ── Particles ───────────────────────────────────────────────────────

    public static long geometryAttributeRotation() {
        return processing_geometry_attribute_rotation();
    }

    public static long geometryAttributeScale() {
        return processing_geometry_attribute_scale();
    }

    public static long geometryAttributeLife() {
        return processing_geometry_attribute_life();
    }

    public static long geometryAttributeVelocity() {
        return processing_geometry_attribute_velocity();
    }

    public static long geometryAttributeAge() {
        return processing_geometry_attribute_age();
    }

    public static byte geometryAttributeFormat(long attrId) {
        byte fmt = processing_geometry_attribute_format(attrId);
        checkError();
        return fmt;
    }

    public static String geometryAttributeName(long attrId) {
        try (Arena arena = Arena.ofConfined()) {
            long needed = processing_geometry_attribute_name(attrId, NULL, 0);
            checkError();
            if (needed == 0) return "";
            MemorySegment buf = arena.allocate(java.lang.foreign.ValueLayout.JAVA_BYTE, needed + 1);
            processing_geometry_attribute_name(attrId, buf, needed + 1);
            checkError();
            return buf.getString(0);
        }
    }

    public static long particlesCreate(int capacity, long[] attrIds) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment attrSegment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_LONG, attrIds);
            long id = processing_particles_create(capacity, attrSegment, attrIds.length);
            checkError();
            return id;
        }
    }

    public static long particlesCreateFromGeometry(long geometryId, long[] attrIds) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment attrSegment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_LONG, attrIds);
            long id = processing_particles_create_from_geometry(geometryId, attrSegment, attrIds.length);
            checkError();
            return id;
        }
    }

    public static void particlesDestroy(long particlesId) {
        processing_particles_destroy(particlesId);
        checkError();
    }

    public static int particlesCapacity(long particlesId) {
        int cap = processing_particles_capacity(particlesId);
        checkError();
        return cap;
    }

    public static long particlesBuffer(long particlesId, long attrId) {
        long bufferId = processing_particles_buffer(particlesId, attrId);
        checkError();
        return bufferId;
    }

    public static void particlesAttributeAdd(long particlesId, long attrId) {
        processing_particles_attribute_add(particlesId, attrId);
        checkError();
    }

    public static void particlesEmit(long particlesId, int n, long[] attrIds, byte[] data, long[] attrByteLengths) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment attrSegment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_LONG, attrIds);
            MemorySegment dataSegment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_BYTE, data);
            MemorySegment lensSegment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_LONG, attrByteLengths);
            processing_particles_emit(particlesId, n, attrSegment, dataSegment, lensSegment, attrIds.length);
            checkError();
        }
    }

    public static void particlesEmitGpu(long particlesId, int n, long computeId) {
        processing_particles_emit_gpu(particlesId, n, computeId);
        checkError();
    }

    public static long particlesKernelNoise() {
        long id = processing_particles_kernel_noise();
        checkError();
        return id;
    }

    public static long particlesKernelTransform() {
        long id = processing_particles_kernel_transform();
        checkError();
        return id;
    }

    public static long particlesKernelAttract() {
        long id = processing_particles_kernel_attract();
        checkError();
        return id;
    }

    public static long particlesKernelDrag() {
        long id = processing_particles_kernel_drag();
        checkError();
        return id;
    }

    public static long particlesKernelVortex() {
        long id = processing_particles_kernel_vortex();
        checkError();
        return id;
    }

    public static long particlesKernelForce() {
        long id = processing_particles_kernel_force();
        checkError();
        return id;
    }

    public static long particlesKernelIntegrate() {
        long id = processing_particles_kernel_integrate();
        checkError();
        return id;
    }

    public static long particlesKernelAge() {
        long id = processing_particles_kernel_age();
        checkError();
        return id;
    }

    public static long particlesKernelBoundsSphere() {
        long id = processing_particles_kernel_bounds_sphere();
        checkError();
        return id;
    }

    public static long particlesKernelBoundsBox() {
        long id = processing_particles_kernel_bounds_box();
        checkError();
        return id;
    }

    public static long particlesKernelBoundsGeometry(long geometryId) {
        long id = processing_particles_kernel_bounds_geometry(geometryId);
        checkError();
        return id;
    }

    public static long particlesScatterCreate(long geometryId) {
        long id = processing_particles_scatter_create(geometryId);
        checkError();
        return id;
    }

    public static long particlesScatterVolumeCreate(long geometryId) {
        long id = processing_particles_scatter_volume_create(geometryId);
        checkError();
        return id;
    }

    public static long particlesKernelImpulse() {
        long id = processing_particles_kernel_impulse();
        checkError();
        return id;
    }

    public static long particlesKernelFlock() {
        long id = processing_particles_kernel_flock();
        checkError();
        return id;
    }

    public static long particlesKernelOrient() {
        long id = processing_particles_kernel_orient();
        checkError();
        return id;
    }

    public static long particlesKernelField() {
        long id = processing_particles_kernel_field();
        checkError();
        return id;
    }

    public static long particlesKernelAttrLinear() {
        long id = processing_particles_kernel_attr_linear();
        checkError();
        return id;
    }

    public static long particlesKernelAttrCombine() {
        long id = processing_particles_kernel_attr_combine();
        checkError();
        return id;
    }

    public static long particlesKernelAttrMix() {
        long id = processing_particles_kernel_attr_mix();
        checkError();
        return id;
    }

    public static long particlesKernelAttrLookup1D() {
        long id = processing_particles_kernel_attr_lookup1d();
        checkError();
        return id;
    }

    public static long particlesKernelAttrLookup2D() {
        long id = processing_particles_kernel_attr_lookup2d();
        checkError();
        return id;
    }

    public static void particlesApply(long particlesId, long computeId) {
        processing_particles_apply(particlesId, computeId);
        checkError();
    }

    public static void particlesDraw(long graphicsId, long particlesId, long geometryId) {
        processing_particles_draw(graphicsId, particlesId, geometryId);
        checkError();
    }

    public static void fillBuffer(long graphicsId, long bufferId) {
        processing_fill_buffer(graphicsId, bufferId);
        checkError();
    }

    // ── Images ──────────────────────────────────────────────────────────

    public static long imageCreate(int width, int height, byte[] data) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dataSegment = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_BYTE, data);
            long imageId = processing_image_create(width, height, dataSegment, data.length);
            checkError();
            return imageId;
        }
    }

    public static void image(long graphicsId, long imageId, float dx, float dy) {
        processing_image(graphicsId, imageId, dx, dy);
        checkError();
    }

    public static void imageScaled(long graphicsId, long imageId, float dx, float dy, float dWidth, float dHeight) {
        processing_image_scaled(graphicsId, imageId, dx, dy, dWidth, dHeight);
        checkError();
    }

    public static void imageRegion(long graphicsId, long imageId, float dx, float dy, float dWidth, float dHeight,
                                   float sx, float sy, float sWidth, float sHeight) {
        processing_image_region(graphicsId, imageId, dx, dy, dWidth, dHeight, sx, sy, sWidth, sHeight);
        checkError();
    }

    public static void imageMode(long graphicsId, byte mode) {
        processing_image_mode(graphicsId, mode);
        checkError();
    }

    // ── Pixels ──────────────────────────────────────────────────────────

    private static int clamp8(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /** Read the framebuffer into an ARGB int array of {@code count} pixels. */
    public static int[] graphicsReadback(long graphicsId, int count) {
        if (count <= 0) {
            return new int[0];
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = processing.ffi.Color.allocateArray(count, arena);
            processing_graphics_readback(graphicsId, buffer, count);
            checkError();
            long stride = processing.ffi.Color.sizeof();
            int[] pixels = new int[count];
            for (int i = 0; i < count; i++) {
                MemorySegment c = buffer.asSlice(i * stride, stride);
                int r = clamp8(Math.round(processing.ffi.Color.c1(c) * 255f));
                int g = clamp8(Math.round(processing.ffi.Color.c2(c) * 255f));
                int b = clamp8(Math.round(processing.ffi.Color.c3(c) * 255f));
                int a = clamp8(Math.round(processing.ffi.Color.a(c) * 255f));
                pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            return pixels;
        }
    }

    /** Marshal an ARGB int array into a native {@code Color[]} segment. */
    private static MemorySegment argbToColors(Arena arena, int[] argb) {
        MemorySegment buffer = processing.ffi.Color.allocateArray(argb.length, arena);
        long stride = processing.ffi.Color.sizeof();
        for (int i = 0; i < argb.length; i++) {
            int c = argb[i];
            MemorySegment el = buffer.asSlice(i * stride, stride);
            processing.ffi.Color.c1(el, ((c >> 16) & 0xFF) / 255f);
            processing.ffi.Color.c2(el, ((c >> 8) & 0xFF) / 255f);
            processing.ffi.Color.c3(el, (c & 0xFF) / 255f);
            processing.ffi.Color.a(el, ((c >>> 24) & 0xFF) / 255f);
            processing.ffi.Color.space(el, COLOR_SPACE_SRGB);
        }
        return buffer;
    }

    /** Push an ARGB int array back to the framebuffer. */
    public static void graphicsUpdate(long graphicsId, int[] pixels) {
        if (pixels.length == 0) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            processing_graphics_update(graphicsId, argbToColors(arena, pixels), pixels.length);
            checkError();
        }
    }

    /** Push a {@code w×h} ARGB block (row-major) into the framebuffer at {@code (x, y)}. */
    public static void graphicsUpdateRegion(long graphicsId, int x, int y, int w, int h, int[] regionArgb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            processing_graphics_update_region(graphicsId, x, y, w, h,
                                              argbToColors(arena, regionArgb), (long) w * h);
            checkError();
        }
    }

    public static void graphicsSet(long graphicsId, int x, int y, int argb) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment color = allocateColor(arena,
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f);
            processing_graphics_set(graphicsId, x, y, color);
            checkError();
        }
    }

    public static long imageCreateHDR(int width, int height, float[] data) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dataSegment =
                arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_FLOAT, data);
            long imageId = processing_image_create_hdr(width, height, dataSegment, data.length);
            checkError();
            return imageId;
        }
    }

    public static long imageLoad(String path) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSegment = arena.allocateFrom(path);
            long imageId = processing_image_load(pathSegment);
            checkError();
            return imageId;
        }
    }

    public static void imageResize(long imageId, int newWidth, int newHeight) {
        processing_image_resize(imageId, newWidth, newHeight);
        checkError();
    }

    public static void imageReadback(long imageId, float[] buffer) {
        try (Arena arena = Arena.ofConfined()) {
            int numPixels = buffer.length / 4;
            MemorySegment colorBuffer = Color.allocateArray(numPixels, arena);
            processing_image_readback(imageId, colorBuffer, numPixels);
            checkError();

            for (int i = 0; i < numPixels; i++) {
                MemorySegment color = Color.asSlice(colorBuffer, i);
                buffer[i * 4] = Color.c1(color);
                buffer[i * 4 + 1] = Color.c2(color);
                buffer[i * 4 + 2] = Color.c3(color);
                buffer[i * 4 + 3] = Color.a(color);
            }
        }
    }

    // ── Input: event ingestion (called by PSurfaceGLFW) ─────────────────

    public static void inputMouseMove(long surfaceId, float x, float y) {
        processing_input_mouse_move(surfaceId, x, y);
        checkError();
    }

    public static void inputMouseButton(long surfaceId, byte button, boolean pressed) {
        processing_input_mouse_button(surfaceId, button, pressed);
        checkError();
    }

    public static void inputScroll(long surfaceId, float x, float y) {
        processing_input_scroll(surfaceId, x, y);
        checkError();
    }

    public static void inputKey(long surfaceId, int keyCode, boolean pressed) {
        processing_input_key(surfaceId, keyCode, pressed);
        checkError();
    }

    public static void inputChar(long surfaceId, int keyCode, int codepoint) {
        processing_input_char(surfaceId, keyCode, codepoint);
        checkError();
    }

    public static void inputCursorEnter(long surfaceId) {
        processing_input_cursor_enter(surfaceId);
        checkError();
    }

    public static void inputCursorLeave(long surfaceId) {
        processing_input_cursor_leave(surfaceId);
        checkError();
    }

    public static void inputFocus(long surfaceId, boolean focused) {
        processing_input_focus(surfaceId, focused);
        checkError();
    }

    public static void inputFlush() {
        processing_input_flush();
        checkError();
    }

    // ── Input: state queries ────────────────────────────────────────────

    public static float mouseX(long surfaceId) {
        return processing_mouse_x(surfaceId);
    }

    public static float mouseY(long surfaceId) {
        return processing_mouse_y(surfaceId);
    }

    public static float pmouseX(long surfaceId) {
        return processing_pmouse_x(surfaceId);
    }

    public static float pmouseY(long surfaceId) {
        return processing_pmouse_y(surfaceId);
    }

    public static boolean mouseIsPressed() {
        return processing_mouse_is_pressed();
    }

    public static byte mouseButton() {
        return processing_mouse_button();
    }

    public static boolean keyIsPressed() {
        return processing_key_is_pressed();
    }

    public static boolean keyIsDown(int keyCode) {
        return processing_key_is_down(keyCode);
    }

    public static boolean keyJustPressed(int keyCode) {
        return processing_key_just_pressed(keyCode);
    }

    public static int key() {
        return processing_key();
    }

    public static int keyCode() {
        return processing_key_code();
    }

    public static float movedX() {
        return processing_moved_x();
    }

    public static float movedY() {
        return processing_moved_y();
    }

    public static float mouseWheel() {
        return processing_mouse_wheel();
    }

    // ── Geometry ────────────────────────────────────────────────────────

    public static final byte TOPOLOGY_POINT_LIST = 0;
    public static final byte TOPOLOGY_LINE_LIST = 1;
    public static final byte TOPOLOGY_LINE_STRIP = 2;
    public static final byte TOPOLOGY_TRIANGLE_LIST = 3;
    public static final byte TOPOLOGY_TRIANGLE_STRIP = 4;

    public static final byte ATTR_FORMAT_FLOAT = 1;
    public static final byte ATTR_FORMAT_FLOAT2 = 2;
    public static final byte ATTR_FORMAT_FLOAT3 = 3;
    public static final byte ATTR_FORMAT_FLOAT4 = 4;

    public static long geometryLayoutCreate() {
        long layoutId = processing_geometry_layout_create();
        checkError();
        return layoutId;
    }

    public static void geometryLayoutAddPosition(long layoutId) {
        processing_geometry_layout_add_position(layoutId);
        checkError();
    }

    public static void geometryLayoutAddNormal(long layoutId) {
        processing_geometry_layout_add_normal(layoutId);
        checkError();
    }

    public static void geometryLayoutAddColor(long layoutId) {
        processing_geometry_layout_add_color(layoutId);
        checkError();
    }

    public static void geometryLayoutAddUv(long layoutId) {
        processing_geometry_layout_add_uv(layoutId);
        checkError();
    }

    public static void geometryLayoutAddAttribute(long layoutId, long attrId) {
        processing_geometry_layout_add_attribute(layoutId, attrId);
        checkError();
    }

    public static void geometryLayoutDestroy(long layoutId) {
        processing_geometry_layout_destroy(layoutId);
        checkError();
    }

    public static long geometryCreate(byte topology) {
        long geoId = processing_geometry_create(topology);
        checkError();
        return geoId;
    }

    public static long geometryCreateWithLayout(long layoutId, byte topology) {
        long geoId = processing_geometry_create_with_layout(layoutId, topology);
        checkError();
        return geoId;
    }

    public static long geometryBox(float width, float height, float depth) {
        long geoId = processing_geometry_box(width, height, depth);
        checkError();
        return geoId;
    }

    public static long geometrySphere(float radius, int sectors, int stacks) {
        long geoId = processing_geometry_sphere(radius, sectors, stacks);
        checkError();
        return geoId;
    }

    public static void geometryDestroy(long geoId) {
        processing_geometry_destroy(geoId);
        checkError();
    }

    public static void geometryNormal(long geoId, float nx, float ny, float nz) {
        processing_geometry_normal(geoId, nx, ny, nz);
        checkError();
    }

    public static void geometryColor(long geoId, float r, float g, float b, float a) {
        processing_geometry_color(geoId, r, g, b, a);
        checkError();
    }

    public static void geometryUv(long geoId, float u, float v) {
        processing_geometry_uv(geoId, u, v);
        checkError();
    }

    public static void geometryVertex(long geoId, float x, float y, float z) {
        processing_geometry_vertex(geoId, x, y, z);
        checkError();
    }

    public static void geometryIndex(long geoId, int i) {
        processing_geometry_index(geoId, i);
        checkError();
    }

    public static long geometryAttributeCreate(String name, byte format) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSegment = arena.allocateFrom(name);
            long attrId = processing_geometry_attribute_create(nameSegment, format);
            checkError();
            return attrId;
        }
    }

    public static void geometryAttributeDestroy(long attrId) {
        processing_geometry_attribute_destroy(attrId);
        checkError();
    }

    public static long geometryAttributePosition() {
        return processing_geometry_attribute_position();
    }

    public static long geometryAttributeNormal() {
        return processing_geometry_attribute_normal();
    }

    public static long geometryAttributeColor() {
        return processing_geometry_attribute_color();
    }

    public static long geometryAttributeUv() {
        return processing_geometry_attribute_uv();
    }

    public static void geometryAttributeFloat(long geoId, long attrId, float v) {
        processing_geometry_attribute_float(geoId, attrId, v);
        checkError();
    }

    public static void geometryAttributeFloat2(long geoId, long attrId, float x, float y) {
        processing_geometry_attribute_float2(geoId, attrId, x, y);
        checkError();
    }

    public static void geometryAttributeFloat3(long geoId, long attrId, float x, float y, float z) {
        processing_geometry_attribute_float3(geoId, attrId, x, y, z);
        checkError();
    }

    public static void geometryAttributeFloat4(long geoId, long attrId, float x, float y, float z, float w) {
        processing_geometry_attribute_float4(geoId, attrId, x, y, z, w);
        checkError();
    }

    public static int geometryVertexCount(long geoId) {
        int count = processing_geometry_vertex_count(geoId);
        checkError();
        return count;
    }

    public static int geometryIndexCount(long geoId) {
        int count = processing_geometry_index_count(geoId);
        checkError();
        return count;
    }

    private static float[] geometryFloats(long geoId, int start, int end, int comps,
                                          java.util.function.ToIntFunction<MemorySegment> call) {
        int count = end - start;
        if (count <= 0) return new float[0];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(java.lang.foreign.ValueLayout.JAVA_FLOAT, (long) count * comps);
            int written = call.applyAsInt(out);
            checkError();
            return out.asSlice(0, (long) written * comps * Float.BYTES)
                      .toArray(java.lang.foreign.ValueLayout.JAVA_FLOAT);
        }
    }

    public static float[] geometryGetPositions(long geoId, int start, int end) {
        return geometryFloats(geoId, start, end, 3,
            out -> processing_geometry_get_positions(geoId, start, end, out, end - start));
    }

    public static float[] geometryGetNormals(long geoId, int start, int end) {
        return geometryFloats(geoId, start, end, 3,
            out -> processing_geometry_get_normals(geoId, start, end, out, end - start));
    }

    public static float[] geometryGetColors(long geoId, int start, int end) {
        return geometryFloats(geoId, start, end, 4,
            out -> processing_geometry_get_colors(geoId, start, end, out, end - start));
    }

    public static float[] geometryGetUvs(long geoId, int start, int end) {
        return geometryFloats(geoId, start, end, 2,
            out -> processing_geometry_get_uvs(geoId, start, end, out, end - start));
    }

    public static int[] geometryGetIndices(long geoId, int start, int end) {
        int count = end - start;
        if (count <= 0) return new int[0];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(java.lang.foreign.ValueLayout.JAVA_INT, count);
            int written = processing_geometry_get_indices(geoId, start, end, out, count);
            checkError();
            return out.asSlice(0, (long) written * Integer.BYTES)
                      .toArray(java.lang.foreign.ValueLayout.JAVA_INT);
        }
    }

    public static void geometrySetVertex(long geoId, int index, float x, float y, float z) {
        processing_geometry_set_vertex(geoId, index, x, y, z);
        checkError();
    }

    public static void geometrySetNormal(long geoId, int index, float nx, float ny, float nz) {
        processing_geometry_set_normal(geoId, index, nx, ny, nz);
        checkError();
    }

    public static void geometrySetColor(long geoId, int index, float r, float g, float b, float a) {
        processing_geometry_set_color(geoId, index, r, g, b, a);
        checkError();
    }

    public static void geometrySetUv(long geoId, int index, float u, float v) {
        processing_geometry_set_uv(geoId, index, u, v);
        checkError();
    }

    public static void model(long graphicsId, long geoId) {
        processing_model(graphicsId, geoId);
        checkError();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static MemorySegment allocateColor(Arena arena, float r, float g, float b, float a) {
        MemorySegment color = Color.allocate(arena);
        Color.c1(color, r);
        Color.c2(color, g);
        Color.c3(color, b);
        Color.a(color, a);
        Color.space(color, COLOR_SPACE_SRGB);
        return color;
    }

    private static void checkError() {
        MemorySegment ret = processing_check_error();
        if (ret.equals(NULL)) {
            return;
        }

        String errorMsg = ret.getString(0);
        if (errorMsg != null && !errorMsg.isEmpty()) {
            throw new PWebGPUException(errorMsg);
        }
    }

    // ── glTF ────────────────────────────────────────────────────────────

    public static long gltfLoad(long graphicsId, String path) {
        try (Arena arena = Arena.ofConfined()) {
            long id = processing_gltf_load(graphicsId, arena.allocateFrom(path));
            checkError();
            return id;
        }
    }

    public static long gltfGeometry(long gltfId, String name) {
        try (Arena arena = Arena.ofConfined()) {
            long id = processing_gltf_geometry(gltfId, arena.allocateFrom(name));
            checkError();
            return id;
        }
    }

    public static long gltfMaterial(long gltfId, String name) {
        try (Arena arena = Arena.ofConfined()) {
            long id = processing_gltf_material(gltfId, arena.allocateFrom(name));
            checkError();
            return id;
        }
    }

    public static long gltfLight(long gltfId, int index) {
        long id = processing_gltf_light(gltfId, index);
        checkError();
        return id;
    }

    public static void gltfCamera(long gltfId, int index) {
        processing_gltf_camera(gltfId, index);
        checkError();
    }

    // ── Fonts / text ────────────────────────────────────────────────────

    public static long createFont(String name) {
        try (Arena arena = Arena.ofConfined()) {
            long id = processing_create_font(arena.allocateFrom(name));
            checkError();
            return id;
        }
    }

    public static long loadFont(String path) {
        try (Arena arena = Arena.ofConfined()) {
            long id = processing_load_font(arena.allocateFrom(path));
            checkError();
            return id;
        }
    }

    public static int fontVariationCount(long fontId) {
        int n = processing_font_variation_count(fontId);
        checkError();
        return n;
    }

    public static PFontWebGPU.FontAxis fontVariation(long fontId, int index) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment tag = arena.allocate(java.lang.foreign.ValueLayout.JAVA_BYTE, 4);
            MemorySegment min = arena.allocate(java.lang.foreign.ValueLayout.JAVA_FLOAT);
            MemorySegment max = arena.allocate(java.lang.foreign.ValueLayout.JAVA_FLOAT);
            MemorySegment def = arena.allocate(java.lang.foreign.ValueLayout.JAVA_FLOAT);
            boolean ok = processing_font_variation(fontId, index, tag, min, max, def);
            checkError();
            if (!ok) {
                return null;
            }
            String tagStr = new String(tag.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE),
                                       java.nio.charset.StandardCharsets.US_ASCII);
            return new PFontWebGPU.FontAxis(tagStr,
                min.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, 0),
                max.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, 0),
                def.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, 0));
        }
    }

    public static void textFont(long graphicsId, long fontId) {
        processing_text_font(graphicsId, fontId);
        checkError();
    }

    public static void text(long graphicsId, String str, float x, float y) {
        try (Arena arena = Arena.ofConfined()) {
            processing_text(graphicsId, arena.allocateFrom(str), x, y);
            checkError();
        }
    }

    public static void text3d(long graphicsId, String str, float x, float y, float z) {
        try (Arena arena = Arena.ofConfined()) {
            processing_text_3d(graphicsId, arena.allocateFrom(str), x, y, z);
            checkError();
        }
    }

    public static void textInt(long graphicsId, int value, float x, float y) {
        processing_text_int(graphicsId, value, x, y);
        checkError();
    }

    public static void textFloat(long graphicsId, float value, float x, float y) {
        processing_text_float(graphicsId, value, x, y);
        checkError();
    }

    public static void textBox(long graphicsId, String str, float x, float y, float w, float h) {
        try (Arena arena = Arena.ofConfined()) {
            processing_text_box(graphicsId, arena.allocateFrom(str), x, y, w, h);
            checkError();
        }
    }

    public static void textSize(long graphicsId, float size) {
        processing_text_size(graphicsId, size);
        checkError();
    }

    public static void textAlign(long graphicsId, byte h, byte v) {
        processing_text_align(graphicsId, h, v);
        checkError();
    }

    public static void textLeading(long graphicsId, float leading) {
        processing_text_leading(graphicsId, leading);
        checkError();
    }

    public static void textWrap(long graphicsId, byte mode) {
        processing_text_wrap(graphicsId, mode);
        checkError();
    }

    public static void textStyle(long graphicsId, byte style) {
        processing_text_style(graphicsId, style);
        checkError();
    }

    public static void textWeight(long graphicsId, float weight) {
        processing_text_weight(graphicsId, weight);
        checkError();
    }

    public static float textWidth(long graphicsId, String str) {
        try (Arena arena = Arena.ofConfined()) {
            float w = processing_text_width(graphicsId, arena.allocateFrom(str));
            checkError();
            return w;
        }
    }

    public static float textAscent(long graphicsId) {
        float a = processing_text_ascent(graphicsId);
        checkError();
        return a;
    }

    public static float textDescent(long graphicsId) {
        float d = processing_text_descent(graphicsId);
        checkError();
        return d;
    }

    /** Returns {@code [x, y, w, h]} of the text's bounding box. */
    public static float[] textBounds(long graphicsId, String str, float x, float y) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(java.lang.foreign.ValueLayout.JAVA_FLOAT, 4);
            processing_text_bounds(graphicsId, arena.allocateFrom(str), x, y, out);
            checkError();
            return out.toArray(java.lang.foreign.ValueLayout.JAVA_FLOAT);
        }
    }

    public static void textVariation(long graphicsId, String tag, float value) {
        try (Arena arena = Arena.ofConfined()) {
            processing_text_variation(graphicsId, arena.allocateFrom(tag), value);
            checkError();
        }
    }

    public static void clearTextVariations(long graphicsId) {
        processing_clear_text_variations(graphicsId);
        checkError();
    }

    public static void textFeature(long graphicsId, String tag, int value) {
        try (Arena arena = Arena.ofConfined()) {
            processing_text_feature(graphicsId, arena.allocateFrom(tag), (short) value);
            checkError();
        }
    }

    public static void noTextFeature(long graphicsId, String tag) {
        try (Arena arena = Arena.ofConfined()) {
            processing_no_text_feature(graphicsId, arena.allocateFrom(tag));
            checkError();
        }
    }

    public static void clearTextFeatures(long graphicsId) {
        processing_clear_text_features(graphicsId);
        checkError();
    }

    /** {@code colors} is a flat RGBA array (4 floats per glyph color). */
    public static void textGlyphColors(long graphicsId, float[] colors) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment c = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_FLOAT, colors);
            processing_text_glyph_colors(graphicsId, c, colors.length / 4);
            checkError();
        }
    }
}
