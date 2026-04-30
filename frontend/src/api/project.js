import { projectClient } from './client.js';

// Projects
export const listProjects = () => projectClient.get('/api/projects').then(r => r.data);
export const getProject = (id) => projectClient.get(`/api/projects/${id}`).then(r => r.data);
export const createProject = (body) => projectClient.post('/api/projects', body).then(r => r.data);

// Tasks
export const getProjectTasks = (id) => projectClient.get(`/api/projects/${id}/tasks`).then(r => r.data);
export const createTask = (projectId, body) =>
  projectClient.post(`/api/projects/${projectId}/tasks`, body).then(r => r.data);
export const updateTaskStatus = (taskId, status) =>
  projectClient.patch(`/api/projects/tasks/${taskId}/status`, null, { params: { status } }).then(r => r.data);

// Milestones
export const getProjectMilestones = (id) =>
  projectClient.get(`/api/projects/${id}/milestones`).then(r => r.data);
export const updateMilestoneStatus = (milestoneId, status) =>
  projectClient
    .patch(`/api/projects/milestones/${milestoneId}/status`, null, { params: { status } })
    .then(r => r.data);

// Approvals
export const listApprovals = () => projectClient.get('/api/approvals').then(r => r.data);
export const pendingApprovals = () => projectClient.get('/api/approvals/pending').then(r => r.data);
export const approvalsByProject = (projectId) =>
  projectClient.get(`/api/approvals/project/${projectId}`).then(r => r.data);
export const approveRequest = (id) =>
  projectClient.post(`/api/approvals/${id}/approve`).then(r => r.data);
export const rejectRequest = (id, reason) =>
  projectClient.post(`/api/approvals/${id}/reject`, null, { params: { reason } }).then(r => r.data);
export const approvalStats = () => projectClient.get('/api/approvals/stats').then(r => r.data);

// Templates
export const listTemplates = () => projectClient.get('/api/templates').then(r => r.data);
export const getTemplate = (id) => projectClient.get(`/api/templates/${id}`).then(r => r.data);

// PM Notifications (the inline notification feed in PM)
export const pmNotifications = () => projectClient.get('/api/notifications').then(r => r.data);
export const pmUnreadCount = () =>
  projectClient.get('/api/notifications/unread-count').then(r => r.data);
export const pmMarkRead = (id) =>
  projectClient.put(`/api/notifications/${id}/read`).then(r => r.data);
