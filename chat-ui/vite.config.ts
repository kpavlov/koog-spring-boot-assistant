import {defineConfig} from 'vite'
import {svelte} from '@sveltejs/vite-plugin-svelte'

// https://vite.dev/config/
export default defineConfig({
    plugins: [svelte()],
    server: {
        port: 3000,
        open: true, // Automatically opens browser
        strictPort: false, // Exit if port is already in use
        proxy: {
            '/api': {
                target: 'http://127.0.0.1:8080',
                changeOrigin: true,
                secure: false
            }
        }
    }
})
