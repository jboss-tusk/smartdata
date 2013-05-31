#!/bin/bash

while :
 do
   qpid-stat -q | grep jdg
   sleep 1 
 done
