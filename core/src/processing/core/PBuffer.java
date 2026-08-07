package processing.core;

public interface PBuffer {

  long size();

  void write(float[] data);

  void write(byte[] data);

  byte[] readBytes();

  float[] readFloats();

  void destroy();
}
