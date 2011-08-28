/*
 * $Id: PDFParser.java,v 1.11 2009/03/15 20:47:38 tomoke Exp $
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sf.andpdf.refs.WeakReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.utils.Utils;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Path.FillType;
import android.util.Log;

import com.sun.pdfview.colorspace.PDFColorSpace;
import com.sun.pdfview.font.PDFFont;

/**
 * PDFParser is the class that parses a PDF content stream and
 * produces PDFCmds for a PDFPage.  You should never ever see it run:
 * it gets created by a PDFPage only if needed, and may even run in
 * its own thread.
 *
 * @author Mike Wessler
 * @author Ferenc Hechler (ferenc@hechler.de)
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public class PDFParser extends BaseWatchable {

    /** emit a file of DCT stream data. */
    public final static String DEBUG_DCTDECODE_DATA = "debugdctdecode";
    static final boolean RELEASE = true;
    static final int PDF_CMDS_RANGE1_MIN = 1;
    static final int PDF_CMDS_RANGE1_MAX = Integer.MAX_VALUE;
    static final int PDF_CMDS_RANGE2_MIN = 0;
    static final int PDF_CMDS_RANGE2_MAX = 0;
    static final int PDF_CMDS_RANGE3_MIN = 0;
    static final int PDF_CMDS_RANGE3_MAX = 0;
    private static final String TAG = "ANDPDF.pdfparser";
    private int cmdCnt;
    // ---- parsing variables
    private Stack<Object> stack;          // stack of Object
    private Stack<ParserState> parserStates;    // stack of RenderState
    // the current render state
    private ParserState state;
    private Path path;
    private int clip;
    private int loc;
    private boolean resend = false;
    private Tok tok;
    private boolean catchexceptions;   // Indicates state of BX...EX
    /** a weak reference to the page we render into.  For the page
     * to remain available, some other code must retain a strong reference to it.
     */
    private WeakReference pageRef;
    /** the actual command, for use within a singe iteration.  Note that
     * this must be released at the end of each iteration to assure the
     * page can be collected if not in use
     */
    private PDFPage cmds;
    // ---- result variables
    byte[] stream;
    HashMap<String, PDFObject> resources;
