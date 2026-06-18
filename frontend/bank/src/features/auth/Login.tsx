import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { authService } from '../../services/authService';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [mfaCode, setMfaCode] = useState('');
  const [mfaRequired, setMfaRequired] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname || '/dashboard';

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const response = await authService.login({ username, password, mfaCode: mfaRequired ? mfaCode : undefined });
      if (response.mfaRequired) {
        setMfaRequired(true);
        setMfaCode('');
        setError('');
        return;
      }
      navigate(response.mustChangePassword ? '/dashboard/settings?forcePasswordChange=true' : from, { replace: true });
    } catch {
      setError(mfaRequired ? 'Invalid MFA code. Check your authenticator app and try again.' : 'Login failed. Check your username, password, and backend server.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <form onSubmit={handleSubmit} className="p-8 space-y-5">
          <div className="flex items-center gap-3">
            <div className="h-11 w-11 rounded-lg bg-blue-600 text-white flex items-center justify-center">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-900">SecureBank Login</h1>
              <p className="text-sm text-slate-500">Connected to Spring Boot API</p>
            </div>
          </div>

          {error && <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}

          <label className="block">
            <span className="text-sm font-medium text-slate-700">Username or email</span>
            <input
              value={username}
              onChange={(event) => {
                setUsername(event.target.value);
                setMfaRequired(false);
              }}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              required
              disabled={mfaRequired}
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-slate-700">Password</span>
            <input
              value={password}
              onChange={(event) => {
                setPassword(event.target.value);
                setMfaRequired(false);
              }}
              type="password"
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              required
              disabled={mfaRequired}
            />
          </label>

          {mfaRequired && (
            <label className="block">
              <span className="text-sm font-medium text-slate-700">Authenticator code</span>
              <input
                value={mfaCode}
                onChange={(event) => setMfaCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                inputMode="numeric"
                pattern="\d{6}"
                autoComplete="one-time-code"
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm tracking-[0.3em] outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                required
              />
            </label>
          )}

          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Signing in...' : mfaRequired ? 'Verify code' : 'Sign in'}
          </Button>

          {mfaRequired && (
            <Button type="button" variant="ghost" className="w-full" onClick={() => setMfaRequired(false)}>
              Use a different account
            </Button>
          )}

          <p className="text-sm text-slate-500 text-center">
            Use the temporary credentials issued by your platform or bank administrator.
          </p>
        </form>
      </Card>
    </div>
  );
}
