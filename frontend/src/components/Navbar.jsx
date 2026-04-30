import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { dashboardPathFor } from '../utils/roleRouting.js';
import { logout as iamLogout } from '../api/iam.js';
import NotificationBell from './NotificationBell.jsx';

export default function Navbar() {
  const { name, role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await iamLogout();
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <nav className="navbar navbar-expand navbar-dark bg-dark px-3">
      <Link className="navbar-brand" to={dashboardPathFor(role)}>
        BuildSmart
      </Link>
      <div className="ms-auto d-flex align-items-center">
        <NotificationBell />
        <span className="text-white-50 me-3 d-none d-md-inline">
          {name} <span className="badge bg-secondary ms-1">{role}</span>
        </span>
        <button className="btn btn-outline-light btn-sm" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </nav>
  );
}
