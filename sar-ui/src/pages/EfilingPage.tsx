import { toast } from 'sonner';
import { useEfilingBatches, useTriggerBatch, useResubmitBatch } from '@/hooks/useEfiling';
import { FilingStatusBadge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { formatDateTime } from '@/lib/format';

export function EfilingPage() {
  const { data, isPending, isError } = useEfilingBatches();
  const trigger = useTriggerBatch();
  const resubmit = useResubmitBatch();

  const handleTrigger = async () => {
    try {
      const res = await trigger.mutateAsync();
      toast.success(res['message'] ?? 'Batch job triggered');
    } catch (err: unknown) {
      toast.error((err as { detail?: string })?.detail ?? 'Failed to trigger batch');
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">eFiling Batches</h1>
          <p className="text-sm text-slate-500">FinCEN BSA E-Filing submissions</p>
        </div>
        <Button onClick={() => { void handleTrigger(); }} disabled={trigger.isPending}>
          {trigger.isPending ? 'Triggering…' : 'Trigger Batch Job'}
        </Button>
      </div>

      <div className="card overflow-hidden">
        {isPending ? (
          <div className="p-12 text-center text-sm text-slate-500">Loading…</div>
        ) : isError ? (
          <div className="p-12 text-center text-sm text-red-600">Failed to load batches</div>
        ) : !data || data.length === 0 ? (
          <div className="p-12 text-center text-sm text-slate-500">No batches yet. Trigger a batch job to create one.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-xs font-medium text-slate-500 uppercase">
                <th className="px-4 py-3 text-left">Batch #</th>
                <th className="px-4 py-3 text-left">Transmitter</th>
                <th className="px-4 py-3 text-left">Status</th>
                <th className="px-4 py-3 text-center">SAR Count</th>
                <th className="px-4 py-3 text-left">Generated</th>
                <th className="px-4 py-3 text-left">Submitted</th>
                <th className="px-4 py-3 text-left">Acknowledged</th>
                <th className="px-4 py-3 text-left">FinCEN Tracking</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.map(b => (
                <tr key={b.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 font-mono text-xs">{b.batchNumber}</td>
                  <td className="px-4 py-3">{b.transmitterName}</td>
                  <td className="px-4 py-3"><FilingStatusBadge status={b.status} /></td>
                  <td className="px-4 py-3 text-center">{b.activityCount}</td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(b.generatedAt)}</td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(b.submittedAt)}</td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(b.acknowledgedAt)}</td>
                  <td className="px-4 py-3 font-mono text-xs">{b.fincenTrackingId ?? '—'}</td>
                  <td className="px-4 py-3 text-right">
                    {(b.status === 'REJECTED' || b.status === 'FAILED') && (
                      <button
                        onClick={() => { void (async () => { try { await resubmit.mutateAsync(b.id); toast.success('Resubmitted'); } catch { toast.error('Failed'); } })(); }}
                        className="text-xs text-brand-700 hover:underline"
                      >
                        Resubmit
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
