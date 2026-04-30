import { useState } from 'react';

/**
 * `tabs` is [{ key, label, content: () => ReactNode }]. Content is lazily
 * rendered so unmounted tabs don't fetch data.
 */
export default function Tabs({ tabs, defaultKey }) {
  const [active, setActive] = useState(defaultKey || tabs[0]?.key);
  const current = tabs.find((t) => t.key === active);

  return (
    <>
      <ul className="nav nav-tabs mb-3 flex-wrap">
        {tabs.map((t) => (
          <li key={t.key} className="nav-item">
            <button
              type="button"
              className={`nav-link ${active === t.key ? 'active' : ''}`}
              onClick={() => setActive(t.key)}
            >
              {t.label}
            </button>
          </li>
        ))}
      </ul>
      <div>{current?.content()}</div>
    </>
  );
}
