/*
 * $Id: PDFPaint.java,v 1.4 2009/01/16 16:26:09 tomoke Exp $
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Paint.Style;

/**
 * PDFPaint is some kind of shader that knows how to fill a path.
 * At the moment, only a solid color is implemented, but gradients
 * and textures should be possible, too.
 * 
 * @author Mike Wessler
 * @author Ferenc Hechler (ferenc@hechler.de)
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public class PDFPaint {

    private Paint mainPaint;
    public static boolean s_doAntiAlias = false;

    /**
     * create a new PDFPaint based on a solid color
     */
    protected PDFPaint(int p) {
        this.mainPaint = new Paint();
        mainPaint.setColor(p);
        mainPaint.setAntiAlias(s_doAntiAlias);
    }

    /**
     * get the PDFPaint representing a solid color
     */
    public static PDFPaint getColorPaint(int c) {
        PDFPaint result = new PDFPaint(c);
//        result.getPaint().setStyle(Style.FILL);
        result.getPaint().setStyle(Style.STROKE);
        return result;
    }

    /**
     * get the PDFPaint representing a generic paint
     */
    public static PDFPaint getPaint(int p) {
        PDFPaint result = new PDFPaint(p);
//        result.getPaint().setStyle(Style.STROKE);
        result.getPaint().setStyle(Style.FILL);
        return result;
    }

    /**
     * fill a path with the paint, and record the dirty area.
     * @param state the current graphics state
     * @param g the graphics into which to draw
     * @param s the path to fill
     */
    public RectF fill(final PDFRenderer state, final Canvas g, final Path s) {
        g.drawPath(s, mainPaint);

        final RectF bounds = new RectF();
        final RectF result = new RectF();
        s.computeBounds(bounds, false);
        g.getMatrix().mapRect(result, bounds);
        return bounds;
    }

    /**
     * get the primary color associated with this PDFPaint.
     */
    public Paint getPaint() {
        return mainPaint;
    }
}
