import { useState } from 'react';
import { publishTestEvent } from '../api.js';

export default function PublishForm() {
  const [type, setType] = useState('WEB_EVENT');
  const [source, setSource] = useState('web-dashboard');
  const [payload, setPayload] = useState('{"note":"hello from the dashboard"}');
  const [status, setStatus] = useState(null);
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setStatus(null);
    try {
      const res = await publishTestEvent({ type, source, payload });
      setStatus({ ok: true, id: res.event.id });
    } catch (err) {
      setStatus({ ok: false, msg: err.message });
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="card publish" onSubmit={submit}>
      <div className="card-title">Publish a test event</div>
      <label>
        Type
        <input value={type} onChange={(e) => setType(e.target.value)} />
      </label>
      <label>
        Source
        <input value={source} onChange={(e) => setSource(e.target.value)} />
      </label>
      <label>
        Payload
        <textarea rows="2" value={payload} onChange={(e) => setPayload(e.target.value)} />
      </label>
      <button type="submit" disabled={busy}>
        {busy ? 'Publishing…' : 'Publish to ipc-events'}
      </button>
      {status?.ok && <div className="ok-msg">Published ✓ id {status.id.slice(0, 8)}…</div>}
      {status && !status.ok && <div className="err-msg">Failed: {status.msg}</div>}
    </form>
  );
}
