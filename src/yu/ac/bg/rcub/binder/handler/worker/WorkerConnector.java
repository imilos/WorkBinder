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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.handler.client.ClientConnector;
import yu.ac.bg.rcub.binder.util.Enums.AccessType;

/**
 * A utility interface that provides communication primitives between the client
 * and the worker. It is intended to be used within the <code>run()</code>
 * method of the <code>WorkerHandler</code>.
 * 
 * @author choppa
 * 
 */
public interface WorkerConnector {

	/**
	 * This method provides functionality to write various debug and other kind
	 * of information on the worker. For each job run on the worker, a unique
	 * file is created and can be later retrieved for debug (or some other)
	 * purposes.
	 * 
	 * @param message
	 *            The message that will be stored.
	 */
	public abstract void log(Object message);

	/**
	 * This method provides functionality to write various debug and other kind
	 * of information on the worker. For each job run on the worker, a unique
	 * file is created and can be later retrieved for debug (or some other)
	 * purposes.
	 * 
	 * @param message
	 *            The message that will be stored.
	 * @param t
	 *            The throwable object whose stacktrace will be stored.
	 */
	public abstract void log(Object message, Throwable t);

	/**
	 * Gets the <code>InputStream</code> that can be used to receive data from
	 * the client.
	 * <p>
	 * NOTE: If the communication via binder is chosen by the client, all data
	 * being sent and received by the worker will go through the binder before
	 * reaching the client.
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
	 * client.
	 * <p>
	 * NOTE: If the communication via binder is chosen by the client, all data
	 * being sent and received by the worker will go through the binder before
	 * reaching the client.
	 * 
	 * @return The <code>OutputStream</code> used to communicate between the
	 *         client and the worker or <code>null</code> if the stream is not
	 *         available.
	 * @throws BinderCommunicationException
	 *             If an error occurred while accessing the stream.
	 */
	public abstract OutputStream getOutputStream() throws BinderCommunicationException;

	/**
	 * Gets the <code>AccessType</code> describing connection between the client
	 * and the worker. It is determined by the access strings that the worker
	 * and the client exchange.
	 * 
	 * @return The <code>AccessType</code> determined by the binder.
	 */
	public abstract AccessType getAccessType();

	/**
	 * Gets the access string from the client used to describe connection
	 * between the client and the worker.
	 * <p>
	 * In case the binder determined that the custom communication will be used,
	 * this string provides the worker with the information on how to connect to
	 * the client.
	 * 
	 * @return The access string received from the client.
	 */
	public abstract String getClientAccessString();

	/**
	 * Gets the external parameters specified for the application chosen by the
	 * client.
	 * 
	 * @return The <code>String</code> containing external parameters.
	 */
	public abstract String getWorkerExternalParameters();

	/**
	 * Creates a new <code>ClientConnector</code> object using the provided
	 * properties object. Properties object can contain various client related
	 * options, but in case some of the options are missing, default values will
	 * be filled in for easier usage.
	 * <p>
	 * This method is provided in order to make things easier for worker jobs
	 * that want to act as clients, or in other words, allocate new worker jobs.
	 * 
	 * @see #createClientConnector(Properties, boolean)
	 * 
	 * @param prop
	 *            <code>Properties</code> object containing various client
	 *            options.
	 * @return A new <code>ClientConnector</code> object.
	 * @throws BinderCommunicationException 
	 */
	public abstract ClientConnector createClientConnector(Properties prop) throws BinderCommunicationException;

	/**
	 * Creates a new <code>ClientConnector</code> object using the provided
	 * properties object. Properties object can contain various client related
	 * options, but in case some of the options are missing, default values will
	 * be filled in for easier usage.
	 * <p>
	 * This method is provided in order to make things easier for worker jobs
	 * that want to act as clients, or in other words, allocate new worker jobs.
	 * <p>
	 * Depending on users choice, new <code>ClientConnector</code> may or may
	 * not use same credentials as the client that has started the worker job.
	 * 
	 * 
	 * @param prop
	 *            <code>Properties</code> object containing various client
	 *            options.
	 * @param delegateCredentials
	 *            If <code>true</code>, new <code>ClientConnector</code> will be
	 *            created with the same credentials as the client that is using
	 *            this worker job.
	 * 
	 * @return A new <code>ClientConnector</code> object.
	 * @throws BinderCommunicationException 
	 */
	public abstract ClientConnector createClientConnector(Properties prop, boolean delegateCredentials) throws BinderCommunicationException;

}
