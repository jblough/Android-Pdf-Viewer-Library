/*
 * $Id: Type1CFont.java,v 1.3 2009/03/09 10:18:03 tomoke Exp $
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


/**
 * A representation, with parser, of an Adobe Type 1C font.
 * @author Mike Wessler
 */
public class Type1CFont extends OutlineFont {

    String chr2name[] = new String[256];

    byte[] data;

    int pos;

    byte[] subrs;

    float[] stack = new float[100];

    int stackptr = 0;

    String names[];

    int glyphnames[];

    int encoding[] = new int[256];

    String fontname;

    Matrix at = Utils.createMatrix(0.001f, 0, 0, 0.001f, 0, 0);

    int num;

    float fnum;

    int type;

    static int CMD = 0;

    static int NUM = 1;

    static int FLT = 2;

    /**
     * create a new Type1CFont based on a font data stream and a descriptor
     * @param baseFont the postscript name of this font
     * @param src a stream containing the font
     * @param descriptor the descriptor for this font
     */
    public Type1CFont (String baseFont, PDFObject src,
                       PDFFontDescriptor descriptor) throws IOException {
        super (baseFont, src, descriptor);

        if (!PDFFont.sUseFontSubstitution) {
	        PDFObject dataObj = descriptor.getFontFile3 ();
	        data = dataObj.getStream ();
        }
        pos = 0;
        if (!PDFFont.sUseFontSubstitution) {
        	parse ();
        }

        // TODO: free up (set to null) unused structures (data, subrs, stack)
    }

    /**
     * a debug method for printing the data
     */
    private void printData () {
        char[] parts = new char[17];
        int partsloc = 0;
        for (int i = 0; i < data.length; i++) {
            int d = ((int) data[i]) & 0xff;
            if (d == 0) {
                parts[partsloc++] = '.';
            } else if (d < 32 || d >= 127) {
                parts[partsloc++] = '?';
            } else {
                parts[partsloc++] = (char) d;
            }
            if (d < 16) {
                System.out.print ("0" + Integer.toHexString (d));
            } else {
                System.out.print (Integer.toHexString (d));
            }
            if ((i & 15) == 15) {
                System.out.println ("      " + new String (parts));
                partsloc = 0;
            } else if ((i & 7) == 7) {
                System.out.print ("  ");
                parts[partsloc++] = ' ';
            } else if ((i & 1) == 1) {
                System.out.print (" ");
            }
        }
        System.out.println ();
    }

    /**
     * read the next decoded value from the stream
     * @param charstring ????
     */
    private int readNext (boolean charstring) {
        num = (int) (data[pos++]) & 0xff;
        if (num == 30 && !charstring) { // goofy floatingpoint rep
            readFNum ();
            return type = FLT;
        } else if (num == 28) {
            num = (((int) data[pos]) << 8) + (((int) data[pos + 1]) & 0xff);
            pos += 2;
            return type = NUM;
        } else if (num == 29 && !charstring) {
            num = (((int) data[pos] & 0xff) << 24) |
                    (((int) data[pos + 1] & 0xff) << 16) |
                    (((int) data[pos + 2] & 0xff) << 8) |
                    (((int) data[pos + 3] & 0xff));
            pos += 4;
            return type = NUM;
        } else if (num == 12) {  // two-byte command
            num = 1000 + ((int) (data[pos++]) & 0xff);
            return type = CMD;
        } else if (num < 32) {
            return type = CMD;
        } else if (num < 247) {
            num -= 139;
            return type = NUM;
        } else if (num < 251) {
            num = (num - 247) * 256 + (((int) data[pos++]) & 0xff) + 108;
            return type = NUM;
        } else if (num < 255) {
            num = -(num - 251) * 256 - (((int) data[pos++]) & 0xff) - 108;
            return type = NUM;
        } else if (!charstring) { // dict shouldn't have a 255 code
            printData ();
            throw new RuntimeException ("Got a 255 code while reading dict");
        } else { // num was 255
            fnum = ((((int) data[pos] & 0xff) << 24) |
                    (((int) data[pos + 1] & 0xff) << 16) |
                    (((int) data[pos + 2] & 0xff) << 8) |
                    (((int) data[pos + 3] & 0xff))) / 65536f;
            pos += 4;
            return type = FLT;
        }
    }

