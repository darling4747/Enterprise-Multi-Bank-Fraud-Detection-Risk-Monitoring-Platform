import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Filter, Download } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { transactionService } from '../../services/transactionService';
import type { RiskLevel, TransactionStatus, TransactionSummaryResponse } from '../../types/api';
import { canDeleteTransactions, canWriteTransactions } from '../../utils/permissions';

const pageSize = 10;
const liveRefreshMs = 30000;
const transactionStatuses: TransactionStatus[] = ['PENDING', 'APPROVED', 'DECLINED', 'REVIEW', 'BLOCKED', 'COMPLETED'];
const riskLevels: RiskLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export default function TransactionList() {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<TransactionSummaryResponse[]>([]);
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | TransactionStatus>('ALL');
  const [riskFilter, setRiskFilter] = useState<'ALL' | RiskLevel>('ALL');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const canCreateOrReview = canWriteTransactions();
  const canDelete = canDeleteTransactions();
  const canUseRowActions = canCreateOrReview || canDelete;

  const loadTransactions = async (filters?: { status?: TransactionStatus; riskLevel?: RiskLevel }, showSpinner = true) => {
    if (showSpinner) {
      setLoading(true);
    }
    setError('');
    try {
      setTransactions(await transactionService.getTransactions(filters));
      setLastUpdated(new Date());
      if (showSpinner) {
        setPage(0);
      }
    } catch {
      setError('Unable to load transactions from the backend.');
    } finally {
      if (showSpinner) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    let active = true;
    const filters = {
      status: statusFilter === 'ALL' ? undefined : statusFilter,
      riskLevel: riskFilter === 'ALL' ? undefined : riskFilter,
    };

    const refresh = async (showSpinner = false) => {
      if (showSpinner) {
        setLoading(true);
      }
      setError('');
      try {
        const data = await transactionService.getTransactions(filters);
        if (active) {
          setTransactions(data);
          setLastUpdated(new Date());
          if (showSpinner) {
            setPage(0);
          }
        }
      } catch {
        if (active) {
          setError('Unable to load transactions from the backend.');
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
  }, [riskFilter, statusFilter]);

  const visibleTransactions = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) {
      return transactions;
    }
    return transactions.filter((transaction) =>
      transaction.reference.toLowerCase().includes(needle)
      || transaction.customerId.toLowerCase().includes(needle)
      || String(transaction.amount).includes(needle)
    );
  }, [query, transactions]);

  const totalPages = Math.max(1, Math.ceil(visibleTransactions.length / pageSize));
  const currentPage = Math.min(page, totalPages - 1);
  const pagedTransactions = visibleTransactions.slice(currentPage * pageSize, currentPage * pageSize + pageSize);

  const createSampleTransaction = async () => {
    if (!canCreateOrReview) {
      return;
    }
    setError('');
    try {
      await transactionService.createTransaction({
        customerId: `CUST-${Math.floor(Math.random() * 9000) + 1000}`,
        sourceAccount: `ACC-${Math.floor(Math.random() * 900000) + 100000}`,
        destinationAccount: `ACC-${Math.floor(Math.random() * 900000) + 100000}`,
        amount: Math.random() > 0.5 ? 45000 : 1200,
        currency: 'USD',
        channel: 'ONLINE',
        merchantCategory: Math.random() > 0.5 ? 'CRYPTO' : 'RETAIL',
        country: Math.random() > 0.5 ? 'NG' : 'US',
        ipAddress: '10.1.20.30',
        deviceId: Math.random() > 0.5 ? '' : 'trusted-device-web',
        accountType: Math.random() > 0.5 ? 'INDIVIDUAL' : 'CORPORATE',
        customerType: Math.random() > 0.5 ? 'NEW_CUSTOMER' : 'ENTERPRISE',
        beneficiaryTrusted: Math.random() > 0.5,
        knownDevice: Math.random() > 0.5,
        knownLocation: Math.random() > 0.5,
        transactionHour: Math.random() > 0.5 ? 11 : 1,
        dailyTransactionPattern: Math.random() > 0.5 ? 'NORMAL' : 'UNUSUAL',
        type: 'TRANSFER',
        step: 1,
        oldbalanceOrg: 1000000,
        newbalanceOrig: 0,
        oldbalanceDest: 0,
        newbalanceDest: 0,
      });
      await loadTransactions(undefined, false);
    } catch {
      setError('Unable to create a sample transaction.');
    }
  };

  const applyFilters = async () => {
    await loadTransactions({
      status: statusFilter === 'ALL' ? undefined : statusFilter,
      riskLevel: riskFilter === 'ALL' ? undefined : riskFilter,
    });
  };

  const exportTransactions = () => {
    const headers = ['Reference', 'Customer', 'Amount', 'Currency', 'Status', 'Risk Level', 'Risk Score', 'Created At'];
    const csvRows = visibleTransactions.map((transaction) => [
      transaction.reference,
      transaction.customerId,
      transaction.amount,
      transaction.currency,
      transaction.status,
      transaction.riskLevel,
      transaction.riskScore,
      transaction.createdAt,
    ]);
    const csv = [headers, ...csvRows]
      .map((row) => row.map((value) => `"${String(value).replaceAll('"', '""')}"`).join(','))
      .join('\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `transactions-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Transactions</h1>
          <p className="text-slate-500 text-sm mt-1">Monitor and investigate all transactions across the network.</p>
          <p className="mt-1 text-xs text-slate-500">
            Last updated {lastUpdated ? lastUpdated.toLocaleTimeString() : 'loading'} · refreshes every 30s
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="outline" className="gap-2" disabled={visibleTransactions.length === 0} onClick={exportTransactions}>
            <Download className="w-4 h-4" /> Export
          </Button>
          {canCreateOrReview && <Button variant="primary" onClick={createSampleTransaction}>Create Sample Transaction</Button>}
        </div>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <Card>
        <div className="p-4 border-b border-slate-100 flex flex-col sm:flex-row gap-4 items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input 
                type="text" 
                placeholder="Search by ID, Account, Amount..." 
                value={query}
                onChange={(event) => {
                  setQuery(event.target.value);
                  setPage(0);
                }}
                className="w-full pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-lg text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
              />
            </div>
            <Button variant="outline" className="px-3" onClick={applyFilters}>
              <Filter className="w-4 h-4 text-slate-500" />
            </Button>
          </div>
          
          <div className="flex items-center gap-4 text-sm w-full sm:w-auto overflow-x-auto pb-2 sm:pb-0">
            <div className="flex items-center gap-2 whitespace-nowrap">
              <span className="text-slate-500">Status:</span>
              <select
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value as 'ALL' | TransactionStatus)}
                className="font-medium text-slate-700 bg-white border border-slate-200 px-3 py-1.5 rounded-md outline-none focus:ring-2 focus:ring-blue-100"
              >
                <option value="ALL">All</option>
                {transactionStatuses.map((status) => <option key={status} value={status}>{status}</option>)}
              </select>
            </div>
            <div className="flex items-center gap-2 whitespace-nowrap">
              <span className="text-slate-500">Risk Score:</span>
              <select
                value={riskFilter}
                onChange={(event) => setRiskFilter(event.target.value as 'ALL' | RiskLevel)}
                className="font-medium text-slate-700 bg-white border border-slate-200 px-3 py-1.5 rounded-md outline-none focus:ring-2 focus:ring-blue-100"
              >
                <option value="ALL">Any</option>
                {riskLevels.map((level) => <option key={level} value={level}>{level}</option>)}
              </select>
            </div>
            <Button variant="outline" size="sm" onClick={applyFilters}>Apply</Button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 text-slate-500 font-medium border-b border-slate-200">
              <tr>
                <th className="px-6 py-4">Transaction ID</th>
                <th className="px-6 py-4">Date & Time</th>
                <th className="px-6 py-4">Account</th>
                <th className="px-6 py-4">Amount</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4">Risk Level</th>
                {canUseRowActions && <th className="px-6 py-4 text-right">Actions</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && (
                <tr>
                  <td colSpan={canUseRowActions ? 7 : 6} className="px-6 py-8 text-center text-slate-500">Loading transactions...</td>
                </tr>
              )}
              {!loading && visibleTransactions.length === 0 && (
                <tr>
                  <td colSpan={canUseRowActions ? 7 : 6} className="px-6 py-8 text-center text-slate-500">No transactions found.</td>
                </tr>
              )}
              {!loading && pagedTransactions.map((trx) => (
                <tr key={trx.id} className="hover:bg-slate-50 transition-colors group cursor-pointer" onClick={() => navigate(`/dashboard/transactions/${trx.id}`)}>
                  <td className="px-6 py-4 font-medium text-blue-600">{trx.reference}</td>
                  <td className="px-6 py-4 text-slate-600">{new Date(trx.createdAt).toLocaleString()}</td>
                  <td className="px-6 py-4 text-slate-600">{trx.customerId}</td>
                  <td className="px-6 py-4 font-medium text-slate-900">
                    {new Intl.NumberFormat('en-US', { style: 'currency', currency: trx.currency }).format(trx.amount)}
                  </td>
                  <td className="px-6 py-4">
                    <Badge variant={
                      trx.status === 'APPROVED' || trx.status === 'COMPLETED' ? 'success' :
                      trx.status === 'PENDING' || trx.status === 'REVIEW' ? 'warning' :
                      trx.status === 'BLOCKED' ? 'danger' : 'default'
                    }>
                      {trx.status}
                    </Badge>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center gap-1.5 font-medium ${
                      trx.riskLevel === 'LOW' ? 'text-emerald-600' :
                      trx.riskLevel === 'MEDIUM' ? 'text-amber-600' :
                      'text-red-600'
                    }`}>
                      <span className={`w-2 h-2 rounded-full ${
                        trx.riskLevel === 'LOW' ? 'bg-emerald-500' :
                        trx.riskLevel === 'MEDIUM' ? 'bg-amber-500' :
                        'bg-red-500'
                      }`}></span>
                      {trx.riskLevel} ({trx.riskScore})
                    </span>
                  </td>
                  {canUseRowActions && (
                    <td className="px-6 py-4 text-right">
                      <div className="flex justify-end gap-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        {canCreateOrReview && trx.status !== 'REVIEW' && (
                          <button
                            className="text-blue-600 hover:text-blue-700 font-medium"
                            onClick={(event) => {
                              event.stopPropagation();
                              transactionService.updateStatus(trx.id, 'REVIEW').then(() => applyFilters()).catch(() => setError('Unable to update transaction status.'));
                            }}
                          >
                            Review
                          </button>
                        )}
                        {canDelete && (
                          <button
                            className="text-red-600 hover:text-red-700 font-medium"
                            onClick={(event) => {
                              event.stopPropagation();
                              transactionService.deleteTransaction(trx.id).then(() => applyFilters()).catch(() => setError('Unable to delete transaction.'));
                            }}
                          >
                            Delete
                          </button>
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        
        <div className="px-6 py-4 border-t border-slate-100 flex items-center justify-between bg-slate-50/50">
          <span className="text-sm text-slate-500">Showing {pagedTransactions.length} of {visibleTransactions.length} results</span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={currentPage === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Previous</Button>
            <Button variant="outline" size="sm" disabled={currentPage >= totalPages - 1} onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))}>Next</Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
