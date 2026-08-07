package processing.webgpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import processing.core.PBuffer;

public class PBufferWebGPU implements PBuffer {

    private long id;
    private final boolean borrowed;

    PBufferWebGPU(long id, boolean borrowed) {
        this.id = id;
        this.borrowed = borrowed;
    }

    PBufferWebGPU(long sizeBytes) {
        this.id = PWebGPU.bufferCreate(sizeBytes);
        this.borrowed = false;
    }

    PBufferWebGPU(float[] data) {
        byte[] bytes = floatsToBytes(data);
        this.id = PWebGPU.bufferCreateWithData(bytes);
        this.borrowed = false;
    }

    PBufferWebGPU(byte[] data) {
        this.id = PWebGPU.bufferCreateWithData(data);
        this.borrowed = false;
    }

    long id() {
        return id;
    }

    @Override
    public long size() {
        return PWebGPU.bufferSize(id);
    }

    @Override
    public void write(float[] data) {
        PWebGPU.bufferWrite(id, floatsToBytes(data));
    }

    @Override
    public void write(byte[] data) {
        PWebGPU.bufferWrite(id, data);
    }

    @Override
    public byte[] readBytes() {
        return PWebGPU.bufferRead(id);
    }

    @Override
    public float[] readFloats() {
        byte[] bytes = readBytes();
        float[] floats = new float[bytes.length / 4];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floats);
        return floats;
    }

    @Override
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
