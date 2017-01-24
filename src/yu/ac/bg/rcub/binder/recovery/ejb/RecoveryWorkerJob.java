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
package yu.ac.bg.rcub.binder.recovery.ejb;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import yu.ac.bg.rcub.binder.util.Enums.BinderStrategy;
import yu.ac.bg.rcub.binder.util.Enums.JobStatus;
import yu.ac.bg.rcub.binder.job.WorkerJob;

@Entity
@Table(name = "RecoveryWorkerJob")
public class RecoveryWorkerJob implements Serializable {

	private static final long serialVersionUID = -8865401160828371469L;

	private int id;

	private String jobID;
	private int jobInstance;
	private JobStatus status; // TODO keep int or Enum for the JobStatus?!
	/* Check Bean JobStatus.toInt()! */
	private long creationTimestamp;
	private long statusTimestamp;
	private long activeTimestamp;
	private long maxWallClockTime = 0;

	private BinderStrategy reason;

	private RecoveryComputingElement recoveryComputingElement;

	public RecoveryWorkerJob() {
	}

	public RecoveryWorkerJob(WorkerJob wj, RecoveryComputingElement rce) {
		this.jobID = wj.getJobID();
		this.status = wj.getStatus();
		this.creationTimestamp = wj.getCreationTimestamp();
		this.statusTimestamp = wj.getStatusTimestamp();
		this.activeTimestamp = -1;
		this.jobInstance = 0;
		this.maxWallClockTime = wj.getMaxWallClockTime();
		this.recoveryComputingElement = rce;
		this.reason = wj.getReason();
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getJobID() {
		return jobID;
	}

	public void setJobID(String jobID) {
		this.jobID = jobID;
	}

	public int getJobInstance() {
		return jobInstance;
	}

	public void setJobInstance(int jobInstance) {
		this.jobInstance = jobInstance;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	// @Temporal(TemporalType.TIMESTAMP)
	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	public void setCreationTimestamp(long creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	// @Temporal(TemporalType.TIMESTAMP)
	public long getStatusTimestamp() {
		return statusTimestamp;
	}

	public void setStatusTimestamp(long statusTimestamp) {
		this.statusTimestamp = statusTimestamp;
	}

	// @Temporal(TemporalType.TIMESTAMP)
	public long getActiveTimestamp() {
		return activeTimestamp;
	}

	public void setActiveTimestamp(long activeTimestamp) {
		this.activeTimestamp = activeTimestamp;
	}

	public long getMaxWallClockTime() {
		return maxWallClockTime;
	}

	public void setMaxWallClockTime(long maxWallClockTime) {
		this.maxWallClockTime = maxWallClockTime;
	}

	@ManyToOne
	@JoinColumn(name = "RCE_ID")
	public RecoveryComputingElement getRecoveryComputingElement() {
		return recoveryComputingElement;
	}

	public void setRecoveryComputingElement(RecoveryComputingElement recoveryComputingElement) {
		this.recoveryComputingElement = recoveryComputingElement;
	}

	public BinderStrategy getReason() {
		return reason;
	}

	public void setReason(BinderStrategy reason) {
		this.reason = reason;
	}

}
