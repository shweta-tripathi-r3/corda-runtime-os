# Persistence POC

## Design

https://lucid.app/lucidspark/invitations/accept/inv_3879490b-d04e-495b-87c7-5322ca73dd42

## Deploy/Run

Docker Compose configuration in the `deploy` folder. This sets up:
- Zookeeper
- Single Kafka broker
- Cluster DB (Postgres)
- App DB (Postgres)
- Kafdrop: UI for Kafka

### Start network:

In `applications/examples/persistence/deploy`:

```shell
docker compose up
```

### Create Kafka topics:

When network is running, create topics:

```shell
./create-topics.sh
```

or to first delete existing persistence-demo topics:

```shell
./create-topics.sh delete
```

### Kafdrop

To launch the [Kafdrop](https://github.com/HomeAdvisor/Kafdrop) UI, browse to: http://localhost:9000/
