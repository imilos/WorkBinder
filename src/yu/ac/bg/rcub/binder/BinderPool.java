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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.util.Enums.BinderMode;
import yu.ac.bg.rcub.binder.util.Enums.BinderStrategy;
import yu.ac.bg.rcub.binder.eventlogging.Event;
import yu.ac.bg.rcub.binder.job.WorkerJob;
import yu.ac.bg.rcub.binder.net.SocketWrapper;
import yu.ac.bg.rcub.binder.recovery.RecoveryManager;

/**
 * @author choppa
 * 
 */
public class BinderPool {

	private HashMap<String, ComputingElement> ceMap = null;
	private SortedMap<String, ComputingElement> ceSortedMap = null;
	private String htmlFileName = "";

	private RecoveryManager recMan;

	private volatile BinderStrategy strategy = BinderStrategy.REGULAR;
	private volatile BinderMode workingMode = BinderMode.IDLE;
	private long idleStartStamp = System.currentTimeMillis();
	private static final long IDLE_TIMEOUT = BinderUtil.initIntField("IdleTimeout", 900) * 1000;
	private final int panicModeCEPercent = BinderUtil.initIntField("PanicModeCEPercent", 30);

	/** variables describing client arrival frequency */
	private double clientFrequency = 0;
	private int numClientsPerInterval = 0;
	private final double clientFreqCoeff = BinderUtil.initDoubleField("ClientFreqCoeff", 0.1);
	private final long clientSamplingInterval = BinderUtil.initIntField("ClientSamplingInterval", 3000);

	/** coeffs for target pool size */
	private final int minPoolSize = BinderUtil.initIntField("MinPoolSize", 15);

	private final double binderBusyCoeff = BinderUtil.initDoubleField("BinderBusyCoeff", 0.1);
	private final double binderClientsCoeff = BinderUtil.initDoubleField("BinderClientsCoeff", 0.5);

	/** total number of active clients & workers */
	private volatile int numClients = 0;
	private volatile int numWorkers = 0;

	/* TODO calculate limits - to be removed? */
	private static final int MAX_WORKERS = 100;
	private static final int MAX_CLIENTS = 100;

	/** Simple tweak used for testing submitted jobs. */
	public static final boolean PROCESS_TWEAK_ENABLED = BinderUtil.getProperty("PROCESS_TWEAK_ENABLED", "yes")
			.equalsIgnoreCase("yes");

	public BinderPool(RecoveryManager recoveryManager) {
		this.recMan = recoveryManager;
		ceMap = new HashMap<String, ComputingElement>();
		ceSortedMap = new TreeMap<String, ComputingElement>();
		init();
	}

	private void init() {
		String listOfCE = BinderUtil.getProperty("CEs");
		htmlFileName = BinderUtil.getProperty("HtmlFileName");
		StringTokenizer st = new StringTokenizer(listOfCE, " ");
		
		boolean dbEmpty = !recMan.recoveryDataExists();
		
		if (dbEmpty) {
			logger.debug("No recovery data found.");
		} else {
			logger.debug("Recovery data found.");
			recMan.initRecoveryManager(System.currentTimeMillis());
			logger.info("Finished updating recovery data.");
		}
		while (st.hasMoreTokens()) {
			String ceShortName = st.nextToken();
			logger.info("Adding new CE to the pool " + ceShortName + "...");
			String ceName = BinderUtil.getProperty(ceShortName + "_CE");
			
			if (ceName != null) {
				ComputingElement ce = new ComputingElement(ceName, ceShortName, recMan, this);
				if (!dbEmpty) {
					ComputingElement recoveredCE = recMan.restoreComputingElement(ce);
					ce.initR(recoveredCE);
					logger.debug("Computing element " + ce.getShortName() + " restored. New emmsj = "
							+ ce.getEffectiveMaxMillisSubmittedJob() + ".");
				} else {
					recMan.saveComputingElement(ce);
					logger.debug("Computing element " + ce.getShortName() + " saved.");
				}
				ceMap.put(ceName, ce);

				ceSortedMap.put(ceShortName, ce);
				logger.debug("Starting a new thread for CE: " + ce.getShortName());
				new ComputingElementStatusThread(ce).start();
			} else {
				logger.info("CE " + ceShortName + " is skipped because it has invalid path name in binder properties.");
			}
		}
		new BinderPoolStatusThread(this).start();
		new BinderPoolClientsThread(this, clientSamplingInterval).start();
	}

