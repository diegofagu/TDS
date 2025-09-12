import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite configuration for the crypto alerts front‑end. The dev server
// listens on port 5173 by default and can be configured via environment
// variables. React fast refresh is enabled via the plugin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    open: false
  },
});