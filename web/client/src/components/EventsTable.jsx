function fmtTime(v) {
  if (!v) return '—';
  const d = new Date(v);
  return d.toLocaleTimeString([], { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0');
}

export default function EventsTable({ events }) {
  return (
    <div className="card table-card">
      <div className="card-title">
        Live event feed <span className="muted">({events.length} shown)</span>
      </div>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Time</th>
              <th>Type</th>
              <th>Source</th>
              <th>Payload</th>
              <th>Part.</th>
              <th>Offset</th>
            </tr>
          </thead>
          <tbody>
            {events.length === 0 && (
              <tr>
                <td colSpan="6" className="empty">
                  Waiting for events… run the Java producer or publish a test event.
                </td>
              </tr>
            )}
            {events.map((e) => (
              <tr key={e.eventId} className="row-new">
                <td className="mono">{fmtTime(e.receivedAt)}</td>
                <td>
                  <span className="badge">{e.type}</span>
                </td>
                <td>{e.source}</td>
                <td className="mono payload">{e.payload}</td>
                <td className="mono center">{e.partition ?? '—'}</td>
                <td className="mono center">{e.offset ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
