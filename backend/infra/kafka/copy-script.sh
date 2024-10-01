#!/bin/bash

files="list-topic.sh desc-topic.sh create-topic.sh delete-topic.sh";

for file in $files; do
	docker cp $file kafka:/home/appuser
done
