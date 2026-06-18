import { useEffect, useState } from 'react';
import { Activity, BrainCircuit, RefreshCw } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { mlMonitorService, type MlFeatureImportanceEntry, type MlHealthResponse, type MlLogEntry } from '../services/mlMonitorService';

const liveRefreshMs = 30000;

export default function MlMonitorPage() {
  const [health, setHealth] = useState<MlHealthResponse | null>(null);
  const [logs, setLogs] = useState<MlLogEntry[]>([]);
  const [featureImportance, setFeatureImportance] = useState<MlFeatureImportanceEntry[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const load = async () => {
    setError('');
    try {
      const [nextHealth, nextLogs] = await Promise.all([
        mlMonitorService.health(),
        mlMonitorService.logs(),
      ]);
      setHealth(nextHealth);
      setLogs(nextLogs.logs);
      setLastUpdated(new Date());
      mlMonitorService.featureImportance()
        .then((importance) => setFeatureImportance(importance.feature_importance ?? []))
        .catch(() => setFeatureImportance([]));
    } catch {
      setError('Unable to load ML service telemetry.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const initial = window.setTimeout(load, 0);
    const timer = window.setInterval(load, liveRefreshMs);
    return () => {
      window.clearTimeout(initial);
      window.clearInterval(timer);
    };
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">ML Monitor</h1>
          <p className="text-sm text-slate-500">Live model health, request telemetry, and prediction events.</p>
          <p className="mt-1 text-xs text-slate-500">
            Last updated {lastUpdated ? lastUpdated.toLocaleTimeString() : 'loading'} · refreshes every 30s
          </p>
        </div>
        <Button variant="outline" className="gap-2" onClick={load}>
          <RefreshCw className="h-4 w-4" /> Refresh
        </Button>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="grid gap-4 md:grid-cols-3">
        <Card className="p-4">
          <div className="flex items-center gap-3">
            <BrainCircuit className="h-5 w-5 text-blue-600" />
            <div>
              <p className="text-sm font-medium text-slate-500">Model Status</p>
              <p className="mt-1 text-xl font-bold text-slate-900">{health?.status || (loading ? 'Loading' : 'Unknown')}</p>
            </div>
          </div>
        </Card>
        <Card className="p-4">
          <p className="text-sm font-medium text-slate-500">Model Version</p>
          <p className="mt-1 truncate text-xl font-bold text-slate-900">{health?.model_version || '-'}</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm font-medium text-slate-500">Reload Interval</p>
          <p className="mt-1 text-xl font-bold text-slate-900">{health?.reload_interval_seconds ?? 30}s</p>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Live Activity</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="divide-y divide-slate-100">
            {logs.length === 0 && <div className="p-6 text-sm text-slate-500">No ML logs yet.</div>}
            {logs.map((log, index) => (
              <div key={`${log.timestamp}-${index}`} className="flex flex-col gap-2 p-4 md:flex-row md:items-center md:justify-between">
                <div className="flex items-start gap-3">
                  <Activity className="mt-0.5 h-4 w-4 text-slate-400" />
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-medium text-slate-900">{log.eventType}</p>
                      <Badge variant={log.status === 'SUCCESS' ? 'success' : log.status === 'WARNING' ? 'warning' : 'danger'}>{log.status}</Badge>
                      {log.method && <span className="text-xs text-slate-500">{log.method} {log.path}</span>}
                    </div>
                    <p className="mt-1 text-xs text-slate-500">
                      {new Date(log.timestamp).toLocaleString()}
                      {typeof log.durationMs === 'number' && ` · ${log.durationMs} ms`}
                      {typeof log.statusCode === 'number' && ` · HTTP ${log.statusCode}`}
                    </p>
                    {typeof log.mlRiskScore === 'number' && <p className="mt-1 text-sm text-slate-600">ML Risk {log.mlRiskScore} · Probability {log.fraudProbability}</p>}
                    {log.message && <p className="mt-1 text-sm text-red-600">{log.message}</p>}
                  </div>
                </div>
                {log.modelVersion && <span className="text-xs text-slate-500">{log.modelVersion}</span>}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Feature Importance</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {featureImportance.length === 0 && <p className="text-sm text-slate-500">Train or reload the model to view feature importance.</p>}
            {featureImportance.slice(0, 10).map((item) => {
              const percent = Math.round((item.normalizedImportance ?? item.importance) * 100);
              return (
                <div key={item.feature}>
                  <div className="mb-1 flex items-center justify-between text-sm">
                    <span className="font-medium text-slate-700">{item.feature}</span>
                    <span className="text-slate-500">{percent}%</span>
                  </div>
                  <div className="h-2 rounded-full bg-slate-100">
                    <div className="h-2 rounded-full bg-blue-600" style={{ width: `${Math.min(percent, 100)}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
