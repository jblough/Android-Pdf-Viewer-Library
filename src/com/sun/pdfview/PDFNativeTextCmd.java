/*
 * $Id: PDFShapeCmd.java,v 1.3 2009/01/16 16:26:15 tomoke Exp $
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
package com.sun.pdfview;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Paint.FontMetrics;
import android.util.Log;


/**
 * Encapsulates a path.  Also contains extra fields and logic to check
 * for consecutive abutting anti-aliased regions.  We stroke the shared
 * line between these regions again with a 1-pixel wide line so that
 * the background doesn't show through between them.
 *
 * @author Mike Wessler
 */
public class PDFNativeTextCmd extends PDFCmd {

	private static final String TAG = "ANDPDF.natTXT";
	
    /** stroke the outline of the path with the stroke paint */
    public static final int STROKE = 1;
    /** fill the path with the fill paint */
    public static final int FILL = 2;
    /** perform both stroke and fill */
    public static final int BOTH = 3;
    /** set the clip region to the path */
    public static final int CLIP = 4;

    /** the style */
    private int style;
    
    /** the bounding box of the path */
    private RectF bounds;
    
    private Matrix mat;
    private float x;
    private float y;
    private float w;
    private float h;
    private String text;

    /**
     * create a new PDFNativeTextCmd 
     */
    public PDFNativeTextCmd(String text, Matrix mat) {
    	
//    	Log.i(TAG, "NATIVETEXTCMD['"+text+"',"+mat+"]");
        this.text = text;
        this.mat = mat;
        float[] values = new float[9];
        mat.getValues(values);
        this.x = values[2];
        this.y = values[5];
        this.w = values[0];
        this.h = values[4];
        bounds = new RectF(x, y, x+w, y+h);
    }

    /**
     * perform the stroke and record the dirty region
     */
    public RectF execute(PDFRenderer state) {
        RectF rect = state.drawNativeText(text, bounds);
        return rect;
    }

//    /**
//     * Check for overlap with the previous shape to make anti-aliased shapes
//     * that are near each other look good
//     */
//    private Path checkOverlap(PDFRenderer state) {
//        if (style == FILL && gp != null && state.getLastShape() != null) {
//            float mypoints[] = new float[16];
//            float prevpoints[] = new float[16];
//
//            int mycount = getPoints(gp, mypoints);
//            int prevcount = getPoints(state.getLastShape(), prevpoints);
//
//            // now check mypoints against prevpoints for opposite pairs:
//            if (mypoints != null && prevpoints != null) {
//                for (int i = 0; i < prevcount; i += 4) {
//                    for (int j = 0; j < mycount; j += 4) {
//                        if ((Math.abs(mypoints[j + 2] - prevpoints[i]) < 0.01 &&
//                                Math.abs(mypoints[j + 3] - prevpoints[i + 1]) < 0.01 &&
//                                Math.abs(mypoints[j] - prevpoints[i + 2]) < 0.01 &&
//                                Math.abs(mypoints[j + 1] - prevpoints[i + 3]) < 0.01)) {
//                            GeneralPath strokeagain = new GeneralPath();
//                            strokeagain.moveTo(mypoints[j], mypoints[j + 1]);
//                            strokeagain.lineTo(mypoints[j + 2], mypoints[j + 3]);
//                            return strokeagain;
//                        }
//                    }
//                }
//            }
//        }
//
//        // no issues
//        return null;
//    }
//
//    /**
//     * Get an array of 16 points from a path
//     * @return the number of points we actually got
//     */
//    private int getPoints(GeneralPath path, float[] mypoints) {
//        int count = 0;
//        float x = 0;
//        float y = 0;
//        float startx = 0;
//        float starty = 0;
//        float[] coords = new float[6];
//
//        PathIterator pi = path.getPathIterator(new AffineTransform());
//        while (!pi.isDone()) {
//            if (count >= mypoints.length) {
//                mypoints = null;
//                break;
//            }
//
//            int pathtype = pi.currentSegment(coords);
//            switch (pathtype) {
//                case PathIterator.SEG_MOVETO:
//                    startx = x = coords[0];
//                    starty = y = coords[1];
//                    break;
//                case PathIterator.SEG_LINETO:
//                    mypoints[count++] = x;
//                    mypoints[count++] = y;
//                    x = mypoints[count++] = coords[0];
//                    y = mypoints[count++] = coords[1];
//                    break;
//                case PathIterator.SEG_QUADTO:
//                    x = coords[2];
//                    y = coords[3];
//                    break;
//                case PathIterator.SEG_CUBICTO:
//                    x = mypoints[4];
//                    y = mypoints[5];
//                    break;
//                case PathIterator.SEG_CLOSE:
//                    mypoints[count++] = x;
//                    mypoints[count++] = y;
//                    x = mypoints[count++] = startx;
//                    y = mypoints[count++] = starty;
//                    break;
//            }
//
//            pi.next();
//        }
//
//        return count;
//    }

    /** Get detailed information about this shape
     */
    @Override
    public String getDetails() {
        StringBuffer sb = new StringBuffer();
        sb.append("NativeTextCommand Text: '" + text + "'\n");
        sb.append("matrix: " + mat + "\n");
        sb.append("Mode: ");
        if ((style & FILL) != 0) {
            sb.append("FILL ");
        }
        if ((style & STROKE) != 0) {
            sb.append("STROKE ");
        }
        if ((style & CLIP) != 0) {
            sb.append("CLIP");
        }
        return sb.toString();
    }

	public float getWidth() {
		return 6.0f*text.length();
//		return 0.5f*text.length()*w;
	}
}
