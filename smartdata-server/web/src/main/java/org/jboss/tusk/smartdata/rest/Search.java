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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * A simple REST service which is able to search the data that has been ingested.
 * 
 * @author jhayes@redhat.com
 * 
 */

@Path("/")
public class Search {
	@Inject
	SearchService searchService;

	@GET
	@Path("/count")
	@Produces({ "application/json" })
	public String getCountJSON() {
		return "{\"result\":\"" + searchService.count() + "\"}";
	}
	
	@GET
	@Path("/search")
	@Produces({ "application/json" })
	public String getSearchJSON() {
		return getSearchErrorResults();
	}

	@GET
	@Path("/search/{criteria}")
	@Produces({ "application/json" })
	public String getSearchJSON(@PathParam("criteria") String criteria) {
		String resultStr = "";
		if ("all".equals(criteria)) {
			//TODO need to make sure this will return JSON results properly
			resultStr = searchService.loadAll();
		} else if (criteria.indexOf("=") == -1) {
			return getSearchErrorResults();
		} else {
			resultStr = searchService.search(criteria);
		}
		return makeJSONResult(resultStr);
	}

	@GET
	@Path("/search/{from}/{to}/{criteria}")
	@Produces({ "application/json" })
	public String getSearchJSON(@PathParam("from") int from, @PathParam("to") int to, @PathParam("criteria") String criteria) {
		String resultStr = "";
		if ("all".equals(criteria)) {
			//TODO need to make sure this will return JSON results properly
			resultStr = searchService.loadAll();
		} else if (criteria.indexOf("=") == -1) {
			return getSearchErrorResults();
		} else {
			resultStr = searchService.search(criteria, from, to);
		}
		return makeJSONResult(resultStr);
	}

	@GET
	@Path("/search/ct/{criteria}")
	@Produces({ "application/json" })
	public String getSearchJSONCt(@PathParam("criteria") String criteria) {
		int num = -1;
		if ("all".equals(criteria)) {
			//TODO need to make sure this will return JSON results properly
			num = searchService.searchCt(null);
		} else if (criteria.indexOf("=") == -1) {
			return getSearchErrorResults();
		} else {
			num = searchService.searchCt(criteria);
		}
		return makeJSONResultForCt(num);
	}

	@GET
	@Path("/localsearch")
	@Produces({ "application/json" })
	public String getLocalSearchJSON() {
		return getSearchErrorResults();
	}

	@GET
	@Path("/localsearch/{criteria}")
	@Produces({ "application/json" })
	public String getLocalSearchJSON(@PathParam("criteria") String criteria) {
		String resultStr = "";
		if ("all".equals(criteria)) {
			//TODO need to make sure this will return JSON results properly
			resultStr = searchService.loadAll();
		} else if (criteria.indexOf("=") == -1) {
			return getSearchErrorResults();
		} else {
			resultStr = searchService.search(criteria);
		}
		return makeJSONResult(resultStr);
	}

	@GET
	@Path("/localsearch/{from}/{to}/{criteria}")
	@Produces({ "application/json" })
	public String getLocalSearchJSON(@PathParam("from") int from, 
			@PathParam("to") int to, @PathParam("criteria") String criteria) {
		String resultStr = "";
		if ("all".equals(criteria)) {
			//TODO need to make sure this will return JSON results properly
			resultStr = searchService.loadAll();
		} else if (criteria.indexOf("=") == -1) {
			return getSearchErrorResults();
		} else {
			resultStr = searchService.localSearch(criteria, from, to);
		}
		return makeJSONResult(resultStr);
	}

	@GET
	@Path("/localsearch/ct/{criteria}")
	@Produces({ "application/json" })
	public String getLocalSearchJSONCt(@PathParam("criteria") String criteria) {
		int num = -1;
		if ("all".equals(criteria)) {
			//TODO need to make sure this will return JSON results properly
			num = searchService.localSearchCt(null);
		} else if (criteria.indexOf("=") == -1) {
			return getSearchErrorResults();
		} else {
			num = searchService.localSearchCt(criteria);
		}
		return makeJSONResultForCt(num);
	}

	@GET
	@Path("/mapreduce/{criteria}")
	@Produces({ "application/json" })
	public String getMapReduceJSON(@PathParam("criteria") String criteria) {
		String resultStr = "";
		if ("all".equals(criteria)) {
			//TODO need to make sure this will return JSON results properly
			resultStr = searchService.loadAll();
		} else if (criteria.indexOf("=") == -1) {
			return getSearchErrorResults();
		} else {
			resultStr = searchService.mapReduce(criteria);
		}
		return makeJSONResult(resultStr);
	}

	@GET
	@Path("/mapreduce")
	@Produces({ "application/json" })
	public String getMapReduceJSON() {
		return getSearchErrorResults();
	}
	
	private String makeJSONResult(String str) {
		return "{\"result\":[" + str + "]}";
	}

	private String makeJSONResultForCt(int num) {
		return "{\"ct\":" + num + "}";
	}

	private String getSearchErrorResults() {
		return "{\"result\":\"No criteria provided. Should be of form 'field1=val1,field2=val2,...' " +
				"for AND searches or 'field1=val1|field2=val2|...' for OR searches.\"}";
	}

}
