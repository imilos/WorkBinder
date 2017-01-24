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
package yu.ac.bg.rcub.binder.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.handler.BinderConnector;
import yu.ac.bg.rcub.binder.handler.BinderHandler;

public class TestBinderHandler implements BinderHandler {

	private String receiveData(DataInputStream in) throws EOFException, IOException {
		int len = in.readInt();
		byte[] b = new byte[len];
		in.readFully(b);
		return new String(b);
	}

	private void sendData(String s, DataOutputStream out) throws IOException {
		out.writeInt(s.length());
		out.writeBytes(s);
	}

	public void run(BinderConnector binderConnector) {
		logger.info("Started TEST binder handler.");
		try {
			DataInputStream clientIn = new DataInputStream(binderConnector.getClientInputStream());
			DataOutputStream clientOut = new DataOutputStream(binderConnector.getClientOutputStream());
			DataInputStream workerIn = new DataInputStream(binderConnector.getWorkerInputStream());
			DataOutputStream workerOut = new DataOutputStream(binderConnector.getWorkerOutputStream());
			String ball = "";
			while (!ball.equalsIgnoreCase("enough")) {
				ball = receiveData(clientIn);
				logger.info("Received from client " + ball);
				sendData(ball, workerOut);
				logger.info("Sent to worker " + ball);
				if (ball.equalsIgnoreCase("enough"))
					break;
				ball = receiveData(workerIn);
				logger.info("Received from worker " + ball);
				sendData(ball, clientOut);
				logger.info("Sent to client " + ball);
			}
		} catch (BinderCommunicationException be) {
			logger.error("Communication with client or worker failed.", be);
		} catch (Exception e) {
			logger.error("Unknown error occured!", e);
		}
	}

	static Logger logger = Logger.getLogger(TestBinderHandler.class);
}
