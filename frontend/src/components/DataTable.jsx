/**
 * Renders an array of rows as a Bootstrap table.
 * - `columns` is [{ key, label, render?: (row) => ReactNode }]
 * - If `columns` is omitted we infer from the keys of the first row.
 * - `actions` is optional: (row) => ReactNode rendered in the last column.
 * - Wrapped in `table-responsive` so it scrolls horizontally on narrow screens.
 */
export default function DataTable({ rows, columns, actions, emptyMessage = 'No records.' }) {
  const list = Array.isArray(rows) ? rows : rows?.content ?? [];
  if (!list || list.length === 0) {
    return <p className="text-muted small mb-0">{emptyMessage}</p>;
  }
  const cols =
    columns ||
    Object.keys(list[0]).map((k) => ({ key: k, label: k }));

  return (
    <div className="table-responsive">
      <table className="table table-sm table-striped table-hover align-middle">
        <thead>
          <tr>
            {cols.map((c) => (
              <th key={c.key}>{c.label}</th>
            ))}
            {actions && <th style={{ width: 1 }}>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {list.map((row, i) => (
            <tr key={row.id || row.userId || row.notificationId || row.allocationId || i}>
              {cols.map((c) => (
                <td key={c.key}>{c.render ? c.render(row) : formatCell(row[c.key])}</td>
              ))}
              {actions && <td className="text-nowrap">{actions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatCell(value) {
  if (value === null || value === undefined) return '';
  if (typeof value === 'object') return JSON.stringify(value);
  if (typeof value === 'boolean') return value ? '✓' : '—';
  return String(value);
}
