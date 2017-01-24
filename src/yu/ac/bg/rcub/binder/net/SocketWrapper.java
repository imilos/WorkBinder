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
package yu.ac.bg.rcub.binder.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

//import sun.util.logging.resources.logging;


public class SocketWrapper implements Serializable {

	private static final long serialVersionUID = 3832716787566761661L;
	protected transient Socket socket;
	protected ProtocolExchange protocolExchange;
	protected transient DataInputStream is = null;
	protected transient DataOutputStream os = null;

	public SocketWrapper() {
	}

	public SocketWrapper(Socket socket, ProtocolExchange protocolExchange) throws IOException {

		this.socket = socket;
		this.protocolExchange = protocolExchange;
		this.is = new DataInputStream(socket.getInputStream());
		this.os = new DataOutputStream(socket.getOutputStream());

	}

	public void close() throws IOException {
		// if (!socket.isInputShutdown())
		// socket.shutdownInput();
		// if (!socket.isOutputShutdown())
		// socket.shutdownOutput();
		// if (!socket.isClosed())
		if (socket != null)
			socket.close();
	}

	public DataInputStream getIs() {
		return is;
	}

	public DataOutputStream getOs() {
		return os;
	}

	public ProtocolExchange getProtocolExchange() {
		return protocolExchange;
	}

	public Socket getSocket() {
		return socket;
	}

	/*
	 * setters removed for the time being
	 * 
	 * public void setIs(DataInputStream stream) { is = stream; }
	 * 
	 * public void setOs(DataOutputStream stream) { os = stream; }
	 * 
	 * public void setProtocolHeader(ProtocolExchange header) { protocolExchange =
	 * header; }
	 * 
	 * public void setSocket(Socket socket) { this.socket = socket; }
	 */

	// static transient Logger logger = Logger.getLogger(SocketWrapper.class);
}