	/**
	 * Searches the CEs in order to get the <code>WorkerJob</code> for a client
	 * that has connected to the binder. The <code>WorkerJob</code> must match
	 * the clients requirements regarding execution time, applicationID and so
	 * on...
	 * 
	 * @param clientSW
	 *            The <code>SocketWrapper</code> containing information about
	 *            the client.
	 * @return The <code>WorkerJob</code> that was matched to the client or
	 *         <code>null</code> if no job was found.
	 */
	public synchronized WorkerJob getWorker(SocketWrapper clientSW) {
		String candidateCEString = clientSW.getProtocolExchange().getClientCandidateCEs();
		logger.debug("Clients candidateCEs: '" + candidateCEString + "'.");
		String appID = clientSW.getProtocolExchange().getClientApplicationID();
		/* If client didnt specify CEs, then we try all CEs. */
		ArrayList<ComputingElement> candidateCEs = fillCandidateCE(candidateCEString, appID);
		/* Shuffle the list of candidate CEs so that we choose a random CE. */
		Collections.shuffle(candidateCEs);
		for (ComputingElement ce : candidateCEs) {
			long currentTime = System.currentTimeMillis();
			WorkerJob wj = ce.readyToBusy(currentTime, clientSW.getProtocolExchange().getClientRequiredWallClockTime());
			if (wj != null)
				return wj;
		}
		logger.info("No ready jobs on CEs: " + candidateCEString + ".");
		return null;
	}

	/**
	 * Creates a new <code>ArrayList&ltComputingElement&gt</code> with a given
	 * list of CEs. If list is empty, then all CEs are returned.
	 * <p>
	 * Only the CEs that support the application are returned.
	 * 
	 * @param candidateCEString
	 *            The <code>String</code> containing a list of CE full names
	 *            (paths).
	 * @param appID
	 *            The <code>String</code> containing an application ID.
	 * @return An <code>ArrayList</code> containing all chosen CEs.
	 */
	private ArrayList<ComputingElement> fillCandidateCE(String candidateCEString, String appID) {
		ArrayList<ComputingElement> candidateCEs = new ArrayList<ComputingElement>();
		if (candidateCEString.equalsIgnoreCase("")) {
			for (ComputingElement ce : ceMap.values())
				if (ce.isAppSupported(appID))
					candidateCEs.add(ce);
		} else {
			StringTokenizer st = new StringTokenizer(candidateCEString, " ");
			while (st.hasMoreTokens()) {
				String ceName = st.nextToken();
				ComputingElement ce = ceMap.get(ceName);
				if (ce != null && ce.isAppSupported(appID))
					candidateCEs.add(ce);
				else
					logger.warn("Specified CE: '" + ceName + "' not found or app not supported.");
			}
		}
		return candidateCEs;
	}

	/**
	 * Executes a query that checks for matching CEs and gets the number of
	 * their available jobs.
	 * 
	 * @param clientSW
	 *            Clients <code>SocketWrapper</code>.
	 * @return The <code>CEInfo[]</code> array containing info about CEs.
	 */
	public CEInfo[] executeCEQuery(SocketWrapper clientSW) {
		String candidateCEString = clientSW.getProtocolExchange().getClientCandidateCEs();
		logger.debug("Clients candidateCEs: '" + candidateCEString + "'.");
		String appID = clientSW.getProtocolExchange().getClientApplicationID();
		/* If client didnt specify CEs, then we try all CEs. */
		ArrayList<ComputingElement> candidateCEs = fillCandidateCE(candidateCEString, appID);
		int size = candidateCEs.size();
		CEInfo[] ceInfo = new CEInfo[size];
		int i = 0;
		for (ComputingElement ce : candidateCEs) {
			long currentTime = System.currentTimeMillis();
			ceInfo[i] = new CEInfo(ce.getName(), ce.getShortName(), ce.getNumAvailableJobs(currentTime, clientSW
					.getProtocolExchange().getClientRequiredWallClockTime()), ce.getAppList());
			i++;
		}
		return ceInfo;
	}

	/**
	 * Switches the state of the <code>WorkerJob</code> from BUSY to REUSABLE,
	 * after it has finished working.
	 * 
	 * @param wj
	 *            The <code>WorkerJob</code> that has finished work.
	 */
	public synchronized void busyToReusable(WorkerJob wj) {
		ComputingElement ce = ceMap.get(wj.getSw().getProtocolExchange().getWorkerCeName());
		ce.busyToReusable(wj.getJobID());

		updateClientNumber(-1);
		updateWorkerNumber(-1);
		try {
			wj.getSw().close();
		} catch (IOException e) {
			logger.error(e, e);
		}
	}

