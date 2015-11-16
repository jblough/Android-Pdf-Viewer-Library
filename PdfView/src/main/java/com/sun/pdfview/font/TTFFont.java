/*
 * $Id: TTFFont.java,v 1.10 2009/02/23 15:29:19 tomoke Exp $
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

import net.sf.andpdf.utils.Utils;

import android.graphics.Matrix;
import android.graphics.Path;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.font.ttf.AdobeGlyphList;
import com.sun.pdfview.font.ttf.CMap;
import com.sun.pdfview.font.ttf.CmapTable;
import com.sun.pdfview.font.ttf.Glyf;
import com.sun.pdfview.font.ttf.GlyfCompound;
import com.sun.pdfview.font.ttf.GlyfSimple;
import com.sun.pdfview.font.ttf.GlyfTable;
import com.sun.pdfview.font.ttf.HeadTable;
import com.sun.pdfview.font.ttf.HmtxTable;
import com.sun.pdfview.font.ttf.PostTable;
import com.sun.pdfview.font.ttf.TrueTypeFont;


/**
 * A true-type font
 */
public class TTFFont extends OutlineFont {

    /** the truetype font itself */
    private TrueTypeFont font;
    /** the number of units per em in the font */
    private float unitsPerEm;

    /**
     * create a new TrueTypeFont object based on a description of the
     * font from the PDF file.  If the description happens to contain
     * an in-line true-type font file (under key "FontFile2"), use the
     * true type font.  Otherwise, parse the description for key information
     * and use that to generate an appropriate font.
     */
    public TTFFont(String baseFont, PDFObject fontObj,
            PDFFontDescriptor descriptor)
            throws IOException {
        super(baseFont, fontObj, descriptor);

        String fontName = descriptor.getFontName();
        PDFObject ttfObj = descriptor.getFontFile2();

        // try {
        //    byte[] fontData = ttfObj.getStream();
        //    java.io.FileOutputStream fis = new java.io.FileOutputStream("/tmp/" + fontName + ".ttf");
        //    fis.write(fontData);
        //    fis.flush();
        //    fis.close();
        // } catch (Exception ex) {
        //    ex.printStackTrace();
        // }
        if (ttfObj != null) {
            font = TrueTypeFont.parseFont(ttfObj.getStreamBuffer());
            // read the units per em from the head table
            HeadTable head = (HeadTable) font.getTable("head");
            unitsPerEm = head.getUnitsPerEm();
        } else {
            font = null;
        }
//        System.out.println ("TTFFont: ttfObj: " + ttfObj + ", fontName: " + fontName);

    }

    /**
     * Get the outline of a character given the character code
     */
    protected synchronized Path getOutline(char src, float width) {
        // find the cmaps
        CmapTable cmap = (CmapTable) font.getTable("cmap");

        // if there are no cmaps, this is (hopefully) a cid-mapped font,
        // so just trust the value we were given for src
        if (cmap == null) {
            return getOutline((int) src, width);
        }

        CMap[] maps = cmap.getCMaps();

        // try the maps in order
        for (int i = 0; i < maps.length; i++) {
            int idx = maps[i].map(src);
            if (idx != 0) {
                return getOutline(idx, width);
            }
        }

        // not found, return the empty glyph
        return getOutline(0, width);
    }

    /**
     * lookup the outline using the CMAPs, as specified in 32000-1:2008,
     * 9.6.6.4, when an Encoding is specified.
     * 
     * @param val
     * @param width
     * @return GeneralPath
     */
    protected synchronized Path getOutlineFromCMaps(char val, float width) {
        // find the cmaps
        CmapTable cmap = (CmapTable) font.getTable("cmap");

        if (cmap == null) {
            return null;
        }

        // try maps in required order of (3, 1), (1, 0)
        CMap map = cmap.getCMap((short) 3, (short) 1);
        if (map == null) {
            map = cmap.getCMap((short) 1, (short) 0);
        }
        int idx = map.map(val);
        if (idx != 0) {
            return getOutline(idx, width);
        }

        return null;
    }

    /**
     * Get the outline of a character given the character name
     */
    protected synchronized Path getOutline(String name, float width) {
        int idx;
        PostTable post = (PostTable) font.getTable("post");
        if (post != null) {
        	idx = post.getGlyphNameIndex(name);
        	if (idx != 0) {
        		return getOutline(idx, width);
        	}
        	return null;
        }
        
        Integer res = AdobeGlyphList.getGlyphNameIndex(name);
        if(res != null) {
        	idx = res;
        	return getOutlineFromCMaps((char)idx, width);
        }        		        
        return null;
    }

    /**
     * Get the outline of a character given the glyph id
     */
    protected synchronized Path getOutline(int glyphId, float width) {
        // find the glyph itself
        GlyfTable glyf = (GlyfTable) font.getTable("glyf");
        Glyf g = glyf.getGlyph(glyphId);

        Path gp = null;
        if (g instanceof GlyfSimple) {
            gp = renderSimpleGlyph((GlyfSimple) g);
        } else if (g instanceof GlyfCompound) {
            gp = renderCompoundGlyph(glyf, (GlyfCompound) g);
        } else {
            gp = new Path();
        }

        // calculate the advance
        HmtxTable hmtx = (HmtxTable) font.getTable("hmtx");
        float advance = (float) hmtx.getAdvance(glyphId) / (float) unitsPerEm;

        // scale the glyph to match the desired advance
        float widthfactor = width / advance;

        // the base transform scales the glyph to 1x1
        Matrix at = new Matrix();
        at.setScale(1 / unitsPerEm, 1 / unitsPerEm);
        Matrix tmp = new Matrix();
        tmp.setScale(widthfactor, 1);
        at.preConcat(tmp);

        gp.transform(at);

        return gp;
    }

