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
package yu.ac.bg.rcub.binder.handler.mpi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.CEInfo;
import yu.ac.bg.rcub.binder.handler.client.ClientConnector;
import yu.ac.bg.rcub.binder.handler.client.ClientConnectorImpl;
import yu.ac.bg.rcub.binder.util.Enums.AccessType;

/**
 * Incomplete, but functional version of ClientConnector that supports MPI. It
 * will be used until binder protocol gets updated to support multiple job
 * request.
 * 
 * @author choppa
 * 
 */
public class MPIClientConnector implements ClientConnector {
	// private Properties prop = null;
	// private boolean externalAuth = false;
	// private AuthContext auth = null;
	private int numNodes;
	private ClientConnector[] clientNodes;

	/**
	 * Creates a <code>MPIClientConnector</code> initiated with the client
	 * properties and the external credentials.
	 * 
	 * @param prop
	 *            The properties object containing various client options.
	 * @throws BinderCommunicationException
	 * 
	 */
	public MPIClientConnector(Properties prop) throws BinderCommunicationException {
		// this.prop = prop;
		// this.externalAuth = auth != null;
		// this.auth = auth;
		this.numNodes = Integer.parseInt(prop.getProperty("NumNodes", "1"));
		if (numNodes < 1)
			throw new IllegalArgumentException("NumNodes must be a positive integer (" + numNodes + ")");
		String targetCE = prop.getProperty("CandidateCE", "").trim();
		// Quick & dirty solution to make sure all nodes are on same CE
		// TODO protocol change is required to make this look good!
		if (targetCE.equals("") || targetCE.contains(" "))
			throw new IllegalArgumentException("Exactly one target CE must be specified ('" + targetCE + "')");

		logger.debug("Trying to fetch " + numNodes + " jobs.");
		clientNodes = new ClientConnector[numNodes];
		for (int i = 0; i < numNodes; i++)
			clientNodes[i] = new ClientConnectorImpl(prop);
	}

	public void connect() throws BinderCommunicationException {
		for (int i = 0; i < numNodes; i++) {
			clientNodes[i].connect();
			try {
				/* notify who is MASTER/SLAVE */
				new DataOutputStream(clientNodes[i].getOutputStream()).writeByte(i == 0 ? Utils.MASTER : Utils.SLAVE);
			} catch (IOException e) {
				logger.error("Unable to receive WN address.", e);
				disconnect();
				throw new BinderCommunicationException("Unable to receive WN address.", e);
			}
		}
		try {
			/* inform master about the number of nodes */
			new DataOutputStream(clientNodes[0].getOutputStream()).writeInt(numNodes);
			/* get the address of the master */
			String masterAddress = BinderUtil.readString(new DataInputStream(clientNodes[0].getInputStream()));
			for (int i = 1; i < numNodes; i++) {
				/* send master address to slaves */
				BinderUtil.writeString(new DataOutputStream(clientNodes[i].getOutputStream()), masterAddress);
			}
		} catch (IOException e) {
			logger.error("Error initiating MPI.", e);
			disconnect();
			throw new BinderCommunicationException("Error initiating MPI.", e);
		}
	}

	public void disconnect() throws BinderCommunicationException {
		for (int i = 0; i < numNodes; i++) {
			clientNodes[i].disconnect();
		}
	}

	public CEInfo[] executeCEListMatch() throws BinderCommunicationException {
		/* execute query using the 1st connector */
		return clientNodes[0].executeCEListMatch();
	}

	public AccessType getAccessType() {
		return clientNodes[0].getAccessType();
	}

	public InputStream getInputStream() throws BinderCommunicationException {
		return clientNodes[0].getInputStream();
	}

	public OutputStream getOutputStream() throws BinderCommunicationException {
		return clientNodes[0].getOutputStream();
	}

	public String getRoutingInfo() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < numNodes; i++) {
			sb.append(i);
			sb.append(". ");
			sb.append(clientNodes[i].getRoutingInfo());
			sb.append("-----------------------------------\n");
		}
		return sb.toString();
	}

	public String getWorkerAccessString() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getWorkerErrorDesc() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getWorkerRoutingInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isIOAvailable() {
		return clientNodes[0].isIOAvailable();
	}

	private static Logger logger = Logger.getLogger(MPIClientConnector.class);
}
