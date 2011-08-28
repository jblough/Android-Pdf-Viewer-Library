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

import com.sun.pdfview.function.PDFFunction;

import android.graphics.Color;


public class AlternateColorSpace extends PDFColorSpace {

	/** The alternate color space */
    private PDFColorSpace alternate;
    
    /** The function */
    private PDFFunction function;
	
    /** Creates a new instance of AlternateColorSpace */
    public AlternateColorSpace(PDFColorSpace alternate, PDFFunction function) {
        this.alternate = alternate;
        this.function = function;
    }

    /**
     * get the number of components expected in the getPaint command
     */
    @Override public int getNumComponents() {
    	if (function != null) {
            return function.getNumInputs();
        } else {
            return alternate.getNumComponents();
        }
    }

    @Override public int toColor(float[] fcomp) {
        if (function != null) {
            // translate values using function
            fcomp = function.calculate(fcomp);
        }
    	float k = fcomp[3];
    	float w = 255*(1-k);
    	float r = w*(1-fcomp[0]);
    	float g = w*(1-fcomp[1]);
    	float b = w*(1-fcomp[2]);
    	return Color.rgb((int)r,(int)g,(int)b);
    }

    @Override public int toColor(int[] icomp) {
    	float[] fcomp = new float[icomp.length];
    	for (int i = 0; i < fcomp.length; i++)
			fcomp[i] = icomp[i]/255;
    	return toColor(fcomp);
    }

    
    /**
     * get the type of this color space (TYPE_CMYK)
     */
    @Override public int getType() {
    	return COLORSPACE_ALTERNATE;
    }

	@Override
	public String getName() {
		return "ALTERNATE";
	}


}
