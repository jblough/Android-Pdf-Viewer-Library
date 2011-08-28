/*
 * $Id: OutlineFont.java,v 1.3 2009/02/09 16:29:58 tomoke Exp $
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
package com.sun.pdfview.font;

import java.io.IOException;

import android.graphics.Path;
import android.graphics.PointF;

import com.sun.pdfview.PDFObject;

/**
 * Supports width operations for Type1, Type1C, TrueType and Type3 fonts
 */
public abstract class OutlineFont extends PDFFont {

    /** the first character code */
    private int firstChar = -1;
    /** the last character code */
    private int lastChar = -1;
    /** the widths for each character code */
    private float[] widths;

    /** Creates a new instance of OutlineFont */
    public OutlineFont(String baseFont, PDFObject fontObj,
            PDFFontDescriptor descriptor) throws IOException {
        super(baseFont, descriptor);

        PDFObject firstCharObj = fontObj.getDictRef("FirstChar");
        PDFObject lastCharObj = fontObj.getDictRef("LastChar");
        PDFObject widthArrayObj = fontObj.getDictRef("Widths");

        if (firstCharObj != null) {
            firstChar = firstCharObj.getIntValue();
        }
        if (lastCharObj != null) {
            lastChar = lastCharObj.getIntValue();
        }

        if (widthArrayObj != null) {
            PDFObject[] widthArray = widthArrayObj.getArray();

            widths = new float[widthArray.length];

            for (int i = 0; i < widthArray.length; i++) {
                widths[i] = widthArray[i].getFloatValue() / getDefaultWidth();
            }
        }
    }

    /** Get the first character code */
    public int getFirstChar() {
        return firstChar;
    }

    /** Get the last character code */
    public int getLastChar() {
        return lastChar;
    }

    /** Get the default width in text space */
    public int getDefaultWidth() {
        return 1000;
    }

    /** Get the number of characters */
    public int getCharCount() {
        return (getLastChar() - getFirstChar()) + 1;
    }

    /** Get the width of a given character */
    public float getWidth(char code, String name) {
        int idx = (code & 0xff) - getFirstChar();

        // make sure we're in range
        if (idx < 0 || widths == null || idx >= widths.length) {
            // try to get the missing width from the font descriptor
            if (getDescriptor() != null) {
                return getDescriptor().getMissingWidth();
            } else {
                return 0;
            }
        }

        return widths[idx];
    }

    /**
     * Get the glyph for a given character code and name
     *
     * The preferred method of getting the glyph should be by name.  If the
     * name is null or not valid, then the character code should be used.
     * If the both the code and the name are invalid, the undefined glyph 
     * should be returned.
     *
     * Note this method must *always* return a glyph.  
     *
     * @param src the character code of this glyph
     * @param name the name of this glyph or null if unknown
     * @return a glyph for this character
     */
    protected PDFGlyph getGlyph(char src, String name) {
        Path outline = null;
        float width = getWidth(src, name);

        // first try by name
        if (name != null) {
            outline = getOutline(name, width);
        }

        // now try by character code (guaranteed to return)
        if (outline == null) {
            outline = getOutline(src, width);
        }

        // calculate the advance
        PointF advance = new PointF(width, 0);
        return new PDFGlyph(src, name, outline, advance);
    }

    /**
     * Get a glyph outline by name
     *
     * @param name the name of the desired glyph
     * @return the glyph outline, or null if unavailable
     */
    protected abstract Path getOutline(String name, float width);

    /**
     * Get a glyph outline by character code
     *
     * Note this method must always return an outline 
     *
     * @param src the character code of the desired glyph
     * @return the glyph outline
     */
    protected abstract Path getOutline(char src, float width);
}
