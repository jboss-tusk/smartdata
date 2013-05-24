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
package org.jboss.tusk.smartdata.rest;

import javax.ejb.EJB;

import org.jboss.tusk.smartdata.ejb.RemoteSearcher;

/**
 * A CDI service which is used to do searches
 * 
 */
public class SearchService {

	/**
	 * Injected SearcherEJB client
	 */
	@EJB
	private RemoteSearcher searcherEJB;

	String localSearch(String criteria) {
		return searcherEJB.localSearch(criteria);
	}

	String localSearch(String criteria, int from, int to) {
		return searcherEJB.localSearch(criteria, from, to);
	}

	int localSearchCt(String criteria) {
		return searcherEJB.localSearchCt(criteria);
	}

	String mapReduce(String criteria) {
		return searcherEJB.mapReduce(criteria);
	}
	
	String search(String criteria) {
		return searcherEJB.search(criteria);
	}
	
	String search(String criteria, int from, int to) {
		return searcherEJB.search(criteria, from, to);
	}
	
	int searchCt(String criteria) {
		return searcherEJB.searchCt(criteria);
	}
	
	String loadAll() {
		return searcherEJB.loadAll();
	}
	
	int count() {
		return searcherEJB.count();
	}

}
