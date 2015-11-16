package net.sf.andpdf.nio;

/**
 * A byte-array backed ByteBuffer
 *
 * @author Ferenc Hechler (ferenc@hechler.de)
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public final class ArrayBackedByteBuffer extends ByteBuffer {

    /**
     * the byte array backing this buffer
     */
    private final byte[] buf;
    /**
     * offset to the first byte to use
     */
    private int ofs;
    /**
     * current buffer position
     */
    private int pos;
    /**
     * buffer size
     */
    private int siz;
    /**
     * a marker inside the buffer
     */
    private int mrk;

    /**
     * Create a new ByteBuffer from an existing array
     *
     * @param buf   the array backing this buffer
     */
    public ArrayBackedByteBuffer(final byte[] buf) {
        this(buf, 0, 0, (buf == null ? 0 : buf.length));
    }

    /**
     * Create a new ByteBuffer from an existing array
     *
     * @param buf   the array backing this buffer
     * @param ofs   offset to the first byte to use
     * @param pos   current buffer position
     * @param siz   total buffer size
     */
    public ArrayBackedByteBuffer(final byte[] buf, final int ofs, final int pos, final int siz) {
        this.buf = buf;
        this.ofs = ofs;
        this.pos = pos;
        this.siz = siz;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#position()
     */
    public int position() {
        return pos - ofs;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#position(int)
     */
    public void position(final int position) {
        // TODO: check range 0..length-1
        pos = position + ofs;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#get()
     */
    public byte get() {
        // TODO: check range
        return buf[pos++];
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#remaining()
     */
    public int remaining() {
        return siz - pos;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#get(int)
     */
    public byte get(final int position) {
        return buf[position + ofs];
    }

    public static ByteBuffer allocate(final int size) {
        return new ArrayBackedByteBuffer(new byte[size]);
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#slice()
     */
    public ByteBuffer slice() {
        return new ArrayBackedByteBuffer(buf, pos, pos, siz);
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#limit(int)
     */
    public void limit(final int length) {
        // TODO: check range 0..buf.length
        siz = ofs + length;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#get(byte[])
     */
    public void get(final byte[] outBuf) {
        System.arraycopy(buf, pos, outBuf, 0, outBuf.length);
        pos += outBuf.length;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#rewind()
     */
    public void rewind() {
        pos = ofs;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#limit()
     */
    public int limit() {
        return siz - ofs;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#hasArray()
     */
    public boolean hasArray() {
        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#arrayOffset()
     */
    public int arrayOffset() {
        return ofs;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#array()
     */
    public byte[] array() {
        return buf;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#flip()
     */
    public void flip() {
        siz = pos;
        pos = ofs;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#duplicate()
     */
    public ByteBuffer duplicate() {
        return new ArrayBackedByteBuffer(buf, ofs, pos, siz);
    }

    public static ByteBuffer wrap(final byte[] bytes) {
        return new ArrayBackedByteBuffer(bytes);
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#getChar(int)
     */
    public char getChar(final int position) {
        // TODO: check current byteorder, assume BIG_ENDIAN
        int result = get(position) & 0xff;
        result = (result << 8) + (get(position + 1) & 0xff);
        return (char) result;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#getInt()
     */
    public int getInt() {
        // TODO: check current byteorder, assume BIG_ENDIAN
        final int pos_ = this.pos;
        final byte[] buf_ = this.buf;
        final int result = ((buf_[pos_] & 0xff) << 24) + ((buf_[pos_ + 1] & 0xff) << 16) + ((buf_[pos_ + 2] & 0xff) << 8) + (buf_[pos_ + 3] & 0xff);

        this.pos += 4;

        return result;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#getLong()
     */
    public long getLong() {
        // TODO: check current byteorder, assume BIG_ENDIAN
        long result = get() & 0xff;
        result = (result << 8) + (get() & 0xff);
        result = (result << 8) + (get() & 0xff);
        result = (result << 8) + (get() & 0xff);
        result = (result << 8) + (get() & 0xff);
        result = (result << 8) + (get() & 0xff);
        result = (result << 8) + (get() & 0xff);
        result = (result << 8) + (get() & 0xff);
        return result;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#getChar()
     */
    public char getChar() {
        // TODO: check current byteorder, assume BIG_ENDIAN
        int result = get() & 0xff;
        result = (result << 8) + (get() & 0xff);
        return (char) result;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#getShort()
     */
    public short getShort() {
        final int result = ((buf[pos] & 0xff) << 8) + (buf[pos + 1] & 0xff);

        pos += 2;

        return (short)result;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#put(int, byte)
     */
    public void put(final int index, final byte b) {
        buf[index + ofs] = b;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#put(byte)
     */
    public void put(final byte b) {
        buf[pos++] = b;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#putInt(int)
     */
    public void putInt(final int i) {
        put((byte) ((i >> 24) & 0xff));
        put((byte) ((i >> 16) & 0xff));
        put((byte) ((i >> 8) & 0xff));
        put((byte) (i & 0xff));
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#putShort(short)
     */
    public void putShort(final short s) {
        put((byte) ((s >> 8) & 0xff));
        put((byte) (s & 0xff));
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#mark()
     */
    public void mark() {
        mrk = pos;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#put(net.sf.andpdf.pdfviewer.ByteBuffer)
     */
    public void put(final ByteBuffer data) {
        int len = data.remaining();
        System.arraycopy(data.array(), data.position() + data.arrayOffset(), buf, pos, len);
        pos += len;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#reset()
     */
    public void reset() {
        // TODO: check for mark set
        pos = mrk;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#putInt(int, int)
     */
    public void putInt(final int index, final int value) {
        put(index, (byte) ((value >> 24) & 0xff));
        put(index + 1, (byte) ((value >> 16) & 0xff));
        put(index + 2, (byte) ((value >> 8) & 0xff));
        put(index + 3, (byte) (value & 0xff));
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#putLong(long)
     */
    public void putLong(final long value) {
        put((byte) ((value >> 56) & 0xff));
        put((byte) ((value >> 48) & 0xff));
        put((byte) ((value >> 40) & 0xff));
        put((byte) ((value >> 32) & 0xff));
        put((byte) ((value >> 24) & 0xff));
        put((byte) ((value >> 16) & 0xff));
        put((byte) ((value >> 8) & 0xff));
        put((byte) (value & 0xff));
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#putChar(char)
     */
    public void putChar(final char value) {
        put((byte) ((value >> 8) & 0xff));
        put((byte) (value & 0xff));
    }
    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#put(byte[])
     */

    public void put(final byte[] data) {
        int len = data.length;
        System.arraycopy(data, 0, buf, pos, len);
        pos += len;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#get(byte[], int, int)
     */
    public void get(final byte[] outBuf, final int outOffset, final int length) {
        System.arraycopy(buf, pos, outBuf, outOffset, length);
        pos += length;
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#toNIO()
     */
    public java.nio.ByteBuffer toNIO() {
        return java.nio.ByteBuffer.wrap(buf, pos, siz - pos);
    }

    /* (non-Javadoc)
     * @see net.sf.andpdf.pdfviewer.ByteBuffer#hasRemaining()
     */
    public boolean hasRemaining() {
        return pos < siz;
    }

    public static ByteBuffer fromNIO(final java.nio.ByteBuffer nioBuf) {
        return new ArrayBackedByteBuffer(nioBuf.array(), nioBuf.arrayOffset(), nioBuf.arrayOffset() + nioBuf.position(), nioBuf.arrayOffset() + nioBuf.position() + nioBuf.remaining());
    }
}
