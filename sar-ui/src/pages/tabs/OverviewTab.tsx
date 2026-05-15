import type { SarReportDto } from '@/api/types';
import { formatDate, formatDateTime, formatAmount } from '@/lib/format';

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex gap-4 py-2.5 border-b border-slate-100 last:border-0">
      <dt className="w-44 flex-shrink-0 text-xs font-medium text-slate-500">{label}</dt>
      <dd className="text-sm text-slate-800">{value ?? '—'}</dd>
    </div>
  );
}

export function OverviewTab({ sar }: { sar: SarReportDto }) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div>
        <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500 mb-3">Filing Institution</h3>
        <dl>
          <Row label="Name" value={sar.filingInstitutionName} />
          <Row label="EIN" value={sar.filingInstitutionEin} />
          <Row label="Type" value={sar.filingInstitutionType} />
          <Row label="Contact Office" value={sar.contactOfficeName} />
          <Row label="Phone" value={sar.contactPhone} />
          <Row label="Email" value={sar.contactEmail} />
        </dl>
      </div>
      <div>
        <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500 mb-3">Activity</h3>
        <dl>
          <Row label="Case ID" value={sar.caseId} />
          <Row label="Activity From" value={formatDate(sar.activityFromDate)} />
          <Row label="Activity To" value={formatDate(sar.activityToDate)} />
          <Row label="Total Amount" value={formatAmount(sar.totalSuspiciousAmount)} />
          <Row label="Continuing Activity" value={sar.continuingActivity ? 'Yes' : 'No'} />
          <Row label="Joint Report" value={sar.jointReport ? 'Yes' : 'No'} />
        </dl>
      </div>
      <div>
        <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500 mb-3">Filing</h3>
        <dl>
          <Row label="BSA Identifier" value={sar.bsaIdentifier} />
          <Row label="Prior BSA ID" value={sar.priorBsaIdentifier} />
          <Row label="FinCEN Tracking #" value={sar.fincenTrackingNumber} />
          <Row label="Filing Date" value={formatDate(sar.filingDate)} />
          <Row label="Submitted At" value={formatDateTime(sar.submittedAt)} />
          <Row label="Acknowledged At" value={formatDateTime(sar.acknowledgedAt)} />
        </dl>
      </div>
      <div>
        <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500 mb-3">Record</h3>
        <dl>
          <Row label="Created" value={formatDateTime(sar.createdAt)} />
          <Row label="Created By" value={sar.createdBy} />
          <Row label="Updated" value={formatDateTime(sar.updatedAt)} />
          <Row label="Updated By" value={sar.updatedBy} />
        </dl>
      </div>
      {sar.narrative && (
        <div className="lg:col-span-2">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500 mb-3">Narrative</h3>
          <p className="whitespace-pre-wrap rounded-lg bg-slate-50 p-4 text-sm text-slate-700">{sar.narrative}</p>
        </div>
      )}
    </div>
  );
}
