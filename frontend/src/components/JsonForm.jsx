import { useState } from 'react';

/**
 * Simple form: a list of `fields` ({ name, label, type?, required?, placeholder?, options? }).
 * - type defaults to "text". `select` with `options=[{value,label}]`. `textarea`.
 *   `number`. `date`. `json` (textarea that parses to an object).
 * - calls onSubmit(values) and resets on success.
 */
export default function JsonForm({ fields, onSubmit, submitLabel = 'Save', initialValues }) {
  const [values, setValues] = useState(() => {
    const seed = {};
    fields.forEach((f) => {
      seed[f.name] = initialValues?.[f.name] ?? (f.type === 'number' ? '' : '');
    });
    return seed;
  });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  const update = (name) => (e) => {
    const value =
      e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setValues((v) => ({ ...v, [name]: value }));
  };

  const handle = async (e) => {
    e.preventDefault();
    setErr('');
    const out = {};
    for (const f of fields) {
      let v = values[f.name];
      if (f.type === 'json') {
        try {
          v = v ? JSON.parse(v) : null;
        } catch (parseErr) {
          setErr(`Invalid JSON in "${f.label || f.name}": ${parseErr.message}`);
          return;
        }
      } else if (f.type === 'number') {
        v = v === '' || v === null ? null : Number(v);
      }
      out[f.name] = v;
    }
    setBusy(true);
    try {
      await onSubmit(out);
    } catch (e2) {
      setErr(e2?.response?.data?.message || e2?.message || 'Request failed');
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={handle} className="row g-2">
      {err && <div className="col-12 alert alert-danger py-2 mb-0">{err}</div>}
      {fields.map((f) => (
        <div key={f.name} className={f.col || 'col-md-6'}>
          <label className="form-label small mb-1">
            {f.label || f.name}
            {f.required && <span className="text-danger ms-1">*</span>}
          </label>
          {f.type === 'select' ? (
            <select
              className="form-select form-select-sm"
              value={values[f.name] ?? ''}
              onChange={update(f.name)}
              required={f.required}
            >
              <option value="" disabled>
                — select —
              </option>
              {f.options.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          ) : f.type === 'textarea' || f.type === 'json' ? (
            <textarea
              className="form-control form-control-sm"
              rows={f.rows || 3}
              value={values[f.name] ?? ''}
              onChange={update(f.name)}
              placeholder={f.placeholder || (f.type === 'json' ? '{ }' : '')}
              required={f.required}
            />
          ) : (
            <input
              className="form-control form-control-sm"
              type={f.type || 'text'}
              value={values[f.name] ?? ''}
              onChange={update(f.name)}
              placeholder={f.placeholder}
              required={f.required}
            />
          )}
        </div>
      ))}
      <div className="col-12 d-flex justify-content-end">
        <button type="submit" className="btn btn-sm btn-primary" disabled={busy}>
          {busy ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  );
}
