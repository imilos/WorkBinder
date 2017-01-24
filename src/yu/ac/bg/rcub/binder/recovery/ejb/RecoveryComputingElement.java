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
package yu.ac.bg.rcub.binder.recovery.ejb;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import yu.ac.bg.rcub.binder.ComputingElement;

@Entity
@Table(name = "RecoveryComputingElement")
public class RecoveryComputingElement implements Serializable {

	private static final long serialVersionUID = 7469168177459629590L;

	private int id;

	private String name;
	private String shortName;
	private int emmsj;

	public RecoveryComputingElement() {
	}

	public RecoveryComputingElement(ComputingElement ce) {
		this.name = ce.getName();
		this.shortName = ce.getShortName();
		this.emmsj = ce.getEffectiveMaxMillisSubmittedJob();
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public int getEmmsj() {
		return emmsj;
	}

	public void setEmmsj(int emmsj) {
		this.emmsj = emmsj;
	}

}
