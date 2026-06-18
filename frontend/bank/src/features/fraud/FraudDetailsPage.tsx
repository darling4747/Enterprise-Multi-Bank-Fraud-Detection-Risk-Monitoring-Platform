import { useEffect, useState, type FormEvent } from 'react';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { fraudService } from '../../services/fraudService';
import type { CasePriority, FraudCaseResponse, FraudCaseStatus } from '../../types/api';
import { canManageAlerts } from '../../utils/permissions';

const investigationStatuses: FraudCaseStatus[] = ['OPEN', 'UNDER_REVIEW', 'ESCALATED', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE', 'CLOSED'];
const priorities: CasePriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export default function FraudDetailsPage() {
  const { id } = useParams();
  const [fraudCase, setFraudCase] = useState<FraudCaseResponse | null>(null);
  const [form, setForm] = useState({ status: 'UNDER_REVIEW' as FraudCaseStatus, investigationNotes: '', assignedToUserId: '', priority: 'MEDIUM' as CasePriority });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const canInvestigate = canManageAlerts();

  useEffect(() => {
    let active = true;

    fraudService.getFraudCase(id ?? '')
      .then((data) => {
        if (!active) {
          return;
        }
        setFraudCase(data);
        setForm({
          status: data.status === 'OPEN' ? 'UNDER_REVIEW' : data.status,
          investigationNotes: data.investigationNotes ?? '',
          assignedToUserId: data.assignedToUserId ? String(data.assignedToUserId) : '',
          priority: data.priority,
        });
      })
      .catch(() => {
        if (active) {
          setError('Unable to load fraud case.');
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [id]);

  const submitInvestigation = async (event: FormEvent) => {
    event.preventDefault();
    if (!id) {
      return;
    }
    setSaving(true);
    setError('');
    try {
      const updated = await fraudService.submitInvestigation(id, {
        status: form.status,
        investigationNotes: form.investigationNotes.trim() || undefined,
        assignedToUserId: form.assignedToUserId ? Number(form.assignedToUserId) : undefined,
        priority: form.priority,
      });
      setFraudCase(updated);
      setForm({
        status: updated.status,
        investigationNotes: updated.investigationNotes ?? '',
        assignedToUserId: updated.assignedToUserId ? String(updated.assignedToUserId) : '',
        priority: updated.priority,
      });
    } catch {
      setError('Unable to submit investigation.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-500">Loading fraud case...</div>;
  }

  if (error || !fraudCase) {
    return (
      <div className="space-y-4">
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
        <Link to="/dashboard/fraud-cases" className="text-sm font-medium text-blue-600 hover:text-blue-700">Back to fraud cases</Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">CASE-{fraudCase.id}</h1>
          <p className="text-sm text-slate-500">{fraudCase.transactionReference}</p>
        </div>
        <Link to="/dashboard/fraud-cases" className="text-sm font-medium text-blue-600 hover:text-blue-700">Back to fraud cases</Link>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Case Summary</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-4">
            <Detail label="Risk Level" value={<Badge variant={fraudCase.riskLevel === 'LOW' ? 'success' : fraudCase.riskLevel === 'MEDIUM' ? 'warning' : 'danger'}>{fraudCase.riskLevel}</Badge>} />
            <Detail label="Risk Score" value={fraudCase.riskScore} />
            <Detail label="Decision" value={fraudCase.decision} />
            <Detail label="Priority" value={fraudCase.priority} />
            <Detail label="Status" value={fraudCase.status} />
          </div>
          <div className="mt-4 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
            <p className="font-medium text-slate-900">Assignment</p>
            <p className="mt-1">
              Assigned to {fraudCase.assignedToUsername ?? 'Unassigned'}
              {fraudCase.assignedAt ? ` on ${new Date(fraudCase.assignedAt).toLocaleString()}` : ''}
            </p>
            {fraudCase.assignedByUsername && <p className="mt-1 text-xs text-slate-500">Assigned by {fraudCase.assignedByUsername}</p>}
          </div>
          <p className="mt-4 text-sm text-slate-600">{fraudCase.reason}</p>
          {fraudCase.investigationNotes && (
            <div className="mt-4 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
              <p className="font-medium text-slate-900">Investigation Notes</p>
              <p className="mt-1 whitespace-pre-wrap">{fraudCase.investigationNotes}</p>
              <p className="mt-2 text-xs text-slate-500">
                Reviewed by {fraudCase.reviewedBy ?? 'analyst'} {fraudCase.reviewedAt ? `on ${new Date(fraudCase.reviewedAt).toLocaleString()}` : ''}
              </p>
            </div>
          )}
        </CardContent>
      </Card>

      {canInvestigate && (
        <Card>
          <CardHeader>
            <CardTitle>Submit Investigation</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={submitInvestigation} className="space-y-3">
              <div className="grid gap-3 md:grid-cols-3">
                <select
                  value={form.status}
                  onChange={(event) => setForm((current) => ({ ...current, status: event.target.value as FraudCaseStatus }))}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                >
                  {investigationStatuses.map((status) => <option key={status} value={status}>{status}</option>)}
                </select>
                <select
                  value={form.priority}
                  onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value as CasePriority }))}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                >
                  {priorities.map((priority) => <option key={priority} value={priority}>{priority}</option>)}
                </select>
                <input
                  value={form.assignedToUserId}
                  onChange={(event) => setForm((current) => ({ ...current, assignedToUserId: event.target.value }))}
                  type="number"
                  min="1"
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  placeholder="Assign to user ID"
                />
              </div>
              <textarea
                value={form.investigationNotes}
                onChange={(event) => setForm((current) => ({ ...current, investigationNotes: event.target.value }))}
                rows={5}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Investigation notes, evidence, customer contact summary, analyst decision"
              />
              <Button type="submit" disabled={saving}>{saving ? 'Submitting...' : 'Submit Investigation'}</Button>
            </form>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function Detail({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <div className="mt-1 text-sm font-semibold text-slate-900">{value}</div>
    </div>
  );
}
