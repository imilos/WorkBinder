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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.util.Enums.AppFilter;
import yu.ac.bg.rcub.binder.util.Enums.BinderMode;
import yu.ac.bg.rcub.binder.util.Enums.BinderStrategy;
import yu.ac.bg.rcub.binder.util.Enums.JobStatus;
import yu.ac.bg.rcub.binder.eventlogging.Event;
import yu.ac.bg.rcub.binder.job.JobCounter;
import yu.ac.bg.rcub.binder.job.WorkerJob;
import yu.ac.bg.rcub.binder.job.submit.JobSubmit;
import yu.ac.bg.rcub.binder.net.ProtocolExchange;
import yu.ac.bg.rcub.binder.net.SocketWrapper;
import yu.ac.bg.rcub.binder.recovery.RecoveryManager;

public class ComputingElement implements Serializable {

	private static final long serialVersionUID = 5499587410748500177L;
	private final String name;
	private final String shortName;

	private final int maxJobsOnCE;
	private final int minJobsOnCE;

	// TODO switch to long
	/** effectiveMaxMillisSubmittedJob */
	private volatile int emmsj;
	private final long maxMillisReadyJob;
	/** maxMillisSubmittedJobCorrectiveCoefficientForDiscardedJobs */
	private final long correctiveCoeffDiscardedJobs;
	/** maxMillisSubmittedJobCorrectiveCoefficientForReadyJobs */
	private final long correctiveCoeffReadyJobs;
	/** multiplicationCoefficientForMaxMillisAgedJobs */
	private final long multiCoeffAgedJobs;
	// private final long maxWallClockTime;
	private final long maxMillisReusableJob;

	/** current job status */
	private volatile int submittedJobsOnCE = 0;
	private volatile int readyJobsOnCE = 0;
	private volatile int busyJobsOnCE = 0;
	private volatile int agedJobsOnCE = 0;
	private volatile int reusableJobsOnCE = 0;

	private HashMap<String, WorkerJob> jobTable = null;

	/** Unique job counter for the entire binder. */
	private final JobCounter jobCounter = JobCounter.getInstance();

	private String binderApplicationList;
	private HashSet<String> appList = null;
	private HashSet<String> filteredAppList = null;
	private AppFilter appFilter;

	private final double busyCoeff = 0.1;

	private final long threadInterval = 5000;

	private RecoveryManager recMan = null;
	private transient BinderPool binderPool = null; /* dont want to serialize it */

	public ComputingElement(String name, String shortName, RecoveryManager recMan, BinderPool binderPool) {
		this.name = name;
		this.shortName = shortName;
		this.maxJobsOnCE = BinderUtil
				.initIntField(shortName + "_MaxJobsOnCE", BinderUtil.initIntField("DefaultMaxJobsOnCE", 5));
		this.minJobsOnCE = BinderUtil
				.initIntField(shortName + "_MinJobsOnCE", BinderUtil.initIntField("DefaultMinJobsOnCE", 2));
		this.emmsj = BinderUtil.initIntField(shortName + "_MaxSecsSubmittedJob", BinderUtil.initIntField(
				"DefaultMaxSecsSubmittedJob", 1200)) * 1000;
		this.maxMillisReadyJob = BinderUtil.initIntField(shortName + "_MaxSecsReadyJob", BinderUtil.initIntField(
				"DefaultMaxSecsReadyJob", 3600)) * 1000;
		this.correctiveCoeffDiscardedJobs = BinderUtil.initIntField(shortName
				+ "_MaxSecsSubmittedJobCorrectiveCoefficientForDiscardedJobs", BinderUtil.initIntField(
				"DefaultMaxSecsSubmittedJobCorrectiveCoefficientForDiscardedJobs", 30)) * 1000;
		this.correctiveCoeffReadyJobs = BinderUtil.initIntField(shortName
				+ "_MaxSecsSubmittedJobCorrectiveCoefficientForReadyJobs", BinderUtil.initIntField(
				"DefaultMaxSecsSubmittedJobCorrectiveCoefficientForReadyJobs", 5)) * 1000;
		this.multiCoeffAgedJobs = BinderUtil.initIntField(shortName + "_MaxSecsSubmittedJobCorrectiveCoefficientForReadyJobs",
				BinderUtil.initIntField("DefaultMultiplicationCoefficientForMaxSecsAgedJobs", 5));
		this.jobTable = new HashMap<String, WorkerJob>();
		// this.binderApplicationList = ;
		initAppList(BinderUtil.getProperty(shortName + "_ApplicationList", BinderUtil.getProperty("DefaultApplicationList")));
		// this.maxWallClockTime = BinderUtil.initIntField(shortName +
		// "_MaxWallClockTime", BinderUtil.initIntField(
		// "DefaultMaxWallClockTime", 7200)) * 1000;
		this.maxMillisReusableJob = BinderUtil.initIntField(shortName + "_MaxSecsReusableJob", BinderUtil.initIntField(
				"DefaultMaxSecsReusableJob", 30)) * 1000;
		this.recMan = recMan;
		this.binderPool = binderPool;
	}

