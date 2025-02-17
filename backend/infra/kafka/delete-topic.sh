#!/bin/bash

topic=$1

if [ "$topic" == "" ]; then
	echo "topic is emtpy";
	exit 0;
fi

kafka-topics --delete --bootstrap-server localhost:9092 --topic $topic
