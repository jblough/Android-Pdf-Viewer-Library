package net.sf.andpdf.refs;

public class WeakReference<T> {

	java.lang.ref.WeakReference<T> weakRef;
	HardReference<T> hardRef;
	
	public WeakReference(T o) {
		if (HardReference.sKeepCaches)
			hardRef = new HardReference<T>(o);
		else
			weakRef = new java.lang.ref.WeakReference<T>(o);
	}

	public T get() {
		if (HardReference.sKeepCaches)
			return hardRef.get();
		else
			return weakRef.get();
	}
	
}
