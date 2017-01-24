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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderUtil;

/**
 * Admin listener used to receive admin commands (shutdown). Some code borrowed
 * from tomcat.
 * 
 * @author choppa
 * 
 */
public class AdminListener implements Runnable {

	private static Logger logger = Logger.getLogger(AdminListener.class);

	int port = BinderUtil.initIntField("AdminPort", 5006);
	String shutdown = BinderUtil.getShutdown();

	/**
	 * A random number generator that is <strong>only</strong> used if the
	 * shutdown command string is longer than 1024 characters.
	 */
	private Random random = null;

	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port, 1, InetAddress.getByName("localhost"));
		} catch (IOException e) {
			logger.error("Unable to listen on port: " + port, e);
			System.exit(1);
		}
		
		logger.info("Admin listener thread started...");

		// Loop waiting for a connection and a valid command
		while (true) {

			// Wait for the next connection
			Socket socket = null;
			InputStream stream = null;
			try {
				System.out.println("OOOO");
				socket = serverSocket.accept();
				socket.setSoTimeout(10 * 1000); // Ten seconds
				
				stream = socket.getInputStream();
				
			} catch (IOException e) {
				logger.error("Admin socket accept failed ", e);
			}

			// Read a set of characters from the socket
			StringBuffer command = new StringBuffer();
			int expected = 1024; // Cut off to avoid DoS attack
			while (expected < shutdown.length()) {
				if (random == null)
					random = new Random(System.currentTimeMillis());
				expected += (random.nextInt() % 1024);
			}
			while (expected > 0) {
				int ch = -1;
				try {
					ch = stream.read();
				} catch (IOException e) {
					logger.warn("Admin command read error", e);
					ch = -1;
				}
				if (ch < 32) // Control character or EOF terminates loop
					break;
				command.append((char) ch);
				expected--;
			}

			// Close the socket now that we are done with it
			try {
				socket.close();
			} catch (IOException e) {
			}

			// Match against our command string
			boolean match = command.toString().equals(shutdown);
			if (match) {
				break;
			} else
				logger.warn("Invalid admin shutdown command '" + command.toString() + "' received");
		}

		// Close the server socket and return
		try {
			serverSocket.close();
		} catch (Exception e) {
		}
		logger.info("Admin connected from localhost, shutting down binder service...");
		System.exit(0);
	}

}
