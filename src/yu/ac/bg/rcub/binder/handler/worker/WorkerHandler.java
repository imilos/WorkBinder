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

/**
 * A simple interface that provides the functionality to implement a specific
 * application communication handler between the worker and the client.
 * <p>
 * 
 * @author choppa
 * 
 */
public interface WorkerHandler {

	/**
	 * A method that implements the communication between the worker and the
	 * client. Depending on clients choice this communication can be direct or
	 * via binder.
	 * 
	 * @param workerConnector
	 *            Provides access to the communication interface between the
	 *            worker and the client.
	 */
	public void run(WorkerConnector workerConnector);

}
