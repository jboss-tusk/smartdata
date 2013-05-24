package org.jboss.tusk.smartdata.ejb;

public interface RemoteSearcher {

	public static final String SEARCH = "localsearch";
	public static final String MAP_REDUCE = "mapreduce";
	public static final String DISTRIBUTED_SEARCH = "search";

	public String loadAll();
	public String localSearch(String criteria);
	public String localSearch(String criteria, int from, int to);
	public int localSearchCt(String criteria);
	public int count();
	public String mapReduce(String criteria);
	public String search(String criteria);
	public String search(String criteria, int from, int to);
	public int searchCt(String criteria);
	
}
