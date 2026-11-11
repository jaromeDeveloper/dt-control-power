import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './authContext'
import { ProtectedRoute } from './ProtectedRoute'
import { RoleRoute } from './RoleRoute'
import { AppShell } from '../components/organisms/AppShell'
import { DashboardPage } from '../pages/DashboardPage'
import { LoginPage } from '../pages/LoginPage'
import { ProcessDetailPage } from '../pages/ProcessDetailPage'
import { ProcessesPage } from '../pages/ProcessesPage'
import { UploadPage } from '../pages/UploadPage'
import { AuditPage } from '../pages/AuditPage'

const AUDIT_ROLES = ['ADMIN', 'SUPERVISOR', 'AUDITOR'] as const

export function AppRouter() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<AnonymousOnlyLogin />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route index element={<DashboardPage />} />
              <Route path="upload" element={<AuthorizedUpload />} />
              <Route path="processes" element={<ProcessesPage />} />
              <Route path="processes/:id" element={<ProcessDetailPage />} />
              <Route element={<RoleRoute allowed={AUDIT_ROLES} />}>
                <Route path="audit" element={<AuditPage />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

function AuthorizedUpload() {
  const auth = useAuth()

  if (auth.status === 'authenticated' && auth.user.role === 'AUDITOR') {
    return <Navigate to="/processes" replace />
  }

  return <UploadPage />
}

function AnonymousOnlyLogin() {
  const auth = useAuth()

  if (auth.status === 'loading') {
    return (
      <main className="centered-state" aria-live="polite">
        <p>Validando sesión…</p>
      </main>
    )
  }

  if (auth.status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  return <LoginPage />
}
