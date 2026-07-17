import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The API base is read from VITE_API_BASE (defaults to localhost:4000).
// A dev proxy also forwards /api and /socket.io so you can leave it unset.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:4000',
      '/socket.io': { target: 'http://localhost:4000', ws: true },
    },
  },
});
