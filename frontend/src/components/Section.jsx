import { useState } from 'react';

/**
 * Bootstrap card with a title, optional collapsible body, and right-aligned
 * "actions" slot in the header (refresh button, etc.).
 */
export default function Section({
  title,
  subtitle,
  actions,
  children,
  collapsible = false,
  defaultOpen = true,
}) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="card mb-3 shadow-sm">
      <div className="card-header d-flex justify-content-between align-items-center">
        <div>
          <h6 className="mb-0">{title}</h6>
          {subtitle && <div className="small text-muted">{subtitle}</div>}
        </div>
        <div className="d-flex gap-2">
          {actions}
          {collapsible && (
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              onClick={() => setOpen((v) => !v)}
            >
              {open ? 'Hide' : 'Show'}
            </button>
          )}
        </div>
      </div>
      {(!collapsible || open) && <div className="card-body">{children}</div>}
    </div>
  );
}