	private void initAppList(String binderAppList) {
		appFilter = (binderAppList != null) ? AppFilter.toAppFilter(binderAppList.trim()) : AppFilter.NONE;
		/* If appFilter is not FILTERED, there is no filter app list to read. */
		if (appFilter != AppFilter.FILTERED)
			return;
		StringTokenizer st = new StringTokenizer(binderAppList, " ");
		filteredAppList = new HashSet<String>();
		while (st.hasMoreTokens())
			filteredAppList.add(st.nextToken());
	}

	/**
	 * Updates the list of supported applications depending on the filtering
	 * rules provided by the binder configuration and the application list
	 * provided by the remote worker job.
	 * 
	 * @param appListing
	 *            The <code>String</code> containing application list.
	 */
	public synchronized void updateAppList(String appListing) {
		switch (appFilter) {
		case NONE:
			break;
		case ANY: {
			StringTokenizer st = new StringTokenizer(appListing, " ");
			appList = new HashSet<String>();
			while (st.hasMoreTokens())
				appList.add(st.nextToken());
		}
			break;
		case FILTERED: {
			StringTokenizer st = new StringTokenizer(appListing, " ");
			appList = new HashSet<String>();
			while (st.hasMoreTokens()) {
				String appID = st.nextToken();
				if (filteredAppList.contains(appID))
					appList.add(appID);
			}
		}
			break;
		}
	}

	/**
	 * Checks whether the CE supports the requested application.
	 * 
	 * @param appID
	 *            The <code>String</code> containing the ID of the application.
	 * @return <code>true</code> if the application is supported by the CE.
	 */
	public boolean isAppSupported(String appID) {
		if (appFilter == AppFilter.NONE || appList == null)
			return false;
		return appList.contains(appID);
	}

	public void initR(ComputingElement recoveredCE) {
		logger.debug("Attempting to restore all jobs for " + getShortName() + " .");
		this.jobTable = recMan.restoreAllJobs(this);
		/* copy all the important fields from the recoveredCE */
		if (recoveredCE != null) {
			this.emmsj = recoveredCE.emmsj;
		}
		logger.debug("Finished restoration for '" + shortName + "'.");
		initJobCount();
	}

	/** Initialize number of jobs after restoration. */
	private void initJobCount() {
		/* We only expect REUSABLE & SUBMITTED after restoration. */
		submittedJobsOnCE = 0;
		reusableJobsOnCE = 0;
		for (WorkerJob wj : jobTable.values()) {
			if (wj.getStatus() == JobStatus.SUBMITTED)
				submittedJobsOnCE++;
			else if (wj.getStatus() == JobStatus.REUSABLE)
				reusableJobsOnCE++;
		}
		logger.debug("CE '" + shortName + "': SUBMITTED: " + submittedJobsOnCE + ", REUSABLE: " + reusableJobsOnCE);
	}