    /**
     * Render a simple glyf
     */
    protected Path renderSimpleGlyph(GlyfSimple g) {
        // the current contour
        int curContour = 0;

        // the render state
        RenderState rs = new RenderState();
        rs.gp = new Path();

        for (int i = 0; i < g.getNumPoints(); i++) {
            PointRec rec = new PointRec(g, i);

            if (rec.onCurve) {
                addOnCurvePoint(rec, rs);
            } else {
                addOffCurvePoint(rec, rs);
            }

            // see if we just ended a contour
            if (i == g.getContourEndPoint(curContour)) {
                curContour++;

                if (rs.firstOff != null) {
                    addOffCurvePoint(rs.firstOff, rs);
                }

                if (rs.firstOn != null) {
                    addOnCurvePoint(rs.firstOn, rs);
                }

                rs.firstOn = null;
                rs.firstOff = null;
                rs.prevOff = null;
            }
        }

        return rs.gp;
    }
    
// --- OLD
//
    
//    protected GeneralPath renderCompoundGlyph(GlyfTable glyf, GlyfCompound g) {
//        GeneralPath gp = new GeneralPath();
//
//        for (int i = 0; i < g.getNumComponents(); i++) {
//            // find and render the component glyf
//            GlyfSimple gs = (GlyfSimple) glyf.getGlyph(g.getGlyphIndex(i));
//            GeneralPath path = renderSimpleGlyph(gs);
//
//            // multiply the translations by units per em
//            double[] matrix = g.getTransform(i);
//
//            // transform the path
//            path.transform(new AffineTransform(matrix));
//
//            // add it to the global path
//            gp.append(path, false);
//        }
//
//        return gp;
//    }
//
// --- NEW
//  
//    protected GeneralPath renderCompoundGlyph (GlyfTable glyf, GlyfCompound g) {
//        GeneralPath gp = new GeneralPath ();
//
//        for (int i = 0; i < g.getNumComponents (); i++) {
//            // find and render the component glyf
//            Glyf gl = glyf.getGlyph (g.getGlyphIndex (i));
//            GeneralPath path = null;
//            if (gl instanceof GlyfSimple) {
//                path = renderSimpleGlyph ((GlyfSimple) gl);
//            } else if (gl instanceof GlyfCompound) {
//                path = renderCompoundGlyph (glyf, (GlyfCompound) gl);
//            } else {
//                throw new RuntimeException (
//                        "Unsupported glyph type " + gl.getClass ().getCanonicalName ());
//            }
//
//            // multiply the translations by units per em
//            double[] matrix = g.getTransform (i);
//
//            // transform the path
//            path.transform (new AffineTransform (matrix));
//
//            // add it to the global path
//            gp.append (path, false);
//        }
    
    /**
     * Render a compound glyf
     */
    protected Path renderCompoundGlyph(GlyfTable glyf, GlyfCompound g) {
        Path gp = new Path();

        for (int i = 0; i < g.getNumComponents(); i++) {
            // find and render the component glyf
            Glyf gl = glyf.getGlyph (g.getGlyphIndex (i));
            Path path = null;
            if (gl instanceof GlyfSimple) {
                path = renderSimpleGlyph ((GlyfSimple) gl);
            } else if (gl instanceof GlyfCompound) {
                path = renderCompoundGlyph (glyf, (GlyfCompound) gl);
            } else {
                throw new RuntimeException (
                        "Unsupported glyph type " + gl.getClass ().getCanonicalName ());
            }

            // multiply the translations by units per em
            float[] matrix = g.getTransform(i);

            // transform and add path to the global path
            Matrix mat = new Matrix();
            Utils.setMatValues(mat, matrix);
            gp.addPath(path, mat);
        }

        return gp;
    }

    /** add a point on the curve */
    private void addOnCurvePoint(PointRec rec, RenderState rs) {
        // if the point is on the curve, either move to it,
        // or draw a line from the previous point
        if (rs.firstOn == null) {
            rs.firstOn = rec;
            rs.gp.moveTo(rec.x, rec.y);
        } else if (rs.prevOff != null) {
            rs.gp.quadTo(rs.prevOff.x, rs.prevOff.y, rec.x, rec.y);
            rs.prevOff = null;
        } else {
            rs.gp.lineTo(rec.x, rec.y);
        }
    }

    /** add a point off the curve */
    private void addOffCurvePoint(PointRec rec, RenderState rs) {
        if (rs.prevOff != null) {
            PointRec oc = new PointRec((rec.x + rs.prevOff.x) / 2,
                    (rec.y + rs.prevOff.y) / 2,
                    true);
            addOnCurvePoint(oc, rs);
        } else if (rs.firstOn == null) {
            rs.firstOff = rec;
        }
        rs.prevOff = rec;
    }

    class RenderState {
        // the shape itself
        Path gp;
        // the first off and on-curve points in the current segment
        PointRec firstOn;
        PointRec firstOff;
        // the previous off and on-curve points in the current segment
        PointRec prevOff;
    }

    /** a point on the stack of points */
    class PointRec {

        int x;
        int y;
        boolean onCurve;

        public PointRec(int x, int y, boolean onCurve) {
            this.x = x;
            this.y = y;
            this.onCurve = onCurve;
        }

        public PointRec(GlyfSimple g, int idx) {
            x = g.getXCoord(idx);
            y = g.getYCoord(idx);
            onCurve = g.onCurve(idx);
        }
    }
}
