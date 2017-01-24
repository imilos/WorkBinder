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
package yu.ac.bg.rcub.binder.util;


/**
 * Annoying enums moved here.
 * 
 * @author choppa
 * 
 */
public class Enums {

	/* Various Enum definitions */

	/**
	 * An Enum used to describe the type of Binder recovery.
	 * 
	 * @author choppa
	 * 
	 */
	public enum RecoveryType {
		NONE, EJB;

		/**
		 * A method that converts a string value into a
		 * <code>RecoveryType</code> enum.
		 * 
		 * @param recType
		 * @return Type of the recovery data, default is <code>NONE</code>.
		 */
		public static RecoveryType toRecType(String recType) {
			try {
				return valueOf(recType);
			} catch (Exception e) {
				return NONE;
			}
		}

	}

	/**
	 * An <code>Enum</code> used to describe the type of connection to the
	 * Binder.
	 * 
	 * @author choppa
	 * 
	 */
	public enum ConnectionType {
		/** Client connected to obtain <code>WorkerJob</code>. */
		CLIENT(0),
		/** Worker connected. */
		WORKER(1),
		/** Client connected to query the status of matching CEs. */
		CE_QUERY(2);

		private final int value;

		ConnectionType(int value) {
			this.value = value;
		}

		/**
		 * A method that converts an int value into a
		 * <code>ConnectionType</code> enum.
		 * 
		 * @param connType
		 * @return Type of the connection, default is <code>CLIENT</code>
		 */
		public static ConnectionType toConnType(int connType) {
			switch (connType) {
			case 1:
				return WORKER;
			case 2:
				return CE_QUERY;
			default:
				return CLIENT;
			}
		}

		/**
		 * A method that converts <code>ConnectionType</code> enum value into an
		 * <code>int</code>.
		 * 
		 * @return An <code>int</code> value representing a connection type.
		 */
		public int toInt() {
			return value;
		}
	}

	/**
	 * An <code>Enum</code> used to describe the status of a Binder worker job.
	 * <p>
	 * NOTE: <code>FINISHED</code> state is used by the worker job to notify
	 * Binder that the job will not be active anymore and that Binder should
	 * remove the job from its records. It is not otherwise used as a job state
	 * by the Binder.
	 * 
	 * @author choppa
	 * 
	 */
	public enum JobStatus {
		SUBMITTED(0), READY(1), BUSY(2), REUSABLE(3), FINISHED(4);

		private final int value;

		JobStatus(int value) {
			this.value = value;
		}

		/**
		 * A method that converts <code>JobStatus</code> enum value into an
		 * <code>int</code>.
		 * 
		 * @return An <code>int</code> value representing a job status.
		 */
		public int toInt() {
			return value;
		}

		/**
		 * A method that converts an int value into an <code>JobStatus</code>
		 * enum.
		 * 
		 * @param jobStatus
		 * @return Type of the binder access type.
		 */
		public static JobStatus toJobStatus(int jobStatus) {
			switch (jobStatus) {
			case 0:
				return SUBMITTED;
			case 1:
				return READY;
			case 2:
				return BUSY;
			case 3:
				return REUSABLE;
			case 4:
				return FINISHED;
			default:
				throw new EnumConstantNotPresentException(null, Integer.toString(jobStatus));
			}
		}

	}

	/**
	 * An <code>Enum</code> used to describe the type of the client - worker
	 * communication. Possible values are communication through binder and
	 * direct communication which are implemented by the Binder.
	 * <p>
	 * Besides those two choices, custom communication is also supported, which
	 * allows client and worker to exchange communications strings and continue
	 * communication on their own.
	 * 
	 * @author choppa
	 * 
	 */
	public enum AccessType {
		BINDER(0), DIRECT(1), CUSTOM(2), UNKNOWN(3);

		private final int value;

		AccessType(int value) {
			this.value = value;
		}

		/**
		 * A method that converts <code>AccessType</code> enum value into an
		 * <code>int</code>.
		 * 
		 * @return An <code>int</code> value representing an access type.
		 */
		public int toInt() {
			return value;
		}

