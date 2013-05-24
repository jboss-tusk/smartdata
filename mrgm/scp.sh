#!/bin/bash -x

scp smartdata-ear.ear root@cirries-3001:/root/juniper/jdg/jboss-eap-6.0_a/standalone/deployments/ 
scp smartdata-ear.ear root@cirries-3001:/root/juniper/jdg/jboss-eap-6.0_b/standalone/deployments/
scp smartdata-ear.ear root@cirries-3001:/root/juniper/jdg/jboss-eap-6.0_c/standalone/deployments/
scp smartdata-ear.ear root@cirries-3001:/root/juniper/jdg/jboss-eap-6.0_d/standalone/deployments/
scp smartdata-ear.ear root@cirries-3002:/root/juniper/jdg/jboss-eap-6.0_a/standalone/deployments/
scp smartdata-ear.ear root@cirries-3002:/root/juniper/jdg/jboss-eap-6.0_b/standalone/deployments/
scp smartdata-ear.ear root@cirries-3002:/root/juniper/jdg/jboss-eap-6.0_c/standalone/deployments/
scp smartdata-ear.ear root@cirries-3002:/root/juniper/jdg/jboss-eap-6.0_d/standalone/deployments/
