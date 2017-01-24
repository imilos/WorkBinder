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

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;
import org.glite.security.util.CertUtil;
import org.glite.security.util.DNHandler;

import yu.ac.bg.rcub.binder.eventlogging.Event;
import yu.ac.bg.rcub.binder.handler.BinderHandlerThread;
import yu.ac.bg.rcub.binder.job.WorkerJob;
import yu.ac.bg.rcub.binder.net.ProtocolExchange;
import yu.ac.bg.rcub.binder.net.SocketWrapper;
import yu.ac.bg.rcub.binder.security.CertUtils;
import yu.ac.bg.rcub.binder.security.VomsUtils;
import yu.ac.bg.rcub.binder.util.Enums.ConnectionType;
import yu.ac.bg.rcub.binder.util.Enums.JobStatus;

/**
 * A thread started each time the binder receives incoming connection from the
 * worker or the client.
 * <p>
 * Modified to support glite-trustmanager. Client is expected to connect with a
 * proxy certificate and then transfer this certificate to the binder.
 * Authentication is done by the trustmanager, authorization by contacting VOMS.
 * <p>
 * TODO Maybe use voms java api to read roles from the proxy.
 * 
 * @author choppa
 * 
 */
public class BinderListenerThread extends Thread {

	private Socket socket = null;
	private ProtocolExchange protocolExchange = null;
	private BinderPool binderPool = null;
	private SocketWrapper sw = null;
	private final long startUpTime = System.currentTimeMillis();

	public BinderListenerThread(Socket socket, BinderPool binderPool) throws IOException {
		super("ListenerThread");
		this.socket = socket;
		this.protocolExchange = new ProtocolExchange();
		this.sw = new SocketWrapper(socket, protocolExchange);
		this.binderPool = binderPool;
	}

	private void exchangeProtocolHeader() throws EOFException, IOException, BinderCommunicationException,
			GeneralSecurityException {
		protocolExchange.receiveHeader(sw.getIs());
		
		switch (protocolExchange.getConnectionType()) {
		case CLIENT:
			processClient();
			break;
		case WORKER:		
			processWorker();
			break;
		case CE_QUERY:
			processCEQuery();
			break;
		}
	}

	/**
	 * Queries the binder to report the available CEs and number of available
	 * jobs on each CE.
	 * <p>
	 * After the response is sent, connection is closed.
	 * 
	 * @throws IOException
	 * @throws BinderCommunicationException
	 * @throws GeneralSecurityException
	 */
	private void processCEQuery() throws IOException, BinderCommunicationException, GeneralSecurityException {
		/* check protocol version */
		if (protocolExchange.getClientProtocolVersion() != BinderUtil.PROTOCOL_VERSION)
			logger.warn("Protocol version not valid: " + protocolExchange.getClientProtocolVersion());

		SocketWrapper clientSW = sw;
		if (BinderUtil.isPerfMonEnabled())
			Event.clientConnected(clientSW, 1);
		/* do we want to authorize the client on query as well? */
		authorizeClient(clientSW);
		logger.info("Processing CE_QUERY for " + protocolExchange.getClientApplicationID() + ".");
		CEInfo[] ceInfo = binderPool.executeCEQuery(clientSW);
		/* Report back to the client query results and disconnect. */
		protocolExchange.sendCEQueryResult(clientSW.getOs(), ceInfo);
		if (BinderUtil.isPerfMonEnabled())
			Event.clientDisconnected(clientSW, System.currentTimeMillis() - startUpTime, 1);
		clientSW.close();
	}

	/**
	 * Client connected to the binder.
	 * 
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * @throws BinderCommunicationException
	 */
	private void processClient() throws IOException, GeneralSecurityException, BinderCommunicationException {
		/* check protocol version */
		if (protocolExchange.getClientProtocolVersion() != BinderUtil.PROTOCOL_VERSION)
			logger.warn("Protocol version not valid: " + protocolExchange.getClientProtocolVersion());

		SocketWrapper clientSW = sw;
		if (BinderUtil.isPerfMonEnabled())
			Event.clientConnected(clientSW, 0);

		authorizeClient(clientSW);

		WorkerJob wj = binderPool.getWorker(clientSW);
		if (wj == null) {
			// logger.warn("Client ServerSelectionHint: " +
			// protocolExchange.getServerSelectionHint());
			logger.warn("Client candidateCEs: " + protocolExchange.getClientCandidateCEs() + ".");
			logger.warn("Could not find appropriate worker job in the pool. Client socket in closed.");
			if (BinderUtil.isPerfMonEnabled())
				Event.clientDisconnected(clientSW, System.currentTimeMillis() - startUpTime, 10);
			throw new BinderCommunicationException("No ready jobs available");
			// clientSW.close();
		} else { /* client and worker matched */
			logger.debug("Worker and client matched. Starting handler thread...");
			if (BinderUtil.isPerfMonEnabled())
				Event.clientMatched(clientSW, wj.getSw());
			/* increase the number of clients and update strategies */
			binderPool.updateClientNumber(1);
			/* start handler thread, it will decrease the number of clients */
			new BinderHandlerThread(clientSW, wj, binderPool, startUpTime).start();
		}
	}

