/*******************************************************************************
 * Work Binder Application Service
 * Copyright 2004, 2009 University of Belgrade Computer Centre and
 * individual contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software.  If not, see:
 * http://www.gnu.org/licenses/lgpl.txt
 *******************************************************************************/
package yu.ac.bg.rcub.binder.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

import org.apache.axis.encoding.Base64;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.bc.BouncyCastleOpenSSLKey;

import yu.ac.bg.rcub.binder.BinderUtil;

public class CertUtils {

	static {
		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/* perhaps a map of principals instead of strings? */
	private static HashMap<String, String> caCertMap = null;
	private static String certDir;

	/* TODO extensions can be '.1', '.2' and '.r1', '.r2' as well! */
	private static final String CERT_FILE_EXT = ".0";
	private static final String CRL_FILE_EXT = ".r0";

	private static final String CIPHER_TRANS = "RSA/ECB/PKCS1Padding";
	private static final String ENC_ALG = "SHA1PRNG";
	private static final int DEFAULT_CHALLENGE_SIZE = 30;

	public static void init() {
		certDir = BinderUtil.getProperty("CertificatesDir", "/etc/grid-security/certificates");
		// caCertMap = new HashMap<String, String>();
		// initCACertMap();
	}

	public static boolean verifyUserCert(X509Certificate cert) {
		return verifyUserCert(new X509Certificate[] { cert });
	}

	public static boolean verifyUserCert(X509Certificate[] certChain) {
		X509Certificate userCert = getLastClientsCert(certChain);
		if (userCert == null) {
			logger.warn("Certificate chain is broken or empty, user rejected.");
			return false;
		}
		final String USER_MESS = "User cert (" + userCert.getSubjectDN().toString() + ") ";
		// String caCertFileName =
		// caCertMap.get(userCert.getIssuerX500Principal().toString());
		String caCertFileName = certDir + File.separator;
		caCertFileName += getMD5Hash(userCert.getIssuerX500Principal().getEncoded()) + CERT_FILE_EXT;
		logger.debug("CA cert file determined: " + caCertFileName + ".");

		if (caCertFileName == null) {
			/* Perhaps call initCACertMap() again to see if new CAs showed up? */
			logger.warn(USER_MESS + "not authenticated (issuer DN not found).");
			return false;
		}
		/* check if user cert is valid */
		try {
			userCert.checkValidity();
		} catch (CertificateExpiredException e) {
			logger.warn(USER_MESS + "expired.");
			return false;
		} catch (CertificateNotYetValidException e) {
			logger.warn(USER_MESS + "not yet valid.");
			return false;
		}
		/* check if CA signed user cert */
		X509Certificate caCert = readCertFile(new File(caCertFileName));
		if (caCert == null) {
			logger.warn(USER_MESS + "rejected, could not read CA cert.");
			return false;
		}
		/* check if CA cert is valid. */
		try {
			caCert.checkValidity();
		} catch (CertificateExpiredException e) {
			logger.warn("CA cert expired!");
			return false;
		} catch (CertificateNotYetValidException e) {
			logger.warn("CA cert not yet valid!");
			return false;
		}
		/* verify user cert with CA cert */
		try {
			userCert.verify(caCert.getPublicKey());
		} catch (SignatureException e) {
			logger.warn(USER_MESS + "rejected, cert not signed by CA (" + caCert.getSubjectDN() + ").");
			return false;
		} catch (GeneralSecurityException e) {
			logger.error(e, e);
			return false;
		}
		/* check if user cert is revoked */
		String crlFileName = caCertFileName.substring(0, caCertFileName.length() - 2) + CRL_FILE_EXT;
		X509CRL caCRL = readCRLFile(new File(crlFileName));
		/* if CRL is not found we reject CA! */
		if (caCRL == null) {
			logger.warn(USER_MESS + "rejected, CA (" + caCert.getSubjectDN() + ") CRL not found!");
			return false;
		}
		boolean result = !caCRL.isRevoked(userCert);
		if (result)
			logger.info(USER_MESS + "passed verification.");
		else
			logger.warn(USER_MESS + "rejected, cert is revoked.");

		return result;
	}

	private static X509Certificate getLastClientsCert(X509Certificate[] certChain) {

		if (certChain.length == 0) {
			logger.warn("Clients cert chain is empty.");
			return null;
		}
		X509Certificate currentCert = certChain[0];
		/* get the last cert in chain, it should be user cert */
		for (int i = 1; i < certChain.length; i++) {
			logger.info("Checking user chain " + i + ". (" + currentCert.getSubjectX500Principal().getName() + "):");
			try {
				currentCert.checkValidity();
			} catch (CertificateExpiredException e) {
				logger.warn("   cert in chain expired.");
				return null;
			} catch (CertificateNotYetValidException e) {
				logger.warn("   cert in chain not yet valid.");
				return null;
			}
			try {
				currentCert.verify(certChain[i].getPublicKey());
			} catch (SignatureException e) {
				logger.warn("   chain ends at " + i + ". cert (out of" + certChain.length + ").");
				break;
			} catch (GeneralSecurityException e) {
				logger.error(e, e);
				break;
			}
			currentCert = certChain[i];
		}
		return currentCert;
	}

	public static void initCACertMap() {
		File[] certFiles = getCertFiles(certDir);
		if (certFiles == null) {
			logger.warn("No CA certificates found with extension '" + CERT_FILE_EXT + "'!");
			return;
		}
		logger.info("Initializing CA cert map with " + certFiles.length + " certs.");
		for (File f : certFiles) {
			String certFileName = f.getAbsolutePath();
			X509Certificate caCert = readCertFile(f);
			if (caCert != null) {
				logger.debug("Adding CA cert to the map, DN: " + caCert.getSubjectDN() + ".");
				caCertMap.put(caCert.getSubjectDN().toString(), certFileName);
			}
		}
	}

	private static File[] getCertFiles(String dir) {
		File root = new File(dir);
		if (root.isDirectory())
			return root.listFiles(getCertFileFilter());
		return null;
	}

	/** Search only files that end with PEM cert file extension. */
	private static FileFilter getCertFileFilter() {
		return new FileFilter() {
			public boolean accept(File file) {
				return file.isFile() && file.canRead() && file.getName().endsWith(CERT_FILE_EXT);
			}
		};
	}

	private static X509Certificate readCertFile(File f) {
		String certFileName = f.getName();
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			logger.warn("Cert file " + certFileName + " not found.");
			return null;
		}
		X509Certificate caCert = null;
		try {
			caCert = getCertfromPEM(fin);
		} catch (CertificateException e) {
			logger.error("Unable to read CA cert from " + certFileName + ".", e);
		} catch (IOException e) {
			logger.error("Error reading file " + certFileName + ".", e);
		}
		try {
			fin.close();
		} catch (IOException e) {
			logger.error("Error closing file " + certFileName + ".", e);
		}
		return caCert;
	}

