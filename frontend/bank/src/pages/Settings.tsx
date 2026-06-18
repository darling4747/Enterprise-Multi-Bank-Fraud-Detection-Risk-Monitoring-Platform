import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { useEffect, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { authService } from '../services/authService';
import type { MfaSetupResponse, UserPreferencesResponse, UserSessionResponse } from '../types/api';

const defaultSettings: UserPreferencesResponse = {
  criticalAlertEmails: true,
  dailySummaryReport: false,
  mfaEnabled: false,
  sessionTimeoutMinutes: 30,
};

const strongPasswordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{12,}$/;

const apiErrorMessage = (error: unknown, fallback: string) => {
  const responseData = (error as { response?: { data?: string | { message?: string } } }).response?.data;
  if (typeof responseData === 'string') {
    return responseData;
  }
  return responseData?.message || fallback;
};

export default function Settings() {
  const [searchParams, setSearchParams] = useSearchParams();
  const storedUser = authService.getStoredUser();
  const [settings, setSettings] = useState<UserPreferencesResponse>({
    ...defaultSettings,
    mfaEnabled: Boolean(storedUser?.mfaEnabled),
  });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [mustChangePassword, setMustChangePassword] = useState(Boolean(storedUser?.mustChangePassword));
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [changingPassword, setChangingPassword] = useState(false);
  const [setup, setSetup] = useState<MfaSetupResponse | null>(null);
  const [mfaCode, setMfaCode] = useState('');
  const [disableCode, setDisableCode] = useState('');
  const [savingMfa, setSavingMfa] = useState(false);
  const [mfaEnabled, setMfaEnabled] = useState(Boolean(storedUser?.mfaEnabled));
  const [sessions, setSessions] = useState<UserSessionResponse[]>([]);
  const forcePasswordChange = mustChangePassword || searchParams.get('forcePasswordChange') === 'true';

  useEffect(() => {
    let active = true;
    authService.getPreferences()
      .then((data) => {
        if (active) {
          setSettings(data);
          setMfaEnabled(data.mfaEnabled);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load settings.');
        }
      });
    authService.getSessions()
      .then((data) => {
        if (active) {
          setSessions(data);
        }
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  const loadSessions = async () => {
    setSessions(await authService.getSessions());
  };

  const logoutOtherSessions = async () => {
    setError('');
    setMessage('');
    try {
      await authService.logoutOtherSessions();
      await loadSessions();
      setMessage('Other sessions logged out.');
    } catch {
      setError('Unable to logout other sessions.');
    }
  };

  const updatePasswordField = (field: keyof typeof passwordForm, value: string) => {
    setPasswordForm((current) => ({ ...current, [field]: value }));
  };

  const changePassword = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setMessage('');

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setError('New password and confirmation must match.');
      return;
    }
    if (!strongPasswordPattern.test(passwordForm.newPassword)) {
      setError('Password must be at least 12 characters and include uppercase, lowercase, number, and special character.');
      return;
    }

    setChangingPassword(true);
    try {
      const session = await authService.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setMustChangePassword(session.mustChangePassword);
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      if (searchParams.has('forcePasswordChange')) {
        setSearchParams({}, { replace: true });
      }
      setMessage(session.mustChangePassword ? 'Password updated.' : 'Password updated. Your account is now active.');
    } catch (err) {
      setError(apiErrorMessage(err, 'Unable to change password.'));
    } finally {
      setChangingPassword(false);
    }
  };

  const updateSettings = async (nextSettings: UserPreferencesResponse) => {
    setError('');
    setMessage('');
    try {
      const saved = await authService.updatePreferences({
        criticalAlertEmails: nextSettings.criticalAlertEmails,
        dailySummaryReport: nextSettings.dailySummaryReport,
        sessionTimeoutMinutes: nextSettings.sessionTimeoutMinutes,
      });
      setSettings(saved);
      setMfaEnabled(saved.mfaEnabled);
      setMessage('Settings updated.');
    } catch {
      setError('Unable to save settings.');
    }
  };

  const startMfaSetup = async () => {
    setSavingMfa(true);
    setError('');
    setMessage('');
    try {
      setSetup(await authService.startMfaSetup());
      setMfaCode('');
    } catch {
      setError('Unable to start MFA setup.');
    } finally {
      setSavingMfa(false);
    }
  };

  const verifyMfaSetup = async () => {
    setSavingMfa(true);
    setError('');
    setMessage('');
    try {
      const session = await authService.verifyMfaSetup({ code: mfaCode });
      setMfaEnabled(session.mfaEnabled);
      setSettings((current) => ({ ...current, mfaEnabled: session.mfaEnabled }));
      setSetup(null);
      setMfaCode('');
      setMessage('MFA enabled.');
    } catch {
      setError('Invalid MFA code. Scan the QR and enter the current 6-digit code.');
    } finally {
      setSavingMfa(false);
    }
  };

  const disableMfa = async () => {
    setSavingMfa(true);
    setError('');
    setMessage('');
    try {
      const session = await authService.disableMfa({ code: disableCode });
      setMfaEnabled(session.mfaEnabled);
      setSettings((current) => ({ ...current, mfaEnabled: session.mfaEnabled }));
      setDisableCode('');
      setMessage('MFA disabled.');
    } catch {
      setError('Invalid MFA code. Enter the current 6-digit code to disable MFA.');
    } finally {
      setSavingMfa(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">System Settings</h1>
      {message && <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div>}
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <Card>
        <CardHeader>
          <CardTitle>Password Security</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {forcePasswordChange && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
              Temporary credentials must be changed before continuing. If this password is not used within 24 hours, the login is blocked and the expiry is written to audit logs.
            </div>
          )}
          <form onSubmit={changePassword} className="space-y-4">
            <div className="grid gap-4 md:grid-cols-3">
              <label className="block">
                <span className="text-sm font-medium text-slate-700">Current password</span>
                <input
                  type="password"
                  value={passwordForm.currentPassword}
                  onChange={(event) => updatePasswordField('currentPassword', event.target.value)}
                  autoComplete="current-password"
                  className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  required
                />
              </label>
              <label className="block">
                <span className="text-sm font-medium text-slate-700">New password</span>
                <input
                  type="password"
                  value={passwordForm.newPassword}
                  onChange={(event) => updatePasswordField('newPassword', event.target.value)}
                  autoComplete="new-password"
                  className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  required
                />
              </label>
              <label className="block">
                <span className="text-sm font-medium text-slate-700">Confirm password</span>
                <input
                  type="password"
                  value={passwordForm.confirmPassword}
                  onChange={(event) => updatePasswordField('confirmPassword', event.target.value)}
                  autoComplete="new-password"
                  className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  required
                />
              </label>
            </div>
            <div className="flex flex-col gap-3 border-t border-slate-100 pt-4 md:flex-row md:items-center md:justify-between">
              <p className="text-sm text-slate-500">
                Minimum 12 characters with uppercase, lowercase, number, special character, and no reuse of the last 5 passwords.
              </p>
              <Button type="submit" disabled={changingPassword}>
                {changingPassword ? 'Updating...' : forcePasswordChange ? 'Activate Account' : 'Update Password'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader>
          <CardTitle>Notifications</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between py-3 border-b border-slate-100">
            <div>
              <p className="font-medium text-slate-900">Critical Alert Emails</p>
              <p className="text-sm text-slate-500">Receive email immediately for high and critical risk alerts.</p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={settings.criticalAlertEmails}
                onChange={(event) => updateSettings({ ...settings, criticalAlertEmails: event.target.checked })}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
          <div className="flex items-center justify-between py-3 border-b border-slate-100">
            <div>
              <p className="font-medium text-slate-900">Daily Summary Report</p>
              <p className="text-sm text-slate-500">Receive an automated daily email digest of flagged transactions.</p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={settings.dailySummaryReport}
                onChange={(event) => updateSettings({ ...settings, dailySummaryReport: event.target.checked })}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader>
          <CardTitle>Security Preferences</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between py-3 border-b border-slate-100">
            <div>
              <p className="font-medium text-slate-900">Two-Factor Authentication</p>
              <p className="text-sm text-slate-500">{mfaEnabled ? 'Authenticator verification is active.' : 'Require an authenticator code after password login.'}</p>
            </div>
            {!mfaEnabled && !setup && (
              <Button variant="outline" size="sm" disabled={savingMfa} onClick={startMfaSetup}>
                Enable 2FA
              </Button>
            )}
          </div>
          {setup && (
            <div className="grid gap-4 rounded-lg border border-slate-200 p-4 md:grid-cols-[240px_1fr]">
              <div className="rounded-md bg-white p-3 shadow-sm">
                <img src={setup.qrCodeUrl} alt="MFA QR code" className="h-[220px] w-[220px]" />
              </div>
              <div className="space-y-3">
                <div>
                  <p className="text-sm font-medium text-slate-900">Manual key</p>
                  <p className="mt-1 break-all rounded-md bg-slate-100 px-3 py-2 font-mono text-xs text-slate-700">{setup.secret}</p>
                </div>
                <label className="block">
                  <span className="text-sm font-medium text-slate-700">Authenticator code</span>
                  <input
                    value={mfaCode}
                    onChange={(event) => setMfaCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                    inputMode="numeric"
                    pattern="\d{6}"
                    className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm tracking-[0.3em] outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  />
                </label>
                <div className="flex gap-2">
                  <Button size="sm" disabled={savingMfa || mfaCode.length !== 6} onClick={verifyMfaSetup}>Verify and Enable</Button>
                  <Button size="sm" variant="outline" disabled={savingMfa} onClick={() => setSetup(null)}>Cancel</Button>
                </div>
              </div>
            </div>
          )}
          {mfaEnabled && (
            <div className="grid gap-3 rounded-lg border border-slate-200 p-4 md:grid-cols-[1fr_auto]">
              <input
                value={disableCode}
                onChange={(event) => setDisableCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                inputMode="numeric"
                pattern="\d{6}"
                placeholder="Current 6-digit code"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm tracking-[0.3em] outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              />
              <Button variant="outline" disabled={savingMfa || disableCode.length !== 6} onClick={disableMfa}>Disable 2FA</Button>
            </div>
          )}
          <div className="flex items-center justify-between py-3 border-b border-slate-100">
            <div>
              <p className="font-medium text-slate-900">Session Timeout</p>
              <p className="text-sm text-slate-500">Automatically log out after inactivity.</p>
            </div>
            <select
              value={settings.sessionTimeoutMinutes}
              onChange={(event) => updateSettings({ ...settings, sessionTimeoutMinutes: Number(event.target.value) })}
              className="border border-slate-200 rounded-md text-sm py-1.5 px-3 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value={15}>15 minutes</option>
              <option value={30}>30 minutes</option>
              <option value={60}>1 hour</option>
            </select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Current Login Sessions</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {sessions.length === 0 && <p className="text-sm text-slate-500">No active sessions found.</p>}
          {sessions.map((session) => (
            <div key={session.id} className="flex flex-col gap-1 rounded-lg border border-slate-200 p-3 text-sm md:flex-row md:items-center md:justify-between">
              <div>
                <p className="font-medium text-slate-900">
                  {session.browser || 'Unknown Browser'} on {session.operatingSystem || 'Unknown OS'}
                  {session.current && <span className="ml-2 rounded bg-emerald-100 px-2 py-0.5 text-xs text-emerald-700">Current</span>}
                </p>
                <p className="text-slate-500">{session.ipAddress || 'Unknown IP'} · {new Date(session.loginTime).toLocaleString()}</p>
              </div>
              <p className="text-xs text-slate-500">Last active {new Date(session.lastSeenAt).toLocaleString()}</p>
            </div>
          ))}
          <div className="flex justify-end">
            <Button variant="outline" disabled={sessions.filter((session) => !session.current).length === 0} onClick={logoutOtherSessions}>
              Logout Other Sessions
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
