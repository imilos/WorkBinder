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

/**
 * A simple interface that provides the functionality to implement a specific
 * application communication handler between the client and the worker when
 * client choses to communicate via binder (<code>AccessType.BINDER</code>).
 * <p>
 * If it is not implemented, the default implementation will be used. This
 * default implementation acts as a simple proxy relaying data between the
 * client and the worker.
 * 
 * @author choppa
 * 
 */
public interface BinderHandler {

	/**
	 * A method that implements the communication between the client and the
	 * worker via binder.
	 * 
	 * @param binderConnector
	 *            Provides access to the communication interfaces between the
	 *            binder and the client and between the binder and the worker.
	 */
	public void run(BinderConnector binderConnector);

}
