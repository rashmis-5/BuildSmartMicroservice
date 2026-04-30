export default function Loader({ inline = false, label = 'Loading…' }) {
  if (inline) {
    return (
      <span className="text-muted small">
        <span className="spinner-border spinner-border-sm me-2" role="status" />
        {label}
      </span>
    );
  }
  return (
    <div className="d-flex align-items-center text-muted py-3">
      <div className="spinner-border spinner-border-sm me-2" role="status" />
      {label}
    </div>
  );
}
