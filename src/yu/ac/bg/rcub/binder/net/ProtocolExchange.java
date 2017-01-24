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
package yu.ac.bg.rcub.binder.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;

import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.CEInfo;
import yu.ac.bg.rcub.binder.util.Enums.AccessType;
import yu.ac.bg.rcub.binder.util.Enums.ConnectionType;
import yu.ac.bg.rcub.binder.util.Enums.JobStatus;

/**
 * Main class that implements the protocol between clients, workers and the
 * binder.
 * 
 * @author choppa
 * 
 */
public class ProtocolExchange implements Serializable {

	private static final long serialVersionUID = 341715075766568770L;

	/** common part of the header */
	private ConnectionType connectionType = null;
	private AccessType accessType = null;

	/** client part of the header */
	private class ClientHeader implements Serializable {
		private static final long serialVersionUID = 3739368088141185365L;
		private int protocolVersion;
		// private String serverSelectionHint;
		private String candidateCEs = "";
		private String applicationID;
		private long requiredWallClockTime;
		private String accessString = "";
		private String routingInfo = "";
		private String clientHost = "undefined";
		private int clientHostPort = -1;
		private byte[] proxyKeyData;
		private byte[][] proxyCertData;

		public String toString() {
			return "Client Header - version: " + protocolVersion + " candidateCEs: " + candidateCEs + " applicationID: "
					+ applicationID;
		}
	}

	private class WorkerHeader implements Serializable {
		private static final long serialVersionUID = 5052001056716650373L;
		private int protocolVersion;
		private String jobID;
		private String ceName;
		private String applicationList;
		private String accessString = "";
		private String maxWallClockTime; /* in minutes */
		private JobStatus jobStatus = JobStatus.FINISHED;

		// private String shortGridCEName;
		public String toString() {
			return "Worker Header - version: " + protocolVersion + " CE Name: " + ceName + " jobID: " + jobID;
		}
	}

	private class WorkerResponse implements Serializable {
		private static final long serialVersionUID = -5604683180638625871L;
		private String routingInfo = "";
		private String errorDescription = "";

		public String toString() {
			return "Worker Response - routing info: " + routingInfo + " error description: " + errorDescription;
		}
	}

	private ClientHeader clientHeader = new ClientHeader();
	private WorkerHeader workerHeader = new WorkerHeader();
	private WorkerResponse workerResponse = new WorkerResponse();

	public ProtocolExchange() {
	}

	public static void exchangeFields(ProtocolExchange client, ProtocolExchange worker) {
		/* copy clients header to the worker */
		worker.setClientAccessString(client.getClientAccessString());
		worker.setClientApplicationID(client.getClientApplicationID());
		worker.setClientCandidateCEs(client.getClientCandidateCEs());
		worker.setClientProtocolVersion(client.getClientProtocolVersion());
		worker.setClientRequiredWallClockTime(client.getClientRequiredWallClockTime());
		worker.setClientRoutingInfo(client.getClientRoutingInfo());
		worker.setClientProxyKeyData(client.getClientProxyKeyData());
		worker.setClientProxyCertData(client.getClientProxyCertData());

		/* copy workers header to client */
		client.setWorkerAccessString(worker.getWorkerAccessString());
		client.setWorkerApplicationList(worker.getWorkerApplicationList());
		client.setWorkerMaxWallClockTime(worker.getWorkerMaxWallClockTime());
		client.setWorkerCeName(worker.getWorkerCeName());
		client.setWorkerJobID(worker.getWorkerJobID());
		client.setWorkerJobStatus(worker.getWorkerJobStatus());
		client.setWorkerProtocolVersion(client.getWorkerProtocolVersion());

		/* copy workers response to client */
		client.setWorkerRoutingInfo(worker.getWorkerRoutingInfo());
		client.setWorkerErrorDescription(worker.getWorkerErrorDescription());
	}

