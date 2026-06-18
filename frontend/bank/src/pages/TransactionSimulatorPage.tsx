import { useEffect, useMemo, useState, type FormEvent, type InputHTMLAttributes } from 'react';
import { useNavigate } from 'react-router-dom';
import { Send } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { authService } from '../services/authService';
import { bankService } from '../services/bankService';
import { branchService } from '../services/branchService';
import { transactionService } from '../services/transactionService';
import { hasAnyRole } from '../utils/permissions';
import type { AccountType, BankResponse, BranchResponse, CustomerType, DailyTransactionPattern, TransactionResponse } from '../types/api';

const transactionTypes = ['TRANSFER', 'CASH_OUT', 'PAYMENT', 'CASH_IN', 'DEBIT'];
const channels = ['MOBILE_BANKING', 'INTERNET_BANKING', 'UPI', 'ATM', 'BRANCH_BANKING', 'CORPORATE_PORTAL', 'CORE_BANKING'];
const accountTypes: AccountType[] = ['INDIVIDUAL', 'SMALL_BUSINESS', 'BUSINESS', 'CORPORATE', 'GOVERNMENT'];
const customerTypes: CustomerType[] = ['RETAIL', 'PREMIUM', 'HIGH_NET_WORTH', 'SME', 'ENTERPRISE', 'GOVERNMENT', 'NEW_CUSTOMER'];
const dailyPatterns: DailyTransactionPattern[] = ['NORMAL', 'UNUSUAL'];

const defaultForm = {
  bankId: '',
  branchId: '',
  customerId: '',
  senderAccountNumber: 'ACC1001',
  receiverAccountNumber: 'ACC2001',
  amount: '50000',
  currency: 'INR',
  transactionType: 'TRANSFER',
  channel: 'MOBILE_BANKING',
  deviceId: 'DEV-8842',
  location: 'Chennai',
  ipAddress: '10.10.20.30',
  accountType: 'INDIVIDUAL' as AccountType,
  customerType: 'RETAIL' as CustomerType,
  beneficiaryTrusted: false,
  knownDevice: false,
  knownLocation: false,
  transactionHour: String(new Date().getHours()),
  dailyTransactionPattern: 'NORMAL' as DailyTransactionPattern,
  oldbalanceOrg: '100000',
  newbalanceOrig: '50000',
  oldbalanceDest: '10000',
  newbalanceDest: '60000',
};

