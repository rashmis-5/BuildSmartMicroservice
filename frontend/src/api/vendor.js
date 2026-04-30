import { vendorClient } from './client.js';

// Contracts
export const listContracts = (params) =>
  vendorClient.get('/api/contracts', { params }).then(r => r.data);
export const getContract = (id) => vendorClient.get(`/api/contracts/${id}`).then(r => r.data);
export const contractsByVendor = (vendorId) =>
  vendorClient.get(`/api/contracts/vendor/${vendorId}`).then(r => r.data);
export const contractsByStatus = (status) =>
  vendorClient.get(`/api/contracts/status/${status}`).then(r => r.data);
export const createContract = (body) => vendorClient.post('/api/contracts', body).then(r => r.data);
export const updateContract = (id, body) =>
  vendorClient.put(`/api/contracts/${id}`, body).then(r => r.data);
export const deleteContract = (id) => vendorClient.delete(`/api/contracts/${id}`).then(r => r.data);

// Invoices
export const listInvoices = (params) =>
  vendorClient.get('/api/invoices', { params }).then(r => r.data);
export const getInvoice = (id) => vendorClient.get(`/api/invoices/${id}`).then(r => r.data);
export const invoicesByContract = (contractId) =>
  vendorClient.get(`/api/invoices/contract/${contractId}`).then(r => r.data);
export const invoicesByStatus = (status) =>
  vendorClient.get(`/api/invoices/status/${status}`).then(r => r.data);
export const createInvoice = (body) => vendorClient.post('/api/invoices', body).then(r => r.data);
export const updateInvoice = (id, body) =>
  vendorClient.put(`/api/invoices/${id}`, body).then(r => r.data);
export const deleteInvoice = (id) => vendorClient.delete(`/api/invoices/${id}`).then(r => r.data);
export const submitInvoice = (id) => vendorClient.post(`/api/invoices/${id}/submit`).then(r => r.data);
export const invoiceStatus = (id) => vendorClient.get(`/api/invoices/${id}/status`).then(r => r.data);

// Deliveries
export const listDeliveries = (params) =>
  vendorClient.get('/api/deliveries', { params }).then(r => r.data);
export const getDelivery = (id) => vendorClient.get(`/api/deliveries/${id}`).then(r => r.data);
export const deliveriesByContract = (contractId) =>
  vendorClient.get(`/api/deliveries/contract/${contractId}`).then(r => r.data);
export const deliveriesByStatus = (status) =>
  vendorClient.get(`/api/deliveries/status/${status}`).then(r => r.data);
export const createDelivery = (body) => vendorClient.post('/api/deliveries', body).then(r => r.data);
export const updateDelivery = (id, body) =>
  vendorClient.put(`/api/deliveries/${id}`, body).then(r => r.data);
export const deleteDelivery = (id) =>
  vendorClient.delete(`/api/deliveries/${id}`).then(r => r.data);

// Documents
export const listDocuments = (params) =>
  vendorClient.get('/api/documents', { params }).then(r => r.data);
export const getDocument = (id) => vendorClient.get(`/api/documents/${id}`).then(r => r.data);
export const documentsByVendor = (vendorId) =>
  vendorClient.get(`/api/documents/vendor/${vendorId}`).then(r => r.data);
export const documentsByType = (type) =>
  vendorClient.get(`/api/documents/type/${type}`).then(r => r.data);
export const documentsByStatus = (status) =>
  vendorClient.get(`/api/documents/status/${status}`).then(r => r.data);
export const uploadDocument = (formData) =>
  vendorClient
    .post('/api/documents', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then(r => r.data);
export const updateDocument = (id, formData) =>
  vendorClient
    .put(`/api/documents/${id}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then(r => r.data);
export const deleteDocument = (id) =>
  vendorClient.delete(`/api/documents/${id}`).then(r => r.data);
export const submitDocument = (id) =>
  vendorClient.post(`/api/documents/${id}/submit`).then(r => r.data);
export const documentStatus = (id) =>
  vendorClient.get(`/api/documents/${id}/status`).then(r => r.data);
export const downloadDocumentUrl = (id) =>
  `${import.meta.env.VITE_API_URL || 'http://localhost:8081'}/api/documents/${id}/download`;

// Assigned Tasks
export const vendorSync = () => vendorClient.post('/api/vendor/tasks/sync').then(r => r.data);
export const vendorMyTasks = (status) =>
  vendorClient.get('/api/vendor/tasks', { params: status ? { status } : {} }).then(r => r.data);
export const vendorMyTasksByProject = (projectId) =>
  vendorClient.get(`/api/vendor/tasks/project/${projectId}`).then(r => r.data);
