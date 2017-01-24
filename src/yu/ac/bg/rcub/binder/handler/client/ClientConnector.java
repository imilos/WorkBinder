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

import java.io.InputStream;
import java.io.OutputStream;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.CEInfo;
import yu.ac.bg.rcub.binder.util.Enums.AccessType;

/**
 * A utility interface that provides functionalities such as access to the
 * binder and communication primitives between the client and the worker.
 * 
 * @author choppa
 * 
 */
public interface ClientConnector {

	/**
	 * Connects to the binder using the client options described in the
	 * properties in order to obtain the <code>WorkerJob</code>.
	 * <p>
	 * If the client choses direct communication, then the initial connection to
	 * the binder will be closed and a new connection from the worker to the
	 * client will be established.
	 * <p>
	 * If the custom communication is chosen (either by client or worker), then
	 * the initial connection to the binder will be closed. Custom
	 * <code>AccessString</code> from the worker is available in order to
	 * establish custom communiction.
	 * 
	 * @throws BinderCommunicationException
	 */
	public abstract void connect() throws BinderCommunicationException;

	/**
	 * Closes the connection to the binder.
	 * <p>
	 * NOTE: If the client chose a direct communication, calling this method
	 * will close the connection between the worker and the client.
	 * 
	 * @throws BinderCommunicationException
	 */
	public abstract void disconnect() throws BinderCommunicationException;

	/**
	 * Queries the binder with the information provided by the client to get the
	 * information about available CEs.
	 * 
	 * @return The <code>CEInfo[]</code> array containing information about
	 *         matched CEs.
	 * @throws BinderCommunicationException
	 */
	public abstract CEInfo[] executeCEListMatch() throws BinderCommunicationException;;

	/**
	 * Gets the routing info received from the worker.
	 * 
	 * @return A <code>String</code> representing worker routing info.
	 */
	public abstract String getWorkerRoutingInfo();

	/**
	 * Gets the error description received from the worker.
	 * 
	 * @return A <code>String</code> representing worker error description.
	 */
	public abstract String getWorkerErrorDesc();

	/**
	 * Gets the routing info client provided.
	 * 
	 * @return A <code>String</code> representing client routing info.
	 */
	public abstract String getRoutingInfo();

	/**
	 * Gets the <code>InputStream</code> that can be used to receive data from
	 * the remote worker.
	 * 
	 * @return The <code>InputStream</code> used to communicate between the
	 *         client and the worker or <code>null</code> if the stream is not
	 *         available.
	 * @throws BinderCommunicationException
	 *             If an error occurred while accessing the stream.
	 */
	public abstract InputStream getInputStream() throws BinderCommunicationException;

	/**
	 * Gets the <code>OutputStream</code> that can be used to send data to the
	 * remote worker.
	 * 
	 * @return The <code>OutputStream</code> used to communicate between the
	 *         client and the worker or <code>null</code> if the stream is not
	 *         available.
	 * @throws BinderCommunicationException
	 *             If an error occurred while accessing the stream.
	 */
	public abstract OutputStream getOutputStream() throws BinderCommunicationException;

	/**
	 * Checks whether the underlying I/O streams that are used to communicate
	 * between the client and the binder (and worker) are available.
	 * <p>
	 * NOTE: If the communication via binder is chosen by the client, all data
	 * being sent and received by the client will go through the binder before
	 * reaching the worker.
	 * 
	 * @return true if the I/O streams are available.
	 */
	public abstract boolean isIOAvailable();

	/**
	 * Gets the <code>AccessType</code> describing connection between the client
	 * and the worker. It is determined by the access strings that the worker
	 * and the client exchange.
	 * 
	 * @return The <code>AccessType</code> determined by the binder.
	 */
	public abstract AccessType getAccessType();

	/**
	 * Gets the access string from the worker used to describe connection
	 * between the client and the worker.
	 * <p>
	 * In case the binder determined that the custom communication will be used,
	 * this string provides the client with the information on how to connect to
	 * the worker.
	 * 
	 * @return The access string received from the worker.
	 */
	public abstract String getWorkerAccessString();

}
