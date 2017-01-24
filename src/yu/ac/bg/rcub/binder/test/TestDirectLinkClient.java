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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.net.ProtocolExchange;

public class TestDirectLinkClient extends Thread {

	private String binderAddress;
	private int binderPort;
	private String prefix = "PING-PONG ";
	private ProtocolExchange protocolExchange;

	public TestDirectLinkClient(String binderAddress, int binderPort) {
		super("TestDirectLinkClient");
		this.binderAddress = binderAddress;
		this.binderPort = binderPort;
	}

	public void run() {

		Socket testSocket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		ServerSocket ss = null;

		try {

			testSocket = new Socket(binderAddress, binderPort);

			in = new DataInputStream(testSocket.getInputStream());
			out = new DataOutputStream(testSocket.getOutputStream());
			logger.info("Before client starts sending data");
			testSocket.setTcpNoDelay(true);

			// protocol version
			out.writeInt(7);
			// connection type (0 - client; 1 - worker)
			out.writeInt(0);
			// server selection description
			// Util.writeString(out, "ANY");
			// candidate CEs
			// BinderUtil.writeString(out,
			// "site-a.x.y:2119/jobmanager-pbs-seegrid
			// site-b.x.y:2119/jobmanager-pbs-seegrid");
			BinderUtil.writeString(out, "");
			logger.info("Sent candidate CE list");
			// application ID
			BinderUtil.writeString(out, "TEST");
			// accessString
			BinderUtil.writeString(out, "direct:147.91.4.104:4004");
			// required wall clock time
			out.writeLong(1000);
			// client credentials string
			BinderUtil.writeString(out, "");
			// routing Info
			String routingInfo = "\n\tClient listening on => localhost : 4004";
			BinderUtil.writeString(out, routingInfo);

			try {
				ss = new ServerSocket(4004);
			} catch (Exception e) {
				System.err.println("ERR while opening new listening socket");
			}

			logger.info("Client initiated; start exchanging headers");

			protocolExchange = new ProtocolExchange();
			protocolExchange.receiveHeader(in);

			logger.info("Recieved data from worker: " + protocolExchange.getWorkerProtocolVersion() + ", "
					+ protocolExchange.getWorkerApplicationList());
			protocolExchange.receiveWorkerResponse(in);

			logger.info("Routing Info received from worker: " + protocolExchange.getWorkerRoutingInfo());
			logger.info("Error Description received from worker: \n" + protocolExchange.getWorkerErrorDescription());

			logger.info("Client exchanged headers; switching sockets");

			try {
				testSocket.shutdownOutput();
				testSocket.shutdownInput();
				testSocket.close();
				testSocket = ss.accept();
				in = new DataInputStream(testSocket.getInputStream());
				out = new DataOutputStream(testSocket.getOutputStream());
				testSocket.setTcpNoDelay(true);
			} catch (Exception e) {
				logger.error("Error while switching sockets");
			}

			logger.info("Sockets switched; starting ping-pong");

			for (int i = 1; i < 4; i++) {
				BinderUtil.writeString(out, prefix + i + ". time");
				System.out.println(BinderUtil.readString(in));
			}

			BinderUtil.writeString(out, "enough");
			logger.info("End of client");

			testSocket.shutdownOutput();
			testSocket.shutdownInput();
			testSocket.close();
			ss.close();

		} catch (UnknownHostException e) {
			logger.error("Don't know about host: ");
		} catch (IOException e) {
			e.printStackTrace();
			try {
				testSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (Exception e) {
			logger.error("Something's wrong!");
			System.exit(1);

		}

	}

	public static void main(String[] args) {
		if (args.length == 2) {
			logger.info("In TestDirectLinkClient main");
			new TestDirectLinkClient(args[0], new Integer(args[1]).intValue()).run();
		} else {
			System.err.println("Need 2 arguments to start!");
			System.err.println("Test Client:   Argument 1 - Binder host name");
			System.err.println("Test Client:   Argument 2 - Binder listening port");
		}
	}

	static Logger logger = Logger.getLogger(TestDirectLinkClient.class);

}
