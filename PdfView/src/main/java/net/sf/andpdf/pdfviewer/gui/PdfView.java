package net.sf.andpdf.pdfviewer.gui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.polites.android.GestureImageView;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.decrypt.PDFAuthenticationFailureException;
import com.sun.pdfview.decrypt.PDFPassword;

import net.sf.andpdf.nio.ByteBuffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class PdfView extends FullScrollView {

  private static final int STARTPAGE = 1;
  private static final float STARTZOOM = 1.0f;

  private static final float MIN_ZOOM = 0.25f;
  private static final float MAX_ZOOM = 3.0f;
  private static final float ZOOM_INCREMENT = 1.5f;

  private Bitmap mBi;
  public GestureImageView mImageView;
  private Handler uiHandler;
  ImageButton bZoomOut;
  ImageButton bZoomIn;
  private PDFFile mPdfFile;
  private PDFPage mPdfPage;
  private Thread backgroundThread;
  private int mPage;
  private float mZoom;

  public PdfView(Context context) {
    this(context, null);
  }

  public PdfView(Context context, AttributeSet attrs) {
    this(context, attrs, android.R.attr.scrollViewStyle);
  }

  public PdfView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    PDFImage.sShowImages = true;
    PDFPaint.s_doAntiAlias = true;
    uiHandler = new Handler();
    LayoutParams matchLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    mImageView = new GestureImageView(context);
    mImageView.setLayoutParams(matchLp);
    addView(mImageView);
    setLayoutParams(matchLp);
    setBackgroundColor(Color.LTGRAY);
    setHorizontalScrollBarEnabled(true);
    setHorizontalFadingEdgeEnabled(true);
    setVerticalScrollBarEnabled(true);
    setVerticalFadingEdgeEnabled(true);
  }

  public PDFFile getmPdfFile() {
    return mPdfFile;
  }

  public void setmPdfFile(PDFFile mPdfFile) {
    this.mPdfFile = mPdfFile;
  }

  private int getDeviceWidth() {
    DisplayMetrics metric = new DisplayMetrics();
    WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    wm.getDefaultDisplay().getMetrics(metric);
    return metric.widthPixels; // 屏幕宽度（像素）
  }

  private int getDeviceHeight() {
    DisplayMetrics metric = new DisplayMetrics();
    WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    wm.getDefaultDisplay().getMetrics(metric);
    return metric.heightPixels; // 屏幕高度（像素）
  }

  public void showPage(int page, float zoom) throws Exception {
    try {
      // free memory from previous page
      updateImage();
      // Only load the page if it's a different page (i.e. not just changing the zoom level)
      if (mPdfPage == null || mPdfPage.getPageNumber() != page) {
        mPdfPage = mPdfFile.getPage(page, true);
      }
      float width = mPdfPage.getWidth();
      float height = mPdfPage.getHeight();
      if (getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT) {
        height *= getDeviceWidth() / width;
      }
      if (getLayoutParams().width == LayoutParams.MATCH_PARENT) {
        width = getDeviceWidth();
      }

      RectF clip = null;
      Bitmap bi = mPdfPage.getImage((int) (width * zoom), (int) (height * zoom), clip, true, true);
      setPageBitmap(bi);
      updateImage();
    } catch (Throwable e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  private void updateImage() {
    uiHandler.post(new Runnable() {
      public void run() {
        mImageView.setImageBitmap(mBi);
      }
    });
  }

  private void setPageBitmap(Bitmap bi) {
    if (bi != null) {
      mBi = bi;
    }
  }

  private void zoomIn() {
    if (mPdfFile != null) {
      if (mZoom < MAX_ZOOM) {
        mZoom *= ZOOM_INCREMENT;
        if (mZoom > MAX_ZOOM) mZoom = MAX_ZOOM;

        if (mZoom >= MAX_ZOOM) {
          Log.d(TAG, "Disabling zoom in button");
          bZoomIn.setEnabled(false);
        } else {
          bZoomIn.setEnabled(true);
        }

        bZoomOut.setEnabled(true);
        startRenderThread(mPage, mZoom);
      }
    }
  }

  private void zoomOut() {
    if (mPdfFile != null) {
      if (mZoom > MIN_ZOOM) {
        mZoom /= ZOOM_INCREMENT;
        if (mZoom < MIN_ZOOM) mZoom = MIN_ZOOM;

        if (mZoom <= MIN_ZOOM) {
          Log.d(TAG, "Disabling zoom out button");
          bZoomOut.setEnabled(false);
        } else {
          bZoomOut.setEnabled(true);
        }

        bZoomIn.setEnabled(true);
        startRenderThread(mPage, mZoom);
      }
    }
  }

  private void nextPage() {
    if (mPdfFile != null) {
      if (mPage < mPdfFile.getNumPages()) {
        mPage += 1;
        bZoomOut.setEnabled(true);
        bZoomIn.setEnabled(true);
        startRenderThread(mPage, mZoom);
      }
    }
  }

  private void prevPage() {
    if (mPdfFile != null) {
      if (mPage > 1) {
        mPage -= 1;
        bZoomOut.setEnabled(true);
        bZoomIn.setEnabled(true);
        startRenderThread(mPage, mZoom);
      }
    }
  }

  private void gotoPage() {
    if (mPdfFile != null) {
      //            showDialog(DIALOG_PAGENUM);
    }
  }

  public synchronized void startRenderThread(final int page, final float zoom) {
    if (backgroundThread != null) return;
    backgroundThread = new Thread(new Runnable() {
      public void run() {
        try {
          if (mPdfFile != null) {
            showPage(page, zoom);
          }
        } catch (Exception e) {
          Log.e(TAG, e.getMessage(), e);
        }
        backgroundThread = null;
      }
    });
    updateImageStatus();
    backgroundThread.start();
  }

  private void updateImageStatus() {
    if (backgroundThread == null) {
      return;
    }
    postDelayed(new Runnable() {
      public void run() {
        updateImageStatus();
      }
    }, 1000);
  }

  public void parsePDF(File f, String password) throws PDFAuthenticationFailureException {
    try {
      long len = f.length();
      if (len == 0) {
        toastMessage("file '" + f.getName() + "' not found");
      } else {
        toastMessage("file '" + f.getName() + "' has " + len + " bytes");
        openFile(f, password);
      }
    } catch (PDFAuthenticationFailureException e) {
      throw e;
    } catch (Throwable e) {
      e.printStackTrace();
      toastMessage("Exception: " + e.getMessage());
    }
  }

  public void parsePDF(String filename, String password) throws PDFAuthenticationFailureException {
    try {
      File f = new File(filename);
      long len = f.length();
      if (len == 0) {
        toastMessage("file '" + filename + "' not found");
      } else {
        toastMessage("file '" + filename + "' has " + len + " bytes");
        openFile(f, password);
      }
    } catch (PDFAuthenticationFailureException e) {
      throw e;
    } catch (Throwable e) {
      e.printStackTrace();
      toastMessage("Exception: " + e.getMessage());
    }
  }

  public void openFile(File file, String password) throws IOException {
    // first open the file for random access
    RandomAccessFile raf = new RandomAccessFile(file, "r");
    // extract a file channel
    FileChannel channel = raf.getChannel();
    // now memory-map a byte-buffer
    ByteBuffer bb = ByteBuffer.NEW(channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()));
    // create a PDFFile from the data
    if (password == null) {
      mPdfFile = new PDFFile(bb);
    } else {
      mPdfFile = new PDFFile(bb, new PDFPassword(password));
    }
    toastMessage("Anzahl Seiten:" + mPdfFile.getNumPages());
  }

  public void toastMessage(String msg) {
    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
  }
}
