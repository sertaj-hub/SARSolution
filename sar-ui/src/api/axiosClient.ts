import axios from 'axios';
import { loadCredentials, toBasicAuth } from '@/lib/auth';

const client = axios.create({ baseURL: '/api/v1' });

client.interceptors.request.use(cfg => {
  const creds = loadCredentials();
  if (creds) cfg.headers.Authorization = toBasicAuth(creds);
  return cfg;
});

client.interceptors.response.use(
  r => r,
  err => {
    const data = err.response?.data;
    return Promise.reject(data ?? err);
  }
);

export default client;
