interface Props {
  violations: string[];
  onDismiss: () => void;
}

export function ValidationPanel({ violations, onDismiss }: Props) {
  if (violations.length === 0) return null;
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1">
          <p className="text-sm font-semibold text-red-800 mb-2">
            Please fix {violations.length} issue{violations.length > 1 ? 's' : ''} before submitting:
          </p>
          <ul className="space-y-1">
            {violations.map((v, i) => (
              <li key={i} className="flex items-start gap-2 text-sm text-red-700">
                <span className="mt-0.5 flex-shrink-0 w-4 h-4 rounded-full bg-red-200 text-red-800 text-xs flex items-center justify-center font-bold">{i + 1}</span>
                {v}
              </li>
            ))}
          </ul>
        </div>
        <button onClick={onDismiss} className="flex-shrink-0 text-red-400 hover:text-red-700 text-lg leading-none">×</button>
      </div>
    </div>
  );
}
