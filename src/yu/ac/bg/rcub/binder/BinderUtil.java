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
package yu.ac.bg.rcub.binder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Properties;

import org.glite.security.trustmanager.ContextWrapper;

import yu.ac.bg.rcub.binder.eventlogging.EventLog;
import yu.ac.bg.rcub.binder.net.BinderSocketFactory;
import yu.ac.bg.rcub.binder.security.CertUtils;
import yu.ac.bg.rcub.binder.security.VomsUtils;
import yu.ac.bg.rcub.binder.util.Enums.AuthzType;
import yu.ac.bg.rcub.binder.util.Enums.RecoveryType;
import yu.ac.bg.rcub.binder.util.Enums.SubmissionType;

public class BinderUtil {

	private static RecoveryType recoveryType;
	private static AuthzType authzType;
	private static SubmissionType submissionType;

	public static final int PROTOCOL_VERSION = 8;

	private static String scriptString;
	private static String HTMLGstatLink;

	private static boolean PERFORMANCE_MONITORING = false;
	/** Binder shutdown command */
	private static String shutdown = "SHUTDOWN";

	private static Properties prop;
	public static EventLog eventLog;
	private static BinderSocketFactory socketFactory;
	private static boolean useSSL = true;
	private static boolean requireClientAuth = true;

	/** Dont allow instantiation. */
	private BinderUtil() {

	}

	/**
	 * Initializes an <code>int</code> field from the properties. If not found,
	 * default value is used.
	 */
	public static int initIntField(String fieldName, int defaultValue) {
		int n = defaultValue;
		try {
			String val = prop.getProperty(fieldName);
			if (val != null)
				n = Integer.parseInt(val);
		} catch (NumberFormatException ne) {
		}
		return n;
	}

	/**
	 * Initializes a <code>double</code> field from the properties. If not
	 * found, default value is used.
	 */
	public static double initDoubleField(String fieldName, double defaultValue) {
		double n = defaultValue;
		try {
			String val = prop.getProperty(fieldName);
			if (val != null)
				n = Double.parseDouble(val);
		} catch (NumberFormatException ne) {
		}
		return n;
	}

	public static void initService(String propPath) {
		prop = new Properties();
		try {
			prop.load(new FileInputStream(new File(propPath)));
			scriptString = prop.getProperty("Script", System.getProperty("binder.home") + File.separator + "bin"
					+ File.separator + "submitRemoteJob.sh");

			initSocketFactory();

			/* RecoveryType init */
			initRecovery(prop.getProperty("RecoveryType", RecoveryType.NONE.toString()).toUpperCase());
			/* Authz init */
			initAuthz(prop.getProperty("AuthzType", AuthzType.NONE.toString()).toUpperCase());
			if (authzType == AuthzType.VOMS) {
				// TODO this can be removed/changed
				CertUtils.init();
				VomsUtils.init();
			}
			initSubType(prop.getProperty("JobSubmissionType", SubmissionType.INTERNAL.toString()).toUpperCase());

			HTMLGstatLink = prop.getProperty("gstatHomeURL");
			shutdown = prop.getProperty("ShutDownCommand", "SHUTDOWN");
			String perfMonitoring = prop.getProperty("GeneratePerformanceMonitoringEvents", "false");
			PERFORMANCE_MONITORING = perfMonitoring.equalsIgnoreCase("true");
		} catch (IOException e) {
			System.out.println("BinderUtil: could not open properties file.");
			e.printStackTrace();
			System.exit(1);
		} catch (GeneralSecurityException e) {
			System.out.println("BinderUtil: could not initialize socket factories.");
			e.printStackTrace();
			System.exit(1);
		}

		if (PERFORMANCE_MONITORING) {
			eventLog = new EventLog();
			eventLog.start();
		}
	}

	// TODO maybe just use properties directly and support pkcs12, jks and pem
	// formats for certificates
	private static void initSocketFactory() throws IOException, GeneralSecurityException {

		// create options needed for socket factories
		Properties config = new Properties();
		useSSL = prop.getProperty("UseSSL", "yes").equalsIgnoreCase("yes");
		config.setProperty("UseSSL", useSSL ? "yes" : "no");
		requireClientAuth = prop.getProperty("RequireClientAuth", "yes").equalsIgnoreCase("yes");
		config.setProperty("RequireClientAuth", requireClientAuth ? "yes" : "no");

		// credentials
		String binderCert = prop.getProperty("BinderCert");
		String binderCertPass = System.getenv("BINDER_CERT_PASS");
		if (binderCertPass == null) {
			binderCertPass = "";
		}
		config.setProperty(ContextWrapper.CREDENTIALS_STORE_FILE, binderCert);
		config.setProperty(ContextWrapper.CREDENTIALS_STORE_TYPE, "PKCS12");
		config.setProperty(ContextWrapper.CREDENTIALS_STORE_PASSWD, binderCertPass);

		// needed for glite-trustmanager
		String certDir = prop.getProperty("CertificatesDir", "/etc/grid-security/certificates");
		config.setProperty(ContextWrapper.TRUSTSTORE_DIR, certDir);
		String crlUpdateInterval = prop.getProperty("CrlUpdateInterval", "2h");
		config.setProperty(ContextWrapper.CRL_UPDATE_INTERVAL, crlUpdateInterval);
		String credentialsUpdateInterval = prop.getProperty("CredentialsUpdateInterval", "1h");
		config.setProperty(ContextWrapper.CREDENTIALS_UPDATE_INTERVAL, credentialsUpdateInterval);

		socketFactory = new BinderSocketFactory(config);
	}

