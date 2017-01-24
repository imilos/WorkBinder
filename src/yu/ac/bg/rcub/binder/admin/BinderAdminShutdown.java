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
package yu.ac.bg.rcub.binder.admin;

import java.io.DataOutputStream;
import java.net.Socket;

public class BinderAdminShutdown {

	private int port = Integer.parseInt(System.getProperty("binder.adminport", "5006"));
	private String shutdown = System.getProperty("binder.shutdown", "SHUTDOWN");

	BinderAdminShutdown() {
	}

	public static void main(String[] args) throws Exception {

		if (args.length > 0)
			usage();

		BinderAdminShutdown admin = new BinderAdminShutdown();
		admin.run();
	}

	public static void usage() {
		System.err.println("\nUsage: BinderAdminShutdown");
		System.err.println("\t use -Dbinder.adminport=\"5006\" to specify binder service port [default - 5006]");
		System.err.println("\t use -Dbinder.shutdown=\"command\" to specify shutdown command [default - SHUTDOWN]");
		System.exit(1);
	}

	public void run() throws Exception {

		Socket socket = new Socket("localhost", port);
		socket.setTcpNoDelay(true);
		System.out.println("Connected to the binder");
		new DataOutputStream(socket.getOutputStream()).writeBytes(shutdown + "\n");

		//		
		// ProtocolExchange protocol = new ProtocolExchange();
		// SocketWrapper adminSW = new SocketWrapper(socket, protocol);
		//
		// /* protocol version */
		// protocol.setAdminProtocolVersion(BinderUtil.PROTOCOL_VERSION);
		// /* connection type (0 - client; 1 - worker) */
		// protocol.setConnectionType(ConnectionType.ADMIN);
		// /* admin shutdown command */
		// protocol.setAdminShutdown(shutdown);
		// protocol.sendAdminHeader(adminSW.getOs());

		System.out.println("Admin shutdown command sent to the binder service at port " + port);
		// adminSW.close();
	}
}
