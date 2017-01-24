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

public class ProxyThread extends Thread {

	private DataInputStream in;
	private DataOutputStream out;
	private static final int BUFFER_SIZE = 100;

	private byte[] buffer = new byte[BUFFER_SIZE];
	private int numberOfBytesRead = 0;
	private boolean listening = true;

	public ProxyThread(DataInputStream input, DataOutputStream output) {
		super("ProxyThread");
		in = input;
		out = output;
	}

	public void run() {
		while (listening) {
			try {
				numberOfBytesRead = in.read(buffer, 0, BUFFER_SIZE);
				if (numberOfBytesRead == -1) {
					listening = false;
					break;
				}
				out.write(buffer, 0, numberOfBytesRead);
			} catch (IOException ioe) {
				listening = false;
			}
		}
	}
}
