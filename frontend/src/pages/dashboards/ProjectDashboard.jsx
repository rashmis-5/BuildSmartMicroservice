import { useState } from 'react';
import Tabs from '../../components/Tabs.jsx';
import Section from '../../components/Section.jsx';
import DataTable from '../../components/DataTable.jsx';
import Loader from '../../components/Loader.jsx';
import { ErrorAlert, InfoAlert } from '../../components/Alert.jsx';
import JsonForm from '../../components/JsonForm.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import {
  listProjects, getProject, createProject,
  getProjectTasks, createTask, updateTaskStatus,
  getProjectMilestones, updateMilestoneStatus,
  listApprovals, pendingApprovals, approvalsByProject, approveRequest, rejectRequest, approvalStats,
  listTemplates, getTemplate,
} from '../../api/project.js';
import {
  listResources, createResource, updateResource, deleteResource, getResource, resourcesByType, availableResources,
  listAllocations, createAllocation, updateAllocation, deleteAllocation, getAllocation,
  allocationsByProject, allocationsByResource, allocationCost, allocationEvent,
} from '../../api/resource.js';
import {
  reportResourceUtilization, reportLaborAllocation, reportProjects,
} from '../../api/report.js';

function ProjectsTab() {
  const { data, loading, error, run } = useAsync(listProjects);
  const [info, setInfo] = useState('');
  const onCreate = async (v) => { await createProject(v); setInfo('Project created'); run(); };
  return (
    <>
      <Section title="Create Project" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'name', label: 'Name', required: true },
            { name: 'description', label: 'Description', type: 'textarea', col: 'col-12' },
            { name: 'startDate', label: 'Start', type: 'date', required: true },
            { name: 'endDate', label: 'End', type: 'date', required: true },
            { name: 'budget', label: 'Budget', type: 'number' },
            { name: 'templateId', label: 'Template ID' },
          ]}
          onSubmit={onCreate}
          submitLabel="Create project"
        />
      </Section>
      <Section title="Projects" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={data || []}
            columns={[
              { key: 'projectId', label: 'ID' },
              { key: 'name', label: 'Name' },
              { key: 'status', label: 'Status' },
              { key: 'startDate', label: 'Start' },
              { key: 'endDate', label: 'End' },
              { key: 'budget', label: 'Budget' },
            ]}
            actions={(r) => (
              <button className="btn btn-sm btn-outline-secondary" onClick={async () => alert(JSON.stringify(await getProject(r.projectId), null, 2))}>View</button>
            )}
          />
        )}
      </Section>
    </>
  );
}

function TasksTab() {
  const [pid, setPid] = useState('');
  const [tasks, setTasks] = useState([]);
  const [err, setErr] = useState('');
  const [info, setInfo] = useState('');
  const fetch = async () => {
    setErr(''); if (!pid) return;
    try { setTasks(await getProjectTasks(pid)); } catch (e) { setErr(e.message); }
  };
  const onCreate = async (v) => { await createTask(pid, v); setInfo('Task created'); fetch(); };
  const onStatus = async (taskId) => {
    const s = prompt('Status (PENDING/IN_PROGRESS/SUBMITTED/APPROVED/REJECTED):');
    if (!s) return;
    try { await updateTaskStatus(taskId, s); setInfo('Updated'); fetch(); }
    catch (e) { setInfo(e.message); }
  };
  return (
    <>
      <Section title="Project Selection">
        <div className="d-flex gap-2 flex-wrap">
          <input className="form-control form-control-sm" style={{ maxWidth: 240 }} placeholder="Project ID" value={pid} onChange={e => setPid(e.target.value)} />
          <button className="btn btn-sm btn-primary" onClick={fetch}>Load Tasks</button>
        </div>
      </Section>
      {pid && (
        <Section title={`Create Task for ${pid}`} collapsible defaultOpen={false}>
          <JsonForm
            fields={[
              { name: 'title', label: 'Title', required: true },
              { name: 'description', label: 'Description', type: 'textarea', col: 'col-12' },
              { name: 'assignedTo', label: 'Assigned To (userId)', required: true },
              { name: 'startDate', label: 'Start', type: 'date' },
              { name: 'endDate', label: 'End', type: 'date' },
              { name: 'priority', label: 'Priority', type: 'select', options: ['LOW', 'MEDIUM', 'HIGH'].map(p => ({ value: p, label: p })) },
            ]}
            onSubmit={onCreate}
            submitLabel="Create task"
          />
        </Section>
      )}
      <Section title="Tasks">
        <ErrorAlert message={err} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        <DataTable
          rows={tasks}
          columns={[
            { key: 'taskId', label: 'ID' },
            { key: 'title', label: 'Title' },
            { key: 'assignedTo', label: 'Assigned' },
            { key: 'status', label: 'Status' },
            { key: 'priority', label: 'Priority' },
          ]}
          actions={(r) => <button className="btn btn-sm btn-outline-primary" onClick={() => onStatus(r.taskId)}>Status</button>}
        />
      </Section>
    </>
  );
}

