package processing.webgpu;

import processing.core.PBuffer;
import processing.core.PCompute;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PMatrix;
import processing.core.PMatrix2D;
import processing.core.PMatrix3D;
import processing.core.PParticles;
import processing.core.PShader;
import processing.core.PLight;
import processing.core.PMaterial;
import processing.core.PShape;
import processing.core.PSurface;

import java.util.ArrayList;
import java.util.List;

public class PGraphicsWebGPU extends PGraphics {
    protected long surfaceId = 0;
    private long graphicsId = 0;

    private long currentGeometry = 0;
    private int shapeKind = 0;
    private float normalX = 0, normalY = 0, normalZ = 1;

    private final List<Long> pendingDestroy = new ArrayList<>();


    @Override
    public PSurface createSurface() {
        String backend = System.getProperty("processing.webgpu.surface", "newt");
        if ("glfw".equalsIgnoreCase(backend)) {
            return surface = new PSurfaceGLFW(this);
        }
        return surface = new PSurfaceNEWT(this);
    }

    protected void initWebGPUSurface(long windowHandle, long displayHandle, int width, int height, float scaleFactor) {
        surfaceId = PWebGPU.createSurface(windowHandle, displayHandle, width, height, scaleFactor);
        if (surfaceId == 0) {
            System.err.println("Failed to create WebGPU surface");
            return;
        }
        graphicsId = PWebGPU.graphicsCreate(surfaceId, width, height);
        if (graphicsId == 0) {
            System.err.println("Failed to create WebGPU graphics context");
        }
    }

    public long getSurfaceId() {
        return surfaceId;
    }

    @Override
    public void setSize(int w, int h) {
        super.setSize(w, h);
        if (surfaceId != 0) {
            PWebGPU.windowResized(surfaceId, pixelWidth, pixelHeight);
        }
    }

    @Override
    public void beginDraw() {
        super.beginDraw();
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.beginDraw(graphicsId);
        checkSettings();
    }

    @Override
    public void flush() {
        super.flush();
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.flush(graphicsId);

        for (long geometryId : pendingDestroy) {
            PWebGPU.geometryDestroy(geometryId);
        }
        pendingDestroy.clear();
    }

    @Override
    public void endDraw() {
        super.endDraw();
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.endDraw(graphicsId);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (surfaceId != 0) {
            PWebGPU.destroySurface(surfaceId);
            surfaceId = 0;
        }
        if (graphicsId != 0) {
            PWebGPU.graphicsDestroy(graphicsId);
            graphicsId = 0;
        }
        PWebGPU.exit();
    }

    // ── Background ──────────────────────────────────────────────────────

