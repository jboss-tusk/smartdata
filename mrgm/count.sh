#!/bin/bash -x

#echo "1: "
curl http://cirries-3001:8080/smartdata-web/rest/count
#echo "2: "
curl http://cirries-3001:8081/smartdata-web/rest/count
#echo "3: "
curl http://cirries-3001:8082/smartdata-web/rest/count
#echo "4: "
curl http://cirries-3001:8083/smartdata-web/rest/count
#echo "5: "
curl http://cirries-3002:8080/smartdata-web/rest/count
#echo "6: "
curl http://cirries-3002:8081/smartdata-web/rest/count
#echo "7: "
curl http://cirries-3002:8082/smartdata-web/rest/count
#echo "8: "
curl http://cirries-3002:8083/smartdata-web/rest/count
