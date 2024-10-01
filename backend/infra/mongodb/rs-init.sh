#!/bin/bash

mongosh -u admin -p rootpass<<EOF
var config = {
    "_id": "mongo-rs",
    "version": 1,
    "members": [
        {
            "_id": 1,
            "host": "mongo-01:27017",
            "priority": 3
        },
        {
            "_id": 2,
            "host": "mongo-02:27017",
            "priority": 2
        },
        {
            "_id": 3,
            "host": "mongo-03:27017",
            "priority": 1
        }
    ]
};
rs.initiate(config, { force: true });
rs.status();

EOF