	/**
	 * Removes the specified worker job from the specified CE.
	 * 
	 * @param workerSW
	 *            The workers <code>SocketWrapper</code> containing information
	 *            about worker job.
	 */
	public synchronized void removeWorkerJob(SocketWrapper workerSW) {
		ComputingElement ce = ceMap.get(workerSW.getProtocolExchange().getWorkerCeName());
		if (ce != null) {
			ce.removeWorkerJob(workerSW.getProtocolExchange().getWorkerJobID());
			try {
				workerSW.close();
			} catch (IOException e) {
				logger.error(e, e);
			}
		}
	}

	/**
	 * Adds a worker job to the binder pool. Works only for SUBMITTED & REUSABLE
	 * jobs.
	 * 
	 * @param workerSW
	 *            Workers <code>SocketWrapper</code>.
	 * @return <code>true</code> if the worker job was accepted.
	 */
	public synchronized boolean addWorkerJob(SocketWrapper workerSW) {
		// TODO add logic needed to add WorkerJobs that have been restored using
		// the recovery engine (those jobs have socketwrapper = null)
		ComputingElement ce = ceMap.get(workerSW.getProtocolExchange().getWorkerCeName());
		if (ce == null) {
			logger.debug("Error, no such CE " + workerSW.getProtocolExchange().getWorkerCeName());
			if (BinderUtil.isPerfMonEnabled())
				Event.jobArrival(null, workerSW, 0, 14);
			return false;
		}
		if (ce.isCeFull()) { /* Extra check to see if CE is currently full. */
			logger.debug("Error, CE " + workerSW.getProtocolExchange().getWorkerCeName()
					+ " has already reached a maximum number of worker jobs.");
			ce.removeWorkerJob(workerSW.getProtocolExchange().getWorkerJobID());
			if (BinderUtil.isPerfMonEnabled())
				Event.jobArrival(null, workerSW, 0, 13);
			return false;
		}

		logger.debug("Trying to add worker job to CE " + workerSW.getProtocolExchange().getWorkerCeName());
	
		ce.updateAppList(workerSW.getProtocolExchange().getWorkerApplicationList());
		logger.debug(ce.getAppList());
		boolean added = false;
		if (ce.submittedToReady(workerSW)) {
			added = true;
		} else if (ce.reusableToReady(workerSW)) {
			added = true;
		}
		if (added)
			updateWorkerNumber(1);
		return added;
	}

	public synchronized void updateClientNumber(int numClientsUpdated) {

		if (numClientsUpdated > 0)
			numClientsPerInterval += numClientsUpdated;

		/*
		 * TODO maybe change IDLE/ACTIVE detection, distinguish between
		 * numClients & numClientsPerInterval
		 */
		if (numClientsUpdated + numClients >= 0 && numClientsUpdated + numClients <= MAX_CLIENTS) {
			numClients += numClientsUpdated;
			if (numClients > 0)
				/* instantly go to ACTIVE mode */
				workingMode = BinderMode.ACTIVE;
			else if (workingMode == BinderMode.ACTIVE) {
				/* reset timestamp only if we were in ACTIVE */
				idleStartStamp = System.currentTimeMillis();
			}
		}
	}

	/**
	 * Updated by client status thread. When the clients show up it goes
	 * straight to ACTIVE (see updateClientNumber()). When the number of active
	 * clients drops to 0, it waits some time before going to IDLE.
	 */
	public synchronized void updateMode() {
		if (numClients == 0 && workingMode == BinderMode.ACTIVE && System.currentTimeMillis() - idleStartStamp > IDLE_TIMEOUT)
			workingMode = BinderMode.IDLE;
	}

	public synchronized void updateWorkerNumber(int numWorkersUpdated) {

		if (numWorkersUpdated + numWorkers >= 0 && numWorkersUpdated + numWorkers <= MAX_WORKERS) {
			numWorkers += numWorkersUpdated;
			// TODO strategy?
		}
	}

	/**
	 * Updates client arrival frequency. It is updated from the status thread
	 * periodically in client sampling intervals.
	 */
	public synchronized void updateClientFrequency() {
		clientFrequency = clientFreqCoeff * clientFrequency + (1 - clientFreqCoeff) * numClientsPerInterval;
		numClientsPerInterval = 0;
	}

