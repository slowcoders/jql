docker-compose -f ./db/docker-compose.yml up postgres

pushd ./tutorial
npm install
npm run build
popd

pushd sample-app
./gradlew bootRun --console=plain
popd

