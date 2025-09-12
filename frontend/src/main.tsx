import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

// Entry point for the React application. The app is mounted into the
// #root element defined in the static index.html file. StrictMode
// activates additional checks and warnings in development.
ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);