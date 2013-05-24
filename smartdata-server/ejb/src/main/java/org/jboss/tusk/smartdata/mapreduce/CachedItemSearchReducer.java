package org.jboss.tusk.smartdata.mapreduce;

import java.util.Iterator;

import org.infinispan.distexec.mapreduce.Reducer;

import org.jboss.tusk.smartdata.data.CachedItem;

public class CachedItemSearchReducer implements Reducer<String, CachedItem> {

	private static final long serialVersionUID = -4815143738122549672L;

	@Override
	public CachedItem reduce(String key, Iterator<CachedItem> iter) {
		//assume there is only one 
		return iter.next();
	}
}
