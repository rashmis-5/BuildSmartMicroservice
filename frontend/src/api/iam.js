import { iamClient } from './client.js';

export const login = (email, password) =>
  iamClient.post('/api/auth/login', { email, password }).then(r => r.data);

export const register = (body) =>
  iamClient.post('/api/auth/signup', body).then(r => r.data);

export const logout = () =>
  iamClient.post('/api/auth/logout', {}).then(r => r.data).catch(() => null);

export const forgotPassword = (email) =>
  iamClient.post('/api/auth/forgot-password', { email }).then(r => r.data);

export const resetPassword = (token, newPassword) =>
  iamClient.post('/api/auth/reset-password', { token, newPassword }).then(r => r.data);

export const validateResetToken = (token) =>
  iamClient.get(`/api/auth/validate-reset-token/${token}`).then(r => r.data);

export const myProfile = () => iamClient.get('/users/profile').then(r => r.data);
export const updateMyProfile = (body) => iamClient.put('/users/profile', body).then(r => r.data);
export const checkRole = (role) => iamClient.get(`/users/check-role/${role}`).then(r => r.data);
export const getUserById = (id) => iamClient.get(`/users/${id}`).then(r => r.data);
export const getUserByEmail = (email) =>
  iamClient.get('/users/by-email', { params: { email } }).then(r => r.data);
export const getAllUsers = () => iamClient.get('/users/all').then(r => r.data);

export const adminPendingUsers = () => iamClient.get('/admin/pending-users').then(r => r.data);
export const adminApproveUser = (id) => iamClient.post(`/admin/approve-user/${id}`).then(r => r.data);
export const adminRejectUser = (id) => iamClient.post(`/admin/reject-user/${id}`).then(r => r.data);
export const adminAllUsers = () => iamClient.get('/admin/users').then(r => r.data);
export const adminGetUser = (id) => iamClient.get(`/admin/users/${id}`).then(r => r.data);
export const adminUpdateUser = (id, body) => iamClient.put(`/admin/users/${id}`, body).then(r => r.data);
export const adminDeleteUser = (id) => iamClient.delete(`/admin/users/${id}`).then(r => r.data);
export const adminUsersByRole = (role) =>
  iamClient.get(`/admin/users/role/${role}`).then(r => r.data);

export const auditLogs = (params = {}) =>
  iamClient.get('/admin/audit/logs', { params }).then(r => r.data);
export const auditLogsByUser = (userId, params = {}) =>
  iamClient.get(`/admin/audit/logs/user/${userId}`, { params }).then(r => r.data);
export const auditLogsByAction = (action, params = {}) =>
  iamClient.get(`/admin/audit/logs/action/${action}`, { params }).then(r => r.data);
export const auditLogsByDateRange = (params = {}) =>
  iamClient.get('/admin/audit/logs/date-range', { params }).then(r => r.data);
