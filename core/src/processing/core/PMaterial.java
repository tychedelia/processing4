package processing.core;

public interface PMaterial extends PUniforms {

    void albedo(float r, float g, float b, float a);

    void metalness(float value);

    void roughness(float value);

    void reflectance(float value);

    void emissive(float r, float g, float b, float a);

    void opaque();

    void transparent();

    void mask(float cutoff);

    void doubleSided(boolean value);

    void unlit(boolean value);

    void depthWrite(boolean value);

    void destroy();
}
