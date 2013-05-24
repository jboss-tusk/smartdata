package org.jboss.tusk.smartdata.distexec;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

import org.jboss.logging.Logger;
import org.jboss.tusk.smartdata.ispn.InfinispanService;

public class DistributedSearch implements Callable<String>, Serializable {

	private static final long serialVersionUID = 4515611884638542107L;
	
	private static final Logger LOG = Logger.getLogger(DistributedSearch.class);

	private String criteria = null;
	
	public DistributedSearch(String criteria) {
		this.criteria = criteria;
	}
	
	@Override
	public String call() throws Exception {
		LOG.info("In call with criteria: " + this.criteria);
		
		InfinispanService ispnService = new InfinispanService();
		List<String> results = ispnService.search(criteria);
		
		return ispnService.makeJSONFromResultList(results);
	}

}
