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

import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.jboss.logging.Logger;
import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.ispn.InfinispanException;
import org.jboss.tusk.smartdata.ispn.InfinispanService;

/**
 * An EJB that handles ingest of data items.
 *
 */
@Stateless
@Remote(RemoteIngester.class)
public class IngesterEJB implements RemoteIngester
{
	private static final Logger LOG = Logger.getLogger(IngesterEJB.class);
	
//	@Inject
	InfinispanService ispnService = new InfinispanService();

    @Override
	public String ingest(String key, CachedItem value) {
		System.out.println("Ingesting " + key + " item: " + value);

	    long start = System.currentTimeMillis();
	    
	    doIngest(key, value);
		
	    long total = System.currentTimeMillis() - start;
	    System.out.println("1 write took " + total + " ms.");
	    
		return key;
	}
    
    @Override
	public String ingest(String[] keys, CachedItem[] values) {
		System.out.println("Ingesting " + keys.length + " items.");

	    long start = System.currentTimeMillis();
	    
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				doIngest(keys[i], values[i]);
			}
		}
		
	    long total = System.currentTimeMillis() - start;
	    System.out.println(keys.length + " writes took " + total + " ms.");
		
	    //TODO what to return 
		return "done";
	}
    
    @Override
    public String ingest(List<String> keys, List<CachedItem> values) {
		System.out.println("Ingesting " + keys.size() + " items.");

	    long start = System.currentTimeMillis();
	    
		for (int i = 0; i < values.size(); i++) {
			if (values.get(i) != null) {
				doIngest(keys.get(i), values.get(i));
			}
		}
		
	    long total = System.currentTimeMillis() - start;
	    System.out.println(keys.size() + " writes took " + total + " ms.");
		
	    //TODO what to return 
		return "done";
    }
    
    @Override
	public String ingest(CachedItem[] values) {
	    long start = System.currentTimeMillis();
	    
	    int ct = 0;
	    for (CachedItem item : values) {
	    	if (item != null) {
	    		ct++;
	    		doIngest(item.getKey(), item);
	    	}
	    }
		
	    long total = System.currentTimeMillis() - start;
	    System.out.println(System.currentTimeMillis() + ": " + ct + " writes took " + total + " ms.");
		
	    //TODO what to return 
		return "done";
    }

//	public String ingest(byte[] encodedValues) {
//		System.out.println("Got it: " + encodedValues);
//		String encodedStr = new String(encodedValues);
//
//		return ingest(encodedStr);
//	}

//	public String ingest(String encodedValues) {
//		System.out.println("Got it: " + encodedValues);
////		System.out.println("EncodedValues=" + encodedValues);
//		String[] vals = encodedValues.split("\\|");
////		for (int i = 0; i < vals.length; i++) {
////			System.out.println("  " + i + "=" + vals[i]);
////		}
//		CachedItem[] items = new CachedItem[vals.length];
//		for (int i = 0; i < vals.length; i++) {
//			items[i] = new CachedItem(vals[i]);
//		}
//
//		return ingest(items);
//	}
    
    private void doIngest(String key, CachedItem value) {
		try {
			ispnService.writeValue(key, value);
		} catch (InfinispanException ex) {
			LOG.error("Caught InfinispanException writing value: " + ex.getMessage());
		}
    }
    
}
