package processing.core;

public interface PCompute extends PUniforms {

  void dispatch(int x, int y, int z);

  void destroy();
}
