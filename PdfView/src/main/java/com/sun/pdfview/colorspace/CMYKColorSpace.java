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


public class CMYKColorSpace extends PDFColorSpace {
	
    public CMYKColorSpace() {
    }

    /**
     * get the number of components (4)
     */
    @Override public int getNumComponents() {
    	return 4;
    }

    @Override public int toColor(float[] fcomp) {
    	float k = fcomp[3];
    	float w = 255*(1-k);
    	float r = w*(1-fcomp[0]);
    	float g = w*(1-fcomp[1]);
    	float b = w*(1-fcomp[2]);
    	return Color.rgb((int)r,(int)g,(int)b);
    }

    @Override public int toColor(int[] icomp) {
    	int k = icomp[3];
    	int w = 255-k;
    	int r = w*(255-icomp[0])/255;
    	int g = w*(255-icomp[1])/255;
    	int b = w*(255-icomp[2])/255;
    	return Color.rgb(r,g,b);
    }

    
    /**
     * get the type of this color space (TYPE_CMYK)
     */
    @Override public int getType() {
    	return COLORSPACE_CMYK;
    }

	@Override
	public String getName() {
		return "CMYK";
	}


}
