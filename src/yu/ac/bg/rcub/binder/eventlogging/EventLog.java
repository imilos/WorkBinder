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
package yu.ac.bg.rcub.binder.eventlogging;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderUtil;
import archiver.beanwrapper.JNDIPerformanceMeasurementArchiveAccess;
import archiver.beanwrapper.PerformanceMeasurementArchiveAccess;

/**
 * Provides access to the Event Logger application.
 * 
 * @author choppa
 * 
 */
public class EventLog extends Thread {

	// /** Buffer size for event messages. */
	// private static final int BUFF_SIZE = 100;
	/** Event message buffer. */
	private BlockingQueue<Event> eventQueue;
	// /** Interval in which messages are sent. */
	// private static final int SEND_INTERVAL = 2000;
	private boolean running = true;

	private PerformanceMeasurementArchiveAccess accessObj;

	public EventLog() {
		init();
	}

	private void init() {
		Properties sProps = new Properties();
		sProps.put("java.naming.factory.initial", BinderUtil.getProperty("java.naming.factory.initial"));
		sProps.put("java.naming.factory.url.pkgs", BinderUtil.getProperty("java.naming.factory.url.pkgs"));
		sProps.put("java.naming.provider.url", BinderUtil.getProperty("java.naming.provider.url"));
		sProps.put("org.jboss.naming.client", BinderUtil.getProperty("org.jboss.naming.client"));
		sProps.put("domain", BinderUtil.getProperty("PerformanceMonitoringDomainName"));
		sProps.put("password", BinderUtil.getProperty("PerformanceMonitoringDomainPassword"));
		sProps.put("jndiName", BinderUtil.getProperty("PerformanceMonitoringJndiName"));

		accessObj = JNDIPerformanceMeasurementArchiveAccess.getAccess(sProps);
		/* do something if access failed?! */
		// BinderUtil.PERFORMANCE_MONITORING = accessObj != null;
		if (accessObj == null)
			logger.error("Failed obtaining event logger stub!");
		else
			logger.info("Event logger stub obtained.");
		eventQueue = new LinkedBlockingQueue<Event>();
	}

	public void run() {

		/* Consumer, only one and blocking... */
		while (running) {
			try {
				Event event = eventQueue.take();
				if (logger.isDebugEnabled())
					logger.debug("Sending " + event);
				/* crappy interface... */
				accessObj.storeEvent(event.getTargetType(), event.getEventType(), event.getDate(), event.getSource(), event
						.getSourceDetail(), event.getDestination(), event.getDestinationDetail(), event.getValue(), event
						.getValueQualifier());
				logger.debug("New event stored in the event logger.");

			} catch (InterruptedException ie) {
				logger.error("Interupted, error = '" + ie.getMessage() + "'!");
				running = false;
			} catch (Exception e) {
				logger.error("Failed to send the event, error = '" + e.getMessage() + "'!");
				// running = false;
			}
		}
		logger.debug("Event logger stopped.");
	}

	public void sendEvent(Event event) {
		if (!running) {
			if (logger.isDebugEnabled())
				logger.debug("Event logger is offline, ignoring this " + event);
			return;
		}

		try {
			/* Producer, one out of many... */
			if (logger.isDebugEnabled())
				logger.debug("Buffering " + event);
			eventQueue.put(event);
			logger.debug("Event buffered.");
		} catch (InterruptedException e) {
			logger.error("Interupted, error = '" + e.getMessage() + "'!");
			// running = false;
		}
	}

	private static Logger logger = Logger.getLogger(EventLog.class);
}
