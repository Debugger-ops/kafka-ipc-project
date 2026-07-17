import { Router } from 'express';
import { EventModel } from '../models/Event.js';
import { publishEvent } from '../kafka/producer.js';

export const eventsRouter = Router();

/** GET /api/events?limit=100&type=SENSOR_READING — recent events, newest first. */
eventsRouter.get('/events', async (req, res) => {
  const limit = Math.min(parseInt(req.query.limit || '100', 10), 500);
  const filter = {};
  if (req.query.type) filter.type = req.query.type;

  const events = await EventModel.find(filter).sort({ receivedAt: -1 }).limit(limit).lean();
  res.json(events);
});

/** GET /api/stats — aggregate counts for the dashboard cards/charts. */
eventsRouter.get('/stats', async (_req, res) => {
  const [total, byType, byPartition, latest] = await Promise.all([
    EventModel.countDocuments(),
    EventModel.aggregate([
      { $group: { _id: '$type', count: { $sum: 1 } } },
      { $sort: { count: -1 } },
    ]),
    EventModel.aggregate([
      { $group: { _id: '$partition', count: { $sum: 1 } } },
      { $sort: { _id: 1 } },
    ]),
    EventModel.findOne().sort({ receivedAt: -1 }).lean(),
  ]);

  res.json({
    total,
    byType: byType.map((t) => ({ type: t._id, count: t.count })),
    byPartition: byPartition.map((p) => ({ partition: p._id, count: p.count })),
    latestAt: latest?.receivedAt ?? null,
  });
});

/** POST /api/events — publish a test event onto the Kafka topic. */
eventsRouter.post('/events', async (req, res) => {
  try {
    const event = await publishEvent(req.body || {});
    res.status(202).json({ published: true, event });
  } catch (err) {
    res.status(500).json({ published: false, error: err.message });
  }
});
