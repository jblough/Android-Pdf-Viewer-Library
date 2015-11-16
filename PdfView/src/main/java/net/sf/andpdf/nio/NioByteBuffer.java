package net.sf.andpdf.nio;

import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * A wrapper for the java.nio.ByteBuffer class
 * 
 * @author Ferenc Hechler (ferenc@hechler.de)
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public final class NioByteBuffer extends ByteBuffer {

    /**
     * the underlying buffer
     */
    private java.nio.ByteBuffer nioBuf;

    public byte[] array() {
        return nioBuf.array();
    }

    public int arrayOffset() {
        return nioBuf.arrayOffset();
    }

    public CharBuffer asCharBuffer() {
        return nioBuf.asCharBuffer();
    }

    public DoubleBuffer asDoubleBuffer() {
        return nioBuf.asDoubleBuffer();
    }

    public FloatBuffer asFloatBuffer() {
        return nioBuf.asFloatBuffer();
    }

    public IntBuffer asIntBuffer() {
        return nioBuf.asIntBuffer();
    }

    public LongBuffer asLongBuffer() {
        return nioBuf.asLongBuffer();
    }

    public java.nio.ByteBuffer asReadOnlyBuffer() {
        return nioBuf.asReadOnlyBuffer();
    }

    public ShortBuffer asShortBuffer() {
        return nioBuf.asShortBuffer();
    }

    public int capacity() {
        return nioBuf.capacity();
    }

    public Buffer clear() {
        return nioBuf.clear();
    }

    public java.nio.ByteBuffer compact() {
        return nioBuf.compact();
    }

    public int compareTo(final java.nio.ByteBuffer otherBuffer) {
        return nioBuf.compareTo(otherBuffer);
    }

    public NioByteBuffer duplicate() {
        return new NioByteBuffer(nioBuf.duplicate());
    }

    @Override
    public boolean equals(final Object other) {
        return nioBuf.equals(other);
    }

    public void flip() {
        nioBuf.flip();
    }

    public byte get() {
        return nioBuf.get();
    }

    public void get(final byte[] dest, final int off, final int len) {
        nioBuf.get(dest, off, len);
    }

    public void get(final byte[] dest) {
        nioBuf.get(dest);
    }

    public byte get(final int index) {
        return nioBuf.get(index);
    }

    public char getChar() {
        return nioBuf.getChar();
    }

    public char getChar(final int index) {
        return nioBuf.getChar(index);
    }

    public double getDouble() {
        return nioBuf.getDouble();
    }

    public double getDouble(final int index) {
        return nioBuf.getDouble(index);
    }

    public float getFloat() {
        return nioBuf.getFloat();
    }

    public float getFloat(final int index) {
        return nioBuf.getFloat(index);
    }

    public int getInt() {
        return nioBuf.getInt();
    }

    public int getInt(final int index) {
        return nioBuf.getInt(index);
    }

    public long getLong() {
        return nioBuf.getLong();
    }

    public long getLong(final int index) {
        return nioBuf.getLong(index);
    }

    public short getShort() {
        return nioBuf.getShort();
    }

    public short getShort(final int index) {
        return nioBuf.getShort(index);
    }

    public boolean hasArray() {
        return nioBuf.hasArray();
    }

    @Override
    public int hashCode() {
        return nioBuf.hashCode();
    }

    public boolean hasRemaining() {
        return nioBuf.hasRemaining();
    }

    public boolean isDirect() {
        return nioBuf.isDirect();
    }

    public boolean isReadOnly() {
        return nioBuf.isReadOnly();
    }

    public int limit() {
        return nioBuf.limit();
    }

    public void limit(final int newLimit) {
        nioBuf.limit(newLimit);
    }

    public void mark() {
        nioBuf.mark();
    }

    public ByteOrder order() {
        return nioBuf.order();
    }

    public java.nio.ByteBuffer order(final ByteOrder byteOrder) {
        return nioBuf.order(byteOrder);
    }

    public int position() {
        return nioBuf.position();
    }

    public void position(final int newPosition) {
        nioBuf.position(newPosition);
    }

    public void put(final byte b) {
        nioBuf.put(b);
    }

    public NioByteBuffer put(final byte[] src, final int off, final int len) {
        nioBuf.put(src, off, len);
        return this;
    }

    public void put(final byte[] src) {
        nioBuf.put(src);
    }

    public ByteBuffer put(final java.nio.ByteBuffer src) {
        nioBuf.put(src);
        return this;
    }

    public void put(final ByteBuffer src) {
        nioBuf.put(src.toNIO());
    }

    public void put(final int index, final byte b) {
        nioBuf.put(index, b);
    }

    public void putChar(final char value) {
        nioBuf.putChar(value);
    }

    public NioByteBuffer putChar(final int index, final char value) {
        nioBuf.putChar(index, value);
        return this;
    }

    public NioByteBuffer putDouble(final double value) {
        nioBuf.putDouble(value);
        return this;
    }

    public NioByteBuffer putDouble(final int index, final double value) {
        nioBuf.putDouble(index, value);
        return this;
    }

    public NioByteBuffer putFloat(final float value) {
        nioBuf.putFloat(value);
        return this;
    }

    public NioByteBuffer putFloat(final int index, final float value) {
        nioBuf.putFloat(index, value);
        return this;
    }

    public void putInt(final int index, final int value) {
        nioBuf.putInt(index, value);
    }

    public void putInt(final int value) {
        nioBuf.putInt(value);
    }

    public void putLong(final int index, final long value) {
        nioBuf.putLong(index, value);
    }

    public void putLong(final long value) {
        nioBuf.putLong(value);
    }

    public void putShort(final int index, final short value) {
        nioBuf.putShort(index, value);
    }

    public void putShort(final short value) {
        nioBuf.putShort(value);
    }

    public int remaining() {
        return nioBuf.remaining();
    }

    public void reset() {
        nioBuf.reset();
    }

    public void rewind() {
        nioBuf.rewind();
    }

    public NioByteBuffer slice() {
        return new NioByteBuffer(nioBuf.slice());
    }

    @Override
    public String toString() {
        return nioBuf.toString();
    }

    public NioByteBuffer(final java.nio.ByteBuffer nioBuf) {
        this.nioBuf = nioBuf;
    }

    public java.nio.ByteBuffer toNIO() {
        return nioBuf;
    }

    public static NioByteBuffer fromNIO(final java.nio.ByteBuffer nioBuf) {
        return new NioByteBuffer(nioBuf);
    }

    public static NioByteBuffer allocate(final int i) {
        return new NioByteBuffer(java.nio.ByteBuffer.allocate(i));
    }

    public static NioByteBuffer wrap(final byte[] bytes) {
        return new NioByteBuffer(java.nio.ByteBuffer.wrap(bytes));
    }
}
