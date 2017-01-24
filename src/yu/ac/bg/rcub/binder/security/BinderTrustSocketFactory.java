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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.axis.components.net.JSSESocketFactory;
import org.apache.log4j.Logger;

/**
 * Trust store factory used to connect to the AXIS VOMS service. It loads the
 * binders certificate from the file and trusts VOMS servers certificates.
 * 
 * @author choppa
 * 
 */
public class BinderTrustSocketFactory extends JSSESocketFactory {
	private static final Logger logger = Logger.getLogger(BinderTrustSocketFactory.class);

	/**
	 * Constructor BinderTrustSocketFactory
	 * 
	 * @param attributes
	 */
	public BinderTrustSocketFactory(Hashtable<?, ?> attributes) {
		super(attributes);
	}

	/**
	 * Method getContext
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	protected SSLContext getContext() throws Exception {

		try {
			logger.trace("Reading user keystore");
			KeyStore ks = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType", "pkcs12"));
			/* get user password and file input stream */
			char[] password = System.getProperty("javax.net.ssl.keyStorePassword", "").toCharArray();
			FileInputStream fis = new FileInputStream(System.getProperty("javax.net.ssl.keyStore"));
			ks.load(fis, password);
			fis.close();

			logger.trace("Initializing SSL context");
			SSLContext sc = SSLContext.getInstance("SSL");
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SUNX509");
			kmf.init(ks, password);
			KeyManager[] km = kmf.getKeyManagers();
			sc.init(km, new TrustManager[] { new FakeX509TrustManager() }, new java.security.SecureRandom());
			return sc;
		} catch (Exception e) {
			throw new Exception("Unable to initiate security context - " + e.getMessage() + ".");
		}
	}

	/**
	 * Read the keystore, init the SSL socket factory.
	 * 
	 * @throws IOException
	 */
	protected void initFactory() throws IOException {

		try {
			// Security.addProvider(new sun.security.provider.Sun());
			// Security.addProvider(new
			// com.sun.net.ssl.internal.ssl.Provider());

			// Configuration specified in wsdd.
			SSLContext context = getContext();
			sslFactory = context.getSocketFactory();
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw (IOException) e;
			}
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * FakeX509TrustManager that accepts all certificates.
	 */
	public static class FakeX509TrustManager implements X509TrustManager {

		/**
		 * Method getAcceptedIssuers
		 * 
		 * @return
		 */
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {

			return null;
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			// Do nothing
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			// Do nothing
		}
	}

}
