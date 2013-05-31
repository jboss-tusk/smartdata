Red Hat Smart Data Reference Architecture
=========

This is the source code for the Smart Data solution reference architecture. It is an integration of various Red Hat and JBoss technologies with some big data technologies to address common big data integration use cases.

This top directory contains all projects for the Red Hat portion of the solution. In some instances this code has been integrated with partner systems, in which case their portion of the overall solution code is housed elsewhere. This repository includes the JBoss Data Grid (JDG) component and the MRGM consumer, which handles buffering and batching of messages if required. It can be run in two modes: jdgconsume mode and JDG-only mode. 

In jdgconsume mode there is a C++ consumer that reads messages from the MRGM queue, hands them off to a java program (smartdata-client) via JNI, which buffers and dispatches the messages to the JDG cluster. jdgconsume mode is useful when there is a very high throughput of individual MRGM messages (ie when they are not batched upstream or when the batches are small). This is done because the C++ consumer is faster than the Java consumer. 

In JDG-only mode the jdgconsume process is not run. Instead, the JDG nodes themselves consume messages off of the MRGM queue. This should be done when the data items are batched into large (ie 50MB) MRGM messages. 

The JDG nodes have been tested to run on JBoss EAP 6.0.1.

The top-level projects are as follows:
* smartdata-client	this is a java program that accepts messages, buffers them, then dispatches them to the JDG cluster
* smartdata-server	this is the code used to run the JDG nodes; it handles indexing and storage of JDG data, as well as searches
* eap6				these are the custom configuration files used in JBoss EAP6, which is the container for the smartdata-server deployable. There is a 'jboss' application user created with password 'password' and role 'guest'.
* mrgm				this contains the C++ MRGM consumer, which hands incoming messages off to the JDG cluster via JNI and the smartdata-client

------------------

Steps to Build and Deploy:
The Maven pom.xml file in the top directory is used to build the smartdata-client and smartdata-server projects. This results in the following deployables:
* smartdata-client	the entire smartdata-client/target directory is copied into the mrgm directory on the server
* smartdata-server	the smartdata-server/ear/target/smartdata-ear.ear file is copied into the $JBOSS_ROOT/jboss-eap-6.0/standalone/deployments directory on the each JDG node
* eap6				this project does not need to be built; its files are copied into the appropriate places in the $JBOSS_ROOT directory on the JDG node
* mrgm				this project can only be built on the server; the entire directory is copied to the server; this assumes that it is running on the same server as the MRGM broker

Note that multiple JDG nodes (ie separate EAP6 processes) can be run at one time on a single VM. This is done by installing EAP6 in multiple directories and changing the jboss.socket.binding.port-offset value at the bottom of each installation's configuration/standalone-full-amqp.xml file.

There is a sample Maven settings.xml file in the top level directory that can be used to build the projects. 

------------------

The Smart Data code running within EAP6 server can be configured at runtime via system properties, as seen in the jdg-start-qmqp.sh file and as listed below:
* __-b [ip_address_of_vm]__: this must be an IP address that is used by a valid interface
* __-Djgroups.bind_addr=[ip_address_of_vm]__: this must be an IP address that is used by a valid interface
* __--server-config=standalone-full-amqp.xml__: use this to change to a different EAP configuration file
* __-Ddomain=[payload_domain]__: which data payload type to use (ie which use case we are running under); valid values are 'cable' and 'cgnat'; TODO need to check whether this is still supported
* __-Dcacheditemhelper=[full_classname_of_cacheditem_helper]__: the full class name, with package, of the CachedItemHelper for the data payload type that is to be used; see CachedItemHelperFactory.java for more information; current valid values are org.jboss.tusk.smartdata.domain.cable.STBLogHelper and org.jboss.tusk.smartdata.domain.cgnat.NATLogHelper
* __-Dparsetype=[type]__: how to treat Strings containing incoming data (ie how to parse them); the default value depends on the CachedItemHelper implementation; see STBLogHelper.java and NATLogHelper.java
* __-DsearchResultCacheTimeout=[time_in_ms]__: time, in ms, that each search's results are cached in memory; default timeout is 60,000 ms

------------------

Steps to Configure a Pristine EAP6.0.1 to be able to Run smartdata-server Code:

