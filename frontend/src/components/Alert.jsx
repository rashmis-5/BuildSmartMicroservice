export function ErrorAlert({ message, onClose }) {
  if (!message) return null;
  return (
    <div className="alert alert-danger d-flex justify-content-between align-items-start py-2" role="alert">
      <span style={{ whiteSpace: 'pre-wrap' }}>{message}</span>
      {onClose && (
        <button type="button" className="btn-close" onClick={onClose} aria-label="Close" />
      )}
    </div>
  );
}

export function InfoAlert({ message, onClose }) {
  if (!message) return null;
  return (
    <div className="alert alert-success d-flex justify-content-between align-items-start py-2">
      <span>{message}</span>
      {onClose && (
        <button type="button" className="btn-close" onClick={onClose} aria-label="Close" />
      )}
    </div>
  );
}
