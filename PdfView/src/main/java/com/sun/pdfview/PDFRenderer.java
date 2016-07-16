/*
 * $Id: PDFRenderer.java,v 1.8 2009/02/12 13:53:56 tomoke Exp $
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

import net.sf.andpdf.refs.WeakReference;

import java.util.Stack;

import net.sf.andpdf.utils.BiCa;
import net.sf.andpdf.utils.Utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Region.Op;
import android.util.Log;

/**
 * This class turns a set of PDF Commands from a PDF page into an image.  It
 * encapsulates the state of drawing in terms of stroke, fill, transform,
 * etc., as well as pushing and popping these states.
 *
 * When the run method is called, this class goes through all remaining commands
 * in the PDF Page and draws them to its buffered image.  It then updates any
 * ImageConsumers with the drawn data.
 *
 * @author ?
 * @author Ferenc Hechler (ferenc@hechler.de)
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public class PDFRenderer extends BaseWatchable implements Runnable {

    private static final String TAG = "APV.PDFRenderer";
    private int cmdCnt;
    /** the page we were generate from */
    private PDFPage page;
    /** where we are in the page's command list */
    private int currentCommand;
    /** a weak reference to the image we render into.  For the image
     * to remain available, some other code must retain a strong reference to it.
     */
    private WeakReference<BiCa> imageRef;
    /** the graphics object for use within an iteration.  Note this must be
     * set to null at the end of each iteration, or the image will not be
     * collected
     */
    private Canvas g;
    /** the current graphics state */
    private GraphicsState state;
    /** the stack of push()ed graphics states */
    private Stack<GraphicsState> stack;
    /** the total region of this image that has been written to */
    private RectF globalDirtyRegion;
    /** the last shape we drew (to check for overlaps) */
    private Path lastShape;
    /** the info about the image, if we need to recreate it */
    private ImageInfo imageinfo;
    /** the next time the image should be notified about updates */
    private long then = 0;
    /** the sum of all the individual dirty regions since the last update */
    private RectF unupdatedRegion;
    /** how long (in milliseconds) to wait between image updates */
    public static final long UPDATE_DURATION = 200;
    public static final float NOPHASE = -1000;
    public static final float NOWIDTH = -1000;
    public static final float NOLIMIT = -1000;
    public static final Cap NOCAP = null;
    public static final float[] NODASH = null;
    public static final Join NOJOIN = null;

    /**
     * create a new PDFGraphics state
     * @param page the current page
     * @param imageinfo the paramters of the image to render
     */
    public PDFRenderer(PDFPage page, ImageInfo imageinfo, Bitmap bi) {
        super();

        this.page = page;
        this.imageinfo = imageinfo;
        this.imageRef = new WeakReference<BiCa>(new BiCa(bi, g));

//        // initialize the list of observers
//        observers = new ArrayList<ImageObserver>();
        this.cmdCnt = 0;
    }

    /**
     * create a new PDFGraphics state, given a Graphics2D. This version
     * will <b>not</b> create an image, and you will get a NullPointerException
     * if you attempt to call getImage().
     * @param page the current page
     * @param g the Graphics2D object to use for drawing
     * @param imgbounds the bounds of the image into which to fit the page
     * @param clip the portion of the page to draw, in page space, or null
     * if the whole page should be drawn
     * @param bgColor the color to draw the background of the image, or
     * null for no color (0 alpha value)
     */
    public PDFRenderer(PDFPage page, Canvas g, RectF imgbounds,
            RectF clip, int bgColor) {
        super();

        this.page = page;
        this.g = g;
        this.imageinfo = new ImageInfo((int) imgbounds.width(), (int) imgbounds.height(),
                clip, bgColor);
        g.translate(imgbounds.left, imgbounds.top);
//	System.out.println("Translating by "+imgbounds.x+","+imgbounds.y);

//        // initialize the list of observers
//        observers = new ArrayList<ImageObserver>();
        this.cmdCnt = 0;
    }

    /**
     * Set up the graphics transform to match the clip region
     * to the image size.
     */
    private void setupRendering(Canvas g) {
//        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                RenderingHints.VALUE_ANTIALIAS_ON);
//        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
//                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        Paint g_p = new Paint();
        g_p.setColor(imageinfo.bgColor);
        g.drawRect(0, 0, imageinfo.width, imageinfo.height, g_p);

        g_p.setColor(Color.BLACK);

        // set the initial clip and transform on the graphics
        Matrix mat = getInitialTransform();
        g.setMatrix(mat);

        // set up the initial graphics state
        state = new GraphicsState();
        state.cliprgn = null;
        state.strokePaint = PDFPaint.getColorPaint(Color.BLACK);
        state.fillPaint = PDFPaint.getPaint(Color.BLACK);
        state.xform = g.getMatrix();

        // initialize the stack
        stack = new Stack<GraphicsState>();

        // initialize the current command
        currentCommand = 0;
    }

    /**
     * push the current graphics state onto the stack.  Continue working
     * with the current object; calling pop() restores the state of this
     * object to its state when push() was called.
     */
    public void push() {
        state.cliprgn = g.getClipBounds();
        stack.push(state);

        state = (GraphicsState) state.clone();
    }

    /**
     * restore the state of this object to what it was when the previous
     * push() was called.
     */
    public void pop() {
        state = (GraphicsState) stack.pop();

        setTransform(state.xform);
        setClip(state.cliprgn);
    }

    /**
     * draw an outline using the current stroke and draw paint
     * @param s the path to stroke
     * @return a Rectangle2D to which the current region being
     * drawn will be added.  May also be null, in which case no dirty
     * region will be recorded.
     */
    public RectF stroke(Path s) {
//        g.setComposite(state.strokeAlpha);
//        s = new GeneralPath(autoAdjustStrokeWidth(g, state.stroke).createStrokedShape(s));
        return state.strokePaint.fill(this, g, s);
    }