export default function TransactionSimulatorPage() {
  const navigate = useNavigate();
  const storedUser = authService.getStoredUser();
  const isPlatformAdmin = hasAnyRole(['PLATFORM_ADMIN', 'SUPER_ADMIN']);
  const [banks, setBanks] = useState<BankResponse[]>([]);
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [form, setForm] = useState(defaultForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState<TransactionResponse | null>(null);

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const [bankData, branchData] = await Promise.all([
          isPlatformAdmin ? bankService.listBanks() : Promise.resolve([] as BankResponse[]),
          branchService.listBranches(),
        ]);
        if (!active) {
          return;
        }
        setBanks(bankData);
        setBranches(branchData);
        setForm((current) => {
          const inferredBankId = current.bankId || (isPlatformAdmin ? String(bankData.find((bank) => bank.status === 'ACTIVE')?.id || '') : String(storedUser?.bankId || ''));
          const inferredBranch = branchData.find((branch) => branch.status === 'ACTIVE' && (!inferredBankId || branch.bankId === Number(inferredBankId)));
          return {
            ...current,
            bankId: inferredBankId,
            branchId: current.branchId || (inferredBranch ? String(inferredBranch.id) : ''),
          };
        });
      } catch {
        if (active) {
          setError('Unable to load bank and branch data.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };
    load();
    return () => {
      active = false;
    };
  }, [isPlatformAdmin, storedUser?.bankId]);

  const activeBanks = useMemo(() => banks.filter((bank) => bank.status === 'ACTIVE'), [banks]);
  const visibleBranches = useMemo(() => {
    const bankId = Number(form.bankId);
    return branches.filter((branch) => branch.status === 'ACTIVE' && (!bankId || branch.bankId === bankId));
  }, [branches, form.bankId]);

  const setField = <K extends keyof typeof form>(field: K, value: typeof form[K]) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const optionalNumber = (value: string) => {
    const trimmed = value.trim();
    return trimmed ? Number(trimmed) : undefined;
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setResult(null);

    if (!form.bankId || !form.branchId) {
      setError('Bank and branch are required.');
      return;
    }
    if (form.senderAccountNumber.trim().toUpperCase() === form.receiverAccountNumber.trim().toUpperCase()) {
      setError('Sender and receiver accounts must be different.');
      return;
    }

    setSaving(true);
    try {
      const response = await transactionService.ingestTransaction({
        bankId: Number(form.bankId),
        branchId: Number(form.branchId),
        customerId: form.customerId.trim() || undefined,
        senderAccountNumber: form.senderAccountNumber.trim(),
        receiverAccountNumber: form.receiverAccountNumber.trim(),
        amount: Number(form.amount),
        currency: form.currency.trim() || 'INR',
        transactionType: form.transactionType,
        channel: form.channel,
        deviceId: form.deviceId.trim() || undefined,
        location: form.location.trim() || undefined,
        ipAddress: form.ipAddress.trim() || undefined,
        accountType: form.accountType,
        customerType: form.customerType,
        beneficiaryTrusted: form.beneficiaryTrusted,
        knownDevice: form.knownDevice,
        knownLocation: form.knownLocation,
        transactionHour: optionalNumber(form.transactionHour),
        dailyTransactionPattern: form.dailyTransactionPattern,
        oldbalanceOrg: optionalNumber(form.oldbalanceOrg),
        newbalanceOrig: optionalNumber(form.newbalanceOrig),
        oldbalanceDest: optionalNumber(form.oldbalanceDest),
        newbalanceDest: optionalNumber(form.newbalanceDest),
      });
      setResult(response);
    } catch {
      setError('Unable to ingest transaction.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Transaction Simulator</h1>
        <p className="text-sm text-slate-500">External channel transaction ingestion</p>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <Card>
        <CardHeader>
          <CardTitle>Ingest Transaction</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={submit} className="grid gap-4 lg:grid-cols-4">
            {isPlatformAdmin && (
              <label className="block">
                <span className="text-sm font-medium text-slate-700">Bank</span>
                <select
                  value={form.bankId}
                  onChange={(event) => setForm((current) => ({ ...current, bankId: event.target.value, branchId: '' }))}
                  className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  required
                >
                  <option value="">Select bank</option>
                  {activeBanks.map((bank) => <option key={bank.id} value={bank.id}>{bank.name} ({bank.code})</option>)}
                </select>
              </label>
            )}
            <label className="block">
              <span className="text-sm font-medium text-slate-700">Branch</span>
              <select
                value={form.branchId}
                onChange={(event) => setField('branchId', event.target.value)}
                className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                required
                disabled={loading}
              >
                <option value="">Select branch</option>
                {visibleBranches.map((branch) => <option key={branch.id} value={branch.id}>{branch.name} ({branch.code})</option>)}
              </select>
            </label>
            <TextInput label="Sender Account" value={form.senderAccountNumber} onChange={(value) => setField('senderAccountNumber', value)} required />
            <TextInput label="Receiver Account" value={form.receiverAccountNumber} onChange={(value) => setField('receiverAccountNumber', value)} required />
            <TextInput label="Amount" type="number" min="0.01" step="0.01" value={form.amount} onChange={(value) => setField('amount', value)} required />
            <TextInput label="Currency" value={form.currency} onChange={(value) => setField('currency', value.toUpperCase().slice(0, 3))} required />
            <SelectInput label="Transaction Type" value={form.transactionType} options={transactionTypes} onChange={(value) => setField('transactionType', value)} />
            <SelectInput label="Channel" value={form.channel} options={channels} onChange={(value) => setField('channel', value)} />
            <TextInput label="Device ID" value={form.deviceId} onChange={(value) => setField('deviceId', value)} />
            <TextInput label="Location" value={form.location} onChange={(value) => setField('location', value)} />
            <TextInput label="IP Address" value={form.ipAddress} onChange={(value) => setField('ipAddress', value)} />
            <TextInput label="Customer ID" value={form.customerId} onChange={(value) => setField('customerId', value)} />
            <SelectInput label="Account Type" value={form.accountType} options={accountTypes} onChange={(value) => setField('accountType', value as AccountType)} />
            <SelectInput label="Customer Type" value={form.customerType} options={customerTypes} onChange={(value) => setField('customerType', value as CustomerType)} />
            <SelectInput label="Daily Pattern" value={form.dailyTransactionPattern} options={dailyPatterns} onChange={(value) => setField('dailyTransactionPattern', value as DailyTransactionPattern)} />
            <TextInput label="Transaction Hour" type="number" min="0" max="23" value={form.transactionHour} onChange={(value) => setField('transactionHour', value)} />
            <TextInput label="Sender Balance Before" type="number" min="0" step="0.01" value={form.oldbalanceOrg} onChange={(value) => setField('oldbalanceOrg', value)} />
            <TextInput label="Sender Balance After" type="number" min="0" step="0.01" value={form.newbalanceOrig} onChange={(value) => setField('newbalanceOrig', value)} />
            <TextInput label="Receiver Balance Before" type="number" min="0" step="0.01" value={form.oldbalanceDest} onChange={(value) => setField('oldbalanceDest', value)} />
            <TextInput label="Receiver Balance After" type="number" min="0" step="0.01" value={form.newbalanceDest} onChange={(value) => setField('newbalanceDest', value)} />
            <div className="flex flex-wrap items-center gap-4 lg:col-span-4">
              <CheckInput label="Trusted Beneficiary" checked={form.beneficiaryTrusted} onChange={(value) => setField('beneficiaryTrusted', value)} />
              <CheckInput label="Known Device" checked={form.knownDevice} onChange={(value) => setField('knownDevice', value)} />
              <CheckInput label="Known Location" checked={form.knownLocation} onChange={(value) => setField('knownLocation', value)} />
            </div>
            <div className="lg:col-span-4">
              <Button type="submit" className="gap-2" disabled={saving || loading}>
                <Send className="h-4 w-4" /> {saving ? 'Submitting...' : 'Submit Transaction'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {result && (
        <Card>
          <CardHeader>
            <CardTitle>Fraud Decision</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-5">
            <ResultItem label="Reference" value={result.reference} />
            <ResultItem label="Risk Score" value={String(result.riskScore)} />
            <div>
              <p className="text-xs font-medium uppercase text-slate-500">Risk Level</p>
              <div className="mt-2"><Badge variant={result.riskLevel === 'LOW' ? 'success' : result.riskLevel === 'MEDIUM' ? 'warning' : 'danger'}>{result.riskLevel}</Badge></div>
            </div>
            <div>
              <p className="text-xs font-medium uppercase text-slate-500">Decision</p>
              <div className="mt-2"><Badge variant={result.fraudDecision === 'APPROVE' ? 'success' : result.fraudDecision === 'REVIEW' ? 'warning' : 'danger'}>{result.fraudDecision}</Badge></div>
            </div>
            <div className="flex items-end">
              <Button variant="outline" onClick={() => navigate(`/dashboard/transactions/${result.id}`)}>View Transaction</Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function TextInput({ label, value, onChange, ...props }: { label: string; value: string; onChange: (value: string) => void } & Omit<InputHTMLAttributes<HTMLInputElement>, 'value' | 'onChange'>) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
        {...props}
      />
    </label>
  );
}

function SelectInput({ label, value, options, onChange }: { label: string; value: string; options: string[]; onChange: (value: string) => void }) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
      >
        {options.map((option) => <option key={option} value={option}>{option}</option>)}
      </select>
    </label>
  );
}

function CheckInput({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="inline-flex items-center gap-2 text-sm font-medium text-slate-700">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
      />
      {label}
    </label>
  );
}

function ResultItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase text-slate-500">{label}</p>
      <p className="mt-2 font-semibold text-slate-900">{value}</p>
    </div>
  );
}
