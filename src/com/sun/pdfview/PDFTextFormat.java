/*
 * $Id: PDFTextFormat.java,v 1.3 2009/01/16 16:26:11 tomoke Exp $
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

import java.util.List;

import net.sf.andpdf.utils.Utils;

import android.graphics.Matrix;
import android.graphics.PointF;

import com.sun.pdfview.font.OutlineFont;
import com.sun.pdfview.font.PDFFont;
import com.sun.pdfview.font.PDFGlyph;

/**
 * a class encapsulating the text state
 *
 * @author Mike Wessler
 * @author Ferenc Hechler (ferenc@hechler.de)
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public class PDFTextFormat implements Cloneable {

    /** character spacing */
    private float tc = 0;
    /** word spacing */
    private float tw = 0;
    /** horizontal scaling */
    private float th = 1;
    /** leading */
    private float tl = 0;
    /** rise amount */
    private float tr = 0;
    /** text mode */
    private int tm = PDFShapeCmd.FILL;
    /** text knockout */
    private float tk = 0;
    /** current matrix transform */
    private final Matrix cur;
    /** matrix transform at start of line */
    private Matrix line;
    /** font */
    private PDFFont font;
    /** font size */
    private float fsize = 1;
    /** are we between BT and ET? */
    private boolean inuse = false;
    //    private Object array[]= new Object[1];
    /** build text rep of word */
    private StringBuffer word = new StringBuffer();
    // this is where we build and keep the word list for this page.
    /** start location of the hunk of text */
    private PointF wordStart;
    /** location of the end of the previous hunk of text */
    private final PointF prevEnd;

    /**
     * create a new PDFTextFormat, with initial values
     */
    public PDFTextFormat() {
        cur = new Matrix();
        line = new Matrix();
        wordStart = new PointF(-100, -100);
        prevEnd = new PointF(-100, -100);

        tc = tw = tr = tk = 0;
        tm = PDFShapeCmd.FILL;
        th = 1;
    }

    /**
     * reset the PDFTextFormat for a new run
     */
    public void reset() {
        cur.reset();
        line.reset();
        inuse = true;
        word.setLength(0);
    }

    /**
     * end a span of text
     */
    public void end() {
        inuse = false;
    }

    /** get the char spacing */
    public float getCharSpacing() {
        return tc;
    }

    /** set the character spacing */
    public void setCharSpacing(float spc) {
        this.tc = spc;
    }

    /** get the word spacing */
    public float getWordSpacing() {
        return tw;
    }

    /** set the word spacing */
    public void setWordSpacing(float spc) {
        this.tw = spc;
    }

    /**
     * Get the horizontal scale
     * @return the horizontal scale, in percent
     */
    public float getHorizontalScale() {
        return th * 100;
    }

    /**
     * set the horizontal scale.
     * @param scl the horizontal scale, in percent (100=normal)
     */
    public void setHorizontalScale(float scl) {
        this.th = scl / 100;
    }

    /** get the leading */
    public float getLeading() {
        return tl;
    }

    /** set the leading */
    public void setLeading(float spc) {
        this.tl = spc;
    }

    /** get the font */
    public PDFFont getFont() {
        return font;
    }

    /** get the font size */
    public float getFontSize() {
        return fsize;
    }

    /** set the font and size */
    public void setFont(PDFFont f, float size) {
        this.font = f;
        this.fsize = size;
    }

    /**
     * Get the mode of the text
     */
    public int getMode() {
        return tm;
    }

    /**
     * set the mode of the text.  The correspondence of m to mode is
     * show in the following table.  m is a value from 0-7 in binary:
     * 
     * 000 Fill
     * 001 Stroke
     * 010 Fill + Stroke
     * 011 Nothing
     * 100 Fill + Clip
     * 101 Stroke + Clip
     * 110 Fill + Stroke + Clip
     * 111 Clip
     *
     * Therefore: Fill corresponds to the low bit being 0; Clip
     * corresponds to the hight bit being 1; and Stroke corresponds
     * to the middle xor low bit being 1.
     */
    public void setMode(int m) {
        int mode = 0;

        if ((m & 0x1) == 0) {
            mode |= PDFShapeCmd.FILL;
        }
        if ((m & 0x4) != 0) {
            mode |= PDFShapeCmd.CLIP;
        }
        if (((m & 0x1) ^ ((m & 0x2) >> 1)) != 0) {
            mode |= PDFShapeCmd.STROKE;
        }

        this.tm = mode;
    }

    /**
     * Set the mode from another text format mode
     *
     * @param mode the text render mode using the
     * codes from PDFShapeCmd and not the wacky PDF codes
     */
    public void setTextFormatMode(int mode) {
        this.tm = mode;
    }

    /**
     * Get the rise
     */
    public float getRise() {
        return tr;
    }

    /**
     * set the rise
     */
    public void setRise(float spc) {
        this.tr = spc;
    }

    /**
     * perform a carriage return
     */
    public void carriageReturn() {
        carriageReturn(0, -tl);
    }

    /**
     * perform a carriage return by translating by x and y.  The next
     * carriage return will be relative to the new location.
     */
    public void carriageReturn(float x, float y) {
        Matrix trans = new Matrix();
        trans.setTranslate(x, y);
        line.preConcat(trans);
        cur.set(line);
    }

    /**
     * Get the current transform
     */
    public Matrix getTransform() {
        return cur;
    }

    /**
     * set the transform matrix directly
     */
    public void setMatrix(float[] matrix) {
        line = new Matrix();
        Utils.setMatValues(line, matrix);
        cur.set(line);
    }

    public void doText(final PDFPage cmds, final String text) {
    	if (PDFFont.sUseFontSubstitution)
    		doTextFontSubst(cmds, text);
    	else 
    		doTextNormal(cmds, text);
    }

    /**
     * add some text to the page.
     * @param cmds the PDFPage to add the commands to
     * @param text the text to add
     */
    private void doTextFontSubst(final PDFPage cmds, final String text) {
        final PointF zero = new PointF();
        final Matrix scale = new Matrix();
        Utils.setMatValues(scale, fsize, 0, 0, fsize * th, 0, tr);
        final Matrix at = new Matrix();
        
        at.set(cur);
        at.preConcat(scale);
        
        PDFNativeTextCmd ntx = new PDFNativeTextCmd(text, at);
        cmds.addCommand(ntx);

        // calc widths
    	for (int i=0; i<text.length(); i++) {
    		char c = text.charAt(0);
            float width = 0.6f;
	        if (font instanceof OutlineFont)
	        	width = ((OutlineFont)font).getWidth(c, null);
            float advanceX = (width * fsize) + tc;
            if (c == ' ') {
                advanceX += tw;
            }
            advanceX *= th;
            cur.preTranslate(advanceX, 0);
        }
	            
        final float[] src = {zero.x, zero.y};
        final float[] dst = new float[src.length];
        cur.mapPoints(dst, src);
        prevEnd.set(dst[0], dst[1]);
            
    }

    
    /**
     * add some text to the page.
     * @param cmds the PDFPage to add the commands to
     * @param text the text to add
     */
    private void doTextNormal(final PDFPage cmds, final String text) {
        final PointF zero = new PointF();
        final Matrix scale = new Matrix();
        Utils.setMatValues(scale, fsize, 0, 0, fsize * th, 0, tr);
        final Matrix at = new Matrix();

        final List<PDFGlyph> l = (List<PDFGlyph>) font.getGlyphs(text);

        for (final PDFGlyph glyph : l) {
            at.set(cur);
            at.preConcat(scale);
            final PointF advance = glyph.addCommands(cmds, at, tm);

            float advanceX = (advance.x * fsize) + tc;
            if (glyph.getChar() == ' ') {
                advanceX += tw;
            }
            advanceX *= th;

            cur.preTranslate(advanceX, advance.y);
        }

        final float[] src = {zero.x, zero.y};
        final float[] dst = new float[src.length];
        cur.mapPoints(dst, src);
        prevEnd.set(dst[0], dst[1]);
    }

    /**
     * add some text to the page.
     * @param cmds the PDFPage to add the commands to
     * @param ary an array of Strings and Doubles, where the Strings
     * represent text to be added, and the Doubles represent kerning
     * amounts.
     */
    public void doText(final PDFPage cmds, final Object ary[]) throws PDFParseException {
        for (int i = 0, to = ary.length; i < to; ++i) {
            if (ary[i] instanceof String) {
                doText(cmds, (String) ary[i]);
            } else if (ary[i] instanceof Double) {
                float val = ((Double) ary[i]).floatValue() / 1000f;
                cur.preTranslate(-val * fsize * th, 0);
            } else {
                throw new PDFParseException("Bad element in TJ array");
            }
        }
    }

    /**
     * finish any unfinished words.  TODO: write this!
     */
    public void flush() {
        // TODO: finish any unfinished words
    }

    /** 
     * Clone the text format
     */
    @Override
    public Object clone() {
        PDFTextFormat newFormat = new PDFTextFormat();

        // copy values
        newFormat.setCharSpacing(getCharSpacing());
        newFormat.setWordSpacing(getWordSpacing());
        newFormat.setHorizontalScale(getHorizontalScale());
        newFormat.setLeading(getLeading());
        newFormat.setTextFormatMode(getMode());
        newFormat.setRise(getRise());

        // copy immutable fields
        newFormat.setFont(getFont(), getFontSize());

        // clone transform (mutable)
        // newFormat.getTransform().setTransform(getTransform());

        return newFormat;
    }
}