//    /**
//     * auto adjust the stroke width, according to 6.5.4, which presumes that
//     * the device characteristics (an image) require a single pixel wide
//     * line, even if the width is set to less. We determine the scaling to
//     * see if we would produce a line that was too small, and if so, scale
//     * it up to produce a graphics line of 1 pixel, or so. This matches our
//     * output with Adobe Reader.
//     * 
//     * @param g
//     * @param bs
//     * @return
//     */
//    private BasicStroke autoAdjustStrokeWidth(Graphics2D g, BasicStroke bs) {
//        AffineTransform bt = new AffineTransform(g.getTransform());
//        float width = bs.getLineWidth() * (float) bt.getScaleX();
//        BasicStroke stroke = bs;
//        if (width < 1f) {
//            if (bt.getScaleX() > 0.01) {
//                width = 1.0f / (float) bt.getScaleX();
//                stroke = new BasicStroke(width,
//                        bs.getEndCap(),
//                        bs.getLineJoin(),
//                        bs.getMiterLimit(),
//                        bs.getDashArray(),
//                        bs.getDashPhase());
//            } else {
//                // prevent division by a really small number
//                width = 1.0f;
//            }
//        }
//        return stroke;
//    }
    /**
     * draw an outline.
     * @param p the path to draw
     * @param bs the stroke with which to draw the path
     */
    public void draw(Path p) {
        g.drawPath(p, state.fillPaint.getPaint());
        g.drawPath(p, state.strokePaint.getPaint());
    }

    /**
     * fill an outline using the current fill paint
     * @param s the path to fill
     */
    public RectF fill(Path s) {
        // g.setComposite(state.fillAlpha);
        return state.fillPaint.fill(this, g, s);
    }

    /**
     * draw native text
     * @param text
     * @param mat
     * @return
     */
	public RectF drawNativeText(String text, RectF bounds) {
		Paint paint = state.fillPaint.getPaint();
		g.save();
		Matrix m;
		Matrix mOrig = g.getMatrix();

//		Log.i(TAG, "orig");
//		m = new Matrix(mOrig);
//		showTrans(m, bounds);
//		g.setMatrix(m);
//		g.drawText(text, bounds.left, bounds.top, paint);
//
//		Log.i(TAG, "m.postScale(1.0f, -1.0f);");
//		m = new Matrix(mOrig);
//		m.postScale(1.0f, -1.0f);
//		showTrans(m, bounds);
//		g.setMatrix(m);
//		g.drawText(text, bounds.left, bounds.top, paint);
//		
//		Log.i(TAG, "m.postScale(1.0f, -1.0f, 10.0f, 10.0f);");
//		m = new Matrix(mOrig);
//		m.postScale(1.0f, -1.0f, 10.0f, 10.0f);
//		showTrans(m, bounds);
//		g.setMatrix(m);
//		g.drawText(text, bounds.left, bounds.top, paint);
//		
//		Log.i(TAG, "m.postScale(1.0f, -1.0f, bounds.left, bounds.top);");
//		m = new Matrix(mOrig);
//		m.postScale(1.0f, -1.0f, bounds.left, bounds.top);
//		showTrans(m, bounds);
//		g.setMatrix(m);
//		g.drawText(text, bounds.left, bounds.top, paint);
//		
//		Log.i(TAG, "m.postScale(1.0f, -1.0f, bounds.left, -bounds.top);");
//		m = new Matrix(mOrig);
//		m.postScale(1.0f, -1.0f, bounds.left, -bounds.top);
//		showTrans(m, bounds);
//		g.setMatrix(m);
//		g.drawText(text, bounds.left, bounds.top, paint);
//		
//		Log.i(TAG, "m.preScale(1.0f, -1.0f);");
//		m = new Matrix(mOrig);
//		m.preScale(1.0f, -1.0f);
//		showTrans(m, bounds);
//		g.setMatrix(m);
//		g.drawText(text, bounds.left, bounds.top, paint);
//		
//		Log.i(TAG, "m.preScale(1.0f, -1.0f, 10.0f, 10.0f);");
//		m = new Matrix(mOrig);
//		m.preScale(1.0f, -1.0f, 10.0f, 10.0f);
//		showTrans(m, bounds);
//		g.setMatrix(m);
//		g.drawText(text, bounds.left, bounds.top, paint);
//		
//		Log.i(TAG, "m.preScale(1.0f, -1.0f, bounds.left, bounds.top);");
		m = new Matrix(mOrig);
		m.preScale(1.0f, -1.0f, bounds.left, bounds.top);
//		showTrans(m, bounds);
		g.setMatrix(m);
		g.drawText(text, bounds.left, bounds.top, paint);
		
//		Log.i(TAG, "m.preScale(1.0f, -1.0f, bounds.left, -bounds.top);");
//		m = new Matrix(mOrig);
//		m.preScale(1.0f, -1.0f, bounds.left, -bounds.top);
//		showTrans(m, bounds);
//		g.setMatrix(m);
//		g.drawText(text, bounds.left, bounds.top, paint);
//		
		g.restore();
		return bounds;
	}
	
    private void showTrans(Matrix m, RectF src) {
    	RectF dst = new RectF();
    	m.mapRect(dst, src);
    	Log.i(TAG, "M="+m);
    	Log.i(TAG, "src="+src);
    	Log.i(TAG, "dst="+dst);
	}

	/**
     * draw native text
     * @param text
     * @param mat
     * @return
     */
	public RectF drawNativeText(String text, float x, float y) {
		Paint paint = state.fillPaint.getPaint();
		g.drawText(text, x, y, paint);
		return new RectF();
	}
	
    /**
     * draw an image.
     * @param image the image to draw
     */
    public RectF drawImage(PDFImage image) {
        Matrix mat = new Matrix();
        Utils.setMatValues(mat,
                1f / image.getWidth(), 0,
                0, -1f / image.getHeight(),
                0, 1);

        Bitmap bi = image.getImage();
        if (image.isImageMask()) {
            bi = getMaskedImage(bi);
        }

        /*
        javax.swing.JFrame frame = new javax.swing.JFrame("Original Image");
        frame.getContentPane().add(new javax.swing.JLabel(new javax.swing.ImageIcon(bi)));
        frame.pack();
        frame.show();
         */

        g.drawBitmap(bi, mat, null);

        // get the total transform that was executed
        Matrix matB = new Matrix(g.getMatrix());
        matB.preConcat(mat);

        float minx = 0;
        float miny = 0;

        float[] points = new float[]{
            minx, miny, minx + bi.getWidth(), miny + bi.getHeight()
        };
        matB.mapPoints(points, 0, points, 0, 2);

        return new RectF(points[0], points[1],
                points[2] - points[0],
                points[3] - points[1]);

    }

    /**
     * add the path to the current clip.  The new clip will be the intersection
     * of the old clip and given path.
     */
    public void clip(Path s) {
        g.clipPath(s);
    }

    /**
     * set the clip to be the given shape.  The current clip is not taken
     * into account.
     */
    private void setClip(Rect s) {
        state.cliprgn = s;
        g.clipRect(s, Op.REPLACE);
    }

    /**
     * get the current affinetransform
     */
    public Matrix getTransform() {
        return state.xform;
    }

    /**
     * concatenate the given transform with the current transform
     */
    public void transform(Matrix mat) {
        state.xform.preConcat(mat);
        g.setMatrix(state.xform);
    }

    /**
     * replace the current transform with the given one.
     */
    public void setTransform(Matrix mat) {
        state.xform = mat;
        g.setMatrix(state.xform);
    }

    /**
     * get the initial transform from page space to Java space
     */
    public Matrix getInitialTransform() {
        return page.getInitialTransform(imageinfo.width,
                imageinfo.height,
                imageinfo.clip);
    }

    /**
     * Set some or all aspects of the current stroke.
     * @param w the width of the stroke, or NOWIDTH to leave it unchanged
     * @param cap the end cap style, or NOCAP to leave it unchanged
     * @param join the join style, or NOJOIN to leave it unchanged
     * @param limit the miter limit, or NOLIMIT to leave it unchanged
     * @param phase the phase of the dash array, or NOPHASE to leave it
     * unchanged
     * @param ary the dash array, or null to leave it unchanged.  phase
     * and ary must both be valid, or phase must be NOPHASE while ary is null.
     */
    public void setStrokeParts(float w, Cap cap, Join join, float limit, float[] ary, float phase) {
        if (w == NOWIDTH) {
            w = state.lineWidth;
        }
        if (cap == NOCAP) {
            cap = state.cap;
        }
        if (join == NOJOIN) {
            join = state.join;
        }
        if (limit == NOLIMIT) {
            limit = state.miterLimit;
        }
        if (phase == NOPHASE) {
//            ary = state.stroke.getDashArray();
//            phase = state.stroke.getDashPhase();
        }
        if (ary != null && ary.length == 0) {
            ary = null;
        }
//        if (phase == NOPHASE) {
//            state.stroke = new BasicStroke(w, cap, join, limit);
//        } else {
//            state.stroke = new BasicStroke(w, cap, join, limit, ary, phase);
//        }
        state.lineWidth = w;
        state.cap = cap;
        state.join = join;
        state.miterLimit = limit;
    }

