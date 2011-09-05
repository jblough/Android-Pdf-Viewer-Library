This is a packaging of the project "Android PDF Viewer" (http://andpdf.sourceforge.net/) into a reusable library to make PDF viewing easier from within your Android application.

Consistent with the Android PDF Viewer project, the licensing for the PdfViewer project is LGPL

Quickstart incorporating a PDF viewing activity into your project:

1) Add PdfViewer.jar into your project's build path

2) Copy the following drawable resources from PdfViewer/res/drawable into YourProject/res/drawable
     left_arrow.png
     right_arrow.png
     zoom_in.png
     zoom_out.png

3) Copy the following layout resources from PdfViewer/res/layout into YourProject/res/layout
     dialog_pagenumber.xml
     pdf_file_password.xml

4) Derive your PDF activity from net.sf.andpdf.pdfviewer.PdfViewerActivity

5) Using the default drawables and layouts:
     public int getPreviousPageImageResource() { return R.drawable.left_arrow; }
     public int getNextPageImageResource() { return R.drawable.right_arrow; }
     public int getZoomInImageResource() { return R.drawable.zoom_in; }
     public int getZoomOutImageResource() { return R.drawable.zoom_out; }
     public int getPdfPasswordLayoutResource() { return R.layout.pdf_file_password; }
     public int getPdfPageNumberResource() { return R.layout.dialog_pagenumber; }
     public int getPdfPasswordEditField() { return R.id.etPassword; }
     public int getPdfPasswordOkButton() { return R.id.btOK; }
     public int getPdfPasswordExitButton() { return R.id.btExit; }
     public int getPdfPageNumberEditField() { return R.id.pagenum_edit; }

6) Invoke your PdfViewActivity derived with the following code:
     Intent intent = new Intent(this, YourPdfViewerActivity.class);
     intent.putExtra(PdfViewerActivity.EXTRA_PDFFILENAME, "PATH TO PDF GOES HERE");
     startActivity(intent);
    
