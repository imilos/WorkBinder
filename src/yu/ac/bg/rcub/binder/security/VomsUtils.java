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

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import org.apache.axis.AxisFault;
import org.apache.log4j.Logger;
import org.glite.wsdl.services.org_glite_security_voms.User;
import org.glite.wsdl.services.org_glite_security_voms.VOMSException;
import org.glite.wsdl.services.org_glite_security_voms_service_admin.VOMSAdminSoapBindingStub;

import yu.ac.bg.rcub.binder.BinderUtil;

public class VomsUtils {

	public static void init() {
		logger.debug("Initializing the VOMS auth engine.");

		String binderCert = BinderUtil.getProperty("BinderCert");
		String binderCertPass = System.getenv("BINDER_CERT_PASS");
		if (binderCertPass == null) {
			logger.warn("$BINDER_CERT_PASS env variable not set, using empty string as password.");
			binderCertPass = "";
		}
		System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
		System.setProperty("javax.net.ssl.keyStore", binderCert);
		System.setProperty("javax.net.ssl.keyStorePassword", binderCertPass);

		// VOMS certs are always trusted by default. This is used just in case
		// glite-trustmanager is missing
		// NOTE: this setting will be overridden by glite-trustmanagers axis
		// socket factory when internal job submission is used, maybe not set it
		// at all?
		System.setProperty("axis.socketSecureFactory", "yu.ac.bg.rcub.binder.security.BinderTrustSocketFactory");
	}

	public static boolean verifyUserGroup(String userIdentity, String appID) {
		String appInfo = BinderUtil.getProperty(appID + "_VOMSAuth");
		if (appInfo == null) {
			logger.error("Application (App = " + appID + ") VOMS info not found.");
			return false;
		}
		String[] values = appInfo.trim().split(" ");
		if (values.length < 2) {
			logger.error("Missing values in voms auth info for App = " + appID + ".");
			return false;
		}
		String vomsAddress = values[0];
		String vomsGroup = values[1];
		logger.debug("Checking: " + userIdentity + ", App = " + appID + ", VOMS = " + vomsAddress + ", VOMS Group = "
				+ vomsGroup + ".");
		try {
			URL url = new URL(vomsAddress);
			VOMSAdminSoapBindingStub vomsAdmin = new VOMSAdminSoapBindingStub(url, null);
			logger.debug("Connected to the VOMS service, checking if user belongs to the app group...");
			User[] users = vomsAdmin.listMembers(vomsGroup);
			for (User u : users)
				// TODO maybe check something else as well?
				// perhaps use vomsAdmin.listGroups(username, userca) and
				// compare groups?
				if (u.getDN().equals(userIdentity)) {
					logger.info("User found in the VOMS App group.");
					return true;
				}
		} catch (MalformedURLException e) {
			logger.error("Bad VOMS address.", e);
			return false;
		} catch (VOMSException e) {
			logger.error("Unable to execute VOMS query.", e);
			return false;
		} catch (AxisFault e) {
			logger.error("Unable to connect to the VOMS service.", e);
			return false;
		} catch (RemoteException e) {
			logger.error("Unable to execute VOMS query.", e);
			return false;
		}
		logger.warn("User does not belong to the VOMS App group.");
		return false;
	}

	private static Logger logger = Logger.getLogger(VomsUtils.class);
}
