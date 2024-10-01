#!/bin/bash

curl -s --location 'http://localhost:8083/connectors' \
--header 'Content-Type: application/json' \
--data '{
    "name": "chat-connector",
    "config": {
        "connector.class": "io.debezium.connector.mysql.MySqlConnector",
        "database.allowPublicKeyRetrieval":"true",
        "database.hostname": "host.docker.internal",
        "database.port": "3307",
        "database.user": "root",
        "database.password": "root1234",
        "database.include.list": "modu-chat",
        "table.include.list": "modu-chat.chat",
        "topic.prefix": "modu",
        "schema.history.internal.kafka.bootstrap.servers": "kafka:9092",
        "schema.history.internal.kafka.topic": "schema-changes.db",
        "database.server.id": 1,
        "transforms": "createdAt",
        "transforms.createdAt.type": "org.apache.kafka.connect.transforms.TimestampConverter$Value",
        "transforms.createdAt.target.type": "string",
        "transforms.createdAt.field": "created_date",
        "transforms.createdAt.format": "yyyy-MM-dd HH:mm:ss",
        "time.precision.mode": "connect"
    }
}' | python3 -m json.tool