    /**
     * read the next funky floating point number from the input stream.
     * value gets put into the fnum field.
     */
    public void readFNum () {
        // work in nybbles: 0-9=0-9, a=. b=E, c=E-, d=rsvd e=neg f=end
        float f = 0;
        boolean neg = false;
        int exp = 0;
        int eval = 0;
        float mul = 1;
        byte work = data[pos++];
        while (true) {
            if (work == (byte) 0xdd) {
                work = data[pos++];
            }
            int nyb = (work >> 4) & 0xf;
            work = (byte) ((work << 4) | 0xd);
            if (nyb < 10) {
                if (exp != 0) {         // working on the exponent
                    eval = eval * 10 + nyb;
                } else if (mul == 1) {  // working on an int
                    f = f * 10 + nyb;
                } else {              // working on decimal part
                    f += nyb * mul;
                    mul /= 10f;
                }
            } else if (nyb == 0xa) {    // decimal
                mul = 0.1f;
            } else if (nyb == 0xb) {    // E+
                exp = 1;
            } else if (nyb == 0xc) {    // E-
                exp = -1;
            } else if (nyb == 0xe) {      // neg
                neg = true;
            } else {
                break;
            }
        }
        fnum = (neg ? -1 : 1) * f * (float) Math.pow (10, eval * exp);
    }

    /**
     * read an integer from the input stream
     * @param len the number of bytes in the integer
     * @return the integer
     */
    private int readInt (int len) {
        int n = 0;
        for (int i = 0; i < len; i++) {
            n = (n << 8) | (((int) data[pos++]) & 0xff);
        }
        return n;
    }

    /**
     * read the next byte from the stream
     * @return the byte
     */
    private int readByte () {
        return ((int) data[pos++]) & 0xff;
    }

    // DICT structure:
    // operand operator operand operator ...
    // INDEX structure:
    // count(2) offsize [offset offset ... offset] data
    // offset array has count+1 entries
    // data starts at 3+(count+1)*offsize
    // offset for data is offset+2+(count+1)*offsize
    /**
     * get the size of the dictionary located within the stream at
     * some offset.
     * @param loc the index of the start of the dictionary
     * @return the size of the dictionary, in bytes.
     */
    public int getIndexSize (int loc) {
        //	System.out.println("Getting size of index at "+loc);
        int hold = pos;
        pos = loc;
        int count = readInt (2);
        if (count <= 0) {
            return 2;
        }
        int encsz = readByte ();
        if (encsz < 1 || encsz > 4) {
            throw new RuntimeException ("Offsize: " + encsz +
                    ", must be in range 1-4.");
        }
        // pos is now at the first offset.  last offset is at count*encsz
        pos += count * encsz;
        int end = readInt (encsz);
        pos = hold;
        return 2 + (count + 1) * encsz + end;
    }

    /**
     * return the number of entries in an Index table.
     *
     * @param loc
     * @return
     */
    public int getTableLength (int loc) {
        int hold = pos;
        pos = loc;
        int count = readInt (2);
        if (count <= 0) {
            return 2;
        }
        pos = hold;
        return count;
    }

    /**
     * A range.  There's probably a version of this class floating around
     * somewhere already in Java.
     */
    class Range {

        private int start;

        private int len;

        public Range (int start, int len) {
            this.start = start;
            this.len = len;
        }

        public final int getStart () {
            return start;
        }

        public final int getLen () {
            return len;
        }

        public final int getEnd () {
            return start + len;
        }

        public String toString () {
            return "Range: start: " + start + ", len: " + len;
        }
    }

    /**
     * Get the range of a particular index in a dictionary.
     * @param index the start of the dictionary.
     * @param id the index of the entry in the dictionary
     * @return a range describing the offsets of the start and end of
     * the entry from the start of the file, not the dictionary
     */
    Range getIndexEntry (int index, int id) {
        int hold = pos;
        pos = index;
        int count = readInt (2);
        int encsz = readByte ();
        if (encsz < 1 || encsz > 4) {
            throw new RuntimeException ("Offsize: " + encsz +
                    ", must be in range 1-4.");
        }
        pos += encsz * id;
        int from = readInt (encsz);
        Range r = new Range (from + 2 + index + encsz * (count + 1), readInt (
                encsz) - from);
        pos = hold;
        return r;
    }
    // Top DICT: NAME    CODE   DEFAULT
    // charstringtype    12 6    2
    // fontmatrix        12 7    0.001 0 0 0.001
    // charset           15      - (offset)  names of glyphs (ref to name idx)
    // encoding          16      - (offset)  array of codes
    // CharStrings       17      - (offset)
    // Private           18      - (size, offset)
    // glyph at position i in CharStrings has name charset[i]
    // and code encoding[i]
    int charstringtype = 2;

