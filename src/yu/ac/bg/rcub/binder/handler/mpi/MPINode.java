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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.KeepAliveThread;
import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;
import yu.ac.bg.rcub.binder.net.SocketWrapper;

/**
 * MPI node wrapper, used to run MPI jobs from WorkerDispatcher.
 * 
 * @author choppa
 * 
 */
public class MPINode {
	private DataInputStream in;
	private DataOutputStream out;
	private byte role;
	private int numNodes;
	private Socket[] slaves;
	private String[] nodeHostnames;
	private ServerSocket server;
	private WorkerConnector wc;
	final String startDir = System.getProperty("user.dir");
	private SocketWrapper workerSW;
	private File machine;
	private File workDir;
	private String mpirunPath;

	public MPINode(WorkerConnector wc, SocketWrapper workerSW) throws BinderCommunicationException, IOException,
			InterruptedException {
		this.wc = wc;
		this.in = new DataInputStream(wc.getInputStream());
		this.out = new DataOutputStream(wc.getOutputStream());
		this.workerSW = workerSW;
		/* Master or slave? */
		this.role = in.readByte();
		if (role == Utils.MASTER)
			initMaster();
		else
			initSlave();
	}

	private void initSlave() throws IOException, InterruptedException {
		/* get master's address */
		String masterAddress = BinderUtil.readString(in);
		wc.log("MPI slave node, connecting to the master: " + masterAddress);
		int ind = masterAddress.lastIndexOf(':');
		if (ind > 0) {
			String masterHostName = masterAddress.substring(0, ind);
			try {
				int masterPort = Integer.parseInt(masterAddress.substring(ind + 1));
				/* connect to the master */
				Socket socket = new Socket(masterHostName, masterPort);
				wc.log("Connected to the master node.");
				KeepAliveThread keepAlive = new KeepAliveThread(socket);
				keepAlive.start();
				/* wait until work is done */
				keepAlive.join();
			} catch (NumberFormatException e) {
				wc.log("Error: invalid master port!");
			}
		} else {
			wc.log("Error: invalid master address!");
		}
	}

	private void initMaster() throws IOException {
		/* get the number of nodes needed */
		numNodes = in.readInt();
		wc.log("MPI master node, total nodes: " + numNodes);
		setUpMPI();
		server = new ServerSocket(0);
		nodeHostnames = new String[numNodes];
		// nodeHostnames[0] = server.getInetAddress().getHostName();
		nodeHostnames[0] = System.getenv("HOSTNAME");
		/* report local address back to client */
		BinderUtil.writeString(out, nodeHostnames[0] + ":" + server.getLocalPort());
		slaves = new Socket[numNodes];

		for (int i = 1; i < numNodes; i++) {
			Socket socket = server.accept();
			socket.setTcpNoDelay(true);
			new KeepAliveThread(socket).start();
			/* assemble machine file */
			nodeHostnames[i] = ((InetSocketAddress) socket.getRemoteSocketAddress()).getHostName();
			slaves[i] = socket;
			wc.log("Got connection from: " + nodeHostnames[i]);
		}
		saveConfig();
	}

	private void saveConfig() throws IOException {
		/* store hostnames to machine file */
		FileWriter fout = null;
		workDir = new File(new File(System.getProperty("user.home")), Utils.WORK_DIR_PREFIX
				+ workerSW.getProtocolExchange().getWorkerJobID());
		workDir.mkdir();
		wc.log("Created working dir: " + workDir.getAbsolutePath());
		machine = new File(workDir, Utils.MACHINE_FILE);
		workDir.deleteOnExit();
		machine.deleteOnExit();
		System.setProperty(Utils.MPI_WORK_DIR, workDir.getAbsolutePath());
		try {
			fout = new FileWriter(machine);
			for (int i = 0; i < numNodes; i++) {
				fout.write(nodeHostnames[i] + System.getProperty("line.separator"));
			}
		} finally {
			fout.close();
		}
	}

	/**
	 * Clean up data from temp MPI dir and disconnect from MPI slave nods.
	 * 
	 * @throws IOException
	 */
	public void cleanUp() throws IOException {
		shutdownNodes();
		deleteData();
	}

	private void deleteData() throws IOException {
		/* delete recursively? */
		wc.log("Deleting temp working dir: " + workDir.getAbsolutePath());
		deleteDirectory(workDir);
	}

	/**
	 * Recursively delete a directory.
	 * 
	 * @param directory
	 *            directory to delete
	 * @throws IOException
	 *             in case deletion is unsuccessful
	 */
	public void deleteDirectory(File directory) throws IOException {
		if (!directory.exists()) {
			return;
		}

		cleanDirectory(directory);
		if (!directory.delete()) {
			String message = "Unable to delete directory " + directory + ".";
			throw new IOException(message);
		}
		wc.log("Deleted dir: " + directory.getAbsolutePath());
	}

	/**
	 * Clean a directory without deleting it.
	 * 
	 * @param directory
	 *            directory to clean
	 * @throws IOException
	 *             in case cleaning is unsuccessful
	 */
	public void cleanDirectory(File directory) throws IOException {
		if (!directory.exists()) {
			String message = directory + " does not exist";
			throw new IllegalArgumentException(message);
		}

		if (!directory.isDirectory()) {
			String message = directory + " is not a directory";
			throw new IllegalArgumentException(message);
		}

		IOException exception = null;

		File[] files = directory.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			try {
				forceDelete(file);
			} catch (IOException ioe) {
				exception = ioe;
			}
		}

