/**
 * Minimal JWT decoder — reads the payload claims without verifying the signature.
 * Verification happens server-side; the frontend just needs to read userId/role/exp
 * for routing and UI gating.
 */
export function decodeJwt(token) {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = payload + '='.repeat((4 - (payload.length % 4)) % 4);
    const json = atob(padded);
    return JSON.parse(decodeURIComponent(escape(json)));
  } catch {
    return null;
  }
}

export function isExpired(claims) {
  if (!claims || !claims.exp) return false;
  return Date.now() >= claims.exp * 1000;
}
