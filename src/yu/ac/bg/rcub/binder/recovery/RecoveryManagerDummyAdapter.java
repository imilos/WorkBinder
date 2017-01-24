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
 * An abstract adapter implementation of the RecoveryManager interface. The
 * methods in this class are empty. This class exists as convenience to allow
 * users to not use Binder recovery capabilities.
 * 
 * @author choppa
 * 
 */
public class RecoveryManagerDummyAdapter implements RecoveryManager {

	public void clearRecoveryData() {

	}

	public void initRecoveryManager(long statusTimeStamp) {

	}

	public boolean recoveryDataExists() {
		return false;
	}

	public void removeComputingElement(ComputingElement ce) {

	}

	public void removeWorkerJob(WorkerJob wj) {

	}

	public HashMap<String, WorkerJob> restoreAllJobs(ComputingElement ce) {
		/*
		 * Return original (unchanged) JobTable, another option is to return an
		 * empty HashMap, which is more accurate according to the interface.
		 */
		return ce.getJobTable();
	}

	public ComputingElement restoreComputingElement(ComputingElement ce) {
		/* return CE unchanged */
		return ce;
	}

	public void restoreWorkerJob(WorkerJob wj) {

	}

	public void saveComputingElement(ComputingElement ce) {

	}

	public void saveWorkerJob(WorkerJob wj, ComputingElement ce) {

	}

	public void updateComputingElement(ComputingElement ce) {

	}

	public void updateRecoveryData() {

	}

	public void updateWorkerJob(WorkerJob wj) {

	}

}