//    public static int debuglevel = 4000;
// TODO [FHe]: changed for debugging
    public static int debuglevel = -1;

    public static void debug(String msg, int level) {
        if (level > debuglevel) {
            System.out.println(escape(msg));
        }
    }

    public static String escape(String msg) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c != '\n' && (c < 32 || c >= 127)) {
                c = '?';
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static void setDebugLevel(int level) {
        debuglevel = level;
    }

    /**
     * Don't call this constructor directly.  Instead, use
     * PDFFile.getPage(int pagenum) to get a PDFPage.  There should
     * never be any reason for a user to create, access, or hold
     * on to a PDFParser.
     */
    public PDFParser(PDFPage cmds, byte[] stream,
            HashMap<String, PDFObject> resources) {
        super();

        this.pageRef = new WeakReference<PDFPage>(cmds);
        this.resources = resources;
        if (resources == null) {
            this.resources = new HashMap<String, PDFObject>();
        }

        this.stream = stream;
        this.cmdCnt = 0;
    }

    /////////////////////////////////////////////////////////////////
    //  B E G I N   R E A D E R   S E C T I O N
    /////////////////////////////////////////////////////////////////
    /**
     * a token from a PDF Stream
     */
    static class Tok {

        /** begin bracket &lt; */
        public static final int BRKB = 11;
        /** end bracket &gt; */
        public static final int BRKE = 10;
        /** begin array [ */
        public static final int ARYB = 9;
        /** end array ] */
        public static final int ARYE = 8;
        /** String (, readString looks for trailing ) */
        public static final int STR = 7;
        /** begin brace { */
        public static final int BRCB = 5;
        /** end brace } */
        public static final int BRCE = 4;
        /** number */
        public static final int NUM = 3;
        /** keyword */
        public static final int CMD = 2;
        /** name (begins with /) */
        public static final int NAME = 1;
        /** unknown token */
        public static final int UNK = 0;
        /** end of stream */
        public static final int EOF = -1;
        /** the string value of a STR, NAME, or CMD token */
        public String name;
        /** the value of a NUM token */
        public double value;
        /** the type of the token */
        public int type;

        /** a printable representation of the token */
        @Override
        public String toString() {
            if (type == NUM) {
                return "NUM: " + value;
            } else if (type == CMD) {
                return "CMD: " + name;
            } else if (type == UNK) {
                return "UNK";
            } else if (type == EOF) {
                return "EOF";
            } else if (type == NAME) {
                return "NAME: " + name;
            } else if (type == CMD) {
                return "CMD: " + name;
            } else if (type == STR) {
                return "STR: (" + name;
            } else if (type == ARYB) {
                return "ARY [";
            } else if (type == ARYE) {
                return "ARY ]";
            } else {
                return "some kind of brace (" + type + ")";
            }
        }
    }

    /**
     * put the current token back so that it is returned again by
     * nextToken().
     */
    private void throwback() {
        resend = true;
    }

    /**
     * get the next token.
     * TODO: this creates a new token each time.  Is this strictly
     * necessary?
     */
    private Tok nextToken() {
        final int slen = stream.length;

        if (resend) {
            resend = false;
            return tok;
        } else {
            tok = new Tok();
            // skip whitespace
            while (loc < slen && PDFFile.isWhiteSpace(stream[loc])) {
                loc++;
            }
            if (loc >= slen) {
                tok.type = Tok.EOF;
                return tok;
            } else {
                int c = stream[loc++];
                // examine the character:
                while (c == '%') {
                    // skip comments
                    final StringBuffer comment = new StringBuffer();
                    while (loc < slen && c != '\n') {
                        comment.append((char) c);
                        c = stream[loc++];
                    }
                    if (loc < slen) {
                        c = stream[loc++];      // eat the newline
                        if (c == '\r') {
                            c = stream[loc++];  // eat a following return
                        }
                    }
                    if (!RELEASE) {
                        debug("Read comment: " + comment.toString(), -1);
                    }
                }

                switch (c) {
                    case '[':
                        tok.type = Tok.ARYB;
                        break;
                    case ']':
                        tok.type = Tok.ARYE;
                        break;
                    case '(':
                        // read a string
                        tok.type = Tok.STR;
                        tok.name = readString();
                        break;
                    case '{':
                        tok.type = Tok.BRCB;
                        break;
                    case '}':
                        tok.type = Tok.BRCE;
                        break;
                    case '<':
                        if (stream[loc++] == '<') {
                            tok.type = Tok.BRKB;
                        } else {
                            loc--;
                            tok.type = Tok.STR;
                            tok.name = readByteArray();
                        }
                        break;
                    case '/':
                        tok.type = Tok.NAME;
                        tok.name = readName();
                        break;
                    case '.':
                    case '-':
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        loc--;
                        tok.type = Tok.NUM;
                        tok.value = readNum();
                        break;
                    default:
                        if (c == '>' && stream[loc++] == '>') {
                            tok.type = Tok.BRKE;
                        } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '\'' || c == '"') {
                            loc--;
                            tok.type = Tok.CMD;
                            tok.name = readName();
                        } else {
                            System.out.println("Encountered character: " + c + " (" + (char) c + ")");
                            tok.type = Tok.UNK;
                        }
                }
                if (!RELEASE) {
                    debug("Read token: " + tok, -1);
                }
                return tok;
            }
        }
    }

    /**
     * read a name (sequence of non-PDF-delimiting characters) from the
     * stream.
     */
    private String readName() {
        final byte[] stream_ = this.stream;

        int start = loc;
        while (loc < stream_.length && PDFFile.isRegularCharacter(stream_[loc])) {
            loc++;
        }
        return new String(stream_, start, loc - start);
    }

    /**
     * read a floating point number from the stream
     */
    private double readNum() {
        final byte[] stream_ = this.stream;

        int c = stream_[loc++];
        boolean neg = c == '-';
        boolean sawdot = c == '.';
        double dotmult = sawdot ? 0.1 : 1;
        double value = (c >= '0' && c <= '9') ? c - '0' : 0;
        while (true) {
            c = stream_[loc++];
            if (c == '.') {
                if (sawdot) {
                    loc--;
                    break;
                }
                sawdot = true;
                dotmult = 0.1;
            } else if (c >= '0' && c <= '9') {
                int val = c - '0';
                if (sawdot) {
                    value += val * dotmult;
                    dotmult *= 0.1;
                } else {
                    value = value * 10 + val;
                }
            } else {
                loc--;
                break;
            }
        }

        return neg ? -value : value;
    }

    /**
     * <p>read a String from the stream.  Strings begin with a '('
     * character, which has already been read, and end with a balanced ')'
     * character.  A '\' character starts an escape sequence of up
     * to three octal digits.</p>
     *
     * <p>Parenthesis must be enclosed by a balanced set of parenthesis,
     * so a string may enclose balanced parenthesis.</p>
     *
     * @return the string with escape sequences replaced with their
     * values
     */
    private String readString() {
        final byte[] stream_ = this.stream;
        int parenLevel = 0;
        final StringBuffer sb = new StringBuffer();

        for (int to = stream_.length; loc < to;) {
            int c = stream_[loc++];
            if (c == ')') {
                if (parenLevel-- == 0) {
                    break;
                }
            } else if (c == '(') {
                parenLevel++;
            } else if (c == '\\') {
                // escape sequences
                c = stream_[loc++];
                if (c >= '0' && c < '8') {
                    int val = 0;
                    for (int count = 0; c >= '0' && c < '8' && count < 3; ++count) {
                        val = (val << 3) + c - '0';
                        c = stream_[loc++];
                    }
                    loc--;
                    c = val;
                } else if (c == 'n') {
                    c = '\n';
                } else if (c == 'r') {
                    c = '\r';
                } else if (c == 't') {
                    c = '\t';
                } else if (c == 'b') {
                    c = '\b';
                } else if (c == 'f') {
                    c = '\f';
                }
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    /**
     * read a byte array from the stream.  Byte arrays begin with a '<'
     * character, which has already been read, and end with a '>'
     * character.  Each byte in the array is made up of two hex characters,
     * the first being the high-order bit.
     *
     * We translate the byte arrays into char arrays by combining two bytes
     * into a character, and then translate the character array into a string.
     * [JK FIXME this is probably a really bad idea!]
     *
     * @return the byte array
     */
    private String readByteArray() {
        final byte[] stream_ = this.stream;
        final StringBuffer buf = new StringBuffer();

        int count = 0;
        char w = (char) 0;

        // read individual bytes and format into a character array
        for (int to = stream_.length; (loc < to) && (stream_[loc] != '>');) {
            final char c = (char) stream_[loc];
            byte b = (byte) 0;

            if (c >= '0' && c <= '9') {
                b = (byte) (c - '0');
            } else if (c >= 'a' && c <= 'f') {
                b = (byte) (10 + (c - 'a'));
            } else if (c >= 'A' && c <= 'F') {
                b = (byte) (10 + (c - 'A'));
            } else {
                loc++;
                continue;
            }

            // calculate where in the current byte this character goes
            final int offset = 1 - (count % 2);
            w |= (0xf & b) << (offset << 2);

            // increment to the next char if we've written four bytes
            if (offset == 0) {
                buf.append(w);
                w = (char) 0;
            }

            ++count;
            ++loc;
        }

        // ignore trailing '>'
        ++loc;

        return buf.toString();
    }

    /////////////////////////////////////////////////////////////////
    //  B E G I N   P A R S E R   S E C T I O N
    /////////////////////////////////////////////////////////////////
    /**
     * Called to prepare for some iterations
     */
    @Override
    public void setup() {
        stack = new Stack<Object>();
        parserStates = new Stack<ParserState>();
        state = new ParserState();
        path = new Path();
        loc = 0;
        clip = 0;

        //initialize the ParserState
        // TODO: check GRAY or RGB
        state.fillCS =
                PDFColorSpace.getColorSpace(PDFColorSpace.COLORSPACE_GRAY);
        state.strokeCS =
                PDFColorSpace.getColorSpace(PDFColorSpace.COLORSPACE_GRAY);
        state.textFormat = new PDFTextFormat();

        // HexDump.printData(stream);
        // System.out.println(dumpStream());
    }

    /**
     * parse the stream.  commands are added to the PDFPage initialized
     * in the constructor as they are encountered.
     * <p>
     * Page numbers in comments refer to the Adobe PDF specification.<br>
     * commands are listed in PDF spec 32000-1:2008 in Table A.1
     *
     * @return <ul><li>Watchable.RUNNING when there are commands to be processed
     *             <li>Watchable.COMPLETED when the page is done and all
     *                 the commands have been processed
     *             <li>Watchable.STOPPED if the page we are rendering into is
     *                 no longer available
     *         </ul> 
     */
    public int iterate() throws Exception {
        // make sure the page is still available, and create the reference
        // to it for use within this iteration
        cmds = (PDFPage) pageRef.get();
        if (cmds == null) {
            System.out.println("Page gone.  Stopping");
            return Watchable.STOPPED;
        }

        Object obj = parseObject();

        // if there's nothing left to parse, we're done
        if (obj == null) {
            return Watchable.COMPLETED;
        }

        if (obj instanceof Tok) {
            // it's a command.  figure out what to do.
            // (if not, the token will be "pushed" onto the stack)
            String cmd = ((Tok) obj).name;
            if (!RELEASE) {
                cmdCnt += 1;
                if (!(((cmdCnt >= PDF_CMDS_RANGE1_MIN) && (cmdCnt <= PDF_CMDS_RANGE1_MAX)) || ((cmdCnt >= PDF_CMDS_RANGE2_MIN) && (cmdCnt <= PDF_CMDS_RANGE2_MAX)) || ((cmdCnt >= PDF_CMDS_RANGE3_MIN) && (cmdCnt <= PDF_CMDS_RANGE3_MAX)))) {
                    stack.setSize(0);
                    return Watchable.RUNNING;
                }
                debug("Command [" + cmdCnt + "]: " + cmd + " (stack size is " + stack.size() + ":" + dump(stack) + ")", 0);
            }
            try {
                switch ((cmd.length() > 0 ? cmd.charAt(0) : 0) + (cmd.length() > 1 ? cmd.charAt(1) << 8 : 0) + (cmd.length() > 2 ? cmd.charAt(2) << 16 : 0)) {
                    case 'q':
                        // push the parser state
                        parserStates.push((ParserState) state.clone());

                        // push graphics state
                        cmds.addPush();
                        break;
                    case 'Q':
                        processQCmd();
                        break;
                    case 'c' + ('m' << 8): {
                        // set transform to array of values
                        float[] elts = popFloat(6);
                        Matrix xform = new Matrix();
                        Utils.setMatValues(xform, elts);
                        cmds.addXform(xform);
                        break;
                    }
                    case 'w':
                        // set stroke width
                        cmds.addStrokeWidth(popFloat());
                        break;
                    case 'J':
                        // set end cap style
                        cmds.addEndCap(popInt());
                        break;
                    case 'j':
                        // set line join style
                        cmds.addLineJoin(popInt());
                        break;
                    case 'M':
                        // set miter limit
                        cmds.addMiterLimit(popInt());
                        break;
                    case 'd': {
                        // set dash style and phase
                        float phase = popFloat();
                        float[] dashary = popFloatArray();
                        cmds.addDash(dashary, phase);
                        break;
                    }
                    case 'r' + ('i' << 8):
                        // TODO: do something with rendering intent (page 197)
                        break;
                    case 'i':
                        popFloat();
                        // TODO: do something with flatness tolerance
                        break;
                    case 'g' + ('s' << 8):
                        // set graphics state to values in a named dictionary
                        setGSState(popString());
                        break;
                    case 'm': {
                        // path move to
                        float y = popFloat();
                        float x = popFloat();
                        path.moveTo(x, y);
                        break;
                    }
                    case 'l': {
                        // path line to
                        float y = popFloat();
                        float x = popFloat();
                        path.lineTo(x, y);
                        break;
                    }
                    case 'c': {
                        // path curve to
                        float a[] = popFloat(6);
                        path.cubicTo(a[0], a[1], a[2], a[3], a[4], a[5]);
                        break;
                    }
                    case 'v': {
                        // path curve; first control point= start
                        float a[] = popFloat(4);
                        // TODO: remember last point
                        path.quadTo(a[0], a[1], a[2], a[3]);
//                PointF cp = path.getCurrentPoint();
//                path.curveTo((float) cp.getX(), (float) cp.getY(),
//                        a[0], a[1], a[2], a[3]);
                        break;
                    }
                    case 'y': {
                        // path curve; last control point= end
                        float a[] = popFloat(4);
                        path.cubicTo(a[0], a[1], a[2], a[3], a[2], a[3]);
                        break;
                    }
                    case 'h':
                        // path close
                        path.close();
                        break;
                    case 'r' + ('e' << 8): {
                        // path add rectangle
                        float a[] = popFloat(4);
                        path.moveTo(a[0], a[1]);
                        path.lineTo(a[0] + a[2], a[1]);
                        path.lineTo(a[0] + a[2], a[1] + a[3]);
                        path.lineTo(a[0], a[1] + a[3]);
                        path.close();
                        break;
                    }
                    case 'S':
                        // stroke the path
                        cmds.addPath(path, PDFShapeCmd.STROKE | clip);
                        clip = 0;
                        path = new Path();
                        break;
                    case 's':
                        // close and stroke the path
                        path.close();
                        cmds.addPath(path, PDFShapeCmd.STROKE | clip);
                        clip = 0;
                        path = new Path();
                        break;
                    case 'f':
                    // the fall-through is intended!
                    case 'F':
                        // fill the path (close/not close identical)
                        cmds.addPath(path, PDFShapeCmd.FILL | clip);
                        clip = 0;
                        path = new Path();
                        break;
                    case 'f' + ('*' << 8):
                        // fill the path using even/odd rule
                        path.setFillType(FillType.EVEN_ODD);
                        cmds.addPath(path, PDFShapeCmd.FILL | clip);
                        clip = 0;
                        path = new Path();
                        break;
                    case 'B':
                        // fill and stroke the path
                        cmds.addPath(path, PDFShapeCmd.BOTH | clip);
                        clip = 0;
                        path = new Path();
                        break;
                    case 'B' + ('*' << 8):
                        // fill path using even/odd rule and stroke it
                        path.setFillType(FillType.EVEN_ODD);
                        cmds.addPath(path, PDFShapeCmd.BOTH | clip);
                        clip = 0;
                        path = new Path();
                        break;
                    case 'b':
                        // close the path, then fill and stroke it
                        path.close();
                        cmds.addPath(path, PDFShapeCmd.BOTH | clip);
                        clip = 0;
                        path = new Path();
                        break;
                    case 'b' + ('*' << 8):
                        // close path, fill using even/odd rule, then stroke it
                        path.close();
                        path.setFillType(FillType.EVEN_ODD);
                        cmds.addPath(path, PDFShapeCmd.BOTH | clip);
                        clip = 0;
                        path = new Path();
                        break;
                    case 'n':
                        // clip with the path and discard it
                        if (clip != 0) {
                            cmds.addPath(path, clip);
                        }
                        clip = 0;
                        path = new Path();
                        break;
                    case 'W':
                        // mark this path for clipping!
                        clip = PDFShapeCmd.CLIP;
                        break;
                    case 'W' + ('*' << 8):
                        // mark this path using even/odd rule for clipping
                        path.setFillType(FillType.EVEN_ODD);
                        clip = PDFShapeCmd.CLIP;
                        break;
                    case 's' + ('h' << 8): {
                        // shade a region that is defined by the shader itself.
                        // shading the current space from a dictionary
                        // should only be used for limited-dimension shadings
                        String gdictname = popString();
                        // set up the pen to do a gradient fill according
                        // to the dictionary
                        PDFObject shobj = findResource(gdictname, "Shading");
                        doShader(shobj);
                        break;
                    }
                    case 'C' + ('S' << 8):
                        // TODO [FHe]: ignoring color space
                        // set the stroke color space
                        state.strokeCS = parseColorSpace(new PDFObject(stack.pop()));
                        break;
                    case 'c' + ('s' << 8):
                        // TODO [FHe]: ignoring color space
                        // set the fill color space
                        state.fillCS = parseColorSpace(new PDFObject(stack.pop()));
                        break;
                    case 'S' + ('C' << 8): {
                        // TODO [FHe]: stroke color
                        // set the stroke color
                        int n = state.strokeCS.getNumComponents();
                        cmds.addStrokePaint(state.strokeCS.getPaint(popFloat(n)));
                        break;
                    }
                    case 'S' + ('C' << 8) + ('N' << 16): {
                        // TODO [FHe]: stroke pattern
//                if (state.strokeCS instanceof PatternSpace) {
//                    cmds.addFillPaint(doPattern((PatternSpace) state.strokeCS));
//                } else {
//                    int n = state.strokeCS.getNumComponents();
//                    cmds.addStrokePaint(state.strokeCS.getPaint(popFloat(n)));
//                }
                        int n = state.strokeCS.getNumComponents();
                        cmds.addStrokePaint(state.strokeCS.getPaint(popFloat(n)));
                        break;
                    }
                    case 's' + ('c' << 8): {
                        // TODO [FHe]: stroke color
                        // set the fill color
                        int n = state.fillCS.getNumComponents();
                        cmds.addFillPaint(state.fillCS.getFillPaint(popFloat(n)));
                        break;
                    }
                    case 's' + ('c' << 8) + ('n' << 16): {
                        // TODO [FHe]: stroke pattern
//                if (state.fillCS instanceof PatternSpace) {
//                    cmds.addFillPaint(doPattern((PatternSpace) state.fillCS));
//                } else {
//                    int n = state.fillCS.getNumComponents();
//                    cmds.addFillPaint(state.fillCS.getPaint(popFloat(n)));
//                }
                        int n = state.fillCS.getNumComponents();
                        cmds.addFillPaint(state.fillCS.getFillPaint(popFloat(n)));
                        break;
                    }
                    case 'G':
                        // set the stroke color to a Gray value
                        state.strokeCS = PDFColorSpace.getColorSpace(PDFColorSpace.COLORSPACE_GRAY);
                        cmds.addStrokePaint(state.strokeCS.getPaint(popFloat(1)));
                        break;
                    case 'g':
                        // set the fill color to a Gray value
                        state.fillCS = PDFColorSpace.getColorSpace(PDFColorSpace.COLORSPACE_GRAY);
                        cmds.addFillPaint(state.fillCS.getFillPaint(popFloat(1)));
                        break;
                    case 'R' + ('G' << 8):
                        // set the stroke color to an RGB value
                        state.strokeCS = PDFColorSpace.getColorSpace(PDFColorSpace.COLORSPACE_RGB);
                        cmds.addStrokePaint(state.strokeCS.getPaint(popFloat(3)));
                        break;
                    case 'r' + ('g' << 8):
                        // set the fill color to an RGB value
                        state.fillCS = PDFColorSpace.getColorSpace(PDFColorSpace.COLORSPACE_RGB);
                        cmds.addFillPaint(state.fillCS.getFillPaint(popFloat(3)));
                        break;
                    case 'K':
                        // set the stroke color to a CMYK value
                        state.strokeCS =
                                PDFColorSpace.getColorSpace(PDFColorSpace.COLORSPACE_CMYK);
                        cmds.addStrokePaint(state.strokeCS.getPaint(popFloat(4)));
                        break;
                    case 'k':
                        // set the fill color to a CMYK value
                        state.fillCS =
                                PDFColorSpace.getColorSpace(PDFColorSpace.COLORSPACE_CMYK);
                        cmds.addFillPaint(state.fillCS.getFillPaint(popFloat(4)));
                        break;
                    case 'D' + ('o' << 8): {
                        // make a do call on the referenced object
                        PDFObject xobj = findResource(popString(), "XObject");
                        doXObject(xobj);
                        break;
                    }
                    case 'B' + ('T' << 8):
                        processBTCmd();
                        break;
                    case 'E' + ('T' << 8):
                        // end of text.  noop
                        state.textFormat.end();
                        break;
                    case 'T' + ('c' << 8):
                        // set character spacing
                        state.textFormat.setCharSpacing(popFloat());
                        break;
                    case 'T' + ('w' << 8):
                        // set word spacing
                        state.textFormat.setWordSpacing(popFloat());
                        break;
                    case 'T' + ('z' << 8):
                        // set horizontal scaling
                        state.textFormat.setHorizontalScale(popFloat());
                        break;
                    case 'T' + ('L' << 8):
                        // set leading
                        state.textFormat.setLeading(popFloat());
                        break;
                    case 'T' + ('f' << 8): {
                        // set text font
                        float sz = popFloat();
                        String fontref = popString();
                        state.textFormat.setFont(getFontFrom(fontref), sz);
                        break;
                    }
                    case 'T' + ('r' << 8):
                        // set text rendering mode
                        state.textFormat.setMode(popInt());
                        break;
                    case 'T' + ('s' << 8):
                        // set text rise
                        state.textFormat.setRise(popFloat());
                        break;
                    case 'T' + ('d' << 8): {
                        // set text matrix location
                        float y = popFloat();
                        float x = popFloat();
                        state.textFormat.carriageReturn(x, y);
                        break;
                    }
                    case 'T' + ('D' << 8): {
                        // set leading and matrix:  -y TL x y Td
                        float y = popFloat();
                        float x = popFloat();
                        state.textFormat.setLeading(-y);
                        state.textFormat.carriageReturn(x, y);
                        break;
                    }
                    case 'T' + ('m' << 8):
                        // set text matrix
                        state.textFormat.setMatrix(popFloat(6));
                        break;
                    case 'T' + ('*' << 8):
                        // go to next line
                        state.textFormat.carriageReturn();
                        break;
                    case 'T' + ('j' << 8):
                        // show text
                        state.textFormat.doText(cmds, popString());
                        break;
                    case '\'':
                        // next line and show text:  T* string Tj
                        state.textFormat.carriageReturn();
                        state.textFormat.doText(cmds, popString());
                        break;
                    case '\"': {
                        // draw string on new line with char & word spacing:
                        // aw Tw ac Tc string '
                        String string = popString();
                        float ac = popFloat();
                        float aw = popFloat();
                        state.textFormat.setWordSpacing(aw);
                        state.textFormat.setCharSpacing(ac);
                        state.textFormat.doText(cmds, string);
                        break;
                    }
                    case 'T' + ('J' << 8):
                        // show kerned string
                        state.textFormat.doText(cmds, popArray());
                        break;
                    case 'B' + ('I' << 8):
                        // parse inline image
                        parseInlineImage();
                        break;
                    case 'B' + ('X' << 8):
                        catchexceptions = true;     // ignore errors
                        break;
                    case 'E' + ('X' << 8):
                        catchexceptions = false;    // stop ignoring errors
                        break;
                    case 'M' + ('P' << 8):
                        // mark point (role= mark role name)
                        popString();
                        break;
                    case 'D' + ('P' << 8): {
                        // mark point with dictionary (role, ref)
                        // ref is either inline dict or name in "Properties" rsrc
                        Object ref = stack.pop();
                        popString();
                        break;
                    }
                    case 'B' + ('M' << 8) + ('C' << 16):
                        // begin marked content (role)
                        popString();
                        break;
                    case 'B' + ('D' << 8) + ('C' << 16): {
                        // begin marked content with dict (role, ref)
                        // ref is either inline dict or name in "Properties" rsrc
                        Object ref = stack.pop();
                        popString();
                        break;
                    }
                    case 'E' + ('M' << 8) + ('C' << 16):
                        // end marked content
                        break;
                    case 'd' + ('0' << 8):
                        // character width in type3 fonts
                        popFloat(2);
                        break;
                    case 'd' + ('1' << 8):
                        // character width in type3 fonts
                        popFloat(6);
                        break;
                    case 'Q' + ('B' << 8) + ('T' << 16):
                        processQCmd();
                        processBTCmd();
                        break;
                    case 'Q' + ('q' << 8):
                        processQCmd();
                    	// 'q'-cmd
	                    //   push the parser state
	                    parserStates.push((ParserState) state.clone());
	                    //   push graphics state
	                    cmds.addPush();
                        break;
                    default:
                        if (catchexceptions) {
                            if (!RELEASE) {
                                debug("**** WARNING: Unknown command: " + cmd + " **************************", 10);
                            }
                        } else {
                            throw new PDFParseException("Unknown command: " + cmd);
                        }
                }
            } catch (Exception e) {
                Log.e(TAG, "cmd='" + cmd + ":" + e.getMessage(), e);
            }
            if (stack.size() != 0) {
                if (!RELEASE) {
                    debug("**** WARNING! Stack not zero! (cmd=" + cmd + ", size=" + stack.size() + ") *************************", 4);
                }
                stack.setSize(0);
            }
        } else {
            stack.push(obj);
        }

        // release or reference to the page object, so that it can be
        // gc'd if it is no longer in use
        cmds = null;

        return Watchable.RUNNING;
    }

    /**
     * abstracted command processing for Q command. Used directly and as
     * part of processing of mushed QBT command.
     */
    private void processQCmd() {
        // pop graphics state ('Q')
        cmds.addPop();
        // pop the parser state
        state = (ParserState) parserStates.pop();
    }

    /**
     * abstracted command processing for BT command. Used directly and as
     * part of processing of mushed QBT command.
     */
    private void processBTCmd() {
        // begin text block:  reset everything.
        state.textFormat.reset();
    }

    /**
     * Cleanup when iteration is done
     */
    @Override
    public void cleanup() {
        if (state != null && state.textFormat != null) {
            state.textFormat.flush();
        }
        if (cmds != null) {
            cmds.finish();
        }

        stack = null;
        parserStates = null;
        state = null;
        path = null;
        cmds = null;
    }
    boolean errorwritten = false;

    public void dumpStreamToError() {
        if (errorwritten) {
            return;
        }
        errorwritten = true;
        try {
            File oops = File.createTempFile("PDFError", ".err");
            FileOutputStream fos = new FileOutputStream(oops);
            fos.write(stream);
            fos.close();
        } catch (IOException ioe) { /* Do nothing */ }
    }

    public String dumpStream() {
        return escape(new String(stream).replace('\r', '\n'));
    }

    /**
     * take a byte array and write a temporary file with it's data.
     * This is intended to capture data for analysis, like after decoders.
     *
     * @param ary
     * @param name
     */
    public static void emitDataFile(byte[] ary, String name) {
        FileOutputStream ostr;

        try {
            File file = File.createTempFile("DateFile", name);
            ostr = new FileOutputStream(file);
            System.out.println("Write: " + file.getPath());
            ostr.write(ary);
            ostr.close();
        } catch (IOException ex) {
            // ignore
        }
    }

    /////////////////////////////////////////////////////////////////
    //  H E L P E R S
    /////////////////////////////////////////////////////////////////
    /**
     * get a property from a named dictionary in the resources of this
     * content stream.
     * @param name the name of the property in the dictionary
     * @param inDict the name of the dictionary in the resources
     * @return the value of the property in the dictionary
     */
    private PDFObject findResource(String name, String inDict)
            throws IOException {
        if (inDict != null) {
            PDFObject in = resources.get(inDict);
            if (in == null || in.getType() != PDFObject.DICTIONARY) {
                throw new PDFParseException("No dictionary called " + inDict + " found in the resources");
            }
            return in.getDictRef(name);
        } else {
            return resources.get(name);
        }
    }

    /**
     * Insert a PDF object into the command stream.  The object must
     * either be an Image or a Form, which is a set of PDF commands
     * in a stream.
     * @param obj the object to insert, an Image or a Form.
     */
    private void doXObject(PDFObject obj) throws IOException {
        String type = obj.getDictRef("Subtype").getStringValue();
        if (type == null) {
            type = obj.getDictRef("S").getStringValue();
        }
        if (type.equals("Image")) {
            doImage(obj);
        } else if (type.equals("Form")) {
            doForm(obj);
        } else {
            throw new PDFParseException("Unknown XObject subtype: " + type);
        }
    }

    /**
     * Parse image data into a Java BufferedImage and add the image
     * command to the page.
     * @param obj contains the image data, and a dictionary describing
     * the width, height and color space of the image.
     */
    private void doImage(PDFObject obj) throws IOException {
        cmds.addImage(PDFImage.createImage(obj, resources));
    }

    /**
     * Inject a stream of PDF commands onto the page.  Optimized to cache
     * a parsed stream of commands, so that each Form object only needs
     * to be parsed once.
     * @param obj a stream containing the PDF commands, a transformation
     * matrix, bounding box, and resources.
     */
    private void doForm(PDFObject obj) throws IOException {
        // check to see if we've already parsed this sucker
        PDFPage formCmds = (PDFPage) obj.getCache();
        if (formCmds == null) {
            // rats.  parse it.
            Matrix at;
            RectF bbox;
            PDFObject matrix = obj.getDictRef("Matrix");
            if (matrix == null) {
                at = new Matrix();
            } else {
                float elts[] = new float[6];
                for (int i = 0; i < elts.length; i++) {
                    elts[i] = ((PDFObject) matrix.getAt(i)).getFloatValue();
                }
                at = new Matrix();
                Utils.setMatValues(at, elts);
            }
            PDFObject bobj = obj.getDictRef("BBox");
            bbox = new RectF(bobj.getAt(0).getFloatValue(),
                    bobj.getAt(1).getFloatValue(),
                    bobj.getAt(2).getFloatValue(),
                    bobj.getAt(3).getFloatValue());
            formCmds = new PDFPage(bbox, 0);
            formCmds.addXform(at);

            HashMap<String, PDFObject> r = new HashMap<String, PDFObject>(resources);
            PDFObject rsrc = obj.getDictRef("Resources");
            if (rsrc != null) {
                r.putAll(rsrc.getDictionary());
            }

            PDFParser form = new PDFParser(formCmds, obj.getStream(), r);
            form.go(true);

            obj.setCache(formCmds);
        }
        cmds.addPush();
        cmds.addCommands(formCmds);
        cmds.addPop();
    }

//    /**
//     * Set the values into a PatternSpace
//     */
//    private PDFPaint doPattern(PatternSpace patternSpace) throws IOException {
//        float[] components = null;
//
//        String patternName = popString();
//        PDFObject pattern = findResource(patternName, "Pattern");
//
//        if (pattern == null) {
//            throw new PDFParseException("Unknown pattern : " + patternName);
//        }
//
//        if (stack.size() > 0) {
//            components = popFloat(stack.size());
//        }
//
//        return patternSpace.getPaint(pattern, components, resources);
//    }
    /**
     * Parse the next object out of the PDF stream.  This could be a
     * Double, a String, a HashMap (dictionary), Object[] array, or
     * a Tok containing a PDF command.
     */
    private Object parseObject() throws PDFParseException {
        final Tok t = nextToken();

        switch (t.type) {
            case Tok.NUM:
                return new Double(tok.value);
            case Tok.STR:
            // the fall-through is intended!
            case Tok.NAME:
                return tok.name;
            case Tok.BRKB: {
                final HashMap<String, PDFObject> hm = new HashMap<String, PDFObject>();
                String name = null;
                for (Object obj = null; (obj = parseObject()) != null;) {
                    if (name == null) {
                        name = (String) obj;
                    } else {
                        hm.put(name, new PDFObject(obj));
                        name = null;
                    }
                }
                if (tok.type != Tok.BRKE) {
                    throw new PDFParseException("Inline dict should have ended with '>>'");
                }
                return hm;
            }
            case Tok.ARYB: {
                // build an array
                final ArrayList<Object> ary = new ArrayList<Object>();
                for (Object obj = null; (obj = parseObject()) != null;) {
                    ary.add(obj);
                }
                if (tok.type != Tok.ARYE) {
                    throw new PDFParseException("Expected ']'");
                }
                return ary.toArray();
            }
            case Tok.CMD:
                return t;
        }
        if (!RELEASE) {
            debug("**** WARNING! parseObject unknown token! (t.type=" + t.type + ") *************************", 4);
        }
        return null;
    }

    /**
     * Parse an inline image.  An inline image starts with BI (already
     * read, contains a dictionary until ID, and then image data until
     * EI.
     */
    private void parseInlineImage() throws IOException {
        // build dictionary until ID, then read image until EI
        HashMap<String, PDFObject> hm = new HashMap<String, PDFObject>();
        while (true) {
            Tok t = nextToken();
            if (t.type == Tok.CMD && t.name.equals("ID")) {
                break;
            }
            // it should be a name;
            String name = t.name;
            if (!RELEASE) {
                debug("ParseInlineImage, token: " + name, 1000);
            }
            if (name.equals("BPC")) {
                name = "BitsPerComponent";
            } else if (name.equals("CS")) {
                name = "ColorSpace";
            } else if (name.equals("D")) {
                name = "Decode";
            } else if (name.equals("DP")) {
                name = "DecodeParms";
            } else if (name.equals("F")) {
                name = "Filter";
            } else if (name.equals("H")) {
                name = "Height";
            } else if (name.equals("IM")) {
                name = "ImageMask";
            } else if (name.equals("W")) {
                name = "Width";
            } else if (name.equals("I")) {
                name = "Interpolate";
            }
            Object vobj = parseObject();
            hm.put(name, new PDFObject(vobj));
        }
        if (stream[loc] == '\r') {
            loc++;
        }
        if (stream[loc] == '\n' || stream[loc] == ' ') {
            loc++;
        }


        PDFObject imObj = (PDFObject) hm.get("ImageMask");
        if (imObj != null && imObj.getBooleanValue()) {
            // [PATCHED by michal.busta@gmail.com] - default value according to PDF spec. is [0, 1]
            // there is no need to swap array - PDF image should handle this values
            Double[] decode = {new Double(0), new Double(1)};

            PDFObject decodeObj = (PDFObject) hm.get("Decode");
            if (decodeObj != null) {
                decode[0] = new Double(decodeObj.getAt(0).getDoubleValue());
                decode[1] = new Double(decodeObj.getAt(1).getDoubleValue());
            }

            hm.put("Decode", new PDFObject(decode));
        }

        PDFObject obj = new PDFObject(null, PDFObject.DICTIONARY, hm);
        int dstart = loc;

        // now skip data until a whitespace followed by EI
        while (!PDFFile.isWhiteSpace(stream[loc]) ||
                stream[loc + 1] != 'E' ||
                stream[loc + 2] != 'I') {
            loc++;
        }

        // data runs from dstart to loc
        byte[] data = new byte[loc - dstart];
        System.arraycopy(stream, dstart, data, 0, loc - dstart);
        obj.setStream(ByteBuffer.wrap(data));
        loc += 3;
        doImage(obj);
    }

    /**
     * build a shader from a dictionary.
     */
    private void doShader(PDFObject shaderObj) throws IOException {
        // TODO [FHe]: shader
//        PDFShader shader = PDFShader.getShader(shaderObj, resources);
//
//        cmds.addPush();
//
//        RectF bbox = shader.getBBox();
//        if (bbox != null) {
//            cmds.addFillPaint(shader.getPaint());
//            Path tmp = new Path();
//            tmp.addRect(bbox, Direction.CW);
//            cmds.addPath(tmp, PDFShapeCmd.FILL);
//        }
//
//        cmds.addPop();
    }

    /**
     * get a PDFFont from the resources, given the resource name of the
     * font.
     *
     * @param fontref the resource key for the font
     */
    private PDFFont getFontFrom(String fontref) throws IOException {
        PDFObject obj = findResource(fontref, "Font");
        return PDFFont.getFont(obj, resources);
    }

    /**
     * add graphics state commands contained within a dictionary.
     * @param name the resource name of the graphics state dictionary
     */
    private void setGSState(String name) throws IOException {
        // obj must be a string that is a key to the "ExtGState" dict
        PDFObject gsobj = findResource(name, "ExtGState");
        // get LW, LC, LJ, Font, SM, CA, ML, D, RI, FL, BM, ca
        // out of the reference, which is a dictionary
        PDFObject d;
        if ((d = gsobj.getDictRef("LW")) != null) {
            cmds.addStrokeWidth(d.getFloatValue());
        }
        if ((d = gsobj.getDictRef("LC")) != null) {
            cmds.addEndCap(d.getIntValue());
        }
        if ((d = gsobj.getDictRef("LJ")) != null) {
            cmds.addLineJoin(d.getIntValue());
        }
        if ((d = gsobj.getDictRef("Font")) != null) {
            state.textFormat.setFont(getFontFrom(d.getAt(0).getStringValue()),
                    d.getAt(1).getFloatValue());
        }
        if ((d = gsobj.getDictRef("ML")) != null) {
            cmds.addMiterLimit(d.getFloatValue());
        }
        if ((d = gsobj.getDictRef("D")) != null) {
            PDFObject pdash[] = d.getAt(0).getArray();
            float dash[] = new float[pdash.length];
            for (int i = 0; i < pdash.length; i++) {
                dash[i] = pdash[i].getFloatValue();
            }
            cmds.addDash(dash, d.getAt(1).getFloatValue());
        }
        if ((d = gsobj.getDictRef("CA")) != null) {
            cmds.addStrokeAlpha(d.getFloatValue());
        }
        if ((d = gsobj.getDictRef("ca")) != null) {
            cmds.addFillAlpha(d.getFloatValue());
        }
        // others: BM=blend mode
    }

    /**
     * generate a PDFColorSpace description based on a PDFObject.  The
     * object could be a standard name, or the name of a resource in
     * the ColorSpace dictionary, or a color space name with a defining
     * dictionary or stream.
     */
    private PDFColorSpace parseColorSpace(PDFObject csobj) throws IOException {
        if (csobj == null) {
            return state.fillCS;
        }

        return PDFColorSpace.getColorSpace(csobj, resources);
    }

    /**
     * pop a single float value off the stack.
     * @return the float value of the top of the stack
     * @throws PDFParseException if the value on the top of the stack
     * isn't a number
     */
    private float popFloat() throws PDFParseException {
        Object obj = stack.pop();
        if (obj instanceof Double) {
            return ((Double) obj).floatValue();
        } else {
            throw new PDFParseException("Expected a number here.");
        }
    }

    /**
     * pop an array of float values off the stack.  This is equivalent
     * to filling an array from end to front by popping values off the
     * stack.
     * @param count the number of numbers to pop off the stack
     * @return an array of length <tt>count</tt>
     * @throws PDFParseException if any of the values popped off the
     * stack are not numbers.
     */
    private float[] popFloat(int count) throws PDFParseException {
        float[] ary = new float[count];
        for (int i = count - 1; i >= 0; i--) {
            ary[i] = popFloat();
        }
        return ary;
    }

    /**
     * pop a single integer value off the stack.
     * @return the integer value of the top of the stack
     * @throws PDFParseException if the top of the stack isn't a number.
     */
    private int popInt() throws PDFParseException {
        Object obj = stack.pop();
        if (obj instanceof Double) {
            return ((Double) obj).intValue();
        } else {
            throw new PDFParseException("Expected a number here.");
        }
    }

    /**
     * pop an array of integer values off the stack.  This is equivalent
     * to filling an array from end to front by popping values off the
     * stack.
     * @param count the number of numbers to pop off the stack
     * @return an array of length <tt>count</tt>
     * @throws PDFParseException if any of the values popped off the
     * stack are not numbers.
     */
    private float[] popFloatArray() throws PDFParseException {
        Object obj = stack.pop();
        if (!(obj instanceof Object[])) {
            throw new PDFParseException("Expected an [array] here.");
        }
        Object[] source = (Object[]) obj;
        float[] ary = new float[source.length];
        for (int i = 0; i < ary.length; i++) {
            if (source[i] instanceof Double) {
                ary[i] = ((Double) source[i]).floatValue();
            } else {
                throw new PDFParseException("This array doesn't consist only of floats.");
            }
        }
        return ary;
    }

    /**
     * pop a String off the stack.
     * @return the String from the top of the stack
     * @throws PDFParseException if the top of the stack is not a NAME
     * or STR.
     */
    private String popString() throws PDFParseException {
        Object obj = stack.pop();
        if (!(obj instanceof String)) {
            throw new PDFParseException("Expected string here: " + obj.toString());
        } else {
            return (String) obj;
        }
    }

    /**
     * pop a PDFObject off the stack.
     * @return the PDFObject from the top of the stack
     * @throws PDFParseException if the top of the stack does not contain
     * a PDFObject.
     */
    private PDFObject popObject() throws PDFParseException {
        Object obj = stack.pop();
        if (!(obj instanceof PDFObject)) {
            throw new PDFParseException("Expected a reference here: " + obj.toString());
        }
        return (PDFObject) obj;
    }

    /**
     * pop an array off the stack
     * @return the array of objects that is the top element of the stack
     * @throws PDFParseException if the top element of the stack does not
     * contain an array.
     */
    private Object[] popArray() throws PDFParseException {
        Object obj = stack.pop();
        if (!(obj instanceof Object[])) {
            throw new PDFParseException("Expected an [array] here: " + obj.toString());
        }
        return (Object[]) obj;
    }

    /**
     * A class to store state needed whiel rendering.  This includes the
     * stroke and fill color spaces, as well as the text formatting
     * parameters.
     */
    class ParserState implements Cloneable {

        /** the fill color space */
        PDFColorSpace fillCS;
        /** the stroke color space */
        PDFColorSpace strokeCS;
        /** the text paramters */
        PDFTextFormat textFormat;

        /**
         * Clone the render state.
         */
        @Override
        public Object clone() {
            ParserState newState = new ParserState();

            // no need to clone color spaces, since they are immutable
            // TODO: uncommented following 2 lines (mutable?)
            newState.fillCS = fillCS;
            newState.strokeCS = strokeCS;

            // we do need to clone the textFormat
            newState.textFormat = (PDFTextFormat) textFormat.clone();

            return newState;
        }
    }

    private String dump(Stack<Object> stk) {
        if (stk == null) {
            return "<null>";
        }
        if (stk.size() == 0) {
            return "[]";
        }
        String result = "";
        String delimiter = "[";
        for (Object obj : stk) {
            result += delimiter + dumpObj(obj);
            delimiter = ",";
        }
        result += "]";
        return result;
    }

    private String dumpObj(Object obj) {
        if (obj == null) {
            return "<null>";
        }
        if (obj instanceof Object[]) {
            return dumpArray((Object[]) obj);
        }
        return obj.toString();
    }

    private String dumpArray(Object[] objs) {
        if (objs == null) {
            return "<null>";
        }
        if (objs.length == 0) {
            return "[]";
        }
        String result = "";
        String delimiter = "[";
        for (Object obj : objs) {
            result += delimiter + dumpObj(obj);
            delimiter = ",";
        }
        result += "]";
        return result;
    }
}
