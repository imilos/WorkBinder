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

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.util.Enums.BinderStrategy;

public class ComputingElementStatusThread extends Thread {

	private ComputingElement ce = null;
	private final long thread_interval;
	private double refillError = 0;

	public ComputingElementStatusThread(ComputingElement ce) {
		super("CEStatusThread-" + ce.getShortName());
		this.ce = ce;
		this.thread_interval = ce.getThreadInterval();
	}

	private void regularRefillCE() {
		double refillCountPerInterval = ce.getRefillCount() * thread_interval / ce.getEffectiveMaxMillisSubmittedJob();
		int refillCountRounded = (int) Math.round(refillCountPerInterval - refillError);
		if (logger.isTraceEnabled())
			logger.trace("Regular refill count = " + refillCountPerInterval + ", old rounding error = " + refillError);
		//if (refillCountPerInterval > 0) {
		if (refillCountRounded > 0) {
			refillError += refillCountRounded - refillCountPerInterval;
			/* Total desired is same as desired for CE in this strat. */
			// What to do with refill counts if submitted 0 jobs?
			int submitted = ce.submitWorkerJobs(refillCountRounded, refillCountRounded, BinderStrategy.REGULAR);
			if (logger.isDebugEnabled())
				logger.debug("Regular CE strategy (submitted/wanted): " + submitted + "/" + refillCountRounded + ", CE = "
						+ ce.getShortName());
		} else
			refillError = 0;
	}

	public void run() {
		while (true) {
			try {
				if (logger.isTraceEnabled())
					logger.trace("Checking job status for CE: " + ce.getShortName() + ". Number of ready jobs : "
							+ ce.getReadyJobsOnCE() + ". Next check is in " + thread_interval + "ms.");
				// TODO CE status thread sleep time needs to be adjusted
				Thread.sleep(thread_interval);
				/* It is important to call checkJobs BEFORE regularRefillCE */
				ce.checkJobsStatus();
				regularRefillCE();
			} catch (InterruptedException ie) {
				logger.error(ie, ie);
				break;
			} catch (IOException e) {
				logger.error(e, e);
			}
		}
	}

	Logger logger = Logger.getLogger(ComputingElementStatusThread.class);
}
