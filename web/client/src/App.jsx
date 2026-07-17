import { useEffect, useRef, useState } from 'react';
import { fetchEvents, fetchStats, connectSocket } from './api.js';
import StatCard from './components/StatCard.jsx';
import EventsTable from './components/EventsTable.jsx';
import { TypeChart, PartitionChart } from './components/Charts.jsx';
import PublishForm from './components/PublishForm.jsx';

const MAX_ROWS = 100;

export default function App() {
  const [events, setEvents] = useState([]);
  const [stats, setStats] = useState({ total: 0, byType: [], byPartition: [] });
  const [connected, setConnected] = useState(false);
  const [rate, setRate] = useState(0);
  const recentTimestamps = useRef([]);

  // Initial load.
  useEffect(() => {
    fetchEvents(MAX_ROWS).then(setEvents).catch(console.error);
    fetchStats().then(setStats).catch(console.error);
  }, []);

  // Live socket stream.
  useEffect(() => {
    const socket = connectSocket();
    socket.on('connect', () => setConnected(true));
    socket.on('disconnect', () => setConnected(false));
    socket.on('event', (evt) => {
      setEvents((prev) => [evt, ...prev].slice(0, MAX_ROWS));
      recentTimestamps.current.push(Date.now());
    });
    return () => socket.close();
  }, []);

  // Refresh aggregate stats periodically + compute events/sec.
  useEffect(() => {
    const id = setInterval(() => {
      fetchStats().then(setStats).catch(() => {});
      const cutoff = Date.now() - 5000;
      recentTimestamps.current = recentTimestamps.current.filter((t) => t > cutoff);
      setRate((recentTimestamps.current.length / 5).toFixed(1));
    }, 2000);
    return () => clearInterval(id);
  }, []);

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1>Kafka IPC — Live Dashboard</h1>
          <p className="subtitle">
            MERN bridge over the <code>ipc-events</code> topic — Express · MongoDB · Socket.IO · React
          </p>
        </div>
        <div className={`conn ${connected ? 'on' : 'off'}`}>
          <span className="dot" />
          {connected ? 'Live' : 'Disconnected'}
        </div>
      </header>

      <section className="stats-grid">
        <StatCard label="Total events stored" value={stats.total} accent="#6366f1" />
        <StatCard label="Event types" value={stats.byType.length} accent="#22c55e" />
        <StatCard label="Partitions seen" value={stats.byPartition.length} accent="#f59e0b" />
        <StatCard label="Events / sec" value={rate} accent="#06b6d4" />
      </section>

      <section className="charts-grid">
        <TypeChart data={stats.byType} />
        <PartitionChart data={stats.byPartition} />
        <PublishForm />
      </section>

      <section>
        <EventsTable events={events} />
      </section>

      <footer className="foot">
        Producer (Java) → Kafka <code>ipc-events</code> → Node consumer → MongoDB → WebSocket → this dashboard
      </footer>
    </div>
  );
}
