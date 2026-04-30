import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { decodeJwt, isExpired } from './jwt.js';

const STORAGE_KEY = 'buildsmart.token';
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(STORAGE_KEY));

  useEffect(() => {
    if (token) {
      localStorage.setItem(STORAGE_KEY, token);
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, [token]);

  const value = useMemo(() => {
    const claims = token ? decodeJwt(token) : null;
    const expired = claims && isExpired(claims);
    const isAuthenticated = !!token && !!claims && !expired;
    return {
      token: isAuthenticated ? token : null,
      claims: isAuthenticated ? claims : null,
      role: isAuthenticated ? claims?.role : null,
      userId: isAuthenticated ? claims?.userId : null,
      name: isAuthenticated ? claims?.name : null,
      email: isAuthenticated ? claims?.sub : null,
      isAuthenticated,
      login: (newToken) => setToken(newToken),
      logout: () => setToken(null),
    };
  }, [token]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
