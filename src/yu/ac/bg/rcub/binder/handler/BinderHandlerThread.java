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
package yu.ac.bg.rcub.binder.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderPool;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.eventlogging.Event;
import yu.ac.bg.rcub.binder.job.WorkerJob;
import yu.ac.bg.rcub.binder.net.ProtocolExchange;
import yu.ac.bg.rcub.binder.net.SocketWrapper;

public class BinderHandlerThread extends Thread {

	private static final String DEFAULT_PLUGINS_DIR = System.getProperty("binder.home") + File.separator + "plugins";

	/** client SocketWrapper */
	private SocketWrapper clientSW;

	/** WorkerJob containing worker SocketWrapper */
	private WorkerJob wj;

	private BinderPool pool;
	private final long startUpTime;

	public BinderHandlerThread(SocketWrapper sw1, WorkerJob wj, BinderPool pool, long startUpTime) {
		super("BinderHandlerThread-" + sw1.getSocket().getInetAddress().toString().substring(1) + '-'
				+ sw1.getProtocolExchange().getClientApplicationID());
		this.clientSW = sw1;
		this.wj = wj;
		this.pool = pool;
		this.startUpTime = startUpTime;
	}

	public void run() {
		SocketWrapper workerSW = wj.getSw();
		try {
			logger.debug("Start exchanging data between " + printSocketStatus(clientSW, workerSW) + ".");
			exchangeProtocolHeader(clientSW, workerSW);

			switch (clientSW.getProtocolExchange().getAccessType()) {
			case BINDER:
				/* communication via binder, run proper binder handler */
				executeBinderApplicationHandler();
				break;
			case DIRECT:
			case CUSTOM:
				shutdownClientStream();
				KeepAliveThread keepAlive = new KeepAliveThread(workerSW.getSocket());
				keepAlive.start();
				keepAlive.join();
				break;
			case UNKNOWN:
				break;
			}

		} catch (EOFException eof) { /* Needed?! */
			logger.error("Finished exchanging data between " + printSocketStatus(clientSW, workerSW) + ".", eof);
		} catch (IOException ioe) {
			logger.error("Error while transmitting data between " + printSocketStatus(clientSW, workerSW) + ".", ioe);
		} catch (Exception e) {
			logger.error("Error while starting application handler!", e);
		} finally {
			logger.debug("End exchanging data between " + printSocketStatus(clientSW, workerSW) + ".");
			shutdownClientStream();
			shutdownWorkerStream();
		}
		if (BinderUtil.isPerfMonEnabled())
			Event.clientDisconnected(clientSW, System.currentTimeMillis() - startUpTime, 0);
	}

	/**
	 * Gets the appropriate application handler from binder.properties, if not
	 * found, the default proxy handler will be used.
	 * <p>
	 * Handler specified in app plugin dir will be ignored.
	 */
	private void executeBinderApplicationHandler() throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {

		String defaultHandler = BinderUtil.getProperty("DefaultApplicationHandler");
		String app = clientSW.getProtocolExchange().getClientApplicationID();
		String handler = BinderUtil.getProperty(app + "_ApplicationHandler", defaultHandler);
		String pluginDir = BinderUtil.getProperty("PluginsDir", DEFAULT_PLUGINS_DIR);
		String[] dirs = { pluginDir };
		AppLoader loader = new AppLoader(dirs, BinderUtil.getProperty("AppsPropertiesFile"));
		ClassLoader cl = null;
		if (loader.readAppConfig(app)) {
			cl = loader.getClassLoader();
			String foundHandler = loader.getAppWorkerHandler();
			if (!handler.equals(foundHandler))
				logger.warn("Configuration conflict, specified handler (" + handler + ") does not match found handler ("
						+ foundHandler + ").");
		}
		Class<?> c = cl == null ? Class.forName(handler) : Class.forName(handler, true, cl);
		logger.debug("Application Handler Class: " + c);
		/* Instantiate a new binder connector and run proper handler */
		BinderHandler bah = (BinderHandler) c.newInstance();
		BinderConnector binderConn = new BinderConnectorImpl(clientSW, wj.getSw());
		bah.run(binderConn);
	}

	private String printSocketStatus(SocketWrapper clientSW, SocketWrapper workerSW) {
		return clientSW.getProtocolExchange().getConnectionType() + " "
				+ clientSW.getSocket().getInetAddress().toString().substring(1) + ":" + clientSW.getSocket().getPort()
				+ " and " + workerSW.getProtocolExchange().getConnectionType() + " "
				+ workerSW.getSocket().getInetAddress().toString().substring(1) + ":" + workerSW.getSocket().getPort();
	}

	private void exchangeProtocolHeader(SocketWrapper clientSW, SocketWrapper workerSW) throws EOFException, IOException {
		// DataInputStream clientToWorkerIs = clientSW.getIs();
		DataOutputStream workerOs = workerSW.getOs();
		DataInputStream workerIs = workerSW.getIs();
		DataOutputStream clientOs = clientSW.getOs();

		clientSW.getProtocolExchange().sendClientHeader(workerOs);
		workerSW.getProtocolExchange().sendWorkerHeader(clientOs);

		String routingInfo = clientSW.getProtocolExchange().getClientRoutingInfo();
		logger.debug("Client routing info: " + routingInfo);

		/* worker sends rest of its header response */
		workerSW.getProtocolExchange().receiveWorkerResponse(workerIs);
		routingInfo = workerSW.getProtocolExchange().getWorkerRoutingInfo();
		String errorDescription = workerSW.getProtocolExchange().getWorkerErrorDescription();
		workerSW.getProtocolExchange().sendWorkerResponse(clientOs);

		ProtocolExchange.exchangeFields(clientSW.getProtocolExchange(), workerSW.getProtocolExchange());

		logger.debug("Worker routing info: " + routingInfo
				+ (errorDescription.equalsIgnoreCase("") ? "" : "\nWorker error description: " + errorDescription));
		logger.debug("Headers exchanged.");
	}

	private void shutdownClientStream() { /* TODO Needs refactoring! */
		try {
			clientSW.close();
			logger.debug("Client socket closed.");
		} catch (IOException e) {
			logger.error("Error occured while closing client socket.", e);
		}
	}

	private void shutdownWorkerStream() {
		try {
			wj.getSw().close();
			pool.busyToReusable(wj);
			logger.debug("Worker socket closed.");
		} catch (IOException e) {
			logger.error("Error occured while closing worker socket.", e);
		}
	}

	private Logger logger = Logger.getLogger(BinderHandlerThread.class);

}