	/**
	 * Submits a new worker job by executing a script and adds it into the CEs
	 * jobtable.
	 * <p>
	 * Note: Parameters are used mainly for event logging.
	 * 
	 * @param desired
	 *            Desired amount of jobs for the refill of the entire pool.
	 * @param actual
	 *            Actual amount of jobs submitted on the CE.
	 * @param reason
	 *            The <code>BinderStrategy</code> because of which the
	 *            submission was initiated.
	 */
	private synchronized boolean submitWorkerJob(int desired, int actual, BinderStrategy reason) {
		try {
			long timeStamp = System.currentTimeMillis();
			final String jobId = timeStamp + "-" + jobCounter.getNext();
			WorkerJob wj = new WorkerJob(jobId, JobStatus.SUBMITTED, timeStamp, timeStamp, reason);

			JobSubmit.getInstance().submit(wj, this);

			jobTable.put(jobId, wj);
			submittedJobsOnCE++;
			logger.debug("Saving worker job ID = " + wj.getJobID() + ".");
			recMan.saveWorkerJob(wj, this);
			logger.debug("Worker job saved.");
			if (BinderUtil.isPerfMonEnabled())
				Event.jobSubmission(wj, name, binderPool, desired, actual, reason);
		} catch (Exception e) {
			logger.error(e, e);
			// NOTE: sometimes an exception can occur and job might still be
			// submitted successfully?!
			return false;
		}
		return true;
	}

	/**
	 * Submits a desired number of worker jobs to the CE. However, depending on
	 * the binder working mode and limits imposed by the CE, some of the jobs
	 * might not get submitted.
	 * 
	 * @param n
	 *            The desired number of jobs to submit on the CE.
	 * @param desired
	 *            Desired amount of jobs for the refill of the entire pool.
	 * @param reason
	 *            The <code>BinderStrategy</code> because of which the
	 *            submission was initiated.
	 * @return The actual number of jobs that were submitted.
	 */
	public synchronized int submitWorkerJobs(int n, int desired, BinderStrategy reason) {
		// TODO recalculate limit?
		int jobsOnCE = (binderPool.getWorkingMode() == BinderMode.IDLE) ? minJobsOnCE : maxJobsOnCE;
		int limit = readyJobsOnCE + busyJobsOnCE + submittedJobsOnCE + reusableJobsOnCE;
		int num = (limit + n < jobsOnCE) ? n : jobsOnCE - limit;
		int actual = num > 0 ? num : 0;
		int total = 0;
		for (int i = 0; i < actual; i++) {
			if (submitWorkerJob(desired, actual, reason))
				total++;
		}
		return total;
	}

	/**
	 * Tests if the <code>WorkerJob</code> can be used by the client.
	 * 
	 * @param wj
	 *            <code>WorkerJob</code> which is tested.
	 * @param currentTime
	 *            Current time moment for which to test.
	 * @param requiredTime
	 *            Required time for the job to be able to execute.
	 * @return <code>true</code> if the job can serve the client.
	 */
	private boolean canJobPerform(WorkerJob wj, long currentTime, long requiredTime) {
		return wj.getStatus() == JobStatus.READY
				&& currentTime - wj.getActiveTimestamp() + requiredTime <= wj.getMaxWallClockTime();
	}

	/**
	 * Counts the number of available (READY) jobs that have enough time to
	 * execute.
	 * 
	 * @param currentTime
	 *            Current time moment for which to test.
	 * @param requiredTime
	 *            Required time for the job to be able to execute.
	 * @return The number of available jobs.
	 */
	public int getNumAvailableJobs(long currentTime, long requiredTime) {
		int num = 0;
		for (WorkerJob wj : jobTable.values())
			if (canJobPerform(wj, currentTime, requiredTime))
				num++;
		return num;
	}

