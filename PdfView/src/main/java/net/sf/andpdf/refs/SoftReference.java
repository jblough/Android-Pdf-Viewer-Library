package net.sf.andpdf.refs;

import java.util.ArrayList;

public class SoftReference<T> {

	java.lang.ref.SoftReference<T> softRef;
	HardReference<T> hardRef;
	
	public SoftReference(T o) {
		if (HardReference.sKeepCaches)
			hardRef = new HardReference<T>(o);
		else
			softRef = new java.lang.ref.SoftReference<T>(o);
	}

	public T get() {
		if (HardReference.sKeepCaches)
			return hardRef.get();
		else
			return softRef.get();
	}
	
}