	private AccessType determineAccessType() {
		String clientAccessString = clientHeader.accessString;
		String workerAccessString = workerHeader.accessString;
		if (clientAccessString.equalsIgnoreCase("") && workerAccessString.equalsIgnoreCase(""))
			return accessType = AccessType.BINDER;

		if (workerAccessString.equalsIgnoreCase("") && clientAccessString.startsWith("direct:")) {
			String address = clientAccessString.substring(7);
			int ind = address.lastIndexOf(':');
			if (ind > 0) {
				clientHeader.clientHost = address.substring(0, ind);
				try {
					clientHeader.clientHostPort = Integer.parseInt(address.substring(ind + 1));
				} catch (NumberFormatException e) {
					return accessType = AccessType.UNKNOWN;
				}
				return accessType = AccessType.DIRECT;
			}
		}

		if (workerAccessString.equalsIgnoreCase("") && clientAccessString.startsWith("custom:")) {
			return accessType = AccessType.CUSTOM;
		}

		return accessType = AccessType.UNKNOWN;
	}

	public void receiveHeader(DataInputStream in) throws EOFException, IOException {
		int protocolVersion = in.readInt();
		connectionType = ConnectionType.toConnType(in.readInt());
		switch (connectionType) {
		case CLIENT:
			clientHeader.protocolVersion = protocolVersion;
			receiveClientHeader(in);
			break;
		case WORKER:
			workerHeader.protocolVersion = protocolVersion;
			receiveWorkerHeader(in);
			break;
		case CE_QUERY:
			/* We consider it a client and expect full client header. */
			clientHeader.protocolVersion = protocolVersion;
			receiveClientHeader(in);
			break;
		}
	}

	private void receiveClientHeader(DataInputStream in) throws EOFException, IOException {
		// /* server selection hint */
		// clientHeader.serverSelectionHint = Util.readString(in);

		/* list of CEs client has chosen */
		clientHeader.candidateCEs = BinderUtil.readString(in);
		/* application name */
		clientHeader.applicationID = BinderUtil.readString(in);
		/* access string for describing connection between client and worker */
		clientHeader.accessString = BinderUtil.readString(in);
		/* clients wall clock time */
		clientHeader.requiredWallClockTime = in.readLong();

		/* -- client credentials encoded -- */
		/* client proxy key */
		clientHeader.proxyKeyData = BinderUtil.readBytes(in);
		/* client cert chain length */
		clientHeader.proxyCertData = new byte[in.readInt()][];
		/* client cert chain encoded */
		for (int i = 0; i < clientHeader.proxyCertData.length; i++)
			clientHeader.proxyCertData[i] = BinderUtil.readBytes(in);

		/* routing info */
		clientHeader.routingInfo = BinderUtil.readString(in);
	}

	private void receiveWorkerHeader(DataInputStream in) throws EOFException, IOException {
		/* worker jobID */
		workerHeader.jobID = BinderUtil.readString(in);
		/* worker sends his name */
		workerHeader.ceName = BinderUtil.readString(in);
		/* supported applications */
		workerHeader.applicationList = BinderUtil.readString(in);
		/* access string for describing connection between client and worker */
		workerHeader.accessString = BinderUtil.readString(in);
		/* remaining wall clock time for the worker job */
		workerHeader.maxWallClockTime = BinderUtil.readString(in);
		/* job status reported, if FINISHED job will be removed */
		workerHeader.jobStatus = JobStatus.toJobStatus(in.readInt());
	}

	public void receiveWorkerResponse(DataInputStream in) throws EOFException, IOException {
		/* worker routing info */
		workerResponse.routingInfo = BinderUtil.readString(in);
		/* worker error description */
		workerResponse.errorDescription = BinderUtil.readString(in);
	}