	/**
	 * Searches for available READY job that will be alive long enough to
	 * execute work for the client.
	 * 
	 * @param currentTime
	 *            Current time moment for which to test.
	 * @param requiredTime
	 *            Required time for the job to be able to execute.
	 * @return The <code>WorkerJob</code> that was matched to the clients
	 *         requirement, or <code>null</code> if no job was found.
	 */
	public synchronized WorkerJob readyToBusy(long currentTime, long requiredTime) {
		for (WorkerJob wj : jobTable.values()) {
			if (canJobPerform(wj, currentTime, requiredTime)) {
				wj.setStatus(JobStatus.BUSY);
				wj.setStatusTimestamp(System.currentTimeMillis());
				readyJobsOnCE--; // TODO check for possible inconsistency
				busyJobsOnCE++;
				recMan.updateWorkerJob(wj);
				return wj;
			}
		}
		return null;
	}

	/**
	 * Switches the <code>WorkerJob</code> from SUBMITTED to READY and updates
	 * EMMSJ.
	 * 
	 * @param sw
	 *            Workers <code>SocketWrapper</code>.
	 * @return <code>true</code> if the job was switched to READY.
	 */
	public synchronized boolean submittedToReady(SocketWrapper sw) {

		String jobId = sw.getProtocolExchange().getWorkerJobID();
		WorkerJob wj = jobTable.get(jobId);

		if (wj != null) {
			if (wj.getStatus() == JobStatus.SUBMITTED) {
				long currentTime = System.currentTimeMillis();
				logger.debug("Adding worker job.");
				decreaseSubmittedJobs(wj, currentTime); /* Position? */
				readyJobsOnCE++;
				logger.debug("ComputingElement " + name + ": submitted->ready old EMMSJ " + emmsj + "ms.");
				if (!wj.isRestored()) {
					wj.setRestored(false);
					emmsj += (int) (((currentTime - wj.getStatusTimestamp() - emmsj) * ((double) correctiveCoeffReadyJobs / 100)) / 1000);
					recMan.updateComputingElement(this);
					logger.debug("ComputingElement " + name + ": submitted->ready new EMMSJ " + emmsj + "ms.");
				}
				if (BinderUtil.isPerfMonEnabled()) {
					int status = hasJobAged(wj, currentTime) ? 1 : 0;
					Event.jobArrival(wj.getReason(), sw, currentTime - wj.getStatusTimestamp(), status);
				}
				wj.setSw(sw);
				wj.setStatus(JobStatus.READY);
				wj.setStatusTimestamp(currentTime);
				wj.setActiveTimestamp(currentTime);
				wj.setMaxWallClockTime(readMaxWallClockTime(sw.getProtocolExchange().getWorkerMaxWallClockTime()));
				recMan.updateWorkerJob(wj);
				return true;
			} else {
				/* Job is not in submitted state. */
				return false;
			}
		}
		return false;
	}

	/**
	 * Returns max wall clock time in ms, from a string containing time in
	 * minutes.
	 */
	private long readMaxWallClockTime(String maxWallClockTime) {
		long result = 0;
		try {
			result = Long.parseLong(maxWallClockTime);
		} catch (NumberFormatException e) {
		}
		return result * 60000; /* from min to ms */
	}

	/**
	 * Decreases the number of submitted jobs. Depending on status timestamp,
	 * job can be aged or submitted, so aged or submitted count is decreased.
	 */
	private void decreaseSubmittedJobs(WorkerJob wj, long currentTime) {
		if (hasJobAged(wj, currentTime))
			agedJobsOnCE--;
		else
			submittedJobsOnCE--;
	}

