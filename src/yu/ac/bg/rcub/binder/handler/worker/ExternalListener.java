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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.ProxyThread;
import yu.ac.bg.rcub.binder.util.Enums.AccessType;

/**
 * Built-in implementation of the <code>WorkerHandler</code> interface similar
 * to the <code>ExternalExecutor</code> with one major difference: this
 * implementation starts the external program and acts as a proxy to the binder
 * (the external program will connect to the <code>ExternalListener</code>
 * process).
 * <p>
 * It creates a listening socket on some available port and passes this
 * information as an argument to the external program.
 * <p>
 * It supports only communication via binder.
 * 
 * @author choppa
 * 
 */
public class ExternalListener implements WorkerHandler {

	/** Wait time in ms for incoming connection. */
	private static final int SO_TIMEOUT = 180000;

	private Socket workerSocket = null;
	private ServerSocket server = null;
	private WorkerConnector wc;
	private String params;
	private String[] args;
	int listenPort;

	private void initSockets() {
		try {
			server = new ServerSocket(0);
		} catch (IOException e) {
			wc.log("Error! Unable to open listening port.", e);
			System.exit(1);
		}
		/* Maybe wait here for some short time? */
		listenPort = server.getLocalPort();
		if (listenPort == -1) {
			wc.log("No listening ports available.");
			System.exit(1);
		}
		wc.log("Listening on " + listenPort + " for external worker program.");
		runExternalProgram(); /* maybe do this in separate thread? */
		try {
			/* Use setSoTimeout to not wait infinitely. */
			server.setSoTimeout(SO_TIMEOUT);
			workerSocket = server.accept();
			workerSocket.setTcpNoDelay(true);
			wc.log("Connection with the external worker process established.");
		} catch (SocketException e) {
			wc.log("Error! IO error occured while setting TcpNoDelay flag.", e);
			System.exit(1);
		} catch (SocketTimeoutException e) {
			wc.log("Error! Timeout while waiting for incoming connection from the external process.", e);
			System.exit(1);
		} catch (IOException e) {
			wc.log("Error! IO error occured while listening for external program.", e);
			System.exit(1);
		}
	}

	private void exchangeData() {
		DataInputStream binderIS = null;
		DataOutputStream workerOS = null;
		DataInputStream workerIS = null;
		DataOutputStream binderOS = null;
		try {
			binderIS = new DataInputStream(wc.getInputStream());
			workerOS = new DataOutputStream(workerSocket.getOutputStream());
			workerIS = new DataInputStream(workerSocket.getInputStream());
			binderOS = new DataOutputStream(wc.getOutputStream());
		} catch (IOException e) {
			wc.log("Error! I/O error occurred while obtaining worker data streams.", e);
			System.exit(1);
		} catch (BinderCommunicationException e) {
			wc.log("Error! I/O error occurred while obtaining binder data streams.", e);
			System.exit(1);
		}
		Thread cs = new ProxyThread(binderIS, workerOS);
		Thread sc = new ProxyThread(workerIS, binderOS);
		cs.start();
		sc.start();
		/* wait for communication to finish */
		while ((!cs.getState().name().equalsIgnoreCase("TERMINATED")) && (!sc.getState().name().equalsIgnoreCase("TERMINATED")))
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ie) {
				wc.log("Error occurred while waiting for proxy threads to finish.", ie);
			}
	}

	private void runExternalProgram() {
		String script = args[0] + " " + args[1] + " " + listenPort;
		/* Only first 2 parameters will be read. */
		wc.log("Executing script: '" + script + "'.\n\n");
		// String[] newArgs = script.split(" ");
		String[] newArgs = BinderUtil.readArgs(script);
		final ProcessBuilder pb = new ProcessBuilder(newArgs);
		pb.redirectErrorStream(true);
		// new Thread() {
		// public void run() {
		try {
			pb.start();
			// /final Process pr = pb.start();
			// pr.waitFor();
		} catch (IOException e) {
			wc.log("Error! External process not started properly.", e);
			System.exit(1);
			// } catch (InterruptedException e) {
			// Thread.currentThread().interrupt();
		}
		// }
		// }.start();
	}

	private void closeSockets() {
		if (workerSocket != null)
			try {
				workerSocket.close();
				wc.log("Worker socket closed.");
			} catch (IOException e) {
				wc.log("Error! IO error occured while closing worker socket.", e);
				System.exit(1);
			}
		if (server != null)
			try {
				server.close();
				wc.log("Server socket closed.");
			} catch (IOException e) {
				wc.log("Error! IO error occured while closing server socket.", e);
				System.exit(1);
			}
	}

	public void run(WorkerConnector workerConnector) {
		wc = workerConnector;
		wc.log("Built-in External Listener handler started.");
		if (wc.getAccessType() != AccessType.BINDER) {
			wc.log("Error! External Listener supports only communication via binder.");
			return;
		}
		params = wc.getWorkerExternalParameters();
		if (params.equalsIgnoreCase("")) {
			wc.log("Error! Application path/name not specified on the worker.");
			return;
		}
		/* Generate command line string by splitting args string. */
		// args = params.split(" ");
		args = BinderUtil.readArgs(params);
		if (args.length < 2) {
			wc.log("Error! External Listener requires atleast 2 parameters.");
			return;
		}
		initSockets();
		exchangeData();
		closeSockets();
		wc.log("Built-in External Listener handler finished.");
	}
}
