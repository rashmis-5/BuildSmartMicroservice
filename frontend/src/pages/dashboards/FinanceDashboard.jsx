import { useState } from 'react';
import Tabs from '../../components/Tabs.jsx';
import Section from '../../components/Section.jsx';
import DataTable from '../../components/DataTable.jsx';
import Loader from '../../components/Loader.jsx';
import { ErrorAlert, InfoAlert } from '../../components/Alert.jsx';
import JsonForm from '../../components/JsonForm.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import {
  createBudget, getBudget, budgetsByProject, submitBudget, approveBudget, budgetsByStatus, updateBudget, deleteBudget,
  createExpense, getExpense, expensesByBudget, expensesByProject, submitExpense, approveExpense, expensesByStatus, updateExpense, deleteExpense,
  createPayment, getPayment, paymentsByExpense, paymentsByStatus, updatePaymentStatus, pendingPayments,
  financeSync, financeMyTasks, financeMyTasksByProject,
} from '../../api/finance.js';
import { reportBudgetVariance, reportCashFlow } from '../../api/report.js';

function unpaged(r) { return r?.content || r?.data?.content || r?.data || r || []; }

function BudgetsTab() {
  const [filterType, setFilterType] = useState('status');
  const [filterValue, setFilterValue] = useState('DRAFT');
  const fetcher = async () => {
    if (filterType === 'project') return budgetsByProject(filterValue, { page: 0, size: 25 });
    return budgetsByStatus(filterValue, { page: 0, size: 25 });
  };
  const { data, loading, error, run } = useAsync(fetcher, [filterType, filterValue]);
  const [info, setInfo] = useState('');
  const onCreate = async (v) => { await createBudget(v); setInfo('Budget created'); run(); };
  const onSubmit = async (id) => { try { await submitBudget(id); setInfo('Submitted'); run(); } catch (e) { setInfo(e.message); } };
  const onApprove = async (id) => {
    const action = prompt('Action (APPROVE/REJECT):', 'APPROVE');
    if (!action) return;
    const comments = prompt('Comments:') || '';
    try { await approveBudget(id, { action, comments }); setInfo('Decision recorded'); run(); }
    catch (e) { setInfo(e.message); }
  };
  const onDelete = async (id) => {
    if (!confirm(`Delete ${id}?`)) return;
    try { await deleteBudget(id); setInfo('Deleted'); run(); } catch (e) { setInfo(e.message); }
  };
  return (
    <>
      <Section title="Filter">
        <div className="d-flex gap-2 flex-wrap">
          <select className="form-select form-select-sm" style={{ maxWidth: 180 }} value={filterType} onChange={e => setFilterType(e.target.value)}>
            <option value="status">By Status</option>
            <option value="project">By Project</option>
          </select>
          <input className="form-control form-control-sm" style={{ maxWidth: 200 }} value={filterValue} onChange={e => setFilterValue(e.target.value)} placeholder={filterType === 'status' ? 'DRAFT/SUBMITTED/APPROVED/REJECTED' : 'Project ID'} />
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </div>
      </Section>
      <Section title="Create Budget" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'projectId', label: 'Project ID', required: true },
            { name: 'name', label: 'Name', required: true },
            { name: 'category', label: 'Category', type: 'select', options: ['LABOR', 'EQUIPMENT', 'MATERIAL', 'OTHER'].map(c => ({ value: c, label: c })) },
            { name: 'totalAmount', label: 'Total Amount', type: 'number', required: true },
            { name: 'description', label: 'Description', type: 'textarea', col: 'col-12' },
          ]}
          onSubmit={onCreate}
          submitLabel="Create budget"
        />
      </Section>
      <Section title="Budgets" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={unpaged(data)}
            columns={[
              { key: 'budgetId', label: 'ID' },
              { key: 'projectId', label: 'Project' },
              { key: 'name', label: 'Name' },
              { key: 'category', label: 'Category' },
              { key: 'totalAmount', label: 'Amount' },
              { key: 'status', label: 'Status' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getBudget(r.budgetId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => onSubmit(r.budgetId)}>Submit</button>
                <button className="btn btn-sm btn-outline-success me-1" onClick={() => onApprove(r.budgetId)}>Approve</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.budgetId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function ExpensesTab() {
  const [filterType, setFilterType] = useState('status');
  const [filterValue, setFilterValue] = useState('DRAFT');
  const fetcher = async () => {
    if (filterType === 'project') return expensesByProject(filterValue, { page: 0, size: 25 });
    if (filterType === 'budget') return expensesByBudget(filterValue, { page: 0, size: 25 });
    return expensesByStatus(filterValue, { page: 0, size: 25 });
  };
  const { data, loading, error, run } = useAsync(fetcher, [filterType, filterValue]);
  const [info, setInfo] = useState('');
  const onCreate = async (v) => { await createExpense(v); setInfo('Expense created'); run(); };
  const onSubmit = async (id) => { try { await submitExpense(id); setInfo('Submitted'); run(); } catch (e) { setInfo(e.message); } };
  const onApprove = async (id) => {
    const action = prompt('Action (APPROVE/REJECT):', 'APPROVE');
    if (!action) return;
    const comments = prompt('Comments:') || '';
    try { await approveExpense(id, { action, comments }); setInfo('Decision recorded'); run(); }
    catch (e) { setInfo(e.message); }
  };
  const onDelete = async (id) => {
    if (!confirm(`Delete ${id}?`)) return;
    try { await deleteExpense(id); setInfo('Deleted'); run(); } catch (e) { setInfo(e.message); }
  };
  return (
    <>
      <Section title="Filter">
        <div className="d-flex gap-2 flex-wrap">
          <select className="form-select form-select-sm" style={{ maxWidth: 180 }} value={filterType} onChange={e => setFilterType(e.target.value)}>
            <option value="status">By Status</option>
            <option value="project">By Project</option>
            <option value="budget">By Budget</option>
          </select>
          <input className="form-control form-control-sm" style={{ maxWidth: 200 }} value={filterValue} onChange={e => setFilterValue(e.target.value)} />
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </div>
      </Section>
      <Section title="Create Expense" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'projectId', label: 'Project ID', required: true },
            { name: 'budgetId', label: 'Budget ID', required: true },
            { name: 'amount', label: 'Amount', type: 'number', required: true },
            { name: 'description', label: 'Description', type: 'textarea', col: 'col-12' },
            { name: 'incurredAt', label: 'Incurred At', type: 'date' },
          ]}
          onSubmit={onCreate}
          submitLabel="Create expense"
        />
      </Section>
      <Section title="Expenses" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={unpaged(data)}
            columns={[
              { key: 'expenseId', label: 'ID' },
              { key: 'projectId', label: 'Project' },
              { key: 'budgetId', label: 'Budget' },
              { key: 'amount', label: 'Amount' },
              { key: 'status', label: 'Status' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getExpense(r.expenseId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => onSubmit(r.expenseId)}>Submit</button>
                <button className="btn btn-sm btn-outline-success me-1" onClick={() => onApprove(r.expenseId)}>Approve</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.expenseId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function PaymentsTab() {
  const [filterType, setFilterType] = useState('pending');
  const [filterValue, setFilterValue] = useState('');
  const fetcher = async () => {
    if (filterType === 'pending') return pendingPayments({ page: 0, size: 25 });
    if (filterType === 'expense') return paymentsByExpense(filterValue, { page: 0, size: 25 });
    return paymentsByStatus(filterValue, { page: 0, size: 25 });
  };
  const { data, loading, error, run } = useAsync(fetcher, [filterType, filterValue]);
  const [info, setInfo] = useState('');
  const onCreate = async (v) => { await createPayment(v); setInfo('Payment created'); run(); };
  const onStatus = async (id) => {
    const status = prompt('New status (PENDING/PAID/FAILED):', 'PAID');
    if (!status) return;
    try { await updatePaymentStatus(id, { status }); setInfo('Updated'); run(); }
    catch (e) { setInfo(e.message); }
  };
  return (
    <>
      <Section title="Filter">
        <div className="d-flex gap-2 flex-wrap">
          <select className="form-select form-select-sm" style={{ maxWidth: 180 }} value={filterType} onChange={e => setFilterType(e.target.value)}>
            <option value="pending">Pending</option>
            <option value="status">By Status</option>
            <option value="expense">By Expense</option>
          </select>
          {filterType !== 'pending' && (
            <input className="form-control form-control-sm" style={{ maxWidth: 200 }} value={filterValue} onChange={e => setFilterValue(e.target.value)} />
          )}
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </div>
      </Section>
      <Section title="Create Payment" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'expenseId', label: 'Expense ID', required: true },
            { name: 'amount', label: 'Amount', type: 'number', required: true },
            { name: 'method', label: 'Method', type: 'select', options: ['BANK_TRANSFER', 'CHEQUE', 'CASH', 'CARD'].map(m => ({ value: m, label: m })) },
            { name: 'reference', label: 'Reference' },
          ]}
          onSubmit={onCreate}
          submitLabel="Create payment"
        />
      </Section>
      <Section title="Payments" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={unpaged(data)}
            columns={[
              { key: 'paymentId', label: 'ID' },
              { key: 'expenseId', label: 'Expense' },
              { key: 'amount', label: 'Amount' },
              { key: 'method', label: 'Method' },
              { key: 'status', label: 'Status' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getPayment(r.paymentId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-primary" onClick={() => onStatus(r.paymentId)}>Status</button>
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
  const { data, loading, error, run } = useAsync(() => financeMyTasks(status || null), [status]);
  const [info, setInfo] = useState('');
  const [pid, setPid] = useState('');
  const onSync = async () => {
    try { const r = await financeSync(); setInfo(`Synced: ${r.newTasksSynced} new, ${r.alreadyExisted} existed`); run(); }
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
        <button className="btn btn-sm btn-outline-info" onClick={async () => { if (pid) alert(JSON.stringify(await financeMyTasksByProject(pid), null, 2)); }}>Tasks by Project</button>
      </div>
      <ErrorAlert message={error} />
      <InfoAlert message={info} onClose={() => setInfo('')} />
      {loading ? <Loader /> : (
        <DataTable rows={data || []} columns={[
          { key: 'id', label: 'Local ID' },
          { key: 'pmTaskId', label: 'PM Task' },
          { key: 'projectId', label: 'Project' },
          { key: 'description', label: 'Description' },
          { key: 'status', label: 'Status' },
          { key: 'syncedAt', label: 'Synced' },
        ]} />
      )}
    </Section>
  );
}

function ReportsTab() {
  const [variancePid, setVariancePid] = useState('');
  const [variance, setVariance] = useState(null);
  const cashFlow = useAsync(reportCashFlow);
  const fetchVariance = async () => {
    if (!variancePid) return;
    try { setVariance(await reportBudgetVariance(variancePid)); }
    catch (e) { alert(e.message); }
  };
  return (
    <>
      <Section title="Budget Variance">
        <div className="d-flex gap-2 mb-2">
          <input className="form-control form-control-sm" style={{ maxWidth: 240 }} placeholder="Project ID" value={variancePid} onChange={e => setVariancePid(e.target.value)} />
          <button className="btn btn-sm btn-primary" onClick={fetchVariance}>Fetch</button>
        </div>
        {variance && <pre className="bg-light p-2 small mb-0">{JSON.stringify(variance, null, 2)}</pre>}
      </Section>
      <Section title="Cash Flow" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => cashFlow.run()}>Refresh</button>}>
        <ErrorAlert message={cashFlow.error} />
        {cashFlow.loading ? <Loader /> : <pre className="bg-light p-2 small mb-0">{JSON.stringify(cashFlow.data, null, 2)}</pre>}
      </Section>
    </>
  );
}

export default function FinanceDashboard() {
  return (
    <>
      <h2 className="h4 mb-3">Finance Dashboard</h2>
      <Tabs
        tabs={[
          { key: 'budgets', label: 'Budgets', content: () => <BudgetsTab /> },
          { key: 'expenses', label: 'Expenses', content: () => <ExpensesTab /> },
          { key: 'payments', label: 'Payments', content: () => <PaymentsTab /> },
          { key: 'tasks', label: 'My Tasks', content: () => <TasksTab /> },
          { key: 'reports', label: 'Reports', content: () => <ReportsTab /> },
        ]}
      />
    </>
  );
}
