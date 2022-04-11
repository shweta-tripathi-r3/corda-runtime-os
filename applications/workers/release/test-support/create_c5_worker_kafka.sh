#!/usr/bin/env bash -x

# Taken from corda-runtime-os 2022-03-29

KAFKA_BIN=/Users/chris.barratt/ExtraSWInstallers/kafka/current/bin
KAFKA_CONF=/Users/chris.barratt/ExtraSWInstallers/kafka/current/config

echo "Starting zookeeper, logging to ./zookeeper.log"
${KAFKA_BIN}/zookeeper-server-start.sh ${KAFKA_CONF}/zookeeper.properties 2>&1 > zookeeper.log &

echo "Starting broker, logging to ./kafka.log"
${KAFKA_BIN}/kafka-server-start.sh ${KAFKA_CONF}/server.properties 2>&1 > kafka.log &