1. Download a copy of EAP 6.0.1 (jboss-eap-6.0.1.zip) from the Red Hat Customer Access Portal.
2. Unzip it into a directory, referred to as $JBOSS_ROOT.
3. Copy the contents of the eap6 directory into their corresponding locations within $JBOSS_ROOT/jboss-eap-6.0/.
4. Update the $JBOSS_ROOT/jboss-eap-6.0/bin/jdg-start-amqp.sh and .../jdg-stop.sh files to use the appropriate IP address and management port (which will change from the default 9999 if running multiple EAP6 processes on a single VM, each using a different set of ports).
5. Update the $JBOSS_ROOT/jboss-eap-6.0/bin/standalone.conf file to tune the JVM appropriately (ie for heap size, garbage collection, etc).
6. Update the $JBOSS_ROOT/jboss-eap-6.0/standalone/configuration/standalone-full-amqp.xml file to change the AMQP host address and queue names in the QPID resource-adapter.
7. Make sure that UDP multicast is enabled on all VMs that will be running the JDG nodes in EAP6.
8. Deploy the latest smartdata-ear.ear file to the $JBOSS_ROOT/jboss-eap-6.0/standalone/deployments directory.
9. Run the $JBOSS_ROOT/jboss-eap-6.0/bin/jdg-start-amqp.sh script to start that node.

------------------

In order to compile the mrgm project on the server, that server must have MRGM installed and various configurations done, as follows:

* Register the RHEL server. For example, use rhn_register:
    - [RedHatNetworkUserName]/[RedHatNetworkPassword]
    - Check on all available updates
    - enter unique profile name
* Go to RedHatNetwork and add channels to the system
    - https://rhn.redhat.com/rhn/systems/SystemList.do -> click on system -> Alter Channel Subscriptions
    - Additional Services Channels for Red Hat Enterprise Linux 6 / MRG Messaging v.2
    - Release Channels for Red Hat Enterprise Linux 6 /  RHEL Server High Availability
* Yum installs
    - yum -y groupinstall "Messaging Client Support"
    - yum -y groupinstall "MRG Messaging"
    - yum -y install qpid-cpp-server-cluster
    - yum -y groupinstall "development tools"
    - yum -y install java-1.6.0-openjdk java-1.6.0-openjdk-devel
* MRG-M Setup
    - service qpidd start
    - Add to bash profile or startup script: export LD_LIBRARY_PATH=/usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/jre/lib/amd64/server
* JDG Prerequisite Setup
    - vim /etc/sysctl.conf
        + # Allow a 25MB UDP receive buffer for JGroups
        + net.core.rmem_max = 26214400
        + # Allow a 1MB UDP send buffer for JGroups
        + net.core.wmem_max = 1048576
    - sysctl net.core.rmem_max=26214400
    - sysctl net.core.wmem_max=1048576
* Install POC Software

------------------

Steps to add support for a new use case (ie new data payload):

1. Create a new module under smartdata-server to house the implementation classes for the new use case. Use the "cable" module as a basis.
2. Update the smartdata-server's pom.xml to add a reference to the new module. 
3. Update the smartdata-ear's pom.xml to add a dependency to your new module.
4. Provide custom subclass for CachedItem (use STBLog as an example). This include the fields to be stored/indexed in JDG, methods to parse input strings to create new objects, methods used in MapReduce jobs, etc. 
5. Provide custom implementation for CachedItemHelper (see STBLogHelper as an example). This is used to parse multiple CachedItems, provide info for the JDG searches, and generally help with usage of the corresponding CachedItem. These help keep the core system (smartdata-ejb) implementation independent.
6. Be sure to add a unit test for the CachedItem subclass.

------------------

Below is a list of several of the main classes, including the purpose they serve. There are other classes and files in the different modules/directories, but they are not currently used for the demos we support.

