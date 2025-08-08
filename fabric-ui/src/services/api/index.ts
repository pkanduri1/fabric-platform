// src/services/api/index.ts
import axios from 'axios';

axios.defaults.headers.common['Cache-Control'] = 'no-cache, no-store, must-revalidate';
axios.defaults.headers.common['Pragma'] = 'no-cache';
axios.defaults.headers.common['Expires'] = '0';

// Add timestamp to all GET requests
axios.interceptors.request.use(config => {
  if (config.method === 'get') {
    config.params = { ...config.params, _t: Date.now() };
  }
  return config;
});