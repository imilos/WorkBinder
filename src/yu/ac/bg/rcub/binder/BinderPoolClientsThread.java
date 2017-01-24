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

import org.apache.log4j.Logger;

public class BinderPoolClientsThread extends Thread {

	private final BinderPool binderPool;
	private final long SAMPLING_INTERVAL;

	public BinderPoolClientsThread(BinderPool binderPool, long samplingInterval) {
		this.binderPool = binderPool;
		this.SAMPLING_INTERVAL = samplingInterval;
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(SAMPLING_INTERVAL);
				binderPool.updateClientFrequency();
				binderPool.updateMode();
				binderPool.checkForPanicMode();
			} catch (InterruptedException e) {
				logger.error(e, e);
				break;
			}
		}

	}

	Logger logger = Logger.getLogger(BinderPoolClientsThread.class);
}
