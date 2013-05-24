package org.jboss.tusk.smartdata.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ejb.EJB;

import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.data.CachedItemHelper;
import org.jboss.tusk.smartdata.data.CachedItemHelperFactory;
import org.jboss.tusk.smartdata.ejb.IngesterHelper;
import org.jboss.tusk.smartdata.ejb.RemoteSearcher;
import org.jboss.tusk.smartdata.ejb.SearcherEJB;
import org.jboss.tusk.smartdata.ispn.InfinispanService;
import org.junit.Before;
import org.junit.Test;

public class IngesterAndSearcherTest {
	
	private static CachedItem[] cachedItems = null;
//	private CachedItemHelper cachedItemHelper = CachedItemHelperFactory.getInstance("org.jboss.tusk.smartdata.domain.cable.STBLogHelper");
	private CachedItemHelper cachedItemHelper = CachedItemHelperFactory.getInstance();
	private String matchAllCriteria = "field1=*";
	private CachedItem extraCachedItem1 = null;
	private CachedItem extraCachedItem2 = null;
	private static final long searchResultsCacheExpirationSleep = 5000L; //must be > 3000

	@EJB
	private RemoteSearcher searcherEJB = new SearcherEJB();
	
	static {
		System.setProperty("searchResultCacheTimeout", String.valueOf(searchResultsCacheExpirationSleep - 2000));
	}
	
	@Before
	public void setup() {
		cachedItems = new CachedItem[3];
		cachedItems[0] = cachedItemHelper.sample();
		cachedItems[1] = cachedItemHelper.sample();
		cachedItems[2] = cachedItemHelper.sample();
		
		extraCachedItem1 = cachedItemHelper.sample();
		extraCachedItem2 = cachedItemHelper.sample();

		try {
			new InfinispanService().clearCache();
		} catch (Exception ex) {
			System.err.println("Caught exception clearing infinispan cache: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	@Test
	public void testSearchCache() {
		IngesterHelper ingesterHelper = new IngesterHelper();
		
		//test happy path
		int numIngested = ingesterHelper.ingestBatch(cachedItems);
		assertEquals("Did not ingest all items.", cachedItems.length, numIngested);
		String resultStr1 = searcherEJB.search(matchAllCriteria);
		
		assertTrue("Could not find the first item on first search.", resultStr1.indexOf(cachedItems[0].toString()) != -1);
		assertTrue("Could not find the second item on first search.", resultStr1.indexOf(cachedItems[1].toString()) != -1);
		assertTrue("Could not find the third item on first search.", resultStr1.indexOf(cachedItems[2].toString()) != -1);

		//now add a few more items and make run the search again and make sure that
		//the original search results are still returned (ie from the cache)
		CachedItem[] cachedItems2 = new CachedItem[3];
		cachedItems2[0] = extraCachedItem1;
		cachedItems2[2] = extraCachedItem2;
		numIngested = ingesterHelper.ingestBatch(cachedItems2);
		assertEquals("Did not ignore null item on second batch ingest", 2, numIngested);
		
		String resultStr2 = searcherEJB.search(matchAllCriteria);
		
		assertEquals("Immediate second search did not return same results from cache.", resultStr1, resultStr2);
		assertTrue("Found the first extra item but wasn't supposed to on second search.", resultStr2.indexOf(extraCachedItem1.toString()) == -1);
		assertTrue("Found the second extra item but wasn't supposed to on second search.", resultStr2.indexOf(extraCachedItem2.toString()) == -1);
		
		//now sleep long enough for the cache to time out and run the search again; results should be different
		//and should contain all 3 original and 2 extra items
		try {
			Thread.sleep(searchResultsCacheExpirationSleep);
		} catch (InterruptedException ex) {
			//do nothing
			System.err.println("We were interrupted too soon while sleeping to wait for the search result cache to expire.");
		}

		//make sure the cache now contains all items
		assertEquals("Cache doesn't have all items", 5, searcherEJB.count());
		
		//now re-do the search to get the new results
		String resultStr3 = searcherEJB.search(matchAllCriteria);

		assertTrue("Third search did not return same result from first search after search results expired.", !resultStr3.equals(resultStr1));
		assertTrue("Third search did not return same result from second search after search results expired.", !resultStr3.equals(resultStr2));

		assertTrue("Could not find the first item after search results expired.", resultStr3.indexOf(cachedItems[0].toString()) != -1);
		assertTrue("Could not find the second item after search results expired.", resultStr3.indexOf(cachedItems[1].toString()) != -1);
		assertTrue("Could not find the third item after search results expired.", resultStr3.indexOf(cachedItems[2].toString()) != -1);
		assertTrue("Could not find the first extra item after search results expired.", resultStr3.indexOf(extraCachedItem1.toString()) != -1);
		assertTrue("Could not find the second extra item after search results expired.", resultStr3.indexOf(extraCachedItem2.toString()) != -1);
	}

}
