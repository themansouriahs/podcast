package org.bottiger.podcast.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;

import javax.annotation.Nonnegative;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

public class CryptoUtils {

	private static final String CIPHERS[] = {"AES/GCM/NoPadding", "AES/ECB/PKCS7Padding"};
	private static final String ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"; // http://developer.android.com/training/articles/keystore.html#UsingAndroidKeyStore
	
	public static String md5(String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	private String decryptSym(@NonNull String argCipherText, Key argKey) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
		Cipher cipher = getCipherSym();
		cipher.init(Cipher.DECRYPT_MODE, argKey);

		byte[] iv = cipher.getIV();
		byte[] data = toBytes(argCipherText);

		byte[] plainText = cipher.doFinal(data);

		return new String(plainText, StandardCharsets.UTF_8);
	}

	private String encryptSym(@NonNull String argPlainText, Key argKey) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
		Cipher cipher = getCipherSym();
		cipher.init(Cipher.ENCRYPT_MODE, argKey);

		byte[] iv = cipher.getIV();
		byte[] data = toBytes(argPlainText);

		byte[] cipherText = cipher.doFinal(data);

		return Base64.encodeToString(cipherText, Base64.URL_SAFE);
	}

	private String getSymKey(@NonNull String argAlias) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
		String secretKey = "testkey"; //String.valueOf(Hex.encodeHex(sk.getEncoded()));

		char[] keystoreKey = "sdfdsf".toCharArray();

		//Storing AES Secret key in keystore
		KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
		ks.load(null);

		boolean hasKey = ks.containsAlias(argAlias);

		if (hasKey) {
			try {
				ks.getKey(argAlias, keystoreKey);
			} catch (UnrecoverableKeyException e) {
				hasKey = false;
			}
		}

		SecretKey sk = null;

		if (!hasKey) {
			KeyGenerator kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			sk = kg.generateKey();
		}


		char[] password = "keystorepassword".toCharArray();
		java.io.FileInputStream fis = null;
		try {
			fis = new java.io.FileInputStream("keyStoreName");
			ks.load(fis, password);
		} finally {
			if (fis != null) {
				fis.close();
			}

			KeyStore.ProtectionParameter protParam =
					new KeyStore.PasswordProtection(password);

			KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(sk);
			ks.setEntry("secretKeyAlias", skEntry, protParam);
		/*
		final String strToEncrypt = "Hello World";
KeyGenerator kg = KeyGenerator.getInstance("AES");
kg.init(128);
SecretKey sk = kg.generateKey();
String secretKey = String.valueOf(Hex.encodeHex(sk.getEncoded()));
//Storing AES Secret key in keystore
KeyStore ks = KeyStore.getInstance("JCEKS");
char[] password = "keystorepassword".toCharArray();
java.io.FileInputStream fis = null;
try {
  fis = new java.io.FileInputStream("keyStoreName");
  ks.load(fis, password);
} finally {
  if (fis != null) {
    fis.close();
  }

  KeyStore.ProtectionParameter protParam =
    new KeyStore.PasswordProtection(password);

  KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(sk);
  ks.setEntry("secretKeyAlias", skEntry, protParam);
		 */

			return null;
		}
	}

	private void encryptStreamAsym(@NonNull String argPlainText, @NonNull OutputStream argOutputStream, @NonNull Key argKey)
			throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
		Cipher inCipher = getCipherAsym();
		inCipher.init(Cipher.ENCRYPT_MODE, argKey);

		CipherOutputStream cipherOutputStream =
				new CipherOutputStream(argOutputStream, inCipher);
		cipherOutputStream.write(argPlainText.getBytes("UTF-8"));
		cipherOutputStream.close();
	}

	private void decryptStreamAsym(@NonNull String argCipherText, @NonNull InputStream argInputStream, @NonNull Key argKey) throws IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
		Cipher outCipher = getCipherAsym();
		outCipher.init(Cipher.DECRYPT_MODE, argKey);

		CipherInputStream cipherInputStream =
				new CipherInputStream(argInputStream,
						outCipher);
		byte [] roundTrippedBytes = new byte[1000]; // TODO: dynamically resize as we get more data

		int index = 0;
		int nextByte;
		while ((nextByte = cipherInputStream.read()) != -1) {
			roundTrippedBytes[index] = (byte)nextByte;
			index++;
		}
		String roundTrippedString = new String(roundTrippedBytes, 0, index, "UTF-8");
	}

	private static Cipher getCipherAsym() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
		return cipher;
	}

	private static Cipher getCipherSym() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
		int count = CIPHERS.length;

		Cipher cipher = null;
		for (int i = 0; i < count; i++) {
			try {
				cipher = Cipher.getInstance(CIPHERS[i], "AndroidOpenSSL");
			} catch (NoSuchAlgorithmException e) {

			}
		}

		if (cipher == null) {
			throw new NoSuchAlgorithmException("Could not find a supported algorithm"); // NoI18N
		}

		return cipher;
	}

	private static byte[] toBytes(@NonNull String argString) throws UnsupportedEncodingException {
		return argString.getBytes("UTF-8");
	}

	private PublicKey getPublicKey(@NonNull String argAlias, @NonNull Context argContext) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableEntryException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException {
		KeyStore.PrivateKeyEntry privateKeyEntry = getEncryptionKey(argAlias, argContext);
		PublicKey publicKey = privateKeyEntry.getCertificate().getPublicKey();

		return publicKey;
	}

	private PrivateKey getPrivateKey(@NonNull String argAlias, @NonNull Context argContext) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, NoSuchProviderException, InvalidAlgorithmParameterException, UnrecoverableEntryException {
		KeyStore.PrivateKeyEntry privateKeyEntry = getEncryptionKey(argAlias, argContext);
		ECPrivateKey privateKey = (ECPrivateKey) privateKeyEntry.getPrivateKey();
		return privateKey;
	}

	private KeyStore.PrivateKeyEntry getEncryptionKey(@NonNull String argAlias, @NonNull Context argContext) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, NoSuchProviderException, InvalidAlgorithmParameterException, UnrecoverableEntryException {
		KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER);
		keyStore.load(null);

		if (!keyStore.containsAlias(argAlias)) {
			generatePrivateKey(argAlias, argContext);
		}

		// Retrieve the keys
		KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(argAlias, null);

		return privateKeyEntry;
	}


	@TargetApi(18)
	private KeyPair generatePrivateKey(@NonNull String alias, @NonNull Context argContext) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		/*
 		* Generate a new EC key pair entry in the Android Keystore by
 		* using the KeyPairGenerator API. The private key can only be
 		* used for signing or verification and only with SHA-256 or
 		* SHA-512 as the message digest.
 		*/

		KeyPairGenerator kpg = null;
		String keyProperties = KeyProperties.KEY_ALGORITHM_EC;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			kpg = KeyPairGenerator.getInstance(
					keyProperties, ANDROID_KEYSTORE_PROVIDER);

			KeyGenParameterSpec keyGenParameterSpec = null;

			keyGenParameterSpec = new KeyGenParameterSpec.Builder(
					alias,
					KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
					.setDigests(KeyProperties.DIGEST_SHA256,
							KeyProperties.DIGEST_SHA512).build();


			kpg.initialize(keyGenParameterSpec);

		} else {
			kpg = legacyKeyPairGenerator(argContext, alias, keyProperties);
		}

		KeyPair kp = kpg.generateKeyPair();

		return kp;
	}

	/**
	 * http://stackoverflow.com/questions/27320610/how-can-i-use-the-android-keystore-to-securely-store-arbitrary-strings
	 *
	 * @param argContex
	 * @param argAlias
	 * @param argKeyProperties
	 * @return
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
     */
	@TargetApi(19)
	private KeyPairGenerator legacyKeyPairGenerator(@NonNull Context argContex, @NonNull String argAlias, @NonNull String argKeyProperties) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		Calendar notBefore = Calendar.getInstance();
		Calendar notAfter = Calendar.getInstance();
		notAfter.add(Calendar.YEAR, 1);
		KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(argContex)
				.setAlias(argAlias)
				.setKeyType(argKeyProperties)
				.setKeySize(2048)
				.setSubject(new X500Principal("CN=test"))
				.setSerialNumber(BigInteger.ONE)
				.setStartDate(notBefore.getTime())
				.setEndDate(notAfter.getTime())
				.build();
		KeyPairGenerator generator = KeyPairGenerator.getInstance(argKeyProperties, ANDROID_KEYSTORE_PROVIDER);
		generator.initialize(spec);

		return generator;
	}

}
