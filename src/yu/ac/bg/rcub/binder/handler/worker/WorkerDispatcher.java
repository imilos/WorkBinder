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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Properties;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.AppLoader;
import yu.ac.bg.rcub.binder.handler.KeepAliveThread;
import yu.ac.bg.rcub.binder.handler.mpi.MPINode;
import yu.ac.bg.rcub.binder.handler.mpi.MPIWorkerHandler;
import yu.ac.bg.rcub.binder.net.BinderSocketFactory;
import yu.ac.bg.rcub.binder.net.ProtocolExchange;
import yu.ac.bg.rcub.binder.net.SocketWrapper;
import yu.ac.bg.rcub.binder.util.Enums.AccessType;
import yu.ac.bg.rcub.binder.util.Enums.ConnectionType;
import yu.ac.bg.rcub.binder.util.Enums.JobStatus;

/**
 * Wrapper for WorkerJob(s). All of the worker code starts here.
 * 
 * @author choppa
 * 
 */
public class WorkerDispatcher {

	private String CE;
	private String propPath;
	private String jobID;
	private String logFileName;
	private String startRoutingInfo;
	private String routingInfo = "";
	private PrintWriter fout = null;
	private static final int PROTOCOL_VERSION = BinderUtil.PROTOCOL_VERSION;
	private final JobStatus jobStatus;
	private String maxWallClockTime;

	private Properties properties = new Properties();
	private SocketWrapper workerSW = null;
	private Socket binderSocket = null;

	private static BinderSocketFactory socketFactory;

	private AppLoader workerLoader = null;

	/* private static WorkerDispatcher _instance = null; */

	private WorkerDispatcher(String propertiesPath, String CE, String routingInfo, String jobID, String logFileName,
			String maxWallClockTime) {
		this.propPath = propertiesPath;
		this.CE = CE;
		this.startRoutingInfo = routingInfo;
		this.jobID = jobID;
		this.logFileName = logFileName;
		this.jobStatus = JobStatus.READY;
		this.maxWallClockTime = maxWallClockTime;
		init();
	}

	private WorkerDispatcher(String propertiesPath, String CE, String routingInfo, String jobID, String logFileName,
			String maxWallClockTime, String jobStatus) {
		this.propPath = propertiesPath;
		this.CE = CE;
		this.startRoutingInfo = routingInfo;
		this.jobID = jobID;
		this.logFileName = logFileName;
		this.jobStatus = (jobStatus.equalsIgnoreCase("finished")) ? JobStatus.FINISHED : JobStatus.READY;
		this.maxWallClockTime = maxWallClockTime;
		init();
	}

	private void init() {
		try {
			properties.load(new FileInputStream(new File(propPath)));
		} catch (IOException e) {
			System.err.println("Worker: Error reading WorkerDispatcher properties file!");
			e.printStackTrace();
			System.exit(1);
		}
		initSocketFactory();
		// workerLoader = new WorkerAppLoader(properties);
		workerLoader = initLoader(properties);
		try {
			String repDir = properties.getProperty("ReportsLocationDir", "");
			if (!repDir.equals("")) {
				File dir = new File(repDir);
				if (!dir.exists())
					dir.mkdirs();
			}
			fout = BinderUtil.getFileOutput(logFileName, "", repDir);
		} catch (FileNotFoundException e) {
			System.err.println("Worker:   *** ERR *** Could not open new output file!!!");
			e.printStackTrace();
		}
	}

	private void initSocketFactory() {
		// add some failsafe credentials search
		// Maybe move this code to BinderSocketFactory?
		String proxyFile = System.getenv("X509_USER_PROXY");
		if (proxyFile != null)
			System.setProperty("X509_USER_PROXY", proxyFile);
		else {
			proxyFile = System.getenv("X509_PROXY_FILE");
			if (proxyFile != null)
				System.setProperty("X509_PROXY_FILE", proxyFile);
		}
		socketFactory = new BinderSocketFactory(properties);
	}

	private AppLoader initLoader(Properties prop) {
		String[] vos = BinderUtil.readArgs(prop.getProperty("VOs", ""), false);
		String[] dirs = new String[vos.length];
		for (int i = 0; i < vos.length; i++) {
			/* get sw_dir from env */
			dirs[i] = (System.getenv("VO_" + vos[i].toUpperCase() + "_SW_DIR"));
		}
		return new AppLoader(dirs, prop.getProperty("AppsPropertiesFile"));
	}

