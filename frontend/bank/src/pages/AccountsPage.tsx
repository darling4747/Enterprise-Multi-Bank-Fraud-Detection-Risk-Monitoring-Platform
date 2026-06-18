import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { CheckCircle2, Plus } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { accountService } from '../services/accountService';
import { customerService } from '../services/customerService';
import { hasAnyRole } from '../utils/permissions';
import type { AccountResponse, AccountStatus, AccountType, CustomerResponse } from '../types/api';

const accountTypes: AccountType[] = ['INDIVIDUAL', 'SMALL_BUSINESS', 'BUSINESS', 'CORPORATE', 'GOVERNMENT'];

export default function AccountsPage() {
  const canCreate = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'BRANCH_MANAGER']);
  const canApprove = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'BRANCH_MANAGER']);
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [form, setForm] = useState({ customerId: '', accountNumber: '', accountType: 'INDIVIDUAL' as AccountType, balance: '100000', currency: 'INR' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      const [accountData, customerData] = await Promise.all([accountService.list(), customerService.list()]);
      setAccounts(accountData);
      setCustomers(customerData.filter((customer) => customer.status === 'ACTIVE'));
    } catch {
      setError('Unable to load accounts.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const createAccount = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    try {
      await accountService.create({
        customerId: form.customerId,
        accountNumber: form.accountNumber,
        accountType: form.accountType,
        balance: Number(form.balance),
        currency: form.currency,
      });
      setForm({ customerId: form.customerId, accountNumber: '', accountType: 'INDIVIDUAL', balance: '100000', currency: 'INR' });
      await load();
    } catch {
      setError('Unable to create account. Check duplicate account number and customer scope.');
    }
  };

  const updateStatus = async (account: AccountResponse, status: AccountStatus) => {
    setError('');
    try {
      await accountService.updateStatus(account.id, status);
      await load();
    } catch {
      setError('Unable to update account status.');
    }
  };

  const approveOperation = async (account: AccountResponse) => {
    setError('');
    try {
      await accountService.approveBranchOperation(account.id);
      await load();
    } catch {
      setError('Unable to approve branch operation.');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Accounts</h1>
        <p className="text-sm text-slate-500">Customer accounts used as sender and receiver sources for transaction validation.</p>
      </div>
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      {canCreate && (
        <Card>
          <CardHeader><CardTitle>Create Account</CardTitle></CardHeader>
          <CardContent>
            <form onSubmit={createAccount} className="grid gap-3 lg:grid-cols-5">
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.customerId} onChange={(event) => setForm({ ...form, customerId: event.target.value })} required>
                <option value="">Select customer</option>
                {customers.map((customer) => <option key={customer.id} value={customer.customerId}>{customer.fullName} ({customer.customerId})</option>)}
              </select>
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Account number" value={form.accountNumber} onChange={(event) => setForm({ ...form, accountNumber: event.target.value })} required />
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.accountType} onChange={(event) => setForm({ ...form, accountType: event.target.value as AccountType })}>
                {accountTypes.map((type) => <option key={type} value={type}>{type}</option>)}
              </select>
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" type="number" min="0" step="0.01" value={form.balance} onChange={(event) => setForm({ ...form, balance: event.target.value })} required />
              <Button type="submit" className="gap-2"><Plus className="h-4 w-4" /> Create</Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-500">
              <tr><th className="px-6 py-4">Account</th><th className="px-6 py-4">Customer</th><th className="px-6 py-4">Balance</th><th className="px-6 py-4">Status</th><th className="px-6 py-4 text-right">Actions</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-500">Loading accounts...</td></tr>}
              {!loading && accounts.map((account) => (
                <tr key={account.id}>
                  <td className="px-6 py-4"><div className="font-medium text-slate-900">{account.accountNumber}</div><div className="text-xs text-slate-500">{account.accountType} · {account.bankCode}/{account.branchCode || '-'}</div></td>
                  <td className="px-6 py-4">{account.customerName}<div className="text-xs text-slate-500">{account.customerId}</div></td>
                  <td className="px-6 py-4 font-medium">{account.currency} {Number(account.balance).toLocaleString()}</td>
                  <td className="px-6 py-4"><Badge variant={account.status === 'ACTIVE' ? 'success' : account.status === 'FROZEN' ? 'warning' : 'danger'}>{account.status}</Badge></td>
                  <td className="px-6 py-4">
                    <div className="flex justify-end gap-2">
                      {canCreate && (
                        <select className="rounded-md border border-slate-300 px-2 py-1 text-xs" value={account.status} onChange={(event) => updateStatus(account, event.target.value as AccountStatus)}>
                          <option value="ACTIVE">ACTIVE</option><option value="FROZEN">FROZEN</option><option value="CLOSED">CLOSED</option>
                        </select>
                      )}
                      {canApprove && <Button size="sm" variant="outline" className="gap-1" onClick={() => approveOperation(account)}><CheckCircle2 className="h-3.5 w-3.5" /> Approve</Button>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
