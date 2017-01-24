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

/**
 * An exception class used to inform about various exceptions that can occur
 * during the communication with the binder.
 * <p>
 * It can contain a <code>String</code> message describing the exception and a
 * <code>Throwable</code> cause whose stack trace can be obtained and
 * analyzed.
 * 
 * @author choppa
 * 
 */
public class BinderCommunicationException extends Exception {

	private static final long serialVersionUID = -5121884935586675470L;

	/**
	 * Constructs an empty <code>BinderCommunicationException</code>.
	 */
	public BinderCommunicationException() {
		super();
	}

	/**
	 * Constructs a <code>BinderCommunicationException</code> with a message
	 * detail.
	 * 
	 * @param message
	 *            A text message explaining exception details.
	 */
	public BinderCommunicationException(String message) {
		super(message);
	}

	/**
	 * Constructs a <code>BinderCommunicationException</code> with a cause
	 * included.
	 * 
	 * @param cause
	 *            A throwable cause used to chain exceptions.
	 */
	public BinderCommunicationException(Throwable cause) {
		super(cause);
		// super(cause.getMessage()); old Java 1.5 IOException variant
	}

	/**
	 * Constructs a <code>BinderCommunicationException</code> with a message
	 * detail and a cause included.
	 * 
	 * @param message
	 *            A text message explaining exception details.
	 * @param cause
	 *            A throwable cause used to chain exceptions.
	 */
	public BinderCommunicationException(String message, Throwable cause) {
		super(message, cause);
		// super(message); old Java 1.5 IOException variant
	}
}