	/**
	 * Switches job status from REUSABLE to READY. Updates status timestamp, but
	 * active timestamp remains unchanged.
	 */
	public synchronized boolean reusableToReady(SocketWrapper workerSW) {
		ProtocolExchange protocolExchange = workerSW.getProtocolExchange();
		String jobId = protocolExchange.getWorkerJobID();
		/* jobId - remove :N */
		if (jobId.lastIndexOf(":") > 0) {
			jobId = jobId.substring(0, jobId.lastIndexOf(":"));
		}
		WorkerJob wj = (WorkerJob) jobTable.get(jobId);
		if (wj != null) {
			if (wj.getStatus() == JobStatus.REUSABLE) {
				long currentTime = System.currentTimeMillis();
				if (BinderUtil.isPerfMonEnabled())
					Event.jobArrival(wj.getReason(), workerSW, currentTime - wj.getStatusTimestamp(), 2);
				wj.setRestored(false);
				wj.setSw(workerSW);
				wj.setStatus(JobStatus.READY);
				wj.setStatusTimestamp(currentTime);
				wj.setJobInstance(wj.getJobInstance() + 1);
				wj.setMaxWallClockTime(readMaxWallClockTime(protocolExchange.getWorkerMaxWallClockTime()));
				logger.debug("Reusing worker job JobID: " + wj.getJobID() + ":" + wj.getJobInstance() + ".");
				reusableJobsOnCE--;
				readyJobsOnCE++;
				recMan.updateWorkerJob(wj);
				return true;
			} else {
				/* job found, but not in REUSABLE state */
				logger.warn("Worker job " + protocolExchange.getWorkerJobID() + " is not in reusable state.");
				if (BinderUtil.isPerfMonEnabled())
					/* we send these 2 events just in this method, not both... */
					Event.jobArrival(null, workerSW, 0, 12);
				return false;
			}
		} else {
			logger.warn("Worker job " + protocolExchange.getWorkerJobID() + " does not exist in pool.");
			if (BinderUtil.isPerfMonEnabled())
				/*
				 * we send these 2 events just in this method, because
				 * submittedToReady is called 1st...
				 */
				Event.jobArrival(null, workerSW, 0, 11);
			return false;
		}
	}

	/**
	 * Switches <code>ServerJob</code>s state from BUSY to REUSABLE. Called
	 * after the job has finished work for the client.
	 * 
	 * @param jobId
	 *            The <code>String</code> representing Job ID.
	 * @return <code>true</code> if WorkerJob was switched to REUSABLE.
	 */
	public synchronized boolean busyToReusable(String jobId) {
		WorkerJob wj = jobTable.get(jobId);
		if (wj == null) {
			logger.warn("CE: " + name + " trying to reuse nonexisting job from pool!");
			return false;
		}
		if (wj.getStatus() == JobStatus.BUSY) {
			wj.setStatus(JobStatus.REUSABLE);
			wj.setStatusTimestamp(System.currentTimeMillis());
			busyJobsOnCE--;
			reusableJobsOnCE++;
			recMan.updateWorkerJob(wj);
			return true;
		}
		return false;
	}

	/**
	 * Removes a worker job from the computing element.
	 * 
	 * @param jobID
	 *            A <code>String</code> representing Job ID.
	 * @return <code>true</code> if a worker job was successfully removed.
	 */
	public synchronized boolean removeWorkerJob(String jobID) {
		boolean removed = false;
		WorkerJob wj = jobTable.get(jobID);
		if (wj != null) {
			if (wj.getStatus() == JobStatus.SUBMITTED || wj.getStatus() == JobStatus.REUSABLE) {
				jobTable.remove(jobID);
				recMan.removeWorkerJob(wj);
				removed = true;
				logger.debug("Worker job: " + jobID + " successfully removed.");
				if (wj.getStatus() == JobStatus.SUBMITTED)
					decreaseSubmittedJobs(wj, System.currentTimeMillis());
				else
					reusableJobsOnCE--;
			}
		} else
			logger.warn("Worker job: " + jobID + " not found.");
		return removed;
	}

	/**
	 * Checks whether a <code>WorkerJob</code> has aged or not. Makes sense only
	 * for SUBMITTED jobs.
	 * 
	 * @param wj
	 *            <code>WorkerJob</code> being checked.
	 * @param currentTime
	 *            Time moment for which job is checked.
	 * @return <code>true</code> if job has aged.
	 */
	private boolean hasJobAged(WorkerJob wj, long currentTime) {
		return wj.getStatus() == JobStatus.SUBMITTED && currentTime - wj.getStatusTimestamp() > emmsj;
	}

