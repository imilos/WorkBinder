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
package yu.ac.bg.rcub.binder.handler.mpi;

import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;

/**
 * A simple interface that provides the functionality to implement a specific
 * communication handler between the worker and the client for MPI applications.
 * <p>
 * The MPI application will be executed from the working directory specified by
 * (read only): <br>
 * <code>System.getProperty(Utils.MPI_WORK_DIR)</code>
 * 
 * @see Utils
 * 
 * @author choppa
 * 
 */
public interface MPIWorkerHandler {

	/**
	 * Called after the MPI environment has been setup and before the MPI
	 * application has been executed. This method gives access to setup the MPI
	 * application that will be executed with arguments, files etc.
	 * <p>
	 * Passing the arguments to the MPI application is done via the: <br>
	 * <code>System.setProperty(Utils.MPI_USER_ARGS, myArgsString)</code>
	 * 
	 * @param workerConnector
	 *            Provides access to the communication interface between the
	 *            worker and the client.
	 */
	public void onInit(WorkerConnector workerConnector);

	/**
	 * Called after the execution of the MPI application. Used to pick up
	 * results (output files). Standard output and standard error output files
	 * of the executed MPI application are available at the root of the working
	 * directory: <br>
	 * <code>
	 * <pre>
	 * String stdOutFile = System.getProperty(Utils.MPI_WORK_DIR) + File.separator + Utils.MPI_STD_OUT;
	 * String stdErrFile = System.getProperty(Utils.MPI_WORK_DIR) + File.separator + Utils.MPI_STD_ERR;
	 * </pre>
	 * </code>
	 */
	public void onFinish();

}
