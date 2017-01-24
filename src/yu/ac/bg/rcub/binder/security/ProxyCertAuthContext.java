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
package yu.ac.bg.rcub.binder.security;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import yu.ac.bg.rcub.binder.BinderUtil;

public class ProxyCertAuthContext extends AuthContext {

	private static final long serialVersionUID = 5478342952783246049L;
	byte[][] certChainData;
	byte[] proxyKeyData;

	public ProxyCertAuthContext(byte[][] certChainData, byte[] proxyKeyData) {
		super();
		this.certChainData = certChainData;
		this.proxyKeyData = proxyKeyData;
	}

	public byte[][] getCertChainData() {
		return certChainData;
	}

	public byte[] getProxyKeyData() {
		return proxyKeyData;
	}

	@Override
	public void customReadObject(DataInputStream in) throws EOFException, IOException {
		/* -- client credentials encoded -- */
		/* client cert chain length */
		certChainData = new byte[in.readInt()][];
		/* client cert chain encoded */
		for (int i = 0; i < certChainData.length; i++)
			certChainData[i] = BinderUtil.readBytes(in);
		/* client private key (PEM) encoded */
		proxyKeyData = BinderUtil.readBytes(in);
	}

	@Override
	public void customWriteObject(DataOutputStream out) throws IOException {
		/* -- client credentials encoded -- */
		/* client cert chain length */
		out.writeInt(certChainData.length);
		/* client cert chain encoded */
		for (byte[] cert : certChainData)
			BinderUtil.writeBytes(out, cert);
		/* client private key (PEM) encoded */
		BinderUtil.writeBytes(out, proxyKeyData);
	}

	@Override
	public String toString() {
		return "Auth via proxy cert.";
	}

}
