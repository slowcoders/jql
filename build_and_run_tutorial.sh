docker-compose -f sample-app+tutorial/db/docker-compose.yml up -d postgres

pushd sample-app+tutorial/tutorial
npm install
npm run build
popd

pushd sample-app+tutorial
./gradlew bootRun --console=plain
popd
