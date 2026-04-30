import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Generic loading + error + data hook around an async function.
 * Pass `args` to control re-fetching when args change.
 */
export function useAsync(fn, args = [], { auto = true } = {}) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const argsRef = useRef(args);
  argsRef.current = args;

  const run = useCallback(async (...overrideArgs) => {
    setLoading(true);
    setError(null);
    try {
      const result = await fn(...(overrideArgs.length ? overrideArgs : argsRef.current));
      setData(result);
      return result;
    } catch (e) {
      // Capture error in state. Do NOT re-throw: callers that fire-and-forget
      // (e.g. refresh buttons) would otherwise produce unhandled rejections.
      // Callers that need success/failure can read `error` from the hook.
      setError(extractMessage(e));
      return undefined;
    } finally {
      setLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fn]);

  useEffect(() => {
    if (auto) run();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auto, ...args]);

  return { data, error, loading, run, setData };
}

export function extractMessage(err) {
  if (!err) return 'Unknown error';
  if (err.response?.data?.message) return err.response.data.message;
  if (err.response?.data?.error) return err.response.data.error;
  if (typeof err.response?.data === 'string') return err.response.data;
  if (err.message) return `${err.message}${err.response?.status ? ` (${err.response.status})` : ''}`;
  return String(err);
}
