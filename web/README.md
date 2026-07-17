# Kafka IPC — MERN Web Layer

A **MERN** (MongoDB · Express · React · Node) bridge that sits on top of the existing
Java/Kafka IPC pipeline. It consumes the live `ipc-events` Kafka topic, stores every event
in MongoDB, and streams events to a real-time React dashboard over WebSocket (Socket.IO).

Your Java producer/consumer processes are untouched — Kafka is still the IPC channel. This
layer is just another independent consumer (its own consumer group) plus a producer.

---

## How it all connects

Five processes talk to each other. Nothing calls anything directly — everything meets at
the Kafka topic and the browser socket:

```
  ┌────────────────────┐      publish        ┌──────────────────────┐
  │  Java EventProducer │ ──────────────────▶ │   Kafka: ipc-events  │
  │  (./run.sh produce) │                     │   (3 partitions)     │
  └────────────────────┘                     └──────────┬───────────┘
                                                         │ subscribe
                            ┌────────────────────────────┼─────────────────────────┐
                            ▼                            ▼                          │
                 ┌────────────────────┐      ┌────────────────────────┐            │
                 │ Java EventConsumer │      │  Node consumer (Express)│            │
                 │  (./run.sh consume)│      │  kafka/consumer.js      │            │
                 └────────────────────┘      └───────────┬────────────┘            │
                                                         │ save         ▲          │
                                                         ▼              │ publish  │
                                              ┌────────────────┐   ┌────┴──────────┴──┐
                                              │    MongoDB     │   │ Express producer │
                                              │  kafka_ipc db  │   │ POST /api/events │
                                              └────────────────┘   └──────────────────┘
                                                         │ emit               ▲
                                                         ▼ Socket.IO          │ "Publish test event"
                                              ┌──────────────────────────────┴───┐
                                              │        React dashboard :5173      │
                                              └──────────────────────────────────┘
```

**The three connection points you configure** (all in `server/.env`):

| Connection | Setting | Default | Who else uses it |
|-----------|---------|---------|------------------|
| Node ⇄ Kafka | `KAFKA_BROKERS` / `KAFKA_TOPIC` | `localhost:9092` / `ipc-events` | same broker + topic as the Java apps |
| Node ⇄ MongoDB | `MONGODB_URI` | `mongodb://localhost:27017/kafka_ipc` | just this bridge |
| React ⇄ Node | `PORT` + `CLIENT_ORIGIN` | `4000` + `http://localhost:5173` | the browser (REST + WebSocket) |

In dev, the React app reaches the API through Vite's proxy (`/api` and `/socket.io` →
`localhost:4000`), so you don't set anything on the client side.

---

## Prerequisites

- Node.js 18+ (native `fetch` + `--watch`)
- Docker (for Kafka + MongoDB), or your own Kafka/Mongo instances
- The parent Java project buildable (`./run.sh build`) if you want to stream real events

---

## Run it — step by step

Run each block in its **own terminal**. Order matters: infra first, then the bridge,
then the UI, then generate traffic.

### 1. Start Kafka + create the topic  (terminal A, from `kafka-ipc-project/`)

```bash
./run.sh up        # Zookeeper + Kafka + Kafka UI
./run.sh topic     # creates ipc-events (3 partitions)
```

### 2. Start MongoDB  (from `kafka-ipc-project/web/`)

```bash
docker compose -f docker-compose.mongo.yml up -d
```

### 3. Start the backend bridge  (terminal B)

```bash
cd web/server
cp .env.example .env      # edit only if your broker/mongo differ from the defaults
npm install
npm run dev               # → http://localhost:4000
```

You should see `[mongo] connected`, `[kafka] producer connected`,
`[kafka] consumer connected`, and `[http] ... listening on http://localhost:4000`.

### 4. Start the dashboard  (terminal C)

```bash
cd web/client
npm install
npm run dev               # → http://localhost:5173
```

### 5. Generate some events  (terminal D, from `kafka-ipc-project/`)

```bash
./run.sh produce 30       # Java producer streams 30 sensor readings
```

Open **http://localhost:5173** — rows should appear live as events stream in.

