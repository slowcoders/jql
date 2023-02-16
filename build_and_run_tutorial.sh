#!/usr/bin/env bash

docker-compose -f ./db/docker-compose.yml up -d postgres

pushd ./tutorial
npm install
npm run build
popd

pushd sample-app
./gradlew bootRun --console=plain
popd

