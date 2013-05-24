package org.jboss.tusk.smartdata.ejb;

import java.util.List;

import org.jboss.tusk.smartdata.data.CachedItem;

public interface RemoteIngester {
	
	public String ingest(String key, CachedItem value);

	public String ingest(String[] keys, CachedItem[] values);

	public String ingest(List<String> keys, List<CachedItem> values);

	public String ingest(CachedItem[] values);
	
}
