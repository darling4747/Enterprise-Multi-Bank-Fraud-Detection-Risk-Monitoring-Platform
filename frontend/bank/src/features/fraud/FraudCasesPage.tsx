import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge } from '../../components/common/Badge';
import { Card } from '../../components/common/Card';
import { fraudService } from '../../services/fraudService';
import type { FraudCaseResponse } from '../../types/api';

const liveRefreshMs = 30000;

export default function FraudCasesPage() {
  const [cases, setCases] = useState<FraudCaseResponse[]>([]);
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
        const data = await fraudService.getFraudCases();
        if (active) {
          setCases(data);
          setLastUpdated(new Date());
        }
      } catch {
        if (active) {
          setError('Unable to load fraud cases.');
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

  const highRiskCount = useMemo(() => cases.filter((item) => item.riskLevel === 'HIGH' || item.riskLevel === 'CRITICAL').length, [cases]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Fraud Cases</h1>
        <p className="text-sm text-slate-500">Investigate transactions escalated by rules and the ML model.</p>
        <p className="mt-1 text-xs text-slate-500">
          Last updated {lastUpdated ? lastUpdated.toLocaleTimeString() : 'loading'} · refreshes every 30s
        </p>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="grid gap-4 md:grid-cols-3">
        <Summary label="Active Cases" value={cases.length} />
        <Summary label="High Risk" value={highRiskCount} />
        <Summary label="Open Review" value={cases.filter((item) => item.status === 'OPEN' || item.status === 'UNDER_REVIEW' || item.status === 'ESCALATED').length} />
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-500">
              <tr>
                <th className="px-6 py-4">Case</th>
                <th className="px-6 py-4">Transaction</th>
                <th className="px-6 py-4">Risk</th>
                <th className="px-6 py-4">Decision</th>
                <th className="px-6 py-4">Priority</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && <tr><td colSpan={7} className="px-6 py-8 text-center text-slate-500">Loading cases...</td></tr>}
              {!loading && cases.length === 0 && <tr><td colSpan={7} className="px-6 py-8 text-center text-slate-500">No fraud cases found.</td></tr>}
              {!loading && cases.map((fraudCase) => (
                <tr key={fraudCase.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 font-medium text-blue-600">
                    <Link to={`/dashboard/fraud-cases/${fraudCase.id}`}>CASE-{fraudCase.id}</Link>
                  </td>
                  <td className="px-6 py-4 text-slate-700">{fraudCase.transactionReference}</td>
                  <td className="px-6 py-4">
                    <span className="font-medium text-slate-900">{fraudCase.riskLevel}</span>
                    <span className="ml-2 text-slate-500">({fraudCase.riskScore})</span>
                  </td>
                  <td className="px-6 py-4">{fraudCase.decision}</td>
                  <td className="px-6 py-4"><Badge variant={fraudCase.priority === 'LOW' ? 'success' : fraudCase.priority === 'MEDIUM' ? 'warning' : 'danger'}>{fraudCase.priority}</Badge></td>
                  <td className="px-6 py-4"><Badge variant={fraudCase.status === 'CLOSED' || fraudCase.status === 'FALSE_POSITIVE' ? 'success' : fraudCase.status === 'CONFIRMED_FRAUD' ? 'danger' : 'warning'}>{fraudCase.status}</Badge></td>
                  <td className="px-6 py-4 text-right text-slate-500">{new Date(fraudCase.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}

function Summary({ label, value }: { label: string; value: number }) {
  return (
    <Card className="p-4">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-bold text-slate-900">{value}</p>
    </Card>
  );
}
