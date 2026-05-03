package processing.webgpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Buffer {

    private long id;
    private final boolean borrowed;

    Buffer(long id, boolean borrowed) {
        this.id = id;
        this.borrowed = borrowed;
    }

    public Buffer(long sizeBytes) {
        this.id = PWebGPU.bufferCreate(sizeBytes);
        this.borrowed = false;
    }

    public Buffer(float[] data) {
        byte[] bytes = floatsToBytes(data);
        this.id = PWebGPU.bufferCreateWithData(bytes);
        this.borrowed = false;
    }

    public Buffer(byte[] data) {
        this.id = PWebGPU.bufferCreateWithData(data);
        this.borrowed = false;
    }

    public long id() {
        return id;
    }

    public long size() {
        return PWebGPU.bufferSize(id);
    }

    public void write(float[] data) {
        PWebGPU.bufferWrite(id, floatsToBytes(data));
    }

    public void write(byte[] data) {
        PWebGPU.bufferWrite(id, data);
    }

    public byte[] readBytes() {
        return PWebGPU.bufferRead(id);
    }

    public float[] readFloats() {
        byte[] bytes = readBytes();
        float[] floats = new float[bytes.length / 4];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floats);
        return floats;
    }

    public void destroy() {
        if (id != 0 && !borrowed) {
            PWebGPU.bufferDestroy(id);
            id = 0;
        }
    }

    static byte[] floatsToBytes(float[] floats) {
        ByteBuffer buf = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buf.asFloatBuffer().put(floats);
        return buf.array();
    }
}
