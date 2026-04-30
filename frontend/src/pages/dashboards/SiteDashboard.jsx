import { useState } from 'react';
import Tabs from '../../components/Tabs.jsx';
import Section from '../../components/Section.jsx';
import DataTable from '../../components/DataTable.jsx';
import Loader from '../../components/Loader.jsx';
import { ErrorAlert, InfoAlert } from '../../components/Alert.jsx';
import JsonForm from '../../components/JsonForm.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import {
  listSiteLogs, createSiteLog, getSiteLog, latestSiteLog, uploadSiteLogPhoto,
  listIssues, createIssue, updateIssue, issuesByLog,
  siteopsSync, siteopsMyTasks, siteopsMyTasksByProject,
} from '../../api/siteops.js';

function SiteLogsTab() {
  const [projectId, setProjectId] = useState('');
  const { data, loading, error, run } = useAsync(
    () => listSiteLogs(projectId ? { projectId } : {}),
    [projectId],
  );
  const [info, setInfo] = useState('');
  const [photoLogId, setPhotoLogId] = useState('');
  const [photoFile, setPhotoFile] = useState(null);

  const onCreate = async (vals) => { await createSiteLog(vals); setInfo('Site log created'); run(); };
  const onUpload = async () => {
    if (!photoLogId || !photoFile) return;
    try { await uploadSiteLogPhoto(photoLogId, photoFile); setInfo('Photo uploaded'); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };

  return (
    <>
      <Section title="Filter">
        <div className="d-flex gap-2 align-items-center flex-wrap">
          <label className="small mb-0">projectId:</label>
          <input className="form-control form-control-sm" style={{ maxWidth: 200 }} value={projectId} onChange={e => setProjectId(e.target.value)} />
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
          <button className="btn btn-sm btn-outline-info" onClick={async () => {
            if (!projectId) return;
            try { const r = await latestSiteLog(projectId); alert(JSON.stringify(r, null, 2)); }
            catch (e) { alert(e.message); }
          }}>Latest for project</button>
        </div>
      </Section>

      <Section title="Create Site Log" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'projectId', label: 'Project ID', required: true },
            { name: 'logDate', label: 'Log Date', type: 'date', required: true },
            { name: 'submittedBy', label: 'Submitted By' },
            { name: 'activitiesSummary', label: 'Activities Summary', type: 'textarea', col: 'col-12' },
            { name: 'manpowerCount', label: 'Manpower', type: 'number' },
            { name: 'weatherConditions', label: 'Weather' },
            { name: 'remarks', label: 'Remarks', type: 'textarea', col: 'col-12' },
          ]}
          onSubmit={onCreate}
          submitLabel="Create site log"
        />
      </Section>

      <Section title="Upload Photo" collapsible defaultOpen={false}>
        <div className="d-flex gap-2 align-items-end flex-wrap">
          <div>
            <label className="form-label small mb-1">Log ID</label>
            <input className="form-control form-control-sm" value={photoLogId} onChange={e => setPhotoLogId(e.target.value)} />
          </div>
          <div>
            <label className="form-label small mb-1">JPEG file</label>
            <input className="form-control form-control-sm" type="file" accept="image/jpeg" onChange={e => setPhotoFile(e.target.files?.[0])} />
          </div>
          <button className="btn btn-sm btn-primary" onClick={onUpload} disabled={!photoLogId || !photoFile}>Upload</button>
        </div>
      </Section>

      <Section title="Site Logs" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={data || []}
            columns={[
              { key: 'logId', label: 'Log ID' },
              { key: 'projectId', label: 'Project' },
              { key: 'logDate', label: 'Date' },
              { key: 'submittedBy', label: 'Submitted By' },
              { key: 'reviewStatus', label: 'Review' },
              { key: 'activitiesSummary', label: 'Summary' },
            ]}
            actions={(row) => (
              <button className="btn btn-sm btn-outline-secondary" onClick={async () => {
                try { const r = await getSiteLog(row.logId); alert(JSON.stringify(r, null, 2)); }
                catch (e) { alert(e.message); }
              }}>View</button>
            )}
          />
        )}
      </Section>
    </>
  );
}

function IssuesTab() {
  const [projectId, setProjectId] = useState('');
  const { data, loading, error, run } = useAsync(
    () => listIssues(projectId ? { projectId } : {}),
    [projectId],
  );
  const [info, setInfo] = useState('');
  const onCreate = async (vals) => { await createIssue(vals); setInfo('Issue created'); run(); };
  const onUpdate = async (id) => {
    const status = prompt('New status (OPEN/IN_PROGRESS/RESOLVED/CLOSED):');
    if (!status) return;
    try { await updateIssue(id, { status }); setInfo(`Issue ${id} updated`); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  return (
    <>
      <Section title="Filter">
        <div className="d-flex gap-2 align-items-center flex-wrap">
          <label className="small mb-0">projectId:</label>
          <input className="form-control form-control-sm" style={{ maxWidth: 200 }} value={projectId} onChange={e => setProjectId(e.target.value)} />
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </div>
      </Section>

      <Section title="Report Issue" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'projectId', label: 'Project ID', required: true },
            { name: 'logId', label: 'Site Log ID' },
            { name: 'severity', label: 'Severity', type: 'select', options: ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map(s => ({ value: s, label: s })) },
            { name: 'title', label: 'Title', required: true },
            { name: 'description', label: 'Description', type: 'textarea', col: 'col-12' },
            { name: 'reportedBy', label: 'Reported By' },
          ]}
          onSubmit={onCreate}
          submitLabel="Report issue"
        />
      </Section>

      <Section title="Issues" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={data || []}
            columns={[
              { key: 'issueId', label: 'ID' },
              { key: 'projectId', label: 'Project' },
              { key: 'severity', label: 'Severity' },
              { key: 'status', label: 'Status' },
              { key: 'title', label: 'Title' },
              { key: 'reportedBy', label: 'Reported By' },
            ]}
            actions={(row) => (
              <>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => onUpdate(row.issueId)}>Update Status</button>
                <button className="btn btn-sm btn-outline-secondary" onClick={async () => {
                  if (!row.logId) return alert('No site log linked');
                  try { const r = await issuesByLog(row.logId); alert(JSON.stringify(r, null, 2)); }
                  catch (e) { alert(e.message); }
                }}>By Log</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function MyTasksTab() {
  const [status, setStatus] = useState('');
  const { data, loading, error, run } = useAsync(() => siteopsMyTasks(status || null), [status]);
  const [info, setInfo] = useState('');
  const [pid, setPid] = useState('');
  const onSync = async () => {
    try { const r = await siteopsSync(); setInfo(`Synced: ${r.newTasksSynced} new, ${r.alreadyExisted} existed`); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  const onByProject = async () => {
    if (!pid) return;
    try { const r = await siteopsMyTasksByProject(pid); alert(JSON.stringify(r, null, 2)); }
    catch (e) { alert(e.message); }
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
        <button className="btn btn-sm btn-outline-info" onClick={onByProject} disabled={!pid}>Tasks by Project</button>
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

export default function SiteDashboard() {
  return (
    <>
      <h2 className="h4 mb-3">Site Dashboard</h2>
      <Tabs
        tabs={[
          { key: 'logs', label: 'Site Logs', content: () => <SiteLogsTab /> },
          { key: 'issues', label: 'Issues', content: () => <IssuesTab /> },
          { key: 'tasks', label: 'My Tasks', content: () => <MyTasksTab /> },
        ]}
      />
    </>
  );
}
