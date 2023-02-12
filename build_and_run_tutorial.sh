pushd sample-app+tutorial/tutorial
npm install
npm run build
popd

pushd sample-app+tutorial
sh ./db/start_postgres.sh
./gradlew bootRun --console=plain
popd
