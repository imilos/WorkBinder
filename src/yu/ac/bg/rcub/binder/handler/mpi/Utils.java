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

/**
 * MPI utility class.
 * 
 * @author choppa
 * 
 */
public class Utils {
	static final byte MASTER = 0;
	static final byte SLAVE = 1;
	static final String MACHINE_FILE = ".machinefile";
	static final String WORK_DIR_PREFIX = "tempBinderMPI-";
	static final String MPI_MPICH_PATH = "MPI_MPICH_PATH";
	static final String MPI_MPICH_PATH_DEFAULT = "/opt/mpich-1.2.7p1/";
	static final String MPI_RUN_LINUX = "mpirun";
	static final String MPI_RUN_WINDOWS = "mpirun.bat";

	/**
	 * Name of the system property containing the working dir of the MPI
	 * application. Changing this property has no effect.
	 * 
	 * @see MPIWorkerHandler
	 */
	public static final String MPI_WORK_DIR = "binder.MPIWorkingDir";
	/**
	 * Name of the system property containing the user-defined arguments for the
	 * MPI application.
	 * 
	 * @see MPIWorkerHandler#onInit(yu.ac.bg.rcub.binder.handler.worker.WorkerConnector)
	 */
	public static final String MPI_USER_ARGS = "binder.MPIUserArgs";
	/**
	 * Name of the file containing standard output of the executed MPI
	 * application.
	 * 
	 * @see MPIWorkerHandler#onFinish()
	 */
	public static final String MPI_STD_OUT = "std.out";
	/**
	 * Name of the file containing standard error output of the executed MPI
	 * application.
	 * 
	 * @see MPIWorkerHandler#onFinish()
	 */
	public static final String MPI_STD_ERR = "std.err";

	private Utils() {
	}
}
