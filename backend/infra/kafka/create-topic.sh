#!/bin/bash

topic=$1
partition=$2

if [ "$topic" == "" ]; then
	echo "topic is empty";
	exit 0;
fi

if [ "$partition" == "" ]; then
	echo "partition is empty";
	exit 0;
fi

kafka-topics --bootstrap-server localhost:9092 --create --topic "$topic" --partitions "$partition"
