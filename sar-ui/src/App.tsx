import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './lib/queryClient';
import { loadCredentials } from './lib/auth';
import { AppShell } from './components/layout/AppShell';
import { LoginPage } from './pages/LoginPage';
import { SarListPage } from './pages/SarListPage';
import { SarDetailPage } from './pages/SarDetailPage';
import { SarCreatePage } from './pages/SarCreatePage';
import { SarFormPage } from './pages/SarFormPage';
import { EfilingPage } from './pages/EfilingPage';

function RequireAuth({ children }: { children: React.ReactNode }) {
  return loadCredentials() ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<Navigate to="/sar" replace />} />
          <Route element={<RequireAuth><AppShell><Outlet /></AppShell></RequireAuth>}>
            <Route path="/sar" element={<SarListPage />} />
            <Route path="/sar/new" element={<SarCreatePage />} />
            <Route path="/sar/:id" element={<SarDetailPage />} />
            <Route path="/sar/:id/form" element={<SarFormPage />} />
            <Route path="/efiling" element={<EfilingPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/sar" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

import { Outlet } from 'react-router-dom';
