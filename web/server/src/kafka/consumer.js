import { Kafka } from 'kafkajs';
import { config } from '../config.js';
import { EventModel } from '../models/Event.js';

/**
 * Consumes the `ipc-events` topic (the IPC channel used by the Java processes),
 * persists each event to MongoDB, and emits it to connected dashboard clients
 * over Socket.IO in real time.
 */
export async function startConsumer(io) {
  const kafka = new Kafka({ clientId: config.kafka.clientId, brokers: config.kafka.brokers });
  const consumer = kafka.consumer({ groupId: config.kafka.groupId });

  await consumer.connect();
  console.log('[kafka] consumer connected');
  await consumer.subscribe({ topic: config.kafka.topic, fromBeginning: false });

  await consumer.run({
    eachMessage: async ({ topic, partition, message }) => {
      let parsed;
      try {
        parsed = JSON.parse(message.value.toString());
      } catch (err) {
        console.error('[kafka] skipping non-JSON message', err.message);
        return;
      }

      const doc = {
        eventId: parsed.id,
        type: parsed.type,
        source: parsed.source,
        payload: parsed.payload ?? '',
        timestamp: parsed.timestamp ? new Date(parsed.timestamp) : new Date(),
        partition,
        offset: message.offset,
        receivedAt: new Date(),
      };

      try {
        // Upsert on eventId so at-least-once redelivery is idempotent.
        const saved = await EventModel.findOneAndUpdate(
          { eventId: doc.eventId },
          { $setOnInsert: doc },
          { upsert: true, new: true, setDefaultsOnInsert: true }
        );
        io.emit('event', saved.toObject());
      } catch (err) {
        console.error('[kafka] failed to persist event', err.message);
      }
    },
  });

  const shutdown = async () => {
    try {
      await consumer.disconnect();
    } catch {
      /* ignore */
    }
  };
  return { consumer, shutdown };
}
