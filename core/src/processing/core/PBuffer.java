package processing.core;

/**
 * A GPU buffer: a block of memory a sketch can fill and read back, and that
 * compute shaders and materials can bind. This is the renderer-agnostic type
 * sketches declare; each renderer supplies its own implementation.
 */
public interface PBuffer {

  /** Size of the buffer in bytes. */
  long size();

  void write(float[] data);

  void write(byte[] data);

  byte[] readBytes();

  float[] readFloats();

  void destroy();
}
