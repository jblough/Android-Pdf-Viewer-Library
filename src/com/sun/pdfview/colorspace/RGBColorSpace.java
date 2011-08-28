/*
 * $Id: CalRGBColor.java,v 1.2 2007/12/20 18:33:34 rbair Exp $
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

import android.graphics.Color;


public class RGBColorSpace extends PDFColorSpace {
	
    public RGBColorSpace() {
    }

    /**
     * get the number of components (3)
     */
    @Override public int getNumComponents() {
    	return 3;
    }

    @Override public int toColor(float[] fcomp) {
    	return Color.rgb((int)(fcomp[0]*255),(int)(fcomp[1]*255),(int)(fcomp[2]*255));
    }

    @Override public int toColor(int[] icomp) {
    	return Color.rgb(icomp[0],icomp[1],icomp[2]);
    }

    
    /**
     * get the type of this color space (TYPE_RGB)
     */
    @Override public int getType() {
    	return COLORSPACE_RGB;
    }

	@Override
	public String getName() {
		return "RGB";
	}


}
