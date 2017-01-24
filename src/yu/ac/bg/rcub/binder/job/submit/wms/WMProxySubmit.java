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
package yu.ac.bg.rcub.binder.job.submit.wms;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.glite.security.trustmanager.ContextWrapper;
import org.glite.voms.contact.UserCredentials;
import org.glite.voms.contact.VOMSProxyInit;
import org.glite.voms.contact.VOMSRequestOptions;
import org.glite.wms.wmproxy.CredentialException;
import org.glite.wms.wmproxy.JobIdStructType;
import org.glite.wms.wmproxy.ServiceException;
import org.glite.wms.wmproxy.ServiceURLException;
import org.glite.wms.wmproxy.WMProxyAPI;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.io.urlcopy.UrlCopy;
import org.globus.io.urlcopy.UrlCopyException;
import org.globus.util.GlobusURL;

import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.ComputingElement;
import yu.ac.bg.rcub.binder.job.WorkerJob;
import yu.ac.bg.rcub.binder.job.submit.JobSubmit;
import yu.ac.bg.rcub.binder.security.CertUtils;

public class WMProxySubmit extends JobSubmit {
	private static Logger logger = Logger.getLogger(WMProxySubmit.class);

	private static WMProxyAPI wmClient = null;
	private static GlobusCredential proxy = null;
	// static final long MIN_PROXY_TIME_LEFT = 24 * 3600 - 120; // TEST-REMOVE
	static final long MIN_PROXY_TIME_LEFT = 7200;
	static final int PROXY_LIFETIME = 24 * 3600;

	private static VOMSProxyInit vomsProxyInit = null;
	static final String DELEGATION_ID = "work-binder";

	static final String PROTOCOL = "gsiftp";

	private static String VO;
	private static String WMProxyUrl;
	static final String JDL_TEMPLATE_FILE = System.getProperty("binder.home") + File.separator + "conf" + File.separator
			+ "jdl.template";

	static final String REMOTE_DIR = System.getProperty("binder.home") + File.separator + "remote";

	private static String jdlTemplate;

	private static String certDir;

	private ArrayList<String> jobIDs = new ArrayList<String>();

	private static String[] inboxFiles;
	private static String remoteFilesString = null;

	private static String binderCert;

	private static String binderCertPass;

	private static String crlUpdateInterval;

	private static String credentialsUpdateInterval;

	static {
		init();
	}

	private static void init() {
		// needed for glite-trustmanager
		// System.setProperty("X509_USER_CERT", cert);
		// System.setProperty("X509_USER_KEY", key);
		// System.setProperty("X509_USER_KEY_PASSWORD", pass);

		// 1st X509_USER_CERT and X509_USER_KEY
		// 2nd PKCS12_USER_CERT and PKCS12_USER_KEY_PASSWORD
		// 3rd BinderCert && BINDER_CERT_PASS
		// maybe not the prefered order - NOT USED
		if (System.getProperty("PKCS12_USER_CERT") == null) {
			binderCert = BinderUtil.getProperty("BinderCert");
			binderCertPass = System.getenv("BINDER_CERT_PASS");
			if (binderCertPass == null) {
				logger.warn("$BINDER_CERT_PASS env variable not set, using empty string as password.");
				binderCertPass = "";
			}
			System.setProperty("PKCS12_USER_CERT", binderCert);
			System.setProperty("PKCS12_USER_KEY_PASSWORD", binderCertPass);
		}

		certDir = BinderUtil.getProperty("CertificatesDir", "/etc/grid-security/certificates");
		System.setProperty("CADIR", certDir);
		String vomsDir = BinderUtil.getProperty("VomsDir", "/etc/grid-security/vomsdir");
		System.setProperty("VOMSDIR", vomsDir);
		String gliteLoc = BinderUtil.getProperty("GliteLoc", "/opt/glite");
		System.setProperty("GLITE_LOCATION", gliteLoc);

		// needed for glite-trustmanager
		System.setProperty(ContextWrapper.TRUSTSTORE_DIR, certDir);
		crlUpdateInterval = BinderUtil.getProperty("CrlUpdateInterval", "2h");
		System.setProperty(ContextWrapper.CRL_UPDATE_INTERVAL, crlUpdateInterval);
		credentialsUpdateInterval = BinderUtil.getProperty("CredentialsUpdateInterval", "1h");
		System.setProperty(ContextWrapper.CREDENTIALS_UPDATE_INTERVAL, credentialsUpdateInterval);
		// we want to use the new KeyManager
		// - too repressive - rejects too many certificates
		System.clearProperty(ContextWrapper.CA_FILES);

		// needed for CoG Jglobus
		System.setProperty("X509_CERT_DIR", certDir);

		VO = BinderUtil.getProperty("VO");
		WMProxyUrl = BinderUtil.getProperty("WMProxyServer");
		initInboxFiles();
		try {
			jdlTemplate = readTemplate();
		} catch (IOException e) {
			logger.error("Error occured while reading jdl template file: ", e);
			jdlTemplate = "";
		}
	}

