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
package yu.ac.bg.rcub.binder.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.net.SocketWrapper;

/**
 * Built-in implementation of the <code>BinderConnector</code> interface.
 * 
 * @author choppa
 * 
 */
public class BinderConnectorImpl implements BinderConnector {

	private SocketWrapper clientSW = null;
	private SocketWrapper workerSW = null;

	/**
	 * Creates a <code>BinderConnector</code> initiated with the clients and
	 * workers socket wrappers.
	 * 
	 * @param clientSW
	 *            Clients <code>SocketWrapper</code>.
	 * @param workerSW
	 *            Workers <code>SocketWrapper</code>.
	 */
	public BinderConnectorImpl(SocketWrapper clientSW, SocketWrapper workerSW) {
		this.clientSW = clientSW;
		this.workerSW = workerSW;
	}

	private void closeSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			logger.error("Error closing socket.", e);
		}
	}

	public InputStream getClientInputStream() throws BinderCommunicationException {
		try {
			return clientSW.getSocket().getInputStream();
		} catch (IOException e) {
			logger.error(e, e);
			closeSocket(clientSW.getSocket());
			throw new BinderCommunicationException("Error occured while accessing the client's InputStream.", e);
		}
	}

	public OutputStream getClientOutputStream() throws BinderCommunicationException {
		try {
			return clientSW.getSocket().getOutputStream();
		} catch (IOException e) {
			logger.error(e, e);
			closeSocket(clientSW.getSocket());
			throw new BinderCommunicationException("Error occured while accessing the client's OutputStream.", e);
		}
	}

	public InputStream getWorkerInputStream() throws BinderCommunicationException {
		try {
			return workerSW.getSocket().getInputStream();
		} catch (IOException e) {
			logger.error(e, e);
			closeSocket(workerSW.getSocket());
			throw new BinderCommunicationException("Error occured while accessing the worker's InputStream.", e);
		}
	}

	public OutputStream getWorkerOutputStream() throws BinderCommunicationException {
		try {
			return workerSW.getSocket().getOutputStream();
		} catch (IOException e) {
			logger.error(e, e);
			closeSocket(workerSW.getSocket());
			throw new BinderCommunicationException("Error occured while accessing the worker's OutputStream.", e);
		}
	}

	static Logger logger = Logger.getLogger(BinderConnectorImpl.class);

}
