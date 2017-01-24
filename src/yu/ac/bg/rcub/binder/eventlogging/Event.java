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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import yu.ac.bg.rcub.binder.BinderPool;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.util.Enums.BinderStrategy;
import yu.ac.bg.rcub.binder.job.WorkerJob;
import yu.ac.bg.rcub.binder.net.SocketWrapper;

/**
 * Represents an event that can be logged into the Event Logger. Also contains
 * static methods to record custom and predefined events.
 * 
 * @author choppa
 * 
 */
public class Event implements Serializable {

	private static final long serialVersionUID = 9129091835504842714L;

	private String targetType;
	private String eventType;
	private String source;
	private String sourceDetail;
	private String destination;
	private String destinationDetail;
	private float value;
	private Date date;
	private int valueQualifier;

	public Event() {
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourceDetail() {
		return sourceDetail;
	}

	public void setSourceDetail(String sourceDetail) {
		this.sourceDetail = sourceDetail;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getDestinationDetail() {
		return destinationDetail;
	}

	public void setDestinationDetail(String destinationDetail) {
		this.destinationDetail = destinationDetail;
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}

	public int getValueQualifier() {
		return valueQualifier;
	}

	public void setValueQualifier(int valueQualifier) {
		this.valueQualifier = valueQualifier;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Event:");
		sb.append("\n\t Target type: ");
		sb.append(targetType);
		sb.append("\n\t Event type: ");
		sb.append(eventType);
		sb.append("\n\t Source: ");
		sb.append(source);
		sb.append("\n\t Source detail: ");
		sb.append(sourceDetail);
		sb.append("\n\t Destination: ");
		sb.append(destination);
		sb.append("\n\t Destination detail: ");
		sb.append(destinationDetail);
		sb.append("\n\t Date: ");
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy. 'at' HH:mm:ss");
		sb.append(sdf.format(date));
		sb.append("\n\t Value: ");
		sb.append(value);
		sb.append("\n\t Value Qualifier: ");
		sb.append(valueQualifier);
		return sb.toString();
	}

	/** Record a custom event specified by fields. */
	public static void recordEvent(String targetType, String eventType, Date date, String source, String sourceDetail,
			String destination, String destinationDetail, float value, int valueQualifier) {

		final Event e = new Event();
		e.setDate(date);
		e.setDestination(destination);
		e.setDestinationDetail(destinationDetail);
		e.setEventType(eventType);
		e.setSource(source);
		e.setSourceDetail(sourceDetail);
		e.setTargetType(targetType);
		e.setValue(value);
		e.setValueQualifier(valueQualifier);

		recordEvent(e);
	}

	/** Record a custom <code>Event</code>. */
	public static void recordEvent(final Event e) {
		new Thread() {
			public void run() {
				BinderUtil.eventLog.sendEvent(e);
			}
		}.start();
	}

	/**
	 * Event sent when the client connects.
	 * 
	 * @param reason
	 *            with values:
	 *            <ul>
	 *            <li><b>0</b> - client connected asking for a job</li>
	 *            <li><b>1</b> - client connected asking for a CE_QUERY</li>
	 *            </ul>
	 * 
	 */
	public static void clientConnected(SocketWrapper clientSW, int reason) {
		Event e = new Event();
		e.targetType = "clientConnect";
		e.eventType = clientSW.getProtocolExchange().getClientApplicationID();
		e.date = new Date();
		e.source = clientSW.getSocket().getInetAddress().toString();
		e.sourceDetail = "";
		e.destinationDetail = clientSW.getProtocolExchange().getClientCandidateCEs().trim();
		e.destination = e.destinationDetail.equals("") ? "Any" : "List";
		e.valueQualifier = reason;
		recordEvent(e);
	}

	/**
	 * Event sent when the client disconnects.
	 * 
	 * @param reason
	 *            an <code>int</code> with values:
	 *            <ul>
	 *            <li><b>0</b> - client was successfully processed and got a
	 *            ready job</li>
	 *            <li><b>1</b> - client was successfully processed and executed
	 *            a query</li>
	 *            <li><b>10</b> - no ready jobs for the client</li>
	 *            <li><b>11</b> - client failed auth</li>
	 *            </ul>
	 * 
	 */
	public static void clientDisconnected(SocketWrapper clientSW, long duration, int reason) {
		Event e = new Event();
		e.targetType = "clientDisconnect";
		e.eventType = clientSW.getProtocolExchange().getClientApplicationID();
		e.date = new Date();
		e.source = clientSW.getSocket().getInetAddress().toString();
		e.sourceDetail = clientSW.getProtocolExchange().getWorkerJobID();
		e.destinationDetail = clientSW.getProtocolExchange().getClientCandidateCEs().trim();
		e.destination = e.destinationDetail.equals("") ? "Any" : "List";
		e.value = duration;
		e.valueQualifier = reason;
		recordEvent(e);
	}

	/** Event sent when the client gets a <code>WorkerJob</code>. */
	public static void clientMatched(SocketWrapper clientSW, SocketWrapper workerSW) {
		Event e = new Event();
		e.targetType = "clientMatch";
		e.eventType = clientSW.getProtocolExchange().getClientApplicationID();
		e.date = new Date();
		e.source = clientSW.getSocket().getInetAddress().toString();
		e.sourceDetail = workerSW.getProtocolExchange().getWorkerJobID();
		e.destination = workerSW.getProtocolExchange().getWorkerCeName();
		e.destinationDetail = workerSW.getSocket().getInetAddress().toString();
		recordEvent(e);
	}

	/**
	 * Event sent when the binder submits a new <code>WorkerJob</code>.
	 * 
	 * @param desiredJobs
	 *            whose value represents:
	 *            <ul>
	 *            <li>in panic (full-throttle) mode - total number of desired
	 *            jobs to be submitted</li>
	 *            <li>in regular mode - desired number of jobs to be submitted
	 *            on the CE that is being refilled</li>
	 *            </ul>
	 * @param actualJobs
	 *            The number of jobs that were actually submitted on the CE,
	 *            regardless of the strategy being used.
	 */
	public static void jobSubmission(WorkerJob wj, String ceName, BinderPool binderPool, int desiredJobs, int actualJobs,
			BinderStrategy reason) {
		Event e = new Event();
		e.targetType = "jobSubmit";
		e.eventType = reason.toString();
		e.date = new Date();
		e.source = String.valueOf(desiredJobs);
		e.sourceDetail = wj.getJobID();
		e.destination = ceName;
		e.destinationDetail = String.valueOf(actualJobs);
		e.value = binderPool.getActualPoolSize();
		e.valueQualifier = binderPool.getBusyJobs();
		recordEvent(e);
	}

	/**
	 * Event sent when the worker connects to the binder offering a new
	 * <code>Workerjob</code>.
	 * 
	 * @param status
	 *            with values:
	 *            <ul>
	 *            <li><b>0</b> - job accepted - <code>submitted</code></li>
	 *            <li><b>1</b> - job accepted - <code>aged</code></li>
	 *            <li><b>2</b> - job accepted - <code>reusable</code></li>
	 *            <li><b>10</b> - job discarded, it informed it has
	 *            <code>finished</code></li>
	 *            <li><b>11</b> - job rejected, not found in the pool</li>
	 *            <li><b>12</b> - job rejected, not in expected state</li>
	 *            <li><b>13</b> - job rejected, CE is full</li>
	 *            <li><b>14</b> - job rejected, CE not found</li>
	 *            </ul>
	 * 
	 */
	public static void jobArrival(BinderStrategy reason, SocketWrapper workerSW, long responseTime, int status) {
		Event e = new Event();
		e.targetType = "jobArrival";
		e.eventType = reason != null ? reason.toString() : "UNKNOWN";
		e.date = new Date();
		e.source = workerSW.getProtocolExchange().getWorkerCeName();
		e.sourceDetail = workerSW.getSocket().getInetAddress().toString();
		e.sourceDetail += " jobID: " + workerSW.getProtocolExchange().getWorkerJobID();
		e.destination = "";
		e.destinationDetail = workerSW.getProtocolExchange().getWorkerApplicationList().trim();
		e.value = responseTime;
		e.valueQualifier = status;
		recordEvent(e);
	}

	/**
	 * Event sent when the job is about to be discarded.
	 * 
	 * @param status
	 *            with values:
	 *            <ul>
	 *            <li><b>0</b> - aged -> removed</li>
	 *            <li><b>1</b> - ready -> exceeded MaxWallClockTime</li>
	 *            <li><b>2</b> - ready -> exceeded Max ready time</li>
	 *            <li><b>3</b> - reusable -> exceeded MaxWallClockTime</li>
	 *            <li><b>4</b> - reusable -> reconnect timeout</li>
	 *            </ul>
	 */
	public static void jobDiscarded(WorkerJob wj, String ceName, long timeout, int status) {
		Event e = new Event();
		e.targetType = "jobDiscard";
		e.eventType = wj.getReason().toString();
		e.date = new Date();
		e.source = ceName;
		e.sourceDetail = wj.getJobID();
		e.destination = "";
		e.destinationDetail = "";
		e.value = timeout;
		e.valueQualifier = status;
		recordEvent(e);
	}
}
