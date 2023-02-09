pushd sample-app+tutorial
sh ./db/start_postgres.sh
./gradlew bootRun --console=plain
popd
