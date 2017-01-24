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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.CEInfo;
import yu.ac.bg.rcub.binder.handler.client.ClientConnector;
import yu.ac.bg.rcub.binder.handler.client.ClientConnectorFactory;

public class TestMPIClient extends Thread {
	private Properties prop = null;
	private ClientConnector clientConn = null;
	private final long waitTime;

	public TestMPIClient() {
		super("TestMPIClient");
		this.waitTime = 1000;
		init("client.properties");
	}

	public TestMPIClient(String propFilePath) {
		super("TestMPIClient");
		this.waitTime = 1000;
		init(propFilePath);
	}

	/* added for test purposes */
	public TestMPIClient(String propFilePath, String waitTime) {
		super("TestMPIClient");
		this.waitTime = Long.parseLong(waitTime);
		init(propFilePath);
		/* override property for test */
		prop.setProperty("RequiredWallClockTime", String.valueOf((3 * this.waitTime)));
	}

	private void init(String propFilePath) {
		/* Read client prop from the file. */
		prop = new Properties();
		try {
			prop.load(new FileInputStream(new File(propFilePath)));
			// prop.setProperty("UseMPI", "yes");
			// prop.setProperty("NumNodes", "2");
			// prop.setProperty("ApplicationID", "mpi-test");
		} catch (FileNotFoundException e1) {
			logger.error("Client prop file not found!", e1);
			System.exit(1);
		} catch (IOException e2) {
			logger.error("Error while reading client prop file!", e2);
			System.exit(1);
		}
	}

	public String receiveData(DataInputStream in) throws EOFException, IOException {
		int len = in.readInt();
		byte[] b = new byte[len];
		in.readFully(b);
		return new String(b);
	}

	public void sendData(String s, DataOutputStream out) throws IOException {
		out.writeInt(s.length());
		out.writeBytes(s);
	}

	private void communicate() throws BinderCommunicationException, InterruptedException, IOException {
		DataInputStream in = new DataInputStream(clientConn.getInputStream());
		DataOutputStream out = new DataOutputStream(clientConn.getOutputStream());
		/* send args */
		String args = prop.getProperty("MPI_ARGS", "");
		sendData(args, out);
		/* get std.out */
		System.out.println("MPI std.out: ");
		System.out.println(receiveData(in));
		/* get std.err */
		System.out.println("MPI std.err: ");
		System.out.println(receiveData(in));
	}

	public void testCEs() throws BinderCommunicationException {
		CEInfo[] ceInfos = clientConn.executeCEListMatch();
		int size = ceInfos.length;
		System.out.println("CE list match report by the binder, total of " + size + " CEs matched.");
		for (CEInfo ceInfo : ceInfos)
			System.out.println(ceInfo);
	}

	public void run() {
		logger.info("Starting test client...");
		try {
			clientConn = ClientConnectorFactory.createClientConnector(prop);
			// testCEs();
			clientConn.connect();
			/* actual client communication */
			communicate();
			/* client finished */
			logger.info("End of client, disconnecting from binder...");
			clientConn.disconnect();
		} catch (BinderCommunicationException be) {
			logger.error("Communication with the binder failed.", be);
		} catch (Exception e) {
			logger.error("Something's wrong!", e);
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			new TestMPIClient("mpi-client.properties").start();
		} else if (args.length == 1) {
			new TestMPIClient(args[0]).start();
		} else if (args.length == 2) {
			new TestMPIClient(args[0], args[1]).start();
		} else {
			System.out.println("Usage: TestMPIClient [filepath]");
			System.out.println(" - [filepath] is an optional prop file name (client.properties is default)");
		}
	}

	Logger logger = Logger.getLogger(TestMPIClient.class);

}
