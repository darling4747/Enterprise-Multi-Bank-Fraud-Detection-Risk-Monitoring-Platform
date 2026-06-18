import { Card, CardHeader, CardTitle, CardContent } from '../common/Card';
import { Link } from 'react-router-dom';
import type { AlertResponse } from '../../types/api';

interface FraudAlertsProps {
  alerts?: AlertResponse[];
}

export function FraudAlerts({ alerts = [] }: FraudAlertsProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Recent High-Risk Alerts</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <div className="divide-y divide-slate-100">
          {alerts.length === 0 && (
            <div className="p-4 text-sm text-slate-500">No alerts generated yet.</div>
          )}
          {alerts.slice(0, 5).map((alert) => (
            <div key={alert.id} className="p-4 hover:bg-slate-50 transition-colors flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-900">{alert.title}</p>
                <p className="text-xs text-slate-500 mt-1">{alert.transactionReference}</p>
              </div>
              <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${
                alert.severity === 'CRITICAL' || alert.severity === 'HIGH'
                  ? 'bg-red-100 text-red-700'
                  : alert.severity === 'MEDIUM'
                    ? 'bg-amber-100 text-amber-700'
                    : 'bg-blue-100 text-blue-700'
              }`}>
                {alert.severity}
              </span>
            </div>
          ))}
        </div>
        <div className="p-4 border-t border-slate-100">
          <Link to="/dashboard/alerts" className="block text-sm font-medium text-blue-600 hover:text-blue-700 w-full text-center">
            View All Alerts &rarr;
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}
