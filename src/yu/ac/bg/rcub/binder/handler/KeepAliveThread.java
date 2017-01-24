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
import java.io.IOException;
import java.net.Socket;

/**
 * Simple thread used to check if the connection between the worker and the
 * binder is alive. Used for built-in direct and custom connection types in
 * order to notify the binder when the job has ended. One thread is started on
 * the binder side and one on the worker side for each <code>WorkerJob</code>.
 * <p>
 * It sends heartbeat signals from both sides, if send fails this means that the
 * connection is lost.
 * 
 * @author choppa
 * 
 */
public class KeepAliveThread extends Thread {

	private Socket socket = null;
	private int timeout = 10000;
	private volatile boolean connected = true;
	private static final byte[] message = { 'H', 'e', 'l', 'l', 'o' };

	// private static final byte[] quit = { 'D', 'o', 'n', 'e' };

	public KeepAliveThread(Socket socket) {
		this.socket = socket;
	}

	public KeepAliveThread(Socket socket, int timeout) {
		this.socket = socket;
		this.timeout = timeout;
	}

	/**
	 * Used to stop the thread.
	 */
	public synchronized void disconnect() {
		connected = false;
	}

	public void run() {

		DataInputStream in = null;
		DataOutputStream out = null;
		try {
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			return;
		}
		byte[] buffer = new byte[message.length];
		while (connected) {
			try {
				out.write(message, 0, message.length);
				out.flush();
				/*
				 * We dont want to get blocked by the read. Also we only read so
				 * that the buffer doesnt get overloaded.
				 */
				if (in.available() > 0)
					in.read(buffer);
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt(); /* Ok? */
				}
			} catch (IOException e) {
				connected = false;
				break;
			}
		}
	}
}
