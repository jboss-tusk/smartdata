package org.jboss.tusk.smartdata.ispn;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.DecoratedCache;
import org.infinispan.context.Flag;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.manager.CacheManager;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.jboss.logging.Logger;
import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.data.CachedItemHelper;
import org.jboss.tusk.smartdata.data.CachedItemHelperFactory;
import org.jboss.tusk.smartdata.distexec.DistributedSearch;
import org.jboss.tusk.smartdata.ispn.InfinispanException;
import org.jboss.tusk.smartdata.mapreduce.CachedItemSearchMapper;
import org.jboss.tusk.smartdata.mapreduce.CachedItemSearchReducer;


public class InfinispanService implements Serializable {

	private static final Logger LOG = Logger.getLogger(InfinispanService.class);
	
	//this is for data payloads (if Infinispan is the current data store)
//	private static Cache<Object, Object> dataGrid = null;
	private static AdvancedCache<String, CachedItem> dataGrid = null;
	private static DecoratedCache<String, CachedItem> dataGridForWriting = null;
	
	private static Cache<String, List<String>> searchResultCache = null;
	private static final long searchResultsCacheExpirationMillis = 
			Long.valueOf(System.getProperty("searchResultCacheTimeout", "60000"));

	private static CachedItemHelper cachedItemHelper = CachedItemHelperFactory.getInstance();
	
	private static int totalTime = 0;
	private static int numPuts = 0;
	
	public InfinispanService() {
		//see if we need to create the grid objects
		if (dataGrid == null || searchResultCache == null) {
			LOG.info("Creating grid objects for first time.");
			
			//create the default cache manager
			String dataGridConfigFile = "ispn_index.xml";
			LOG.info("Using " + dataGridConfigFile + " for the data grid caches.");
			DefaultCacheManager cacheManager = null;
			try {
				cacheManager = new DefaultCacheManager(dataGridConfigFile);
			} catch (IOException ex) {
				LOG.error("Caught IOException creating search results cache: " + ex.getMessage());
			}

			//create cache for storing objects to index
			if (dataGrid == null) {
				LOG.info("Creating index cache.");
				Cache cache = cacheManager.getCache();
				dataGrid = cache.getAdvancedCache();
				
	//				dataGrid.addListener(new LoggingListener());
				
				//make a decorated cache with the per-invocation flags
				//use PUT_FOR_EXTERNAL_READ and FAIL_SILENTLY too?
				dataGridForWriting = new DecoratedCache<String, CachedItem>(dataGrid, 
						Flag.CACHE_MODE_LOCAL, //including this improves performance, but no data is replicated
						Flag.SKIP_CACHE_LOAD,
	//						Flag.ZERO_LOCK_ACQUISITION_TIMEOUT,
	//						Flag.FORCE_ASYNCHRONOUS,
						Flag.SKIP_REMOTE_LOOKUP);
			}

			//create cache for storing search results, if necessary	
			if (searchResultCache == null) {
				LOG.info("Creating search result cache with expiration " + searchResultsCacheExpirationMillis + " ms.");
				searchResultCache = cacheManager.getCache("searchResultCache");
			}
		} else {
			LOG.debug("Already created data grid.");
		}
		
	}
	
