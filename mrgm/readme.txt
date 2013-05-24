How to Start JDGConsume

The JDG consumer program is run on cirries-2002. It requires the target directory to be there. This directory, which is created during the build of the jdg project, should be copied from the smartdata-client/ directory.

---Startup Command---
>cd /root/juniper/mrgm
>./jdgconsume -j 8nodejdg.properties -a 1 -m 1000
Options:
     -j the properties file configuring how the consumer works; includes
buffer and batch sizes, as well as URLs for all JDG nodes
     -a how many messages to batch up before acknowledging
     -m how many messages to consume from the queue before terminating

---JDGConsume Properties File---
8nodejdg.properties is the version that should be used for demos
Fields:
     -buffer.size how many messages to store locally before dispatching to the JDG nodes
     -batch.size how big each batch of messages is for a single JDG node; buffer.size / batch.size = number of concurrent dispatcher threads; buffer.size should be a multiple of batch.size
     -ingester.server comma-separate list of server name and JBoss remoting port for all JDG nodes; the number of concurrent dispatcher threads should be >= the number of JDG nodes

---Shutdown Command---
>ctrl-c
Notes:
     Depending on when the consumer is killed, it might leave messages on the MRGM queue that are in a bad state
     If this happens the MRGM bad messages should be manually cleared out via: TBD

