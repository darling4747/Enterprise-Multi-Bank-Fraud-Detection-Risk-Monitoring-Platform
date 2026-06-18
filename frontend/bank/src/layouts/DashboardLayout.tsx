import { useEffect, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Activity, 
  AlertTriangle, 
  ShieldCheck, 
  Settings, 
  Bell,
  Search,
  User as UserIcon,
  LogOut,
  Building2,
  GitBranch,
  Users,
  FileSearch,
  BarChart3,
  FileText,
  BrainCircuit,
  Send,
  UserRound,
  WalletCards,
  Handshake,
  Siren,
  Menu,
  X
} from 'lucide-react';
import { SESSION_ACTIVITY_KEY, authService } from '../services/authService';
import type { EnterpriseRoleType } from '../types/api';

export function DashboardLayout() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const user = authService.getStoredUser();
  const userSessionId = user?.sessionId;
  const userSessionTimeoutMinutes = user?.sessionTimeoutMinutes;

  const userRoles = user?.roles ?? [];
  const canSee = (roles?: EnterpriseRoleType[]) => !roles || userRoles.some((role) => roles.includes(role));
  const adminRoles: EnterpriseRoleType[] = ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN'];
  const readRoles: EnterpriseRoleType[] = ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'BRANCH_MANAGER', 'FRAUD_ANALYST', 'RISK_OFFICER', 'AUDITOR'];
  const basePath = '/dashboard';

  useEffect(() => {
    if (!authService.isAuthenticated()) {
      return undefined;
    }

    let lastActivityWrite = 0;
    const markActive = () => {
      const now = Date.now();
      if (now - lastActivityWrite < 1000) {
        return;
      }
      lastActivityWrite = now;
      localStorage.setItem(SESSION_ACTIVITY_KEY, String(now));
    };

    if (!localStorage.getItem(SESSION_ACTIVITY_KEY)) {
      markActive();
    }

    const activityEvents = ['click', 'keydown', 'mousedown', 'mousemove', 'scroll', 'touchstart'];
    activityEvents.forEach((eventName) => window.addEventListener(eventName, markActive, { passive: true }));

    const timer = window.setInterval(() => {
      if (!authService.isAuthenticated()) {
        return;
      }
      const storedUser = authService.getStoredUser();
      const timeoutMinutes = storedUser?.sessionTimeoutMinutes || 30;
      const lastActivityAt = Number(localStorage.getItem(SESSION_ACTIVITY_KEY) || Date.now());
      if (Date.now() - lastActivityAt >= timeoutMinutes * 60 * 1000) {
        authService.logout();
        navigate('/login', { replace: true, state: { reason: 'session-timeout' } });
      }
    }, 1000);

    return () => {
      window.clearInterval(timer);
      activityEvents.forEach((eventName) => window.removeEventListener(eventName, markActive));
    };
  }, [navigate, userSessionId, userSessionTimeoutMinutes]);

  const navItems = [
    { path: basePath, label: 'Overview', icon: LayoutDashboard, roles: readRoles },
    { path: `${basePath}/banks`, label: 'Banks', icon: Building2, roles: ['PLATFORM_ADMIN', 'SUPER_ADMIN'] as EnterpriseRoleType[] },
    { path: `${basePath}/branches`, label: 'Branches', icon: GitBranch, roles: adminRoles },
    { path: `${basePath}/users`, label: 'User Management', icon: Users, roles: adminRoles },
    { path: `${basePath}/customers`, label: 'Customers', icon: UserRound, roles: ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'BRANCH_MANAGER', 'FRAUD_ANALYST', 'RISK_OFFICER', 'AUDITOR'] as EnterpriseRoleType[] },
    { path: `${basePath}/accounts`, label: 'Accounts', icon: WalletCards, roles: ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'BRANCH_MANAGER', 'FRAUD_ANALYST', 'RISK_OFFICER', 'AUDITOR'] as EnterpriseRoleType[] },
    { path: `${basePath}/beneficiaries`, label: 'Beneficiaries', icon: Handshake, roles: ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'BRANCH_MANAGER', 'FRAUD_ANALYST', 'RISK_OFFICER', 'AUDITOR'] as EnterpriseRoleType[] },
    { path: `${basePath}/simulator`, label: 'Simulator', icon: Send, roles: ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN'] as EnterpriseRoleType[] },
    { path: `${basePath}/transactions`, label: 'Transactions', icon: Activity, roles: readRoles },
    { path: `${basePath}/fraud-cases`, label: 'Fraud Cases', icon: ShieldCheck, roles: readRoles },
    { path: `${basePath}/alerts`, label: 'Fraud Alerts', icon: AlertTriangle, roles: readRoles },
    { path: `${basePath}/analytics`, label: 'Analytics', icon: BarChart3, roles: readRoles },
    { path: `${basePath}/reports`, label: 'Reports', icon: FileText, roles: ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'RISK_OFFICER', 'AUDITOR'] as EnterpriseRoleType[] },
    { path: `${basePath}/audit-logs`, label: 'Audit Logs', icon: FileSearch, roles: ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'AUDITOR'] as EnterpriseRoleType[] },
    { path: `${basePath}/ml-monitor`, label: 'ML Monitor', icon: BrainCircuit, roles: ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'RISK_OFFICER', 'AUDITOR'] as EnterpriseRoleType[] },
    { path: `${basePath}/security-incidents`, label: 'Security Incidents', icon: Siren, roles: ['PLATFORM_ADMIN', 'SUPER_ADMIN', 'BANK_ADMIN', 'AUDITOR'] as EnterpriseRoleType[] },
    { path: `${basePath}/notifications`, label: 'Notifications', icon: Bell, roles: readRoles },
    { path: `${basePath}/settings`, label: 'Settings', icon: Settings, roles: readRoles },
  ];

  return (
    <div className="flex h-screen bg-slate-50 overflow-hidden relative">
      {/* Mobile Backdrop */}
      {isMobileMenuOpen && (
        <div 
          className="fixed inset-0 bg-slate-900/50 z-40 md:hidden" 
          onClick={() => setIsMobileMenuOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`fixed inset-y-0 left-0 z-50 w-64 bg-[#0f172a] text-slate-300 flex flex-col transition-transform duration-300 ease-in-out md:relative md:translate-x-0 ${isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="h-16 flex items-center justify-between px-6 border-b border-slate-800 shrink-0">
          <div className="flex items-center">
            <ShieldCheck className="w-8 h-8 text-blue-500 mr-3" />
            <span className="text-lg font-bold text-white tracking-wide">SecureBank</span>
          </div>
          <button 
            className="md:hidden text-slate-400 hover:text-white"
            onClick={() => setIsMobileMenuOpen(false)}
          >
            <X className="w-6 h-6" />
          </button>
        </div>
        
        <div className="p-4 flex-1 overflow-y-auto">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">
            Main Menu
          </div>
          <nav className="space-y-1">
            {navItems.filter((item) => canSee(item.roles)).map((item) => {
              const Icon = item.icon;
              const isActive = item.path === basePath
                ? location.pathname === basePath
                : location.pathname === item.path || location.pathname.startsWith(`${item.path}/`);
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={() => setIsMobileMenuOpen(false)}
                  className={`flex items-center px-3 py-2.5 rounded-lg transition-colors group ${
                    isActive 
                      ? 'bg-blue-600 text-white' 
                      : 'hover:bg-slate-800 hover:text-white'
                  }`}
                >
                  <Icon className={`w-5 h-5 mr-3 ${isActive ? 'text-white' : 'text-slate-400 group-hover:text-slate-300'}`} />
                  <span className="font-medium text-sm">{item.label}</span>
                </Link>
              );
            })}
          </nav>
        </div>

        <div className="p-4 border-t border-slate-800">
          <div className="flex items-center px-2">
            <div className="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center mr-3">
              <UserIcon className="w-4 h-4 text-slate-300" />
            </div>
            <div className="min-w-0">
              <div className="text-sm font-medium text-white truncate">{user?.username || 'Analyst'}</div>
              <div className="text-xs text-slate-500 truncate">{user?.roles?.[0]?.replace('_', ' ') || 'Fraud Analyst'}</div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden w-full">
        {/* Topbar */}
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 md:px-6 shrink-0">
          <div className="flex items-center flex-1 gap-4">
            <button
              onClick={() => setIsMobileMenuOpen(true)}
              className="p-2 -ml-2 text-slate-500 hover:bg-slate-100 rounded-md md:hidden"
            >
              <Menu className="w-6 h-6" />
            </button>
            <div className="flex items-center w-full max-w-md relative hidden sm:flex">
              <Search className="w-5 h-5 text-slate-400 absolute left-3" />
              <input 
                type="text" 
                placeholder="Search transactions, accounts, alerts..." 
                className="w-full pl-10 pr-4 py-2 bg-slate-100 border-transparent rounded-md text-sm focus:bg-white focus:border-blue-500 focus:ring-2 focus:ring-blue-200 outline-none transition-all"
              />
            </div>
          </div>
          
          <div className="flex items-center space-x-4">
            <button
              onClick={() => navigate('/dashboard/notifications')}
              className="relative p-2 text-slate-500 hover:bg-slate-100 rounded-full transition-colors"
              title="Open alerts"
            >
              <Bell className="w-5 h-5" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
            </button>
            <button
              onClick={() => {
                authService.logout();
                navigate('/login', { replace: true });
              }}
              className="p-2 text-slate-500 hover:bg-slate-100 rounded-full transition-colors"
              title="Sign out"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
