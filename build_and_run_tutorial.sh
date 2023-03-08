#!/usr/bin/env bash

docker-compose -f ./db/docker-compose.yml up -d postgres

pushd ./tutorial+test
npm install
npm run build
popd

pushd sample-app
./gradlew bootRun --console=plain --args='--spring.profiles.active=demo'
popd

