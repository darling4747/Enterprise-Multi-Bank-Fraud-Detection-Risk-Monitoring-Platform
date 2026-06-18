import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { transactionService } from '../../services/transactionService';
import type { TransactionResponse } from '../../types/api';

export default function TransactionDetails() {
  const { id } = useParams();
  const [transaction, setTransaction] = useState<TransactionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) {
      return;
    }

    let active = true;
    transactionService.getTransactionById(id)
      .then((data) => {
        if (active) {
          setTransaction(data);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load transaction details.');
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

  if (!id) {
    return (
      <div className="space-y-4">
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          Missing transaction id.
        </div>
        <Link to="/dashboard/transactions" className="text-sm font-medium text-blue-600 hover:text-blue-700">
          Back to transactions
        </Link>
      </div>
    );
  }

  if (loading) {
    return <div className="text-sm text-slate-500">Loading transaction...</div>;
  }

  if (error || !transaction) {
    return (
      <div className="space-y-4">
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error || 'Transaction not found.'}
        </div>
        <Link to="/dashboard/transactions" className="text-sm font-medium text-blue-600 hover:text-blue-700">
          Back to transactions
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{transaction.reference}</h1>
          <p className="text-sm text-slate-500">Transaction risk and payment details.</p>
        </div>
        <Link to="/dashboard/transactions" className="text-sm font-medium text-blue-600 hover:text-blue-700">
          Back to transactions
        </Link>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Risk Assessment</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-4">
            <Detail label="Status" value={transaction.status} />
            <Detail label="Risk Level" value={<Badge variant={transaction.riskLevel === 'LOW' ? 'success' : transaction.riskLevel === 'MEDIUM' ? 'warning' : 'danger'}>{transaction.riskLevel}</Badge>} />
            <Detail label="Risk Score" value={transaction.riskScore} />
            <Detail label="Decision" value={transaction.fraudDecision} />
          </div>
          <p className="mt-4 text-sm text-slate-600">{transaction.riskSummary || 'No risk summary available.'}</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Transaction Details</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-3">
            <Detail label="Customer" value={transaction.customerId} />
            <Detail label="Amount" value={new Intl.NumberFormat('en-US', { style: 'currency', currency: transaction.currency }).format(transaction.amount)} />
            <Detail label="Channel" value={transaction.channel} />
            <Detail label="Source" value={transaction.sourceAccount} />
            <Detail label="Destination" value={transaction.destinationAccount} />
            <Detail label="Merchant" value={transaction.merchantCategory || 'N/A'} />
            <Detail label="Country" value={transaction.country || 'N/A'} />
            <Detail label="IP Address" value={transaction.ipAddress || 'N/A'} />
            <Detail label="Device" value={transaction.deviceId || 'Unknown'} />
            <Detail label="Account Type" value={transaction.accountType} />
            <Detail label="Customer Type" value={transaction.customerType} />
            <Detail label="Trusted Beneficiary" value={transaction.beneficiaryTrusted ? 'Yes' : 'No'} />
            <Detail label="Known Device" value={transaction.knownDevice ? 'Yes' : 'No'} />
            <Detail label="Known Location" value={transaction.knownLocation ? 'Yes' : 'No'} />
            <Detail label="Transaction Hour" value={typeof transaction.transactionHour === 'number' ? `${transaction.transactionHour}:00` : 'N/A'} />
            <Detail label="Daily Pattern" value={transaction.dailyTransactionPattern} />
          </div>
        </CardContent>
      </Card>
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