function MilestonesTab() {
  const [pid, setPid] = useState('');
  const [milestones, setMilestones] = useState([]);
  const [info, setInfo] = useState('');
  const fetch = async () => { if (pid) setMilestones(await getProjectMilestones(pid)); };
  const onStatus = async (id) => {
    const s = prompt('New status (PENDING/IN_PROGRESS/COMPLETED):');
    if (!s) return;
    try { await updateMilestoneStatus(id, s); setInfo('Updated'); fetch(); }
    catch (e) { setInfo(e.message); }
  };
  return (
    <>
      <Section title="Project Selection">
        <div className="d-flex gap-2">
          <input className="form-control form-control-sm" style={{ maxWidth: 240 }} placeholder="Project ID" value={pid} onChange={e => setPid(e.target.value)} />
          <button className="btn btn-sm btn-primary" onClick={fetch}>Load Milestones</button>
        </div>
      </Section>
      <Section title="Milestones">
        <InfoAlert message={info} onClose={() => setInfo('')} />
        <DataTable
          rows={milestones}
          columns={[
            { key: 'milestoneId', label: 'ID' },
            { key: 'title', label: 'Title' },
            { key: 'dueDate', label: 'Due' },
            { key: 'status', label: 'Status' },
          ]}
          actions={(r) => <button className="btn btn-sm btn-outline-primary" onClick={() => onStatus(r.milestoneId)}>Status</button>}
        />
      </Section>
    </>
  );
}

