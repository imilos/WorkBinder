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
package yu.ac.bg.rcub.binder.recovery;

import java.util.HashMap;

import yu.ac.bg.rcub.binder.ComputingElement;
import yu.ac.bg.rcub.binder.job.WorkerJob;

/**
 * The interface used for the binder recovery engine. Different implementations
 * for recovery are supported with this interface. In case that no recovery is
 * needed, an adapter implementation <code>RecoveryManagerDummyAdapter</code>
 * is provided.
 * 
 * @author choppa
 * 
 */
public interface RecoveryManager {

	/**
	 * Initializes the recovery manager engine by changing the state of all the
	 * saved jobs that are not in the SUBMITTED state to the REUSABLE state so
	 * that they might get a chance to reconnect to the binder.
	 * <p>
	 * This new REUSABLE jobs will now have new status timestamp, while old
	 * SUBMITTED jobs will keep their old timestaps. However, if they age and
	 * get removed or they do connect to the binder, <i>emmsj</i> will not get
	 * updated for them.
	 * <p>
	 * NOTE: New status timestamp must be supplied as a parameter and not
	 * inserted by the recovery engine, because binders time zone and recovery
	 * repository timezone might not be in sync. For this argument in most cases
	 * <code>System.currentTimeMillis()</code> is expected.
	 * 
	 * @param statusTimestamp
	 *            The <code>long</code> value representing new status
	 *            timestamp for the modified jobs.
	 */
	public void initRecoveryManager(long statusTimestamp);

	/**
	 * Saves the specified <code>WorkerJob</code> on the specified
	 * <code>ComputingElement</code> in the recovery repository.
	 * 
	 * @param wj
	 *            The <code>WorkerJob</code> to be saved.
	 * @param ce
	 *            The <code>ComputingElement</code> to which the worker job
	 *            belongs.
	 */
	public void saveWorkerJob(WorkerJob wj, ComputingElement ce);

	/**
	 * Restores the specified <code>WorkerJob</code> from the recovery
	 * repository into the binder.
	 * <p>
	 * NOTE: Transient fields of the return type will most likely be
	 * <code>null</code> after restoration.
	 * 
	 * @param wj
	 *            The <code>WorkerJob</code> to be restored.
	 */
	public void restoreWorkerJob(WorkerJob wj);

	/**
	 * Removes the specified <code>WorkerJob</code> from the recovery
	 * repository.
	 * 
	 * @param wj
	 *            The <code>WorkerJob</code> to be removed.
	 */
	public void removeWorkerJob(WorkerJob wj);

	/**
	 * Updates the specified <code>WorkerJob</code> located in the recovery
	 * repository.
	 * 
	 * @param wj
	 *            The <code>WorkerJob</code> to be updated.
	 */
	public void updateWorkerJob(WorkerJob wj);

	/**
	 * Saves information about the <code>ComputingElement</code> into the
	 * recovery repository.
	 * 
	 * @param ce
	 *            The <code>ComputingElement</code> to be saved.
	 */
	public void saveComputingElement(ComputingElement ce);

	/**
	 * Restores information about the <code>ComputingElement</code> from the
	 * recovery repository into the binder.
	 * <p>
	 * NOTE: Transient fields of the return type will most likely be
	 * <code>null</code> after restoration.
	 * 
	 * @param ce
	 *            The <code>ComputingElement</code> to be restored.
	 * 
	 * @return The <code>ComputingElement</code> restored.
	 */
	public ComputingElement restoreComputingElement(ComputingElement ce);

	/**
	 * Removes information about the <code>ComputingElement</code> from the
	 * recovery repository.
	 * 
	 * @param ce
	 *            The <code>ComputingElement</code> to be removed.
	 */
	public void removeComputingElement(ComputingElement ce);

	/**
	 * Updates information about the <code>ComputingElement</code> located in
	 * the recovery repository.
	 * 
	 * @param ce
	 *            The <code>ComputingElement</code> to be updated.
	 */
	public void updateComputingElement(ComputingElement ce);

	/**
	 * Checks whether the recovery data exists in the recovery repository.
	 * 
	 * @return <b>true</b> if data exists.
	 */
	public boolean recoveryDataExists();

	/**
	 * Clears the recovery data from the recovery repository.
	 */
	public void clearRecoveryData();

	/**
	 * Restores all worker jobs for the specified <code>ComputingElement</code>.
	 * <p>
	 * For all restored jobs it is expected for this method to modify
	 * <i>restored</i> flag, so that the CE wont modify its <i>emmsj</i> when
	 * checking status of these jobs.
	 * <p>
	 * NOTE: Transient fields of the return type will most likely be
	 * <code>null</code> after restoration.
	 * 
	 * @param ce
	 *            The <code>ComputingElement</code> for which we restore
	 *            worker jobs.
	 * @return The <code>HashMap</code> containing
	 *         <code>(JobID, WorkerJob)</code> pairs.
	 */
	public HashMap<String, WorkerJob> restoreAllJobs(ComputingElement ce);

	/**
	 * Updates binder data located in the recovery repository.
	 * 
	 */
	public void updateRecoveryData();
}
