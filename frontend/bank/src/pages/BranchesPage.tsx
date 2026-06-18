import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { GitBranch, Plus } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { bankService } from '../services/bankService';
import { branchService } from '../services/branchService';
import type { BankResponse, BranchResponse } from '../types/api';
import { canManageBranches, hasAnyRole } from '../utils/permissions';

export default function BranchesPage() {
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [banks, setBanks] = useState<BankResponse[]>([]);
  const [form, setForm] = useState({
    bankId: '',
    code: '',
    name: '',
    ifscCode: '',
    city: '',
    state: '',
    address: '',
    managerName: '',
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const canManage = canManageBranches();
  const isPlatformAdmin = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN']);
  const activeBanks = useMemo(() => banks.filter((bank) => bank.status === 'ACTIVE'), [banks]);

  const loadBranches = async () => {
    setLoading(true);
    setError('');
    try {
      setBranches(await branchService.listBranches());
    } catch {
      setError('Unable to load branches.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;

    Promise.all([
      branchService.listBranches(),
      isPlatformAdmin ? bankService.listBanks() : Promise.resolve([] as BankResponse[]),
    ])
      .then(([branchData, bankData]) => {
        if (active) {
          setBranches(branchData);
          setBanks(bankData);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load branches.');
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
  }, [isPlatformAdmin]);

  const createBranch = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      await branchService.createBranch({
        bankId: isPlatformAdmin && form.bankId ? Number(form.bankId) : undefined,
        code: form.code.trim(),
        name: form.name.trim(),
        ifscCode: form.ifscCode.trim() || undefined,
        city: form.city.trim() || undefined,
        state: form.state.trim() || undefined,
        address: form.address.trim() || undefined,
        managerName: form.managerName.trim() || undefined,
      });
      setForm({ bankId: '', code: '', name: '', ifscCode: '', city: '', state: '', address: '', managerName: '' });
      await loadBranches();
    } catch {
      setError('Unable to create branch.');
    } finally {
      setSaving(false);
    }
  };

  const disableBranch = async (id: number) => {
    setError('');
    try {
      await branchService.disableBranch(id);
      await loadBranches();
    } catch {
      setError('Unable to disable branch.');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Branches</h1>
        <p className="text-sm text-slate-500">Manage branches within a bank tenant.</p>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      {canManage && (
        <Card>
          <CardHeader>
            <CardTitle>Create Branch</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={createBranch} className="grid gap-3 lg:grid-cols-4">
              {isPlatformAdmin && (
                <select
                  value={form.bankId}
                  onChange={(event) => setForm((current) => ({ ...current, bankId: event.target.value }))}
                  className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  required
                >
                  <option value="">Select bank</option>
                  {activeBanks.map((bank) => <option key={bank.id} value={bank.id}>{bank.name} ({bank.code})</option>)}
                </select>
              )}
              <input
                value={form.code}
                onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Branch code"
                required
              />
              <input
                value={form.name}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Branch name"
                required
              />
              <input
                value={form.ifscCode}
                onChange={(event) => setForm((current) => ({ ...current, ifscCode: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="IFSC code"
              />
              <input
                value={form.city}
                onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="City"
              />
              <input
                value={form.state}
                onChange={(event) => setForm((current) => ({ ...current, state: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="State"
              />
              <input
                value={form.managerName}
                onChange={(event) => setForm((current) => ({ ...current, managerName: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Manager"
              />
              <input
                value={form.address}
                onChange={(event) => setForm((current) => ({ ...current, address: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 lg:col-span-2"
                placeholder="Address"
              />
              <Button type="submit" disabled={saving} className="gap-2">
                <Plus className="h-4 w-4" /> Create
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-500">
              <tr>
                <th className="px-6 py-4">Branch</th>
                <th className="px-6 py-4">Bank</th>
                <th className="px-6 py-4">Location</th>
                <th className="px-6 py-4">Manager</th>
                <th className="px-6 py-4">Status</th>
                {canManage && <th className="px-6 py-4 text-right">Action</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && <tr><td colSpan={canManage ? 6 : 5} className="px-6 py-8 text-center text-slate-500">Loading branches...</td></tr>}
              {!loading && branches.length === 0 && <tr><td colSpan={canManage ? 6 : 5} className="px-6 py-8 text-center text-slate-500">No branches found.</td></tr>}
              {!loading && branches.map((branch) => (
                <tr key={branch.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2 font-medium text-slate-900">
                      <GitBranch className="h-4 w-4 text-slate-400" /> {branch.name}
                    </div>
                    <div className="mt-1 text-xs text-slate-500">{branch.code}{branch.ifscCode ? ` · ${branch.ifscCode}` : ''}</div>
                  </td>
                  <td className="px-6 py-4 text-slate-600">{branch.bankCode} #{branch.bankId}</td>
                  <td className="px-6 py-4 text-slate-600">
                    <div>{[branch.city, branch.state].filter(Boolean).join(', ') || '-'}</div>
                    <div className="mt-1 max-w-xs text-xs text-slate-500">{branch.address || '-'}</div>
                  </td>
                  <td className="px-6 py-4 text-slate-600">{branch.managerName || '-'}</td>
                  <td className="px-6 py-4"><Badge variant={branch.status === 'ACTIVE' ? 'success' : 'default'}>{branch.status}</Badge></td>
                  {canManage && (
                    <td className="px-6 py-4 text-right">
                      <Button variant="outline" size="sm" disabled={branch.status !== 'ACTIVE'} onClick={() => disableBranch(branch.id)}>Disable</Button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
