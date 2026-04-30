import { financeClient } from './client.js';

// Budgets
export const createBudget = (body) => financeClient.post('/api/budgets', body).then(r => r.data);
export const getBudget = (id) => financeClient.get(`/api/budgets/${id}`).then(r => r.data);
export const budgetsByProject = (projectId, params) =>
  financeClient.get(`/api/budgets/projects/${projectId}`, { params }).then(r => r.data);
export const submitBudget = (id) => financeClient.post(`/api/budgets/${id}/submit`).then(r => r.data);
export const approveBudget = (id, body) =>
  financeClient.post(`/api/budgets/${id}/approval`, body).then(r => r.data);
export const budgetsByStatus = (status, params) =>
  financeClient.get(`/api/budgets/status/${status}`, { params }).then(r => r.data);
export const budgetsByCreator = (createdBy, params) =>
  financeClient.get(`/api/budgets/users/${createdBy}`, { params }).then(r => r.data);
export const updateBudget = (id, body) =>
  financeClient.patch(`/api/budgets/${id}`, body).then(r => r.data);
export const deleteBudget = (id) => financeClient.delete(`/api/budgets/${id}`).then(r => r.data);

// Expenses
export const createExpense = (body) => financeClient.post('/api/expenses', body).then(r => r.data);
export const getExpense = (id) => financeClient.get(`/api/expenses/${id}`).then(r => r.data);
export const expensesByBudget = (budgetId, params) =>
  financeClient.get(`/api/expenses/budgets/${budgetId}`, { params }).then(r => r.data);
export const expensesByProject = (projectId, params) =>
  financeClient.get(`/api/expenses/projects/${projectId}`, { params }).then(r => r.data);
export const submitExpense = (id) => financeClient.post(`/api/expenses/${id}/submit`).then(r => r.data);
export const approveExpense = (id, body) =>
  financeClient.post(`/api/expenses/${id}/approval`, body).then(r => r.data);
export const expensesByStatus = (status, params) =>
  financeClient.get(`/api/expenses/status/${status}`, { params }).then(r => r.data);
export const expensesByCreator = (createdBy, params) =>
  financeClient.get(`/api/expenses/users/${createdBy}`, { params }).then(r => r.data);
export const updateExpense = (id, body) =>
  financeClient.patch(`/api/expenses/${id}`, body).then(r => r.data);
export const deleteExpense = (id) => financeClient.delete(`/api/expenses/${id}`).then(r => r.data);

// Payments
export const createPayment = (body) => financeClient.post('/api/payments', body).then(r => r.data);
export const getPayment = (id) => financeClient.get(`/api/payments/${id}`).then(r => r.data);
export const paymentsByExpense = (expenseId, params) =>
  financeClient.get(`/api/payments/expenses/${expenseId}`, { params }).then(r => r.data);
export const paymentsByStatus = (status, params) =>
  financeClient.get(`/api/payments/status/${status}`, { params }).then(r => r.data);
export const updatePaymentStatus = (id, body) =>
  financeClient.post(`/api/payments/${id}/status`, body).then(r => r.data);
export const paymentsByCreator = (createdBy, params) =>
  financeClient.get(`/api/payments/users/${createdBy}`, { params }).then(r => r.data);
export const pendingPayments = (params) =>
  financeClient.get('/api/payments/pending', { params }).then(r => r.data);

// Assigned Tasks
export const financeSync = () => financeClient.post('/api/finance/tasks/sync').then(r => r.data);
export const financeMyTasks = (status) =>
  financeClient.get('/api/finance/tasks', { params: status ? { status } : {} }).then(r => r.data);
export const financeMyTasksByProject = (projectId) =>
  financeClient.get(`/api/finance/tasks/project/${projectId}`).then(r => r.data);