    float temps[] = new float[32];

    int charsetbase = 0;

    int encodingbase = 0;

    int charstringbase = 0;

    int privatebase = 0;

    int privatesize = 0;

    int gsubrbase = 0;

    int lsubrbase = 0;

    int gsubrsoffset = 0;

    int lsubrsoffset = 0;

    int nglyphs = 1;

    /**
     * read a dictionary that exists within some range, parsing the entries
     * within the dictionary.
     */
    private void readDict (Range r) {
        //	System.out.println("reading dictionary from "+r.getStart()+" to "+r.getEnd());
        pos = r.getStart ();
        while (pos < r.getEnd ()) {
            int cmd = readCommand (false);
            if (cmd == 1006) { // charstringtype, default=2
                charstringtype = (int) stack[0];
            } else if (cmd == 1007) { // fontmatrix
                if (stackptr == 4) {
                    at = Utils.createMatrix((float) stack[0], (float) stack[1],
                            (float) stack[2], (float) stack[3],
                            0, 0);
                } else {
                    at = Utils.createMatrix((float) stack[0], (float) stack[1],
                            (float) stack[2], (float) stack[3],
                            (float) stack[4], (float) stack[5]);
                }
            } else if (cmd == 15) { // charset
                charsetbase = (int) stack[0];
            } else if (cmd == 16) { // encoding
                encodingbase = (int) stack[0];
            } else if (cmd == 17) { // charstrings
                charstringbase = (int) stack[0];
            } else if (cmd == 18) { // private
                privatesize = (int) stack[0];
                privatebase = (int) stack[1];
            } else if (cmd == 19) { // subrs (in Private dict)
                lsubrbase = privatebase + (int) stack[0];
                lsubrsoffset = calcoffset (lsubrbase);
            }
            stackptr = 0;
        }
    }

    /**
     * read a complete command.  this may involve several numbers
     * which go onto a stack before an actual command is read.
     * @param charstring ????
     * @return the command.  Some numbers may also be on the stack.
     */
    private int readCommand (boolean charstring) {
        while (true) {
            int t = readNext (charstring);
            if (t == CMD) {
                /*
                System.out.print("CMD= "+num+", args=");
                for (int i=0; i<stackptr; i++) {
                System.out.print(" "+stack[i]);
                }
                System.out.println();
                 */
                return num;
            } else {
                stack[stackptr++] = (t == NUM) ? (float) num : fnum;
            }
        }
    }

    /**
     * parse information about the encoding of this file.
     * @param base the start of the encoding data
     */
    private void readEncodingData (int base) {
        if (base == 0) {  // this is the StandardEncoding
            //	    System.out.println("**** STANDARD ENCODING!");
            System.arraycopy (FontSupport.standardEncoding, 0, encoding, 0,
                    FontSupport.standardEncoding.length);
        } else if (base == 1) {  // this is the expert encoding
            System.out.println ("**** EXPERT ENCODING!");
            // TODO: copy ExpertEncoding
        } else {
            pos = base;
            int encodingtype = readByte ();
            if ((encodingtype & 127) == 0) {
                int ncodes = readByte ();
                for (int i = 1; i < ncodes + 1; i++) {
                    int idx = readByte () & 0xff;
                    encoding[idx] = i;
                }
            } else if ((encodingtype & 127) == 1) {
                int nranges = readByte ();
                int p = 1;
                for (int i = 0; i < nranges; i++) {
                    int start = readByte ();
                    int more = readByte ();
                    for (int j = start; j < start + more + 1; j++) {
                        encoding[j] = p++;
                    }
                }
            } else {
                System.out.println ("Bad encoding type: " + encodingtype);
            }
            // TODO: now check for supplemental encoding data
        }
    }

