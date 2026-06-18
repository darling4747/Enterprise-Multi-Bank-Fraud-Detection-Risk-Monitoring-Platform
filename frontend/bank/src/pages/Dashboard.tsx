import { useEffect, useMemo, useState } from 'react';
import { 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  LineChart,
  Line
} from 'recharts';
import { ShieldAlert, Activity, CreditCard, Ban } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { StatsCard } from '../components/dashboard/StatsCard';
import { FraudAlerts } from '../components/dashboard/FraudAlerts';
import { RecentTransactions } from '../components/dashboard/RecentTransactions';
import { RiskScoreCard } from '../components/dashboard/RiskScoreCard';
import { dashboardService } from '../services/dashboardService';
import { transactionService } from '../services/transactionService';
import { fraudService } from '../services/fraudService';
import type { AlertResponse, DashboardChartResponse, DashboardStatsResponse, TransactionSummaryResponse } from '../types/api';

const liveRefreshMs = 30000;

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStatsResponse | null>(null);
  const [charts, setCharts] = useState<DashboardChartResponse | null>(null);
  const [transactions, setTransactions] = useState<TransactionSummaryResponse[]>([]);
  const [alerts, setAlerts] = useState<AlertResponse[]>([]);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  useEffect(() => {
    let active = true;

    const load = async () => {
      try {
        const [nextStats, nextCharts, nextTransactions, nextAlerts] = await Promise.all([
          dashboardService.getStats(),
          dashboardService.getCharts(),
          transactionService.getTransactions(),
          fraudService.getAlerts(),
        ]);
        if (active) {
          setStats(nextStats);
          setCharts(nextCharts);
          setTransactions(nextTransactions);
          setAlerts(nextAlerts);
          setError('');
          setLastUpdated(new Date());
        }
      } catch {
        if (active) {
          setError('Unable to load dashboard data. Make sure the Spring Boot backend is running on port 8080.');
        }
      }
    };

    load();
    const timer = window.setInterval(load, liveRefreshMs);

    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  const trendData = useMemo(() => {
    if (!charts) {
      return [];
    }
    return Object.entries(charts.transactionStatusDistribution).map(([name, count]) => ({
      name,
      transactions: count,
      alerts: charts.alertStatusDistribution.OPEN ?? 0,
    }));
  }, [charts]);

  const averageRisk = transactions.length === 0
    ? 0
    : Math.round(transactions.reduce((sum, transaction) => sum + transaction.riskScore, 0) / transactions.length);

  const approvalRate = stats && stats.totalTransactions > 0
    ? `${Math.round(((stats.totalTransactions - stats.blockedTransactions - stats.reviewTransactions) / stats.totalTransactions) * 100)}%`
    : '0%';

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Dashboard Overview</h1>
          <p className="text-slate-500 text-sm mt-1">Real-time fraud monitoring and transaction analytics.</p>
        </div>
        <p className="text-xs text-slate-500">
          Last updated {lastUpdated ? lastUpdated.toLocaleTimeString() : 'loading'} · refreshes every 30s
        </p>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatsCard 
          title="Total Transactions" 
          value={(stats?.totalTransactions ?? 0).toLocaleString()} 
          icon={Activity} 
          trend={12.5} 
          trendLabel="vs last week"
          colorClass="bg-blue-100 text-blue-600"
        />
        <StatsCard 
          title="Active Alerts" 
          value={(stats?.openAlerts ?? 0).toLocaleString()} 
          icon={ShieldAlert} 
          trend={-4.2} 
          trendLabel="vs last week"
          colorClass="bg-amber-100 text-amber-600"
        />
        <StatsCard 
          title="Blocked Transactions" 
          value={(stats?.blockedTransactions ?? 0).toLocaleString()} 
          icon={Ban} 
          trend={8.1} 
          trendLabel="vs last week"
          colorClass="bg-red-100 text-red-600"
        />
        <StatsCard 
          title="Approval Rate" 
          value={approvalRate} 
          icon={CreditCard} 
          trend={0.1} 
          trendLabel="vs last week"
          colorClass="bg-emerald-100 text-emerald-600"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 min-w-0">
        <Card className="col-span-1 lg:col-span-2 min-w-0">
          <CardHeader>
            <CardTitle>Transaction vs Alerts Trend</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="w-full min-w-0 mt-4">
              <ResponsiveContainer width="100%" height={320} minWidth={0}>
                <LineChart data={trendData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: '#64748b'}} />
                  <YAxis yAxisId="left" axisLine={false} tickLine={false} tick={{fill: '#64748b'}} />
                  <YAxis yAxisId="right" orientation="right" axisLine={false} tickLine={false} tick={{fill: '#64748b'}} />
                  <Tooltip 
                    contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                  />
                  <Line yAxisId="left" type="monotone" dataKey="transactions" stroke="#3b82f6" strokeWidth={3} dot={false} activeDot={{ r: 8 }} />
                  <Line yAxisId="right" type="monotone" dataKey="alerts" stroke="#ef4444" strokeWidth={3} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <div className="space-y-6">
          <RiskScoreCard score={averageRisk} />
          <FraudAlerts alerts={alerts} />
        </div>
      </div>
      
      <div className="grid grid-cols-1 gap-6">
        <RecentTransactions transactions={transactions} />
      </div>
    </div>
  );
}
