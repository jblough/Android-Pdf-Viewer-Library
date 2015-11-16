package net.sf.andpdf.crypto;

import java.nio.ByteBuffer;
import java.security.Key;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

public class RC4Cipher extends Cipher {

	private RC4Engine rc4;
	
	@Override
	public void doFinal(ByteBuffer nio, ByteBuffer decryptedBuf) {
		while (nio.hasRemaining())
			decryptedBuf.put(rc4.returnByte(nio.get()));
	}

	@Override
	public byte[] doFinal(byte[] input) {
		byte[] result = new byte[input.length];
		rc4.processBytes(input, 0, input.length, result, 0);
		return result;
	}

	@Override
	public void doFinal(byte[] src, int from, int length, byte[] dest) {
		rc4.processBytes(src, from, length, dest, 0);
	}

	@Override
	public void init(int mode, Key key) {
		rc4 = new RC4Engine();
		rc4.init(mode == Cipher.ENCRYPT_MODE, new KeyParameter(key.getEncoded()));
	}

	@Override
	public void init(int mode, SecretKey key) {
		rc4 = new RC4Engine();
		rc4.init(mode == Cipher.ENCRYPT_MODE, new KeyParameter(key.getEncoded()));
	}

	@Override
	public void init(int mode, SecretKeySpec keySpec) {
		rc4 = new RC4Engine();
		rc4.init(mode == Cipher.ENCRYPT_MODE, new KeyParameter(keySpec.getEncoded()));
	}

	@Override
	public void init(int mode, SecretKeySpec keySpec, IvParameterSpec Iv) {
		throw new RuntimeException("not yet supported"); 
	}

}
