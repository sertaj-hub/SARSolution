import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { useSarReport, useSubmitForReview, useApproveSar, useRejectSar, useValidateXml } from '@/hooks/useSarReport';
import { SarStatusBadge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Textarea } from '@/components/ui/Textarea';
import { ValidationPanel } from '@/components/ui/ValidationPanel';
import { Part1SubjectTab } from './form-tabs/Part1SubjectTab';
import { Part2ActivityTab } from './form-tabs/Part2ActivityTab';
import { Part3BranchAccountTab } from './form-tabs/Part3BranchAccountTab';
import { Part4FilingTab } from './form-tabs/Part4FilingTab';
import { Part5NarrativeTab } from './form-tabs/Part5NarrativeTab';
import { AuditTab } from './tabs/AuditTab';
import { formatDate, formatAmount } from '@/lib/format';
import { sarApi } from '@/api/sarApi';

type TabId = 'part1' | 'part2' | 'part3' | 'part4' | 'part5' | 'audit';

const TABS: { id: TabId; label: string }[] = [
  { id: 'part1', label: 'I · Subjects' },
  { id: 'part2', label: 'II · Activity' },
  { id: 'part3', label: 'III · Branches & Accounts' },
  { id: 'part4', label: 'IV · Filing Institution' },
  { id: 'part5', label: 'V · Narrative' },
  { id: 'audit', label: 'Audit Log' },
];

