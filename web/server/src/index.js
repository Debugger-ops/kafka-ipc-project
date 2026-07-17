import http from 'node:http';
import express from 'express';
import cors from 'cors';
import { Server as SocketServer } from 'socket.io';

import { config } from './config.js';
import { connectMongo } from './db.js';
import { startConsumer } from './kafka/consumer.js';
import { startProducer, stopProducer } from './kafka/producer.js';
import { eventsRouter } from './routes/events.js';

async function main() {
  const app = express();
  app.use(cors({ origin: config.clientOrigin }));
  app.use(express.json());

  app.get('/api/health', (_req, res) => res.json({ ok: true }));
  app.use('/api', eventsRouter);

  const server = http.createServer(app);
  const io = new SocketServer(server, { cors: { origin: config.clientOrigin } });

  io.on('connection', (socket) => {
    console.log('[socket] client connected', socket.id);
    socket.on('disconnect', () => console.log('[socket] client disconnected', socket.id));
  });

  // Infrastructure first, then Kafka wiring.
  await connectMongo();
  await startProducer();
  const { shutdown: shutdownConsumer } = await startConsumer(io);

  server.listen(config.port, () => {
    console.log(`[http] API + WebSocket listening on http://localhost:${config.port}`);
    console.log(`[http] consuming Kafka topic '${config.kafka.topic}' via ${config.kafka.brokers.join(',')}`);
  });

  const shutdown = async () => {
    console.log('\n[app] shutting down...');
    await shutdownConsumer();
    await stopProducer();
    server.close(() => process.exit(0));
    setTimeout(() => process.exit(0), 3000).unref();
  };
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);
}

main().catch((err) => {
  console.error('[app] fatal startup error:', err);
  process.exit(1);
});
