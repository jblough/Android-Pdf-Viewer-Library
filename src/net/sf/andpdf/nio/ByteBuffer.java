package net.sf.andpdf.nio;

import java.nio.MappedByteBuffer;

public abstract class ByteBuffer {

	public abstract int position();

	public abstract void position(int position);

	public abstract byte get();

	public abstract int remaining();

	public abstract byte get(int position);

	public abstract ByteBuffer slice();

	public abstract void limit(int length);

	public abstract void get(byte[] outBuf);

	public abstract void rewind();

	public abstract int limit();

	public abstract boolean hasArray();

	public abstract int arrayOffset();

	public abstract byte[] array();

	public abstract void flip();

	public abstract ByteBuffer duplicate();

	public abstract char getChar(int position);

	public abstract int getInt();

	public abstract long getLong();

	public abstract char getChar();

	public abstract short getShort();

	public abstract void put(int index, byte b);

	public abstract void put(byte b);

	public abstract void putInt(int i);

	public abstract void putShort(short s);

	public abstract void mark();

	public abstract void put(ByteBuffer data);

	public abstract void reset();

	public abstract void putInt(int index, int value);

	public abstract void putLong(long value);

	public abstract void putChar(char value);

	public abstract void put(byte[] data);

	public abstract void get(byte[] outBuf, int outOffset, int length);

	public abstract java.nio.ByteBuffer toNIO();

	public abstract boolean hasRemaining();

	
	public static boolean sUseNIO = true;
	public static ByteBuffer NEW(MappedByteBuffer map) {
		return new NioByteBuffer(map);
	}
	public static ByteBuffer NEW(byte[] buf) {
		return new ArrayBackedByteBuffer(buf);
	}
	public static ByteBuffer wrap(byte[] decode) {
		if (sUseNIO)
			return NioByteBuffer.wrap(decode);
		else
			return ArrayBackedByteBuffer.wrap(decode);
	}
	public static ByteBuffer fromNIO(java.nio.ByteBuffer byteBuf) {
//		if (USE_NIO)
			return NioByteBuffer.fromNIO(byteBuf);
//		else
//			return OwnByteBuffer.fromNIO(byteBuf);
	}
	public static ByteBuffer allocate(int i) {
		if (sUseNIO)
			return NioByteBuffer.allocate(i);
		else
			return ArrayBackedByteBuffer.allocate(i);
	}

}