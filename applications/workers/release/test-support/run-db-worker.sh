java -jar ./db-worker/build/bin/corda-db-worker-5.0.0.0-SNAPSHOT.jar \
	--messagingParams kafka.common.bootstrap.servers=127.0.0.1:9092 \
	--workspace-dir /tmp/corda \
	--secretsParams passphrase="bad passphrase" \
	--secretsParams salt="not so random" \
	--databaseParams database.user=user \
	--databaseParams database.pass.configSecret.encryptedSecret="4LNuCvt+NhGIBwL7gRRhvAZh3k6JRN9NHv0aG3pi1xM=" \
	--databaseParams database.jdbc.url=jdbc:postgresql://127.0.0.1:5432/allinonecluster \
        | tee -a corda-cluster-$(date "+%Y-%m-%dT%H:%M:%S").log

#        --instanceId $(od -A n -N 5 -t u4 /dev/urandom | tr -d ' \n') \

#java -jar ./combined-worker/build/bin/corda-combined-worker-5.0.0.0-SNAPSHOT.jar \
#	--messagingParams kafka.common.bootstrap.servers=127.0.0.1:9092 \
#	--workspace-dir /tmp/corda 
