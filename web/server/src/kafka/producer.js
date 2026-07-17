import { Kafka } from 'kafkajs';
import { randomUUID } from 'node:crypto';
import { config } from '../config.js';

const kafka = new Kafka({ clientId: config.kafka.clientId + '-producer', brokers: config.kafka.brokers });
const producer = kafka.producer({ idempotent: true });

let connected = false;

export async function startProducer() {
  await producer.connect();
  connected = true;
  console.log('[kafka] producer connected');
}

export async function stopProducer() {
  if (connected) await producer.disconnect();
}

/**
 * Publish an event onto `ipc-events` using the SAME JSON shape the Java
 * producer uses, so the existing Java consumer can also read it.
 * The event id is the record key (matches Java partitioning by id).
 */
export async function publishEvent({ type, source, payload }) {
  const event = {
    id: randomUUID(),
    type: type || 'WEB_EVENT',
    source: source || 'web-dashboard',
    payload: typeof payload === 'string' ? payload : JSON.stringify(payload ?? {}),
    timestamp: new Date().toISOString(),
  };

  await producer.send({
    topic: config.kafka.topic,
    messages: [{ key: event.id, value: JSON.stringify(event) }],
  });

  return event;
}
