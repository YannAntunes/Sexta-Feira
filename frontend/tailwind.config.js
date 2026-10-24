/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        hologram: {
          cyan: '#00f3ff',
          blue: '#0066ff',
          purple: '#7928ca',
          gold: '#f5a623',
          red: '#ff0055',
          green: '#00ff88',
        },
        void: {
          950: '#030712',
          900: '#070d1d',
          800: '#0a1428',
          700: '#111e3b',
        }
      },
      fontFamily: {
        mono: ['"JetBrains Mono"', 'monospace'],
        sans: ['"Inter"', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        'glow-cyan': '0 0 25px rgba(0, 243, 255, 0.45)',
        'glow-blue': '0 0 25px rgba(0, 102, 255, 0.45)',
        'glow-purple': '0 0 25px rgba(121, 40, 202, 0.45)',
        'glow-red': '0 0 25px rgba(255, 0, 85, 0.45)',
      },
      animation: {
        'scanline': 'scanline 8s linear infinite',
        'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        scanline: {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(1000%)' },
        }
      }
    },
  },
  plugins: [],
}
