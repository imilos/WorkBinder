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

import java.util.HashMap;
import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import yu.ac.bg.rcub.binder.ComputingElement;
import yu.ac.bg.rcub.binder.util.Enums.JobStatus;
import yu.ac.bg.rcub.binder.job.WorkerJob;
import yu.ac.bg.rcub.binder.recovery.RecoveryManager;

@Stateless
@Remote(RecoveryManager.class)
public class RecoveryManagerBean implements RecoveryManager {
	@PersistenceContext(unitName = "binderRecovery")
	private EntityManager manager;

	public RecoveryManagerBean() {
	}

	public void initRecoveryManager(long statusTimestamp) {
		Query q = manager.createQuery("UPDATE RecoveryWorkerJob rwj SET rwj.status = ?1, rwj.statusTimestamp = ?2"
				+ " WHERE rwj.status != ?3");
		q.setParameter(1, JobStatus.REUSABLE.toInt());
		q.setParameter(2, statusTimestamp);
		q.setParameter(3, JobStatus.SUBMITTED);
		q.executeUpdate();
	}

	public void saveWorkerJob(WorkerJob wj, ComputingElement ce) {
		RecoveryComputingElement rce = (RecoveryComputingElement) manager.createQuery(
				"SELECT rce FROM RecoveryComputingElement rce WHERE rce.shortName = ?1").setParameter(1, ce.getShortName())
				.getSingleResult();
		RecoveryWorkerJob rwj = new RecoveryWorkerJob(wj, rce);
		manager.persist(rwj);
	}

	public void restoreWorkerJob(WorkerJob wj) {
	}

	public HashMap<String, WorkerJob> restoreAllJobs(ComputingElement ce) {

		Query q = manager.createQuery("SELECT rce FROM RecoveryComputingElement rce WHERE rce.shortName = ?1");
		q.setParameter(1, ce.getShortName());
		RecoveryComputingElement rce = (RecoveryComputingElement) q.getSingleResult();

		q = manager.createQuery("SELECT rwj FROM RecoveryWorkerJob rwj WHERE rwj.recoveryComputingElement = ?1");
		q.setParameter(1, rce);
		List<?> listJobs = q.getResultList();
		HashMap<String, WorkerJob> jt = new HashMap<String, WorkerJob>();
		for (Object o : listJobs) {
			/*
			 * Socket wrapper is null, it needs to be initialized externally,
			 * when WorkerJob tries to reestablish the connection.
			 */
			RecoveryWorkerJob rwj = (RecoveryWorkerJob) o;
			WorkerJob wj = new WorkerJob(rwj.getJobID(), rwj.getStatus(), rwj.getStatusTimestamp(), rwj.getCreationTimestamp(),
					rwj.getActiveTimestamp(), rwj.getJobInstance(), null, rwj.getReason());
			wj.setMaxWallClockTime(rwj.getMaxWallClockTime());
			wj.setRestored(true); /* indicate that the job was restored */
			jt.put(wj.getJobID(), wj);
		}
		return jt;
	}

	public void removeWorkerJob(WorkerJob wj) {
		manager.createQuery("DELETE FROM RecoveryWorkerJob rwj WHERE rwj.jobID = ?1").setParameter(1, wj.getJobID())
				.executeUpdate();
	}

	public void updateWorkerJob(WorkerJob wj) {
		RecoveryWorkerJob rwj = (RecoveryWorkerJob) manager.createQuery(
				"SELECT rwj FROM RecoveryWorkerJob rwj WHERE rwj.jobID = ?1").setParameter(1, wj.getJobID()).getSingleResult();
		rwj.setStatus(wj.getStatus());
		rwj.setStatusTimestamp(wj.getStatusTimestamp());
		rwj.setActiveTimestamp(wj.getActiveTimestamp());
		rwj.setJobInstance(wj.getJobInstance());
		rwj.setMaxWallClockTime(wj.getMaxWallClockTime());
		rwj.setReason(wj.getReason());
		manager.persist(rwj);
	}

	public void saveComputingElement(ComputingElement ce) {
		RecoveryComputingElement rce = new RecoveryComputingElement(ce);
		manager.persist(rce);
	}

	public ComputingElement restoreComputingElement(ComputingElement ce) {
		try {
			RecoveryComputingElement rce = (RecoveryComputingElement) manager.createQuery(
					"SELECT rce FROM RecoveryComputingElement rce WHERE rce.shortName = ?1").setParameter(1, ce.getShortName())
					.getSingleResult();
			ce.setEffectiveMaxMillisSubmittedJob(rce.getEmmsj());
		} catch (NoResultException e) {
			/* CE not found in the database, save it. */
			saveComputingElement(ce);
		}
		return ce;
	}

	public void removeComputingElement(ComputingElement ce) {
		manager.createQuery("DELETE FROM RecoveryComputingElement rce WHERE rce.shortName = ?1").setParameter(1,
				ce.getShortName()).executeUpdate();
	}

	public void updateComputingElement(ComputingElement ce) {
		RecoveryComputingElement rce = (RecoveryComputingElement) manager.createQuery(
				"SELECT rce FROM RecoveryComputingElement rce WHERE rce.shortName = ?1").setParameter(1, ce.getShortName())
				.getSingleResult();

		rce.setEmmsj(ce.getEffectiveMaxMillisSubmittedJob());
		manager.persist(rce);
	}

	public boolean recoveryDataExists() {
		return !manager.createQuery("select rce from RecoveryComputingElement rce").getResultList().isEmpty();
	}

	public void clearRecoveryData() {
		manager.createQuery("delete from RecoveryWorkerJob").executeUpdate();
		manager.createQuery("delete from RecoveryComputingElement").executeUpdate();
	}

	public void updateRecoveryData() { // TODO not good! Maybe remove all!
		updateBusyToReusable();
		updateReadyToReusable();
	}

	/* dodatne metode - check JobStatus type Enum or int?! */
	public void updateBusyToReusable() {
		manager.createQuery("UPDATE RecoveryWorkerJob rwj SET rwj.status = ?1, rwj.statusTimestamp = ?2 WHERE rwj.status = ?3")
				.setParameter(1, JobStatus.REUSABLE.toInt()).setParameter(2, System.currentTimeMillis()).setParameter(3,
						JobStatus.BUSY).executeUpdate();
	}

	public void updateReadyToReusable() {
		manager.createQuery("UPDATE RecoveryWorkerJob rwj SET rwj.status = ?1, rwj.statusTimestamp = ?2 WHERE rwj.status = ?3")
				.setParameter(1, JobStatus.REUSABLE.toInt()).setParameter(2, System.currentTimeMillis()).setParameter(3,
						JobStatus.READY).executeUpdate();
	}

}
