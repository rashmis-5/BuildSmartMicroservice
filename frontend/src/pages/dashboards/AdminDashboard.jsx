import { useState } from 'react';
import Tabs from '../../components/Tabs.jsx';
import Section from '../../components/Section.jsx';
import DataTable from '../../components/DataTable.jsx';
import Loader from '../../components/Loader.jsx';
import { ErrorAlert, InfoAlert } from '../../components/Alert.jsx';
import JsonForm from '../../components/JsonForm.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import {
  adminAllUsers,
  adminPendingUsers,
  adminApproveUser,
  adminRejectUser,
  adminGetUser,
  adminUpdateUser,
  adminDeleteUser,
  adminUsersByRole,
  auditLogs,
  auditLogsByUser,
  auditLogsByAction,
} from '../../api/iam.js';
import { ROLES } from '../../utils/roleRouting.js';

const userColumns = [
  { key: 'userId', label: 'ID' },
  { key: 'name', label: 'Name' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Phone' },
  { key: 'role', label: 'Role' },
  { key: 'status', label: 'Status' },
];

function PendingUsers() {
  const { data, loading, error, run } = useAsync(adminPendingUsers);
  const [info, setInfo] = useState('');
  const onApprove = async (id) => {
    try { await adminApproveUser(id); setInfo(`Approved ${id}`); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  const onReject = async (id) => {
    if (!confirm(`Reject ${id}?`)) return;
    try { await adminRejectUser(id); setInfo(`Rejected ${id}`); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  return (
    <Section
      title="Pending Users"
      actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}
    >
      <ErrorAlert message={error} />
      <InfoAlert message={info} onClose={() => setInfo('')} />
      {loading ? <Loader /> : (
        <DataTable
          rows={data?.data || data || []}
          columns={userColumns}
          actions={(u) => (
            <>
              <button className="btn btn-sm btn-success me-1" onClick={() => onApprove(u.userId)}>Approve</button>
              <button className="btn btn-sm btn-outline-danger" onClick={() => onReject(u.userId)}>Reject</button>
            </>
          )}
        />
      )}
    </Section>
  );
}

function AllUsers() {
  const { data, loading, error, run } = useAsync(adminAllUsers);
  const [info, setInfo] = useState('');
  const [edit, setEdit] = useState(null);
  const list = data?.data || data || [];
  const onDelete = async (id) => {
    if (!confirm(`Delete user ${id}?`)) return;
    try { await adminDeleteUser(id); setInfo(`Deleted ${id}`); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  const onUpdate = async (vals) => {
    await adminUpdateUser(edit.userId, vals);
    setInfo(`Updated ${edit.userId}`);
    setEdit(null);
    run();
  };
  return (
    <>
      <Section
        title="All Users"
        actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}
      >
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={list}
            columns={userColumns}
            actions={(u) => (
              <>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => setEdit(u)}>Edit</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(u.userId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
      {edit && (
        <Section title={`Edit User ${edit.userId}`} actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => setEdit(null)}>Close</button>}>
          <JsonForm
            initialValues={edit}
            fields={[
              { name: 'name', label: 'Name' },
              { name: 'email', label: 'Email' },
              { name: 'phone', label: 'Phone' },
              { name: 'role', label: 'Role', type: 'select', options: ROLES.map(r => ({ value: r, label: r })) },
              { name: 'status', label: 'Status', type: 'select', options: ['ACTIVE', 'PENDING', 'INACTIVE'].map(s => ({ value: s, label: s })) },
            ]}
            onSubmit={onUpdate}
            submitLabel="Update"
          />
        </Section>
      )}
    </>
  );
}

function UsersByRole() {
  const [role, setRole] = useState('PROJECT_MANAGER');
  const { data, loading, error, run } = useAsync(() => adminUsersByRole(role), [role]);
  return (
    <Section
      title="Users by Role"
      actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}
    >
      <div className="mb-2 d-flex align-items-center gap-2">
        <label className="small mb-0">Role:</label>
        <select className="form-select form-select-sm" style={{ maxWidth: 200 }} value={role} onChange={e => setRole(e.target.value)}>
          {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
        </select>
      </div>
      <ErrorAlert message={error} />
      {loading ? <Loader /> : <DataTable rows={data?.data || data || []} columns={userColumns} />}
    </Section>
  );
}

function GetUserById() {
  const [id, setId] = useState('');
  const [user, setUser] = useState(null);
  const [err, setErr] = useState('');
  const fetch = async () => {
    setErr('');
    try { setUser(await adminGetUser(id)); }
    catch (e) { setErr(e?.response?.data?.message || e.message); setUser(null); }
  };
  return (
    <Section title="Get User by ID">
      <div className="d-flex gap-2 mb-2">
        <input className="form-control form-control-sm" placeholder="userId" value={id} onChange={e => setId(e.target.value)} />
        <button className="btn btn-sm btn-primary" onClick={fetch}>Fetch</button>
      </div>
      <ErrorAlert message={err} />
      {user && <pre className="bg-light p-2 small">{JSON.stringify(user, null, 2)}</pre>}
    </Section>
  );
}

function AuditLogs() {
  const [tab, setTab] = useState('all');
  const [userId, setUserId] = useState('');
  const [action, setAction] = useState('');
  const [data, setData] = useState(null);
  const [err, setErr] = useState('');
  const [loading, setLoading] = useState(false);
  const run = async () => {
    setErr(''); setLoading(true);
    try {
      let r;
      if (tab === 'user' && userId) r = await auditLogsByUser(userId, { page: 0, size: 25 });
      else if (tab === 'action' && action) r = await auditLogsByAction(action, { page: 0, size: 25 });
      else r = await auditLogs({ page: 0, size: 25 });
      setData(r);
    } catch (e) { setErr(e?.response?.data?.message || e.message); }
    finally { setLoading(false); }
  };
  return (
    <Section
      title="Audit Logs"
      actions={<button className="btn btn-sm btn-outline-secondary" onClick={run}>Fetch</button>}
    >
      <div className="d-flex gap-2 mb-2 flex-wrap">
        <select className="form-select form-select-sm" style={{ maxWidth: 180 }} value={tab} onChange={e => setTab(e.target.value)}>
          <option value="all">All</option>
          <option value="user">By user</option>
          <option value="action">By action</option>
        </select>
        {tab === 'user' && (
          <input className="form-control form-control-sm" style={{ maxWidth: 200 }} placeholder="userId" value={userId} onChange={e => setUserId(e.target.value)} />
        )}
        {tab === 'action' && (
          <input className="form-control form-control-sm" style={{ maxWidth: 200 }} placeholder="ACTION_NAME" value={action} onChange={e => setAction(e.target.value)} />
        )}
      </div>
      <ErrorAlert message={err} />
      {loading ? <Loader /> : data ? (
        <DataTable
          rows={data?.content || data?.data?.content || data?.data || data || []}
          columns={[
            { key: 'id', label: 'ID' },
            { key: 'userId', label: 'User' },
            { key: 'action', label: 'Action' },
            { key: 'description', label: 'Description' },
            { key: 'timestamp', label: 'When' },
          ]}
        />
      ) : null}
    </Section>
  );
}

export default function AdminDashboard() {
  return (
    <>
      <h2 className="h4 mb-3">Admin Dashboard</h2>
      <Tabs
        tabs={[
          { key: 'pending', label: 'Pending Users', content: () => <PendingUsers /> },
          { key: 'users', label: 'All Users', content: () => <AllUsers /> },
          { key: 'by-role', label: 'Users by Role', content: () => <UsersByRole /> },
          { key: 'lookup', label: 'User Lookup', content: () => <GetUserById /> },
          { key: 'audit', label: 'Audit Logs', content: () => <AuditLogs /> },
        ]}
      />
    </>
  );
}
