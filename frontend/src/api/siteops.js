import { siteopsClient } from './client.js';

// Site Logs
export const listSiteLogs = (params) =>
  siteopsClient.get('/api/sitelogs', { params }).then(r => r.data);
export const listSiteLogsPaged = (params) =>
  siteopsClient.get('/api/sitelogs/paginated/list', { params }).then(r => r.data);
export const getSiteLog = (id) => siteopsClient.get(`/api/sitelogs/${id}`).then(r => r.data);
export const createSiteLog = (body) => siteopsClient.post('/api/sitelogs', body).then(r => r.data);
export const siteLogByDate = (params) =>
  siteopsClient.get('/api/sitelogs/by-date', { params }).then(r => r.data);
export const latestSiteLog = (projectId) =>
  siteopsClient.get(`/api/sitelogs/latest/${projectId}`).then(r => r.data);
export const uploadSiteLogPhoto = (logId, file) => {
  const fd = new FormData();
  fd.append('photo', file);
  return siteopsClient
    .post(`/api/sitelogs/${logId}/photo-upload`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then(r => r.data);
};
export const siteLogInstanceInfo = () =>
  siteopsClient.get('/api/sitelogs/instance-info').then(r => r.data);

// Issues
export const listIssues = (params) => siteopsClient.get('/api/issues', { params }).then(r => r.data);
export const listIssuesPaged = (params) =>
  siteopsClient.get('/api/issues/paginated/list', { params }).then(r => r.data);
export const getIssue = (id) => siteopsClient.get(`/api/issues/${id}`).then(r => r.data);
export const createIssue = (body) => siteopsClient.post('/api/issues', body).then(r => r.data);
export const updateIssue = (id, body) =>
  siteopsClient.patch(`/api/issues/${id}`, body).then(r => r.data);
export const issuesByLog = (logId) =>
  siteopsClient.get(`/api/issues/by-log/${logId}`).then(r => r.data);

// Assigned Tasks (the To-Do pattern we built)
export const siteopsSync = () => siteopsClient.post('/api/siteops/tasks/sync').then(r => r.data);
export const siteopsMyTasks = (status) =>
  siteopsClient.get('/api/siteops/tasks', { params: status ? { status } : {} }).then(r => r.data);
export const siteopsMyTasksByProject = (projectId) =>
  siteopsClient.get(`/api/siteops/tasks/project/${projectId}`).then(r => r.data);
