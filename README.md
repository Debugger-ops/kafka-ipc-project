# Kafka IPC — Interprocess Communication with Apache Kafka

A hands-on project that implements **Interprocess Communication (IPC)** between distributed
processes using **Apache Kafka** for real-time data streaming. Independent producer and
consumer processes exchange data asynchronously through a Kafka topic, coordinated by
**ZooKeeper**, with JSON message serialization and an event-driven design.

---

## What this project demonstrates

- **IPC via a message broker** — decoupled processes communicate by publishing and
  subscribing to a shared Kafka topic instead of talking to each other directly.
- **Real-time data streaming** — a producer continuously streams events that consumers
  react to as they arrive.
- **Reliable, scalable, asynchronous communication** — `acks=all` + idempotent producer,
  a partitioned topic, and consumer groups for horizontal scaling.
- **Fault-tolerant pipelines** — partitions and consumer-group rebalancing let the system
  survive process failures; offsets are committed only after processing (at-least-once).
- **Message serialization** — custom Kafka `Serializer`/`Deserializer` convert domain
  `Event` objects to and from JSON (Jackson).
- **Event-driven architecture & distributed system design** — the consumer is a pure event
  handler; producers and consumers scale and deploy independently.

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Messaging broker | Apache Kafka 3.7 (Confluent `cp-kafka` 7.6 image) |
| Cluster coordination | Apache ZooKeeper (`cp-zookeeper` 7.6 image) |
| Client library | Kafka Clients (Producer, Consumer & Admin API) |
| Serialization | JSON via Jackson (`jackson-databind`, `jsr310`) |
| Language / build | Java 17, Maven (shade plugin for a runnable fat jar) |
| Logging | SLF4J + slf4j-simple |
| Infrastructure | Docker Compose (Zookeeper + Kafka + Kafka UI) |
| Monitoring (optional) | Kafka UI at http://localhost:8080 |

---

## Architecture

```
 ┌──────────────┐        publish         ┌───────────────────────────┐        subscribe        ┌──────────────┐
 │  Producer    │ ─────────────────────▶ │        Kafka Broker       │ ─────────────────────▶  │  Consumer    │
 │  process     │   topic: ipc-events    │   (topic: ipc-events,     │   consumer group:       │  process     │
 │ (EventProducer)│  JSON-serialized     │    3 partitions)          │   ipc-consumer-group    │(EventConsumer)│
 └──────────────┘     Event objects      └────────────┬──────────────┘                         └──────────────┘
                                                       │ registers / coordinates
                                                       ▼
                                              ┌──────────────────┐
                                              │    ZooKeeper     │  broker metadata, controller
                                              │                  │  election, partition assignment
                                              └──────────────────┘
```

The producer and consumer are **separate processes** (separate JVMs). They never reference
each other — Kafka is the IPC channel. This is what makes the design distributed, decoupled
and independently scalable.

---

## Project layout

```
kafka-ipc-project/
├── docker-compose.yml          # Zookeeper + Kafka + Kafka UI
├── pom.xml                     # Maven build (Kafka, Jackson, SLF4J)
├── run.sh                      # convenience wrapper
└── src/main/java/com/example/ipc/
    ├── App.java                # entry point: create-topic | produce | consume
    ├── config/KafkaConfig.java # bootstrap servers, topic, partitions, group id
    ├── model/Event.java        # the message exchanged between processes
    ├── serde/                  # JSON Serializer / Deserializer + shared ObjectMapper
    ├── admin/TopicManager.java # creates the topic via the Admin API
    ├── producer/EventProducer.java
    └── consumer/EventConsumer.java
```

---

## Prerequisites

- Docker + Docker Compose
- Java 17+
- Maven 3.8+

---

## Quick start

```bash
# 1. Start the cluster (Zookeeper + Kafka + Kafka UI)
./run.sh up

# 2. Build the runnable jar
./run.sh build

# 3. Create the topic
./run.sh topic

# 4. In one terminal, start the consumer process
./run.sh consume

# 5. In another terminal, stream events from the producer process
./run.sh produce 30

# 6. (optional) Open the Kafka UI to inspect topics/messages
#    http://localhost:8080

# 7. Tear everything down
./run.sh down
```

### Manual commands (without run.sh)

```bash
docker compose up -d
mvn clean package
java -jar target/kafka-ipc.jar create-topic
java -jar target/kafka-ipc.jar consume        # terminal A
java -jar target/kafka-ipc.jar produce 30     # terminal B
```

To see **scalability and fault tolerance** in action, start the consumer in two or three
terminals — because they share the `ipc-consumer-group`, Kafka splits the 3 partitions
across them. Kill one and watch the partitions rebalance onto the survivors.

---

## Web dashboard (MERN layer)

The [`web/`](web/README.md) folder adds an optional **MERN** (MongoDB · Express · React ·
Node) layer on top of this pipeline. A Node/Express service consumes the `ipc-events` topic,
stores events in MongoDB, and streams them to a live React dashboard over WebSocket — with a
form to publish test events back onto the topic. The Java processes are untouched; the bridge
is just another independent Kafka consumer.

See **[web/README.md](web/README.md)** for full setup, the connection diagram, and usage. In
short:

```bash
./run.sh up && ./run.sh topic                              # Kafka + topic
cd web && docker compose -f docker-compose.mongo.yml up -d  # MongoDB
cd server && cp .env.example .env && npm install && npm run dev   # API on :4000
cd ../client && npm install && npm run dev                  # dashboard on :5173
```

---

## Running Kafka + ZooKeeper manually (no Docker)

If you'd rather not use Docker, download Kafka from https://kafka.apache.org/downloads and:

```bash
# Start ZooKeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start the Kafka broker (new terminal)
bin/kafka-server-start.sh config/server.properties
```

Then point the apps at your broker (defaults to `localhost:9092`, override with the
`KAFKA_BOOTSTRAP_SERVERS` environment variable) and run the same `create-topic / produce /
consume` commands as above.

---

## Configuration

All shared settings live in `KafkaConfig.java`:

| Setting | Default | Meaning |
|---------|---------|---------|
| `BOOTSTRAP_SERVERS` | `localhost:9092` | broker address (env `KAFKA_BOOTSTRAP_SERVERS` overrides) |
| `TOPIC` | `ipc-events` | topic used as the IPC channel |
| `PARTITIONS` | `3` | parallelism for scalable consumption |
| `REPLICATION_FACTOR` | `1` | fine for a single-broker dev cluster |
| `CONSUMER_GROUP` | `ipc-consumer-group` | consumers here share the partitions |
```