    /**
     * read the names of the glyphs.
     * @param base the start of the glyph name table
     */
    private void readGlyphNames (int base) {
        if (base == 0) {
            glyphnames = new int[229];
            for (int i = 0; i < glyphnames.length; i++) {
                glyphnames[i] = i;
            }
            return;
        } else if (base == 1) {
            glyphnames = FontSupport.type1CExpertCharset;
            return;
        } else if (base == 2) {
            glyphnames = FontSupport.type1CExpertSubCharset;
            return;
        }
        // nglyphs has already been set.
        glyphnames = new int[nglyphs];
        glyphnames[0] = 0;
        pos = base;
        int t = readByte ();
        if (t == 0) {
            for (int i = 1; i < nglyphs; i++) {
                glyphnames[i] = readInt (2);
            }
        } else if (t == 1) {
            int n = 1;
            while (n < nglyphs) {
                int sid = readInt (2);
                int range = readByte () + 1;
                for (int i = 0; i < range; i++) {
                    glyphnames[n++] = sid++;
                }
            }
        } else if (t == 2) {
            int n = 1;
            while (n < nglyphs) {
                int sid = readInt (2);
                int range = readInt (2) + 1;
                for (int i = 0; i < range; i++) {
                    glyphnames[n++] = sid++;
                }
            }
        }
    }

    /**
     * read a list of names
     * @param base the start of the name table
     */
    private void readNames (int base) {
        pos = base;
        int nextra = readInt (2);
        names = new String[nextra];
        //	safenames= new String[nextra];
        for (int i = 0; i < nextra; i++) {
            Range r = getIndexEntry (base, i);
            names[i] = new String (data, r.getStart (), r.getLen ());
            //	    System.out.println("Read name: "+i+" from "+r.getStart()+" to "+r.getEnd()+": "+safe(names[i]));
        }
    }

    /**
     * parse the font data.
     * @param encdif a dictionary describing the encoding.
     */
    private void parse () throws IOException {
        int majorVersion = readByte ();
        int minorVersion = readByte ();
        int hdrsz = readByte ();
        int offsize = readByte ();
        // jump over rest of header: base of font names index
        int fnames = hdrsz;
        // offset in the file of the array of font dicts
        int topdicts = fnames + getIndexSize (fnames);
        // offset in the file of local names
        int theNames = topdicts + getIndexSize (topdicts);
        // offset in the file of the array of global subroutines
        gsubrbase = theNames + getIndexSize (theNames);
        gsubrsoffset = calcoffset (gsubrbase);
        // read extra names
        readNames (theNames);
        // does this file have more than one font?
        pos = topdicts;
        if (readInt (2) != 1) {
            printData ();
            throw new RuntimeException ("More than one font in this file!");
        }
        Range r = getIndexEntry (fnames, 0);
        fontname = new String (data, r.getStart (), r.getLen ());
        // read first dict
        //	System.out.println("TOPDICT[0]:");
        readDict (getIndexEntry (topdicts, 0));
        // read the private dictionary
        //	System.out.println("PRIVATE DICT:");
        readDict (new Range (privatebase, privatesize));
        // calculate the number of glyphs
        pos = charstringbase;
        nglyphs = readInt (2);
        // now get the glyph names
        //	System.out.println("GLYPHNAMES:");
        readGlyphNames (charsetbase);
        // now figure out the encoding
        //	System.out.println("ENCODING:");
        readEncodingData (encodingbase);
    }

    /**
     * get the index of a particular name.  The name table starts with
     * the standard names in FontSupport.stdNames, and is appended by
     * any names in the name table from this font's dictionary.
     */
    private int getNameIndex (String name) {
        int val = FontSupport.findName (name, FontSupport.stdNames);
        if (val == -1) {
            val = FontSupport.findName (name, names) + FontSupport.stdNames.length;
        }
        if (val == -1) {
            val = 0;
        }
        return val;
    }

    /**
     * convert a string to one in which any non-printable bytes are
     * replaced by "<###>" where ## is the value of the byte.
     */
    private String safe (String src) {
        StringBuffer sb = new StringBuffer ();
        for (int i = 0; i < src.length (); i++) {
            char c = src.charAt (i);
            if (c >= 32 && c < 128) {
                sb.append (c);
            } else {
                sb.append ("<" + (int) c + ">");
            }
        }
        return sb.toString ();
    }