	/** Counts the total number of busy jobs on all CEs. */
	public int getBusyJobs() {
		int busyCount = 0;
		for (ComputingElement ce : ceSortedMap.values())
			busyCount += ce.getBusyJobsOnCE();

		return busyCount;
	}

	/** Calculates pool size by counting READY jobs on all CEs. */
	public int getActualPoolSize() {
		int readyCount = 0;
		for (ComputingElement ce : ceSortedMap.values())
			readyCount += ce.getReadyJobsOnCE();

		return readyCount;
	}

	/** Calculates desired target pool size. */
	public double getTargetPoolSize() {
		return minPoolSize + binderBusyCoeff * getBusyJobs() + binderClientsCoeff * clientFrequency;
	}

	/**
	 * Counts the number of jobs that are expected to arrive on all CEs within
	 * the time frame specified by the client sampling interval.
	 */
	public int getNumJobsExpectedToArrive() {
		int num = 0;
		for (ComputingElement ce : ceSortedMap.values())
			num += ce.getNumJobsExpectedToArrive(System.currentTimeMillis() + clientSamplingInterval);

		return num;
	}

	/** Calculates refill count for the FULL-THROTTLE (panic) strategy. */
	public double getPanicRefillCount() {
		return getTargetPoolSize() - getActualPoolSize() - getNumJobsExpectedToArrive();
	}

	/** Sorts CEs by emmsj in ascending order (best CEs are first). */
	private ArrayList<ComputingElement> sortCEByEmmsj() {
		int size = ceSortedMap.size();
		ArrayList<Integer> emmsjArray = new ArrayList<Integer>(size);
		ArrayList<ComputingElement> sortedCEs = new ArrayList<ComputingElement>(size);

		for (ComputingElement ce : ceSortedMap.values()) {
			sortedCEs.add(ce);
			emmsjArray.add(ce.getEffectiveMaxMillisSubmittedJob());
		}
		/* Critical performance section, shellsort used. */
		for (int gap = size / 2; gap > 0; gap = gap == 2 ? 1 : (int) (gap / 2.2))
			for (int i = gap; i < size; i++) {
				int tmp = emmsjArray.get(i);
				ComputingElement ceTmp = sortedCEs.get(i);
				int j = i;
				for (; j >= gap && tmp < emmsjArray.get(j - gap); j -= gap) {
					emmsjArray.set(j, emmsjArray.get(j - gap));
					sortedCEs.set(j, sortedCEs.get(j - gap));
				}
				emmsjArray.set(j, tmp);
				sortedCEs.set(j, ceTmp);
			}

		return sortedCEs;
	}

	/**
	 * Checks for panic mode and submits new jobs if needed. Jobs are first
	 * submitted on best CEs, and then if some panic refill jobs are remaining,
	 * they are submitted on all CEs. If there are still some jobs not
	 * submitted, warning is given.
	 */
	public synchronized void checkForPanicMode() {
		final int panicRefillCount = (int) Math.round(getPanicRefillCount());
		strategy = (panicRefillCount > 0) ? BinderStrategy.FULL_THROTTLE : BinderStrategy.REGULAR;
		if (panicRefillCount > 0) {
			ArrayList<ComputingElement> sortedCEsEmmsj = sortCEByEmmsj();
			int submittedOnBestCEs = panicRefillBestCEs(sortedCEsEmmsj, panicRefillCount, panicRefillCount);
			logger.debug("Submitted on best CEs = " + submittedOnBestCEs + ".");
			if (panicRefillCount > submittedOnBestCEs) {
				int submittedOnAllCEs = panicRefillAllCEs(sortedCEsEmmsj, panicRefillCount - submittedOnBestCEs,
						panicRefillCount);
				logger.debug("Submitted on all CEs = " + submittedOnAllCEs + ".");
				if (panicRefillCount > submittedOnAllCEs + submittedOnBestCEs)
					logger.warn("Total of " + (panicRefillCount - submittedOnAllCEs - submittedOnBestCEs)
							+ " panic refill jobs could not be submitted.");
			}
		}
	}

	/**
	 * Submits a specified number of jobs on best CEs. Part of the panic refill
	 * strategy.
	 * 
	 * @param totalCount
	 *            The total number of desired refill jobs decided by the
	 *            strategy (used mainly for the event logging).
	 */
	private int panicRefillBestCEs(ArrayList<ComputingElement> ceSortedEmmsj, int refillCount, int totalCount) {
		int numCEs = ceSortedEmmsj.size() * panicModeCEPercent / 100;
		int totalNumSubmitted = 0;
		int index = numCEs - 1;

		while (index >= 0) {
			ComputingElement ce = ceSortedEmmsj.get(index);
			int submittedOnCE = ce.submitWorkerJobs(refillCount, totalCount, BinderStrategy.FULL_THROTTLE);
			refillCount -= submittedOnCE;
			numCEs--;
			index--;
			totalNumSubmitted += submittedOnCE;
		}
		return totalNumSubmitted;
	}

