import { useEffect, useMemo, useState } from 'react';
import { Search } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Card } from '../components/common/Card';
import { auditService } from '../services/auditService';
import type { AuditLogResponse } from '../types/api';

const liveRefreshMs = 30000;

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLogResponse[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  useEffect(() => {
    let active = true;

    const refresh = async (showSpinner = false) => {
      if (showSpinner) {
        setLoading(true);
      }
      setError('');
      try {
        const data = await auditService.listAuditLogs();
        if (active) {
          setLogs(data);
          setLastUpdated(new Date());
        }
      } catch {
        if (active) {
          setError('Unable to load audit logs.');
        }
      } finally {
        if (active && showSpinner) {
          setLoading(false);
        }
      }
    };

    refresh(true);
    const timer = window.setInterval(() => refresh(false), liveRefreshMs);

    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  const visibleLogs = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) {
      return logs;
    }
    return logs.filter((log) =>
      log.eventType.toLowerCase().includes(needle)
      || log.description.toLowerCase().includes(needle)
      || String(log.bankId ?? '').includes(needle)
    );
  }, [logs, query]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Audit Logs</h1>
        <p className="text-sm text-slate-500">Read-only platform activity and compliance history.</p>
        <p className="mt-1 text-xs text-slate-500">
          Last updated {lastUpdated ? lastUpdated.toLocaleTimeString() : 'loading'} · refreshes every 30s
        </p>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <Card>
        <div className="border-b border-slate-100 bg-slate-50/50 p-4">
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="w-full rounded-md border border-slate-300 bg-white py-2 pl-9 pr-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              placeholder="Search events, description, bank..."
            />
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-500">
              <tr>
                <th className="px-6 py-4">Event</th>
                <th className="px-6 py-4">Description</th>
                <th className="px-6 py-4">Bank</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-500">Loading audit logs...</td></tr>}
              {!loading && visibleLogs.length === 0 && <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-500">No audit logs found.</td></tr>}
              {!loading && visibleLogs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 font-medium text-slate-900">{log.eventType}</td>
                  <td className="px-6 py-4 text-slate-600">{log.description}</td>
                  <td className="px-6 py-4 text-slate-600">{log.bankId ?? '-'}</td>
                  <td className="px-6 py-4"><Badge variant={log.status === 'SUCCESS' ? 'success' : 'danger'}>{log.status}</Badge></td>
                  <td className="px-6 py-4 text-right text-slate-500">{new Date(log.timestamp).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
