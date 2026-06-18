import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Plus } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { securityIncidentService } from '../services/securityIncidentService';
import { hasAnyRole } from '../utils/permissions';
import type { RiskLevel, SecurityAlertResponse, SecurityIncidentStatus, SecurityIncidentType } from '../types/api';

const incidentTypes: SecurityIncidentType[] = ['IMPOSSIBLE_TRAVEL', 'MULTIPLE_MFA_FAILURES', 'ACCOUNT_TAKEOVER_ATTEMPT', 'PRIVILEGE_ESCALATION', 'MULTIPLE_FAILED_LOGINS', 'NEW_DEVICE_LOGIN', 'ADMIN_PRIVILEGE_CHANGE'];
const severities: RiskLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const statuses: SecurityIncidentStatus[] = ['OPEN', 'UNDER_REVIEW', 'RESOLVED', 'FALSE_POSITIVE'];

export default function SecurityIncidentsPage() {
  const canManage = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN']);
  const [incidents, setIncidents] = useState<SecurityAlertResponse[]>([]);
  const [form, setForm] = useState({ eventType: 'MULTIPLE_FAILED_LOGINS' as SecurityIncidentType, severity: 'HIGH' as RiskLevel, description: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      setIncidents(await securityIncidentService.list());
    } catch {
      setError('Unable to load security incidents.');
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

  const createIncident = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    try {
      await securityIncidentService.create(form);
      setForm({ eventType: 'MULTIPLE_FAILED_LOGINS', severity: 'HIGH', description: '' });
      await load();
    } catch {
      setError('Unable to create security incident.');
    }
  };

  const updateStatus = async (incident: SecurityAlertResponse, status: SecurityIncidentStatus) => {
    setError('');
    try {
      await securityIncidentService.updateStatus(incident.id, status);
      await load();
    } catch {
      setError('Unable to update incident status.');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Security Incidents</h1>
        <p className="text-sm text-slate-500">Track impossible travel, MFA failures, lockouts, and privilege events.</p>
      </div>
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      {canManage && (
        <Card>
          <CardHeader><CardTitle>Create Incident</CardTitle></CardHeader>
          <CardContent>
            <form onSubmit={createIncident} className="grid gap-3 lg:grid-cols-4">
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.eventType} onChange={(event) => setForm({ ...form, eventType: event.target.value as SecurityIncidentType })}>
                {incidentTypes.map((type) => <option key={type} value={type}>{type}</option>)}
              </select>
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.severity} onChange={(event) => setForm({ ...form, severity: event.target.value as RiskLevel })}>
                {severities.map((severity) => <option key={severity} value={severity}>{severity}</option>)}
              </select>
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Description" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} required />
              <Button type="submit" className="gap-2"><Plus className="h-4 w-4" /> Create</Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-500">
              <tr><th className="px-6 py-4">Incident</th><th className="px-6 py-4">Severity</th><th className="px-6 py-4">Tenant/User</th><th className="px-6 py-4">Status</th><th className="px-6 py-4 text-right">Created</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-500">Loading incidents...</td></tr>}
              {!loading && incidents.map((incident) => (
                <tr key={incident.id}>
                  <td className="px-6 py-4"><div className="font-medium text-slate-900">{incident.eventType}</div><div className="text-xs text-slate-500">{incident.description}</div></td>
                  <td className="px-6 py-4"><Badge variant={incident.severity === 'LOW' ? 'success' : incident.severity === 'MEDIUM' ? 'warning' : 'danger'}>{incident.severity}</Badge></td>
                  <td className="px-6 py-4">{incident.bankCode || '-'} / {incident.branchCode || '-'}<div className="text-xs text-slate-500">{incident.username || '-'}</div></td>
                  <td className="px-6 py-4">
                    {canManage ? (
                      <select className="rounded-md border border-slate-300 px-2 py-1 text-xs" value={incident.status} onChange={(event) => updateStatus(incident, event.target.value as SecurityIncidentStatus)}>
                        {statuses.map((status) => <option key={status} value={status}>{status}</option>)}
                      </select>
                    ) : (
                      <Badge variant={incident.status === 'RESOLVED' ? 'success' : 'warning'}>{incident.status}</Badge>
                    )}
                  </td>
                  <td className="px-6 py-4 text-right text-slate-500">{new Date(incident.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