	/**
	 * Checks status of jobs on a CE. Depending on their timestamps jobs may
	 * change their states or be removed. Called outside from a status thread.
	 * 
	 * @throws IOException
	 */
	public synchronized void checkJobsStatus() throws IOException {

		Iterator<String> it = jobTable.keySet().iterator();
		/* Cheap trick, because we use same state for both aged and submitted. */
		int currentSubmitted = 0;
		int currentAged = 0;

		long currentTime = System.currentTimeMillis();
		while (it.hasNext()) {
			WorkerJob wj = jobTable.get(it.next());
			JobStatus status = wj.getStatus();
			switch (status) {
			case SUBMITTED:
				if (hasJobAged(wj, currentTime)) {
					if ((currentTime - wj.getStatusTimestamp()) > getMaxMillisAgedJob()) {
						/* Update emmsj only if job was not restored. */
						logger.debug("CE: " + shortName + ", JobID: " + wj.getJobID() + "; old EMMSJ: " + emmsj + "ms.");
						if (!wj.isRestored()) {
							emmsj += (int) (((currentTime - wj.getStatusTimestamp() - emmsj) * ((double) correctiveCoeffDiscardedJobs / 100)) / 1000);
							recMan.updateComputingElement(this);
						}
						logger.debug("CE: " + shortName + ": aged->removed, new EMMSJ: " + emmsj + "ms.");
						recMan.removeWorkerJob(wj);
						it.remove();
						if (BinderUtil.isPerfMonEnabled())
							Event.jobDiscarded(wj, name, currentTime - wj.getStatusTimestamp(), 0);
					} else {
						currentAged++;
					}
				} else {
					currentSubmitted++;
				}
				break;
			case READY:
				if ((currentTime - wj.getActiveTimestamp()) > wj.getMaxWallClockTime()) {
					logger.debug("CE: " + shortName + ", Job " + wj.getJobID() + ":" + wj.getJobInstance()
							+ " exceeded MaxWallClockTime and is removed.");
					wj.getSw().close(); /* shut down socket */
					recMan.removeWorkerJob(wj);
					it.remove(); // TODO decrease workers?
					readyJobsOnCE--;
					if (BinderUtil.isPerfMonEnabled())
						Event.jobDiscarded(wj, name, currentTime - wj.getActiveTimestamp(), 1);
					break;
				}
				if ((currentTime - wj.getStatusTimestamp()) > maxMillisReadyJob) {
					wj.getSw().close(); /* shut down socket */
					recMan.removeWorkerJob(wj);
					it.remove();
					readyJobsOnCE--;
					if (BinderUtil.isPerfMonEnabled())
						Event.jobDiscarded(wj, name, currentTime - wj.getStatusTimestamp(), 2);
					break;
				} // else readyJobsOnCE++;
				break;
			case BUSY:
				// busyJobsOnCE++;
				break;
			case REUSABLE:
				// TODO check if this makes sense, activetimestamp & reusable
				if ((currentTime - wj.getStatusTimestamp()) > wj.getMaxWallClockTime()) {
					logger.debug("CE: " + shortName + ", Job " + wj.getJobID() + ":" + wj.getJobInstance()
							+ " exceeded MaxWallClockTime and is removed.");
					recMan.removeWorkerJob(wj);
					it.remove();
					reusableJobsOnCE--;
					if (BinderUtil.isPerfMonEnabled())
						Event.jobDiscarded(wj, name, currentTime - wj.getStatusTimestamp(), 3);
					break;
				}
				if ((currentTime - wj.getStatusTimestamp()) > maxMillisReusableJob) {
					logger.debug("CE: " + shortName + ", Job " + wj.getJobID() + ":" + wj.getJobInstance()
							+ " did not reconnect in time and is removed.");
					recMan.removeWorkerJob(wj);
					it.remove();
					reusableJobsOnCE--;
					if (BinderUtil.isPerfMonEnabled())
						Event.jobDiscarded(wj, name, currentTime - wj.getStatusTimestamp(), 4);
					break;
				}
				break;
			}
		}
		submittedJobsOnCE = currentSubmitted;
		agedJobsOnCE = currentAged;
	}

