sh sample-app+tutorial/db/start_postgres.sh

pushd sample-app+tutorial/tutorial
npm install
npm run build
popd

pushd sample-app+tutorial
./gradlew bootRun --console=plain
popd
