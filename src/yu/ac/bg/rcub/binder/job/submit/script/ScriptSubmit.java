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
package yu.ac.bg.rcub.binder.job.submit.script;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderPool;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.ComputingElement;
import yu.ac.bg.rcub.binder.job.WorkerJob;
import yu.ac.bg.rcub.binder.job.submit.JobSubmit;

public class ScriptSubmit extends JobSubmit {
	static Logger logger = Logger.getLogger(ScriptSubmit.class);

	/**
	 * Simple thread that tests the Process being executed. Process is the
	 * worker job submission script being executed on the binder. This tests
	 * ensure (or more accuratly, increase the probability) that the script will
	 * actually be executed.
	 * <p>
	 * Newer (more demanding) version.
	 */
	private void processTweak(final Process pr, final String jobID) {
		new Thread() {
			public void run() {
				final BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()), 50);
				String line;
				int numLines = 0;
				final int MAX_LINES = 16;
				try {
					try {
						while (numLines++ < MAX_LINES && (line = br.readLine()) != null)
							logger.debug("PROCESS JOBID: " + jobID + ". Output: " + line);
					} catch (EOFException e) {
					}
					br.close();
				} catch (IOException e) {
					logger.error(e, e);
				}
			}
		}.start();
	}

	public void submit(WorkerJob job, ComputingElement ce) throws Exception {
		long timeStamp = job.getStatusTimestamp();
		String jobId = job.getJobID();
		String scriptString = BinderUtil.getScriptString();
		String routingInfo = "Started_at_" + timeStamp;
		String logFileName = BinderUtil.getFileOutputName(jobId);
		String[] execString = { scriptString, ce.getName(), routingInfo, jobId, logFileName };
		logger.debug("Executing script '" + execString[0] + " " + execString[1] + " " + execString[2] + " " + execString[3]
				+ "'...");
		ProcessBuilder pb = new ProcessBuilder(execString);
		pb.redirectErrorStream(true);
		final Process pr = pb.start();
		if (BinderPool.PROCESS_TWEAK_ENABLED)
			processTweak(pr, jobId);
	}

}
