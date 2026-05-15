import { useAuditHistory } from '@/hooks/useSarReport';
import { formatDateTime, humanize } from '@/lib/format';

export function AuditTab({ sarId }: { sarId: string }) {
  const { data, isPending } = useAuditHistory(sarId);

  if (isPending) return <div className="py-12 text-center text-sm text-slate-500">Loading audit log…</div>;
  if (!data || data.content.length === 0)
    return <div className="py-12 text-center text-sm text-slate-500">No audit events yet.</div>;

  return (
    <div className="space-y-2">
      {data.content.map(log => (
        <div key={log.id} className="flex gap-4 rounded-lg border border-slate-100 bg-slate-50 px-4 py-3 text-sm">
          <div className="min-w-32 text-xs text-slate-400">{formatDateTime(log.performedAt)}</div>
          <div className="flex-1">
            <span className="font-medium text-slate-800">{humanize(log.action)}</span>
            {log.fieldName && (
              <span className="ml-2 text-xs text-slate-500">
                {log.fieldName}: <span className="line-through text-red-400">{log.oldValue}</span> → <span className="text-green-600">{log.newValue}</span>
              </span>
            )}
            {log.reason && <p className="mt-0.5 text-xs text-slate-500">{log.reason}</p>}
          </div>
          <div className="text-xs text-slate-400 text-right">{log.performedBy}</div>
        </div>
      ))}
    </div>
  );
}
