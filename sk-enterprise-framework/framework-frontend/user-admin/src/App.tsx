import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ToastProvider } from './components/Toast';
import { DashboardPage } from './pages/DashboardPage';
import { UserListPage } from './pages/UserListPage';

export default function App() {
  return (
    <ToastProvider>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/users" element={<UserListPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </ToastProvider>
  );
}
