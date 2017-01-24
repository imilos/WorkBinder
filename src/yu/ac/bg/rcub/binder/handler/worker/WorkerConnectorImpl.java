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
package yu.ac.bg.rcub.binder.handler.worker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.handler.client.ClientConnector;
import yu.ac.bg.rcub.binder.handler.client.ClientConnectorImpl;
import yu.ac.bg.rcub.binder.net.SocketWrapper;
import yu.ac.bg.rcub.binder.util.Enums.AccessType;

/**
 * Built-in implementation of <code>WorkerConnector</code> interface.
 * 
 * @author choppa
 * 
 */
public class WorkerConnectorImpl implements WorkerConnector {

	private SocketWrapper workerSW;

	/* Log file for worker handler, test for possible blocking! */
	private PrintWriter workerFout;

	/* worker dispatcher properties file */
	private Properties workerProp;

	/**
	 * Creates a <code>WorkerConnector</code> initiated with the workers socket
	 * wraper, worker dispatchers properties file and log output file handle.
	 * 
	 * @param workerSW
	 *            Workers <code>SocketWrapper</code>.
	 * @param workerFout
	 *            Workers log file reference.
	 * @param workerProp
	 *            Worker dispatchers properties object.
	 */
	public WorkerConnectorImpl(SocketWrapper workerSW, PrintWriter workerFout, Properties workerProp) {
		this.workerSW = workerSW;
		this.workerFout = workerFout;
		this.workerProp = workerProp;
	}

	private void closeSocket() {
		try {
			workerSW.close();
		} catch (IOException e) {
			log("Error closing socket.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * yu.ac.bg.rcub.binder.handler.worker.WorkerConnector#log(java.lang.Object)
	 */
	public void log(Object message) {
		log(message, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * yu.ac.bg.rcub.binder.handler.worker.WorkerConnector#log(java.lang.Object,
	 * java.lang.Throwable)
	 */
	public void log(Object message, Throwable t) {
		if (workerFout != null) {
			workerFout.println(message);
			if (t != null)
				t.printStackTrace(workerFout);
			workerFout.flush();
		}
	}

	public InputStream getInputStream() throws BinderCommunicationException {
		try {
			return (workerSW.getProtocolExchange().getAccessType() == AccessType.BINDER || workerSW.getProtocolExchange()
					.getAccessType() == AccessType.DIRECT) ? workerSW.getSocket().getInputStream() : null;
		} catch (IOException e) {
			log(e, e);
			closeSocket();
			throw new BinderCommunicationException("Error occurred while accessing the InputStream.", e);
		}
	}

	public OutputStream getOutputStream() throws BinderCommunicationException {
		try {
			return (workerSW.getProtocolExchange().getAccessType() == AccessType.BINDER || workerSW.getProtocolExchange()
					.getAccessType() == AccessType.DIRECT) ? workerSW.getSocket().getOutputStream() : null;
		} catch (IOException e) {
			log(e, e);
			closeSocket();
			throw new BinderCommunicationException("Error occurred while accessing the OutputStream.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yu.ac.bg.rcub.binder.handler.worker.WorkerConnector#getAccessType()
	 */
	public AccessType getAccessType() {
		return workerSW.getProtocolExchange().getAccessType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * yu.ac.bg.rcub.binder.handler.worker.WorkerConnector#getClientAccessString
	 * ()
	 */
	public String getClientAccessString() {
		return workerSW.getProtocolExchange().getClientAccessString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeyu.ac.bg.rcub.binder.handler.worker.WorkerConnector#
	 * getWorkerExternalParameters()
	 */
	public String getWorkerExternalParameters() {
		return workerProp.getProperty(workerSW.getProtocolExchange().getClientApplicationID() + "_Parameters", "");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * yu.ac.bg.rcub.binder.handler.worker.WorkerConnector#createClientConnector
	 * (java.util.Properties)
	 */
	public ClientConnector createClientConnector(Properties prop) throws BinderCommunicationException {
		return createClientConnector(prop, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * yu.ac.bg.rcub.binder.handler.worker.WorkerConnector#createClientConnector
	 * (java.util.Properties, boolean)
	 */
	public ClientConnector createClientConnector(Properties prop, boolean delegateCredentials)
			throws BinderCommunicationException {
		/*
		 * In case some of the properties fields are missing we read them from
		 * the worker properties and exchange protocol.
		 */
		if (!prop.containsKey("BinderAddress"))
			prop.setProperty("BinderAddress", workerProp.getProperty("BinderAddress"));
		if (!prop.containsKey("BinderPort"))
			prop.setProperty("BinderPort", workerProp.getProperty("BinderPort"));
		if (!prop.containsKey("ApplicationID"))
			prop.setProperty("ApplicationID", workerSW.getProtocolExchange().getClientApplicationID());
		if (!prop.containsKey("UseSSL"))
			prop.setProperty("UseSSL", workerProp.getProperty("UseSSL", "yes"));

		// TODO Specify new proxy in properties!!!
		return new ClientConnectorImpl(prop);

		// return (delegateCredentials) ? new ClientConnectorImpl(prop,
		// workerSW.getProtocolExchange().getClientAuth())
		// : new ClientConnectorImpl(prop);
	}

}
