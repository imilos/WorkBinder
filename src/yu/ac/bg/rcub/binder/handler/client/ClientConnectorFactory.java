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

import java.util.Properties;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.handler.mpi.MPIClientConnector;

/**
 * Factory that can be used to instantiate <code>ClientConnector</code> objects.
 * 
 * @author choppa
 * 
 */
public class ClientConnectorFactory {

	/**
	 * Creates an instance of the <code>ClientConnector</code>. Options are read
	 * from system properties.
	 * 
	 * @return An instance of <code>ClientConnector</code>.
	 * @throws BinderCommunicationException 
	 */
	public static ClientConnector createClientConnector() throws BinderCommunicationException {
		return createClientConnector(System.getProperties());
	}

	/**
	 * Creates an instance of the <code>ClientConnector</code>.
	 * 
	 * @param prop
	 *            The <code>Properties</code> object containing various client
	 *            options.
	 * @return An instance of <code>ClientConnector</code>.
	 * @throws BinderCommunicationException 
	 */
	public static ClientConnector createClientConnector(Properties prop) throws BinderCommunicationException {
		if (prop.getProperty("UseMPI", "no").equalsIgnoreCase("yes"))
			return new MPIClientConnector(prop);
		else
			return new ClientConnectorImpl(prop);
	}

}
