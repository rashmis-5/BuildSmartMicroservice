import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import PrivateRoute from './auth/PrivateRoute.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import NotFound from './pages/NotFound.jsx';
import AdminDashboard from './pages/dashboards/AdminDashboard.jsx';
import ProjectDashboard from './pages/dashboards/ProjectDashboard.jsx';
import SafetyDashboard from './pages/dashboards/SafetyDashboard.jsx';
import FinanceDashboard from './pages/dashboards/FinanceDashboard.jsx';
import VendorDashboard from './pages/dashboards/VendorDashboard.jsx';
import SiteDashboard from './pages/dashboards/SiteDashboard.jsx';
import RoleLanding from './pages/RoleLanding.jsx';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<PrivateRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<RoleLanding />} />
          <Route
            path="/dashboard/admin"
            element={<PrivateRoute roles={['ADMIN']} />}
          >
            <Route index element={<AdminDashboard />} />
          </Route>
          <Route
            path="/dashboard/project"
            element={<PrivateRoute roles={['PROJECT_MANAGER', 'ADMIN']} />}
          >
            <Route index element={<ProjectDashboard />} />
          </Route>
          <Route
            path="/dashboard/safety"
            element={<PrivateRoute roles={['SAFETY_OFFICER', 'ADMIN']} />}
          >
            <Route index element={<SafetyDashboard />} />
          </Route>
          <Route
            path="/dashboard/finance"
            element={<PrivateRoute roles={['FINANCE_OFFICER', 'ADMIN']} />}
          >
            <Route index element={<FinanceDashboard />} />
          </Route>
          <Route
            path="/dashboard/vendor"
            element={<PrivateRoute roles={['VENDOR', 'ADMIN']} />}
          >
            <Route index element={<VendorDashboard />} />
          </Route>
          <Route
            path="/dashboard/site"
            element={<PrivateRoute roles={['SITE_ENGINEER', 'ADMIN']} />}
          >
            <Route index element={<SiteDashboard />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
