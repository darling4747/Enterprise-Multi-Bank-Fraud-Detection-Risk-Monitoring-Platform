import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Copy, KeyRound, LockOpen, Plus, Save, UserCheck, UserPlus, UserX } from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { bankService } from '../services/bankService';
import { branchService } from '../services/branchService';
import { userService } from '../services/userService';
import type { BankResponse, BranchResponse, EnterpriseRoleType, UserResponse } from '../types/api';
import { canIssueBankAdmins, canIssueEmployees, canManageUsers } from '../utils/permissions';

const bankAdminRoles: EnterpriseRoleType[] = ['BANK_ADMIN'];
const employeeRoles: EnterpriseRoleType[] = ['BRANCH_MANAGER', 'FRAUD_ANALYST', 'RISK_OFFICER', 'AUDITOR'];

export default function UserManagementPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [banks, setBanks] = useState<BankResponse[]>([]);
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [form, setForm] = useState({
    username: '',
    email: '',
    fullName: '',
    bankId: '',
    branchId: '',
    employeeId: '',
    role: 'FRAUD_ANALYST' as EnterpriseRoleType,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [copiedUserId, setCopiedUserId] = useState<number | null>(null);
  const [roleEdits, setRoleEdits] = useState<Record<number, EnterpriseRoleType>>({});
  const canCreateBankAdmins = canIssueBankAdmins();
  const canCreateEmployees = canIssueEmployees();
  const canManage = canManageUsers();
  const activeBanks = useMemo(() => banks.filter((bank) => bank.status === 'ACTIVE'), [banks]);
  const activeBranches = useMemo(() => branches.filter((branch) => branch.status === 'ACTIVE'), [branches]);
  const assignableRoles = useMemo(() => {
    if (canCreateBankAdmins && canCreateEmployees) {
      return [...bankAdminRoles, ...employeeRoles];
    }
    if (canCreateBankAdmins) {
      return bankAdminRoles;
    }
    if (canCreateEmployees) {
      return employeeRoles;
    }
    return [];
  }, [canCreateBankAdmins, canCreateEmployees]);

  const selectedRole = assignableRoles.includes(form.role) ? form.role : assignableRoles[0];

  const loadUsers = async () => {
    setLoading(true);
    setError('');
    try {
      setUsers(await userService.listUsers());
    } catch {
      setError('Unable to load users.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;

    Promise.all([
      userService.listUsers(),
      canCreateBankAdmins ? bankService.listBanks() : Promise.resolve([] as BankResponse[]),
      canCreateEmployees ? branchService.listBranches() : Promise.resolve([] as BranchResponse[]),
    ])
      .then(([userData, bankData, branchData]) => {
        if (active) {
          setUsers(userData);
          setBanks(bankData);
          setBranches(branchData);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load users.');
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
  }, [canCreateBankAdmins, canCreateEmployees]);

  const createUser = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedRole || !assignableRoles.includes(selectedRole)) {
      setError('Your role cannot issue this type of credential.');
      return;
    }
    setSaving(true);
    setError('');
    setMessage('');
    setCopiedUserId(null);
    const request = {
      username: form.username.trim(),
      email: form.email.trim(),
      password: 'Temporary@123',
      fullName: form.fullName.trim(),
      bankId: selectedRole === 'BANK_ADMIN' && form.bankId ? Number(form.bankId) : undefined,
      branchId: selectedRole !== 'BANK_ADMIN' && form.branchId ? Number(form.branchId) : undefined,
      employeeId: form.employeeId.trim() || undefined,
      roles: [selectedRole],
      enabled: true,
    };

    try {
      const response = selectedRole === 'BANK_ADMIN'
        ? await userService.createBankAdmin(request)
        : await userService.createEmployee(request);
      setMessage(`Temporary password for ${response.user.username} is now visible in that user's Password column until they change it.`);
      setForm({ username: '', email: '', fullName: '', bankId: '', branchId: '', employeeId: '', role: selectedRole });
      await loadUsers();
    } catch {
      setError('Unable to create user. Check role, bank, and branch permissions.');
    } finally {
      setSaving(false);
    }
  };

  const activeCount = useMemo(() => users.filter((user) => user.status === 'ACTIVE').length, [users]);

  const roleOptionsFor = (user: UserResponse) => {
    if (canCreateEmployees && user.roles.every((role) => employeeRoles.includes(role))) {
      return employeeRoles;
    }
    if (canCreateBankAdmins && user.roles.includes('BANK_ADMIN')) {
      return bankAdminRoles;
    }
    return [];
  };

  const selectedRoleFor = (user: UserResponse) => {
    const options = roleOptionsFor(user);
    return roleEdits[user.id] || user.roles.find((role) => options.includes(role)) || options[0];
  };

  const updateUserRole = async (user: UserResponse) => {
    const nextRole = selectedRoleFor(user);
    if (!nextRole) {
      return;
    }
    setError('');
    setMessage('');
    try {
      await userService.updateRoles(user.id, [nextRole]);
      setMessage(`Role updated for ${user.username}.`);
      await loadUsers();
    } catch {
      setError('Unable to update role for this user.');
    }
  };

  const resetPassword = async (id: number) => {
    setError('');
    setMessage('');
    setCopiedUserId(null);
    try {
      const response = await userService.resetPassword(id);
      setMessage(`New temporary password for ${response.user.username} is visible in that user's Password column.`);
      await loadUsers();
    } catch {
      setError('Unable to reset password for this user.');
    }
  };

  const copyTemporaryPassword = async (user: UserResponse) => {
    if (!user.visibleTemporaryPassword) {
      return;
    }
    const expires = user.temporaryPasswordExpiresAt ? new Date(user.temporaryPasswordExpiresAt).toLocaleString() : 'Not set';
    const value = `Username: ${user.username}\nTemporary password: ${user.visibleTemporaryPassword}\nExpires: ${expires}`;
    try {
      await navigator.clipboard.writeText(value);
      setCopiedUserId(user.id);
    } catch {
      setError('Unable to copy password. Select it from the table and copy it manually.');
    }
  };

  const unlockUser = async (id: number) => {
    setError('');
    try {
      await userService.unlockUser(id);
      await loadUsers();
    } catch {
      setError('Unable to unlock this user.');
    }
  };

  const setUserActive = async (user: UserResponse, enabled: boolean) => {
    setError('');
    try {
      await userService.setEnabled(user.id, enabled);
      await loadUsers();
    } catch {
      setError(enabled ? 'Unable to reactivate this user.' : 'Unable to deactivate this user.');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">User Management</h1>
        <p className="text-sm text-slate-500">Create bank admins, branch managers, analysts, risk officers, and auditors.</p>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}
      {message && <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div>}

      <div className="grid gap-4 md:grid-cols-3">
        <Summary label="Total Users" value={users.length} />
        <Summary label="Active Users" value={activeCount} />
        <Summary label="Temporary Passwords" value={users.filter((user) => user.passwordStatus === 'TEMPORARY').length} />
      </div>

      {assignableRoles.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Create Login</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={createUser} className="grid gap-3 lg:grid-cols-4">
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Username" value={form.username} onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))} required />
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Email" type="email" value={form.email} onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))} required />
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Full name" value={form.fullName} onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))} required />
              <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={selectedRole} onChange={(event) => setForm((current) => ({ ...current, role: event.target.value as EnterpriseRoleType }))}>
                {assignableRoles.map((role) => <option key={role} value={role}>{role}</option>)}
              </select>
              {selectedRole === 'BANK_ADMIN' && (
                <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.bankId} onChange={(event) => setForm((current) => ({ ...current, bankId: event.target.value }))} required>
                  <option value="">Select bank</option>
                  {activeBanks.map((bank) => <option key={bank.id} value={bank.id}>{bank.name} ({bank.code})</option>)}
                </select>
              )}
              {selectedRole !== 'BANK_ADMIN' && (
                <select className="rounded-md border border-slate-300 px-3 py-2 text-sm" value={form.branchId} onChange={(event) => setForm((current) => ({ ...current, branchId: event.target.value }))}>
                  <option value="">No branch</option>
                  {activeBranches.map((branch) => <option key={branch.id} value={branch.id}>{branch.name} ({branch.bankCode})</option>)}
                </select>
              )}
              <input className="rounded-md border border-slate-300 px-3 py-2 text-sm" placeholder="Employee ID" value={form.employeeId} onChange={(event) => setForm((current) => ({ ...current, employeeId: event.target.value }))} />
              <Button type="submit" disabled={saving} className="gap-2"><Plus className="h-4 w-4" /> Create User</Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-500">
              <tr>
                <th className="px-6 py-4">User</th>
                <th className="px-6 py-4">Tenant</th>
                <th className="px-6 py-4">Roles</th>
                <th className="px-6 py-4">Password</th>
                <th className="px-6 py-4">Status</th>
                {canManage && <th className="px-6 py-4 text-right">Actions</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading && <tr><td colSpan={canManage ? 6 : 5} className="px-6 py-8 text-center text-slate-500">Loading users...</td></tr>}
              {!loading && users.length === 0 && <tr><td colSpan={canManage ? 6 : 5} className="px-6 py-8 text-center text-slate-500">No users found.</td></tr>}
              {!loading && users.map((user) => (
                <tr key={user.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2 font-medium text-slate-900"><UserPlus className="h-4 w-4 text-slate-400" /> {user.fullName}</div>
                    <div className="mt-1 text-xs text-slate-500">{user.username} · {user.email}</div>
                  </td>
                  <td className="px-6 py-4 text-slate-600">
                    <div>{user.bankCode || '-'}</div>
                    <div className="text-xs text-slate-500">{user.branchCode || '-'}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex flex-wrap gap-1">{user.roles.map((role) => <Badge key={role} variant="info">{role}</Badge>)}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="min-w-[240px] space-y-2">
                      <Badge variant={user.passwordStatus === 'TEMPORARY' ? 'warning' : user.passwordStatus === 'EXPIRED' ? 'danger' : 'success'}>{user.passwordStatus}</Badge>
                      {user.visibleTemporaryPassword && (
                        <div className="rounded-md border border-amber-200 bg-amber-50 p-2">
                          <div className="flex items-start justify-between gap-2">
                            <div>
                              <p className="text-xs font-medium text-amber-700">Temporary password</p>
                              <p className="mt-1 break-all font-mono text-sm font-semibold text-slate-900">{user.visibleTemporaryPassword}</p>
                            </div>
                            <Button type="button" variant="outline" size="sm" className="h-7 gap-1 bg-white px-2" onClick={() => copyTemporaryPassword(user)}>
                              <Copy className="h-3.5 w-3.5" /> {copiedUserId === user.id ? 'Copied' : 'Copy'}
                            </Button>
                          </div>
                          {user.temporaryPasswordExpiresAt && (
                            <p className="mt-2 text-xs text-amber-700">Expires {new Date(user.temporaryPasswordExpiresAt).toLocaleString()}</p>
                          )}
                        </div>
                      )}
                      {!user.visibleTemporaryPassword && user.passwordStatus === 'TEMPORARY' && (
                        <p className="text-xs text-slate-500">Reset to generate a visible temporary password.</p>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4"><Badge variant={user.status === 'ACTIVE' ? 'success' : user.status === 'LOCKED' ? 'danger' : 'default'}>{user.status}</Badge></td>
                  {canManage && (
                    <td className="px-6 py-4">
                      <div className="flex flex-wrap justify-end gap-2">
                        {roleOptionsFor(user).length > 0 && (
                          <>
                            <select
                              value={selectedRoleFor(user)}
                              onChange={(event) => setRoleEdits((current) => ({ ...current, [user.id]: event.target.value as EnterpriseRoleType }))}
                              className="h-8 rounded-md border border-slate-300 bg-white px-2 text-xs text-slate-700 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                            >
                              {roleOptionsFor(user).map((role) => <option key={role} value={role}>{role}</option>)}
                            </select>
                            <Button variant="outline" size="sm" className="gap-1" onClick={() => updateUserRole(user)}>
                              <Save className="h-3.5 w-3.5" /> Role
                            </Button>
                          </>
                        )}
                        <Button variant="outline" size="sm" className="gap-1" onClick={() => resetPassword(user.id)}><KeyRound className="h-3.5 w-3.5" /> Reset</Button>
                        <Button variant="outline" size="sm" className="gap-1" disabled={user.status !== 'LOCKED'} onClick={() => unlockUser(user.id)}><LockOpen className="h-3.5 w-3.5" /> Unlock</Button>
                        {user.status === 'INACTIVE' ? (
                          <Button variant="outline" size="sm" className="gap-1" onClick={() => setUserActive(user, true)}><UserCheck className="h-3.5 w-3.5" /> Activate</Button>
                        ) : (
                          <Button variant="outline" size="sm" className="gap-1 text-red-600" onClick={() => setUserActive(user, false)}><UserX className="h-3.5 w-3.5" /> Deactivate</Button>
                        )}
                      </div>
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

function Summary({ label, value }: { label: string; value: number }) {
  return (
    <Card className="p-4">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-bold text-slate-900">{value}</p>
    </Card>
  );
}
