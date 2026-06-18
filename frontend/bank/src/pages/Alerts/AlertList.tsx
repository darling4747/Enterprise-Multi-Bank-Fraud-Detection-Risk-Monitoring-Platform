import { useEffect, useMemo, useState } from 'react';
import { Clock, Search, Filter, AlertOctagon, CheckCircle2 } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { fraudService } from '../../services/fraudService';
import { authService } from '../../services/authService';
import type { AlertResponse, AlertStatus } from '../../types/api';
import { canManageAlerts } from '../../utils/permissions';

const liveRefreshMs = 30000;

export default function AlertList() {
  const [alerts, setAlerts] = useState<AlertResponse[]>([]);
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | AlertStatus>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const canManage = canManageAlerts();
  const currentUser = authService.getStoredUser()?.username;

  const loadAlerts = async (filter = statusFilter, showSpinner = true) => {
    if (showSpinner) {
      setLoading(true);
    }
    setError('');
    try {
      setAlerts(await fraudService.getAlerts(filter === 'ALL' ? undefined : filter));
      setLastUpdated(new Date());
    } catch {
      setError('Unable to load alerts from the backend.');
    } finally {
      if (showSpinner) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    let active = true;

    const refresh = async (showSpinner = false) => {
      if (showSpinner) {
        setLoading(true);
      }
      setError('');
      try {
        const data = await fraudService.getAlerts(statusFilter === 'ALL' ? undefined : statusFilter);
        if (active) {
          setAlerts(data);
          setLastUpdated(new Date());
        }
      } catch {
        if (active) {
          setError('Unable to load alerts from the backend.');
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
  }, [statusFilter]);

  const visibleAlerts = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) {
      return alerts;
    }
    return alerts.filter((alert) =>
      alert.title.toLowerCase().includes(needle)
      || alert.transactionReference.toLowerCase().includes(needle)
      || String(alert.id).includes(needle)
    );
  }, [alerts, query]);

  const criticalCount = alerts.filter((alert) => alert.severity === 'CRITICAL').length;
  const pendingCount = alerts.filter((alert) => alert.status === 'OPEN' || alert.status === 'IN_REVIEW').length;
  const resolvedCount = alerts.filter((alert) => alert.status === 'RESOLVED').length;
  const openVisibleAlerts = visibleAlerts.filter((alert) => alert.status === 'OPEN');
  const criticalVisibleAlerts = visibleAlerts.filter((alert) =>
    alert.status === 'OPEN' && (alert.severity === 'CRITICAL' || alert.severity === 'HIGH')
  );

  const updateStatus = async (alert: AlertResponse, nextStatus: 'IN_REVIEW' | 'RESOLVED') => {
    setError('');
    try {
      await fraudService.updateAlertStatus(alert.id, nextStatus, currentUser);
      await loadAlerts(statusFilter, false);
    } catch {
      setError('Unable to update alert status.');
    }
  };

  const updateMany = async (items: AlertResponse[], nextStatus: 'IN_REVIEW' | 'RESOLVED') => {
    if (items.length === 0) {
      return;
    }
    setError('');
    try {
      await Promise.all(items.map((alert) => fraudService.updateAlertStatus(alert.id, nextStatus, currentUser)));
      await loadAlerts(statusFilter, false);
    } catch {
      setError('Unable to update selected alerts.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Fraud Alerts</h1>
          <p className="text-slate-500 text-sm mt-1">Review and action suspicious activities flagged by the risk engine.</p>
          <p className="mt-1 text-xs text-slate-500">
            Last updated {lastUpdated ? lastUpdated.toLocaleTimeString() : 'loading'} · refreshes every 30s
          </p>
        </div>
        {canManage && (
          <div className="flex gap-2">
            <Button variant="outline" disabled={openVisibleAlerts.length === 0} onClick={() => updateMany(openVisibleAlerts, 'IN_REVIEW')}>Review Open</Button>
            <Button
              variant="primary"
              disabled={criticalVisibleAlerts.length === 0}
              onClick={() => updateMany(criticalVisibleAlerts, 'IN_REVIEW')}
              className="bg-red-600 hover:bg-red-700 border-red-600"
            >
              Investigate Critical
            </Button>
          </div>
        )}
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <Card className="border-l-4 border-l-red-500">
          <div className="p-4 flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-slate-500">Critical Alerts</p>
              <h4 className="text-2xl font-bold text-slate-900 mt-1">{criticalCount}</h4>
            </div>
            <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
              <AlertOctagon className="w-5 h-5 text-red-600" />
            </div>
          </div>
        </Card>
        <Card className="border-l-4 border-l-amber-500">
          <div className="p-4 flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-slate-500">Pending Review</p>
              <h4 className="text-2xl font-bold text-slate-900 mt-1">{pendingCount}</h4>
            </div>
            <div className="w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center">
              <Clock className="w-5 h-5 text-amber-600" />
            </div>
          </div>
        </Card>
        <Card className="border-l-4 border-l-emerald-500">
          <div className="p-4 flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-slate-500">Resolved Today</p>
              <h4 className="text-2xl font-bold text-slate-900 mt-1">{resolvedCount}</h4>
            </div>
            <div className="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center">
              <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            </div>
          </div>
        </Card>
      </div>

      <Card>
        <div className="p-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="relative w-80">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input 
              type="text" 
              placeholder="Search alerts by ID or rule type..." 
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-lg text-sm focus:border-red-500 focus:ring-1 focus:ring-red-500 outline-none"
            />
          </div>
          <div className="flex items-center gap-2">
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as 'ALL' | AlertStatus)}
              className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500"
            >
              <option value="ALL">All statuses</option>
              <option value="OPEN">Open</option>
              <option value="IN_REVIEW">In review</option>
              <option value="RESOLVED">Resolved</option>
              <option value="FALSE_POSITIVE">False positive</option>
            </select>
            <Button variant="outline" className="px-3" onClick={() => loadAlerts(statusFilter)}>
              <Filter className="w-4 h-4 text-slate-500 mr-2" /> Apply
            </Button>
          </div>
        </div>

        <div className="divide-y divide-slate-100">
          {loading && <div className="p-8 text-center text-slate-500">Loading alerts...</div>}
          {!loading && visibleAlerts.length === 0 && <div className="p-8 text-center text-slate-500">No alerts found.</div>}
          {!loading && visibleAlerts.map((alert) => (
            <div key={alert.id} className="p-4 hover:bg-slate-50 transition-colors flex flex-col md:flex-row gap-4 items-start md:items-center justify-between group">
              <div className="flex items-start gap-4 flex-1">
                <div className={`mt-1 w-2 h-2 rounded-full shrink-0 ${
                  alert.severity === 'CRITICAL' ? 'bg-red-600 shadow-[0_0_8px_rgba(220,38,38,0.6)]' :
                  alert.severity === 'HIGH' ? 'bg-orange-500' :
                  alert.severity === 'MEDIUM' ? 'bg-amber-400' : 'bg-blue-400'
                }`}></div>
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <h4 className="font-semibold text-slate-900">{alert.title}</h4>
                    <Badge variant={
                      alert.status === 'OPEN' ? 'danger' :
                      alert.status === 'IN_REVIEW' ? 'warning' :
                      alert.status === 'RESOLVED' ? 'success' : 'default'
                    }>
                      {alert.status}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-4 text-sm text-slate-500">
                    <span className="flex items-center gap-1">
                      <span className="font-mono text-xs bg-slate-100 px-1.5 py-0.5 rounded">{alert.id}</span>
                    </span>
                    <span>&bull;</span>
                    <span>Ref: <span className="text-blue-600 hover:underline cursor-pointer">{alert.transactionReference}</span></span>
                    <span>&bull;</span>
                    <span className="flex items-center gap-1"><Clock className="w-3 h-3" /> {new Date(alert.createdAt).toLocaleString()}</span>
                  </div>
                  <p className="text-sm text-slate-500 mt-2">{alert.message}</p>
                </div>
              </div>
              
              <div className="flex items-center gap-6 w-full md:w-auto justify-between md:justify-end border-t md:border-t-0 pt-3 md:pt-0 border-slate-100 mt-2 md:mt-0">
                <div className="text-right">
                  <p className="text-xs text-slate-500">Severity</p>
                  <p className="font-medium text-slate-900">{alert.severity}</p>
                </div>
                {canManage && alert.status === 'OPEN' && (
                  <Button size="sm" variant="primary" className="bg-red-600 hover:bg-red-700 border-red-600 text-white" onClick={() => updateStatus(alert, 'IN_REVIEW')}>
                    Review
                  </Button>
                )}
                {canManage && alert.status === 'IN_REVIEW' && (
                  <Button size="sm" variant="outline" onClick={() => updateStatus(alert, 'RESOLVED')}>
                    Resolve
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
