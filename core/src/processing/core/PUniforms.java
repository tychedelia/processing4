package processing.core;

/**
 * The named-parameter setter surface shared by render shaders
 * ({@link PShader}), compute shaders ({@link PCompute}), and materials
 * ({@link PMaterial}).
 *
 * @webref rendering:shaders
 * @webBrief The named-parameter setters shared by shaders, computes, and materials
 */
public interface PUniforms {

  /**
   * Sets a uniform variable inside the program to modify the effect while it
   * runs.
   *
   * @webref rendering:shaders
   * @webBrief Sets a variable within the shader
   * @param name the name of the uniform variable to modify
   * @param x first component of the variable to modify
   */
  void set(String name, int x);

  /**
   * @param y second component of the variable to modify. The variable has to be declared with an array/vector type in the shader (i.e.: int[2], vec2)
   */
  void set(String name, int x, int y);

  /**
   * @param z third component of the variable to modify. The variable has to be declared with an array/vector type in the shader (i.e.: int[3], vec3)
   */
  void set(String name, int x, int y, int z);

  /**
   * @param w fourth component of the variable to modify. The variable has to be declared with an array/vector type in the shader (i.e.: int[4], vec4)
   */
  void set(String name, int x, int y, int z, int w);

  void set(String name, float x);
  void set(String name, float x, float y);
  void set(String name, float x, float y, float z);
  void set(String name, float x, float y, float z, float w);

  /**
   * @param vec modifies all the components of an array/vector uniform variable. PVector can only be used if the type of the variable is vec3.
   */
  void set(String name, PVector vec);

  void set(String name, boolean x);
  void set(String name, boolean x, boolean y);
  void set(String name, boolean x, boolean y, boolean z);
  void set(String name, boolean x, boolean y, boolean z, boolean w);

  void set(String name, int[] vec);

  /**
   * @param ncoords number of coordinates per element, max 4
   */
  void set(String name, int[] vec, int ncoords);

  void set(String name, float[] vec);
  void set(String name, float[] vec, int ncoords);

  void set(String name, boolean[] vec);
  void set(String name, boolean[] boolvec, int ncoords);

  /**
   * @param mat matrix of values
   */
  void set(String name, PMatrix2D mat);

  void set(String name, PMatrix3D mat);

  /**
   * @param use3x3 enforces the matrix is 3 x 3
   */
  void set(String name, PMatrix3D mat, boolean use3x3);

  /**
   * @param tex sets the sampler uniform variable to read from this image texture
   */
  void set(String name, PImage tex);

  void set(String name, PBuffer buffer);
}
