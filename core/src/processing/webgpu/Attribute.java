package processing.webgpu;

public class Attribute {

    public static final int FLOAT = 1;
    public static final int FLOAT2 = 2;
    public static final int FLOAT3 = 3;
    public static final int FLOAT4 = 4;

    private final long id;

    Attribute(long id) {
        this.id = id;
    }

    public Attribute(String name, int format) {
        this.id = PWebGPU.geometryAttributeCreate(name, (byte) format);
    }

    public long id() {
        return id;
    }

    public String name() {
        return PWebGPU.geometryAttributeName(id);
    }

    public int format() {
        return PWebGPU.geometryAttributeFormat(id);
    }

    public int floatCount() {
        return format();
    }

    public int byteSize() {
        return floatCount() * 4;
    }

    public static Attribute position() {
        return new Attribute(PWebGPU.geometryAttributePosition());
    }

    public static Attribute normal() {
        return new Attribute(PWebGPU.geometryAttributeNormal());
    }

    public static Attribute color() {
        return new Attribute(PWebGPU.geometryAttributeColor());
    }

    public static Attribute uv() {
        return new Attribute(PWebGPU.geometryAttributeUv());
    }

    public static Attribute rotation() {
        return new Attribute(PWebGPU.geometryAttributeRotation());
    }

    public static Attribute scale() {
        return new Attribute(PWebGPU.geometryAttributeScale());
    }

    public static Attribute life() {
        return new Attribute(PWebGPU.geometryAttributeLife());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Attribute a && a.id == id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
