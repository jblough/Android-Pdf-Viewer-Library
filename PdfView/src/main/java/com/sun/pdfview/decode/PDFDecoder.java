/*
 * $Id: PDFDecoder.java,v 1.5 2009/03/12 12:26:19 tomoke Exp $
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

import net.sf.andpdf.nio.ByteBuffer;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.decrypt.PDFDecrypterFactory;

/**
 * A PDF Decoder encapsulates all the methods of decoding a stream of bytes
 * based on all the various encoding methods.
 * <p>
 * You should use the decodeStream() method of this object rather than using
 * any of the decoders directly.
 */
public class PDFDecoder {

    /** Creates a new instance of PDFDecoder */
    private PDFDecoder() {
    }

    /**
     * decode a byte[] stream using the filters specified in the object's
     * dictionary (passed as argument 1).
     * @param dict the dictionary associated with the stream
     * @param streamBuf the data in the stream, as a byte buffer
     */
    public static ByteBuffer decodeStream(PDFObject dict, ByteBuffer streamBuf)
            throws IOException {

        PDFObject filter = dict.getDictRef("Filter");
        if (filter == null) {
            // just apply default decryption
            return dict.getDecrypter().decryptBuffer(null, dict, streamBuf);
        } else {
            // apply filters
            PDFObject ary[];
            PDFObject params[];
	    if (filter.getType() == PDFObject.NAME) {
                ary = new PDFObject[1];
                ary[0] = filter;
                params = new PDFObject[1];
                params[0] = dict.getDictRef("DecodeParms");
            } else {
                ary = filter.getArray();
                PDFObject parmsobj = dict.getDictRef("DecodeParms");
                if (parmsobj != null) {
                    params = parmsobj.getArray();
                } else {
                    params = new PDFObject[ary.length];
                }
            }

            // determine whether default encryption applies or if there's a
            // specific Crypt filter; it must be the first filter according to
            // the errata for PDF1.7
            boolean specificCryptFilter =
                    ary.length != 0 && ary[0].getStringValue().equals("Crypt");
            if (!specificCryptFilter) {
                // No Crypt filter, so should apply default decryption (if
                // present!)
                streamBuf = dict.getDecrypter().decryptBuffer(
                        null, dict, streamBuf);
            }

            for (int i = 0; i < ary.length; i++) {
                String enctype = ary[i].getStringValue();
                if (enctype == null) {
                } else if (enctype.equals("FlateDecode") || enctype.equals("Fl")) {
                    streamBuf = FlateDecode.decode(dict, streamBuf, params[i]);
                } else if (enctype.equals("LZWDecode") || enctype.equals("LZW")) {
                    streamBuf = LZWDecode.decode(streamBuf, params[i]);
                } else if (enctype.equals("ASCII85Decode") || enctype.equals("A85")) {
                    streamBuf = ASCII85Decode.decode(streamBuf, params[i]);
                } else if (enctype.equals("ASCIIHexDecode") || enctype.equals("AHx")) {
                    streamBuf = ASCIIHexDecode.decode(streamBuf, params[i]);
                } else if (enctype.equals("RunLengthDecode") || enctype.equals("RL")) {
                    streamBuf = RunLengthDecode.decode(streamBuf, params[i]);
                } else if (enctype.equals("DCTDecode") || enctype.equals("DCT")) {
                    streamBuf = DCTDecode.decode(dict, streamBuf, params[i]);
                } else if (enctype.equals("CCITTFaxDecode") || enctype.equals("CCF")) {
                    streamBuf = CCITTFaxDecode.decode(dict, streamBuf, params[i]);
                } else if (enctype.equals("Crypt")) {
                    String cfName = PDFDecrypterFactory.CF_IDENTITY;
                    if (params[i] != null) {
                        final PDFObject nameObj = params[i].getDictRef("Name");
                        if (nameObj != null && nameObj.getType() == PDFObject.NAME) {
                            cfName = nameObj.getStringValue();
                        }
                    }
                    streamBuf = dict.getDecrypter().decryptBuffer(cfName, null, streamBuf);
                } else {
                    throw new PDFParseException("Unknown coding method:" + ary[i].getStringValue());
                }
            }
        }

        return streamBuf;
    }
}