	/**
	 * Checks if CE has already reached a maximum number of worker jobs. Called
	 * when the binder accepts connection from the worker.
	 */
	public synchronized boolean isCeFull() {
		switch (binderPool.getWorkingMode()) {
		case ACTIVE:
			return readyJobsOnCE >= maxJobsOnCE;
		case IDLE:
			return readyJobsOnCE >= minJobsOnCE;
		default: /* in case of unhandled situation declare CE as full */
			return true;
		}
	}

	/* Methods used in REGULAR refill strategy */

	/**
	 * Calculates refill count needed for the CE within an <i>emmsj</i> time
	 * interval. It is used for a REGULAR strategy on each CE.
	 * 
	 * @return Number of jobs needed for a refill.
	 */
	public synchronized double getRefillCount() {
		return targetPoolSize() - expectedPoolSize();
	}

	/** Calculates desired pool size for the CE. */
	private double targetPoolSize() {
		return minJobsOnCE + loadPreference();
	}

	/** Calculates load preference on the CE. */
	private double loadPreference() {
		return busyCoeff * busyJobsOnCE;
	}

	/**
	 * Calculates expected pool size within <i>emmsj</i> time period. Used in
	 * REGULAR refill strategy.
	 */
	private int expectedPoolSize() {
		int num = 0;
		long currentTime = System.currentTimeMillis();
		long futureTime = currentTime + emmsj;
		/* NOTE desinguish between active and status timestamp! */
		for (WorkerJob wj : jobTable.values()) {
			switch (wj.getStatus()) {
			case READY:
			case REUSABLE:
				if (futureTime - wj.getStatusTimestamp() < wj.getMaxWallClockTime())
					num++;
				break;
			case SUBMITTED:
				// if (futureTime - wj.getStatusTimestamp() < maxMillisReadyJob
				// + emmsj) CHECK!
				if (!hasJobAged(wj, currentTime))
					num++;
				break;
			case BUSY:
				if (wj.getSw().getProtocolExchange().getClientRequiredWallClockTime() + wj.getStatusTimestamp() < futureTime
						&& futureTime - wj.getStatusTimestamp() < wj.getMaxWallClockTime())
					num++;
				break;
			}
		}
		return num;
	}

	/* End of REGULAR refill methods. */

	/**
	 * Calculates the number of jobs that should become READY until the
	 * <code>arrivalMoment<code>. Used in panic (FULL-THROTTLE) strategy.
	 * <p>
	 * Counts SUBMITTED jobs that are not aged and are expected to become
	 * ready within the arrival moment (it is generally a very short time
	 * frame) and REUSABLE jobs.
	 * 
	 * @param arrivalMoment
	 *            The moment for which we calculate expected READY jobs.
	 * @return The number of expected READY jobs.
	 */
	public synchronized int getNumJobsExpectedToArrive(long arrivalMoment) {
		// TODO check if we wanna count submitted && reusable
		// (what time frame for reusable)
		int num = 0;
		long currentTime = System.currentTimeMillis();
		for (WorkerJob wj : jobTable.values()) {
			if ((wj.getStatus() == JobStatus.SUBMITTED && !hasJobAged(wj, currentTime) && wj.getStatusTimestamp() + emmsj < arrivalMoment)
					|| wj.getStatus() == JobStatus.REUSABLE)
				num++;
		}
		return num;
	}

	/**
	 * Check whether there are READY jobs that will have enough wall clock time
	 * to serve the client.
	 * <p>
	 * NOTE: Not used anymore, because <code>ComputingElement.readyToBusy</code>
	 * has been extended to inlcude this check.
	 * 
	 * @see <code>ComputingElement.readyToBusy</code>
	 */
	public synchronized boolean anyReadyJob(long currentTime, long requiredTime) {
		for (WorkerJob wj : jobTable.values()) {
			if (canJobPerform(wj, currentTime, requiredTime))
				return true;
		}
		return false;
	}

