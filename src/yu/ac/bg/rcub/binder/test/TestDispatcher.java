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
package yu.ac.bg.rcub.binder.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import yu.ac.bg.rcub.binder.BinderUtil;

/**
 * Test class used to check performance of the binder. It is used instead of the
 * standard <code>WorkerDispatcher</code> to submit new worker jobs. It
 * simulates CE delays in different ways. To actually submit a job, it calls the
 * regular <code>WorkerDispatcher</code> after it has done its work.
 * 
 * 
 * @author choppa
 * 
 */
public class TestDispatcher {
	private String CE;
	private String propPath;
	private String jobID;
	private String logFileName;
	private String routingInfo = "";
	// private String maxWallClockTime;
	private PrintWriter fout = null;
	private final boolean PROCESS_TWEAK_ENABLED = true;

	private final String scriptString = "\"C:/Documents and Settings/choppa/eclipse/workspace/binder/startBinderJob.bat\"";

	private Properties properties = new Properties();

	private TestDispatcher(String propertiesPath, String CE, String routingInfo, String jobID, String logFileName,
			String maxWallClockTime) {
		this.propPath = propertiesPath;
		this.CE = CE;
		this.jobID = jobID;
		this.logFileName = logFileName;
		this.routingInfo = routingInfo;
		// this.maxWallClockTime = maxWallClockTime;
		init();
	}

	private TestDispatcher(String propertiesPath, String CE, String routingInfo, String jobID, String logFileName,
			String maxWallClockTime, String jobStatus) {
		this.propPath = propertiesPath;
		this.CE = CE;
		this.jobID = jobID;
		this.logFileName = logFileName;
		this.routingInfo = routingInfo;
		// this.maxWallClockTime = maxWallClockTime;
		init();
	}

	private void init() {
		/*
		 * init properties if it hasn't beed initiated already, this is because
		 * we use static properties field
		 */
		if (properties.isEmpty()) {
			try {
				properties.load(new FileInputStream(new File(propPath)));
			} catch (FileNotFoundException e1) {
				System.err.println("Worker:   *** ERR *** WorkerDispatcher properties file cannot be opened!");
				e1.printStackTrace();
			} catch (IOException e2) {
				System.err.println("Worker:   *** ERR *** Error reading WorkerDispatcher properties file!");
				e2.printStackTrace();
			}
		}

		try {
			fout = BinderUtil.getFileOutput(logFileName, "", properties.getProperty("ReportsLocationDir") + "TEST/");
		} catch (FileNotFoundException e) {
			System.err.println("Worker:   *** ERR *** Could not open new output file!!!");
			e.printStackTrace();
		}
	}

	public void run() {
		log("Test Dispatcher started...");
		try {
			int ceNum = Integer.parseInt(CE.charAt(5) + "");
			int waitTime = 0;
			log("CE name " + CE + ".");
			log("Client routing info = " + routingInfo + ".");
			log("JobID = " + jobID + ".");

			// if (ceNum == 7) {
			// log("7th CE chosen to simulate fail...");
			// System.exit(0);
			// }

			switch (ceNum % 3) {
			case 0:
				/* Fast CEs */
				waitTime = (int) (Math.random() * 2000);
				log("Fast CE - short wait: " + waitTime + "ms.");
				break;
			case 1:
				/* Moderate CEs */
				waitTime = (int) (Math.random() * 3000) + 8000;
				log("Moderate CE - moderate wait: " + waitTime + "ms.");
				break;
			case 2:
				/* Slow CEs */
				waitTime = (int) (Math.random() * 5000) + 20000;
				log("Slow CE - long wait: " + waitTime + "ms.");
				break;
			}
			Thread.sleep(waitTime);
			String script = scriptString + " " + CE + " " + routingInfo + " " + jobID + " " + logFileName;

			final Process pr = Runtime.getRuntime().exec(script);
			if (PROCESS_TWEAK_ENABLED)
				processTweak(pr, jobID);

			log("Executed script = " + script);
		} catch (Exception e) {
			log("Error!", e);
		}
	}

	/** Older (liter) version */
	private void processTweak(final Process pr, final String jobID) {
		new Thread() {
			private boolean running = true;
			private final int CHECK_CYCLES = 4;
			private int check = 0;

			public void run() {
				while (running && check < CHECK_CYCLES) {
					try {
						pr.exitValue();
						running = false;
					} catch (IllegalThreadStateException e) {
						try {
							sleep(500);
						} catch (InterruptedException e1) {
						}
					}
					check++;
				}
				if (!running)
					log("PROCESS EXIT VALUE: " + pr.exitValue() + " JOBID: " + jobID);
				else
					log("PROCESS JOBID: " + jobID + " did not finish in 2s.");
			}
		}.start();
	}

	private void log(Object message) {
		log(message, null);
	}

	private void log(Object message, Throwable t) {
		if (fout != null) {
			fout.println(message);
			if (t != null)
				t.printStackTrace(fout);
			fout.flush();
		}
	}

	public static void main(String[] args) {

		if (args.length == 6) {
			TestDispatcher worker = new TestDispatcher(args[0], args[1], args[2], args[3], args[4], args[5]);
			worker.run();
		} else if (args.length == 7) {
			TestDispatcher worker = new TestDispatcher(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
			worker.run();
		} else {
			System.err.println("Need 6 arguments to start!");
			System.err.println("Worker:   Argument 1 - Path to .properties file");
			System.err.println("Worker:   Argument 2 - Full CE name");
			System.err.println("Worker:   Argument 3 - Routing Information (NO white spaces allowed!!!)");
			System.err.println("Worker:   Argument 4 - JobID");
			System.err.println("Worker:   Argument 5 - Log Filename");
			System.err.println("Worker:   Argument 6 - MaxWallClockTime (in minutes)");
			System.err.println("Worker:   Argument 7 - FINISHED [optional argument]");
		}
	}
}
