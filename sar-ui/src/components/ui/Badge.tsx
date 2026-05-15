import type { SarStatus, FilingStatus } from '@/api/types';
import { humanize } from '@/lib/format';

const SAR_COLORS: Record<SarStatus, string> = {
  DRAFT:        'bg-slate-100 text-slate-700',
  IN_REVIEW:    'bg-yellow-100 text-yellow-800',
  APPROVED:     'bg-blue-100 text-blue-800',
  SUBMITTED:    'bg-purple-100 text-purple-800',
  ACKNOWLEDGED: 'bg-green-100 text-green-800',
  AMENDED:      'bg-orange-100 text-orange-800',
};

const FILING_COLORS: Record<FilingStatus, string> = {
  PENDING:    'bg-slate-100 text-slate-700',
  GENERATING: 'bg-yellow-100 text-yellow-800',
  GENERATED:  'bg-blue-100 text-blue-800',
  SUBMITTED:  'bg-purple-100 text-purple-800',
  ACCEPTED:   'bg-green-100 text-green-800',
  REJECTED:   'bg-red-100 text-red-800',
  FAILED:     'bg-red-100 text-red-800',
};

export function SarStatusBadge({ status }: { status: SarStatus }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${SAR_COLORS[status]}`}>
      {humanize(status)}
    </span>
  );
}

export function FilingStatusBadge({ status }: { status: FilingStatus }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${FILING_COLORS[status]}`}>
      {humanize(status)}
    </span>
  );
}
