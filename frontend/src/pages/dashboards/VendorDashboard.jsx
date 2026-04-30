import { useState } from 'react';
import Tabs from '../../components/Tabs.jsx';
import Section from '../../components/Section.jsx';
import DataTable from '../../components/DataTable.jsx';
import Loader from '../../components/Loader.jsx';
import { ErrorAlert, InfoAlert } from '../../components/Alert.jsx';
import JsonForm from '../../components/JsonForm.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import {
  listContracts, getContract, contractsByVendor, contractsByStatus, createContract, updateContract, deleteContract,
  listInvoices, getInvoice, invoicesByContract, invoicesByStatus, createInvoice, updateInvoice, deleteInvoice, submitInvoice, invoiceStatus,
  listDeliveries, getDelivery, deliveriesByContract, deliveriesByStatus, createDelivery, updateDelivery, deleteDelivery,
  listDocuments, getDocument, documentsByVendor, documentsByType, documentsByStatus, uploadDocument, deleteDocument, submitDocument, documentStatus, downloadDocumentUrl,
  vendorSync, vendorMyTasks, vendorMyTasksByProject,
} from '../../api/vendor.js';

function unpaged(r) { return r?.content || r?.data?.content || r?.data || r || []; }

function ContractsTab() {
  const [params, setParams] = useState({ page: 0, size: 25 });
  const { data, loading, error, run } = useAsync(() => listContracts(params), [JSON.stringify(params)]);
  const [info, setInfo] = useState('');
  const [filterValue, setFilterValue] = useState('');
  const [filterType, setFilterType] = useState('all');
  const fetchAlt = async () => {
    if (filterType === 'vendor' && filterValue) { try { const r = await contractsByVendor(filterValue); alert(JSON.stringify(r, null, 2)); } catch (e) { alert(e.message); } }
    if (filterType === 'status' && filterValue) { try { const r = await contractsByStatus(filterValue); alert(JSON.stringify(r, null, 2)); } catch (e) { alert(e.message); } }
  };
  const onCreate = async (v) => { await createContract(v); setInfo('Contract created'); run(); };
  const onDelete = async (id) => {
    if (!confirm(`Delete ${id}?`)) return;
    try { await deleteContract(id); setInfo('Deleted'); run(); } catch (e) { setInfo(e.message); }
  };
  return (
    <>
      <Section title="Filter / Search">
        <div className="d-flex gap-2 flex-wrap">
          <select className="form-select form-select-sm" style={{ maxWidth: 200 }} value={filterType} onChange={e => setFilterType(e.target.value)}>
            <option value="all">List All (paged)</option>
            <option value="vendor">By Vendor ID</option>
            <option value="status">By Status</option>
          </select>
          {filterType !== 'all' && (
            <>
              <input className="form-control form-control-sm" style={{ maxWidth: 240 }} value={filterValue} onChange={e => setFilterValue(e.target.value)} />
              <button className="btn btn-sm btn-primary" onClick={fetchAlt}>Fetch</button>
            </>
          )}
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh All</button>
        </div>
      </Section>
      <Section title="Create Contract" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'contractTitle', label: 'Title', required: true },
            { name: 'description', label: 'Description', type: 'textarea', col: 'col-12' },
            { name: 'startDate', label: 'Start', type: 'date', required: true },
            { name: 'endDate', label: 'End', type: 'date', required: true },
            { name: 'totalValue', label: 'Total Value', type: 'number', required: true },
            { name: 'projectId', label: 'Project ID' },
          ]}
          onSubmit={onCreate}
          submitLabel="Create contract"
        />
      </Section>
      <Section title="Contracts" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={unpaged(data)}
            columns={[
              { key: 'contractId', label: 'ID' },
              { key: 'contractTitle', label: 'Title' },
              { key: 'vendorId', label: 'Vendor' },
              { key: 'status', label: 'Status' },
              { key: 'totalValue', label: 'Value' },
              { key: 'endDate', label: 'End Date' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getContract(r.contractId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.contractId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function InvoicesTab() {
  const [params] = useState({ page: 0, size: 25 });
  const { data, loading, error, run } = useAsync(() => listInvoices(params), []);
  const [info, setInfo] = useState('');
  const [filterValue, setFilterValue] = useState('');
  const [filterType, setFilterType] = useState('all');
  const onCreate = async (v) => { await createInvoice(v); setInfo('Invoice created'); run(); };
  const fetchAlt = async () => {
    try {
      const r = filterType === 'contract' ? await invoicesByContract(filterValue) : await invoicesByStatus(filterValue);
      alert(JSON.stringify(r, null, 2));
    } catch (e) { alert(e.message); }
  };
  const onSubmit = async (id) => { try { await submitInvoice(id); setInfo('Submitted'); run(); } catch (e) { setInfo(e.message); } };
  const onDelete = async (id) => { if (!confirm(`Delete ${id}?`)) return; try { await deleteInvoice(id); setInfo('Deleted'); run(); } catch (e) { setInfo(e.message); } };
  return (
    <>
      <Section title="Filter / Search">
        <div className="d-flex gap-2 flex-wrap">
          <select className="form-select form-select-sm" style={{ maxWidth: 200 }} value={filterType} onChange={e => setFilterType(e.target.value)}>
            <option value="all">List All</option>
            <option value="contract">By Contract</option>
            <option value="status">By Status</option>
          </select>
          {filterType !== 'all' && (
            <>
              <input className="form-control form-control-sm" style={{ maxWidth: 240 }} value={filterValue} onChange={e => setFilterValue(e.target.value)} />
              <button className="btn btn-sm btn-primary" onClick={fetchAlt}>Fetch</button>
            </>
          )}
          <button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>
        </div>
      </Section>
      <Section title="Create Invoice" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'contractId', label: 'Contract ID', required: true },
            { name: 'invoiceNumber', label: 'Invoice Number', required: true },
            { name: 'amount', label: 'Amount', type: 'number', required: true },
            { name: 'invoiceDate', label: 'Invoice Date', type: 'date', required: true },
            { name: 'description', label: 'Description', type: 'textarea', col: 'col-12' },
          ]}
          onSubmit={onCreate}
          submitLabel="Create invoice"
        />
      </Section>
      <Section title="Invoices" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={unpaged(data)}
            columns={[
              { key: 'invoiceId', label: 'ID' },
              { key: 'invoiceNumber', label: 'No.' },
              { key: 'contractId', label: 'Contract' },
              { key: 'amount', label: 'Amount' },
              { key: 'status', label: 'Status' },
              { key: 'invoiceDate', label: 'Date' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getInvoice(r.invoiceId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-info me-1" onClick={async () => { try { alert(JSON.stringify(await invoiceStatus(r.invoiceId), null, 2)); } catch (e) { alert(e.message); } }}>Status</button>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => onSubmit(r.invoiceId)}>Submit</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.invoiceId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function DeliveriesTab() {
  const { data, loading, error, run } = useAsync(() => listDeliveries({ page: 0, size: 25 }));
  const [info, setInfo] = useState('');
  const onCreate = async (v) => { await createDelivery(v); setInfo('Delivery created'); run(); };
  const onDelete = async (id) => { if (!confirm(`Delete ${id}?`)) return; try { await deleteDelivery(id); setInfo('Deleted'); run(); } catch (e) { setInfo(e.message); } };
  return (
    <>
      <Section title="Create Delivery" collapsible defaultOpen={false}>
        <JsonForm
          fields={[
            { name: 'contractId', label: 'Contract ID', required: true },
            { name: 'itemDescription', label: 'Item Description', required: true },
            { name: 'quantity', label: 'Quantity', type: 'number', required: true },
            { name: 'expectedDate', label: 'Expected Date', type: 'date' },
            { name: 'actualDate', label: 'Actual Date', type: 'date' },
          ]}
          onSubmit={onCreate}
          submitLabel="Create delivery"
        />
      </Section>
      <Section title="Deliveries" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={unpaged(data)}
            columns={[
              { key: 'deliveryId', label: 'ID' },
              { key: 'contractId', label: 'Contract' },
              { key: 'itemDescription', label: 'Item' },
              { key: 'quantity', label: 'Qty' },
              { key: 'status', label: 'Status' },
            ]}
            actions={(r) => (
              <>
                <button className="btn btn-sm btn-outline-secondary me-1" onClick={async () => alert(JSON.stringify(await getDelivery(r.deliveryId), null, 2))}>View</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.deliveryId)}>Delete</button>
              </>
            )}
          />
        )}
      </Section>
    </>
  );
}

function DocumentsTab() {
  const { data, loading, error, run } = useAsync(() => listDocuments({ page: 0, size: 25 }));
  const [info, setInfo] = useState('');
  const [form, setForm] = useState({ documentType: 'CERTIFICATE', documentName: '', file: null });
  const onUpload = async () => {
    if (!form.file) return;
    const fd = new FormData();
    fd.append('file', form.file);
    fd.append('documentType', form.documentType);
    fd.append('documentName', form.documentName);
    try { await uploadDocument(fd); setInfo('Uploaded'); run(); }
    catch (e) { setInfo(`Error: ${e.message}`); }
  };
  const onSubmit = async (id) => { try { await submitDocument(id); setInfo('Submitted'); run(); } catch (e) { setInfo(e.message); } };
  const onDelete = async (id) => { if (!confirm(`Delete ${id}?`)) return; try { await deleteDocument(id); setInfo('Deleted'); run(); } catch (e) { setInfo(e.message); } };
  return (
    <>
      <Section title="Upload Document" collapsible defaultOpen={false}>
        <div className="row g-2">
          <div className="col-md-4">
            <label className="form-label small">Type</label>
            <select className="form-select form-select-sm" value={form.documentType} onChange={e => setForm({ ...form, documentType: e.target.value })}>
              {['CERTIFICATE', 'CONTRACT', 'INVOICE', 'OTHER'].map(t => <option key={t}>{t}</option>)}
            </select>
          </div>
          <div className="col-md-4">
            <label className="form-label small">Name</label>
            <input className="form-control form-control-sm" value={form.documentName} onChange={e => setForm({ ...form, documentName: e.target.value })} />
          </div>
          <div className="col-md-4">
            <label className="form-label small">File</label>
            <input className="form-control form-control-sm" type="file" onChange={e => setForm({ ...form, file: e.target.files?.[0] })} />
          </div>
          <div className="col-12 text-end">
            <button className="btn btn-sm btn-primary" onClick={onUpload} disabled={!form.file}>Upload</button>
          </div>
        </div>
      </Section>
      <Section title="Documents" actions={<button className="btn btn-sm btn-outline-secondary" onClick={() => run()}>Refresh</button>}>
        <ErrorAlert message={error} />
        <InfoAlert message={info} onClose={() => setInfo('')} />
        {loading ? <Loader /> : (
          <DataTable
            rows={unpaged(data)}
            columns={[
              { key: 'documentId', label: 'ID' },
              { key: 'documentName', label: 'Name' },
              { key: 'documentType', label: 'Type' },
              { key: 'status', label: 'Status' },
              { key: 'vendorId', label: 'Vendor' },
            ]}
            actions={(r) => (
              <>
                <a className="btn btn-sm btn-outline-secondary me-1" href={downloadDocumentUrl(r.documentId)} target="_blank" rel="noreferrer">Download</a>
                <button className="btn btn-sm btn-outline-info me-1" onClick={async () => { try { alert(JSON.stringify(await documentStatus(r.documentId), null, 2)); } catch (e) { alert(e.message); } }}>Status</button>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => onSubmit(r.documentId)}>Submit</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => onDelete(r.documentId)}>Delete</button>
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
  const { data, loading, error, run } = useAsync(() => vendorMyTasks(status || null), [status]);
  const [info, setInfo] = useState('');
  const [pid, setPid] = useState('');
  const onSync = async () => { try { const r = await vendorSync(); setInfo(`Synced: ${r.newTasksSynced} new, ${r.alreadyExisted} existed`); run(); } catch (e) { setInfo(e.message); } };
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
        <button className="btn btn-sm btn-outline-info" onClick={async () => { if (pid) alert(JSON.stringify(await vendorMyTasksByProject(pid), null, 2)); }}>Tasks by Project</button>
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

export default function VendorDashboard() {
  return (
    <>
      <h2 className="h4 mb-3">Vendor Dashboard</h2>
      <Tabs
        tabs={[
          { key: 'contracts', label: 'Contracts', content: () => <ContractsTab /> },
          { key: 'invoices', label: 'Invoices', content: () => <InvoicesTab /> },
          { key: 'deliveries', label: 'Deliveries', content: () => <DeliveriesTab /> },
          { key: 'documents', label: 'Documents', content: () => <DocumentsTab /> },
          { key: 'tasks', label: 'My Tasks', content: () => <TasksTab /> },
        ]}
      />
    </>
  );
}
