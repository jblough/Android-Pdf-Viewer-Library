/*
 * $Id: Type3Font.java,v 1.3 2009/02/12 13:53:54 tomoke Exp $
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
import java.util.HashMap;
import java.util.Map;

import net.sf.andpdf.utils.Utils;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFParser;


/**
 * A Type 3 Font, in which each glyph consists of a sequence of PDF
 * commands.
 * 
 * @author Mike Wessler
 */
public class Type3Font extends PDFFont {

    /** resources for the character definitions */
    HashMap<String,PDFObject> rsrc;
    /** the character processes, mapped by name */
    Map charProcs;
    /** bounding box for the font characters */
    RectF bbox;
    /** affine transform for the font characters */
    Matrix at;
    /** the widths */
    float[] widths;
    /** the start code */
    int firstChar;
    /** the end code */
    int lastChar;

    /**
     * Generate a Type 3 font.
     * @param baseFont the postscript name of this font
     * @param fontObj a dictionary containing references to the character
     * definitions and font information
     * @param resources a set of resources used by the character definitions
     * @param descriptor the descriptor for this font
     */
    public Type3Font(String baseFont, PDFObject fontObj,
            HashMap<String,PDFObject> resources, PDFFontDescriptor descriptor) throws IOException {
        super(baseFont, descriptor);

        rsrc = new HashMap<String,PDFObject>();

        if (resources != null) {
            rsrc.putAll(resources);
        }

        // get the transform matrix
        PDFObject matrix = fontObj.getDictRef("FontMatrix");
        float matrixAry[] = new float[6];
        for (int i = 0; i < 6; i++) {
            matrixAry[i] = matrix.getAt(i).getFloatValue();
        }
        at = Utils.createMatrix(matrixAry);

        // get the scale from the matrix
        float scale = matrixAry[0] + matrixAry[2];

        // put all the resources in a Hash
        PDFObject rsrcObj = fontObj.getDictRef("Resources");
        if (rsrcObj != null) {
            rsrc.putAll(rsrcObj.getDictionary());
        }

        // get the character processes, indexed by name
        charProcs = fontObj.getDictRef("CharProcs").getDictionary();

        // get the font bounding box
        PDFObject[] bboxdef = fontObj.getDictRef("FontBBox").getArray();
        float[] bboxfdef = new float[4];
        for (int i = 0; i < 4; i++) {
            bboxfdef[i] = bboxdef[i].getFloatValue();
        }
        bbox = new RectF(bboxfdef[0], bboxfdef[1],
                bboxfdef[2] - bboxfdef[0],
                bboxfdef[3] - bboxfdef[1]);
        if (bbox.isEmpty()) {
            bbox = null;
        }

        // get the widths
        PDFObject[] widthArray = fontObj.getDictRef("Widths").getArray();
        widths = new float[widthArray.length];
        for (int i = 0; i < widthArray.length; i++) {
            widths[i] = widthArray[i].getFloatValue();
        }

        // get first and last chars
        firstChar = fontObj.getDictRef("FirstChar").getIntValue();
        lastChar = fontObj.getDictRef("LastChar").getIntValue();
    }

    /**
     * Get the first character code
     */
    public int getFirstChar() {
        return firstChar;
    }

    /**
     * Get the last character code
     */
    public int getLastChar() {
        return lastChar;
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
        if (name == null) {
            throw new IllegalArgumentException("Glyph name required for Type3 font!" +
                    "Source character: " + (int) src);
        }

        PDFObject pageObj = (PDFObject) charProcs.get(name);
        if (pageObj == null) {
            // glyph not found.  Return an empty glyph...
            return new PDFGlyph(src, name, new Path(), new PointF(0, 0));
        }

        try {
            PDFPage page = new PDFPage(bbox, 0);
            page.addXform(at);

            PDFParser prc = new PDFParser(page, pageObj.getStream(), rsrc);
            prc.go(true);

            float width = widths[src - firstChar];

            PointF advance = new PointF(width, 0);
            float[]pts = {advance.x,advance.y};
            at.mapPoints(pts);
            advance.x = pts[0];
            advance.y = pts[1];

            return new PDFGlyph(src, name, page, advance);
        } catch (IOException ioe) {
            // help!
            System.out.println("IOException in Type3 font: " + ioe);
            ioe.printStackTrace();
            return null;
        }
    }
}

