import { useCallback, useEffect, useState } from 'react';
import { CheckCheck } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { notificationService } from '../services/notificationService';
import type { NotificationResponse } from '../types/api';

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      setNotifications(await notificationService.list());
    } catch {
      setError('Unable to load notifications.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initialTimer = window.setTimeout(() => {
      void load();
    }, 0);
    const timer = window.setInterval(load, 30000);
    return () => {
      window.clearTimeout(initialTimer);
      window.clearInterval(timer);
    };
  }, [load]);

  const markRead = async (id: number) => {
    await notificationService.markRead(id);
    await load();
  };

  const markAllRead = async () => {
    await notificationService.markAllRead();
    await load();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Notification Center</h1>
          <p className="text-sm text-slate-500">In-app notifications for fraud alerts, case assignments, reports, and security events.</p>
        </div>
        <Button variant="outline" className="gap-2" onClick={markAllRead}><CheckCheck className="h-4 w-4" /> Mark all read</Button>
      </div>
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}
      <Card>
        <div className="divide-y divide-slate-100">
          {loading && <div className="p-8 text-center text-slate-500">Loading notifications...</div>}
          {!loading && notifications.length === 0 && <div className="p-8 text-center text-slate-500">No notifications yet.</div>}
          {!loading && notifications.map((notification) => (
            <div key={notification.id} className={`flex items-start justify-between gap-4 p-4 ${notification.read ? 'bg-white' : 'bg-blue-50/60'}`}>
              <div>
                <div className="flex items-center gap-2">
                  <Badge variant={notification.read ? 'default' : 'info'}>{notification.type}</Badge>
                  {!notification.read && <span className="text-xs font-semibold text-blue-600">NEW</span>}
                </div>
                <p className="mt-2 text-sm text-slate-700">{notification.message}</p>
                <p className="mt-1 text-xs text-slate-500">{new Date(notification.createdAt).toLocaleString()}</p>
              </div>
              {!notification.read && <Button size="sm" variant="outline" onClick={() => markRead(notification.id)}>Mark read</Button>}
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
