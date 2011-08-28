/*
 * $Id: CMapFormat6.java,v 1.1 2009/02/16 00:26:24 tomoke Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.sun.pdfview.font.ttf;

import net.sf.andpdf.nio.ByteBuffer;

import java.util.*;

/**
 *
 * @author  jkaplan
 */
public class CMapFormat6 extends CMap {
    /** First character code of subrange. */
    private short firstCode;
    /** Number of character codes in subrange. */
    private short entryCount;
    /** Array of glyph index values for character codes in the range. */
    private short [] glyphIndexArray;
    /** a reverse lookup from glyph id to index. */
    private HashMap<Short,Short> glyphLookup = new HashMap<Short,Short>();

    /** Creates a new instance of CMapFormat0 */
    protected CMapFormat6(short language) {
        super((short) 6, language);
    }

    /**
     * Get the length of this table
     */
    public short getLength() {
        // start with the size of the fixed header
        short size = 5 * 2;

        // add the size of each segment header
        size += entryCount * 2;
        return size;
    }

    /**
     * Cannot map from a byte
     */
    public byte map(byte src) {
        char c = map((char) src);
        if (c < Byte.MIN_VALUE || c > Byte.MAX_VALUE) {
            // out of range
            return 0;
        }
        return (byte) c;
    }

    /**
     * Map from char
     */
    public char map(char src) {

        // find first segment with endcode > src
        if (src < firstCode || src > (firstCode + entryCount)) {
            // Codes outside of the range are assumed to be missing and are
            // mapped to the glyph with index 0
            return '\000';
        }
        return (char) glyphIndexArray[src - firstCode];
    }

    /**
     * Get the src code which maps to the given glyphID
     */
    public char reverseMap(short glyphID) {
        Short result = glyphLookup.get(new Short(glyphID));
        if (result == null) {
            return '\000';
        }
        return (char) result.shortValue();
    }


    /**
     * Get the data in this map as a ByteBuffer
     */
    public void setData(int length, ByteBuffer data) {
        // read the table size values
        firstCode = data.getShort();
        entryCount = data.getShort();

        glyphIndexArray = new short [entryCount];
        for (int i = 0; i < glyphIndexArray.length; i++) {
            glyphIndexArray[i] = data.getShort();
            glyphLookup.put(new Short(glyphIndexArray[i]),
                            new Short((short) (i + firstCode)));
        }
    }

    /**
     * Get the data in the map as a byte buffer
     */
    public ByteBuffer getData() {
        ByteBuffer buf = ByteBuffer.allocate(getLength());

        // write the header
        buf.putShort(getFormat());
        buf.putShort((short) getLength());
        buf.putShort(getLanguage());

        // write the various values
        buf.putShort(firstCode);
        buf.putShort(entryCount);

        // write the endCodes
        for (int i = 0; i < glyphIndexArray.length; i++) {
            buf.putShort(glyphIndexArray[i]);
        }
        // reset the data pointer
        buf.flip();

        return buf;
    }
}
