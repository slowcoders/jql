#!/usr/bin/env bash

DIR=`dirname $0`
DELAY=1
if [ ! -e "$DIR/data/postgres" ]; then
    echo "installing DB.." $DIR/data/postgres
    DELAY=15
fi
docker-compose -f $DIR/docker-compose.yml down
docker-compose -f $DIR/docker-compose.yml up -d postgres
sleep $DELAY
