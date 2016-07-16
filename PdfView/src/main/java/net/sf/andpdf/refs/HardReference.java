package net.sf.andpdf.refs;

import java.util.ArrayList;

public class HardReference<T> {

	public static boolean sKeepCaches = false;

	private static ArrayList<HardReference> cleanupList = new ArrayList<HardReference>();
	public static void cleanup() {
		ArrayList<HardReference> oldList = cleanupList;
		cleanupList = new ArrayList<HardReference>();
		for (HardReference hr:oldList) {
			hr.clean();
		}
		oldList.clear();
	}

	private T ref;
	
	
	public HardReference(T o) {
		ref = o;
		cleanupList.add(this);
	}

	public T get() {
		return ref;
	}

	public void clean() {
		ref = null;
	}

	
	
}