	public String writeValue(String key, CachedItem value) throws InfinispanException {
    	//generate a new key if we don't have one
    	key = StringUtils.isEmpty(key) ? CachedItem.makeKey() : key;
    	
		try {
//			LOG.info(System.currentTimeMillis() + ": Writing " + key + "->" + value + " to " + dataGrid);
			
			long start = System.currentTimeMillis();
			
//			dataGrid.putAsync(key, value);
			dataGridForWriting.put(key, value);
//			dataGridForWriting.putForExternalRead(key, value);
			
			totalTime += (System.currentTimeMillis() - start);
			numPuts++;
			if (numPuts % 100000 == 0) {
				System.out.println("+++ Total number of ISPN writes: " + numPuts + " (cache reports size " + 
						dataGridForWriting.size() + "). ISPN-only ms/write=" + ((1.0 * totalTime) / numPuts));
			}
		} catch (Exception ex) {
			LOG.error("Got " + ex.getClass().getName() + " writing value: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return key;
	}
	
	public CachedItem loadValue(String key) throws InfinispanException {
		try {
			LOG.info("Loading " + key + " from " + dataGrid);
			
			CachedItem value = null;
			value = (CachedItem)dataGrid.get(key);
			return value;
		} catch (Exception ex) {
			LOG.error("Got " + ex.getClass().getName() + " loading value: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public int count() {
		System.out.println("My size is " + dataGridForWriting.size());
		return dataGridForWriting.size();
	}
	
	public List<CachedItem> loadAllValues() throws InfinispanException {
		List<CachedItem> allValues = new ArrayList<CachedItem>(dataGrid.size());
		for (Entry<String, CachedItem> entry : dataGrid.entrySet()) {
			allValues.add(entry.getValue());
		}
		return allValues;
	}
	
	public List<String> search(String criteria) {
		List<SearchCriterion> criteriaList = parseCriteria(criteria);
		return search(criteriaList, isAndQuery(criteria));
	}

	/**
	 * Searches the index, given a set of criteria fields. Any resulting objects will
	 * match ALL criteria.
	 * @param criteria a list of index entries that match ALL criteria fields
	 * @return
	 */
	private List<String> search(List<SearchCriterion> criteria, boolean isAndQuery) {
		LOG.info("Search criteria for '" + (isAndQuery ? "and" : "or") + "' query:");
		for (SearchCriterion criterion : criteria) {
			LOG.info("  " + criterion);
		}

		SearchManager searchManager = Search.getSearchManager(dataGrid);
		
		return doQuery(searchManager, criteria, cachedItemHelper.getSearchClass(), isAndQuery);
	}

	/**
	 * Executes a query where results only have to match at least one
	 * of the search criteria.
	 * @param criteria
	 * @param searchManager
	 * @return
	 */
	private List<String> doQuery(SearchManager searchManager, 
			List<SearchCriterion> criteria, Class clazz, boolean isAndQuery) {
		List<String> results = new ArrayList<String>();
		
		QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(clazz).get();
		
		//make sub-queries for each criteria field
		BooleanJunction<BooleanJunction> blnJunction = queryBuilder.bool();
		for (SearchCriterion entry : criteria) {
			Query query = queryBuilder.keyword().wildcard()
	    		.onField(entry.getField())
	    		.matching(entry.getValue().toLowerCase() + "*")
	    		.createQuery();
			
			if (isAndQuery) {
				blnJunction.must(query);
			} else {
				blnJunction.should(query);
			}
		}

		Query allQuery = blnJunction.createQuery();
		
		//now execute the query and gather the results
		CacheQuery query = searchManager.getQuery(allQuery, clazz);
		List<?> queryResults = query.list();

		LOG.info("Query \"" + allQuery + "\" returned " + queryResults.size() + " results.");
		
		if (queryResults != null) {
			for (Object result : queryResults) {
				if (result != null) {
//					LOG.info("  " + result);
					results.add(result.toString());
				} else {
					LOG.warn("Got a result from the search, but it was null.");
				}
			}
		}
		
		return results;
	}

	public List<String> mapReduce(String criteria) throws InfinispanException {
		return this.mapReduce(criteria, true);
	}
	
	public List<String> mapReduce(String criteria, boolean isAndQuery) throws InfinispanException {
		MapReduceTask<String, CachedItem, String, CachedItem> mapReduceTask = 
				new MapReduceTask<String, CachedItem, String, CachedItem>(dataGrid);
		Map<String, CachedItem> resultMap = 
				mapReduceTask
					.mappedWith(new CachedItemSearchMapper(criteria, isAndQuery))
					.reducedWith(new CachedItemSearchReducer())
					.execute();
		
		List<String> results = new ArrayList<String>();
		for (Entry<String, CachedItem> item : resultMap.entrySet()) {
			results.add(item.getValue().toString());
		}
		
		return results;
	}
	
	public List<String> distributedSearch(String criteria) throws InfinispanException {
		//first check cache
		if (searchResultCache.containsKey(criteria)) {
			List<String> results = searchResultCache.get(criteria);
			LOG.info("Retrieving search results from cache for criteria: " + criteria);
			return results;
		}
		
		DistributedExecutorService des = new DefaultExecutorService(dataGrid);
        List<Future<String>> callableResults = des.submitEverywhere(new DistributedSearch(criteria));
        List<String> results = new ArrayList<String>();
        for (Future<String> f : callableResults) {
        	try {
        		if (!StringUtils.isEmpty(f.get(5, TimeUnit.MINUTES))) {
        			results.add(f.get());
        		}
        	} catch (ExecutionException ex) {
        		LOG.error("Caught ExecutionException getting Future value: " + ex.getMessage());
        		ex.printStackTrace();
        	} catch (InterruptedException ex) {
        		LOG.error("Caught InterruptedException getting Future value: " + ex.getMessage());
        		ex.printStackTrace();
        	} catch (TimeoutException ex) {
        		LOG.error("Caught TimeoutException getting Future value: " + ex.getMessage());
        		ex.printStackTrace();
        	}
        }
        
        //now cache the results if there was at least 1 match
        if (results != null && results.size() > 0) {
        	searchResultCache.put(criteria, results, searchResultsCacheExpirationMillis, TimeUnit.MILLISECONDS);
        }
		
		return results;
	}

	public String makeJSONFromResultList(List<String> results) {
		return makeJSONFromResultList(results, false);
	}

	public String makeJSONFromResultList(List<String> results, boolean includePrefix) {
//		int ct = 0;
		StringBuffer buf = new StringBuffer();
//		LOG.info("Making string from result list of size " + results.size());
		
		if (results == null || results.isEmpty()) {
			return "";
		}
		
		for (String result : results) {
			if (!StringUtils.isEmpty(result)) {
//				LOG.info("  '" + result + "'");
				if (buf.length() > 0) {
					buf.append(", ");
				}
				if (includePrefix) {
					buf.append("\"item\":[" + result + "]");
				} else {
					buf.append(result);
				}
	//			ct++;
			}
		}
		return buf.length() == 0 ? null : buf.toString();
//		return "Found " + ct + " matches.";
	}
	
	public static boolean isAndQuery(String criteria) {
		return criteria.indexOf(",") != -1;
	}
	
	public static List<SearchCriterion> parseCriteria(String criteria) {
		String[] conditions = !isAndQuery(criteria) ?
				criteria.split("\\|") :
					criteria.split(",");
		
		List<SearchCriterion> criteriaList = new ArrayList<SearchCriterion>(conditions.length);
		for (String condition : conditions) {
			condition = condition.trim();
			String field = condition.substring(0, condition.indexOf("="));
			String value = condition.substring(condition.indexOf("=") + 1);
			criteriaList.add(new SearchCriterion(field, value));
		}
		
		return criteriaList;
	}
	
	public void clearCache() throws InfinispanException {
		try {
			synchronized(dataGrid) {
				dataGrid.clear();
			}
		} catch (Exception ex) {
			throw new InfinispanException("Caught exception clearing cache: " + ex.getMessage(), ex);
		}
	}
	
	public void stopCache() {
		dataGrid.stop();
	}
	
	public void startCache() {
		dataGrid.start();
	}

}