	public void run() {
		log("Worker Dispatcher started...");
		KeepAliveThread keepAlive = null;
		try {
			log("CE full name: " + CE + "\n" + "Properties path: " + propPath + "\n" + "JobID: " + jobID);
			// log(properties);
			log("Started new worker thread!");
			
			connect();
			
			AccessType accessType = workerSW.getProtocolExchange().getAccessType();
			if (accessType == AccessType.DIRECT || accessType == AccessType.CUSTOM) {
				keepAlive = new KeepAliveThread(binderSocket);
				
				keepAlive.start();
				
			}
			executeWorkerHandler();
		} catch (ClassNotFoundException e) {
			log("*** ERR *** Worker Handler class not found!", e);
		} catch (Exception e) {
			log("*** ERR *** Error executing handler thread!", e);
		} finally {
			if (keepAlive != null)
				keepAlive.disconnect();
			disconnect();
			log("Worker finished.");
		}
	}

	private void executeWorkerHandler() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			BinderCommunicationException, IOException, InterruptedException {
		String handlerClass;
		ClassLoader cl = null;
		if (!workerLoader.readAppConfig(workerSW.getProtocolExchange().getClientApplicationID())) {
			/* old impl used for testing, new impl has precedance */
			handlerClass = properties.getProperty(workerSW.getProtocolExchange().getClientApplicationID() + "_Class");
		} else {
			handlerClass = workerLoader.getAppWorkerHandler();
			/* We fill (or override) properties with new options. */
			properties.put(workerLoader.getAppName() + "_Parameters", workerLoader.getAppParams());
			cl = workerLoader.getClassLoader();
		}
		Class<?> c = cl == null ? Class.forName(handlerClass) : Class.forName(handlerClass, true, cl);
		log("Handler Class: " + c + ".");
		WorkerConnector workerConn = new WorkerConnectorImpl(workerSW, fout, properties);
		/* process handler */
		log("==============================================================================");
		Object handler = c.newInstance();
		if (handler instanceof WorkerHandler) {
			WorkerHandler worker = (WorkerHandler) handler;
			worker.run(workerConn);
		} else if (handler instanceof MPIWorkerHandler) {
			MPIWorkerHandler mpiHandler = (MPIWorkerHandler) handler;
			MPINode node = new MPINode(workerConn, workerSW);
			if (node.isMaster()) {
				try {
					mpiHandler.onInit(workerConn);
					node.execute();
					mpiHandler.onFinish();
				} finally {
					node.cleanUp();
				}
			}
		} else
			log("*** ERR *** Invalid Handler loaded!");
		log("==============================================================================");
	}

	private void connect() throws IOException, GeneralSecurityException {
		String binderAddress = properties.getProperty("BinderAddress");
		int binderPort = new Integer(properties.getProperty("BinderPort"));
		int timeout = new Integer(properties.getProperty("ConnectionTimeout", "2000"));
		// binderSocket = new Socket();
		// binderSocket.connect(new InetSocketAddress(binderAddress,
		// binderPort), timeout);
		log("Binder adr = " + binderAddress + " Binder port = " + binderPort);
		binderSocket = socketFactory.createSocket(binderAddress, binderPort);
		
		
		
		binderSocket.setTcpNoDelay(true);
		ProtocolExchange protocolExchange = new ProtocolExchange();
		workerSW = new SocketWrapper(binderSocket, protocolExchange);

		DataOutputStream out = workerSW.getOs();
		DataInputStream in = workerSW.getIs();
		log("in za worker socek wrapper je: " + in);

		/* protocol exchange */
		exchangeProtocolHeader(protocolExchange, in, out, binderSocket);
		/* access type is determined when client info is read */
		readClientInfo(in, out);
		switch (protocolExchange.getAccessType()) {
		case BINDER:
			log("Communication via binder chosen.");
			break;
		case DIRECT:
			String address = protocolExchange.getClientHost();
			int port = protocolExchange.getClientHostPort();
			log("Attempting to establish a direct connection to the client (" + address + ":" + port + ")...");
			try {
				// Socket socket = new Socket();
				// socket.connect(new InetSocketAddress(address, port),
				// timeout);
				Socket socket = socketFactory.createSocket(address, port);
				socket.setTcpNoDelay(true);
				/* Reroute workerSW to new connection to the client. */
				workerSW = new SocketWrapper(socket, protocolExchange);
				log("Connection to the client established.");
			} catch (IOException e) {
				log("Error occured while trying to connect to the client.", e);
				throw e;
			}
			break;
		case CUSTOM:
			log("Custom communication chosen...");
			// workerSW.close();
			break;
		case UNKNOWN:
			log("Unsupported communication type chosen, disconnecting from binder...");
			throw new IllegalArgumentException("Unsupported communication type chosen.");
		}

	}

