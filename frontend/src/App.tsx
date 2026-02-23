import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Monitoring from './pages/Monitoring'
import DigitalTwin from './pages/DigitalTwin'
import Repair from './pages/Repair'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token } = useAuthStore()
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/monitoring" 
          element={
            <ProtectedRoute>
              <Monitoring />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/digital-twin" 
          element={
            <ProtectedRoute>
              <DigitalTwin />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/repair" 
          element={
            <ProtectedRoute>
              <Repair />
            </ProtectedRoute>
          } 
        />
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
