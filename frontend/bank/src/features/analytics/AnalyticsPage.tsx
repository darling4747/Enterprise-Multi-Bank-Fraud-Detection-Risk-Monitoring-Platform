import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { dashboardService } from '../../services/dashboardService';
import type { DashboardChartResponse } from '../../types/api';

export default function AnalyticsPage() {
  const [charts, setCharts] = useState<DashboardChartResponse | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    dashboardService.getCharts()
      .then((data) => {
        if (active) {
          setCharts(data);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load analytics data.');
        }
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Analytics</h1>
        <p className="text-sm text-slate-500">Risk, status, and alert distributions from live backend data.</p>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="grid gap-6 lg:grid-cols-3">
        <Distribution title="Risk Distribution" data={charts?.riskDistribution} />
        <Distribution title="Transaction Status" data={charts?.transactionStatusDistribution} />
        <Distribution title="Alert Status" data={charts?.alertStatusDistribution} />
      </div>
    </div>
  );
}

function Distribution({ title, data }: { title: string; data?: Record<string, number> }) {
  const rows = Object.entries(data ?? {});

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {rows.length === 0 && <p className="text-sm text-slate-500">No data available.</p>}
        <div className="space-y-3">
          {rows.map(([label, value]) => (
            <div key={label} className="flex items-center justify-between">
              <span className="text-sm text-slate-600">{label}</span>
              <span className="font-semibold text-slate-900">{value}</span>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