    /**
     * Read the data for a glyph from the glyph table, and transform
     * it based on the current transform.
     *
     * @param base the start of the glyph table
     * @param offset the index of this glyph in the glyph table
     */
    private synchronized Path readGlyph (int base, int offset) {
        FlPoint pt = new FlPoint ();

        // find this entry
        Range r = getIndexEntry (base, offset);

        // create a path
        Path gp = new Path ();


        // rember the start position (for recursive calls due to seac)
        int hold = pos;

        // read the glyph itself
        stackptr = 0;
        parseGlyph (r, gp, pt);

        // restore the start position
        pos = hold;

        gp.transform (at);

        return gp;
    }

    /**
     * calculate an offset code for a dictionary. Uses the count of entries
     * to determine what the offset should be.
     *
     * @param base the index of the start of the dictionary
     */
    public int calcoffset (int base) {
        int len = getTableLength (base);
        if (len < 1240) {
            return 107;
        } else if (len < 33900) {
            return 1131;
        } else {
            return 32768;
        }
    }

    /**
     * get the name associated with an ID.
     * @param id the index of the name
     * @return the name from the FontSupport.stdNames table augmented
     * by the local name table
     */
    public String getSID (int id) {
        if (id < FontSupport.stdNames.length) {
            return FontSupport.stdNames[id];
        } else {
            id -= FontSupport.stdNames.length;
            return names[id];
        }
    }

    /**
     * build an accented character out of two pre-defined glyphs.
     * @param x the x offset of the accent
     * @param y the y offset of the accent
     * @param b the index of the base glyph
     * @param a the index of the accent glyph
     * @param gp the GeneralPath into which the combined glyph will be
     * written.
     */
    private void buildAccentChar (float x, float y, char b, char a,
                                  Path gp) {
        // get the outline of the accent
        Path pathA = getOutline (a, getWidth (a, null));

        // undo the effect of the transform applied in read 
        Matrix xformA = new Matrix();
        xformA.setTranslate(x, y);
        Matrix tmp = new Matrix(at);
        if (at.invert(tmp)) {
        	xformA.preConcat(tmp);
        }
        else {
	        // oh well ...
	    }
        pathA.transform (xformA);

        Path pathB = getOutline (b, getWidth (b, null));

        Matrix xformB = new Matrix();
        if (at.invert(xformB)) {
            pathB.transform(xformB);
        } else {
            // ignore
        }

        gp.addPath(pathB);
        gp.addPath(pathA);
    }

