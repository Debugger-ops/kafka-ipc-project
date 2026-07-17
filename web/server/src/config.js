import 'dotenv/config';

export const config = {
  port: parseInt(process.env.PORT || '4000', 10),
  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9092').split(','),
    topic: process.env.KAFKA_TOPIC || 'ipc-events',
    groupId: process.env.KAFKA_GROUP_ID || 'web-bridge-group',
    clientId: 'kafka-ipc-web-bridge',
  },
  mongoUri: process.env.MONGODB_URI || 'mongodb://localhost:27017/kafka_ipc',
  clientOrigin: process.env.CLIENT_ORIGIN || 'http://localhost:5173',
};
