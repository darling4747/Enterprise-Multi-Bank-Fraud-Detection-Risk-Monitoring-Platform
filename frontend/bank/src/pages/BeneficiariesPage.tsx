import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Plus, TrendingUp } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { accountService } from '../services/accountService';
import { beneficiaryService } from '../services/beneficiaryService';
import { hasAnyRole } from '../utils/permissions';
import type { AccountResponse, BeneficiaryResponse } from '../types/api';

export default function BeneficiariesPage() {
  const canWrite = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'BRANCH_MANAGER', 'FRAUD_ANALYST']);
  const [beneficiaries, setBeneficiaries] = useState<BeneficiaryResponse[]>([]);
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [form, setForm] = useState({ accountNumber: '', beneficiaryAccount: '', trustScore: '0' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      const [beneficiaryData, accountData] = await Promise.all([beneficiaryService.list(), accountService.list()]);
      setBeneficiaries(beneficiaryData);
      setAccounts(accountData.filter((account) => account.status === 'ACTIVE'));
    } catch {
      setError('Unable to load beneficiaries.');
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

  const createBeneficiary = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    try {
      await beneficiaryService.create({
        accountNumber: form.accountNumber,
        beneficiaryAccount: form.beneficiaryAccount,
        trustScore: Number(form.trustScore),
      });
      setForm({ accountNumber: form.accountNumber, beneficiaryAccount: '', trustScore: '0' });
      await load();
    } catch {
      setError('Unable to add beneficiary. Check duplicate beneficiary and account scope.');
    }
  };

  const markUsed = async (id: number) => {
    setError('');
    try {
      await beneficiaryService.markUsed(id);
      await load();
    } catch {
      setError('Unable to update beneficiary trust.');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Beneficiaries</h1>
        <p className="text-sm text-slate-500">Maintain trusted beneficiary history used by the rule engine.</p>
      </div>
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      {canWrite && (
        <Card>
          <CardHeader><CardTitle>Add Beneficiary</CardTitle></CardHeader>
          <CardContent>
            <form onSubmit={createBeneficiary} className="grid gap-3 lg:grid-cols-4">
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.accountNumber} onChange={(event) => setForm({ ...form, accountNumber: event.target.value })} required>
                <option value="">Select source account</option>
                {accounts.map((account) => <option key={account.id} value={account.accountNumber}>{account.accountNumber} · {account.customerName}</option>)}
              </select>
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Beneficiary account" value={form.beneficiaryAccount} onChange={(event) => setForm({ ...form, beneficiaryAccount: event.target.value })} required />
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" type="number" min="0" max="100" value={form.trustScore} onChange={(event) => setForm({ ...form, trustScore: event.target.value })} />
              <Button type="submit" className="gap-2"><Plus className="h-4 w-4" /> Add</Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-500">
              <tr><th className="px-6 py-4">Source</th><th className="px-6 py-4">Beneficiary</th><th className="px-6 py-4">Trust</th><th className="px-6 py-4">Usage</th><th className="px-6 py-4 text-right">Actions</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-500">Loading beneficiaries...</td></tr>}
              {!loading && beneficiaries.map((beneficiary) => (
                <tr key={beneficiary.id}>
                  <td className="px-6 py-4 font-medium text-slate-900">{beneficiary.accountNumber}</td>
                  <td className="px-6 py-4">{beneficiary.beneficiaryAccount}</td>
                  <td className="px-6 py-4"><Badge variant={beneficiary.trustScore >= 50 ? 'success' : 'warning'}>{beneficiary.trustScore}/100</Badge></td>
                  <td className="px-6 py-4">{beneficiary.usageCount}</td>
                  <td className="px-6 py-4 text-right">{canWrite && <Button size="sm" variant="outline" className="gap-1" onClick={() => markUsed(beneficiary.id)}><TrendingUp className="h-3.5 w-3.5" /> Mark Used</Button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
