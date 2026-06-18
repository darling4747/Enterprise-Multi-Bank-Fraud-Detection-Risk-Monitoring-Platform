import { Card, CardHeader, CardTitle, CardContent } from '../common/Card';
import { Badge } from '../common/Badge';
import { Link } from 'react-router-dom';
import type { TransactionSummaryResponse } from '../../types/api';

interface RecentTransactionsProps {
  transactions?: TransactionSummaryResponse[];
}

export function RecentTransactions({ transactions = [] }: RecentTransactionsProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Recent Transactions</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <div className="divide-y divide-slate-100">
          {transactions.length === 0 && (
            <div className="p-4 text-sm text-slate-500">No transactions yet.</div>
          )}
          {transactions.slice(0, 6).map((trx) => (
            <Link key={trx.id} to={`/dashboard/transactions/${trx.id}`} className="p-4 hover:bg-slate-50 transition-colors flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-900">{trx.reference}</p>
                <p className="text-xs text-slate-500 mt-1">
                  {new Intl.NumberFormat('en-US', { style: 'currency', currency: trx.currency }).format(trx.amount)}
                </p>
              </div>
              <Badge variant={
                trx.status === 'APPROVED' || trx.status === 'COMPLETED' ? 'success' :
                trx.status === 'PENDING' || trx.status === 'REVIEW' ? 'warning' :
                trx.status === 'BLOCKED' ? 'danger' : 'default'
              }>
                {trx.status}
              </Badge>
            </Link>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
