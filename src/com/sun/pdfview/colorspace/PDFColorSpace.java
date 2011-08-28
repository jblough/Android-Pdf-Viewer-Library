/*
 * $Id: PDFColorSpace.java,v 1.5 2009/03/08 20:46:16 tomoke Exp $
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
import java.util.Map;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.function.PDFFunction;


/**
 * A color space that can convert a set of color components into
 * PDFPaint.
 * @author Mike Wessler
 */
public abstract class PDFColorSpace {
	
    /** the name of the device-dependent gray color space */
    public static final int COLORSPACE_GRAY = 0;

    /** the name of the device-dependent RGB color space */
    public static final int COLORSPACE_RGB = 1;

    /** the name of the device-dependent CMYK color space */
    public static final int COLORSPACE_CMYK = 2;

    /** the name of the pattern color space */
    public static final int COLORSPACE_PATTERN = 3;

    public static final int COLORSPACE_INDEXED = 4;

    public static final int COLORSPACE_ALTERNATE = 5;

    /** the device-dependent color spaces */
    //    private static PDFColorSpace graySpace =
    //            new PDFColorSpace(ColorSpace.getInstance(ColorSpace.CS_GRAY));
    private static PDFColorSpace rgbSpace = new RGBColorSpace();
    private static PDFColorSpace cmykSpace = new CMYKColorSpace(); 

    /** the pattern space */
    private static PDFColorSpace patternSpace = new RGBColorSpace(); // TODO [FHe]

    /** graySpace and the gamma correction for it. */
    private static PDFColorSpace graySpace = new GrayColorSpace(); 


    /**
     * create a PDFColorSpace based on a Java ColorSpace
     * @param cs the Java ColorSpace
     */
    protected PDFColorSpace() {
    }

    /**
     * Get a color space by name
     *
     * @param name the name of one of the device-dependent color spaces
     */
    public static PDFColorSpace getColorSpace(int name) {
        switch (name) {
        case COLORSPACE_GRAY:
            return graySpace;

        case COLORSPACE_RGB:
            return rgbSpace;

        case COLORSPACE_CMYK:
            return cmykSpace;

        case COLORSPACE_PATTERN:
            return patternSpace;

        default:
            throw new IllegalArgumentException("Unknown Color Space name: " +
                name);
        }
    }

    /**
     * Get a color space specified in a PDFObject
     *
     * @param csobj the PDFObject with the colorspace information
     */
    public static PDFColorSpace getColorSpace(PDFObject csobj, Map resources)
        throws IOException {
        String name;

        PDFObject colorSpaces = null;

        if (resources != null) {
            colorSpaces = (PDFObject) resources.get("ColorSpace");
        }

        if (csobj.getType() == PDFObject.NAME) {
            name = csobj.getStringValue();

            if (name.equals("DeviceGray") || name.equals("G")) {
                return getColorSpace(COLORSPACE_GRAY);
            } else if (name.equals("DeviceRGB") || name.equals("RGB")) {
                return getColorSpace(COLORSPACE_RGB);
            } else if (name.equals("DeviceCMYK") || name.equals("CMYK")) {
                return getColorSpace(COLORSPACE_CMYK);
            } else if (name.equals("Pattern")) {
                return getColorSpace(COLORSPACE_PATTERN);
            } else if (colorSpaces != null) {
                csobj = (PDFObject) colorSpaces.getDictRef(name);
            }
        }

        if (csobj == null) {
            return null;
        } else if (csobj.getCache() != null) {
            return (PDFColorSpace) csobj.getCache();
        }

        PDFColorSpace value = null;

        // csobj is [/name <<dict>>]
        PDFObject[] ary = csobj.getArray();
        name = ary[0].getStringValue();

        if (name.equals("CalGray")) {
            value = graySpace; // TODO [FHe]
        } else if (name.equals("CalRGB")) {
            value = rgbSpace; // TODO [FHe]
        } else if (name.equals("Lab")) {
            value = rgbSpace; // TODO [FHe]
        } else if (name.equals("ICCBased")) {
            value = rgbSpace; // TODO [FHe]
        } else if (name.equals("Separation") || name.equals("DeviceN")) {
            PDFColorSpace alternate = getColorSpace(ary[2], resources);
            PDFFunction function = PDFFunction.getFunction(ary[3]);
            value = new AlternateColorSpace(alternate, function);
        } else if (name.equals("Indexed") || name.equals("I")) {
            /**
             * 4.5.5 [/Indexed baseColor hival lookup]
             */
            PDFColorSpace refspace = getColorSpace(ary[1], resources);

            // number of indices= ary[2], data is in ary[3];
            int count = ary[2].getIntValue();
            value = new IndexedColor(refspace, count, ary[3]);
        } else if (name.equals("Pattern")) {
            return rgbSpace; // TODO [FHe]
        } else {
            throw new PDFParseException("Unknown color space: " + name +
                " with " + ary[1]);
        }

        csobj.setCache(value);

        return value;
    }

    /**
     * get the number of components expected in the getPaint command
     */
    public abstract int getNumComponents();

    /**
     * get the PDFPaint representing the color described by the
     * given color components
     * @param components the color components corresponding to the given
     * colorspace
     * @return a PDFPaint object representing the closest Color to the
     * given components.
     */
    public PDFPaint getPaint(float[] components) {
		return PDFPaint.getColorPaint(toColor(components));
    }
    public PDFPaint getFillPaint(float[] components) {
		return PDFPaint.getPaint(toColor(components));
    }

    /**
     * get the type of this color space
     */
    public abstract int getType();
    /**
     * get the name of this color space
     */
    public abstract String getName();

	public abstract int toColor(float[] fcomp);
	
	public abstract int toColor(int[] icomp);
    
	@Override
	public String toString() {
		return "ColorSpace["+getName()+"]";
	}

}
