import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useMemo,
  type ReactNode,
} from 'react';
import { theme as antdTheme, type ThemeConfig } from 'antd';

type ThemeMode = 'light' | 'dark';

interface ThemeContextValue {
  mode: ThemeMode;
  toggle: () => void;
  config: ThemeConfig;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

const lightTokens: ThemeConfig['token'] = {
  colorPrimary: '#4f46e5',
  colorBgContainer: '#ffffff',
  colorBgLayout: '#f5f5f5',
  colorBgElevated: '#ffffff',
  colorText: '#1a1a1a',
  colorTextSecondary: '#666666',
  colorBorder: '#e0e0e0',
  colorError: '#dc2626',
};

const darkTokens: ThemeConfig['token'] = {
  colorPrimary: '#818cf8',
  colorBgContainer: '#1a1a2e',
  colorBgLayout: '#16213e',
  colorBgElevated: '#1e1e3a',
  colorText: '#eaeaea',
  colorTextSecondary: '#a0a0a0',
  colorBorder: '#333355',
  colorError: '#f87171',
};

function getInitialMode(): ThemeMode {
  if (typeof window === 'undefined') return 'light';
  const stored = localStorage.getItem('theme');
  if (stored === 'light' || stored === 'dark') return stored;
  return window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light';
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<ThemeMode>(getInitialMode);

  useEffect(() => {
    localStorage.setItem('theme', mode);
  }, [mode]);

  const toggle = useCallback(() => {
    setMode((prev) => (prev === 'light' ? 'dark' : 'light'));
  }, []);

  const config = useMemo<ThemeConfig>(
    () => ({
      cssVar: true,
      hashed: false,
      algorithm:
        mode === 'dark'
          ? antdTheme.darkAlgorithm
          : antdTheme.defaultAlgorithm,
      token: mode === 'dark' ? darkTokens : lightTokens,
    }),
    [mode],
  );

  const value = useMemo(() => ({ mode, toggle, config }), [mode, toggle, config]);

  return (
    <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider');
  return ctx;
}