	public void sendClientHeader(DataOutputStream out) throws IOException {
		/* protocol version */
		out.writeInt(clientHeader.protocolVersion);
		/* connection type (0 - client; 1 - worker, 3 - ClientQuery) TEST! */
		out.writeInt(connectionType.toInt());
		// /* server selection description */
		// Util.writeString(out, protocolExchange.getServerSelectionHint());

		/* candidate CEs */
		BinderUtil.writeString(out, clientHeader.candidateCEs);
		/* application ID */
		BinderUtil.writeString(out, clientHeader.applicationID);
		/* accessString describing connection between client and worker */
		BinderUtil.writeString(out, clientHeader.accessString);
		/* required wall clock time */
		out.writeLong(clientHeader.requiredWallClockTime);

		/* -- client credentials encoded -- */
		/* client proxy key */
		BinderUtil.writeBytes(out, clientHeader.proxyKeyData);
		/* client cert chain length */
		out.writeInt(clientHeader.proxyCertData.length);
		/* client cert chain encoded */
		for (byte[] cert : clientHeader.proxyCertData)
			BinderUtil.writeBytes(out, cert);

		/* routing info */
		BinderUtil.writeString(out, clientHeader.routingInfo);
	}

	public void sendWorkerHeader(DataOutputStream out) throws IOException {
		/* protocol version */
		out.writeInt(workerHeader.protocolVersion);
		/* connection type (0 - client; 1 - worker) */
		out.writeInt(ConnectionType.WORKER.toInt());
		/* worker jobId */
		BinderUtil.writeString(out, workerHeader.jobID);
		/* gridCE */
		BinderUtil.writeString(out, workerHeader.ceName);
		/* application list supported by worker */
		BinderUtil.writeString(out, workerHeader.applicationList);
		/* accessString describing connection type between client and worker */
		BinderUtil.writeString(out, workerHeader.accessString);
		/* remaining wall clock time for the worker job */
		BinderUtil.writeString(out, workerHeader.maxWallClockTime);
		/* whether worker job should be removed or not. */
		out.writeInt(workerHeader.jobStatus.toInt());
	}

	public void sendWorkerResponse(DataOutputStream out) throws IOException {
		/* worker routing info */
		BinderUtil.writeString(out, workerResponse.routingInfo);
		/* worker error description */
		BinderUtil.writeString(out, workerResponse.errorDescription);
	}

	public void sendChallenge(DataOutputStream out, byte[] challenge) throws IOException {
		/* size of the challenge */
		out.writeInt(challenge.length);
		/* the challenge itself */
		out.write(challenge);
	}

	public byte[] receiveChallenge(DataInputStream in) throws IOException {
		/* size of the challenge */
		byte[] challenge = new byte[in.readInt()];
		/* the challenge itself */
		in.readFully(challenge);
		return challenge;
	}

	public CEInfo[] receiveCEQueryResult(DataInputStream in) throws EOFException, IOException {
		/* the size of result array */
		int size = in.readInt();
		CEInfo[] ceInfo = new CEInfo[size];
		/* array of CEInfo serialized */
		for (int i = 0; i < size; i++) {
			ceInfo[i] = new CEInfo();
			ceInfo[i].customReadObject(in);
		}
		return ceInfo;
	}

	public void sendCEQueryResult(DataOutputStream out, CEInfo[] ceInfo) throws IOException {
		/* the size of result array */
		int size = ceInfo != null ? ceInfo.length : 0;
		out.writeInt(size);
		/* array of CEInfo serialized */
		for (int i = 0; i < size; i++) {
			ceInfo[i].customWriteObject(out);
		}
	}

	public ConnectionType getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(ConnectionType connectionType) {
		this.connectionType = connectionType;
	}

	public AccessType getAccessType() {
		determineAccessType();
		return accessType;
	}

	public String toString() {
		// TODO needs refactoring
		String s1 = (clientHeader.applicationID != null) ? clientHeader.toString() : "";
		String s2 = (workerHeader.ceName != null) ? workerHeader.toString() : "";
		String s3 = (workerResponse != null) ? workerResponse.toString() : "";
		return s1 + s2 + s3;
	}

