#!/usr/bin/env bash -x
CONT_NAME=all-in-one-db

docker container rm $CONT_NAME

docker run \
  --name $CONT_NAME \
  --env POSTGRES_DB="allinonecluster" \
  --env POSTGRES_USER="user" \
  --env POSTGRES_PASSWORD="pass" \
  --publish 5432:5432 \
  postgres 
  
