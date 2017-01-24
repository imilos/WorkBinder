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
package yu.ac.bg.rcub.binder.handler.worker;

import java.io.IOException;

import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.util.Enums.AccessType;

/**
 * Built-in implementation of the <code>WorkerHandler</code> interface that
 * executes an external program with the arguments provided by the clients
 * access string. Format is 'custom:&ltlist of arguments&gt'.
 * 
 * @author choppa
 * 
 */
public class ExternalExecutor implements WorkerHandler {

	public void run(WorkerConnector workerConnector) {
		workerConnector.log("Built-in External Executor handler started.");
		String clientAccessString = workerConnector.getClientAccessString();

		if (workerConnector.getAccessType() != AccessType.CUSTOM) {
			workerConnector.log("Error! External Executor works only with CUSTOM connection types.");
			return;
		}
		/* For precaution, but this should not happen. */
		if (!clientAccessString.startsWith("custom:")) {
			workerConnector.log("Error! Access string not beggining with 'custom:'.");
			return;
		}
		String appPath = workerConnector.getWorkerExternalParameters();
		if (appPath.equalsIgnoreCase("")) {
			workerConnector.log("Error! Application path/name not specified on the worker.");
			return;
		}
		/* Generate command line string by adding App name to the args. */
		String argsString = appPath + " " + clientAccessString.substring(7);
		// String[] script = argsString.split(" ");
		String[] script = BinderUtil.readArgs(argsString);
		workerConnector.log("Executing script: '" + argsString + "'.\n\n");
		ProcessBuilder pb = new ProcessBuilder(script);
		pb.redirectErrorStream(true);
		try {
			final Process pr = pb.start();
			pr.waitFor();
		} catch (IOException e) {
			workerConnector.log("Error! External process not started properly.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		workerConnector.log("Built-in External Executor handler finished.");
	}
}