export function SarDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: sar, isPending, isError } = useSarReport(id);
  const submit = useSubmitForReview(id ?? '');
  const approve = useApproveSar(id ?? '');
  const reject = useRejectSar(id ?? '');
  const validateXml = useValidateXml(id ?? '');

  const [tab, setTab] = useState<TabId>('part1');
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [xmlResult, setXmlResult] = useState<string | null>(null);
  const [xmlOpen, setXmlOpen] = useState(false);
  const [violations, setViolations] = useState<string[]>([]);

  if (isPending) return <div className="card p-12 text-center text-sm text-slate-500">Loading…</div>;
  if (isError || !sar) return <div className="card p-12 text-center text-sm text-red-600">SAR report not found</div>;

  const canSubmit = sar.status === 'DRAFT';
  const canApprove = sar.status === 'IN_REVIEW';
  const canReject = sar.status === 'IN_REVIEW';
  const canPreviewXml = sar.status === 'APPROVED' || sar.status === 'SUBMITTED' || sar.status === 'ACKNOWLEDGED';

  const handleSubmit = async () => {
    setViolations([]);
    try {
      await submit.mutateAsync();
      toast.success('SAR submitted for review');
    } catch (err: unknown) {
      const e = err as { detail?: string; violations?: string[] };
      if (e.violations && e.violations.length > 0) {
        setViolations(e.violations);
      } else {
        toast.error(e.detail ?? 'Failed to submit for review');
      }
    }
  };

  const handleApprove = async () => {
    try { await approve.mutateAsync(); toast.success('SAR approved'); }
    catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  const handleReject = async () => {
    try { await reject.mutateAsync(rejectReason); toast.success('SAR rejected'); setRejectOpen(false); }
    catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  const handlePreviewXml = async () => {
    try { const xml = await sarApi.previewXml(sar.id); setXmlResult(xml); setXmlOpen(true); }
    catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Could not generate XML'); }
  };

  const handleValidate = async () => {
    try {
      const result = await validateXml.mutateAsync();
      if (result.valid) toast.success('XML is valid against FinCEN XSD schemas');
      else toast.error(`XML validation failed: ${result.errors.join('; ')}`);
    } catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Validation error'); }
  };

  return (
    <div className="flex flex-col gap-4">
      <button onClick={() => navigate('/sar')} className="self-start text-xs text-slate-500 hover:text-brand-700">← Back to reports</button>

      {/* Header card */}
      <div className="card p-5">
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <SarStatusBadge status={sar.status} />
              <span className="font-mono text-xs text-slate-500">{sar.reportNumber}</span>
            </div>
            <h1 className="text-xl font-semibold text-slate-900">{sar.filingInstitutionName}</h1>
            <div className="mt-1 flex flex-wrap gap-x-4 text-sm text-slate-500">
              {sar.activityFromDate && <span>Activity: {formatDate(sar.activityFromDate)} – {formatDate(sar.activityToDate)}</span>}
              {sar.totalSuspiciousAmount != null && <span>Amount: {formatAmount(sar.totalSuspiciousAmount)}</span>}
              {sar.subjects.length > 0 && <span>{sar.subjects.length} subject(s)</span>}
              {sar.activityTypes.length > 0 && <span>{sar.activityTypes.length} activity type(s)</span>}
            </div>
          </div>
          <div className="flex gap-2 flex-wrap justify-end">
            {canSubmit && (
              <Button size="sm" onClick={() => { void handleSubmit(); }} disabled={submit.isPending}>Submit for Review</Button>
            )}
            {canApprove && (
              <Button size="sm" onClick={() => { void handleApprove(); }} disabled={approve.isPending}>Approve</Button>
            )}
            {canReject && (
              <Button size="sm" variant="danger" onClick={() => setRejectOpen(true)}>Reject</Button>
            )}
            {canPreviewXml && (
              <>
                <Button size="sm" variant="secondary" onClick={() => { void handlePreviewXml(); }}>Preview XML</Button>
                <Button size="sm" variant="secondary" onClick={() => { void handleValidate(); }} disabled={validateXml.isPending}>Validate XML</Button>
              </>
            )}
          </div>
        </div>
      </div>

      <ValidationPanel violations={violations} onDismiss={() => setViolations([])} />

      {/* 5-part form tabs */}
      <div className="card overflow-hidden">
        <nav className="flex overflow-x-auto border-b border-slate-200 bg-slate-50">
          {TABS.map((t, i) => (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className={`
                flex-shrink-0 flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors whitespace-nowrap
                ${tab === t.id
                  ? 'border-brand-700 text-brand-700 bg-white'
                  : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                }
              `}
            >
              {i < 5 && (
                <span className={`
                  inline-flex items-center justify-center w-5 h-5 rounded-full text-xs font-bold flex-shrink-0
                  ${tab === t.id ? 'bg-brand-700 text-white' : 'bg-slate-200 text-slate-600'}
                `}>
                  {i + 1}
                </span>
              )}
              {t.label}
            </button>
          ))}
        </nav>

        <div className="p-6">
          {tab === 'part1' && <Part1SubjectTab sar={sar} />}
          {tab === 'part2' && <Part2ActivityTab sar={sar} />}
          {tab === 'part3' && <Part3BranchAccountTab sar={sar} />}
          {tab === 'part4' && <Part4FilingTab sar={sar} />}
          {tab === 'part5' && <Part5NarrativeTab sar={sar} />}
          {tab === 'audit' && <AuditTab sarId={sar.id} />}
        </div>
      </div>

      <Modal open={rejectOpen} title="Reject SAR" onClose={() => setRejectOpen(false)}
        footer={<>
          <Button variant="secondary" onClick={() => setRejectOpen(false)}>Cancel</Button>
          <Button variant="danger" onClick={() => { void handleReject(); }} disabled={reject.isPending || !rejectReason.trim()}>Reject</Button>
        </>}
      >
        <Textarea label="Rejection reason *" rows={4} value={rejectReason} onChange={e => setRejectReason(e.target.value)} placeholder="Explain why this SAR is being rejected…" />
      </Modal>

      <Modal open={xmlOpen} title="BSA XML Preview" onClose={() => setXmlOpen(false)}>
        <pre className="max-h-96 overflow-auto rounded bg-slate-50 p-3 text-xs text-slate-700 whitespace-pre-wrap">{xmlResult}</pre>
      </Modal>
    </div>
  );
}
