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

public class CertAuthContext extends AuthContext {

	private static final long serialVersionUID = -2846919270071196456L;
	byte[] certData;

	public CertAuthContext(byte[] certData) {
		super();
		this.certData = certData;
	}

	public byte[] getCertData() {
		return certData;
	}

	@Override
	public void customReadObject(DataInputStream in) throws EOFException, IOException {
		/* client cert encoded */
		certData = BinderUtil.readBytes(in);
	}

	@Override
	public void customWriteObject(DataOutputStream out) throws IOException {
		/* client cert encoded */
		BinderUtil.writeBytes(out, certData);
	}

	@Override
	public String toString() {
		return "Auth via cert.";
	}

}
