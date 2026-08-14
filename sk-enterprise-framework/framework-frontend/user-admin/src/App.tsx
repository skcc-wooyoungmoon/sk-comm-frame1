import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ToastProvider } from './components/Toast';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { DashboardPage } from './pages/DashboardPage';
import { UserListPage } from './pages/UserListPage';
import { ProductsPage } from './pages/ProductsPage';
import { OrdersPage } from './pages/OrdersPage';
import { MonitoringPage } from './pages/MonitoringPage';
import { LogsPage } from './pages/LogsPage';
import { LoginPage } from './pages/LoginPage';

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<ProtectedRoute permission="dashboard:view"><DashboardPage /></ProtectedRoute>} />
            <Route path="/users" element={<ProtectedRoute permission="user:view"><UserListPage /></ProtectedRoute>} />
            <Route path="/products" element={<ProtectedRoute permission="product:view"><ProductsPage /></ProtectedRoute>} />
            <Route path="/orders" element={<ProtectedRoute permission="order:view"><OrdersPage /></ProtectedRoute>} />
            <Route path="/monitoring" element={<ProtectedRoute permission="monitoring:view"><MonitoringPage /></ProtectedRoute>} />
            <Route path="/logs" element={<ProtectedRoute permission="log:view"><LogsPage /></ProtectedRoute>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </ToastProvider>
  );
}
