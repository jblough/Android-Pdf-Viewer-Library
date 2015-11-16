package net.sf.andpdf.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * AWT: BufferedImage creates and stores Graphics2D object for reuse
 * Use BiCa to simulate this behaviour in PDFRenderer imageRef
 * @author ferenc.hechler
 */
public class BiCa {
	
	private Bitmap bi;
	private Canvas ca;
	
	public BiCa(Bitmap bi, Canvas ca) {
		this.bi = bi;
		this.ca = ca;
	}
	/**
	 * get stored Bitmap
	 * @return
	 */
	public Bitmap getBi() {
		return bi;
	}
	/**
	 * create new Canvas or reuse already created
	 * @return
	 */
	public Canvas createCa() {
		if (ca == null)
			ca = new Canvas(bi);
		return ca;
	}

}
