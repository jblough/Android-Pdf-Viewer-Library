/*
 * $Id: DCTDecode.java,v 1.2 2007/12/20 18:33:33 rbair Exp $
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

package com.sun.pdfview.decode;

import java.io.IOException;
import java.nio.IntBuffer;

import net.sf.andpdf.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.util.Log;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.colorspace.PDFColorSpace;

/**
 * decode a DCT encoded array into a byte array.  This class uses Java's
 * built-in JPEG image class to do the decoding.
 *
 * @author Mike Wessler
 */
public class DCTDecode {

    /**
     * decode an array of bytes in DCT format.
     * <p>
     * DCT is the format used by JPEG images, so this class simply
     * loads the DCT-format bytes as an image, then reads the bytes out
     * of the image to create the array.  Unfortunately, their most
     * likely use is to get turned BACK into an image, so this isn't
     * terribly efficient... but is is general... don't hit, please.
     * <p>
     * The DCT-encoded stream may have 1, 3 or 4 samples per pixel, depending
     * on the colorspace of the image.  In decoding, we look for the colorspace
     * in the stream object's dictionary to decide how to decode this image.
     * If no colorspace is present, we guess 3 samples per pixel.
     *
     * @param dict the stream dictionary
     * @param buf the DCT-encoded buffer
     * @param params the parameters to the decoder (ignored)
     * @return the decoded buffer
     */
    protected static ByteBuffer decode(PDFObject dict, ByteBuffer buf,
        PDFObject params) throws PDFParseException
    {
	//	System.out.println("DCTDecode image info: "+params);
        buf.rewind();
        
        // copy the data into a byte array required by createimage
        byte[] ary = new byte[buf.remaining()];
        buf.get(ary);

        Bitmap img = BitmapFactory.decodeByteArray(ary, 0, ary.length);

        if (img == null)
        	throw new PDFParseException("could not decode image of compressed size "+ary.length);
    	Config conf = img.getConfig();
    	Log.e("ANDPDF.dctdecode", "decoded image type"+conf);
    	int size = 4*img.getWidth()*img.getHeight();
    	if (conf == Config.RGB_565) 
    		size = 2*img.getWidth()*img.getHeight();
    	// TODO [FHe]: else ... what do we get for gray? Config.ALPHA_8?
    
        java.nio.ByteBuffer byteBuf = java.nio.ByteBuffer.allocate(size);
        img.copyPixelsToBuffer(byteBuf);
        
        ByteBuffer result = ByteBuffer.fromNIO(byteBuf);
        result.rewind();
        return result;
    }
}

