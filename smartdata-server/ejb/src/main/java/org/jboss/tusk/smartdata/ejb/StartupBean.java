package org.jboss.tusk.smartdata.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.tusk.smartdata.ispn.InfinispanService;

@Singleton
@Startup
public class StartupBean {
    @PostConstruct
    private void postConstruct() {
    	//do this just to make sure ispn is initialized on startup.
    	InfinispanService ispnService = new InfinispanService(); 
    }

    @PreDestroy
    private void preDestroy() { /* ... */ }
}