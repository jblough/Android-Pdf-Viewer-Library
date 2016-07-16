/*
 * $Id: IndexedColor.java,v 1.4 2009/01/26 05:40:42 tomoke Exp $
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
package com.sun.pdfview.colorspace;

import java.io.IOException;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPaint;

/**
 * A PDFColorSpace for an IndexedColor model
 *
 * @author Mike Wessler
 */
public class IndexedColor extends PDFColorSpace {

    /** the color table */
    int table[];
    /** size of the color table */
    int count;

    /**
     * create a new IndexColor PDFColorSpace based on another PDFColorSpace,
     * a count of colors, and a stream of values.  Every consecutive n bytes
     * of the stream is interpreted as a color in the base ColorSpace, where
     * n is the number of components in that color space.
     *
     * @param base the color space in which the data is interpreted
     * @param count the number of colors in the table
     * @param stream a stream of bytes.  The number of bytes must be count*n,
     * where n is the number of components in the base colorspace.
     */
    public IndexedColor(PDFColorSpace base, int count, PDFObject stream) throws IOException {
        count++;
        this.count = count;
        byte[] data = stream.getStream();
        int nchannels = base.getNumComponents();
        boolean offSized = (data.length / nchannels) < count;
        table = new int[count];
        float comps[] = new float[nchannels];
        int loc = 0;
        int finalloc = 0;
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < comps.length; j++) {
                if (loc < data.length) {
                    comps[j] = (((int) data[loc++]) & 0xff) / 255f;
                } else {
                    comps[j] = 1.0f;
                }
            }
            table[i] = base.toColor(comps);
        }
    }

    /**
     * create a new IndexColor PDFColorSpace based on a table of colors.  
     * 
     * @param table an array of colors
     */
    public IndexedColor(int[] table) throws IOException {
        this.count = table.length;
        this.table = table;
    }

    /**
     * Get the number of indices
     */
    public int getCount() {
        return count;
    }

    /**
     * Get the table of color components
     */
    public int[] getColorTable() {
        return table;
    }

    /**
     * get the number of components of this colorspace (1)
     */
    @Override
    public int getNumComponents() {
        return 1;
    }

	@Override
	public String getName() {
		return "I";
	}

	@Override
	public int getType() {
		return COLORSPACE_INDEXED;
	}

	@Override
	public int toColor(float[] comp) {
		return table[(int)(255*comp[0])];
	}
	
	@Override
	public int toColor(int[] comp) {
		return table[comp[0]];
	}
	
}