		/**
		 * A method that converts an int value into an <code>AccessType</code>
		 * enum.
		 * 
		 * @param accessType
		 * @return Type of the binder access type, default is
		 *         <code>UNKNOWN</code>.
		 */
		public static AccessType toAccessType(int accessType) {
			switch (accessType) {
			case 0:
				return BINDER;
			case 1:
				return DIRECT;
			case 2:
				return CUSTOM;
			default:
				// throw new EnumConstantNotPresentException(null,
				// Integer.toString(accessType));
				return UNKNOWN;
			}
		}

		/**
		 * A method that converts a string value into an <code>AccessType</code>
		 * enum.
		 * 
		 * @param accessType
		 * @return Type of the binder access type, default is
		 *         <code>BINDER</code>.
		 */
		public static AccessType toAccessType(String accessType) {
			try {
				return valueOf(accessType);
			} catch (Exception e) {
				return UNKNOWN;
			}
		}

	}

	/**
	 * An <code>Enum</code> used to describe the operational mode of the binder.
	 * 
	 * @author choppa
	 * 
	 */
	public enum BinderMode {
		/** No clients are connected to the binder. */
		IDLE,
		/** There are some clients connected to the binder. */
		ACTIVE;
	}

	/**
	 * An <code>Enum</code> used to describe the operational strategy of the
	 * binder.
	 * 
	 * @author choppa
	 * 
	 */
	public enum BinderStrategy {
		/** Strategy used when binder is working in stable mode. */
		REGULAR,
		/**
		 * Strategy used when binder decides that the regular strategy is not
		 * refilling the pool at the desired rate (pool might soon get empty).
		 */
		FULL_THROTTLE;
	}

	/**
	 * An <code>Enum</code> used to describe the filtering of supported
	 * applications on a CE. It is read from the binder configuration.
	 * 
	 * @author choppa
	 * 
	 */
	public enum AppFilter {
		/** Any app listing from the worker is accepted. */
		ANY,
		/** Only applications defined on the binder are accepted. */
		FILTERED,
		/** No application is accepted, meaning that CE is disabled. */
		NONE;

		/**
		 * Converts a <code>String</code> into an <code>AppFilter</code>.
		 * 
		 * @param appFilter
		 *            <code>String</code> received from the binder.
		 * @return The type of the filter that will be used.
		 */
		public static AppFilter toAppFilter(String appFilter) {
			if (appFilter.equalsIgnoreCase(ANY.toString()))
				return ANY;
			if (appFilter.equalsIgnoreCase(NONE.toString()))
				return NONE;
			return FILTERED;
		}
	}

	/**
	 * An <code>Enum</code> used to describe the type of binder authorization
	 * used.
	 * 
	 * @author choppa
	 * 
	 */
	public enum AuthzType {
		/** No authentication used. */
		NONE(0),
		/** */
		VOMS(1);

		public final int value;

		AuthzType(int type) {
			this.value = type;
		}

		/**
		 * A method that converts <code>AuthzType</code> enum value into an
		 * <code>int</code>.
		 * 
		 * @return An <code>int</code> value representing the auth type.
		 */
		public int toInt() {
			return value;
		}

		/**
		 * A method that converts an int value into an <code>AuthzType</code>
		 * enum.
		 * 
		 * @param authType
		 * @return Type of the authentication, default is <code>NONE</code>.
		 */
		public static AuthzType toAuthzType(int authType) {
			switch (authType) {
			case 1:
				return VOMS;
			default:
				return NONE;
			}
		}

		/**
		 * A method that converts a string value into an <code>AuthzType</code>
		 * enum.
		 * 
		 * @param authType
		 * @return Type of the authentication used, default is <code>NONE</code>
		 *         .
		 */
		public static AuthzType toAuthzType(String authType) {
			try {
				return valueOf(authType);
			} catch (Exception e) {
				return NONE;
			}
		}
	}

	public enum SubmissionType {
		INTERNAL, SCRIPT;

		public static SubmissionType toSubType(String subType) {
			if (subType.equalsIgnoreCase(INTERNAL.toString()))
				return INTERNAL;
			return SCRIPT;
		}
	}

}