    /**
     * parse a glyph defined in a particular range
     * @param r the range of the glyph definition
     * @param gp a GeneralPath in which to store the glyph outline
     * @param pt a FlPoint representing the end of the current path
     */
    void parseGlyph (Range r, Path gp, FlPoint pt) {
        pos = r.getStart ();
        int i;
        float x1, y1, x2, y2, x3, y3, ybase;
        int hold;
        int stemhints = 0;
        while (pos < r.getEnd ()) {
            int cmd = readCommand (true);
            hold = 0;
            switch (cmd) {
                case 1: // hstem
                case 3: // vstem
                    stackptr = 0;
                    break;
                case 4: // vmoveto
                    if (stackptr > 1) {  // this is the first call, arg1 is width
                        stack[0] = stack[1];
                    }
                    pt.y += stack[0];
                    if (pt.open) {
                        gp.close();
                    }
                    pt.open = false;
                    gp.moveTo (pt.x, pt.y);
                    stackptr = 0;
                    break;
                case 5: // rlineto
                    for (i = 0; i < stackptr;) {
                        pt.x += stack[i++];
                        pt.y += stack[i++];
                        gp.lineTo (pt.x, pt.y);
                    }
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 6: // hlineto
                    for (i = 0; i < stackptr;) {
                        if ((i & 1) == 0) {
                            pt.x += stack[i++];
                        } else {
                            pt.y += stack[i++];
                        }
                        gp.lineTo (pt.x, pt.y);
                    }
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 7: // vlineto
                    for (i = 0; i < stackptr;) {
                        if ((i & 1) == 0) {
                            pt.y += stack[i++];
                        } else {
                            pt.x += stack[i++];
                        }
                        gp.lineTo (pt.x, pt.y);
                    }
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 8: // rrcurveto
                    for (i = 0; i < stackptr;) {
                        x1 = pt.x + stack[i++];
                        y1 = pt.y + stack[i++];
                        x2 = x1 + stack[i++];
                        y2 = y1 + stack[i++];
                        pt.x = x2 + stack[i++];
                        pt.y = y2 + stack[i++];
                        gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    }
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 10: // callsubr
                    hold = pos;
                    i = (int) stack[--stackptr] + lsubrsoffset;
                    Range lsubr = getIndexEntry (lsubrbase, i);
                    parseGlyph (lsubr, gp, pt);
                    pos = hold;
                    break;
                case 11: // return
                    return;
                case 14: // endchar
                    // width x y achar bchar endchar == x y achar bchar seac
                    if (stackptr == 5) {
                        buildAccentChar (stack[1], stack[2], (char) stack[3],
                                (char) stack[4], gp);
                    }
                    if (pt.open) {
                        gp.close();
                    }
                    pt.open = false;
                    stackptr = 0;
                    break;
                case 18: // hstemhm
                    stemhints += stackptr / 2;
                    stackptr = 0;
                    break;
                case 19: // hintmask
                case 20: // cntrmask
                    stemhints += stackptr / 2;
                    pos += (stemhints - 1) / 8 + 1;
                    stackptr = 0;
                    break;
                case 21: // rmoveto
                    if (stackptr > 2) {
                        stack[0] = stack[1];
                        stack[1] = stack[2];
                    }
                    pt.x += stack[0];
                    pt.y += stack[1];
                    if (pt.open) {
                        gp.close();
                    }
                    gp.moveTo (pt.x, pt.y);
                    pt.open = false;
                    stackptr = 0;
                    break;
                case 22: // hmoveto
                    if (stackptr > 1) {
                        stack[0] = stack[1];
                    }
                    pt.x += stack[0];
                    if (pt.open) {
                        gp.close();
                    }
                    gp.moveTo (pt.x, pt.y);
                    pt.open = false;
                    stackptr = 0;
                    break;
                case 23: // vstemhm
                    stemhints += stackptr / 2;
                    stackptr = 0;
                    break;
                case 24: // rcurveline
                    for (i = 0; i < stackptr - 2;) {
                        x1 = pt.x + stack[i++];
                        y1 = pt.y + stack[i++];
                        x2 = x1 + stack[i++];
                        y2 = y1 + stack[i++];
                        pt.x = x2 + stack[i++];
                        pt.y = y2 + stack[i++];
                        gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    }
                    pt.x += stack[i++];
                    pt.y += stack[i++];
                    gp.lineTo (pt.x, pt.y);
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 25: // rlinecurve
                    for (i = 0; i < stackptr - 6;) {
                        pt.x += stack[i++];
                        pt.y += stack[i++];
                        gp.lineTo (pt.x, pt.y);
                    }
                    x1 = pt.x + stack[i++];
                    y1 = pt.y + stack[i++];
                    x2 = x1 + stack[i++];
                    y2 = y1 + stack[i++];
                    pt.x = x2 + stack[i++];
                    pt.y = y2 + stack[i++];
                    gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 26: // vvcurveto
                    i = 0;
                    if ((stackptr & 1) == 1) { // odd number of arguments
                        pt.x += stack[i++];
                    }
                    while (i < stackptr) {
                        x1 = pt.x;
                        y1 = pt.y + stack[i++];
                        x2 = x1 + stack[i++];
                        y2 = y1 + stack[i++];
                        pt.x = x2;
                        pt.y = y2 + stack[i++];
                        gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    }
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 27: // hhcurveto
                    i = 0;
                    if ((stackptr & 1) == 1) { // odd number of arguments
                        pt.y += stack[i++];
                    }
                    while (i < stackptr) {
                        x1 = pt.x + stack[i++];
                        y1 = pt.y;
                        x2 = x1 + stack[i++];
                        y2 = y1 + stack[i++];
                        pt.x = x2 + stack[i++];
                        pt.y = y2;
                        gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    }
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 29: // callgsubr
                    hold = pos;
                    i = (int) stack[--stackptr] + gsubrsoffset;
                    Range gsubr = getIndexEntry (gsubrbase, i);
                    parseGlyph (gsubr, gp, pt);
                    pos = hold;
                    break;
                case 30: // vhcurveto
                    hold = 4;
                case 31: // hvcurveto
                    for (i = 0; i < stackptr;) {
                        boolean hv = (((i + hold) & 4) == 0);
                        x1 = pt.x + (hv ? stack[i++] : 0);
                        y1 = pt.y + (hv ? 0 : stack[i++]);
                        x2 = x1 + stack[i++];
                        y2 = y1 + stack[i++];
                        pt.x = x2 + (hv ? 0 : stack[i++]);
                        pt.y = y2 + (hv ? stack[i++] : 0);
                        if (i == stackptr - 1) {
                            if (hv) {
                                pt.x += stack[i++];
                            } else {
                                pt.y += stack[i++];
                            }
                        }
                        gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    }
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 1000: // old dotsection command.  ignore.
                    stackptr = 0;
                    break;
                case 1003: // and
                    x1 = stack[--stackptr];
                    y1 = stack[--stackptr];
                    stack[stackptr++] = ((x1 != 0) && (y1 != 0)) ? 1 : 0;
                    break;
                case 1004: // or
                    x1 = stack[--stackptr];
                    y1 = stack[--stackptr];
                    stack[stackptr++] = ((x1 != 0) || (y1 != 0)) ? 1 : 0;
                    break;
                case 1005: // not
                    x1 = stack[--stackptr];
                    stack[stackptr++] = (x1 == 0) ? 1 : 0;
                    break;
                case 1009: // abs
                    stack[stackptr - 1] = Math.abs (stack[stackptr - 1]);
                    break;
                case 1010: // add
                    x1 = stack[--stackptr];
                    y1 = stack[--stackptr];
                    stack[stackptr++] = x1 + y1;
                    break;
                case 1011: // sub
                    x1 = stack[--stackptr];
                    y1 = stack[--stackptr];
                    stack[stackptr++] = y1 - x1;
                    break;
                case 1012: // div
                    x1 = stack[--stackptr];
                    y1 = stack[--stackptr];
                    stack[stackptr++] = y1 / x1;
                    break;
                case 1014: // neg
                    stack[stackptr - 1] = -stack[stackptr - 1];
                    break;
                case 1015: // eq
                    x1 = stack[--stackptr];
                    y1 = stack[--stackptr];
                    stack[stackptr++] = (x1 == y1) ? 1 : 0;
                    break;
                case 1018: // drop
                    stackptr--;
                    break;
                case 1020: // put
                    i = (int) stack[--stackptr];
                    x1 = stack[--stackptr];
                    temps[i] = x1;
                    break;
                case 1021: // get
                    i = (int) stack[--stackptr];
                    stack[stackptr++] = temps[i];
                    break;
                case 1022: // ifelse
                    if (stack[stackptr - 2] > stack[stackptr - 1]) {
                        stack[stackptr - 4] = stack[stackptr - 3];
                    }
                    stackptr -= 3;
                    break;
                case 1023: // random
                    stack[stackptr++] = (float) Math.random ();
                    break;
                case 1024: // mul
                    x1 = stack[--stackptr];
                    y1 = stack[--stackptr];
                    stack[stackptr++] = y1 * x1;
                    break;
                case 1026: // sqrt
                    stack[stackptr - 1] = (float) Math.sqrt (stack[stackptr - 1]);
                    break;
                case 1027: // dup
                    x1 = stack[stackptr - 1];
                    stack[stackptr++] = x1;
                    break;
                case 1028: // exch
                    x1 = stack[stackptr - 1];
                    stack[stackptr - 1] = stack[stackptr - 2];
                    stack[stackptr - 2] = x1;
                    break;
                case 1029: // index
                    i = (int) stack[stackptr - 1];
                    if (i < 0) {
                        i = 0;
                    }
                    stack[stackptr - 1] = stack[stackptr - 2 - i];
                    break;
                case 1030: // roll
                    i = (int) stack[--stackptr];
                    int n = (int) stack[--stackptr];
                    // roll n number by i (+ = upward)
                    if (i > 0) {
                        i = i % n;
                    } else {
                        i = n - (-i % n);
                    }
                    // x x x x i y y y -> y y y x x x x i (where i=3)
                    if (i > 0) {
                        float roll[] = new float[n];
                        System.arraycopy (stack, stackptr - 1 - i, roll, 0, i);
                        System.arraycopy (stack, stackptr - 1 - n, roll, i,
                                n - i);
                        System.arraycopy (roll, 0, stack, stackptr - 1 - n, n);
                    }
                    break;
                case 1034: // hflex
                    x1 = pt.x + stack[0];
                    y1 = ybase = pt.y;
                    x2 = x1 + stack[1];
                    y2 = y1 + stack[2];
                    pt.x = x2 + stack[3];
                    pt.y = y2;
                    gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    x1 = pt.x + stack[4];
                    y1 = pt.y;
                    x2 = x1 + stack[5];
                    y2 = ybase;
                    pt.x = x2 + stack[6];
                    pt.y = y2;
                    gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 1035: // flex
                    x1 = pt.x + stack[0];
                    y1 = pt.y + stack[1];
                    x2 = x1 + stack[2];
                    y2 = y1 + stack[3];
                    pt.x = x2 + stack[4];
                    pt.y = y2 + stack[5];
                    gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    x1 = pt.x + stack[6];
                    y1 = pt.y + stack[7];
                    x2 = x1 + stack[8];
                    y2 = y1 + stack[9];
                    pt.x = x2 + stack[10];
                    pt.y = y2 + stack[11];
                    gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 1036: // hflex1
                    ybase = pt.y;
                    x1 = pt.x + stack[0];
                    y1 = pt.y + stack[1];
                    x2 = x1 + stack[2];
                    y2 = y1 + stack[3];
                    pt.x = x2 + stack[4];
                    pt.y = y2;
                    gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    x1 = pt.x + stack[5];
                    y1 = pt.y;
                    x2 = x1 + stack[6];
                    y2 = y1 + stack[7];
                    pt.x = x2 + stack[8];
                    pt.y = ybase;
                    gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    pt.open = true;
                    stackptr = 0;
                    break;
                case 1037: // flex1
                    ybase = pt.y;
                    float xbase = pt.x;
                    x1 = pt.x + stack[0];
                    y1 = pt.y + stack[1];
                    x2 = x1 + stack[2];
                    y2 = y1 + stack[3];
                    pt.x = x2 + stack[4];
                    pt.y = y2 + stack[5];
                    gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    x1 = pt.x + stack[6];
                    y1 = pt.y + stack[7];
                    x2 = x1 + stack[8];
                    y2 = y1 + stack[9];
                    if (Math.abs (x2 - xbase) > Math.abs (y2 - ybase)) {
                        pt.x = x2 + stack[10];
                        pt.y = ybase;
                    } else {
                        pt.x = xbase;
                        pt.y = y2 + stack[10];
                    }
                    gp.cubicTo(x1, y1, x2, y2, pt.x, pt.y);
                    pt.open = true;
                    stackptr = 0;
                    break;
                default:
                    System.out.println ("ERROR! TYPE1C CHARSTRING CMD IS " + cmd);
                    break;
            }
        }
    }