function ApprovalsTab() {
  const [view, setView] = useState('pending');
  const [pid, setPid] = useState('');
  const fetcher = async () => {
    if (view === 'pending') return pendingApprovals();
    if (view === 'project' && pid) return approvalsByProject(pid);
    return listApprovals();
  };
  const { data, loading, error, run } = useAsync(fetcher, [view, pid]);
  const stats = useAsync(approvalStats);
  const [info, setInfo] = useState('');
  const onApprove = async (id) => { try { await approveRequest(id); setInfo('Approved'); run(); } catch (e) { setInfo(e.message); } };
  const onReject = async (id) => {
    const reason = prompt('Reason:');
    if (!reason) return;
    try { await rejectRequest(id, reason); setInfo('Rejected'); run(); }
    catch (e) { setInfo(e.message); }
  };
  return (
    <>
      <Section title="Stats" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => stats.run()}>Refresh</button>}>
        {stats.loading ? <Loader inline /> : <pre className="bg-light p-2 small mb-0">{JSON.stringify(stats.data, null, 2)}</pre>}
      </Section>
      <Section title="Filter">
        <div className="d-flex gap-2 flex-wrap">
          <select className="form-select form-select-sm" style={{ maxWidth: 200 }} value={view} onChange={e => setView(e.target.value)}>
            <option value="pending">Pending</option>
            <option value="all">All</option>
            <option value="project">By Project</option>
          </select>
          {view === 'project' && (
            <input className="form-control form-control-sm" style={{ maxWidth: 240 }} value={pid} onChange={e => setPid(e.target.value)} placeholder="Project ID" />
          )}
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </div>
      </Section>
      <Section title="Approvals">
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={data || []}
            columns={[
              { key: 'approvalId', label: 'ID' },
              { key: 'requestType', label: 'Type' },
              { key: 'projectId', label: 'Project' },
              { key: 'requestedBy', label: 'By' },
              { key: 'status', label: 'Status' },
              { key: 'description', label: 'Description' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-success me-1" onClick={() => onApprove(r.approvalId)}>Approve</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onReject(r.approvalId)}>Reject</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function ResourcesTab() {
  const { data, loading, error, run } = useAsync(listResources);
  const [info, setInfo] = useState('');
  const [filterType, setFilterType] = useState('');
  const onCreate = async (v) => { await createResource(v); setInfo('Resource created'); run(); };
  const onDelete = async (id) => { if (!confirm(`Delete ${id}?`)) return; try { await deleteResource(id); setInfo('Deleted'); run(); } catch (e) { setInfo(e.message); } };
  const onByType = async () => {
    if (!filterType) return;
    try { alert(JSON.stringify(await resourcesByType(filterType), null, 2)); }
    catch (e) { alert(e.message); }
  };
  const onAvail = async () => { try { alert(JSON.stringify(await availableResources(), null, 2)); } catch (e) { alert(e.message); } };
  return (
    <>
      <Section title="Filter">
        <div className="d-flex gap-2 flex-wrap">
          <input className="form-control form-control-sm" style={{ maxWidth: 200 }} placeholder="Type (LABOR/EQUIPMENT)" value={filterType} onChange={e => setFilterType(e.target.value)} />
          <button className="btn btn-sm btn-outline-info" onClick={onByType} disabled={!filterType}>By Type</button>
          <button className="btn btn-sm btn-outline-info" onClick={onAvail}>Available Only</button>
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </div>
      </Section>
      <Section title="Add Resource" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'type', label: 'Type', type: 'select', options: ['LABOR', 'EQUIPMENT'].map(t => ({ value: t, label: t })), required: true },
            { name: 'availability', label: 'Availability', type: 'select', options: ['AVAILABLE', 'IN_USE', 'UNAVAILABLE'].map(a => ({ value: a, label: a })) },
            { name: 'numberOfLabors', label: 'Number of Labors', type: 'number' },
            { name: 'skillLevel', label: 'Skill Level' },
            { name: 'equipmentName', label: 'Equipment Name' },
            { name: 'equipmentLevel', label: 'Equipment Level' },
            { name: 'costPerHour', label: 'Cost per Hour', type: 'number' },
            { name: 'totalCost', label: 'Total Cost', type: 'number' },
          ]}
          onSubmit={onCreate}
          submitLabel="Add resource"
        />
      </Section>
      <Section title="Resources">
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={data || []}
            columns={[
              { key: 'resourceId', label: 'ID' },
              { key: 'type', label: 'Type' },
              { key: 'availability', label: 'Availability' },
              { key: 'skillLevel', label: 'Skill' },
              { key: 'equipmentName', label: 'Equipment' },
              { key: 'costPerHour', label: 'Cost/hr' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getResource(r.resourceId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.resourceId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function AllocationsTab() {
  const { data, loading, error, run } = useAsync(listAllocations);
  const [info, setInfo] = useState('');
  const onCreate = async (v) => {
    const body = {
      projectId: v.projectId,
      resource: { resourceId: v.resourceId },
      assignedDate: v.assignedDate,
      releasedDate: v.releasedDate,
      status: v.status || 'ACTIVE',
    };
    await createAllocation(body); setInfo('Allocation created'); run();
  };
  const onDelete = async (id) => { if (!confirm(`Delete ${id}?`)) return; try { await deleteAllocation(id); setInfo('Deleted'); run(); } catch (e) { setInfo(e.message); } };
  return (
    <>
      <Section title="Allocate Resource" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'projectId', label: 'Project ID', required: true },
            { name: 'resourceId', label: 'Resource ID', required: true },
            { name: 'assignedDate', label: 'Assigned Date', type: 'date', required: true },
            { name: 'releasedDate', label: 'Released Date', type: 'date' },
            { name: 'status', label: 'Status', type: 'select', options: ['ACTIVE', 'COMPLETED', 'CANCELLED'].map(s => ({ value: s, label: s })) },
          ]}
          onSubmit={onCreate}
          submitLabel="Allocate"
        />
      </Section>
      <Section title="Allocations" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={data || []}
            columns={[
              { key: 'allocationId', label: 'ID' },
              { key: 'projectId', label: 'Project' },
              { key: 'status', label: 'Status' },
              { key: 'assignedDate', label: 'Assigned' },
              { key: 'releasedDate', label: 'Released' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getAllocation(r.allocationId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-info me-1" onClick={async () => alert(JSON.stringify(await allocationCost(r.allocationId), null, 2))}>Cost</button>
                <button className="btn btn-sm btn-outline-info me-1" onClick={async () => alert(JSON.stringify(await allocationEvent(r.allocationId), null, 2))}>Event</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.allocationId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function TemplatesTab() {
  const { data, loading, error, run } = useAsync(listTemplates);
  return (
    <Section title="Templates" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
      <ErrorAlert message={error} />
      {loading ? <Loader /> : (
        <DataTable
          rows={data || []}
          columns={[
            { key: 'templateId', label: 'ID' },
            { key: 'name', label: 'Name' },
            { key: 'description', label: 'Description' },
          ]}
          actions={(r) => <button className="btn btn-sm btn-outline-secondary" onClick={async () => alert(JSON.stringify(await getTemplate(r.templateId), null, 2))}>View</button>}
        />
      )}
    </Section>
  );
}

function ReportsTab() {
  const util = useAsync(reportResourceUtilization);
  const labor = useAsync(reportLaborAllocation);
  const projs = useAsync(reportProjects);
  return (
    <>
      <Section title="Resource Utilization" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => util.run()}>Refresh</button>}>
        <ErrorAlert message={util.error} />
        {util.loading ? <Loader /> : <pre className="bg-light p-2 small mb-0">{JSON.stringify(util.data, null, 2)}</pre>}
      </Section>
      <Section title="Labor Allocation" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => labor.run()}>Refresh</button>}>
        <ErrorAlert message={labor.error} />
        {labor.loading ? <Loader /> : <pre className="bg-light p-2 small mb-0">{JSON.stringify(labor.data, null, 2)}</pre>}
      </Section>
      <Section title="Projects (Analytics)" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => projs.run()}>Refresh</button>}>
        <ErrorAlert message={projs.error} />
        {projs.loading ? <Loader /> : <pre className="bg-light p-2 small mb-0">{JSON.stringify(projs.data, null, 2)}</pre>}
      </Section>
    </>
  );
}

export default function ProjectDashboard() {
  return (
    <>
      <h2 className="h4 mb-3">Project Dashboard</h2>
      <Tabs
        tabs={[
          { key: 'projects', label: 'Projects', content: () => <ProjectsTab /> },
          { key: 'tasks', label: 'Tasks', content: () => <TasksTab /> },
          { key: 'milestones', label: 'Milestones', content: () => <MilestonesTab /> },
          { key: 'approvals', label: 'Approvals', content: () => <ApprovalsTab /> },
          { key: 'resources', label: 'Resources', content: () => <ResourcesTab /> },
          { key: 'allocations', label: 'Allocations', content: () => <AllocationsTab /> },
          { key: 'templates', label: 'Templates', content: () => <TemplatesTab /> },
          { key: 'reports', label: 'Reports', content: () => <ReportsTab /> },
        ]}
      />
    </>
  );
}
