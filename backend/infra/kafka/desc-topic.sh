#!/bin/bash

topic=$1

if [ "$topic" == "" ]; then
	echo "topic is empty";
	exit 0;
fi

kafka-topics --bootstrap-server localhost:9092 --describe --topic $topic
