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
package yu.ac.bg.rcub.binder.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;

import org.glite.security.trustmanager.ContextWrapper;

public class BinderSocketFactory {

	// 2 instances because trustmanager doesnt allow same ContextWrapper
	// to be used for server sockets and client sockets

	// ContextWrapper should be used in a light weight manner, i.e.
	// use factories for key managers and trust managers!
	private ContextWrapper clientContext;
	private ContextWrapper serverContext;

	private Properties config;
	public final boolean USE_SSL;
	private boolean requireClientAuth;
	private static final String KEY_ALG = "RSA";

	public BinderSocketFactory(Properties config) {

		USE_SSL = config.getProperty("UseSSL", "yes").equalsIgnoreCase("yes");

		if (!USE_SSL)
			// dont need wrappers
			return;

		requireClientAuth = config.getProperty("RequireClientAuth", "yes").equalsIgnoreCase("yes");
		this.config = config;
	}

	public Socket createSocket(String host, int port) throws UnknownHostException, SSLException, IOException,
			GeneralSecurityException {

		
		if (!USE_SSL)
		{
			return new Socket(host, port);
		}
			
		
		if (clientContext == null){
			
			clientContext = new ContextWrapper(config);
			
		}
		
		// do we require client auth for client sockets?
		return clientContext.getSocketFactory().createSocket(host, port);
	}

	public ServerSocket createServerSocket(int port) throws SSLException, IOException, GeneralSecurityException {
		if (!USE_SSL)
			return new ServerSocket(port);

		if (serverContext == null)
			serverContext = new ContextWrapper(config);

		SSLServerSocket serverSocket = (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(port);
		if (requireClientAuth)
			serverSocket.setNeedClientAuth(requireClientAuth);
		return serverSocket;
	}

	public X509Certificate[] getClientCertificateChain() {
		if (clientContext == null)
			return null;

		String alias = clientContext.getKeyManager().getClientAliases(KEY_ALG, null)[0];
		return clientContext.getKeyManager().getCertificateChain(alias);
	}

	public PrivateKey getClientPrivateKey() {
		if (clientContext == null)
			return null;

		String alias = clientContext.getKeyManager().getClientAliases(KEY_ALG, null)[0];
		return clientContext.getKeyManager().getPrivateKey(alias);
	}

	public X509Certificate[] getServerCertificateChain() {
		if (serverContext == null)
			return null;

		String alias = serverContext.getKeyManager().getServerAliases(KEY_ALG, null)[0];
		return serverContext.getKeyManager().getCertificateChain(alias);
	}

	public PrivateKey getServerPrivateKey() {
		if (serverContext == null)
			return null;

		String alias = serverContext.getKeyManager().getServerAliases(KEY_ALG, null)[0];
		return serverContext.getKeyManager().getPrivateKey(alias);
	}
}
