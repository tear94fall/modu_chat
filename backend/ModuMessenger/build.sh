#!/bin/bash

# remove *.jar file
rm build/libs/*

# gradle clean
./gradlew clean build

# gradle build
./gradlew build

# stop container
docker stop modu-chat

# remove container
docker rm modu-chat

# remove container images
docker rmi modu-chat-backend

# build container
docker build -t modu-chat-backend .

# run container
docker run --name modu-chat -d -it -p 8080:8080 modu-chat-backend
