package net.sf.andpdf.crypto;

import java.nio.ByteBuffer;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoCipher extends Cipher {

	javax.crypto.Cipher cipher;
	
	public CryptoCipher(String ciphername) throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.cipher = javax.crypto.Cipher.getInstance(ciphername);
		
	}

	@Override
	public void doFinal(ByteBuffer nio, ByteBuffer decryptedBuf) throws IllegalBlockSizeException, ShortBufferException, BadPaddingException {
		cipher.doFinal(nio, decryptedBuf);
	}

	@Override
	public byte[] doFinal(byte[] input) throws IllegalBlockSizeException, BadPaddingException {
		return cipher.doFinal(input);
	}

	@Override
	public void doFinal(byte[] src, int from, int length, byte[] dest) throws IllegalBlockSizeException, ShortBufferException, BadPaddingException {
		cipher.doFinal(src, from, length, dest);
	}

	@Override
	public void init(int mode, Key key) {
		init(mode, key);
	}

	@Override
	public void init(int mode, SecretKey key) {
		init(mode, key);
	}

	@Override
	public void init(int mode, SecretKeySpec keySpec) {
		init(mode, keySpec);
	}

	@Override
	public void init(int mode, SecretKeySpec keySpec, IvParameterSpec Iv) {
		init(mode, keySpec, Iv);
	}

}
