import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { dashboardPathFor } from '../utils/roleRouting.js';

export default function RoleLanding() {
  const { role } = useAuth();
  return <Navigate to={dashboardPathFor(role)} replace />;
}
