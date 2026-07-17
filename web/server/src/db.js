import mongoose from 'mongoose';
import { config } from './config.js';

export async function connectMongo() {
  mongoose.connection.on('connected', () => console.log('[mongo] connected'));
  mongoose.connection.on('error', (err) => console.error('[mongo] error', err.message));
  mongoose.connection.on('disconnected', () => console.warn('[mongo] disconnected'));

  await mongoose.connect(config.mongoUri);
  return mongoose.connection;
}
