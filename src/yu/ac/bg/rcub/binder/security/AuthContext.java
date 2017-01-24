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
import java.io.Serializable;

/**
 * @deprecated glite-trustmanager is used now
 * @author choppa
 * 
 */
public abstract class AuthContext implements Serializable {

	protected AuthContext() {
	}

	/**
	 * @param out
	 * @throws IOException
	 */
	public abstract void customWriteObject(DataOutputStream out) throws IOException;

	/**
	 * @param in
	 * @throws EOFException
	 * @throws IOException
	 */
	public abstract void customReadObject(DataInputStream in) throws EOFException, IOException;

	public String toString() {
		return "Abstract auth context.";
	}

}