    /**
     * Get a glyph outline by name
     *
     * @param name the name of the desired glyph
     * @return the glyph outline, or null if unavailable
     */
    protected Path getOutline (String name, float width) {
        // first find the index of this name
        int index = getNameIndex (name);

        // now find the glyph with that name
        for (int i = 0; i < glyphnames.length; i++) {
            if (glyphnames[i] == index) {
                return readGlyph (charstringbase, i);
            }
        }

        // not found -- return the unknown glyph
        return readGlyph (charstringbase, 0);
    }

    /**
     * Get a glyph outline by character code
     *
     * Note this method must always return an outline 
     *
     * @param src the character code of the desired glyph
     * @return the glyph outline
     */
    protected Path getOutline (char src, float width) {
        // ignore high bits
        int index = (int) (src & 0xff);

        // if we use a standard encoding, the mapping is from glyph to SID
        // therefore we must find the glyph index in the name table
        if (encodingbase == 0 || encodingbase == 1) {
            for (int i = 0; i < glyphnames.length; i++) {
                if (glyphnames[i] == encoding[index]) {
                    return readGlyph (charstringbase, i);
                }
            }
        } else {
            // for a custom encoding, the mapping is from glyph to GID, so
            // we can just map the glyph directly
            if (index > 0 && index < encoding.length) {
                return readGlyph (charstringbase, encoding[index]);
            }
        }

        // for some reason the glyph was not found, return the empty glyph
        return readGlyph (charstringbase, 0);
    }
}