	/** Returns a string describing CE status. */
	public String getCEStatus() {
		String str = "CE " + name + ":" + "\n" + "  effectiveMaxMillisSubmittedJob:  " + emmsj + "\n"
				+ "  submittedJobsOnCE:  " + submittedJobsOnCE + "\n" + "  readyJobsOnCE:      " + readyJobsOnCE + "\n"
				+ "  busyJobsOnCE:       " + busyJobsOnCE + "\n" + "  agedJobsOnCE:       " + agedJobsOnCE + "\n";
		return str;
	}

	/** Returns a string describing CE status in html format. */
	public String getHtmlCEStatus() {
		// za shortPath skidamo sve sto je iza dve tacke
		String shortPath;
		String displayName;
		int ind = name.indexOf(":");
		shortPath = (ind == -1) ? name : name.substring(0, ind);
		// u shortName dodajemo i link
		String shortNameWithLink;
		shortNameWithLink = "<a href='" + BinderUtil.getHTMLGstatLink() + shortName + "/' target='_blank'>" + shortName
				+ "</a>";
		if (readyJobsOnCE + busyJobsOnCE == 0)
			displayName = "<i>" + shortNameWithLink + " " + shortPath + "</i>";
		else
			displayName = shortNameWithLink + " " + shortPath;

		String str = "<tr><td>" + displayName + "</td>" + "<td>" + getAppList() + "</td>" + "<td>" + readyJobsOnCE + "</td>"
				+ "<td>" + busyJobsOnCE + "</td>" + "<td>" + submittedJobsOnCE + "</td>" + "<td>" + reusableJobsOnCE + "</td>"
				+ "<td>" + agedJobsOnCE + "</td>" + "<td>" + emmsj + "</td></tr>";
		return str;
	}

	public int getEffectiveMaxSecsSubmittedJob() {
		/* millis to seconds */
		return emmsj / 1000;
	}

	public int getEffectiveMaxMillisSubmittedJob() {
		return emmsj;
	}

	public void setEffectiveMaxMillisSubmittedJob(int emmsj) {
		this.emmsj = emmsj;
	}

	public long getMaxMillisAgedJob() {
		return emmsj * multiCoeffAgedJobs;
	}

	public String getShortName() {
		return shortName;
	}

	public String getName() {
		return name;
	}

	public HashMap<String, WorkerJob> getJobTable() {
		return jobTable;
	}

	public void setJobTable(HashMap<String, WorkerJob> jt) {
		this.jobTable = jt;
	}

	/**
	 * @return
	 */
	public int getAgedJobsOnCE() {
		return agedJobsOnCE;
	}

	/**
	 * @return
	 */
	public int getBusyJobsOnCE() {
		return busyJobsOnCE;
	}

	/**
	 * @return
	 */
	public int getReadyJobsOnCE() {
		return readyJobsOnCE;
	}

	/**
	 * @return
	 */
	public int getSubmittedJobsOnCE() {
		return submittedJobsOnCE;
	}

	public int getReusableJobsOnCE() {
		return reusableJobsOnCE;
	}

	/**
	 * Lists the supported apps depending on the filtering selected from the
	 * binder config.
	 * 
	 * @return The <code>String</code> containing the list of supported apps.
	 */
	public String getAppList() {
		String prefix = "CE: " + shortName;
		switch (appFilter) {
		case ANY:
			return prefix + " accepts any app, current list is: " + (appList == null ? "[]" : appList) + ".";
		case NONE:
			return prefix + " is disabled in configuration (no app is allowed).";
		default:
			return prefix + " is filtered: " + filteredAppList + ", currently supported apps: "
					+ (appList == null ? "[]" : appList) + ".";
		}
	}

	public String getBinderApplicationList() {
		return binderApplicationList;
	}

	public long getThreadInterval() {
		return threadInterval;
	}

	static Logger logger = Logger.getLogger(ComputingElement.class);

}
