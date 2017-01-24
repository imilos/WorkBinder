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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.mpi.MPIWorkerHandler;
import yu.ac.bg.rcub.binder.handler.mpi.Utils;
import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;

public class TestMPIHandler implements MPIWorkerHandler {
	private WorkerConnector wc;

	public void onInit(WorkerConnector workerConnector) {
		this.wc = workerConnector;
		wc.log("Test MPI handler init.");
		try {
			DataInputStream in = new DataInputStream(wc.getInputStream());
			String args = BinderUtil.readString(in);
			System.setProperty(Utils.MPI_USER_ARGS, args);
			wc.log("Got args from the client:" + args);
		} catch (BinderCommunicationException e) {
			wc.log(e, e);
		} catch (IOException e) {
			wc.log(e, e);
		}
	}

	public void onFinish() {
		wc.log("Post process stage.");
		wc.log("Reading std.out of the MPI job");
		DataOutputStream out;
		try {
			out = new DataOutputStream(wc.getOutputStream());
		} catch (BinderCommunicationException e) {
			return;
		}
		try {
			String stdOutFile = System.getProperty(Utils.MPI_WORK_DIR) + File.separator + "std.out";
			BinderUtil.writeString(out, printFile(stdOutFile));
			String stdErrFile = System.getProperty(Utils.MPI_WORK_DIR) + File.separator + "std.err";
			BinderUtil.writeString(out, printFile(stdErrFile));
		} catch (FileNotFoundException e) {
			wc.log("Cant find std.out file.", e);
		} catch (IOException e) {
			wc.log(e, e);
		}
		try {
			/* Wait 15s to slow down the deletion of temp folder. */
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			wc.log(e, e);
		}
		wc.log("Test MPI handler finish.");
	}

	private String printFile(String filename) throws FileNotFoundException {
		Scanner scanner = new Scanner(new File(filename));
		StringBuilder sb = new StringBuilder();
		try {
			while (scanner.hasNextLine()) {
				String s = scanner.nextLine();
				sb.append(s);
				sb.append("\n");
				wc.log(s);
			}
		} finally {
			scanner.close();
		}
		return sb.toString();
	}
}
