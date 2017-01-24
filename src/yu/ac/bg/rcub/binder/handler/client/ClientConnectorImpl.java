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
package yu.ac.bg.rcub.binder.handler.client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.glite.security.trustmanager.ContextWrapper;
import org.glite.voms.contact.UserCredentials;
import org.glite.voms.contact.VOMSProxyInit;
import org.glite.voms.contact.VOMSRequestOptions;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.CEInfo;
import yu.ac.bg.rcub.binder.net.BinderSocketFactory;
import yu.ac.bg.rcub.binder.net.ProtocolExchange;
import yu.ac.bg.rcub.binder.net.SocketWrapper;
import yu.ac.bg.rcub.binder.util.Enums.AccessType;
import yu.ac.bg.rcub.binder.util.Enums.ConnectionType;

/**
 * Built-in implementation of <code>ClientConnector</code> interface. If SSL is
 * used and a certificate is provided it will generate a proxy certificate.
 * Existing proxy certificate can also be used.
 * <p>
 * NOTE: Entire proxy certificate (along with the proxy key) will be transmitted
 * over SSL to the binder (and the worker).
 * 
 * @author choppa
 * 
 */
public class ClientConnectorImpl implements ClientConnector {

	private SocketWrapper clientSW = null;
	private ServerSocket serverSocket = null;
	private Properties properties = null;
	public static final int PROTOCOL_VERSION = BinderUtil.PROTOCOL_VERSION;
	private static final int CONNECTION_TIMEOUT = 2000;
	private boolean socketAvailable = false;
	private boolean createProxy = false;
	private BinderSocketFactory socketFactory;

	/**
	 * Creates a <code>ClientConnector</code> initiated with the client
	 * properties and the external credentials.
	 * 
	 * @param prop
	 *            The properties object containing various client options.
	 * @throws BinderCommunicationException
	 * 
	 */
	public ClientConnectorImpl(Properties prop) throws BinderCommunicationException {
		this.properties = prop;

		boolean useSSL = prop.getProperty("UseSSL", "yes").equalsIgnoreCase("yes");
		if (useSSL) {
			if (prop.getProperty(ContextWrapper.CREDENTIALS_STORE_FILE) != null
					|| (prop.getProperty(ContextWrapper.CREDENTIALS_CERT_FILE) != null && prop
							.getProperty(ContextWrapper.CREDENTIALS_KEY_FILE) != null))
				createProxy = true;
		}
		try {
			initSocketFactory();
		} catch (Exception e) {
			throw new BinderCommunicationException("Error initializing socket factory", e);
		}
	}

	private void initSocketFactory() throws NoSuchAlgorithmException, CertificateException, IOException {
		if (!createProxy) {
			socketFactory = new BinderSocketFactory(properties);
		} else {// need to create proxy first
			String proxy = createProxy();
			if (logger.isTraceEnabled()) {
				logger.trace("Generated proxy stream: " + proxy);
			}
			System.setProperty(ContextWrapper.GRID_PROXY_STREAM, proxy);
			// clear certs so that generated proxy is read
			properties.remove(ContextWrapper.CREDENTIALS_CERT_FILE);
			properties.remove(ContextWrapper.CREDENTIALS_STORE_FILE);
			socketFactory = new BinderSocketFactory(properties);
		}
	}

