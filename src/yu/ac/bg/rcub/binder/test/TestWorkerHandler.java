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

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;
import yu.ac.bg.rcub.binder.handler.worker.WorkerHandler;

public class TestWorkerHandler implements WorkerHandler {

	private String prefix = "Binder ";
	private String ball;

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

	public void run(WorkerConnector workerConnector) {

		workerConnector.log("Started a new TEST worker handler thread!");
		try {
			DataInputStream din = new DataInputStream(workerConnector.getInputStream());
			DataOutputStream dout = new DataOutputStream(workerConnector.getOutputStream());
			ball = receiveData(din);
			workerConnector.log("Received: " + ball);
			while (!ball.equalsIgnoreCase("enough")) {
				ball = prefix + ball;
				workerConnector.log("Returning: " + ball);
				sendData(ball, dout);
				ball = receiveData(din);
				workerConnector.log("Received: " + ball);
			}
		} catch (BinderCommunicationException be) {
			workerConnector.log("*** ERROR ***   Communication with the binder or the client failed.", be);
		} catch (Exception e) {
			workerConnector.log("*** ERROR ***   Unknown error occured.", e);
		}
		workerConnector.log("Test worker handler end.");
	}
}
