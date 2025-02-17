#!/bin/bash

curl -s -X GET -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ | python3 -m json.tool
