import { useEffect, useState } from 'react';
import { Send } from 'lucide-react';
import { Button } from '../../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { dashboardService } from '../../services/dashboardService';
import { reportService } from '../../services/reportService';
import type { DashboardStatsResponse } from '../../types/api';
import { hasAnyRole } from '../../utils/permissions';

export default function ReportsPage() {
  const [stats, setStats] = useState<DashboardStatsResponse | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);
  const canSendReports = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN']);

  useEffect(() => {
    let active = true;

    dashboardService.getStats()
      .then((data) => {
        if (active) {
          setStats(data);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load report data.');
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const reviewLoad = (stats?.reviewTransactions ?? 0) + (stats?.openAlerts ?? 0);

  const sendDailySummaryNow = async () => {
    setError('');
    setMessage('');
    setSending(true);
    try {
      const response = await reportService.sendDailySummaryNow();
      setMessage(`Daily summary processed for ${response.recipientCount} enabled superadmin recipient(s).`);
    } catch {
      setError('Unable to send daily summary report.');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Reports</h1>
          <p className="text-sm text-slate-500">Operational fraud monitoring summary.</p>
        </div>
        {canSendReports && (
          <Button variant="outline" className="gap-2" disabled={sending} onClick={sendDailySummaryNow}>
            <Send className="h-4 w-4" /> {sending ? 'Sending...' : 'Send Daily Summary'}
          </Button>
        )}
      </div>

      {message && <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div>}
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
        <ReportCard title="Total Transactions" value={stats?.totalTransactions ?? 0} />
        <ReportCard title="Blocked Transactions" value={stats?.blockedTransactions ?? 0} />
        <ReportCard title="Active Fraud Cases" value={stats?.activeFraudCases ?? 0} />
        <ReportCard title="Review Workload" value={reviewLoad} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Last 24 Hours</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-slate-500">Transaction volume</p>
          <p className="mt-1 text-3xl font-bold text-slate-900">
            {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(stats?.last24hVolume ?? 0)}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

function ReportCard({ title, value }: { title: string; value: number }) {
  return (
    <Card>
      <CardContent>
        <p className="text-sm font-medium text-slate-500">{title}</p>
        <p className="mt-2 text-2xl font-bold text-slate-900">{value.toLocaleString()}</p>
      </CardContent>
    </Card>
  );
}
