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
package yu.ac.bg.rcub.binder.job;

import java.io.Serializable;

import yu.ac.bg.rcub.binder.util.Enums.BinderStrategy;
import yu.ac.bg.rcub.binder.util.Enums.JobStatus;
import yu.ac.bg.rcub.binder.net.SocketWrapper;

public class WorkerJob implements Serializable {

	private static final long serialVersionUID = 7056266822425795852L;
	/** unique jobID, timestamp_count */
	private String jobID;
	/** instance of reusable job */
	private int jobInstance;
	private JobStatus status;
	private long creationTimestamp;
	/** last state change timestamp */
	private long statusTimestamp;
	/** timestamp when job becomes READY */
	private long activeTimestamp;
	private SocketWrapper sw;
	/** max wall clock time in ms */
	private long maxWallClockTime = 0;

	/** reason because the job was submitted */
	private BinderStrategy reason;

	/** Used to indicate whether the job was restored. */
	private boolean restored = false;

	public WorkerJob(String jobID, JobStatus status, long statusTimestamp, long creationTimestamp, BinderStrategy reason) {
		this.jobID = jobID;
		this.status = status;
		this.statusTimestamp = statusTimestamp;
		this.creationTimestamp = creationTimestamp;
		this.sw = null;
		this.jobInstance = 0;
		this.reason = reason;
	}

	public WorkerJob(String jobID, JobStatus status, long statusTimestamp, long creationTimestamp, long activeTimestamp,
			int jobInstance, SocketWrapper sw, BinderStrategy reason) {
		this.jobID = jobID;
		this.status = status;
		this.statusTimestamp = statusTimestamp;
		this.creationTimestamp = creationTimestamp;
		this.activeTimestamp = activeTimestamp;
		this.sw = sw;
		this.jobInstance = jobInstance;
		this.reason = reason;
	}

	/**
	 * vraca nam ukupno vreme koje je job proveo u sistemu do ovog trenutka
	 */
	public long getTotalTime() {
		return System.currentTimeMillis() - creationTimestamp;
	}

	/**
	 * vraca nam ukupno vreme koje je job proveo u trenutnom statusu do ovog
	 * trenutka
	 */
	public long getStatusTime() {
		return System.currentTimeMillis() - statusTimestamp;
	}

	/**
	 * vraca nam ukupno vreme od trenutka prvog aktiviranja job-a
	 */
	public long getActiveTime() {
		return System.currentTimeMillis() - activeTimestamp;
	}

	public long getActiveTimestamp() {
		return activeTimestamp;
	}

	public void setActiveTimestamp(long l) {
		activeTimestamp = l;
	}

	public String getJobID() {
		return jobID;
	}

	public void setJobID(String string) {
		jobID = string;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public SocketWrapper getSw() {
		return sw;
	}

	public void setSw(SocketWrapper wrapper) {
		sw = wrapper;
	}

	public long getStatusTimestamp() {
		return statusTimestamp;
	}

	public void setStatusTimestamp(long l) {
		statusTimestamp = l;
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	public void setCreationTimestamp(long l) {
		creationTimestamp = l;
	}

	/**
	 * @return
	 */
	public int getJobInstance() {
		return jobInstance;
	}

	/**
	 * @param i
	 */
	public void setJobInstance(int i) {
		jobInstance = i;
	}

	public boolean isRestored() {
		return restored;
	}

	public void setRestored(boolean restored) {
		this.restored = restored;
	}

	public long getMaxWallClockTime() {
		return maxWallClockTime;
	}

	public void setMaxWallClockTime(long maxWallClockTime) {
		this.maxWallClockTime = maxWallClockTime;
	}

	public BinderStrategy getReason() {
		return reason;
	}

	public void setReason(BinderStrategy reason) {
		this.reason = reason;
	}

}
