
java    -Dlog4j.configurationFile=log4j2-console.xml \
        -jar ./combined-worker/build/bin/corda-combined-worker-5.0.0.0-SNAPSHOT.jar \
	--messagingParams kafka.common.bootstrap.servers=127.0.0.1:9092 \
	--workspace-dir /tmp/corda \
	--secretsParams passphrase="bad passphrase" \
	--secretsParams salt="not so random" \
	--databaseParams database.user=user \
	--databaseParams database.pass.configSecret.encryptedSecret="4LNuCvt+NhGIBwL7gRRhvAZh3k6JRN9NHv0aG3pi1xM=" \
	--databaseParams database.jdbc.url=jdbc:postgresql://127.0.0.1:5432/allinonecluster \
        --proc db --proc rpc \
        | tee -a mixed-worker-$(date "+%Y-%m-%dT%H:%M:%S").log


#java -jar ./db-worker/build/bin/corda-db-worker-5.0.0.0-SNAPSHOT.jar \
#	--messagingParams kafka.common.bootstrap.servers=127.0.0.1:9092 \
#	--workspace-dir /tmp/corda \
#	--secretsParams passphrase="bad passphrase" \
#	--secretsParams salt="not so random" \
#	--databaseParams database.user=user \
#	--databaseParams database.pass.configSecret.encryptedSecret="4LNuCvt+NhGIBwL7gRRhvAZh3k6JRN9NHv0aG3pi1xM=" \
#	--databaseParams database.jdbc.url=jdbc:postgresql://127.0.0.1:5432/allinonecluster \
#        --healthMonitorPort 7001 \
#        | tee -a db-worker-$(date "+%Y-%m-%dT%H:%M:%S").log &

java -jar ./flow-worker/build/bin/corda-flow-worker-5.0.0.0-SNAPSHOT.jar \
	--messagingParams kafka.common.bootstrap.servers=127.0.0.1:9092 \
	--workspace-dir /tmp/corda \
        --healthMonitorPort 7002 \
        | tee -a flow-worker-$(date "+%Y-%m-%dT%H:%M:%S").log &

java -jar ./member-worker/build/bin/corda-member-worker-5.0.0.0-SNAPSHOT.jar \
	--messagingParams kafka.common.bootstrap.servers=127.0.0.1:9092 \
        --healthMonitorPort 7003 \
        | tee -a member-worker-$(date "+%Y-%m-%dT%H:%M:%S").log &


#java -jar ./rpc-worker/build/bin/corda-rpc-worker-5.0.0.0-SNAPSHOT.jar \
#	--messagingParams kafka.common.bootstrap.servers=127.0.0.1:9092 \
#        --healthMonitorPort 7004 \
#        | tee -a rpc-worker-$(date "+%Y-%m-%dT%H:%M:%S").log &

java -jar ./crypto-worker/build/bin/corda-crypto-worker-5.0.0.0-SNAPSHOT.jar \
	--messagingParams kafka.common.bootstrap.servers=127.0.0.1:9092 \
        --healthMonitorPort 7005 \
        | tee -a crypto-worker-$(date "+%Y-%m-%dT%H:%M:%S").log &