	private String createProxy() throws IOException {
		logger.debug("Creating proxy...");

		// setup dirs for voms api
		String certDir = properties.getProperty(ContextWrapper.TRUSTSTORE_DIR, ContextWrapper.TRUSTSTORE_DIR_DEFAULT);
		System.setProperty("CADIR", certDir);
		String vomsDir = properties.getProperty("VomsDir", "/etc/grid-security/vomsdir");
		System.setProperty("VOMSDIR", vomsDir);
		String gliteLoc = properties.getProperty("GliteLoc", "/opt/glite");
		System.setProperty("GLITE_LOCATION", gliteLoc);

		String keystore = properties.getProperty(ContextWrapper.CREDENTIALS_STORE_FILE);
		if (keystore != null) {
			// only pkcs12 is supported for now
			System.setProperty("PKCS12_USER_CERT", keystore);
			System.setProperty("PKCS12_USER_KEY_PASSWORD", properties.getProperty(ContextWrapper.CREDENTIALS_STORE_PASSWD));
		} else {// try pem
			System.setProperty("X509_USER_CERT", properties.getProperty(ContextWrapper.CREDENTIALS_CERT_FILE));
			System.setProperty("X509_USER_KEY", properties.getProperty(ContextWrapper.CREDENTIALS_KEY_FILE));
			System.setProperty("X509_USER_KEY_PASSWORD", properties.getProperty(ContextWrapper.CREDENTIALS_KEY_PASSWD));
		}

		VOMSProxyInit vomsProxyInit = VOMSProxyInit.instance();
		// dont save proxy to the file
		vomsProxyInit.setProxyOutputFile(null);
		Collection<VOMSRequestOptions> requests = new ArrayList<VOMSRequestOptions>();
		VOMSRequestOptions option = new VOMSRequestOptions();
		option.setVoName(properties.getProperty("VOName"));
		option.setLifetime(Integer.valueOf(properties.getProperty("ProxyLifetime", VOMSRequestOptions.DEFAULT_LIFETIME + "")));
		String roles = properties.getProperty("Roles");
		// comma seperated list of roles, need to be parsed...
		if (roles != null)
			option.addFQAN(roles);
		requests.add(option);
		logger.debug("Contacting voms server...");
		UserCredentials vomsproxy = vomsProxyInit.getVomsProxy(requests);
		logger.debug("Created proxy");
		ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
		vomsproxy.save(bytestream);
		return bytestream.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * yu.ac.bg.rcub.binder.handler.client.ClientConnector#executeCEListMatch()
	 */
	public CEInfo[] executeCEListMatch() throws BinderCommunicationException {
		logger.info("Initiating connection to the binder in order to execute the query.");
		Socket socket = initConn();
		logger.info("Connection established.");
		/*
		 * We use local SocketWrapper in case there is some client-worker
		 * connection already established.
		 */
		CEInfo[] ceInfo = null;
		SocketWrapper clientSW = null;
		ProtocolExchange protocolExchange = new ProtocolExchange();
		try {
			clientSW = new SocketWrapper(socket, protocolExchange);
			DataInputStream in = clientSW.getIs();
			exchangeProtocolHeader(clientSW, ConnectionType.CE_QUERY);

			ceInfo = protocolExchange.receiveCEQueryResult(in);
		} catch (IOException e) {
			logger.error("I/O error occured, disconnecting...", e);
			closeSocket(socket);
			throw new BinderCommunicationException("I/O error occured, disconnecting...", e);
		} catch (Exception e) {
			logger.error("Error occured while proccesing query.", e);
			closeSocket(socket);
			throw new BinderCommunicationException("Error occured while proccesing query.", e);
		}
		/* After the result is received we disconnect. */
		logger.debug("Query finished, disconnecting from the binder.");
		closeSocket(socket);
		return ceInfo;
	}

	private Socket initConn() throws BinderCommunicationException {
		Socket socket = null;
		try {
			String address = properties.getProperty("BinderAddress");
			int port = Integer.valueOf(properties.getProperty("BinderPort", "4566"));
			socket = socketFactory.createSocket(address, port);
			socket.setTcpNoDelay(true);
		} catch (UnknownHostException e) {
			logger.error("Unknown host", e);
			closeSocket(socket);
			throw new BinderCommunicationException("Binder host not found", e);
		} catch (IOException e) {
			logger.error("I/O error occured, disconnecting", e);
			closeSocket(socket);
			throw new BinderCommunicationException("Error occured while initiating connection to the binder", e);
		} catch (GeneralSecurityException e) {
			logger.error("Security error occured, unable to connect to the service", e);
			closeSocket(socket);
			throw new BinderCommunicationException("Security error occured, unable to connect to the service", e);
		}
		return socket;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yu.ac.bg.rcub.binder.handler.client.ClientConnector#connect()
	 */
	public void connect() throws BinderCommunicationException {
		logger.info("Initiating connection to the binder.");
		Socket socket = initConn();
		logger.info("Connection established.");
		try {
			ProtocolExchange protocolExchange = new ProtocolExchange();
			clientSW = new SocketWrapper(socket, protocolExchange);
			DataOutputStream out = clientSW.getOs();
			DataInputStream in = clientSW.getIs();
			exchangeProtocolHeader(clientSW, ConnectionType.CLIENT);

			initServerSocket(protocolExchange);
			/* access type is determined when worker info is read */
			readWorkerInfo(in, out);
			logger.debug("Client exchanged headers with the binder.");

			switch (clientSW.getProtocolExchange().getAccessType()) {
			case BINDER:
				logger.debug("Communication via binder chosen.");
				/* Socket is available now */
				socketAvailable = true;
				break;
			case DIRECT:
				logger.debug("Attempting to establish a direct connection to the worker...");
				try {
					/* close the socket to the binder */
					clientSW.close();
					if (serverSocket == null)
						throw new IOException("Unable to open a listening socket.");

					socket = serverSocket.accept();
					socket.setTcpNoDelay(true);
					/* establish a new socket to the worker */
					clientSW = new SocketWrapper(socket, protocolExchange);
					/* Socket is available now */
					socketAvailable = true;
					logger.debug("Connection with the worker established.");
				} catch (IOException e) {
					logger.error("Error while trying to connect to the worker.");
					socket.close(); /* check? */
					throw e;
				}
				break;
			case CUSTOM:
				logger.debug("Custom communication chosen, disconnecting from binder...");
				clientSW.close();
				break;
			case UNKNOWN:
				logger.error("Unsupported communication chosen, disconnecting from binder...");
				clientSW.close();
				throw new IllegalArgumentException("Unsupported communication chosen, disconnecting from binder...");
			}

		} catch (IOException e) {
			logger.error("I/O error occured, disconnecting...", e);
			disconnect();
			throw new BinderCommunicationException("I/O error occured, disconnecting...", e);
		} catch (BinderCommunicationException e) {
			disconnect();
			logger.error(e);
			throw e;
		} catch (Exception e) {
			logger.error("Error occured while trying to connect.", e);
			disconnect();
			throw new BinderCommunicationException("Error occured while trying to connect.", e);
		}
	}

	private void initServerSocket(ProtocolExchange protocolExchange) {
		/*
		 * Simple workaround to init server socket before worker tries to
		 * connect if direct connection is chosen. Check of access type is
		 * incomplete, but must be done in order to accept connection properly.
		 * 
		 * Note: worker access string is ""!
		 */
		protocolExchange.setClientAccessString(properties.getProperty("AccessString", ""));
		if (protocolExchange.getAccessType() == AccessType.DIRECT) {
			try {
				// serverSocket = new
				// ServerSocket(protocolExchange.getClientHostPort());
				serverSocket = socketFactory.createServerSocket(protocolExchange.getClientHostPort());
			} catch (Exception e) {
				serverSocket = null;
			}
		}
	}

	private void exchangeProtocolHeader(SocketWrapper clientSW, ConnectionType connType) throws IOException,
			GeneralSecurityException {
		Socket socket = clientSW.getSocket();
		DataOutputStream out = clientSW.getOs();
		ProtocolExchange protocolExchange = clientSW.getProtocolExchange();
		/* protocol version */
		protocolExchange.setClientProtocolVersion(PROTOCOL_VERSION);
		/* connection type (0 - client; 1 - worker) */
		protocolExchange.setConnectionType(connType);
		// /* server selection hint */
		// protocolExchange.setServerSelectionHint(properties.getProperty("ServerSelectionHint",
		// "ANY"));

		/* candidate CEs */
		protocolExchange.setClientCandidateCEs(properties.getProperty("CandidateCE", ""));
		/* application ID */
		protocolExchange.setClientApplicationID(properties.getProperty("ApplicationID"));
		/* accessString describing connection between client and worker */
		protocolExchange.setClientAccessString(properties.getProperty("AccessString", ""));
		/* required wall clock time */
		protocolExchange.setClientRequiredWallClockTime(Long.valueOf(properties.getProperty("RequiredWallClockTime")));
		/* client credentials data */
		protocolExchange.setClientProxyKeyData(getEncodedProxyKey());
		// dont send certificate, it will be sent by SSL
		protocolExchange.setClientProxyCertData(new byte[0][]);
		/* routing info */
		String routingInfo = (protocolExchange.getClientAccessString().equalsIgnoreCase("")) ? "\n\tClient => "
				+ socket.getInetAddress().getCanonicalHostName() + " : " + socket.getLocalPort()
				: "\n\tClient => accessString = " + protocolExchange.getClientAccessString();
		protocolExchange.setClientRoutingInfo(routingInfo);

		protocolExchange.sendClientHeader(out);
	}

	private byte[] getEncodedProxyKey() throws IOException {
		PrivateKey key = socketFactory.getClientPrivateKey();
		if (key != null)
			return key.getEncoded();
		else
			return new byte[0];
		// BouncyCastleOpenSSLKey key = new
		// BouncyCastleOpenSSLKey(userCred.getPrivateKey());
		// ByteArrayOutputStream output = new ByteArrayOutputStream();
		// key.writeTo(output);
		// return output.toByteArray();
	}

	private void closeSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			logger.error("Error closing socket.", e);
		}
	}