//    /**
//     * get the current stroke as a BasicStroke
//     */
//    public BasicStroke getStroke() {
//        return state.stroke;
//    }
//
//    /**
//     * set the current stroke as a BasicStroke
//     */
//    public void setStroke(BasicStroke bs) {
//        state.stroke = bs;
//    }
    /**
     * set the stroke color
     */
    public void setStrokePaint(PDFPaint paint) {
        state.strokePaint = paint;
    }

    /**
     * set the fill color
     */
    public void setFillPaint(PDFPaint paint) {
        state.fillPaint = paint;
    }

//    /**
//     * set the stroke alpha
//     */
//    public void setStrokeAlpha(float alpha) {
//        state.strokeAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
//                alpha);
//    }
//
//    /**
//     * set the stroke alpha
//     */
//    public void setFillAlpha(float alpha) {
//        state.fillAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
//                alpha);
//    }
//    /**
//     * Add an image observer
//     */
//    public void addObserver(ImageObserver observer) {
//        if (observer == null) {
//            return;
//        }
//
//        // update the new observer to the current state
//        Image i = (Image) imageRef.get();
//        if (rendererFinished()) {
//            // if we're finished, just send a finished notification, don't
//            // add to the list of observers
//            // System.out.println("Late notify");
//            observer.imageUpdate(i, ImageObserver.ALLBITS, 0, 0,
//                    imageinfo.width, imageinfo.height);
//            return;
//        } else {
//            // if we're not yet finished, add to the list of observers and
//            // notify of the current dirty region
//            synchronized (observers) {
//                observers.add(observer);
//            }
//
//            if (globalDirtyRegion != null) {
//                observer.imageUpdate(i, ImageObserver.SOMEBITS,
//                        (int) globalDirtyRegion.getMinX(),
//                        (int) globalDirtyRegion.getMinY(),
//                        (int) globalDirtyRegion.getWidth(),
//                        (int) globalDirtyRegion.getHeight());
//            }
//        }
//    }
//
//    /**
//     * Remove an image observer
//     */
//    public void removeObserver(ImageObserver observer) {
//        synchronized (observers) {
//            observers.remove(observer);
//        }
//    }
    /**
     * Set the last shape drawn
     */
    public void setLastShape(Path shape) {
        this.lastShape = shape;
    }

    /**
     * Get the last shape drawn
     */
    public Path getLastShape() {
        return lastShape;
    }

    /**
     * Setup rendering.  Called before iteration begins
     */
    @Override
    public void setup() {
        Canvas graphics = null;

        if (imageRef != null) {
            BiCa bica = (BiCa) imageRef.get();
            if (bica != null) {
                graphics = bica.createCa();
            }
        } else {
            graphics = g;
        }


        if (graphics != null) {
            setupRendering(graphics);
        }
    }

    /**
     * Draws the next command in the PDFPage to the buffered image.
     * The image will be notified about changes no less than every
     * UPDATE_DURATION milliseconds.
     *
     * @return <ul><li>Watchable.RUNNING when there are commands to be processed
     *             <li>Watchable.NEEDS_DATA when there are no commands to be
     *                 processed, but the page is not yet complete
     *             <li>Watchable.COMPLETED when the page is done and all
     *                 the commands have been processed
     *             <li>Watchable.STOPPED if the image we are rendering into
     *                 has gone away
     *         </ul>
     */
    public int iterate() throws Exception {
        // make sure we have a page to render
        if (page == null) {
            return Watchable.COMPLETED;
        }

        // check if this renderer is based on a weak reference to a graphics
        // object.  If it is, and the graphics is no longer valid, then just quit
        if (imageRef != null) {
            final BiCa bica = imageRef.get();
            if (bica == null) {
                System.out.println("Image went away.  Stopping");
                return Watchable.STOPPED;
            }
            g = bica.createCa();
        }

        // check if there are any commands to parse.  If there aren't,
        // just return, but check if we'return really finished or not
        if (currentCommand >= page.getCommandCount()) {
            if (page.isFinished()) {
                return Watchable.COMPLETED;
            } else {
                return Watchable.NEEDS_DATA;
            }
        }

        // TODO [FHe]: display currentCommand / page.commandCount

        // find the current command
        final PDFCmd cmd = page.getCommand(currentCommand++);
        if (cmd == null) {
            // uh oh.  Synchronization problem!
            throw new PDFParseException("Command not found!");
        }

        if (!PDFParser.RELEASE) {
            cmdCnt += 1;
            Log.i(TAG, "CMD[" + cmdCnt + "]: " + cmd.toString() + ": " + cmd.getDetails());
        }
        RectF dirtyRegion = null;
        // execute the command
        try {
            dirtyRegion = cmd.execute(this);
        } catch (Exception e) {
            // TODO [FHe] remove if all commands are supported, now catch image not yet supp. excp.
            Log.e(TAG, e.getMessage(), e);
//	throw new PDFParseException(e.getMessage());
        }

        // append to the global dirty region
        globalDirtyRegion = addDirtyRegion(dirtyRegion, globalDirtyRegion);
        unupdatedRegion = addDirtyRegion(dirtyRegion, unupdatedRegion);

        final long now = System.currentTimeMillis();
        if (now > then || rendererFinished()) {
            // now tell any observers, so they can repaint
//            notifyObservers(bi, unupdatedRegion);
            unupdatedRegion = null;
            then = now + UPDATE_DURATION;
        }

        // if we are based on a reference to a graphics, don't hold on to it
        // since that will prevent the image from being collected.
        if (imageRef != null) {
            g = null;
        }

        // if we need to stop, it will be caught at the start of the next
        // iteration.
        return Watchable.RUNNING;
    }

    /**
     * Called when iteration has stopped
     */
    @Override
    public void cleanup() {
        page = null;
        state = null;
        stack = null;
        globalDirtyRegion = null;
        lastShape = null;

//        observers.clear();

        // keep around the image ref and image info for use in
        // late addObserver() call
    }

    /**
     * Append a rectangle to the total dirty region of this shape
     */
    private RectF addDirtyRegion(RectF region, RectF glob) {
        if (region == null) {
            return glob;
        } else if (glob == null) {
            return region;
        } else {
            glob.union(region);
            return glob;
        }
    }

    /**
     * Determine if we are finished
     */
    private boolean rendererFinished() {
        if (page == null) {
            return true;
        }

        return (page.isFinished() && currentCommand == page.getCommandCount());
    }

