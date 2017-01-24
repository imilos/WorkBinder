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
package yu.ac.bg.rcub.binder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.admin.AdminListener;
import yu.ac.bg.rcub.binder.recovery.RecoveryManager;
import yu.ac.bg.rcub.binder.recovery.RecoveryManagerDummyAdapter;

public class Binder {

	private Socket socket = null;
	private ServerSocket serverSocket = null;
	private BinderPool binderPool = null;
	private int port = 0;
	boolean listening = false;

	private RecoveryManager recMan;

	Binder(ServerSocket serverSocket, int port) {

		this.port = port;
		listening = true;
		this.serverSocket = serverSocket;
		
		/* initialize Binder Recovery Module */
		initRecoveryModule();
		
		binderPool = new BinderPool(recMan);
		
	}

	Binder() {
	}

	// public void updateRecData() {
	// recMan.updateRecoveryData();
	// }

	public void initRecMngr() {
		recMan.initRecoveryManager(System.currentTimeMillis());
	}

	private void initRecoveryModule() {
		switch (BinderUtil.getRecoveryType()) {
		case NONE:
			this.recMan = new RecoveryManagerDummyAdapter();
			logger.debug("Dummy Recovery Manager initiated (no recovery will be used).");
			break;
		case EJB:
			/* initialization of the recovery bean with the context lookup */
			InitialContext ctx;
			try {
				ctx = new InitialContext();
				recMan = (RecoveryManager) ctx.lookup("RecoveryManagerBean/remote");
			} catch (NamingException e) {
				logger.error(e, e);
				System.exit(1);
			}
			if (recMan != null)
				logger.debug("Recovery Manager Bean initiated.");
			break;
		}

	}

	void run() {
		while (listening) {
			try {
				acceptIncomingConnection();
				if (socket != null && socket.isConnected())
					new BinderListenerThread(socket, binderPool).start();
			} catch (Exception e) {
				logger.error("Unknown error", e);
				closeSocket();
			}
		}
		closeSocket();
		closeBinderListener();
	}

	/** Waits for incoming connection from client or worker. */
	private void acceptIncomingConnection() {
		try {
			socket = null; /* for precaution */
			socket = serverSocket.accept();
			socket.setTcpNoDelay(true);
		} catch (SocketException e) {
			logger.error("Error while setting TcpNoDelay flag. This request in skiped and socket on port " + socket.getPort()
					+ " in closed.", e);
			closeSocket();
		} catch (IOException e) {
			logger.warn("Error while accepting reqest. This request in skiped.", e);
			closeSocket();
		}
	}

	/** Shuts down binder listener. */
	private void closeBinderListener() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error(e, e);
		}
		logger.info("Stopped listening on port: " + port + ".");
	}

	/** Closes a socket. */
	private void closeSocket() {
		try {
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			logger.error("Error while closing socket.", e);
		}
	}

	public static void main(String[] args) {
		int port;
		ServerSocket ss;
		String propPath = new String();

		if (args.length > 0) {
			propPath = args[0];
			BinderUtil.initService(propPath);

			// start admin listener
			new Thread(new AdminListener()).start();

			port = BinderUtil.initIntField("ListeningPort", 8731);
			try {
				// ss = new ServerSocket(port);
				ss = BinderUtil.getSocketFactory().createServerSocket(port);
				logger.info("Start listening on port: " + port + ".");
				Binder binder = new Binder(ss, port);
				binder.run();
			} catch (Exception e) {
				logger.error("Could not listen on port: " + port + ".");
				System.exit(1);
			}
		} else {
			logger.info("Usage - BinderProperties.");
		}
	}

	private static Logger logger = Logger.getLogger(Binder.class);

}
