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
package org.jboss.tusk.smartdata.ejb;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.jboss.logging.Logger;
import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.ispn.InfinispanException;
import org.jboss.tusk.smartdata.ispn.InfinispanService;
import org.jboss.tusk.smartdata.ispn.SearchCriterion;

/**
 * An EJB that does searches.
 *
 */
@Stateless
@Remote(RemoteSearcher.class)
public class SearcherEJB implements RemoteSearcher
{
	private static final Logger LOG = Logger.getLogger(SearcherEJB.class);
	
//	@Inject
	InfinispanService ispnService = new InfinispanService();
	
	@Override
	public int count() {
		return ispnService.count();
	}
	
	@Override
	public String loadAll() {
		try {
			List<CachedItem> allValues = ispnService.loadAllValues();
			int num = allValues.size();
			System.out.println("  There are " + num + " items in the data grid.");
			
			List<String> allValuesStr = new ArrayList<String>(allValues.size());
			for (CachedItem item : allValues) {
				allValuesStr.add(item.toString());
			}

			return ispnService.makeJSONFromResultList(allValuesStr);
		} catch (InfinispanException ex) {
			LOG.error("Caught InfinispanException loading all values: " + ex.getMessage());
			return null;
		}
		
	}
	
	@Override
	public String localSearch(String criteria) {
		//tell it to return all results
		return localSearch(criteria, 0, 0);
	}

	public String localSearch(String criteria, int from, int to) {
		System.out.println("Local search returning matches (" + 
				from + "-" + to + ") with criteria " + criteria);
		
		if (StringUtils.isEmpty(criteria)) {
			return "Nothing to search on.";
		}
		
		List<String> results = ispnService.search(criteria);

		return pageAndMakeJSON(results, from , to);
	}
	
	@Override
	public int localSearchCt(String criteria) {
		System.out.println("Local search (to get a count) with criteria " + criteria);
		
		//if no criteria, return total size
		if (StringUtils.isEmpty(criteria)) {
			return ispnService.count();
		}
		
		List<String> results = ispnService.search(criteria);

		return results.size();
	}

	public String search(String criteria) {
		//tell it to return all results
		return search(criteria, 0, 0);
	}

	@Override
	public String search(String criteria, int from, int to) {
		System.out.println("Search returning matches (" + from + "-" + to + ") with criteria " + criteria);
		
		List<String> results = new ArrayList<String>();
		try {
			results = ispnService.distributedSearch(criteria);
		} catch (InfinispanException ex) {
			LOG.error("Caught exception making distributedSearch call: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
		
		return pageAndMakeJSON(results, from , to);
	}
	
	private String pageAndMakeJSON(List<String> results, int from, int to) {
		if (from < 1 || to < 1 || from > to) {
			//return all results if from or to is < 1, 
			//or if from > to (ie it's an invalid range),
			//or if from is greater than the number of results
			LOG.info("Returning all results because range is either not given or is invalid.");
			return ispnService.makeJSONFromResultList(results, false);
		} else {
			long start = System.currentTimeMillis();
			
			String completeJSON = ispnService.makeJSONFromResultList(results, false);
			
			//this is a hack to do paging, I know, but it's the best I can do on short notice
			StringBuffer buf = new StringBuffer();
			String[] pieces = completeJSON.split("\\{\"key\"");
			int numItems = pieces.length - 1; //subtract 1 because the first item is empty
			
			//the range must not start too high; if so, just return everything
			if (from > numItems) {
				return completeJSON;
			}
			
			int effectiveFrom = Math.min(from, numItems);
			int effectiveTo = Math.min(to, numItems);
			LOG.info("Paging on " + numItems + " matches with effective from and to of " + effectiveFrom + "-" + effectiveTo + ".");
			
			//'to' and 'from' start at 1, so we need to subtract 1 from them to get the corresponding indexes...
			//but there is also an empty first element of pieces[], so we add 1 back to 'from' and 'to'
			//and 'from' is inclusive...
			for (int i = effectiveFrom; i <= effectiveTo; i++) {
				buf.append("{\"key\"").append(pieces[i]);
			}
			
			//remove trailing ", " if necessary
			if (buf.lastIndexOf(", ") == (buf.length() - 2)) {
				buf.deleteCharAt(buf.length() - 2);
			}
			
			LOG.info("Paging took " + (System.currentTimeMillis() - start) + " ms.");
			
			return buf.toString();
		}
	}

	@Override
	public int searchCt(String criteria) {
		System.out.println("Search (to get a count) with criteria " + criteria);
		
		//if no criteria, return total size
		if (StringUtils.isEmpty(criteria)) {
			return ispnService.count();
		}

//		List<String> results = ispnService.search(criteria);

		List<String> results = new ArrayList<String>();
		try {
			results = ispnService.distributedSearch(criteria);
		} catch (InfinispanException ex) {
			LOG.error("Caught exception making distributedSearch call for searchCt: " + ex.getMessage());
			ex.printStackTrace();
			return -1;
		}
		
		return results.size();
	}
	
	@Override
	public String mapReduce(String criteria) {
		List<String> results = new ArrayList<String>();
		try {
			results = ispnService.mapReduce(criteria);
		} catch (InfinispanException ex) {
			LOG.error("Caught exception making mapReduce call: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
		
		return ispnService.makeJSONFromResultList(results);
	}
}
