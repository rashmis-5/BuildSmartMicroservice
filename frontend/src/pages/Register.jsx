import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register as iamRegister } from '../api/iam.js';
import { ROLES } from '../utils/roleRouting.js';

export default function Register() {
  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    password: '',
    role: 'PROJECT_MANAGER',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSubmitting(true);
    try {
      await iamRegister(form);
      setSuccess('Account created. An admin will activate your account before you can sign in.');
      setTimeout(() => navigate('/login', { replace: true }), 2500);
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container py-5" style={{ maxWidth: 480 }}>
      <h2 className="mb-4">Create account</h2>
      <form onSubmit={onSubmit}>
        {error && <div className="alert alert-danger py-2">{error}</div>}
        {success && <div className="alert alert-success py-2">{success}</div>}
        <div className="mb-3">
          <label className="form-label">Full name</label>
          <input className="form-control" value={form.name} onChange={update('name')} required />
        </div>
        <div className="mb-3">
          <label className="form-label">Gmail address</label>
          <input
            type="email"
            className="form-control"
            value={form.email}
            onChange={update('email')}
            placeholder="user@gmail.com"
            required
          />
          <div className="form-text">IAM only accepts Gmail addresses.</div>
        </div>
        <div className="mb-3">
          <label className="form-label">Phone</label>
          <input
            className="form-control"
            value={form.phone}
            onChange={update('phone')}
            placeholder="+1234567890"
            required
          />
        </div>
        <div className="mb-3">
          <label className="form-label">Password</label>
          <input
            type="password"
            className="form-control"
            value={form.password}
            onChange={update('password')}
            required
          />
          <div className="form-text">
            6+ chars, at least one uppercase, one lowercase, one number.
          </div>
        </div>
        <div className="mb-3">
          <label className="form-label">Role</label>
          <select className="form-select" value={form.role} onChange={update('role')}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>
        <button className="btn btn-primary w-100" type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create account'}
        </button>
      </form>
      <p className="mt-3 text-center">
        Already registered? <Link to="/login">Sign in</Link>
      </p>
    </div>
  );
}