###smartdata-ejb module
* __CachedItem.java__ - abstract superclass that all data payload objects for specific use cases should extend; these are what's added to the JDG cache
* __CachedItemHelper.java__ - interface for the helper class that the core framework uses to work with the actual data payload objects
* __CachedItemHelperFactory.java__ - factory class that the core framework uses to get CachedItemHelpers; uses the cacheditemhelper system property to decide which data payload type to use at runtime
* __DistributedSearch.java__ - Callable implementation that is used for distributed searches
* __IngesterMDB.java__ - message driven bean that listens on a queue for messages with data payload(s) to ingest; the queue (destination) used in the class annotation must match a queue defined in the standalone-full-amqp.xml EAP6 file 
* __IngesterHelper.java__ - helper class that handles the actual work for ingesting data payload(s)
* __RemoteSearcher.java/SearcherEJB.java__ - EJB that is used to do local and distributed searches, as well as MapReduce jobs, against the JDG cache
* __StartupBean.java__ - instantiated during EAP6 startup to initialize the JDG cache(s), getting them to go ahead and form the clusters so processing can start immediately once the app is started up
* __InfinispanService.java__ - facade to the JDG (Infinispan) api; used for configuring caches, writing/loading values, doing searches, and starting MapReduce jobs
* __CachedItemSearchMapper.java__ - mapper implementation for the MapReduce engine
* __CachedItemSearchReducer.java__ - reducer implementation for the MapReduce engine
* __ispn_index.xml__ - configuration for the JDG caches
* __ispn_index_lucene.xml__ - configuration for the lucene JDG caches in the event that the lucen index is stored in JDG
* __jgroups_smartdata.xml__ - jgroups configuration for clustering

###smartdata-web module
* __Search.java__ - JAX-RS implementation for the search REST service; parses to/from JSON
* __SearchService.java__ - bridge between the Search rest service implementation and the Searcher EJB, which does the actual searches
* __Ingest.java__ - JAX-RS implementation for the ingest REST service; parses to/from JSON
* __IngestService.java__ - bridge between the Ingest rest service implementation and the IngestHelper class, which does the actual ingests

###cable module
* __STBLog.java__ - data payload implementation for the cable Set Top Box (STB) use case; represents data sent from a cable box to a central system for storage/search
* __STBLogHelper.java__ - helper implementation for the cable Set Top Box use case

###cgnat module
* __NATLog.java__ - data payload implementation for the carrier grade network address translation (CGNAT) use case; represents a single network address translation (NAT) log file
* __NATLogHelper.java__ - helper implementation for the carrier grade NAT use case

###eap6 directory
* __jdg-start-qmqp.sh__ - start script for EAP6 when running in a setup where the IngesterMDB consumes messages directly off of the AMQP queue; the IP address must match a valid IP address on the server on which it is running
* __jdg-stop.sh__ - stop script for EAP6
* __standalone.conf__ - startup options for the JVM on which EAP6 is running
* __application-users.properties/application-roles.properties__ - contains an application user, required by the code; its username is 'jboss' and its password is 'password', role 'guest'
* __standalone-full-amqp.xml__ - EAP6 standalone configuration file; contains configuration for the qpid (AMQP) resource adapter, the AMQP queues, and other items
* __qpid-ra-0.18.rar__ - the qpid resource adapter to be able to consume messages from the amqp queue

###mrgm directory
* __jdgconsume.cpp__ - the C++ program that consumes messages off of the QMP queue (if running in jdgconsume mode), buffering and subsequently dispatching them to the JDG nodes via a JMS queue or directly via a remote EJB reference
* __8nodejdg.properties__ - properties file to be used when there is an 8 node JDG cluster
* __count.sh__ - helper script to see how many items have been added to each JDG node
* __poll-intake-queue.sh/poll-jdg-queue.sh__ - command that shows the statistics for specific AMQP queues, which are executed in a never-ending loop
* __scp.sh__ - script to copy the smartdata-ear.ear file to all JDG nodes

------------------

###TODOs

Below is a list of outstanding TODOs for this project, in order of priority as of 5/31/2013:
* Make sure that we are using the latest Infinishap and hibernate-search libraries; the hibernate-search version currently used was built off of the hibernate-search project trunk
* Test MapReduce with the new code organization.
* Figure out why searches take so long when it's the first search (or first search in a while) whereas subsequent searches are faster
* Test with larger maxEntries sizes (in ispn_index.xml) to see how many entries we can support (which will be different for different payload types)
* Figure out how to use off-heap memory to increase the total amount of memory available and prevent long garbage collection pauses from large heap sizes
* Once RHS and Hadoop are integrated, create some MapReduce (and Hive or Pig?) code to demonstrate usage of Hadoop for analytics and other processing