	private static X509CRL readCRLFile(File f) {
		String crlFileName = f.getName();
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			logger.warn("CRL file " + crlFileName + " not found.");
			return null;
		}
		X509CRL caCRL = null;
		try {
			caCRL = getCRLfromPEM(fin);
		} catch (CertificateException e) {
			logger.error("Unable to read CA CRL from " + crlFileName + ".", e);
		} catch (IOException e) {
			logger.error("Error reading file " + crlFileName + ".", e);
		} catch (CRLException e) {
			logger.error("Unable to read CA CRL from " + crlFileName + ".", e);
		}
		try {
			fin.close();
		} catch (IOException e) {
			logger.error("Error closing file " + crlFileName + ".", e);
		}
		return caCRL;
	}

	/**
	 * Reads a certificate in PEM-format from an InputStream. The stream may
	 * contain other things, the first certificate in the stream is read.
	 * 
	 * @param certstream
	 *            the input stream containing the certificate in PEM-format
	 * 
	 * @see http://jcetaglib.sourceforge.net/ (modified to use another base64
	 *      decoder)
	 * 
	 * @return X509Certificate
	 * @exception IOException
	 *                if the stream cannot be read.
	 * @exception CertificateException
	 *                if the stream does not contain a correct certificate.
	 */
	public static X509Certificate getCertfromPEM(InputStream certstream) throws IOException, CertificateException {
		String beginKey = "-----BEGIN CERTIFICATE-----";
		String endKey = "-----END CERTIFICATE-----";
		BufferedReader bufRdr = new BufferedReader(new InputStreamReader(certstream));
		String temp;
		StringBuffer s = new StringBuffer();
		while ((temp = bufRdr.readLine()) != null && !temp.equals(beginKey))
			continue;
		if (temp == null)
			throw new IOException("Error in " + certstream.toString() + ", missing " + beginKey + " boundary.");
		while ((temp = bufRdr.readLine()) != null && !temp.equals(endKey))
			s.append(temp);
		if (temp == null)
			throw new IOException("Error in " + certstream.toString() + ", missing " + endKey + " boundary.");
		byte[] certbuf = Base64.decode(s.toString());
		/* Decode the cert from file back to X509Certificate. */
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate x509cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certbuf));
		return x509cert;
	}

	/**
	 * Reads a CRL in PEM-format from an InputStream. The stream may contain
	 * other things, the first certificate in the stream is read.
	 * 
	 * @param crlstream
	 *            the input stream containing the CRL in PEM-format
	 * 
	 * @return X509CRL
	 * @exception IOException
	 *                if the stream cannot be read.
	 * @exception CertificateException
	 *                if the stream does not contain a correct certificate.
	 * @throws CRLException
	 * @throws CertificateException
	 * @throws CRLException
	 */
	public static X509CRL getCRLfromPEM(InputStream crlstream) throws IOException, CRLException, CertificateException {
		String beginKey = "-----BEGIN X509 CRL-----";
		String endKey = "-----END X509 CRL-----";
		BufferedReader bufRdr = new BufferedReader(new InputStreamReader(crlstream));
		String temp;
		StringBuffer s = new StringBuffer();
		while ((temp = bufRdr.readLine()) != null && !temp.equals(beginKey))
			continue;
		if (temp == null)
			throw new IOException("Error in " + crlstream.toString() + ", missing " + beginKey + " boundary.");
		while ((temp = bufRdr.readLine()) != null && !temp.equals(endKey))
			s.append(temp);
		if (temp == null)
			throw new IOException("Error in " + crlstream.toString() + ", missing " + endKey + " boundary.");
		byte[] crlbuf = Base64.decode(s.toString());
		/* Decode the cert from file back to X509CRL. */
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509CRL x509crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(crlbuf));
		return x509crl;
	}

	/**
	 * Decodes (decrypts) an <code>Array</code> of bytes using the provided
	 * <code>Key</code>.
	 * 
	 * @param data
	 *            <code>Array</code> of bytes that will be decoded.
	 * @param key
	 *            The <code>Key</code> that will be used to decode data.
	 * @return The decoded <code>Array</code> of bytes.
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public static byte[] decodeData(byte[] data, Key key) throws IOException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException {
		if (data.length == 0)
			return new byte[0];

		Cipher cipher = Cipher.getInstance(CIPHER_TRANS);
		cipher.init(Cipher.DECRYPT_MODE, key);
		final CipherInputStream cin = new CipherInputStream(new ByteArrayInputStream(data), cipher);
		/* read big endian short length, msb then lsb */
		final int messageLengthInBytes = (cin.read() << 8) | cin.read();
		/* check here if size is ok! */
		final byte[] decodedBytes = new byte[messageLengthInBytes];
		/* we can't trust CipherInputStream to give us all the data in one shot */
		int bytesReadSoFar = 0;
		int bytesRemaining = messageLengthInBytes;
		while (bytesRemaining > 0) {
			final int bytesThisChunk = cin.read(decodedBytes, bytesReadSoFar, bytesRemaining);
			if (bytesThisChunk == 0) {
				throw new IOException("Input byte stream corrupted.");
			}
			bytesReadSoFar += bytesThisChunk;
			bytesRemaining -= bytesThisChunk;
		}
		cin.close();
		return decodedBytes;
	}

	/**
	 * Encodes (encrypts) an <code>Array</code> of bytes using the provided
	 * <code>Key</code>.
	 * 
	 * @param data
	 *            <code>Array</code> of bytes that will be encoded.
	 * @param key
	 *            The <code>Key</code> that will be used to encode data.
	 * @return The encoded <code>Array</code> of bytes.
	 * @throws InvalidKeyException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public static byte[] encodeData(byte[] data, Key key) throws InvalidKeyException, IOException, NoSuchAlgorithmException,
			NoSuchPaddingException {
		Cipher cipher = Cipher.getInstance(CIPHER_TRANS);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		final CipherOutputStream cout = new CipherOutputStream(bout, cipher);
		/* prepend with big-endian short message length, will be encrypted too. */
		cout.write(data.length >>> 8); /* msb */
		cout.write(data.length & 0xff); /* lsb */
		cout.write(data);
		cout.close();
		return bout.toByteArray();
	}

	/**
	 * Generates a random sequence of bytes used to send a challenge to the
	 * client in order to verify its authenticity.
	 * 
	 * @return A <code>byte</code> <code>Array</code> representing generated
	 *         challenge.
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] generateChallenge() throws NoSuchAlgorithmException {
		return generateChallenge(DEFAULT_CHALLENGE_SIZE);
	}

	/**
	 * Generates a random sequence of bytes used to send a challenge to the
	 * client in order to verify its authenticity.
	 * 
	 * @param size
	 *            An <code>int</code> representing the size of the challenge.
	 * @return A <code>byte</code> <code>Array</code> representing generated
	 *         challenge.
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] generateChallenge(int size) throws NoSuchAlgorithmException {
		byte[] result = new byte[size];
		SecureRandom rand = SecureRandom.getInstance(ENC_ALG);
		rand.nextBytes(result);
		return result;
	}

	/**
	 * Reads the private key from the PKCS12 key store, the first private key
	 * found will be read.
	 * 
	 * @see #getPrivateKeyPKCS(InputStream, char[], String)
	 * @param in
	 *            <code>InputStream</code> from where <code>KeyStore</code> will
	 *            be read.
	 * @param password
	 *            Password to open the keystore.
	 * @return The <code>PrivateKey</code> read from the key store,
	 *         <code>null</code> otherwise.
	 * @throws IOException
	 * @throws UnrecoverableKeyException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws NoSuchProviderException
	 */
	public static PrivateKey getPrivateKeyPKCS(InputStream in, char[] password) throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, IOException, NoSuchProviderException {
		return getPrivateKeyPKCS(in, password, null);
	}

	/**
	 * Reads the private key from the PKCS12 key store.
	 * 
	 * @param in
	 *            <code>InputStream</code> from where <code>KeyStore</code> will
	 *            be read.
	 * @param password
	 *            Password to open the keystore.
	 * @param alias
	 *            Alias representing the <code>PrivateKey</code>.
	 * @return The <code>PrivateKey</code> read from the key store,
	 *         <code>null</code> otherwise.
	 * @throws KeyStoreException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws NoSuchProviderException
	 * 
	 */
	public static PrivateKey getPrivateKeyPKCS(InputStream in, char[] password, String alias) throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, NoSuchProviderException {
		KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
		ks.load(in, password);
		if (alias == null) {
			Enumeration<String> aliases = ks.aliases();
			/* Read only the first alias, if no alias provided. */
			if (aliases.hasMoreElements())
				alias = aliases.nextElement();
			else {
				throw new KeyStoreException("Keystore seems to be empty, no key found.");
			}
		}
		return (PrivateKey) ks.getKey(alias, password);
	}

	/**
	 * Reads the cert from the PKCS12 key store, the first cert found will be
	 * read.
	 * 
	 * @see #getCertPKCS(InputStream, char[], String)
	 * @param in
	 *            <code>InputStream</code> from where <code>KeyStore</code> will
	 *            be read.
	 * @param password
	 *            Password to open the keystore.
	 * @return The <code>Certificate</code> read from the key store,
	 *         <code>null</code> otherwise.
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 */
	public static Certificate getCertPKCS(InputStream in, char[] password) throws NoSuchAlgorithmException,
			CertificateException, KeyStoreException, IOException, NoSuchProviderException {
		return getCertPKCS(in, password, null);
	}

	/**
	 * Reads the cert from the PKCS12 key store.
	 * 
	 * @param in
	 *            <code>InputStream</code> from where <code>KeyStore</code> will
	 *            be read.
	 * @param password
	 *            Password to open the keystore.
	 * @param alias
	 *            Alias representing the <code>Certificate</code>.
	 * @return The <code>Certificate</code> read from the key store,
	 *         <code>null</code> otherwise.
	 * @throws IOException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws NoSuchProviderException
	 */
	public static Certificate getCertPKCS(InputStream in, char[] password, String alias) throws NoSuchAlgorithmException,
			CertificateException, IOException, KeyStoreException, NoSuchProviderException {
		// provider added for voms proxy
		KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
		ks.load(in, password);
		if (alias == null) {
			Enumeration<String> aliases = ks.aliases();
			/* Read only the first alias, if no alias provided. */
			if (aliases.hasMoreElements())
				alias = aliases.nextElement();
			else {
				throw new KeyStoreException("Keystore seems to be empty, no cert found.");
			}
		}
		return ks.getCertificate(alias);
	}

	/**
	 * Generates a <code>GlobusCredential</code> from data.
	 * 
	 * @param certData
	 *            A two dimensional <code>Array</code> of bytes representing the
	 *            certificate chain.
	 * @param keyData
	 *            An <code>Array</code> of bytes representing the private key
	 * @return A <code>GlobusCredential</code> if the data was in correct
	 *         format, <code>null</code> otherwise.
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static GlobusCredential getCertFromData(byte[][] certData, byte[] keyData) throws IOException,
			GeneralSecurityException {
		// CertificateFactory cf = CertificateFactory.getInstance("X.509");
		CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
		X509Certificate[] certs = new X509Certificate[certData.length];
		for (int i = 0; i < certData.length; i++)
			certs[i] = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certData[i]));
		BouncyCastleOpenSSLKey key = null;
		if (keyData != null && keyData.length > 0) {
			key = new BouncyCastleOpenSSLKey(new ByteArrayInputStream(keyData));
			return new GlobusCredential(key.getPrivateKey(), certs);
		} else
			return new GlobusCredential(null, certs);
	}

	public static byte[][] getEncodedCertsData(X509Certificate[] certChain) throws CertificateEncodingException {
		byte[][] data = new byte[certChain.length][];
		for (int i = 0; i < certChain.length; i++)
			data[i] = certChain[i].getEncoded();
		return data;
	}

	/**
	 * Gets the MD5 hash value of the given byte array.
	 * 
	 * @param data
	 *            the data from which to compute the hash.
	 * 
	 * @return the hash value.
	 * 
	 * @throws IllegalArgumentException
	 *             if data is <code>null</code>.
	 * @throws IllegalStateException
	 *             if MD5 is algorithm not available.
	 */
	public static String getMD5Hash(byte[] data) {
		if (data == null)
			throw new IllegalArgumentException("Null certificate passed to getHash().");

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			logger.error("MD5 algorithm not available.");
			throw new IllegalStateException("MD5 algorithm not available.", e);
		}
		md.update(data);
		byte[] digest = md.digest();
		ByteBuffer bb = ByteBuffer.wrap(digest).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		bb.rewind();
		return Integer.toHexString(bb.getInt());
	}

	private static Logger logger = Logger.getLogger(CertUtils.class);
}
