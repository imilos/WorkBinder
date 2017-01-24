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

import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderCommunicationException;

public class ProxyBinderHandler implements BinderHandler {

	/*
	 * Implementation using low level access with the ProxyThread. It might
	 * destroy data streams of the socket wrappers.
	 */
	public void run(BinderConnector binderConn) {

		DataInputStream clientIS = null;
		DataOutputStream workerOS = null;
		DataInputStream workerIS = null;
		DataOutputStream clientOS = null;
		try {
			clientIS = new DataInputStream(binderConn.getClientInputStream());
			workerOS = new DataOutputStream(binderConn.getWorkerOutputStream());
			workerIS = new DataInputStream(binderConn.getWorkerInputStream());
			clientOS = new DataOutputStream(binderConn.getClientOutputStream());
		} catch (BinderCommunicationException e) {
			logger.error("Error obtaining data streams.", e);
			return;
		}

		logger.debug("Data streams obtained, starting proxy threads.");

		Thread cs = new ProxyThread(clientIS, workerOS);
		Thread sc = new ProxyThread(workerIS, clientOS);

		cs.start();
		sc.start();
		/* wait for communication to finish */
		while ((!cs.getState().name().equalsIgnoreCase("TERMINATED")) && (!sc.getState().name().equalsIgnoreCase("TERMINATED")))
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ie) {
				logger.error("Error while waiting", ie);
			}
	}

	static Logger logger = Logger.getLogger(ProxyBinderHandler.class);
}
