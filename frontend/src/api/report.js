import { reportClient } from './client.js';

export const reportProjects = () => reportClient.get('/api/projects').then(r => r.data);
export const reportProject = (id) => reportClient.get(`/api/projects/${id}`).then(r => r.data);

// Finance reports
export const reportBudgetVariance = (projectId) =>
  reportClient.get(`/api/reports/finance/budget-variance/${projectId}`).then(r => r.data);
export const reportCashFlow = () =>
  reportClient.get('/api/reports/finance/cash-flow').then(r => r.data);

// Safety reports
export const reportSafetyTrends = () =>
  reportClient.get('/api/reports/safety/trends').then(r => r.data);
export const reportInspectionsSummary = () =>
  reportClient.get('/api/reports/safety/inspections-summary').then(r => r.data);

// Resource reports
export const reportResourceUtilization = () =>
  reportClient.get('/api/reports/resources/utilization').then(r => r.data);
export const reportLaborAllocation = () =>
  reportClient.get('/api/reports/resources/labor-allocation').then(r => r.data);

// Reporting management
export const generateReport = (body) =>
  reportClient.post('/api/reports/generate', body).then(r => r.data);
export const getReport = (id) => reportClient.get(`/api/reports/${id}`).then(r => r.data);
export const dashboardSummary = () =>
  reportClient.get('/api/reports/dashboard-summary').then(r => r.data);
