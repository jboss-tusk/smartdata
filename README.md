smartdata
=========

This is the source code for the Smart Data solution reference architecture. It is an integration of various Red Hat and JBoss technologies with some big data technologies to address common big data integration use cases.

This top directory contains all projects for the Red Hat portion of the solution. In some instances this code has been integrated with partner systems, in which case their portion of the overall solution code is housed elsewhere. This repository includes the JBoss Data Grid (JDG) component and the MRGM consumer, which handles buffering and batching of messages if required. It can be run in two modes: jdgconsume mode and JDG-only mode. 

In jdgconsume mode there is a C++ consumer that reads messages from the MRGM queue, hands them off to a java program (smartdata-client) via JNI, which buffers and dispatches the messages to the JDG cluster. jdgconsume mode is useful when there is a very high throughput of individual MRGM messages (ie when they are not batched upstream or when the batches are small). This is done because the C++ consumer is faster than the Java consumer. 

In JDG-only mode the jdgconsume process is not run. Instead, the JDG nodes themselves consume messages off of the MRGM queue. This should be done when the data items are batched into large (ie 50MB) MRGM messages. 

The JDG nodes have been tested to run on JBoss EAP 6.0.1.

The projects are as follows:
* smartdata-client	this is a java program that accepts messages, buffers them, then dispatches them to the JDG cluster
* smartdata-server	this is the code used to run the JDG nodes; it handles indexing and storage of JDG data, as well as searches
* eap6				these are the custom configuration files used in JBoss EAP6, which is the container for the smartdata-server deployable. There is a 'jboss' application user created with password 'password' and role 'guest'.
* mrgm				this contains the C++ MRGM consumer, which hands incoming messages off to the JDG cluster via JNI and the smartdata-client

How to Build and Deploy:
The Maven pom.xml file in the top directory is used to build the smartdata-client and smartdata-server projects. This results in the following deployables:
* smartdata-client	the entire smartdata-client/target directory is copied into the mrgm directory on the server
* smartdata-server	the smartdata-server/ear/target/smartdata-ear.ear file is copied into the $JOSS_HOME/standalone/deployments directory on the server
* eap6				this project does not need to be built; its files are copied into the appropriate places in the $JOSS_HOME directory on the server
* mrgm				this project can only be built on the server; the entire directory is copied to the server; this assumes that it is running on the same server as the MRGM broker

There is also a sample Maven settings.xml file in the top level directory that can be used to build the projects. In order to compile the mrgm project on the server, that server must have MRGM installed and various configurations done, as follows:

Register the RHEL server. For example, use rhn_register:
    [RedHatNetworkUserName]/[RedHatNetworkPassword]
    Check on all available updates
    enter unique profile name
Go to RedHatNetwork and add channels to the system
    https://rhn.redhat.com/rhn/systems/SystemList.do -> click on system -> Alter Channel Subscriptions
    Additional Services Channels for Red Hat Enterprise Linux 6 / MRG Messaging v.2
    Release Channels for Red Hat Enterprise Linux 6 /  RHEL Server High Availability
Yum installs
    yum -y groupinstall "Messaging Client Support"
    yum -y groupinstall "MRG Messaging"
    yum -y install qpid-cpp-server-cluster
    yum -y groupinstall "development tools"
    yum -y install java-1.6.0-openjdk java-1.6.0-openjdk-devel
MRG-M Setup
    service qpidd start
    Add to bash profile or startup script: export LD_LIBRARY_PATH=/usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/jre/lib/amd64/server
JDG Prerequisite Setup
    vim /etc/sysctl.conf
        # Allow a 25MB UDP receive buffer for JGroups
        net.core.rmem_max = 26214400
        # Allow a 1MB UDP send buffer for JGroups
        net.core.wmem_max = 1048576
    sysctl net.core.rmem_max=26214400
    sysctl net.core.wmem_max=1048576
Install POC Software

