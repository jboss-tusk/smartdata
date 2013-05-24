The smartdata-server project contains three modules:

* ear	this contains the ejb and war modules, bundling them into a deployable that can be deployed to the JBoss EAP6 server
* ejb	this contains the JDG server side code used to ingest, index, store, and search the data elements
* war	this contains the web UI and REST interfaces

ejb module
The org.jboss.tusk.smartdata.distexec package contains the impementation of the JDG distributed search functionality.
The org.jboss.tusk.smartdata.ejb package contains EJBs for ingesting into the data grid and searching its values. The key classes are below:
	IngesterMDB - this message driven bean retrieves a batched message off of the MRGM queue, parses it, and writes it to the data grid.
	HQIngesterMDB - this message driven bean retrieves a jdgconsume-originated, batched message off of a local HornetQ queue, parses it, and writes it to the data grid.
	SearcherEJB - this is the interface into the actual JDG search code, providing additional functionality suck as paging and caching.
The org.jboss.tusk.smartdata.ispn package contains classes closely tied to the Infinispan project (which is the open source version of JDG). The key classes are below:
	InfinispanService - this creates and configures the JDG cache, and writes/reads/searches data items.
	NATLog - this is the class whose objects are added to the JDG cache
	SearchCriterion - this represents one clause of a search
The org.jboss.tusk.smartdata.mapreduce package contains classes used to do map reduce searches across the data grid

web module
The org.jboss.tusk.smartdata.rest package contains the JAX-RS class (Search) and a bridge to the SearcherEJB (SearchService)
The org.jboss.tusk.smartdata.ui.controller package contains the web UI controller classes, which call into the IngesterEJB and SearcherEJB.
The src/main/webapp directory contains the HTML pages and Faces config for the web ui.
