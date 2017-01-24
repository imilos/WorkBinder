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

import org.apache.log4j.Logger;

public class StartTest {

	public static void main(String[] args) {

		int numClients = 20;
		if (args.length == 1)
			numClients = Integer.parseInt(args[0]);
		try {
			while (true) {
				for (int i = 0; i < numClients; i++) {
					int waitTime = (int) ((Math.random() * 1000) + 1000);
					logger.debug("Starting new client with wait time = " + waitTime + ".");
					new TestClient("client.properties", waitTime + "").start();
				}
				logger.info("===== Finished first portion - lots of quick jobs. Waiting 1min. =====");
				Thread.sleep(60000);

				for (int i = 0; i < numClients / 2; i++) {
					int waitTime = (int) ((Math.random() * 5000) + 10000);
					logger.debug("Starting new client with wait time = " + waitTime + ".");
					new TestClient("client.properties", waitTime + "").start();
				}
				logger.info("===== Finished second portion - moderate amount of moderate jobs. Waiting 1min. =====");
				Thread.sleep(60000);

				for (int i = 0; i < numClients / 4; i++) {
					int waitTime = (int) ((Math.random() * 20000) + 240000);
					logger.debug("Starting new client with wait time = " + waitTime + ".");
					new TestClient("client.properties", waitTime + "").start();
				}
				logger.info("===== Finished third portion - small amount of slow jobs. Waiting 1min. =====");
				Thread.sleep(60000);
			}

		} catch (Exception e) {
			logger.error(e, e);
		}
	}

	static Logger logger = Logger.getLogger(StartTest.class);
}