//    /**
//     * Notify the observer that a region of the image has changed
//     */
//    private void notifyObservers(BufferedImage bi, Rectangle2D region) {
//        if (bi == null) {
//            return;
//        }
//
//        int startx, starty, width, height;
//        int flags = 0;
//
//        // don't do anything if nothing is there or no one is listening
//        if ((region == null && !rendererFinished()) || observers == null ||
//                observers.size() == 0) {
//            return;
//        }
//
//        if (region != null) {
//            // get the image data for the total dirty region
//            startx = (int) Math.floor(region.getMinX());
//            starty = (int) Math.floor(region.getMinY());
//            width = (int) Math.ceil(region.getWidth());
//            height = (int) Math.ceil(region.getHeight());
//
//            // sometimes width or height is negative.  Grrr...
//            if (width < 0) {
//                startx += width;
//                width = -width;
//            }
//            if (height < 0) {
//                starty += height;
//                height = -height;
//            }
//
//            flags = 0;
//        } else {
//            startx = 0;
//            starty = 0;
//            width = imageinfo.width;
//            height = imageinfo.height;
//        }
//        if (rendererFinished()) {
//            flags |= ImageObserver.ALLBITS;
//            // forget about the Graphics -- allows the image to be
//            // garbage collected.
//            g = null;
//        } else {
//            flags |= ImageObserver.SOMEBITS;
//        }
//
//        synchronized (observers) {
//            for (Iterator i = observers.iterator(); i.hasNext();) {
//                ImageObserver observer = (ImageObserver) i.next();
//
//                boolean result = observer.imageUpdate(bi, flags,
//                        startx, starty,
//                        width, height);
//
//                // if result is false, the observer no longer wants to
//                // be notified of changes
//                if (!result) {
//                    i.remove();
//                }
//            }
//        }
//    }
    /**
     * Convert an image mask into an image by painting over any pixels
     * that have a value in the image with the current paint
     */
    private Bitmap getMaskedImage(Bitmap bi) {
        // get the color of the current paint
        int col = state.fillPaint.getPaint().getColor();

        // format as 8 bits each of ARGB
        int paintColor = Color.alpha(col) << 24;
        paintColor |= Color.red(col) << 16;
        paintColor |= Color.green(col) << 8;
        paintColor |= Color.blue(col);

        // transparent (alpha = 1)
        int noColor = 0;

        // get the coordinates of the source image
        int startX = 0;
        int startY = 0;
        int width = bi.getWidth();
        int height = bi.getHeight();

        // create a destion image of the same size
        Bitmap dstImage = Bitmap.createBitmap(width, height, Config.ARGB_8888);

        // copy the pixels row by row
        for (int i = 0; i < height; i++) {
            int[] srcPixels = new int[width];
            int[] dstPixels = new int[srcPixels.length];

            // read a row of pixels from the source
            bi.getPixels(srcPixels, 0, 0, startX, startY + i, width, 1);
//            bi.getRGB(startX, startY + i, width, 1, srcPixels, 0, height);

            // figure out which ones should get painted
            for (int j = 0; j < srcPixels.length; j++) {
                if (srcPixels[j] == 0xff000000) {
                    dstPixels[j] = paintColor;
                } else {
                    dstPixels[j] = noColor;
                }
            }

            // write the destination image
//            dstImage.setRGB(startX, startY + i, width, 1, dstPixels, 0, height);
            dstImage.setPixels(dstPixels, 0, 0, startX, startY + i, width, 1);
        }

        return dstImage;
    }

    class GraphicsState implements Cloneable {

        /** the clip region */
        Rect cliprgn;
        /** the current stroke */
        Cap cap;
        Join join;
        float lineWidth;
        float miterLimit;
        /** the current paint for drawing strokes */
        PDFPaint strokePaint;
        /** the current paint for filling shapes */
        PDFPaint fillPaint;
        /** the current transform */
        Matrix xform;

        /** Clone this Graphics state.
         *
         * Note that cliprgn is not cloned.  It must be set manually from
         * the current graphics object's clip
         */
        @Override
        public Object clone() {
            GraphicsState cState = new GraphicsState();
            cState.cliprgn = null;
            cState.cap = cap;
            cState.join = join;
            cState.strokePaint = strokePaint;
            cState.fillPaint = fillPaint;
            cState.xform = new Matrix(xform);
            cState.lineWidth = lineWidth;
            cState.miterLimit = miterLimit;
            return cState;
        }
    }

}