	/* client related getters & setters */
	public int getClientProtocolVersion() {
		return clientHeader.protocolVersion;
	}

	public void setClientProtocolVersion(int protocolVersion) {
		clientHeader.protocolVersion = protocolVersion;
	}

	public String getClientCandidateCEs() {
		return clientHeader.candidateCEs;
	}

	public void setClientCandidateCEs(String candidateCEs) {
		clientHeader.candidateCEs = candidateCEs;
	}

	public String getClientApplicationID() {
		return clientHeader.applicationID;
	}

	public void setClientApplicationID(String applicationID) {
		clientHeader.applicationID = applicationID;
	}

	public long getClientRequiredWallClockTime() {
		return clientHeader.requiredWallClockTime;
	}

	public void setClientRequiredWallClockTime(long requiredWallClockTime) {
		clientHeader.requiredWallClockTime = requiredWallClockTime;
	}

	public String getClientAccessString() {
		return clientHeader.accessString;
	}

	public void setClientAccessString(String accessString) {
		clientHeader.accessString = accessString;
	}

	public String getClientRoutingInfo() {
		return clientHeader.routingInfo;
	}

	public void setClientRoutingInfo(String routingInfo) {
		clientHeader.routingInfo = routingInfo;
	}

	public String getClientHost() {
		return clientHeader.clientHost;
	}

	public int getClientHostPort() {
		return clientHeader.clientHostPort;
	}

	public byte[] getClientProxyKeyData() {
		return clientHeader.proxyKeyData;
	}

	public void setClientProxyKeyData(byte[] data) {
		clientHeader.proxyKeyData = data;
	}

	public byte[][] getClientProxyCertData() {
		return clientHeader.proxyCertData;
	}

	public void setClientProxyCertData(byte[][] data) {
		clientHeader.proxyCertData = data;
	}

	/* end of client getters & setters */

	/* worker related getters & setters */
	public int getWorkerProtocolVersion() {
		return workerHeader.protocolVersion;
	}

	public void setWorkerProtocolVersion(int protocolVersion) {
		workerHeader.protocolVersion = protocolVersion;
	}

	public String getWorkerJobID() {
		return workerHeader.jobID;
	}

	public void setWorkerJobID(String jobID) {
		workerHeader.jobID = jobID;
	}

	public String getWorkerCeName() {
		return workerHeader.ceName;
	}

	public void setWorkerCeName(String ceName) {
		workerHeader.ceName = ceName;
	}

	public String getWorkerApplicationList() {
		return workerHeader.applicationList;
	}

	public void setWorkerApplicationList(String applicationList) {
		workerHeader.applicationList = applicationList;
	}

	public String getWorkerAccessString() {
		return workerHeader.accessString;
	}

	public void setWorkerAccessString(String accessString) {
		workerHeader.accessString = accessString;
	}

	public String getWorkerMaxWallClockTime() {
		return workerHeader.maxWallClockTime;
	}

	public void setWorkerMaxWallClockTime(String maxWallClockTime) {
		workerHeader.maxWallClockTime = maxWallClockTime;
	}

	public JobStatus getWorkerJobStatus() {
		return workerHeader.jobStatus;
	}

	public void setWorkerJobStatus(JobStatus jobStatus) {
		workerHeader.jobStatus = jobStatus;
	}

	public String getWorkerRoutingInfo() {
		return workerResponse.routingInfo;
	}

	public void setWorkerRoutingInfo(String routingInfo) {
		workerResponse.routingInfo = routingInfo;
	}

	public String getWorkerErrorDescription() {
		return workerResponse.errorDescription;
	}

	public void setWorkerErrorDescription(String errorDescription) {
		workerResponse.errorDescription = errorDescription;
	}

	/* end of worker related getters & setters */
}