	/**
	 * Submits a specified number of jobs on all CEs. Part of the panic refill
	 * strategy.
	 * 
	 * @param totalCount
	 *            The total number of desired refill jobs decided by the
	 *            strategy (used mainly for the event logging).
	 */
	private int panicRefillAllCEs(ArrayList<ComputingElement> ceSortedEmmsj, int refillCount, int totalCount) {
		int totalNumSubmitted = 0;
		for (ComputingElement ce : ceSortedEmmsj) {
			int submittedOnCE = ce.submitWorkerJobs(refillCount, totalCount, BinderStrategy.FULL_THROTTLE);
			refillCount -= submittedOnCE;
			totalNumSubmitted += submittedOnCE;
		}
		return totalNumSubmitted;
	}

	public void printHtmlPoolStatus() {
		StringBuffer str;

		str = new StringBuffer("<html> <meta http-equiv=\"refresh\" content=\"30\" > <body>");
		str.append("<p>Working mode: " + workingMode + ", strategy: " + strategy + ".</p>");
		str.append("<p>Active clients: " + numClients + ", clients per interval: " + numClientsPerInterval);
		str.append(", client arrival frequency: " + String.format("%8.3f", clientFrequency) + ".</p>");
		str.append("<TABLE width=\"100%\" align=\"right\">");
		str
				.append("<TR width=\"100%\" align=\"center\"><TH>Site</TH><TH>Supported Apps</TH><TH>Ready</TH><TH>Busy</TH><TH>Submitted</TH><TH>Reusable</TH><TH>Aged</TH><TH>ESD (ms)</TH></TR>");
		int sumAgedJobsOnCE = 0;
		int sumBusyJobsOnCE = 0;
		int lastBusyJobsOnCe = 0;
		int sumReadyJobsOnCE = 0;
		int lastReadyJobsOnCe = 0;
		int sumSubmittedJobsOnCE = 0;
		int sumReusableJobsOnCE = 0;
		int total = 0;
		int active = 0;
		logger.debug("Periodical status check of the BinderPool (operational mode is " + workingMode
				+ ", operational strategy is " + strategy + ").");

		for (ComputingElement ce : ceSortedMap.values()) {
			str.append(ce.getHtmlCEStatus());
			sumAgedJobsOnCE += ce.getAgedJobsOnCE();
			lastBusyJobsOnCe = ce.getBusyJobsOnCE();
			sumBusyJobsOnCE += lastBusyJobsOnCe;
			lastReadyJobsOnCe = ce.getReadyJobsOnCE();
			sumReadyJobsOnCE += lastReadyJobsOnCe;
			sumSubmittedJobsOnCE += ce.getSubmittedJobsOnCE();
			sumReusableJobsOnCE += ce.getReusableJobsOnCE();

			total++;
			if (lastBusyJobsOnCe + lastReadyJobsOnCe > 0)
				active++;
		}
		str.append("<TR><TD><B>Total: " + total + " &nbsp; Active: " + active + "</B></TD>" + "<td><B>N/A</B></td>" + "<td><B>"
				+ sumReadyJobsOnCE + "</B></td>" + "<td><B>" + sumBusyJobsOnCE + "</B></td>" + "<td><B>" + sumSubmittedJobsOnCE
				+ "</B></td>" + "<td><B>" + sumReusableJobsOnCE + "</B></td>" + "<td><B>" + sumAgedJobsOnCE + "</B></td>"
				+ "<td><B>N/A</B></td>" + "</TR>");

		str.append("<tr><td>Generated on ");
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy. 'at' HH:mm:ss");
		str.append(sdf.format(new Date()));
		str.append("</td></tr></TABLE></body></html>");

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(htmlFileName));
			out.write(str.toString());
			out.close();
		} catch (IOException e) {
			logger.error("Error while writing to html file: " + htmlFileName, e);
		}
	}

	public BinderMode getWorkingMode() {
		return workingMode;
	}

	public BinderStrategy getStrategy() {
		return strategy;
	}

	static Logger logger = Logger.getLogger(BinderPool.class);

}
