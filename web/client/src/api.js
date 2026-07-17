import { io } from 'socket.io-client';

// Empty base => same origin, which the Vite dev proxy forwards to :4000.
const API_BASE = import.meta.env.VITE_API_BASE || '';

export async function fetchEvents(limit = 100) {
  const res = await fetch(`${API_BASE}/api/events?limit=${limit}`);
  if (!res.ok) throw new Error(`GET /api/events failed: ${res.status}`);
  return res.json();
}

export async function fetchStats() {
  const res = await fetch(`${API_BASE}/api/stats`);
  if (!res.ok) throw new Error(`GET /api/stats failed: ${res.status}`);
  return res.json();
}

export async function publishTestEvent(body) {
  const res = await fetch(`${API_BASE}/api/events`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`POST /api/events failed: ${res.status}`);
  return res.json();
}

export function connectSocket() {
  return io(API_BASE || undefined, { transports: ['websocket', 'polling'] });
}
