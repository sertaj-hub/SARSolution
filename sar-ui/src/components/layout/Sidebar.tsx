import { NavLink } from 'react-router-dom';
import { clearCredentials, loadCredentials } from '@/lib/auth';
import { useNavigate } from 'react-router-dom';
import { queryClient } from '@/lib/queryClient';

const links = [
  { to: '/sar', label: 'SAR Reports', icon: '📋' },
  { to: '/efiling', label: 'eFiling Batches', icon: '📤' },
];

export function Sidebar() {
  const navigate = useNavigate();
  const creds = loadCredentials();

  function logout() {
    clearCredentials();
    queryClient.clear();
    navigate('/login');
  }

  return (
    <aside className="flex h-screen w-56 flex-col border-r border-slate-200 bg-white">
      <div className="flex h-14 items-center gap-2 border-b border-slate-200 px-4">
        <span className="text-lg">🏦</span>
        <span className="text-sm font-bold text-slate-900">SAR Solution</span>
      </div>

      <nav className="flex-1 overflow-y-auto py-3 px-2 space-y-0.5">
        {links.map(l => (
          <NavLink
            key={l.to}
            to={l.to}
            className={({ isActive }) =>
              `flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
              }`
            }
          >
            <span>{l.icon}</span>
            {l.label}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-slate-200 px-4 py-3">
        <p className="text-xs text-slate-500 truncate">{creds?.username}</p>
        <button onClick={logout} className="mt-1 text-xs text-red-500 hover:text-red-700">Sign out</button>
      </div>
    </aside>
  );
}
