import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

// Suppress ResizeObserver loop errors (harmless browser quirk from MUI DataGrid and Monaco Editor)
// This prevents the red error overlay in development mode
const suppressResizeObserverError = (e: ErrorEvent) => {
  // Check if the error message contains ResizeObserver
  if (e.message && e.message.includes('ResizeObserver loop')) {
    const resizeObserverErrDiv = document.getElementById('webpack-dev-server-client-overlay');
    const resizeObserverErr = document.getElementById('webpack-dev-server-client-overlay-div');
    if (resizeObserverErr) {
      resizeObserverErr.setAttribute('style', 'display: none');
    }
    if (resizeObserverErrDiv) {
      resizeObserverErrDiv.setAttribute('style', 'display: none');
    }
    e.stopImmediatePropagation();
    e.stopPropagation();
    e.preventDefault();
    return false;
  }
  return true;
};

// Suppress console errors from ResizeObserver
const originalError = console.error;
console.error = (...args: any[]) => {
  if (args[0]?.toString().includes('ResizeObserver loop')) {
    return;
  }
  originalError.apply(console, args);
};

// Add event listeners with capture phase to intercept before React error boundary
window.addEventListener('error', suppressResizeObserverError, true);
window.addEventListener('unhandledrejection', (e) => {
  if (e.reason?.message?.includes('ResizeObserver loop')) {
    e.stopImmediatePropagation();
    e.preventDefault();
  }
});

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
