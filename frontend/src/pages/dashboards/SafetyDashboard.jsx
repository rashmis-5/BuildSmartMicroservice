import { useState } from 'react';
import Tabs from '../../components/Tabs.jsx';
import Section from '../../components/Section.jsx';
import DataTable from '../../components/DataTable.jsx';
import Loader from '../../components/Loader.jsx';
import { ErrorAlert, InfoAlert } from '../../components/Alert.jsx';
import JsonForm from '../../components/JsonForm.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import {
  listIncidents, getIncident, createIncident, updateIncidentStatus, deleteIncident,
  listInspections, getInspection, createInspection, updateInspectionStatus, deleteInspection, inspectionTypes,
  safetySync, safetyMyTasks, safetyMyTasksByProject,
} from '../../api/safety.js';
import {
  reportSafetyTrends, reportInspectionsSummary,
} from '../../api/report.js';

function IncidentsTab() {
  const [filters, setFilters] = useState({ projectId: '', status: '', severity: '' });
  const params = Object.fromEntries(Object.entries(filters).filter(([, v]) => v));
  const { data, loading, error, run } = useAsync(() => listIncidents(params), [JSON.stringify(params)]);
  const [info, setInfo] = useState('');
  const list = data?.content || data || [];
  const onCreate = async (v) => { await createIncident(v); setInfo('Incident created'); run(); };
  const onStatus = async (id) => {
    const s = prompt('New status (OPEN/UNDER_INVESTIGATION/RESOLVED):');
    if (!s) return;
    try { await updateIncidentStatus(id, s); setInfo('Updated'); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  const onDelete = async (id) => {
    if (!confirm(`Delete ${id}?`)) return;
    try { await deleteIncident(id); setInfo('Deleted'); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  return (
    <>
      <Section title="Filters">
        <div className="d-flex gap-2 flex-wrap">
          {['projectId', 'status', 'severity'].map(f => (
            <input key={f} className="form-control form-control-sm" style={{ maxWidth: 180 }} placeholder={f} value={filters[f]} onChange={e => setFilters({ ...filters, [f]: e.target.value })} />
          ))}
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </div>
      </Section>
      <Section title="Report Incident" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'projectId', label: 'Project ID', required: true },
            { name: 'taskId', label: 'Assigned Task ID' },
            { name: 'severity', label: 'Severity', type: 'select', options: ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map(s => ({ value: s, label: s })), required: true },
            { name: 'incidentDate', label: 'Date', type: 'date', required: true },
            { name: 'location', label: 'Location' },
            { name: 'description', label: 'Description', type: 'textarea', col: 'col-12', required: true },
          ]}
          onSubmit={onCreate}
          submitLabel="Report incident"
        />
      </Section>
      <Section title="Incidents" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={list}
            columns={[
              { key: 'incidentId', label: 'ID' },
              { key: 'projectId', label: 'Project' },
              { key: 'severity', label: 'Severity' },
              { key: 'status', label: 'Status' },
              { key: 'incidentDate', label: 'Date' },
              { key: 'description', label: 'Description' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getIncident(r.incidentId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => onStatus(r.incidentId)}>Status</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.incidentId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function InspectionsTab() {
  const [filters, setFilters] = useState({ projectId: '', status: '' });
  const params = Object.fromEntries(Object.entries(filters).filter(([, v]) => v));
  const { data, loading, error, run } = useAsync(() => listInspections(params), [JSON.stringify(params)]);
  const types = useAsync(inspectionTypes);
  const [info, setInfo] = useState('');
  const list = data?.content || data || [];
  const onCreate = async (v) => { await createInspection(v); setInfo('Inspection created'); run(); };
  const onStatus = async (id) => {
    const s = prompt('New status (SCHEDULED/IN_PROGRESS/COMPLETED/FAILED):');
    if (!s) return;
    try { await updateInspectionStatus(id, s); setInfo('Updated'); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  const onDelete = async (id) => {
    if (!confirm(`Delete ${id}?`)) return;
    try { await deleteInspection(id); setInfo('Deleted'); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  return (
    <>
      <Section title="Filters">
        <div className="d-flex gap-2 flex-wrap">
          {Object.keys(filters).map(f => (
            <input key={f} className="form-control form-control-sm" style={{ maxWidth: 180 }} placeholder={f} value={filters[f]} onChange={e => setFilters({ ...filters, [f]: e.target.value })} />
          ))}
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </div>
      </Section>
      <Section title="Schedule Inspection" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'projectId', label: 'Project ID', required: true },
            { name: 'taskId', label: 'Assigned Task ID', required: true },
            { name: 'inspectionType', label: 'Type', type: 'select', options: (types.data || []).map(t => ({ value: t, label: t })) },
            { name: 'inspectionDate', label: 'Inspection Date', type: 'date', required: true },
            { name: 'inspector', label: 'Inspector' },
            { name: 'notes', label: 'Notes', type: 'textarea', col: 'col-12' },
          ]}
          onSubmit={onCreate}
          submitLabel="Schedule"
        />
      </Section>
      <Section title="Inspections" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={list}
            columns={[
              { key: 'inspectionId', label: 'ID' },
              { key: 'projectId', label: 'Project' },
              { key: 'inspectionType', label: 'Type' },
              { key: 'status', label: 'Status' },
              { key: 'inspectionDate', label: 'Date' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getInspection(r.inspectionId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => onStatus(r.inspectionId)}>Status</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.inspectionId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function TasksTab() {
  const [status, setStatus] = useState('');
  const { data, loading, error, run } = useAsync(() => safetyMyTasks(status || null), [status]);
  const [info, setInfo] = useState('');
  const [pid, setPid] = useState('');
  const onSync = async () => {
    try { const r = await safetySync(); setInfo(`Synced: ${r.newTasksSynced} new, ${r.alreadyExisted} existed`); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  return (
    <Section
      title="My Tasks"
      actions={
        <>
          <button className="btn btn-sm btn-success me-1" onClick={onSync}>Sync from PM</button>
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </>
      }
    >
      <div className="d-flex gap-2 mb-2 flex-wrap">
        <select className="form-select form-select-sm" style={{ maxWidth: 180 }} value={status} onChange={e => setStatus(e.target.value)}>
          <option value="">All</option>
          <option value="PENDING">PENDING</option>
          <option value="COMPLETED">COMPLETED</option>
        </select>
        <input className="form-control form-control-sm" style={{ maxWidth: 220 }} placeholder="By project ID" value={pid} onChange={e => setPid(e.target.value)} />
        <button className="btn btn-sm btn-outline-info" onClick={async () => { if (pid) alert(JSON.stringify(await safetyMyTasksByProject(pid), null, 2)); }}>Tasks by Project</button>
      </div>
      <ErrorAlert message={error} />
      <InfoAlert message={info} onClose={() => setInfo('')} />
      {loading ? <Loader /> : (
        <DataTable
          rows={data || []}
          columns={[
            { key: 'id', label: 'Local ID' },
            { key: 'pmTaskId', label: 'PM Task' },
            { key: 'projectId', label: 'Project' },
            { key: 'description', label: 'Description' },
            { key: 'status', label: 'Status' },
            { key: 'syncedAt', label: 'Synced' },
          ]}
        />
      )}
    </Section>
  );
}

function ReportsTab() {
  const trends = useAsync(reportSafetyTrends);
  const summary = useAsync(reportInspectionsSummary);
  return (
    <>
      <Section title="Safety Trends" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => trends.run()}>Refresh</button>}>
        <ErrorAlert message={trends.error} />
        {trends.loading ? <Loader /> : <pre className="bg-light p-2 small mb-0">{JSON.stringify(trends.data, null, 2)}</pre>}
      </Section>
      <Section title="Inspections Summary" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => summary.run()}>Refresh</button>}>
        <ErrorAlert message={summary.error} />
        {summary.loading ? <Loader /> : <pre className="bg-light p-2 small mb-0">{JSON.stringify(summary.data, null, 2)}</pre>}
      </Section>
    </>
  );
}

export default function SafetyDashboard() {
  return (
    <>
      <h2 className="h4 mb-3">Safety Dashboard</h2>
      <Tabs
        tabs={[
          { key: 'tasks', label: 'My Tasks', content: () => <TasksTab /> },
          { key: 'inspections', label: 'Inspections', content: () => <InspectionsTab /> },
          { key: 'incidents', label: 'Incidents', content: () => <IncidentsTab /> },
          { key: 'reports', label: 'Reports', content: () => <ReportsTab /> },
        ]}
      />
    </>
  );
}
