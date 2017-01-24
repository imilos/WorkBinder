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
package yu.ac.bg.rcub.binder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;

/**
 * Info class containing various information about a CE.
 * 
 * @author choppa
 * 
 */
public class CEInfo implements Serializable {

	private static final long serialVersionUID = -8730805415008937744L;
	private String name;
	private String shortName;
	private int numReadyJobs;
	private String appList;

	public CEInfo() {
	}

	public CEInfo(String name, String shortName, int numReadyJobs, String appList) {
		this.name = name;
		this.shortName = shortName;
		this.numReadyJobs = numReadyJobs;
		this.appList = appList;
	}

	public void customWriteObject(DataOutputStream out) throws IOException {
		BinderUtil.writeString(out, name);
		BinderUtil.writeString(out, shortName);
		out.writeInt(numReadyJobs);
		BinderUtil.writeString(out, appList);
	}

	public void customReadObject(DataInputStream in) throws EOFException, IOException {
		name = BinderUtil.readString(in);
		shortName = BinderUtil.readString(in);
		numReadyJobs = in.readInt();
		appList = BinderUtil.readString(in);
	}

	public String getName() {
		return name;
	}

	public String getShortName() {
		return shortName;
	}

	public int getNumReadyJobs() {
		return numReadyJobs;
	}

	public String getAppList() {
		return appList;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CE: ");
		sb.append(shortName);
		sb.append(", full name/path: ");
		sb.append(name);
		sb.append(", ready jobs: ");
		sb.append(numReadyJobs);
		sb.append(", ");
		sb.append(appList);
		return sb.toString();
	}

}
