package org.jboss.tusk.smartdata.ejb;

import org.jboss.logging.Logger;
import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.ispn.InfinispanException;
import org.jboss.tusk.smartdata.ispn.InfinispanService;

public class IngesterHelper {
	
	private final static Logger LOG = Logger.getLogger(IngesterHelper.class);

	private static int totalTime = 0;
	private static int numPuts = 0;
	private static int totalNumReceived = 0;
	
	InfinispanService ispnService = new InfinispanService();
	
	public IngesterHelper() {
	}
	
	public int ingestBatch(CachedItem[] cachedItems) {
		totalNumReceived++;
		LOG.info("Received message number " + totalNumReceived + " for ingest.");
		long start = System.currentTimeMillis();

		int numActuallyIngested = -1;
		
		//if we have a string, ingest it
		if (cachedItems == null) {
			LOG.warn("Could not extract items from message, so we are ignoring it.");
		} else {
			LOG.info(System.currentTimeMillis() + ": Parsing batch into " + (cachedItems != null ? cachedItems.length + " " : "") + 
					"items took " + (System.currentTimeMillis() - start) + " ms.");

			//now ingest
			if (cachedItems == null || cachedItems.length < 1) {
				LOG.info("This batch was not able to be parsed and is being ignored.");
			} else {
				numActuallyIngested = ingest(cachedItems);
			}
			
			totalTime += (System.currentTimeMillis() - start);
			numPuts+= cachedItems.length;
			System.out.println("*** Total number of items ingested: " + numPuts + " (cache reports size " + 
					ispnService.count() + "). Average number of items per message is " + (numPuts / totalNumReceived) + 
					"; ingest ms/write=" + ((1.0 * totalTime) / numPuts));
		}

		return numActuallyIngested;		
	}

	private int ingest(CachedItem[] values) {
	    long start = System.currentTimeMillis();
	    
	    int numActuallyIngested = 0;
	    for (CachedItem cachedItem : values) {
	    	if (cachedItem != null) {
	    		numActuallyIngested++;
	    		doIngest(cachedItem.getKey(), cachedItem);
	    	} else {
//	    		LOG.warn("  Ignoring null item");
	    	}
	    }
		
	    long total = System.currentTimeMillis() - start;
	    LOG.info(System.currentTimeMillis() + ": " + numActuallyIngested + " writes took " + total + " ms.");
	    
	    return numActuallyIngested;
    }

    private void doIngest(String key, CachedItem value) {
		try {
			ispnService.writeValue(key, value);
		} catch (InfinispanException ex) {
			LOG.error("Caught InfinispanException writing value: " + ex.getMessage());
		}
    }
    
}
