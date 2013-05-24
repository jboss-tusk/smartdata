#!/bin/sh

#This starts JDG configured to handle data from Connect
./standalone.sh -b 192.168.201.161 --server-config=standalone-full.xml &

#This starts JDG configured to handle data from the test driver on cirries-2002
#./standalone.sh -b 192.168.201.161 --server-config=standalone-full.xml -Dparsetype=pipeSeparatedJSON &