	private void exchangeProtocolHeader(ProtocolExchange protocolExchange, DataInputStream in, DataOutputStream out,
			Socket socket) throws EOFException, IOException {
		// TODO check if we want to keep the AppList in WorkerDispatcher.prop
		String appList1 = properties.getProperty("ApplicationList", "");
		String appList2 = workerLoader.getAppsListing();
		String appList = appList1.equalsIgnoreCase("") ? appList2 : appList2 + " " + appList1;

		/* protocol version */
		protocolExchange.setWorkerProtocolVersion(PROTOCOL_VERSION);
		/* connection type (0 - client; 1 - worker) */
		protocolExchange.setConnectionType(ConnectionType.WORKER);
		/* jobId */
		protocolExchange.setWorkerJobID(jobID);
		/* CE name */
		protocolExchange.setWorkerCeName(CE);
		/* application list */
		protocolExchange.setWorkerApplicationList(appList);
		/* accessString describing connection type between client and worker */
		protocolExchange.setWorkerAccessString(properties.getProperty("AccessString", ""));
		/* tell binder whether worker job should be removed or not */
		protocolExchange.setWorkerJobStatus(jobStatus);
		/* remaining max wall clock time of the worker job */
		protocolExchange.setWorkerMaxWallClockTime(maxWallClockTime);

		log("Application list: " + protocolExchange.getWorkerApplicationList());
		log("Job Status sent to the binder: " + jobStatus + ".");
		log("Access string: '" + protocolExchange.getWorkerAccessString() + "'.");
		log("MaxWallClockTime: " + maxWallClockTime + ".");

		protocolExchange.sendWorkerHeader(out);
		log("Worker started; starting header exchange.");
	}

	private void readClientInfo(DataInputStream in, DataOutputStream out) throws EOFException, IOException {
		ProtocolExchange protocolExchange = workerSW.getProtocolExchange();
		log("U readClienInfo in je: " + in);
		
		protocolExchange.receiveHeader(in);
		
		if (protocolExchange.getConnectionType() != ConnectionType.CLIENT) {
			log("ERROR: Exchanging protocol with someone who is not client!");
			throw new RuntimeException("Exchanging protocol with someone who is not client!");
		}
		log("Client protocol version: " + protocolExchange.getClientProtocolVersion());
		log("Application ID: " + protocolExchange.getClientApplicationID());
		log("Routing info received from client: " + protocolExchange.getClientRoutingInfo());

		if (protocolExchange.getAccessType() == AccessType.BINDER)
			routingInfo = protocolExchange.getClientRoutingInfo() + "\n\tWorker => "
					+ workerSW.getSocket().getInetAddress().getCanonicalHostName() + " : "
					+ workerSW.getSocket().getLocalPort() + "\n\t\tReceived from starting script: " + startRoutingInfo;
		else
			routingInfo = protocolExchange.getClientRoutingInfo() + "\n\tWorker => " + protocolExchange.getWorkerAccessString()
					+ "\n\t\tReceived from starting script: " + startRoutingInfo;
		protocolExchange.setWorkerRoutingInfo(routingInfo);
		// /* error description */
		// protocolExchange.setWorkerErrorDescription("");
		protocolExchange.sendWorkerResponse(out);
		log("Starting appropriate handler thread...");
	}

	private void disconnect() {
		if (workerSW.getProtocolExchange().getAccessType() == AccessType.DIRECT) {
			try {
				binderSocket.close();
				log("Disconnected from the client.");
			} catch (IOException ioe) {
				log("Error occured while disconnecting from the client.", ioe);
			}
		}
		try {
			workerSW.close();
			log("Disconnected from the binder.");
		} catch (IOException ioe) {
			log("Error occured while disconnecting from the binder.", ioe);
		}
	}

	private void log(Object message) {
		log(message, null);
	}

	private void log(Object message, Throwable t) {
		if (fout != null) {
			fout.println(message);
			if (t != null)
				t.printStackTrace(fout);
			fout.flush();
		}
	}

	public static void main(String[] args) {

		if (args.length == 6) {
			WorkerDispatcher worker = new WorkerDispatcher(args[0], args[1], args[2], args[3], args[4], args[5]);
			worker.run();
		} else if (args.length == 7) {
			WorkerDispatcher worker = new WorkerDispatcher(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
			worker.run();
		} else {
			System.err.println("Need 6 arguments to start!");
			System.err.println("Worker:   Argument 1 - Path to .properties file");
			System.err.println("Worker:   Argument 2 - Full CE name");
			System.err.println("Worker:   Argument 3 - Routing Information (NO white spaces allowed!!!)");
			System.err.println("Worker:   Argument 4 - JobID");
			System.err.println("Worker:   Argument 5 - Log Filename");
			System.err.println("Worker:   Argument 6 - MaxWallClockTime (in minutes)");
			System.err.println("Worker:   Argument 7 - FINISHED [optional argument]");
			System.exit(1);
		}
	}
}
