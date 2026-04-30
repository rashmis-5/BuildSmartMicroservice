import { notificationClient } from './client.js';

export const createNotification = (body) =>
  notificationClient.post('/api/notifications', body).then(r => r.data);

export const unreadCount = () =>
  notificationClient.get('/api/notifications/unread-count').then(r => r.data);

export const listNotifications = (params = {}) =>
  notificationClient.get('/api/notifications', { params }).then(r => r.data);

export const markRead = (id) =>
  notificationClient.put(`/api/notifications/${id}/read`).then(r => r.data);