	/**
	 * Initializes authentication type.
	 * 
	 * @param type
	 */
	private static void initAuthz(String type) {
		if (useSSL && requireClientAuth)
			authzType = AuthzType.toAuthzType(type);
		else
			authzType = AuthzType.NONE;
	}

	/**
	 * Initializes job submission type.
	 * 
	 * @param type
	 */
	private static void initSubType(String type) {
		submissionType = SubmissionType.toSubType(type);
	}

	/**
	 * Initializes Recovery Module.
	 * 
	 * @param recType
	 */
	private static void initRecovery(String recType) {
		recoveryType = RecoveryType.toRecType(recType);
	}

	public static String getProperty(String key) {
		return prop.getProperty(key);
	}

	public static String getProperty(String key, String defaultValue) {
		return prop.getProperty(key, defaultValue);
	}

	public static BinderSocketFactory getSocketFactory() {
		return socketFactory;
	}

	public static boolean SSLEnabled() {
		return useSSL;
	}

	public static String getScriptString() {
		return scriptString;
	}

	public static String getShutdown() {
		return shutdown;
	}

	/**
	 * @return
	 */
	public static String getHTMLGstatLink() {
		return HTMLGstatLink;
	}

	/**
	 * @param string
	 */
	public static void setHTMLGstatLink(String string) {
		HTMLGstatLink = string;
	}

	public static RecoveryType getRecoveryType() {
		return recoveryType;
	}

	public static AuthzType getAuthType() {
		return authzType;
	}

	public static SubmissionType getSubmissionType() {
		return submissionType;
	}

	public static boolean isPerfMonEnabled() {
		return PERFORMANCE_MONITORING;
	}

	public static String readString(DataInputStream in) throws EOFException, IOException {
		int len = in.readInt();
		byte[] b = new byte[len];
		in.readFully(b);
		return new String(b);
	}

	public static void writeString(DataOutputStream out, String s) throws IOException {
		if (s == null)
			s = "";
		out.writeInt(s.length());
		out.writeBytes(s);
	}

	public static byte[] readBytes(DataInputStream in) throws EOFException, IOException {
		int len = in.readInt();
		byte[] b = new byte[len];
		in.readFully(b);
		return b;
	}

	public static void writeBytes(DataOutputStream out, byte[] b) throws IOException {
		out.writeInt(b.length);
		out.write(b);
	}

	public static double[] readDoubles(DataInputStream in) throws EOFException, IOException {
		int len = in.readInt();
		double[] b = new double[len];
		for (int i = 0; i < len; i++)
		{
			b[i] = in.readDouble();
		}
		return b;
	}
	
	public static void writeDoubles(DataOutputStream out, double[] b) throws IOException {
		out.writeInt(b.length);
		for (int i = 0; i < b.length; i++) 
		{
			out.writeDouble(b[i]);
		}
	}
	
	
	public static PrintWriter getFileOutput(String jobID, String modifier, String dir) throws FileNotFoundException {
		String outFile = getFileOutputName(jobID) + modifier;
		return new PrintWriter(dir + outFile);
	}

	public static String getFileOutputName(String jobID) {
		/* substitute :N for _N if there is one in jobID */
		return jobID.lastIndexOf(":") > 0 ? jobID.replace(':', '_') : jobID;
	}

	/**
	 * Reads arguments from the string. Same as <code>readArgs(s, true)</code>.
	 * <p>
	 * NOTE: Empty string arguments will be ignored!
	 * 
	 * @see #readArgs(String, boolean)
	 * 
	 * @param s
	 *            The <code>String</code> containing arguments.
	 * @return The <code>String</code> array of arguments.
	 */
	public static String[] readArgs(String s) {
		return readArgs(s, true);
	}

	/**
	 * Reads arguments from the string.
	 * <p>
	 * NOTE: Empty string arguments will be ignored!
	 * 
	 * @param s
	 *            The <code>String</code> containing arguments.
	 * @param includeQuotes
	 *            If <code>true</code>, includes quotes in quoted arguments.
	 * @return The <code>String</code> array of arguments.
	 */
	public static String[] readArgs(String s, boolean includeQuotes) {
		/* NOTE: This will also be accepted: '"aa""bb"' */
		s = " " + s.trim() + " ";
		int lastPos = 0;
		boolean insideQuote = false;
		ArrayList<String> results = new ArrayList<String>();
		for (int i = 1; i < s.length(); i++) {
			if (!insideQuote && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
				if (i > lastPos + 1) /* to avoid adding empty strings */
					results.add(s.substring(lastPos + 1, i));
				lastPos = i;
			} else if (insideQuote && s.charAt(i) == '"') {
				if (i > lastPos + 1) /* to avoid adding empty strings */
					results.add(includeQuotes ? s.substring(lastPos, i + 1) : s.substring(lastPos + 1, i));
				insideQuote = false;
				lastPos = i;
			} else if (!insideQuote && s.charAt(i) == '"') {
				insideQuote = true;
				lastPos = i;
			}
		}
		/*
		 * NOTE: If insideQuote == true after the loop, portion of the string
		 * will not be returned! Maybe throw some exception.
		 */
		String[] a = new String[results.size()];
		/* Copy results from ArrayList to Array. */
		return results.toArray(a);
	}

}
