package processing.webgpu;

import processing.core.PFont;

/**
 * WebGPU implementation of {@link PFont}, wrapping a native font handle. Text is
 * laid out and rendered natively, so the software glyph data on {@link PFont} is
 * unused here — only the native {@link #id()} matters.
 */
public class PFontWebGPU extends PFont {

    private final long id;

    PFontWebGPU(long id, float size) {
        super();
        this.id = id;
        this.size = size > 0 ? (int) size : 12;
    }

    long id() {
        return id;
    }

    /** A variable-font variation axis (tag plus its min/max/default value). */
    public record FontAxis(String tag, float min, float max, float defaultValue) {}

    /** Number of variable-font variation axes ({@code 0} if not variable). */
    public int variationCount() {
        return PWebGPU.fontVariationCount(id);
    }

    /** The variation axis at {@code index}, or {@code null} if out of range. */
    public FontAxis variation(int index) {
        return PWebGPU.fontVariation(id, index);
    }
}
