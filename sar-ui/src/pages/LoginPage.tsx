import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { saveCredentials } from '@/lib/auth';
import { checkCredentials } from '@/api/sarApi';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { toast } from 'sonner';
import { Toaster } from 'sonner';

export function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      const ok = await checkCredentials(username, password);
      if (!ok) {
        toast.error('Invalid credentials');
        return;
      }
      saveCredentials({ username, password });
      navigate('/sar');
    } catch {
      toast.error('Could not reach server');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100">
      <Toaster position="top-center" richColors />
      <div className="w-full max-w-sm">
        <div className="card p-8">
          <div className="mb-6 text-center">
            <span className="text-4xl">🏦</span>
            <h1 className="mt-2 text-xl font-bold text-slate-900">SAR Solution</h1>
            <p className="mt-1 text-sm text-slate-500">FinCEN Suspicious Activity Reporting</p>
          </div>
          <form onSubmit={e => { void submit(e); }} className="space-y-4">
            <Input
              label="Username"
              id="username"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="analyst"
              autoComplete="username"
              required
            />
            <Input
              label="Password"
              id="password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
              required
            />
            <Button type="submit" disabled={loading} className="w-full justify-center">
              {loading ? 'Signing in…' : 'Sign in'}
            </Button>
          </form>
          <div className="mt-5 rounded-lg bg-slate-50 p-3 text-xs text-slate-500 space-y-0.5">
            <p className="font-medium text-slate-700 mb-1">Demo credentials</p>
            <p>analyst / analyst123</p>
            <p>compliance / compliance123</p>
            <p>admin / admin123</p>
          </div>
        </div>
      </div>
    </div>
  );
}