	private void readWorkerInfo(DataInputStream in, DataOutputStream out) throws EOFException, IOException,
			BinderCommunicationException {
		ProtocolExchange protocolExchange = clientSW.getProtocolExchange();
		/* Maybe check the connection type to make sure worker responded. */
		protocolExchange.receiveHeader(in);
		protocolExchange.receiveWorkerResponse(in);
		logger.debug("Routing info received from worker: " + protocolExchange.getWorkerRoutingInfo());

		String workerErrorDesc = protocolExchange.getWorkerErrorDescription();
		if (!workerErrorDesc.equals("")) {
			logger.error("Error description received from worker: \n" + workerErrorDesc);
			throw new BinderCommunicationException(workerErrorDesc);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yu.ac.bg.rcub.binder.handler.client.ClientConnector#disconnect()
	 */
	public void disconnect() throws BinderCommunicationException {
		/* Socket is not available after disconnecting. */
		socketAvailable = false;
		try {
			if (clientSW != null)
				clientSW.close();
			if (serverSocket != null)
				serverSocket.close();
			logger.info("Disconnected from binder.");
		} catch (IOException e) {
			logger.error("Error occured while disconnecting.", e);
			throw new BinderCommunicationException("Error occured while disconnecting.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * yu.ac.bg.rcub.binder.handler.client.ClientConnector#getWorkerRoutingInfo
	 * ()
	 */
	public String getWorkerRoutingInfo() {
		return clientSW.getProtocolExchange().getWorkerRoutingInfo();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * yu.ac.bg.rcub.binder.handler.client.ClientConnector#getWorkerErrorDesc()
	 */
	public String getWorkerErrorDesc() {
		return clientSW.getProtocolExchange().getWorkerErrorDescription();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yu.ac.bg.rcub.binder.handler.client.ClientConnector#getRoutingInfo()
	 */
	public String getRoutingInfo() {
		return clientSW.getProtocolExchange().getClientRoutingInfo();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yu.ac.bg.rcub.binder.handler.client.ClientConnector#isIOAvailable()
	 */
	public boolean isIOAvailable() {
		return socketAvailable;
	}

	public InputStream getInputStream() throws BinderCommunicationException {
		try {
			return socketAvailable ? clientSW.getSocket().getInputStream() : null;
		} catch (IOException e) {
			logger.error(e, e);
			disconnect(); /* needed? */
			throw new BinderCommunicationException("Error occurred while accessing the InputStream.", e);
		}
	}

	public OutputStream getOutputStream() throws BinderCommunicationException {
		try {
			return socketAvailable ? clientSW.getSocket().getOutputStream() : null;
		} catch (IOException e) {
			logger.error(e, e);
			disconnect(); /* needed? */
			throw new BinderCommunicationException("Error occurred while accessing the OutputStream.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yu.ac.bg.rcub.binder.handler.client.ClientConnector#getAccessType()
	 */
	public AccessType getAccessType() {
		return clientSW.getProtocolExchange().getAccessType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * yu.ac.bg.rcub.binder.handler.client.ClientConnector#getWorkerAccessString
	 * ()
	 */
	public String getWorkerAccessString() {
		return clientSW.getProtocolExchange().getWorkerAccessString();
	}

	private static Logger logger = Logger.getLogger(ClientConnectorImpl.class);

}
