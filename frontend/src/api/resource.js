import { resourceClient } from './client.js';

// Resources
export const listResources = () => resourceClient.get('/api/resources').then(r => r.data);
export const listResourcesPaged = (params) =>
  resourceClient.get('/api/resources/page', { params }).then(r => r.data);
export const getResource = (id) => resourceClient.get(`/api/resources/${id}`).then(r => r.data);
export const createResource = (body) => resourceClient.post('/api/resources', body).then(r => r.data);
export const updateResource = (id, body) =>
  resourceClient.put(`/api/resources/${id}`, body).then(r => r.data);
export const deleteResource = (id) => resourceClient.delete(`/api/resources/${id}`).then(r => r.data);
export const resourcesByType = (type) =>
  resourceClient.get(`/api/resources/type/${type}`).then(r => r.data);
export const availableResources = () => resourceClient.get('/api/resources/available').then(r => r.data);

// Allocations
export const listAllocations = () => resourceClient.get('/api/allocations').then(r => r.data);
export const listAllocationsPaged = (params) =>
  resourceClient.get('/api/allocations/page', { params }).then(r => r.data);
export const getAllocation = (id) => resourceClient.get(`/api/allocations/${id}`).then(r => r.data);
export const createAllocation = (body) =>
  resourceClient.post('/api/allocations', body).then(r => r.data);
export const updateAllocation = (id, body) =>
  resourceClient.put(`/api/allocations/${id}`, body).then(r => r.data);
export const deleteAllocation = (id) =>
  resourceClient.delete(`/api/allocations/${id}`).then(r => r.data);
export const allocationsByProject = (projectId) =>
  resourceClient.get(`/api/allocations/project/${projectId}`).then(r => r.data);
export const allocationsByResource = (resourceId) =>
  resourceClient.get(`/api/allocations/resource/${resourceId}`).then(r => r.data);
export const allocationCost = (id) =>
  resourceClient.get(`/api/allocations/${id}/cost`).then(r => r.data);
export const allocationEvent = (id) =>
  resourceClient.get(`/api/allocations/${id}/event`).then(r => r.data);