		if (null != exception) {
			throw exception;
		}
	}

	/**
	 * <p>
	 * Delete a file. If file is a directory, delete it and all sub-directories.
	 * </p>
	 * <p>
	 * The difference between File.delete() and this method are:
	 * </p>
	 * <ul>
	 * <li>A directory to be deleted does not have to be empty.</li>
	 * <li>You get exceptions when a file or directory cannot be deleted.
	 * (java.io.File methods returns a boolean)</li>
	 * </ul>
	 * 
	 * @param file
	 *            file or directory to delete.
	 * @throws IOException
	 *             in case deletion is unsuccessful
	 */
	public void forceDelete(File file) throws IOException {
		if (file.isDirectory()) {
			deleteDirectory(file);
		} else {
			if (!file.exists()) {
				throw new FileNotFoundException("File does not exist: " + file);
			}
			if (!file.delete()) {
				String message = "Unable to delete file: " + file;
				throw new IOException(message);
			}
			wc.log("Deleted file: " + file.getAbsolutePath());
		}
	}

	private void shutdownNodes() throws IOException {
		IOException exception = null;
		for (int i = 1; i < numNodes; i++)
			if (slaves[i] != null)
				try {
					slaves[i].close();
				} catch (IOException e) {
					exception = e;
				}
		if (server != null)
			try {
				server.close();
			} catch (IOException e) {
				exception = e;
			}

		if (exception != null)
			throw exception;
	}

	private void setUpMPI() throws IOException {
		String mpiPath = System.getenv(Utils.MPI_MPICH_PATH);
		if (mpiPath == null) {
			wc.log("Warning: MPI_MPICH_PATH not defined, using default.");
			mpiPath = Utils.MPI_MPICH_PATH_DEFAULT;
		}
		wc.log("MPI_MPICH_PATH=" + mpiPath);
		if (System.getProperty("os.name").toLowerCase().contains("windows"))
			mpirunPath = mpiPath + File.separator + "bin" + File.separator + Utils.MPI_RUN_WINDOWS;
		else
			mpirunPath = mpiPath + File.separator + "bin" + File.separator + Utils.MPI_RUN_LINUX;
		File mpirun = new File(mpirunPath);
		if (!mpirun.exists() || !mpirun.isFile() || !mpirun.canRead()) {
			wc.log("ERROR - Cant find or access mpirun executable (" + mpirunPath + ")!");
			throw new IOException("mpirun not found or not accessible");
		}
		wc.log("mpirun found in: " + mpirun.getAbsolutePath());
	}

	public boolean isMaster() {
		return role == Utils.MASTER;
	}

	/**
	 * Execute the MPI process. Standard output and error output will be saved
	 * in the MPI temp directory.
	 * 
	 * @throws IOException
	 */
	public void execute() throws IOException {
		List<String> command = createCommand();
		wc.log("Executing script: '" + command + "'.");
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(workDir);
		try {
			final Process pr = pb.start();
			new WriterThread(workDir.getAbsolutePath() + File.separator + Utils.MPI_STD_OUT, pr.getInputStream()).start();
			new WriterThread(workDir.getAbsolutePath() + File.separator + Utils.MPI_STD_ERR, pr.getErrorStream()).start();
			pr.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		wc.log("Execution finished.");
	}

	/**
	 * Hook into the process and write outputs to files.
	 * 
	 * @author choppa
	 * 
	 */
	private class WriterThread extends Thread {
		private InputStream in;
		private String filename;
		private static final int BUFF_SIZE = 100;

		WriterThread(String filename, InputStream in) {
			this.filename = filename;
			this.in = in;
		}

		public void run() {
			final BufferedWriter wr;
			try {
				wr = new BufferedWriter(new FileWriter(filename));
			} catch (IOException e) {
				wc.log(e, e);
				return;
			}
			final BufferedReader br = new BufferedReader(new InputStreamReader(in), BUFF_SIZE);
			String line;
			try {
				try {
					while ((line = br.readLine()) != null) {
						wr.write(line + "\n");
					}
				} catch (EOFException e) {
				}
			} catch (IOException e) {
				wc.log(e, e);
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					wc.log(e, e);
				}
				try {
					wr.close();
				} catch (IOException e) {
					wc.log(e, e);
				}
			}
		}
	}

	private List<String> createCommand() {
		String exe = wc.getWorkerExternalParameters().trim();
		String userArgs = System.getProperty(Utils.MPI_USER_ARGS, "");
		List<String> command = new ArrayList<String>();
		command.add(mpirunPath);
		command.add("-np");
		command.add(numNodes + "");
		command.add("-machinefile");
		command.add(machine.getAbsolutePath());
		command.add(exe);
		if (!userArgs.equals("")) {
			String[] args = BinderUtil.readArgs(userArgs, false);
			for (String arg : args) {
				command.add(arg);
			}
		}
		return command;
		// return exe + " " + numNodes + " \"" + machine.getAbsolutePath() + "\"
		// " + userArgs; /* old version*/
	}

}
