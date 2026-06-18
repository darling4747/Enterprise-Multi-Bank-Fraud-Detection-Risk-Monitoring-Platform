import { useEffect, useState, type FormEvent } from 'react';
import { Building2, Plus } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { bankService } from '../services/bankService';
import type { BankResponse } from '../types/api';
import { canManageBanks } from '../utils/permissions';

const emptyBankForm = {
  code: '',
  name: '',
  headOffice: '',
  headOfficeCity: '',
  headOfficeState: '',
  headOfficeCountry: '',
  swiftCode: '',
  licenseNumber: '',
  contactEmail: '',
  contactPhone: '',
};

export default function BanksPage() {
  const [banks, setBanks] = useState<BankResponse[]>([]);
  const [form, setForm] = useState(emptyBankForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const canManage = canManageBanks();

  const loadBanks = async () => {
    setLoading(true);
    setError('');
    try {
      setBanks(await bankService.listBanks());
    } catch {
      setError('Unable to load banks.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;

    bankService.listBanks()
      .then((data) => {
        if (active) {
          setBanks(data);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load banks.');
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
  }, []);

  const createBank = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      await bankService.createBank({
        code: form.code.trim(),
        name: form.name.trim(),
        headOffice: form.headOffice.trim() || undefined,
        headOfficeCity: form.headOfficeCity.trim() || undefined,
        headOfficeState: form.headOfficeState.trim() || undefined,
        headOfficeCountry: form.headOfficeCountry.trim() || undefined,
        swiftCode: form.swiftCode.trim() || undefined,
        licenseNumber: form.licenseNumber.trim() || undefined,
        contactEmail: form.contactEmail.trim() || undefined,
        contactPhone: form.contactPhone.trim() || undefined,
      });
      setForm(emptyBankForm);
      await loadBanks();
    } catch {
      setError('Unable to create bank.');
    } finally {
      setSaving(false);
    }
  };

  const disableBank = async (id: number) => {
    setError('');
    try {
      await bankService.disableBank(id);
      await loadBanks();
    } catch {
      setError('Unable to disable bank.');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Banks</h1>
        <p className="text-sm text-slate-500">Create and monitor tenant banks on the fraud platform.</p>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      {canManage && (
        <Card>
          <CardHeader>
            <CardTitle>Create Bank</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={createBank} className="grid gap-3 lg:grid-cols-4">
              <input
                value={form.code}
                onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Bank code"
                required
              />
              <input
                value={form.name}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Bank name"
                required
              />
              <input
                value={form.headOffice}
                onChange={(event) => setForm((current) => ({ ...current, headOffice: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Head office"
              />
              <input
                value={form.headOfficeCity}
                onChange={(event) => setForm((current) => ({ ...current, headOfficeCity: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Head office city"
              />
              <input
                value={form.headOfficeState}
                onChange={(event) => setForm((current) => ({ ...current, headOfficeState: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Head office state"
              />
              <input
                value={form.headOfficeCountry}
                onChange={(event) => setForm((current) => ({ ...current, headOfficeCountry: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Head office country"
              />
              <input
                value={form.swiftCode}
                onChange={(event) => setForm((current) => ({ ...current, swiftCode: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="SWIFT code"
              />
              <input
                value={form.licenseNumber}
                onChange={(event) => setForm((current) => ({ ...current, licenseNumber: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="License number"
              />
              <input
                value={form.contactEmail}
                onChange={(event) => setForm((current) => ({ ...current, contactEmail: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Contact email"
                type="email"
              />
              <input
                value={form.contactPhone}
                onChange={(event) => setForm((current) => ({ ...current, contactPhone: event.target.value }))}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                placeholder="Contact phone"
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
                <th className="px-6 py-4">Bank</th>
                <th className="px-6 py-4">Head Office</th>
                <th className="px-6 py-4">Regulatory</th>
                <th className="px-6 py-4">Contact</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4">Created</th>
                {canManage && <th className="px-6 py-4 text-right">Action</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && <tr><td colSpan={canManage ? 7 : 6} className="px-6 py-8 text-center text-slate-500">Loading banks...</td></tr>}
              {!loading && banks.length === 0 && <tr><td colSpan={canManage ? 7 : 6} className="px-6 py-8 text-center text-slate-500">No banks found.</td></tr>}
              {!loading && banks.map((bank) => (
                <tr key={bank.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2 font-medium text-slate-900">
                      <Building2 className="h-4 w-4 text-slate-400" /> {bank.name}
                    </div>
                    <div className="mt-1 text-xs text-slate-500">{bank.code}</div>
                  </td>
                  <td className="px-6 py-4 text-slate-600">
                    <div>{bank.headOffice || '-'}</div>
                    <div className="mt-1 text-xs text-slate-500">
                      {[bank.headOfficeCity, bank.headOfficeState, bank.headOfficeCountry].filter(Boolean).join(', ') || '-'}
                    </div>
                  </td>
                  <td className="px-6 py-4 text-slate-600">
                    <div>{bank.swiftCode || '-'}</div>
                    <div className="mt-1 text-xs text-slate-500">{bank.licenseNumber || '-'}</div>
                  </td>
                  <td className="px-6 py-4 text-slate-600">
                    <div>{bank.contactEmail || '-'}</div>
                    <div className="mt-1 text-xs text-slate-500">{bank.contactPhone || '-'}</div>
                  </td>
                  <td className="px-6 py-4"><Badge variant={bank.status === 'ACTIVE' ? 'success' : 'default'}>{bank.status}</Badge></td>
                  <td className="px-6 py-4 text-slate-500">
                    <div>{new Date(bank.createdAt).toLocaleString()}</div>
                    <div className="mt-1 text-xs text-slate-400">Updated {new Date(bank.updatedAt).toLocaleString()}</div>
                  </td>
                  {canManage && (
                    <td className="px-6 py-4 text-right">
                      <Button variant="outline" size="sm" disabled={bank.status !== 'ACTIVE'} onClick={() => disableBank(bank.id)}>Disable</Button>
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
