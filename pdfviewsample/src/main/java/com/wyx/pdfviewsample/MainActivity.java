package com.wyx.pdfviewsample;

import java.io.IOException;
import net.sf.andpdf.pdfviewer.PdfViewerActivity;
import net.sf.andpdf.utils.FileUtils;

public class MainActivity extends PdfViewerActivity {

  @Override public String getFileName() {
    try {
      return FileUtils.fileFromAsset(this, "about.pdf").toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override public int getPreviousPageImageResource() {
    return R.drawable.left_arrow;
  }

  @Override public int getNextPageImageResource() {
    return R.drawable.right_arrow;
  }

  @Override public int getZoomInImageResource() {
    return R.drawable.zoom_in;
  }

  @Override public int getZoomOutImageResource() {
    return R.drawable.zoom_out;
  }

  @Override public int getPdfPasswordLayoutResource() {
    return 0;
  }

  @Override public int getPdfPageNumberResource() {
    return 0;
  }

  @Override public int getPdfPasswordEditField() {
    return 0;
  }

  @Override public int getPdfPasswordOkButton() {
    return 0;
  }

  @Override public int getPdfPasswordExitButton() {
    return 0;
  }

  @Override public int getPdfPageNumberEditField() {
    return 0;
  }
}

