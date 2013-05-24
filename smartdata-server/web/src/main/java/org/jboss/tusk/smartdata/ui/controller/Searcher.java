/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.tusk.smartdata.ui.controller;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.jboss.tusk.smartdata.ejb.RemoteSearcher;

import java.io.Serializable;

/**
 * A simple managed bean that is used to invoke the SearcherEJB.
 *
 * @author jhayes
 */
@Named("searcher")
@SessionScoped
public class Searcher implements Serializable {

	/**
	 * Injected SearcherEJB client
	 */
	@EJB
	private RemoteSearcher searcherEJB;
	
	private String criteria;
	private String response;

	public void setCriteria(String criteria) {
		this.criteria = criteria;
		System.out.println("Set criteria to " + this.criteria);
	}

	public void search(String criteria) {
		this.response = searcherEJB.search(criteria);
		System.out.println("search response=" + this.response);
	}

	public String getResponse() {
		return response;
	}

}
