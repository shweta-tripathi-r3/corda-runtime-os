# Taken from corda-runtime-os 2022-03-29

KAFKA_BIN=/Users/chris.barratt/ExtraSWInstallers/kafka/current/bin
BOOTSTRAP_SERV=localhost:9092


echo -e 'Creating kafka topics'
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic config.management.request
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic config.management.request.resp
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic config.topic --config "cleanup.policy=compact"

${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic p2p.in
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic p2p.out

${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic virtual.node.creation.request
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic virtual.node.creation.request.resp
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic virtual.node.info --config "cleanup.policy=compact"
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic cpi.info --config "cleanup.policy=compact"

${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic cpi.upload
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic cpi.upload.status

${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic cpk.file --config "cleanup.policy=compact"

${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic rpc.permissions.management
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic rpc.permissions.management.resp
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic rpc.permissions.user --config "cleanup.policy=compact"
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic rpc.permissions.group --config "cleanup.policy=compact"
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic rpc.permissions.role --config "cleanup.policy=compact"
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic rpc.permissions.permission --config "cleanup.policy=compact"
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic permissions.user.summary --config "cleanup.policy=compact"

${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic flow.status --config "cleanup.policy=compact"
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic flow.event
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic flow.event.state --config "cleanup.policy=compact"
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic flow.event.dlq
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic flow.mapper.event
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic flow.mapper.event.state --config "cleanup.policy=compact"
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic flow.mapper.event.dlq

${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic membership.members --config "cleanup.policy=compact"
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic membership.rpc.ops
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic membership.rpc.ops.resp

${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic crypto.ops.rpc
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic crypto.ops.rpc.resp
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic crypto.key.soft
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic crypto.key.info
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --partitions 1 --replication-factor 1 --create --topic crypto.ops.flow

echo -e 'Successfully created the following topics:'
${KAFKA_BIN}/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERV --list

