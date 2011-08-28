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

public abstract class Cipher {

	public static final int ENCRYPT_MODE = javax.crypto.Cipher.ENCRYPT_MODE;
	public static final int DECRYPT_MODE = javax.crypto.Cipher.DECRYPT_MODE;

	public static Cipher getInstance(String cipher) throws NoSuchAlgorithmException, NoSuchPaddingException {
		if (cipher.equals("RC4"))
			return new RC4Cipher();
		return new CryptoCipher(cipher);
	}

	public abstract void init(int mode, Key key);
	public abstract void init(int mode, SecretKey key);
	public abstract void init(int mode, SecretKeySpec keySpec);
	public abstract void init(int mode, SecretKeySpec keySpec, IvParameterSpec Iv);

	public abstract void doFinal(ByteBuffer nio, ByteBuffer decryptedBuf) throws IllegalBlockSizeException, ShortBufferException, BadPaddingException;
	public abstract byte[] doFinal(byte[] input) throws IllegalBlockSizeException, BadPaddingException;
	public abstract void doFinal(byte[] src, int from, int length, byte[] dest) throws IllegalBlockSizeException, ShortBufferException, BadPaddingException;




}