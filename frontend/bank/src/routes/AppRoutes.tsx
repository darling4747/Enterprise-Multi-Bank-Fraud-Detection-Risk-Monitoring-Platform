import { Routes, Route, Navigate, useParams } from 'react-router-dom';
import { DashboardLayout } from '../layouts/DashboardLayout';
import Dashboard from '../pages/Dashboard';
import TransactionList from '../pages/Transactions/TransactionList';
import AlertList from '../pages/Alerts/AlertList';
import Login from '../features/auth/Login';
import { PrivateRoute } from './PrivateRoute';
import BanksPage from '../pages/BanksPage';
import BranchesPage from '../pages/BranchesPage';
import UserManagementPage from '../pages/UserManagementPage';
import TransactionSimulatorPage from '../pages/TransactionSimulatorPage';
import CustomersPage from '../pages/CustomersPage';
import AccountsPage from '../pages/AccountsPage';
import BeneficiariesPage from '../pages/BeneficiariesPage';
import SecurityIncidentsPage from '../pages/SecurityIncidentsPage';
import NotificationsPage from '../pages/NotificationsPage';
import TransactionDetails from '../features/transactions/TransactionDetails';
import FraudCasesPage from '../features/fraud/FraudCasesPage';
import FraudDetailsPage from '../features/fraud/FraudDetailsPage';
import AnalyticsPage from '../features/analytics/AnalyticsPage';
import ReportsPage from '../features/analytics/ReportsPage';
import AuditLogsPage from '../pages/AuditLogsPage';
import MlMonitorPage from '../pages/MlMonitorPage';
import Profile from '../pages/Profile';
import Settings from '../pages/Settings';
import NotFound from '../pages/NotFound';

const legacyRoutes = [
  'banks',
  'branches',
  'users',
  'customers',
  'accounts',
  'beneficiaries',
  'simulator',
  'transactions',
  'fraud-cases',
  'alerts',
  'analytics',
  'reports',
  'audit-logs',
  'ml-monitor',
  'security-incidents',
  'notifications',
  'profile',
  'settings',
];

function dashboardRoutes() {
  return (
    <>
      <Route index element={<Dashboard />} />
      <Route path="banks" element={<BanksPage />} />
      <Route path="branches" element={<BranchesPage />} />
      <Route path="users" element={<UserManagementPage />} />
      <Route path="customers" element={<CustomersPage />} />
      <Route path="accounts" element={<AccountsPage />} />
      <Route path="beneficiaries" element={<BeneficiariesPage />} />
      <Route path="simulator" element={<TransactionSimulatorPage />} />
      <Route path="transactions" element={<TransactionList />} />
      <Route path="transactions/:id" element={<TransactionDetails />} />
      <Route path="fraud-cases" element={<FraudCasesPage />} />
      <Route path="fraud-cases/:id" element={<FraudDetailsPage />} />
      <Route path="alerts" element={<AlertList />} />
      <Route path="analytics" element={<AnalyticsPage />} />
      <Route path="reports" element={<ReportsPage />} />
      <Route path="audit-logs" element={<AuditLogsPage />} />
      <Route path="ml-monitor" element={<MlMonitorPage />} />
      <Route path="security-incidents" element={<SecurityIncidentsPage />} />
      <Route path="notifications" element={<NotificationsPage />} />
      <Route path="profile" element={<Profile />} />
      <Route path="settings" element={<Settings />} />
      <Route path="*" element={<NotFound />} />
    </>
  );
}

function LegacyDetailRedirect({ resource }: { resource: 'transactions' | 'fraud-cases' }) {
  const { id } = useParams();
  return <Navigate to={`/dashboard/${resource}${id ? `/${id}` : ''}`} replace />;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<PrivateRoute />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardLayout />}>
          {dashboardRoutes()}
        </Route>

        {legacyRoutes.map((path) => (
          <Route key={path} path={path} element={<Navigate to={`/dashboard/${path}`} replace />} />
        ))}
        <Route path="transactions/:id" element={<LegacyDetailRedirect resource="transactions" />} />
        <Route path="fraud-cases/:id" element={<LegacyDetailRedirect resource="fraud-cases" />} />
      </Route>
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
