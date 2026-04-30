import { Outlet } from 'react-router-dom';
import Navbar from './Navbar.jsx';

export default function Layout() {
  return (
    <>
      <Navbar />
      <main className="container-fluid py-3 px-md-4">
        <Outlet />
      </main>
    </>
  );
}
