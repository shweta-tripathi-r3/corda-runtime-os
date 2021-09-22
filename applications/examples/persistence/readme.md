# Persistence POC

## Design

https://lucid.app/lucidspark/invitations/accept/inv_3879490b-d04e-495b-87c7-5322ca73dd42

## Run

Docker Compose configuration in the `deploy` folder. This sets up:
- Zookeeper
- Single Kafka broker
- Cluster DB (Postgres)
- App DB (Postgres)
- Kafdrop: UI for Kafka