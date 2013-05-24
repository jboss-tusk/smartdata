#!/bin/sh

#This starts JDG configured to handle data from Connect; use -Ddomaon=stblog to support the Cablevision use case
./standalone.sh -b 192.168.201.161 -Djgroups.bind_addr=192.168.201.161 --server-config=standalone-full-amqp.xml -Ddomain=stblog -Dcacheditemhelper=org.jboss.tusk.smartdata.domain.cable.STBLogHelper &

#This starts JDG configured to handle data from the test driver on cirries-2002
#./standalone.sh -b 192.168.201.161 --server-config=standalone-full-amqp.xml -Dparsetype=pipeSeparatedJSON &
