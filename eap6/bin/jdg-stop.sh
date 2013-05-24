#!/bin/sh

#Change the port number to the appropriate port for the given offset if the EAP instance uses something other than the default ports
#E.g. 10000, 10001, 10002
./jboss-cli.sh --connect command=:shutdown --controller=localhost:9999