> **Quick sanity check without Java:** skip step 5 and instead use the dashboard's
> **Publish a test event** card. It sends an event through Express → Kafka → back into the
> Node consumer → MongoDB → socket, so a successful round-trip proves the whole chain is wired.

---

## How people use the dashboard

Once it's open at `http://localhost:5173`:

- **Live status pill** (top-right) shows *Live* when the WebSocket is connected, *Disconnected* otherwise.
- **Stat cards** — total events stored, distinct event types, partitions seen, and a rolling *events/sec* rate.
- **Charts** — events by type (bar) and by Kafka partition (pie), refreshed every couple of seconds.
- **Live event feed** — newest events at the top with time, type, source, payload, partition, and offset. New rows flash briefly.
- **Publish a test event** — fill in type / source / payload and click *Publish to ipc-events*. The event travels the full pipeline and reappears in the feed within a second.

Anyone on the same network can use it too: run the client with `--host`
(`npm run dev -- --host`) or build it (`npm run build`) and serve `client/dist`, then set
`VITE_API_BASE` (or a reverse proxy) so the browser can reach the API host.

---

## API reference

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/health` | liveness check → `{ ok: true }` |
| GET | `/api/events?limit=100&type=SENSOR_READING` | recent events, newest first |
| GET | `/api/stats` | `{ total, byType, byPartition, latestAt }` |
| POST | `/api/events` | body `{ type, source, payload }` → publishes to Kafka |
| WS | `event` | Socket.IO event emitted for every stored event |

---

## Configuration (`server/.env`)

| Var | Default | Meaning |
|-----|---------|---------|
| `PORT` | `4000` | API + WebSocket port |
| `KAFKA_BROKERS` | `localhost:9092` | comma-separated broker list |
| `KAFKA_TOPIC` | `ipc-events` | topic to consume/produce |
| `KAFKA_GROUP_ID` | `web-bridge-group` | keep distinct from the Java `ipc-consumer-group` |
| `MONGODB_URI` | `mongodb://localhost:27017/kafka_ipc` | MongoDB connection |
| `CLIENT_ORIGIN` | `http://localhost:5173` | CORS origin for the React dev server |

---

## Layout

```
web/
├── docker-compose.mongo.yml   # convenience MongoDB container
├── server/                    # Express + kafkajs + mongoose + socket.io
│   └── src/
│       ├── index.js           # app entry: HTTP + WebSocket, wires everything
│       ├── config.js          # env-driven config
│       ├── db.js              # mongoose connection
│       ├── models/Event.js    # mirrors com.example.ipc.model.Event
│       ├── kafka/consumer.js  # ipc-events -> MongoDB -> socket emit
│       ├── kafka/producer.js  # publish test events (same JSON shape as Java)
│       └── routes/events.js   # GET /api/events, GET /api/stats, POST /api/events
└── client/                    # Vite + React dashboard (recharts, socket.io-client)
```

---

## Troubleshooting

| Symptom | Likely cause / fix |
|---------|--------------------|
| `npm error Missing script: "dev"` | You're in the wrong folder. Run `npm run dev` inside `web/server` or `web/client`, not the project root. |
| Backend exits with a Kafka/Mongo connection error | Start Kafka (`./run.sh up`) and MongoDB (step 2) **before** the backend. |
| Dashboard shows *Disconnected* | Backend isn't running on `:4000`, or `CLIENT_ORIGIN` doesn't match the client URL. |
| Feed stays empty but status is *Live* | No new events yet — the consumer reads only events published after it starts. Run `./run.sh produce 30` or publish a test event. |
| Want to replay history on startup | In `server/src/kafka/consumer.js`, set `fromBeginning: true`. |

---

## Notes

- Events are upserted by their producer-generated `id`, so at-least-once redelivery
  won't create duplicate documents.
- The Node producer writes the exact JSON shape (`id, type, source, payload, timestamp`)
  the Java `EventDeserializer` expects, so events published from the dashboard are also
  readable by the Java consumer.
- For production, build the client (`npm run build`) and serve `client/dist` behind the
  same origin as the API, or point `VITE_API_BASE` at the API host.
```
