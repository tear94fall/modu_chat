#!/bin/bash

curl -s -i -X DELETE localhost:8083/connectors/chat-connector | python3 -m json.tool
