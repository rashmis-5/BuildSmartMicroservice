import { useEffect, useState } from 'react';
import { listNotifications, markRead, unreadCount } from '../api/notification.js';
import { useAuth } from '../auth/AuthContext.jsx';

const POLL_MS = 30000;

export default function NotificationBell() {
  const { isAuthenticated } = useAuth();
  const [count, setCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) return;
    const tick = async () => {
      try {
        const r = await unreadCount();
        const n = r?.data?.count ?? r?.count ?? 0;
        setCount(n);
      } catch {
        // notification service may be down — silent
      }
    };
    tick();
    const id = setInterval(tick, POLL_MS);
    return () => clearInterval(id);
  }, [isAuthenticated]);

  const toggle = async () => {
    const nextOpen = !open;
    setOpen(nextOpen);
    if (nextOpen) {
      setLoading(true);
      try {
        const r = await listNotifications({ size: 20 });
        const page = r?.data ?? r;
        setItems(page?.content || []);
      } catch {
        setItems([]);
      } finally {
        setLoading(false);
      }
    }
  };

  const handleMark = async (id) => {
    try {
      await markRead(id);
      setItems((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
      setCount((c) => Math.max(0, c - 1));
    } catch {
      // ignore
    }
  };

  return (
    <div className="position-relative">
      <button type="button" className="btn btn-outline-light btn-sm me-2" onClick={toggle}>
        🔔
        {count > 0 && (
          <span className="badge bg-danger ms-1">{count > 99 ? '99+' : count}</span>
        )}
      </button>
      {open && (
        <div
          className="dropdown-menu show p-2 shadow"
          style={{ position: 'absolute', right: 0, top: '110%', width: 360, maxHeight: 480, overflowY: 'auto', display: 'block' }}
        >
          <div className="d-flex justify-content-between align-items-center mb-2 px-2">
            <strong>Notifications</strong>
            <button type="button" className="btn-close" onClick={() => setOpen(false)} />
          </div>
          {loading && <div className="text-muted small px-2">Loading…</div>}
          {!loading && items.length === 0 && (
            <div className="text-muted small px-2">No notifications.</div>
          )}
          {!loading &&
            items.map((n) => (
              <div
                key={n.id}
                className={`p-2 border-bottom small ${n.read ? 'text-muted' : ''}`}
              >
                <div className="d-flex justify-content-between">
                  <strong>{n.eventType}</strong>
                  {!n.read && (
                    <button
                      className="btn btn-link btn-sm p-0"
                      onClick={() => handleMark(n.id)}
                    >
                      mark read
                    </button>
                  )}
                </div>
                <div>{n.message}</div>
                {n.referenceId && (
                  <div className="text-muted">ref: {n.referenceId}</div>
                )}
              </div>
            ))}
        </div>
      )}
    </div>
  );
}