	private void authorizeClient(SocketWrapper clientSW) throws IOException, BinderCommunicationException,
			CertificateEncodingException {
		// if SSL is not used, cant authorize
		if (!BinderUtil.SSLEnabled())
			return;

		switch (BinderUtil.getAuthType()) {
		case NONE:
			logger.debug("No authentication used, client accepted.");
			break;
		case VOMS:
			// SSL is used and client auth was required by the service,
			// so we should be able to obtain users cert chain
			SSLSocket socket = (SSLSocket) clientSW.getSocket();

			X509Certificate[] certs = (X509Certificate[]) socket.getSession().getPeerCertificates();
			X509Certificate clientCert = certs[CertUtil.findClientCert(certs)];
			String userIdentity = DNHandler.getDN(clientCert.getSubjectX500Principal()).getX500();

			logger.info("Checking VOMS App group for: " + userIdentity);
			if (!VomsUtils.verifyUserGroup(userIdentity, clientSW.getProtocolExchange().getClientApplicationID())) {
				throw new BinderCommunicationException("Client didn't pass the VOMS authorization");
			}
			logger.info("Client (" + userIdentity + ") passed authorization.");
			// copy clients cert chain to client header
			clientSW.getProtocolExchange().setClientProxyCertData(CertUtils.getEncodedCertsData(certs));
		}
	}

	/**
	 * Worker connected to the binder.
	 * 
	 * @throws EOFException
	 * @throws IOException
	 */
	private void processWorker() throws EOFException, IOException {
		/* check protocol version */
		if (protocolExchange.getWorkerProtocolVersion() != BinderUtil.PROTOCOL_VERSION)
			logger.warn("Protocol version not valid: " + protocolExchange.getWorkerProtocolVersion());

		SocketWrapper workerSW = sw;
		if (protocolExchange.getWorkerJobStatus() == JobStatus.FINISHED) {
			logger.debug("WorkerJob (JobID:" + protocolExchange.getWorkerJobID()
					+ ") informed that it has finished, it will be removed.");
			binderPool.removeWorkerJob(workerSW);
			if (BinderUtil.isPerfMonEnabled())
				Event.jobArrival(null, workerSW, 0, 10);
		} else {
			if (!binderPool.addWorkerJob(workerSW)) {
				/* Unknown job connected to the binder. */
				logger.warn("WorkerJob not added to the pool. Worker socket is closed.");
				workerSW.close();
			}
		}
	}

	/** Closes a socket. */
	private void closeSocket() {
		try {
			socket.close();
		} catch (IOException e) {
			logger.error("Error while closing socket.", e);
		}
	}

	public void run() {
		logger.debug("Binder listener thread started...");
		try {
			exchangeProtocolHeader();
		} catch (EOFException e) {
			logger.info("EOF reached, ending listener thread.", e);
			closeSocket();
		} catch (IOException e) {
			logger.error("Error while reading protocol header. This request is skiped and socket is closed.", e);
			closeSocket();
		} catch (BinderCommunicationException e) {
			/* auth failed or no ready jobs for the client */
			
			logger.warn(e);
			if (BinderUtil.isPerfMonEnabled())
				/* extra check, just in case */
				if (protocolExchange != null && protocolExchange.getConnectionType() == ConnectionType.CLIENT
						|| protocolExchange.getConnectionType() == ConnectionType.CE_QUERY) {

					Event.clientDisconnected(sw, System.currentTimeMillis() - startUpTime, 11);
				}
			try {
				if (socket.isConnected()) {
					// send an empty header
					protocolExchange.sendWorkerHeader(sw.getOs());
					protocolExchange.setWorkerErrorDescription(e.getMessage());
					protocolExchange.sendWorkerResponse(sw.getOs());
				}
			} catch (IOException ioe) {
				// ignore
			} finally {
				closeSocket();
			}

		} catch (Exception e) {
			logger.error(e, e);
			closeSocket();
		}
		logger.debug("Binder listener thread ended.");
	}

	Logger logger = Logger.getLogger(BinderListenerThread.class);

}