    @Override
    protected void backgroundImpl() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.backgroundColor(graphicsId, backgroundR, backgroundG, backgroundB, backgroundA);
    }

    @Override
    protected void backgroundImpl(PImage image) {
        if (graphicsId == 0) {
            return;
        }
        if (!(image instanceof PImageWebGPU)) {
            throw new RuntimeException("WebGPU renderer requires PImageWebGPU. Use createImage().");
        }
        PImageWebGPU img = (PImageWebGPU) image;
        if (img.getId() == 0) {
            img.loadPixels();
            byte[] rgba = pixelsToRGBA(img.pixels);
            long imageId = PWebGPU.imageCreate(img.pixelWidth, img.pixelHeight, rgba);
            img.setId(imageId);
        }
        PWebGPU.backgroundImage(graphicsId, img.getId());
    }

    /** Upload the image to the GPU on first use and return its native id. */
    private long ensureImageId(PImage image) {
        if (!(image instanceof PImageWebGPU)) {
            throw new RuntimeException("WebGPU renderer requires PImageWebGPU. Use createImage().");
        }
        PImageWebGPU img = (PImageWebGPU) image;
        if (img.getId() == 0) {
            img.loadPixels();
            byte[] rgba = pixelsToRGBA(img.pixels);
            img.setId(PWebGPU.imageCreate(img.pixelWidth, img.pixelHeight, rgba));
        }
        return img.getId();
    }

    // imageMode and the plain/scaled image() overloads are resolved to absolute
    // corner coordinates by PGraphics before reaching imageImpl, so a single
    // region draw covers every case (native image_mode stays CORNER).
    @Override
    protected void imageImpl(PImage img,
                             float x1, float y1, float x2, float y2,
                             int u1, int v1, int u2, int v2) {
        if (graphicsId == 0) {
            return;
        }
        long imageId = ensureImageId(img);
        PWebGPU.imageRegion(graphicsId, imageId,
                            x1, y1, x2 - x1, y2 - y1,
                            u1, v1, u2 - u1, v2 - v1);
    }

    // ── Pixels ──────────────────────────────────────────────────────────

    @Override
    public void loadPixels() {
        if (graphicsId == 0) {
            return;
        }
        int n = pixelWidth * pixelHeight;
        if (pixels == null || pixels.length != n) {
            pixels = new int[n];
        }
        int[] read = PWebGPU.graphicsReadback(graphicsId, n);
        System.arraycopy(read, 0, pixels, 0, Math.min(read.length, n));
    }

    @Override
    public void updatePixels() {
        if (graphicsId == 0 || pixels == null) {
            return;
        }
        PWebGPU.graphicsUpdate(graphicsId, pixels);
    }

    @Override
    public void updatePixels(int x, int y, int w, int h) {
        if (graphicsId == 0 || pixels == null) {
            return;
        }
        if (x < 0) { w += x; x = 0; }
        if (y < 0) { h += y; y = 0; }
        if (x + w > pixelWidth) {
            w = pixelWidth - x;
        }
        if (y + h > pixelHeight) {
            h = pixelHeight - y;
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        int[] region = new int[w * h];
        for (int row = 0; row < h; row++) {
            System.arraycopy(pixels, (y + row) * pixelWidth + x, region, row * w, w);
        }
        PWebGPU.graphicsUpdateRegion(graphicsId, x, y, w, h, region);
    }

    @Override
    public void set(int x, int y, int argb) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.graphicsSet(graphicsId, x, y, argb);
    }

    // ── Fill / stroke ───────────────────────────────────────────────────

    @Override
    protected void fillFromCalc() {
        super.fillFromCalc();
        if (graphicsId == 0) {
            return;
        }
        if (fill) {
            PWebGPU.setFill(graphicsId, fillR, fillG, fillB, fillA);
        } else {
            PWebGPU.noFill(graphicsId);
        }
    }

    @Override
    protected void strokeFromCalc() {
        super.strokeFromCalc();
        if (graphicsId == 0) {
            return;
        }
        if (stroke) {
            PWebGPU.setStrokeColor(graphicsId, strokeR, strokeG, strokeB, strokeA);
        } else {
            PWebGPU.noStroke(graphicsId);
        }
    }

    @Override
    public void strokeWeight(float weight) {
        super.strokeWeight(weight);
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.setStrokeWeight(graphicsId, weight);
    }

    @Override
    public void noFill() {
        super.noFill();
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.noFill(graphicsId);
    }

    @Override
    public void noStroke() {
        super.noStroke();
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.noStroke(graphicsId);
    }

    @Override
    protected void tintFromCalc() {
        super.tintFromCalc();
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.tint(graphicsId, tintR, tintG, tintB, tintA);
    }

    @Override
    public void noTint() {
        super.noTint();
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.noTint(graphicsId);
    }

    @Override
    public void clear() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.clear(graphicsId);
    }

    @Override
    public void strokeCap(int cap) {
        super.strokeCap(cap);
        if (graphicsId == 0) {
            return;
        }
        byte nativeCap = switch (cap) {
            case ROUND -> PWebGPU.STROKE_CAP_ROUND;
            case SQUARE -> PWebGPU.STROKE_CAP_SQUARE;
            case PROJECT -> PWebGPU.STROKE_CAP_PROJECT;
            default -> PWebGPU.STROKE_CAP_ROUND;
        };
        PWebGPU.setStrokeCap(graphicsId, nativeCap);
    }

    @Override
    public void strokeJoin(int join) {
        super.strokeJoin(join);
        if (graphicsId == 0) {
            return;
        }
        byte nativeJoin = switch (join) {
            case ROUND -> PWebGPU.STROKE_JOIN_ROUND;
            case MITER -> PWebGPU.STROKE_JOIN_MITER;
            case BEVEL -> PWebGPU.STROKE_JOIN_BEVEL;
            default -> PWebGPU.STROKE_JOIN_ROUND;
        };
        PWebGPU.setStrokeJoin(graphicsId, nativeJoin);
    }

    // ── Blend mode ──────────────────────────────────────────────────────

    @Override
    public void blendMode(int mode) {
        super.blendMode(mode);
        if (graphicsId == 0) {
            return;
        }
        byte nativeMode = switch (mode) {
            case BLEND -> PWebGPU.BLEND_MODE_BLEND;
            case ADD -> PWebGPU.BLEND_MODE_ADD;
            case SUBTRACT -> PWebGPU.BLEND_MODE_SUBTRACT;
            case DARKEST -> PWebGPU.BLEND_MODE_DARKEST;
            case LIGHTEST -> PWebGPU.BLEND_MODE_LIGHTEST;
            case DIFFERENCE -> PWebGPU.BLEND_MODE_DIFFERENCE;
            case EXCLUSION -> PWebGPU.BLEND_MODE_EXCLUSION;
            case MULTIPLY -> PWebGPU.BLEND_MODE_MULTIPLY;
            case SCREEN -> PWebGPU.BLEND_MODE_SCREEN;
            case REPLACE -> PWebGPU.BLEND_MODE_REPLACE;
            default -> PWebGPU.BLEND_MODE_BLEND;
        };
        PWebGPU.setBlendMode(graphicsId, nativeMode);
    }

    // ── Custom blend factors/operations, for {@link #customBlendMode} ───
    public static final int BLEND_ZERO                = 0;
    public static final int BLEND_ONE                 = 1;
    public static final int BLEND_SRC                 = 2;
    public static final int BLEND_ONE_MINUS_SRC       = 3;
    public static final int BLEND_SRC_ALPHA           = 4;
    public static final int BLEND_ONE_MINUS_SRC_ALPHA = 5;
    public static final int BLEND_DST                 = 6;
    public static final int BLEND_ONE_MINUS_DST       = 7;
    public static final int BLEND_DST_ALPHA           = 8;
    public static final int BLEND_ONE_MINUS_DST_ALPHA = 9;
    public static final int BLEND_SRC_ALPHA_SATURATED = 10;
    public static final int BLEND_OP_ADD              = 0;
    public static final int BLEND_OP_SUBTRACT         = 1;
    public static final int BLEND_OP_REVERSE_SUBTRACT = 2;
    public static final int BLEND_OP_MIN              = 3;
    public static final int BLEND_OP_MAX              = 4;

    /**
     * Set a fully custom blend state from the {@code BLEND_*} factor and
     * {@code BLEND_OP_*} operation constants, e.g. classic alpha-over is
     * {@code customBlendMode(BLEND_SRC_ALPHA, BLEND_ONE_MINUS_SRC_ALPHA,
     * BLEND_OP_ADD, BLEND_ONE, BLEND_ONE_MINUS_SRC_ALPHA, BLEND_OP_ADD)}.
     * Reset with {@link #blendMode(int)}.
     */
    public void customBlendMode(int colorSrc, int colorDst, int colorOp,
                                int alphaSrc, int alphaDst, int alphaOp) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.setCustomBlendMode(graphicsId, (byte) colorSrc, (byte) colorDst, (byte) colorOp,
                                   (byte) alphaSrc, (byte) alphaDst, (byte) alphaOp);
    }

    // ── 2D primitives ───────────────────────────────────────────────────

    @Override
    protected void rectImpl(float x1, float y1, float x2, float y2) {
        rectImpl(x1, y1, x2, y2, 0, 0, 0, 0);
    }

    @Override
    protected void rectImpl(float x1, float y1, float x2, float y2,
                            float tl, float tr, float br, float bl) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.rect(graphicsId, x1, y1, x2 - x1, y2 - y1, tl, tr, br, bl);
    }

    @Override
    protected void ellipseImpl(float a, float b, float c, float d) {
        if (graphicsId == 0) {
            return;
        }
        // PGraphics.ellipse() applies ellipseMode and passes corner-form
        // (top-left + width/height) to ellipseImpl. The native call expects
        // center coords, so convert back here.
        PWebGPU.ellipse(graphicsId, a + c / 2f, b + d / 2f, c, d);
    }

    @Override
    protected void arcImpl(float a, float b, float c, float d,
                           float start, float stop, int mode) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.arc(graphicsId, a, b, c, d, start, stop, (byte) mode);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.line(graphicsId, x1, y1, x2, y2);
    }

    @Override
    public void point(float x, float y) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.point(graphicsId, x, y);
    }

    @Override
    public void triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.triangle(graphicsId, x1, y1, x2, y2, x3, y3);
    }

    @Override
    public void quad(float x1, float y1, float x2, float y2,
                     float x3, float y3, float x4, float y4) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.quad(graphicsId, x1, y1, x2, y2, x3, y3, x4, y4);
    }

    // ── Curves ──────────────────────────────────────────────────────────

    @Override
    public void bezier(float x1, float y1, float x2, float y2,
                       float x3, float y3, float x4, float y4) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.bezier(graphicsId, x1, y1, x2, y2, x3, y3, x4, y4);
    }

    @Override
    public void curve(float x1, float y1, float x2, float y2,
                      float x3, float y3, float x4, float y4) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.curve(graphicsId, x1, y1, x2, y2, x3, y3, x4, y4);
    }

    // ── 3D shapes ───────────────────────────────────────────────────────

    @Override
    public void box(float w, float h, float d) {
        if (graphicsId == 0) {
            return;
        }
        long boxGeometry = PWebGPU.geometryBox(w, h, d);
        PWebGPU.model(graphicsId, boxGeometry);
        pendingDestroy.add(boxGeometry);
    }

    @Override
    public void sphere(float r) {
        if (graphicsId == 0) {
            return;
        }
        long sphereGeometry = PWebGPU.geometrySphere(r, sphereDetailU, sphereDetailV);
        PWebGPU.model(graphicsId, sphereGeometry);
        pendingDestroy.add(sphereGeometry);
    }

    public void cone(float radius, float height) {
        cone(radius, height, 24);
    }

    public void cone(float radius, float height, int detail) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.cone(graphicsId, radius, height, detail);
    }

    public void cylinder(float radius, float height) {
        cylinder(radius, height, 24);
    }

    public void cylinder(float radius, float height, int detail) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.cylinder(graphicsId, radius, height, detail);
    }

    public void torus(float radius, float tubeRadius) {
        torus(radius, tubeRadius, 24, 16);
    }

    public void torus(float radius, float tubeRadius, int majorSegments, int minorSegments) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.torus(graphicsId, radius, tubeRadius, majorSegments, minorSegments);
    }

    public void plane(float width, float height) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.plane(graphicsId, width, height);
    }

    public void capsule(float radius, float length) {
        capsule(radius, length, 24);
    }

    public void capsule(float radius, float length, int detail) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.capsule(graphicsId, radius, length, detail);
    }

    public void conicalFrustum(float radiusTop, float radiusBottom, float height) {
        conicalFrustum(radiusTop, radiusBottom, height, 24);
    }

    public void conicalFrustum(float radiusTop, float radiusBottom, float height, int detail) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.conicalFrustum(graphicsId, radiusTop, radiusBottom, height, detail);
    }

    public void tetrahedron(float radius) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.tetrahedron(graphicsId, radius);
    }

    // ── Vertex shapes ───────────────────────────────────────────────────

    @Override
    public void beginShape(int kind) {
        super.beginShape(kind);
        if (graphicsId == 0) {
            return;
        }
        shapeKind = kind;
        byte topology = shapeKindToTopology(kind);
        currentGeometry = PWebGPU.geometryCreate(topology);
    }

    private byte shapeKindToTopology(int kind) {
        return switch (kind) {
            case POINTS -> PWebGPU.TOPOLOGY_POINT_LIST;
            case LINES -> PWebGPU.TOPOLOGY_LINE_LIST;
            case LINE_STRIP -> PWebGPU.TOPOLOGY_LINE_STRIP;
            case TRIANGLES -> PWebGPU.TOPOLOGY_TRIANGLE_LIST;
            case TRIANGLE_STRIP -> PWebGPU.TOPOLOGY_TRIANGLE_STRIP;
            case TRIANGLE_FAN, QUADS, QUAD_STRIP, POLYGON -> PWebGPU.TOPOLOGY_TRIANGLE_LIST;
            default -> PWebGPU.TOPOLOGY_TRIANGLE_LIST;
        };
    }

    @Override
    public void normal(float nx, float ny, float nz) {
        normalX = nx;
        normalY = ny;
        normalZ = nz;
    }

    @Override
    public void vertex(float x, float y) {
        vertex(x, y, 0);
    }

    @Override
    public void vertex(float x, float y, float z) {
        if (currentGeometry == 0) {
            return;
        }
        PWebGPU.geometryColor(currentGeometry, fillR, fillG, fillB, fillA);
        PWebGPU.geometryNormal(currentGeometry, normalX, normalY, normalZ);
        PWebGPU.geometryVertex(currentGeometry, x, y, z);
    }

    @Override
    public void endShape(int mode) {
        if (graphicsId == 0 || currentGeometry == 0) {
            return;
        }

        if (shapeKind == QUADS) {
            int vertexCount = PWebGPU.geometryVertexCount(currentGeometry);
            for (int i = 0; i < vertexCount; i += 4) {
                PWebGPU.geometryIndex(currentGeometry, i);
                PWebGPU.geometryIndex(currentGeometry, i + 1);
                PWebGPU.geometryIndex(currentGeometry, i + 2);
                PWebGPU.geometryIndex(currentGeometry, i);
                PWebGPU.geometryIndex(currentGeometry, i + 2);
                PWebGPU.geometryIndex(currentGeometry, i + 3);
            }
        }

        PWebGPU.model(graphicsId, currentGeometry);
        pendingDestroy.add(currentGeometry);
        currentGeometry = 0;
    }

    // ── Transform matrix ────────────────────────────────────────────────

    @Override
    public void pushMatrix() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.pushMatrix(graphicsId);
    }

    @Override
    public void popMatrix() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.popMatrix(graphicsId);
    }

    @Override
    public void resetMatrix() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.resetMatrix(graphicsId);
    }

    @Override
    public void push() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.push(graphicsId);
    }

    @Override
    public void pop() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.pop(graphicsId);
    }

    @Override
    public void pushStyle() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.pushStyle(graphicsId);
    }

    @Override
    public void popStyle() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.popStyle(graphicsId);
    }

    @Override
    public void applyMatrix(float n00, float n01, float n02,
                            float n10, float n11, float n12) {
        applyMatrix(n00, n01, n02, 0,
                    n10, n11, n12, 0,
                    0,   0,   1,   0,
                    0,   0,   0,   1);
    }

    @Override
    public void applyMatrix(float n00, float n01, float n02, float n03,
                            float n10, float n11, float n12, float n13,
                            float n20, float n21, float n22, float n23,
                            float n30, float n31, float n32, float n33) {
        if (graphicsId == 0) {
            return;
        }
        // Processing supplies row-major (n{row}{col}); the native matrix is column-major.
        PWebGPU.applyMatrix(graphicsId, new float[] {
            n00, n10, n20, n30,
            n01, n11, n21, n31,
            n02, n12, n22, n32,
            n03, n13, n23, n33,
        });
    }

    @Override
    public PMatrix getMatrix() {
        return getMatrix((PMatrix3D) null);
    }

    @Override
    public PMatrix3D getMatrix(PMatrix3D target) {
        if (target == null) {
            target = new PMatrix3D();
        }
        if (graphicsId == 0) {
            return target;
        }
        float[] c = PWebGPU.getMatrix(graphicsId); // column-major
        if (c.length >= 16) {
            // column-major → PMatrix3D's row-major (m{row}{col})
            target.set(c[0], c[4], c[8],  c[12],
                       c[1], c[5], c[9],  c[13],
                       c[2], c[6], c[10], c[14],
                       c[3], c[7], c[11], c[15]);
        }
        return target;
    }

    @Override
    public PMatrix2D getMatrix(PMatrix2D target) {
        if (target == null) {
            target = new PMatrix2D();
        }
        if (graphicsId == 0) {
            return target;
        }
        float[] c = PWebGPU.getMatrix(graphicsId); // column-major
        if (c.length >= 16) {
            target.set(c[0], c[4], c[12],
                       c[1], c[5], c[13]);
        }
        return target;
    }

    @Override
    public void translate(float x, float y) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.translate(graphicsId, x, y);
    }

    @Override
    public void rotate(float angle) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.rotate(graphicsId, angle);
    }

    @Override
    public void scale(float x, float y) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.scale(graphicsId, x, y);
    }

    @Override
    public void shearX(float angle) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.shearX(graphicsId, angle);
    }

    @Override
    public void shearY(float angle) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.shearY(graphicsId, angle);
    }

    // ── Coordinate mapping ──────────────────────────────────────────────

    @Override
    public float screenX(float x, float y) {
        return screenX(x, y, 0);
    }

    @Override
    public float screenX(float x, float y, float z) {
        if (graphicsId == 0) {
            return 0;
        }
        return PWebGPU.screenX(graphicsId, x, y, z);
    }

    @Override
    public float screenY(float x, float y) {
        return screenY(x, y, 0);
    }

    @Override
    public float screenY(float x, float y, float z) {
        if (graphicsId == 0) {
            return 0;
        }
        return PWebGPU.screenY(graphicsId, x, y, z);
    }

    @Override
    public float screenZ(float x, float y, float z) {
        if (graphicsId == 0) {
            return 0;
        }
        return PWebGPU.screenZ(graphicsId, x, y, z);
    }

    @Override
    public float modelX(float x, float y, float z) {
        if (graphicsId == 0) {
            return 0;
        }
        return PWebGPU.modelX(graphicsId, x, y, z);
    }

    @Override
    public float modelY(float x, float y, float z) {
        if (graphicsId == 0) {
            return 0;
        }
        return PWebGPU.modelY(graphicsId, x, y, z);
    }

    @Override
    public float modelZ(float x, float y, float z) {
        if (graphicsId == 0) {
            return 0;
        }
        return PWebGPU.modelZ(graphicsId, x, y, z);
    }

    // ── 3D camera / projection ──────────────────────────────────────────

    @Override
    public void camera(float eyeX, float eyeY, float eyeZ,
                       float centerX, float centerY, float centerZ,
                       float upX, float upY, float upZ) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.mode3d(graphicsId);
        PWebGPU.camera(graphicsId, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    public void cameraPosition(float x, float y, float z) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.transformSetPosition(graphicsId, x, y, z);
    }

    public void cameraLookAt(float x, float y, float z) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.transformLookAt(graphicsId, x, y, z);
    }

    /** Camera-controller modes for {@link #cameraControl(int)}. */
    public static final int ORBIT = 0;
    public static final int PAN = 1;
    public static final int FREE = 2;
    public static final int NONE = 3;

    /** Reset the camera to its default position. */
    @Override
    public void camera() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.cameraReset(graphicsId);
    }

    /**
     * Enable a built-in interactive camera controller, or {@link #NONE} to
     * disable it. {@code ORBIT} drags to orbit the center, {@code PAN} drags to
     * pan, {@code FREE} is free-fly.
     */
    public void cameraControl(int mode) {
        if (graphicsId == 0) {
            return;
        }
        switch (mode) {
            case ORBIT -> PWebGPU.orbitCamera(graphicsId);
            case PAN -> PWebGPU.panCamera(graphicsId);
            case FREE -> PWebGPU.freeCamera(graphicsId);
            case NONE -> PWebGPU.disableCameraController(graphicsId);
            default -> throw new IllegalArgumentException("Unknown camera control mode: " + mode);
        }
    }

    /** Point the orbit/pan camera at {@code (x, y, z)}. */
    public void cameraCenter(float x, float y, float z) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.cameraSetCenter(graphicsId, x, y, z);
    }

    /** Distance of the orbit camera from its center. */
    public void cameraDistance(float distance) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.cameraSetDistance(graphicsId, distance);
    }

    public void cameraMinDistance(float min) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.cameraSetMinDistance(graphicsId, min);
    }

    public void cameraMaxDistance(float max) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.cameraSetMaxDistance(graphicsId, max);
    }

    public void cameraSpeed(float speed) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.cameraSetSpeed(graphicsId, speed);
    }

    public void mode3d() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.mode3d(graphicsId);
    }

    @Override
    public void perspective(float fov, float aspect, float near, float far) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.mode3d(graphicsId);
        PWebGPU.perspective(graphicsId, fov, aspect, near, far);
    }

    @Override
    public void ortho(float left, float right, float bottom, float top, float near, float far) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.ortho(graphicsId, left, right, bottom, top, near, far);
    }

    // ── Lights ───────────────────────────────────────────────────────────

    @Override
    public void directionalLight(float r, float g, float b,
                                 float nx, float ny, float nz) {
        if (graphicsId == 0) return;
        long light = PWebGPU.lightCreateDirectional(graphicsId, r, g, b, 1.0f, 600.0f);
        PWebGPU.transformSetRotation(light, nx, ny, nz);
    }

    @Override
    public void pointLight(float r, float g, float b,
                           float x, float y, float z) {
        if (graphicsId == 0) return;
        long light = PWebGPU.lightCreatePoint(graphicsId, r, g, b, 1.0f, 100000.0f, 800.0f, 0.0f);
        PWebGPU.transformSetPosition(light, x, y, z);
    }

    public PLight directionalLight(float r, float g, float b, float illuminance) {
        if (graphicsId == 0) return null;
        return new PLightWebGPU(PWebGPU.lightCreateDirectional(graphicsId, r, g, b, 1.0f, illuminance));
    }

    public PLight pointLight(float r, float g, float b,
                             float intensity, float range, float radius,
                             float x, float y, float z) {
        if (graphicsId == 0) return null;
        long light = PWebGPU.lightCreatePoint(graphicsId, r, g, b, 1.0f, intensity, range, radius);
        PWebGPU.transformSetPosition(light, x, y, z);
        return new PLightWebGPU(light);
    }

    public PLight spotLight(float r, float g, float b,
                            float intensity, float range, float radius,
                            float innerAngle, float outerAngle) {
        if (graphicsId == 0) return null;
        return new PLightWebGPU(PWebGPU.lightCreateSpot(graphicsId, r, g, b, 1.0f,
                intensity, range, radius, innerAngle, outerAngle));
    }

    // ── Images / shapes ─────────────────────────────────────────────────

    public PImageWebGPU createImage(int width, int height, int format) {
        return new PImageWebGPU(width, height, format);
    }

    @Override
    public PShape createShape() {
        return new PShapeWebGPU(this, PShape.GEOMETRY);
    }

    @Override
    public PShape createShape(int type) {
        return new PShapeWebGPU(this, type);
    }

    @Override
    public PShape createShape(int kind, float... p) {
        switch (kind) {
            case BOX: {
                float w = p.length > 0 ? p[0] : 100;
                float h = p.length > 1 ? p[1] : w;
                float d = p.length > 2 ? p[2] : w;
                return PShapeWebGPU.createBox(this, w, h, d);
            }
            case SPHERE: {
                float r = p.length > 0 ? p[0] : 100;
                return PShapeWebGPU.createSphere(this, r, sphereDetailU, sphereDetailV);
            }
            default:
                return super.createShape(kind, p);
        }
    }

    /** Draw a shape's geometry — a {@code PShapeWebGPU} from {@code createShape}. */
    @Override
    public void shape(PShape shape) {
        if (graphicsId == 0) return;
        model(PShapeWebGPU.geometryId(shape));
    }

    /** Draw a mesh by native id — internal bridge for {@link PShapeWebGPU#draw}. */
    void model(long geometryId) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.model(graphicsId, geometryId);
    }

    // ── PParticles ──────────────────────────────────────────────────────��

    public PParticles createParticles(int capacity) {
        return new PParticlesWebGPU(capacity, Attribute.position());
    }

    public PParticles createParticles(int capacity, Attribute... attributes) {
        return new PParticlesWebGPU(capacity, attributes);
    }

    public PParticles createParticles(PShape source) {
        return PParticlesWebGPU.fromGeometryId(PShapeWebGPU.geometryId(source), Attribute.position());
    }

    public void particles(PParticles p, PShape shape) {
        if (graphicsId == 0) {
            return;
        }
        PParticlesWebGPU pw = (PParticlesWebGPU) p;
        // Bind the system's default (unlit) material so instances are visible
        // without a light or an explicit material(). Use the 3-arg overload to
        // supply your own.
        material(pw.defaultMaterial());
        PWebGPU.particlesDraw(graphicsId, pw.id(), PShapeWebGPU.geometryId(shape));
    }

    public void particles(PParticles p, PShape shape, PMaterial mat) {
        if (graphicsId == 0) {
            return;
        }
        material(mat);
        PWebGPU.particlesDraw(graphicsId, ((PParticlesWebGPU) p).id(), PShapeWebGPU.geometryId(shape));
    }

    public void particles(PParticles p) {
        if (graphicsId == 0) {
            return;
        }
        PParticlesWebGPU pw = (PParticlesWebGPU) p;
        material(pw.defaultMaterial());
        particles(pw, pw.defaultGeometry());
    }

    /**
     * Draw a dynamic-topology target: whatever primitives kernels added to
     * it this frame, with its own topology and per-vertex colors.
     */
    public void particles(PPrimitives primitives) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.particlesDrawTopology(graphicsId, primitives.field(), 0, primitives.topology());
    }

    public void fill(PBuffer colorBuffer) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.fillBuffer(graphicsId, ((PBufferWebGPU) colorBuffer).id());
    }

    // ── Materials ───────────────────────────────────────────────────────

    public PMaterial createMaterial() {
        return PMaterialWebGPU.pbr();
    }

    public PCompute createCompute(String wgslSource) {
        return new PComputeWebGPU(PWebGPU.computeCreate(PWebGPU.shaderCreate(wgslSource)));
    }

    /**
     * Load a compute shader from a {@code .wgsl}/{@code .wesl} file path.
     * Unlike {@link #createCompute(String)}, the WESL compiler resolves
     * {@code import} statements (e.g. {@code lygia::…},
     * {@code processing::prims}) against the loader's module roots.
     */
    public PCompute loadCompute(String path) {
        return new PComputeWebGPU(PWebGPU.computeCreate(PWebGPU.shaderLoad(path)));
    }

    public PBuffer createBuffer(long sizeBytes) {
        return new PBufferWebGPU(sizeBytes);
    }

    public PBuffer createBuffer(float[] data) {
        return new PBufferWebGPU(data);
    }

    public PBuffer createBuffer(byte[] data) {
        return new PBufferWebGPU(data);
    }

    public Gltf loadGltf(String path) {
        return new Gltf(PWebGPU.gltfLoad(graphicsId, path), this);
    }

    @Override
    public PShader loadShader(String fragFilename) {
        return PShaderWebGPU.load(fragFilename);
    }

    @Override
    public PShader loadShader(String fragFilename, String vertFilename) {
        // A WGSL module carries both stages, so the frag file is the whole shader.
        return PShaderWebGPU.load(fragFilename);
    }

    @Override
    public void shader(PShader shader) {
        if (graphicsId == 0 || !(shader instanceof PShaderWebGPU sh)) {
            return;
        }
        material(sh.material());
    }

    @Override
    public void shader(PShader shader, int kind) {
        shader(shader);
    }

    @Override
    public void resetShader() {
        if (graphicsId == 0) {
            return;
        }
        material(createMaterial());
    }

    @Override
    public void resetShader(int kind) {
        resetShader();
    }

    // ── Filters ─────────────────────────────────────────────────────────

    @Override
    public void filter(int kind) {
        if (graphicsId == 0) {
            return;
        }
        long filterId = builtinFilter(kind);
        if (filterId != 0) {
            PWebGPU.graphicsApplyFilter(graphicsId, filterId);
        }
    }

    @Override
    public void filter(int kind, float param) {
        if (graphicsId == 0) {
            return;
        }
        long filterId = builtinFilter(kind);
        if (filterId == 0) {
            return;
        }
        switch (kind) {
            case BLUR      -> PWebGPU.computeSetFloat(filterId, "radius", param);
            case THRESHOLD -> PWebGPU.computeSetFloat(filterId, "cutoff", param);
            case POSTERIZE -> PWebGPU.computeSetUInt(filterId, "levels", (int) param);
            default        -> { }
        }
        PWebGPU.graphicsApplyFilter(graphicsId, filterId);
    }

    @Override
    public void filter(PShader shader) {
        if (graphicsId == 0 || !(shader instanceof PShaderWebGPU sh)) {
            return;
        }
        PWebGPU.graphicsApplyFilter(graphicsId, sh.filterId());
    }

    private static long builtinFilter(int kind) {
        return switch (kind) {
            case BLUR      -> PWebGPU.filterBlur();
            case INVERT    -> PWebGPU.filterInvert();
            case GRAY      -> PWebGPU.filterGray();
            case THRESHOLD -> PWebGPU.filterThreshold();
            case POSTERIZE -> PWebGPU.filterPosterize();
            case OPAQUE    -> PWebGPU.filterOpaque();
            case ERODE     -> PWebGPU.filterErode();
            case DILATE    -> PWebGPU.filterDilate();
            default        -> 0;
        };
    }

    // ── Text ────────────────────────────────────────────────────────────

    @Override
    public PFont createFont(String name, float size, boolean smooth, char[] charset) {
        return new PFontWebGPU(PWebGPU.createFont(name), size);
    }

    /** Load a font from a file (e.g. .ttf / .otf) resolved against the sketch. */
    public PFont loadFont(String path) {
        return new PFontWebGPU(PWebGPU.loadFont(path), 0);
    }

    @Override
    protected void textFontImpl(PFont which, float size) {
        textFont = which;
        if (size > 0) {
            textSize = size;
        }
        if (graphicsId == 0 || !(which instanceof PFontWebGPU wf)) {
            return;
        }
        PWebGPU.textFont(graphicsId, wf.id());
        if (size > 0) {
            PWebGPU.textSize(graphicsId, size);
        }
    }

    @Override
    protected void textSizeImpl(float size) {
        textSize = size;
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textSize(graphicsId, size);
    }

    @Override
    public void textLeading(float leading) {
        super.textLeading(leading);
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textLeading(graphicsId, leading);
    }

    @Override
    public void textAlign(int alignX, int alignY) {
        super.textAlign(alignX, alignY);
        if (graphicsId == 0) {
            return;
        }
        byte h = switch (alignX) {
            case CENTER -> (byte) 1;
            case RIGHT -> (byte) 2;
            default -> (byte) 0; // LEFT
        };
        byte v = switch (alignY) {
            case TOP -> (byte) 1;
            case CENTER -> (byte) 2;
            case BOTTOM -> (byte) 3;
            default -> (byte) 0; // BASELINE
        };
        PWebGPU.textAlign(graphicsId, h, v);
    }

    @Override
    public float textAscent() {
        return graphicsId == 0 ? 0 : PWebGPU.textAscent(graphicsId);
    }

    @Override
    public float textDescent() {
        return graphicsId == 0 ? 0 : PWebGPU.textDescent(graphicsId);
    }

    @Override
    protected float textWidthImpl(char[] buffer, int start, int stop) {
        if (graphicsId == 0) {
            return 0;
        }
        return PWebGPU.textWidth(graphicsId, new String(buffer, start, stop - start));
    }

    @Override
    public void text(String str, float x, float y) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.text(graphicsId, str, x, y);
    }

    @Override
    public void text(String str, float x, float y, float z) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.text3d(graphicsId, str, x, y, z);
    }

    @Override
    public void text(char c, float x, float y) {
        text(String.valueOf(c), x, y);
    }

    @Override
    public void text(char[] chars, int start, int stop, float x, float y) {
        text(new String(chars, start, stop - start), x, y);
    }

    @Override
    public void text(int num, float x, float y) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textInt(graphicsId, num, x, y);
    }

    @Override
    public void text(float num, float x, float y) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textFloat(graphicsId, num, x, y);
    }

    @Override
    public void text(String str, float x1, float y1, float x2, float y2) {
        if (graphicsId == 0) {
            return;
        }
        float x = x1, y = y1, w = x2, h = y2; // CORNER: x2,y2 are width,height
        switch (rectMode) {
            case CORNERS -> { w = x2 - x1; h = y2 - y1; }
            case RADIUS -> { x = x1 - x2; y = y1 - y2; w = x2 * 2; h = y2 * 2; }
            case CENTER -> { x = x1 - x2 / 2; y = y1 - y2 / 2; }
            default -> { }
        }
        PWebGPU.textBox(graphicsId, str, x, y, w, h);
    }

    // Text style modes for textStyle() (0 = normal).
    public static final int ITALIC = 1;
    public static final int BOLD = 2;
    public static final int BOLDITALIC = 3;
    // Text wrap modes for textWrap().
    public static final int WORD = 0;
    public static final int CHAR = 1;

    /** Font style: {@code 0} (normal), {@link #ITALIC}, {@link #BOLD}, {@link #BOLDITALIC}. */
    public void textStyle(int style) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textStyle(graphicsId, (byte) style);
    }

    /** Variable-font weight (typically 100–900). */
    public void textWeight(float weight) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textWeight(graphicsId, weight);
    }

    /** Line-wrap mode inside a text box: {@link #WORD} or {@link #CHAR}. */
    public void textWrap(int mode) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textWrap(graphicsId, (byte) mode);
    }

    /** {@code [x, y, w, h]} bounding box of {@code str} drawn at {@code (x, y)}. */
    public float[] textBounds(String str, float x, float y) {
        return graphicsId == 0 ? new float[4] : PWebGPU.textBounds(graphicsId, str, x, y);
    }

    /** Set an OpenType variation axis (e.g. {@code "wght"}, 600). */
    public void textVariation(String tag, float value) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textVariation(graphicsId, tag, value);
    }

    public void clearTextVariations() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.clearTextVariations(graphicsId);
    }

    /** Enable an OpenType feature (e.g. {@code "smcp"}, 1). */
    public void textFeature(String tag, int value) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textFeature(graphicsId, tag, value);
    }

    public void noTextFeature(String tag) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.noTextFeature(graphicsId, tag);
    }

    public void clearTextFeatures() {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.clearTextFeatures(graphicsId);
    }

    /** Per-glyph colors (cycled) for the next {@code text()} call; flat RGBA array. */
    public void textGlyphColors(float[] rgba) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.textGlyphColors(graphicsId, rgba);
    }

    public void material(PMaterial mat) {
        if (graphicsId == 0) {
            return;
        }
        PWebGPU.material(graphicsId, ((PMaterialWebGPU) mat).id());
    }

    // ── Picking / unproject ──────────────────────────────────────────────

    /**
     * Inverse of {@link #screenX}: returns the world-space X coordinate of the
     * point at screen pixel ({@code sx}, {@code sy}) with normalized depth
     * {@code depth} in {@code [0, 1]} (0 = near plane, 1 = far plane).
     */
    public float worldX(float sx, float sy, float depth) {
        if (graphicsId == 0) return 0;
        return PWebGPU.graphicsWorldFromScreen(graphicsId, sx, sy, depth)[0];
    }

    public float worldY(float sx, float sy, float depth) {
        if (graphicsId == 0) return 0;
        return PWebGPU.graphicsWorldFromScreen(graphicsId, sx, sy, depth)[1];
    }

    public float worldZ(float sx, float sy, float depth) {
        if (graphicsId == 0) return 0;
        return PWebGPU.graphicsWorldFromScreen(graphicsId, sx, sy, depth)[2];
    }

    // ── Post-processing ──────────────────────────────────────────────────

    /**
     * Enable bloom post-processing. `intensity` controls bloom strength
     * (additive — values above 1.0 are valid). `threshold` is the HDR
     * brightness floor: pixels below this value bloom weakly, so a
     * threshold of ~1.0 isolates bloom to HDR-bright pixels (e.g. emissive
     * surfaces) for a more dramatic effect.
     */
    public void bloom(float intensity, float threshold) {
        if (graphicsId == 0) return;
        PWebGPU.graphicsSetBloom(graphicsId, intensity, threshold);
    }

    /** Convenience: bloom with no threshold (whole scene contributes). */
    public void bloom(float intensity) {
        bloom(intensity, 0.0f);
    }

    public void noBloom() {
        if (graphicsId == 0) return;
        PWebGPU.graphicsRemoveBloom(graphicsId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private byte[] pixelsToRGBA(int[] pixels) {
        byte[] rgba = new byte[pixels.length * 4];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            rgba[i * 4]     = (byte) ((pixel >> 16) & 0xFF);
            rgba[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF);
            rgba[i * 4 + 2] = (byte) (pixel & 0xFF);
            rgba[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF);
        }
        return rgba;
    }
}
