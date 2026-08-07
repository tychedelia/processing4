package processing.core;

public interface PParticles {

  int capacity();

  PBuffer buffer(String name);

  void timeStep(float seconds);

  void scatter(float radius);

  void update();

  void applyForce(float x, float y, float z);

  void applyForce(float x, float y);

  void gravity(float x, float y, float z);

  void gravity(float x, float y);

  void noise(float scale, float strength);

  void noise(float scale, float strength, float time);

  void curlNoise(float scale, float strength);

  void flock();

  void flock(float neighborDistance, float separationDistance);

  void attract(float x, float y, float z, float strength);

  void attract(float x, float y, float z, float strength, float radius);

  void repel(float x, float y, float z, float strength);

  void repel(float x, float y, float z, float strength, float radius);

  void vortex(float x, float y, float z, float strength);

  void vortex(float x, float y, float z, float strength, float radius);

  void drag(float amount);

  void age();

  void bounds(float radius);

  void bounds(float width, float height, float depth);

  void bounds(PShape shape);

  void apply(int kind, float... params);

  void apply(PCompute compute);

  PCompute createKernel(int kind);

  void emit(int n, PCompute source);

  void destroy();
}