	private static void initInboxFiles() {
		File dir = new File(REMOTE_DIR);
		File[] files = null;
		if (dir.isDirectory()) {
			files = dir.listFiles();
		}
		int length = 0;
		if (files != null && files.length > 0) {
			length = files.length;
		}
		inboxFiles = new String[3 + length];
		inboxFiles[0] = System.getProperty("binder.home") + File.separator + "bin" + File.separator + "startBinderJob.sh";
		inboxFiles[1] = System.getProperty("binder.home") + File.separator + "bin" + File.separator + "binder-remote.jar";
		inboxFiles[2] = System.getProperty("binder.home") + File.separator + "conf" + File.separator
				+ "WorkerDispatcher.properties";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < files.length; i++) {
			inboxFiles[i + 3] = REMOTE_DIR + File.separator + files[i].getName();
			if (i > 0) {
				sb.append(",");
				sb.append(" ");
			}
			sb.append('"');
			sb.append(files[i].getName());
			sb.append('"');
		}
		if (sb.length() > 0)
			remoteFilesString = sb.toString();
	}

	private static void getWMClient() throws IOException, ServiceException, ServiceURLException, CredentialException {
		ByteArrayOutputStream proxyStream = new ByteArrayOutputStream();

		proxy.save(proxyStream);
		wmClient = new WMProxyAPI(WMProxyUrl, new ByteArrayInputStream(proxyStream.toByteArray()), certDir);

		// NOTE: WMProxyAPI overwrittes some system properties each time its
		// called, causing some unwanted behaviour. Among other things it
		// changes default AxisSocket factory, which is ignored fow now
		// see WMProxyAPI.setUpService ()

		// restore overwritten properties
		System.setProperty(ContextWrapper.CRL_UPDATE_INTERVAL, crlUpdateInterval);
		// we want to use the new KeyManager
		// - too repressive - rejects too many certificates
		System.clearProperty(ContextWrapper.CA_FILES);

		logger.debug("Refreshed wmproxy client");
	}

	private static String readTemplate() throws IOException {
		StringBuilder sb = new StringBuilder();

		BufferedReader reader = new BufferedReader(new FileReader(JDL_TEMPLATE_FILE));
		String line;
		while ((line = reader.readLine()) != null) {
			// skip comments && and empty lines
			if (line.length() > 0 && line.trim().charAt(0) != '#') {
				sb.append(line);
				sb.append('\n');
			}
		}
		String result = sb.toString();
		logger.debug("Loaded JDL template file:\n" + result);
		return result;
	}

	private String processJDLTemplate(String jobID, String logFile, String ce, String routingInfo, String vo) {
		String result = jdlTemplate;
		result = result.replace("$LOGFILE", logFile);
		result = result.replace("$CE", ce);
		result = result.replace("$JOBID", jobID);
		result = result.replace("$VO", VO);
		result = result.replace("$ROUTINGINFO", routingInfo);
		if (remoteFilesString != null)
			result = result.replace("$REMOTEFILES", remoteFilesString);
		return result;
	}

	private static synchronized void getProxy() throws Exception {

		if (proxyOK())
			return;

		logger.debug("Generating proxy...");
		// maybe not needed
		// this way expired certificate can be reloaded in realtime
		X509Certificate cert = (X509Certificate) CertUtils.getCertPKCS(new FileInputStream(binderCert), binderCertPass
				.toCharArray());
		PrivateKey key = CertUtils.getPrivateKeyPKCS(new FileInputStream(binderCert), binderCertPass.toCharArray());
		UserCredentials creds = UserCredentials.instance(key, new X509Certificate[] { cert });

		vomsProxyInit = VOMSProxyInit.instance(creds);

		// dont save proxy to the file
		vomsProxyInit.setProxyOutputFile(null);
		Collection<VOMSRequestOptions> requests = new ArrayList<VOMSRequestOptions>();
		VOMSRequestOptions option = new VOMSRequestOptions();
		option.setVoName(VO);
		option.setLifetime(PROXY_LIFETIME);
		requests.add(option);
		logger.debug("Contacting voms server...");
		UserCredentials vomsproxy = vomsProxyInit.getVomsProxy(requests);

		proxy = new GlobusCredential(vomsproxy.getUserKey(), vomsproxy.getUserChain());
		logger.debug("Setting default proxy...");
		GlobusCredential.setDefaultCredential(proxy);
		if (logger.isDebugEnabled()) {
			logger.debug("Generated proxy:\n" + proxy);
		}

		// refresh wmproxy client
		getWMClient();
	}

	public static boolean proxyOK() {
		if (proxy == null)
			return false;
		try {
			// redundant!
			proxy.verify();
		} catch (GlobusCredentialException e) {
			return false;
		}

		if (proxy.getTimeLeft() < MIN_PROXY_TIME_LEFT)
			return false;

		return true;
	}

	// TODO switch from Exception to something more informative
	public void submit(WorkerJob job, ComputingElement ce) throws Exception {
		if (!proxyOK()) {
			getProxy();
		}

		String jobId = job.getJobID();
		String routingInfo = "Started_at_" + job.getStatusTimestamp();
		String logFileName = BinderUtil.getFileOutputName(jobId);
		String jdl = processJDLTemplate(jobId, logFileName, ce.getName(), routingInfo, VO);
		if (logger.isTraceEnabled()) {
			logger.trace("Generated JDL:\n{" + jdl + "\n}");
		}

		String proxyReq = wmClient.grstGetProxyReq(DELEGATION_ID);
		wmClient.grstPutProxy(DELEGATION_ID, proxyReq);
		JobIdStructType jobIdStruct = wmClient.jobRegister(jdl, DELEGATION_ID);
		if (logger.isDebugEnabled()) {
			logger.debug("Job Registered:");
			logger.debug("\tJOB-ID: [" + jobIdStruct.getId() + "]");
			logger.debug("\tName:   [" + jobIdStruct.getName() + "]");
			logger.debug("\tPath:   [" + jobIdStruct.getPath() + "]");
		}

		String[] uris = wmClient.getSandboxDestURI(jobIdStruct.getId(), PROTOCOL).getItem();
		if (logger.isDebugEnabled()) {
			logger.debug("Sandbox URIs:");
			if (uris != null) {
				for (int i = 0; i < uris.length; i++) {
					logger.debug("\t-" + uris[i]);
				}
			}
		}
		copyFilesToWMS(uris[0]);
		wmClient.jobStart(jobIdStruct.getId());
		jobIDs.add(jobIdStruct.getId());
		logger.info("Job successfully submitted: '" + jobId + "', grid job ID: " + jobIdStruct.getId());
	}

	private void copyFilesToWMS(String destURI) throws MalformedURLException, UrlCopyException {
		for (String fileName : inboxFiles) {
			File file = new File(fileName);
			URL fromURL = file.toURI().toURL();
			GlobusURL from = new GlobusURL(fromURL);
			// use linux file separator just for precaution
			String toURL = destURI + "/" + file.getName();
			// quick fix for a bug where destURI path starts from root but
			// GlobusURL doesnt figure this out and cuts out '/' resulting in
			// destURI going to ~home dir
			toURL = toURL.replace("/var/glite/", "//var/glite/");
			GlobusURL to = new GlobusURL(toURL);
			UrlCopy uCopy = new UrlCopy();
			uCopy.setSourceUrl(from);
			uCopy.setDestinationUrl(to);
			uCopy.setUseThirdPartyCopy(true);
			uCopy.copy();
			if (logger.isTraceEnabled()) {
				logger.trace("Copying:");
				logger.trace("From:\n" + from);
				logger.trace("To:\n" + to);
			}
		}
		logger.debug("Files copied - " + inboxFiles.length);
	}

	public String[] getSubmittedJobs() {
		String[] result = new String[jobIDs.size()];
		jobIDs.toArray(result);
		return result;
	}
}
