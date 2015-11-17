package com.wyx.pdfviewsample;

import android.app.Activity;
import android.os.Bundle;

import net.sf.andpdf.pdfviewer.gui.PdfView;
import net.sf.andpdf.utils.FileUtils;

import java.io.IOException;

public class Main2Activity extends Activity {

    PdfView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        pdfView = (PdfView) findViewById(R.id.pdf_view);

        try {
            pdfView.parsePDF(FileUtils.fileFromAsset(this, "about.pdf"), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        pdfView.startRenderThread(1, 1.0f);


    }
}
