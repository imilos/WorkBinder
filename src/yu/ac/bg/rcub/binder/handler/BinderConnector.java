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

import java.io.InputStream;
import java.io.OutputStream;

import yu.ac.bg.rcub.binder.BinderCommunicationException;

/**
 * A utility interface that provides functionalities such as access from the
 * binder and communication primitives to the client and the worker. It is
 * intended to be used within the <code>run</code> method of the
 * <code>BinderHandler</code>.
 * <p>
 * Two pairs of communication primitives are provided, one from binder to the
 * client, and another from the binder to the worker.
 * 
 * 
 * @author choppa
 * 
 */
public interface BinderConnector {

	/**
	 * Gets the <code>InputStream</code> that can be used to receive data from
	 * the remote client.
	 * 
	 * @return The <code>InputStream</code> used to communicate with the
	 *         client or <code>null</code> if the stream is not available.
	 * @throws BinderCommunicationException
	 *             If an error occurred while accessing the stream.
	 */
	public abstract InputStream getClientInputStream() throws BinderCommunicationException;

	/**
	 * Gets the <code>OutputStream</code> that can be used to send data to the
	 * remote client.
	 * 
	 * @return The <code>OutputStream</code> used to communicate with the
	 *         client or <code>null</code> if the stream is not available.
	 * @throws BinderCommunicationException
	 *             If an error occurred while accessing the stream.
	 */
	public abstract OutputStream getClientOutputStream() throws BinderCommunicationException;

	/**
	 * Gets the <code>InputStream</code> that can be used to receive data from
	 * the remote worker.
	 * 
	 * @return The <code>InputStream</code> used to communicate with the
	 *         worker or <code>null</code> if the stream is not available.
	 * @throws BinderCommunicationException
	 *             If an error occurred while accessing the stream.
	 */
	public abstract InputStream getWorkerInputStream() throws BinderCommunicationException;

	/**
	 * Gets the <code>OutputStream</code> that can be used to send data to the
	 * remote worker.
	 * 
	 * @return The <code>OutputStream</code> used to communicate with the
	 *         worker or <code>null</code> if the stream is not available.
	 * @throws BinderCommunicationException
	 *             If an error occurred while accessing the stream.
	 */
	public abstract OutputStream getWorkerOutputStream() throws BinderCommunicationException;

}
