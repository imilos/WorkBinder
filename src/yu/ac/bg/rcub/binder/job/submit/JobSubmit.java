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
package yu.ac.bg.rcub.binder.job.submit;

import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.ComputingElement;
import yu.ac.bg.rcub.binder.job.WorkerJob;
import yu.ac.bg.rcub.binder.job.submit.script.ScriptSubmit;
import yu.ac.bg.rcub.binder.job.submit.wms.WMProxySubmit;

public abstract class JobSubmit {

	public abstract void submit(WorkerJob job, ComputingElement ce) throws Exception;

	private static JobSubmit instance = null;

	public static final JobSubmit getInstance() {
		if (instance != null)
			return instance;

		switch (BinderUtil.getSubmissionType()) {
		case INTERNAL:
			instance = new WMProxySubmit();
			break;
		case SCRIPT:
			instance = new ScriptSubmit();
			break;
		}
		return instance;
	}

}
