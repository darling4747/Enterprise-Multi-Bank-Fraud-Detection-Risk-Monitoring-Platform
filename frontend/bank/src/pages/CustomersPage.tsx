import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { Plus } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { bankService } from '../services/bankService';
import { branchService } from '../services/branchService';
import { customerService } from '../services/customerService';
import { hasAnyRole } from '../utils/permissions';
import type { BankResponse, BranchResponse, CustomerResponse, CustomerStatus, CustomerType } from '../types/api';

const customerTypes: CustomerType[] = ['RETAIL', 'PREMIUM', 'HIGH_NET_WORTH', 'SME', 'ENTERPRISE', 'GOVERNMENT', 'NEW_CUSTOMER'];

export default function CustomersPage() {
  const isPlatform = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN']);
  const canSelectBranch = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN']);
  const canCreate = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'BRANCH_MANAGER']);
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [banks, setBanks] = useState<BankResponse[]>([]);
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [form, setForm] = useState({ customerId: '', fullName: '', email: '', phone: '', bankId: '', branchId: '', customerType: 'RETAIL' as CustomerType });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      const [customerData, bankData, branchData] = await Promise.all([
        customerService.list(),
        isPlatform ? bankService.listBanks() : Promise.resolve([] as BankResponse[]),
        canSelectBranch ? branchService.listBranches() : Promise.resolve([] as BranchResponse[]),
      ]);
      setCustomers(customerData);
      setBanks(bankData);
      setBranches(branchData);
    } catch {
      setError('Unable to load customers.');
    } finally {
      setLoading(false);
    }
  }, [canSelectBranch, isPlatform]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const visibleBranches = useMemo(() => {
    const bankId = Number(form.bankId);
    return branches.filter((branch) => !bankId || branch.bankId === bankId);
  }, [branches, form.bankId]);

  const createCustomer = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    try {
      await customerService.create({
        customerId: form.customerId,
        fullName: form.fullName,
        email: form.email || undefined,
        phone: form.phone || undefined,
        bankId: form.bankId ? Number(form.bankId) : undefined,
        branchId: form.branchId ? Number(form.branchId) : undefined,
        customerType: form.customerType,
      });
      setForm({ customerId: '', fullName: '', email: '', phone: '', bankId: form.bankId, branchId: '', customerType: 'RETAIL' });
      await load();
    } catch {
      setError('Unable to create customer. Check bank/branch scope and duplicate customer ID.');
    }
  };

  const updateStatus = async (customer: CustomerResponse, status: CustomerStatus) => {
    setError('');
    try {
      await customerService.updateStatus(customer.id, status);
      await load();
    } catch {
      setError('Unable to update customer status.');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Customers</h1>
        <p className="text-sm text-slate-500">Create and monitor bank customers linked to fraud-scored accounts.</p>
      </div>
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      {canCreate && (
        <Card>
          <CardHeader><CardTitle>Create Customer</CardTitle></CardHeader>
          <CardContent>
            <form onSubmit={createCustomer} className="grid gap-3 lg:grid-cols-4">
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Customer ID" value={form.customerId} onChange={(event) => setForm({ ...form, customerId: event.target.value })} required />
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Full name" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} required />
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Email" type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Phone" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
              {isPlatform && (
                <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.bankId} onChange={(event) => setForm({ ...form, bankId: event.target.value, branchId: '' })} required>
                  <option value="">Select bank</option>
                  {banks.map((bank) => <option key={bank.id} value={bank.id}>{bank.name} ({bank.code})</option>)}
                </select>
              )}
              {canSelectBranch && (
                <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.branchId} onChange={(event) => setForm({ ...form, branchId: event.target.value })}>
                  <option value="">No branch</option>
                  {visibleBranches.map((branch) => <option key={branch.id} value={branch.id}>{branch.name} ({branch.code})</option>)}
                </select>
              )}
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.customerType} onChange={(event) => setForm({ ...form, customerType: event.target.value as CustomerType })}>
                {customerTypes.map((type) => <option key={type} value={type}>{type}</option>)}
              </select>
              <Button type="submit" className="gap-2"><Plus className="h-4 w-4" /> Create</Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-500">
              <tr><th className="px-6 py-4">Customer</th><th className="px-6 py-4">Tenant</th><th className="px-6 py-4">Type</th><th className="px-6 py-4">Status</th><th className="px-6 py-4 text-right">Actions</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-500">Loading customers...</td></tr>}
              {!loading && customers.map((customer) => (
                <tr key={customer.id}>
                  <td className="px-6 py-4"><div className="font-medium text-slate-900">{customer.fullName}</div><div className="text-xs text-slate-500">{customer.customerId} · {customer.email || '-'}</div></td>
                  <td className="px-6 py-4">{customer.bankCode}<div className="text-xs text-slate-500">{customer.branchCode || '-'}</div></td>
                  <td className="px-6 py-4">{customer.customerType}</td>
                  <td className="px-6 py-4"><Badge variant={customer.status === 'ACTIVE' ? 'success' : customer.status === 'SUSPENDED' ? 'danger' : 'default'}>{customer.status}</Badge></td>
                  <td className="px-6 py-4 text-right">
                    {canCreate && (
                      <select className="rounded-md border border-slate-300 px-2 py-1 text-xs" value={customer.status} onChange={(event) => updateStatus(customer, event.target.value as CustomerStatus)}>
                        <option value="ACTIVE">ACTIVE</option><option value="INACTIVE">INACTIVE</option><option value="SUSPENDED">SUSPENDED</option>
                      </select>
                    )}
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
