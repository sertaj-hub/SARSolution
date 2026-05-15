import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useSarReports } from '@/hooks/useSarReports';
import { SarStatusBadge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Select } from '@/components/ui/Select';
import { Pagination } from '@/components/ui/Pagination';
import { ALL_SAR_STATUSES, type SarStatus } from '@/api/types';
import { formatDate, formatAmount, humanize } from '@/lib/format';

export function SarListPage() {
  const [status, setStatus] = useState<SarStatus | ''>('');
  const [page, setPage] = useState(0);
  const { data, isPending, isError } = useSarReports({ status: status || undefined, page, size: 20, sort: 'createdAt,desc' });

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">SAR Reports</h1>
          <p className="text-sm text-slate-500">Suspicious Activity Reports</p>
        </div>
        <Link to="/sar/new"><Button>New SAR</Button></Link>
      </div>

      <div className="flex gap-3 items-end">
        <div className="w-44">
          <Select label="Status" value={status} onChange={e => { setStatus(e.target.value as SarStatus | ''); setPage(0); }}>
            <option value="">All statuses</option>
            {ALL_SAR_STATUSES.map(s => <option key={s} value={s}>{humanize(s)}</option>)}
          </Select>
        </div>
      </div>

      <div className="card overflow-hidden">
        {isPending ? (
          <div className="p-12 text-center text-sm text-slate-500">Loading…</div>
        ) : isError ? (
          <div className="p-12 text-center text-sm text-red-600">Failed to load reports</div>
        ) : !data || data.content.length === 0 ? (
          <div className="p-16 text-center">
            <p className="text-base font-medium text-slate-700">No SAR reports yet</p>
            <p className="text-sm text-slate-400 mt-1 mb-4">Create your first SAR to get started</p>
            <Link to="/sar/new"><Button>+ New SAR Report</Button></Link>
          </div>
        ) : (
          <>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-medium text-slate-500 uppercase tracking-wide">
                  <th className="px-4 py-3 text-left">Report #</th>
                  <th className="px-4 py-3 text-left">Institution</th>
                  <th className="px-4 py-3 text-left">Status</th>
                  <th className="px-4 py-3 text-left">Activity Period</th>
                  <th className="px-4 py-3 text-right">Amount</th>
                  <th className="px-4 py-3 text-left">Created</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.content.map(r => (
                  <tr key={r.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-slate-700">{r.reportNumber}</td>
                    <td className="px-4 py-3 text-slate-900 font-medium max-w-xs truncate">{r.filingInstitutionName}</td>
                    <td className="px-4 py-3"><SarStatusBadge status={r.status} /></td>
                    <td className="px-4 py-3 text-slate-600">
                      {r.activityFromDate && r.activityToDate
                        ? `${formatDate(r.activityFromDate)} – ${formatDate(r.activityToDate)}`
                        : '—'}
                    </td>
                    <td className="px-4 py-3 text-right text-slate-700">{formatAmount(r.totalSuspiciousAmount)}</td>
                    <td className="px-4 py-3 text-slate-500">{formatDate(r.createdAt)}</td>
                    <td className="px-4 py-3 text-right">
                      <Link to={`/sar/${r.id}`} className="text-brand-700 hover:underline font-medium">View</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={data.number} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />
          </>
        )}
      </div>
    </div>
  );
}
