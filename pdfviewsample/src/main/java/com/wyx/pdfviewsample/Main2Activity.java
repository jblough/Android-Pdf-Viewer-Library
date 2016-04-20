package com.wyx.pdfviewsample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import java.io.IOException;
import net.sf.andpdf.pdfviewer.gui.PdfView;
import net.sf.andpdf.utils.FileUtils;

public class Main2Activity extends Activity {

  PdfView pdfView;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main2);

    pdfView = (PdfView) findViewById(R.id.pdf_view);

    //ViewGroup.LayoutParams params =
    //    new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    //GestureImageView view = pdfView.mImageView;
    //view.setImageResource(R.drawable.back02);
    //view.setLayoutParams(params);

    //ViewGroup layout = (ViewGroup) findViewById(R.id.layout);

    //layout.addView(view);
    View view = pdfView.mImageView;
  }

  @Override protected void onStart() {
    super.onStart();
    try {
      pdfView.parsePDF(FileUtils.fileFromAsset(this, "sample.pdf"), null);
    } catch (IOException e) {
      e.printStackTrace();
    }
    pdfView.startRenderThread(1, 1.0f);
  }
}
