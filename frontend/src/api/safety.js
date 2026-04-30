import { safetyClient } from './client.js';

// Incidents
export const listIncidents = (params = {}) =>
  safetyClient.get('/api/safety/incidents', { params }).then(r => r.data);
export const getIncident = (id) => safetyClient.get(`/api/safety/incidents/${id}`).then(r => r.data);
export const createIncident = (body) =>
  safetyClient.post('/api/safety/incidents', body).then(r => r.data);
export const updateIncidentStatus = (id, status) =>
  safetyClient.patch(`/api/safety/incidents/${id}/status`, null, { params: { status } }).then(r => r.data);
export const deleteIncident = (id) =>
  safetyClient.delete(`/api/safety/incidents/${id}`).then(r => r.data);

// Inspections
export const listInspections = (params = {}) =>
  safetyClient.get('/api/safety/inspections', { params }).then(r => r.data);
export const getInspection = (id) =>
  safetyClient.get(`/api/safety/inspections/${id}`).then(r => r.data);
export const createInspection = (body) =>
  safetyClient.post('/api/safety/inspections', body).then(r => r.data);
export const updateInspectionStatus = (id, status) =>
  safetyClient.patch(`/api/safety/inspections/${id}/status`, null, { params: { status } }).then(r => r.data);
export const deleteInspection = (id) =>
  safetyClient.delete(`/api/safety/inspections/${id}`).then(r => r.data);
export const inspectionTypes = () =>
  safetyClient.get('/api/safety/inspections/types').then(r => r.data);

// Assigned Tasks
export const safetySync = () => safetyClient.post('/api/safety/tasks/sync').then(r => r.data);
export const safetyMyTasks = (status) =>
  safetyClient.get('/api/safety/tasks', { params: status ? { status } : {} }).then(r => r.data);
export const safetyMyTasksByProject = (projectId) =>
  safetyClient.get(`/api/safety/tasks/project/${projectId}`).then(r => r.data);
