/*
 * $Id: FunctionType3.java,v 1.1 2009/02/09 16:32:23 tomoke Exp $
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
package com.sun.pdfview.function;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;

/**
 * 3.9.3 - A stitching function define a <i>stitching</i> of the subdomains of
 * several 1-input functions to produce a single new 1-input function.
 * Since the resulting stitching function is a 1-input function, the
 * domain is given by a two-element array, [ <b>Domain</b>0 <b>Domain</b>1 ].
 *
 * <pre>
 * Example 4.25
 * 5 0 obj                          % Shading dictionary
 *  << /ShadingType 3
 *      /ColorSpace /DeviceCMYK
 *      /Coords [ 0.0 0.0 0.096 0.0 0.0 1.0 00]% Concentric circles
 *      /Function 10 0 R
 *      /Extend [ true true ]
 *  >>
 * endobj
 *
 * 10 0 obj                         % Color function
 *  << /FunctionType 3
 *      /Domain [ 0.0 1.0 ]
 *      /Functions [ 11 0 R 12 0 R ]
 *      /Bounds [ 0.708 ]
 *      /Encode [ 1.0 0.0 0.0 1.0 ]
 *  >>
 * endobj
 *
 * 11 0 obj                         % First subfunction
 *  << /FunctionType 2
 *      /Domain [ 0.0 1.0 ]
 *      /C0 [ 0.929 0.357 1.000 0.298 ]
 *      /C1 [ 0.631 0.278 1.000 0.027 ]
 *      /N 1.048
 *  >>
 * endobj
 *
 * 12 0 obj                         % Second subfunction
 *  << /FunctionType 2
 *      /Domain [ 0.0 1.0 ]
 *      /C0 [ 0.929 0.357 1.000 0.298 ]
 *      /C1 [ 0.941 0.400 1.000 0.102 ]
 *      /N 1.374
 *  >>
 * endobj
 * </pre>
 */
public class FunctionType3 extends PDFFunction {

    /** 
     * the actual samples, converted to integers.  The first index is
     * input values (from 0 to size[m - 1] * size[m - 2] * ... * size[0]), 
     * and the second is the output dimension within the sample (from 0 to n)
     */
    private int[][] samples;

    /** Creates a new instance of FunctionType3 */
    protected FunctionType3() {
        super(TYPE_3);
    }

    /**
     * <p>Read the function information from a PDF Object.</p>
     * <p>Required entries ( Table 3.38)  (3200-1:2008:7.10.4, table: 41)
     * are:<li>
     *
     * <b>Functions</b> <i>array</i> (Required) An array of k 1-input functions making up
     * the stitching function. The output dimensionality of all functions
     * must be the same, and compatible with the value of <b>Range</b>
     * if <b>Range</b> is present.</li><li>
     *
     * <b>Domain</b><i>array</i> (Required) A 2 element array where
     * <b>Domain</b>0 is less than <b>Domain</b>1. This is read by the
     * <code>PDFFunction</code> superclass.</li><li>
     *
     * <b>Bounds</b> <i>array</i> (Required) An array of k-1 numbers that,
     * in combination with <b>Domain</b>, define the intervals to which each
     * function from the <b>Functions</b> array applies. <b>Bounds</b> elements
     * must be in order of increasing value, and each value must be within
     * the domain defined by >b>Domain</b>.</li><li>
     *
     * <b>Encode</b> <i>array</i> (Required) An array of 2 * k numbers that,
     * taken in pairs, map each subset of the domain defined by <bDomain</b>
     * and the <b>Bounds</b> array to the domain of the corresponding function.
     * </li></p>
     */
    protected void parse(PDFObject obj) throws IOException {
        // read the Functions array (required)
        PDFObject functionsObj = obj.getDictRef("Functions");
        if (functionsObj == null) {
            throw new PDFParseException("Functions required for function type 3!");
        }
        PDFObject[] functionsAry = functionsObj.getArray();
        int[] size = new int[functionsAry.length];
        for (int i = 0; i < functionsAry.length; i++) {
            size[i] = functionsAry[i].getIntValue();
        }

        // read the Bounds array (required)
        PDFObject boundsObj = obj.getDictRef("Bounds");
        if (boundsObj == null) {
            throw new PDFParseException("Bounds required for function type 3!");
        }
        PDFObject[] boundsAry = boundsObj.getArray();
        int[] size1 = new int[boundsAry.length];
        for (int i = 0; i < boundsAry.length; i++) {
            size1[i] = boundsAry[i].getIntValue();
        }

        // read the encode array (required)
        PDFObject encodeObj = obj.getDictRef("Encode");
        if (encodeObj != null) {
            throw new PDFParseException("Encode required for function type 3!");
        }
        PDFObject[] encodeAry = encodeObj.getArray();
        float[] encode = new float[encodeAry.length];
        for (int i = 0; i < encodeAry.length; i++) {
            encode[i] = encodeAry[i].getFloatValue();
        }
        throw new PDFParseException("Unsupported function type 3.");
    }

    /**
     * Map from <i>m</i> input values to <i>n</i> output values.
     * The number of inputs <i>m</i> must be exactly one half the size of the
     * domain.  The number of outputs should match one half the size of the
     * range.
     *
     * @param inputs an array of <i>m</i> input values
     * @param outputs an array of size <i>n</i> which will be filled
     *                with the output values, or null to return a new array
     */
    protected void doFunction(float[] inputs, int inputOffset,
            float[] outputs, int outputOffset) {
        // calculate the encoded values for each input
        float[] encoded = new float[getNumInputs()];
//        for (int i = 0; i < getNumInputs(); i++) {
//            // encode -- interpolate(x<i>, domain<2i>, domain<2i + 1>,
//            //                       encode<2i>, encode<2i + 1>)
//            encoded[i] = interpolate(inputs[i + inputOffset],
//                                     getDomain(2 * i),
//                                     getDomain((2 * i) + 1),
//                                     getEncode(2 * i),
//                                     getEncode((2 * i) + 1));
//
//            // clip to size of sample table -- min(max(e<i>, 0), size<i> - 1)
//            encoded[i] = Math.max(encoded[i], 0);
//            encoded[i] = Math.min(encoded[i], size[i] - 1);
//        }

        // do some magic
        for (int i = 0; i < getNumOutputs(); i++) {
//            if (getOrder() == 1) {
//                outputs[i + outputOffset] = multilinearInterpolate(encoded, i);
//            } else {
//                outputs[i + outputOffset] = multicubicInterpolate(encoded, i);
//            }
        }

    // now adjust the output to be within range
//        for (int i = 0; i < outputs.length; i++) {
//            // decode -- interpolate(r<i>, 0, 2^bps - 1,
//            //                       decode<2i>, decode<2i + 1>)
//            outputs[i + outputOffset] = interpolate(outputs[i + outputOffset],
//                                     0,
//                                     (float) Math.pow(2, getBitsPerSample()) - 1,
//                                     getDecode(2 * i),
//                                     getDecode((2 * i) + 1));
//        }
    }
}
