#!/bin/bash

while :
 do
   qpid-stat -q | grep intake
   sleep 1 
 done
