package org.jboss.tusk.smartdata.domain.cgnat;

import java.util.Iterator;

import org.infinispan.distexec.mapreduce.Reducer;


public class NATLogSearchReducer implements Reducer<String, NATLog> {

	private static final long serialVersionUID = -4815143738122549672L;

	@Override
	public NATLog reduce(String key, Iterator<NATLog> iter) {
		//assume there is only one 
		return iter.next();
	}
}
