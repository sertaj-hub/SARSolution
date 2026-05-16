import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { toast } from 'sonner';
import { useSarReport, useSubmitForReview } from '@/hooks/useSarReport';
import { SarStatusBadge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { ValidationPanel } from '@/components/ui/ValidationPanel';
import { Part1SubjectTab } from './form-tabs/Part1SubjectTab';
import { Part2ActivityTab } from './form-tabs/Part2ActivityTab';
import { Part3BranchAccountTab } from './form-tabs/Part3BranchAccountTab';
import { Part4FilingTab } from './form-tabs/Part4FilingTab';
import { Part5NarrativeTab } from './form-tabs/Part5NarrativeTab';

type TabId = 'part1' | 'part2' | 'part3' | 'part4' | 'part5';

const TABS: { id: TabId; label: string; short: string }[] = [
  { id: 'part1', label: 'Part I – Subject Information', short: 'I: Subjects' },
  { id: 'part2', label: 'Part II – Suspicious Activity', short: 'II: Activity' },
  { id: 'part3', label: 'Part III – Branches & Accounts', short: 'III: Branches' },
  { id: 'part4', label: 'Part IV – Filing Institution', short: 'IV: Filing FI' },
  { id: 'part5', label: 'Part V – Narrative', short: 'V: Narrative' },
];

export function SarFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: sar, isPending, isError } = useSarReport(id);
  const submitForReview = useSubmitForReview(id ?? '');
  const [tab, setTab] = useState<TabId>('part1');
  const [violations, setViolations] = useState<string[]>([]);

  const handleSubmitForReview = async () => {
    setViolations([]);
    try {
      await submitForReview.mutateAsync();
      toast.success('SAR submitted for review');
      navigate(`/sar/${id}`);
    } catch (err: unknown) {
      const e = err as { detail?: string; violations?: string[] };
      if (e.violations && e.violations.length > 0) {
        setViolations(e.violations);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      } else {
        toast.error(e.detail ?? 'Failed to submit for review');
      }
    }
  };

  if (isPending) return <div className="card p-12 text-center text-sm text-slate-500">Loading…</div>;
  if (isError || !sar) return <div className="card p-12 text-center text-sm text-red-600">SAR report not found</div>;

  const currentIdx = TABS.findIndex(t => t.id === tab);
  const prevTab = currentIdx > 0 ? TABS[currentIdx - 1] : null;
  const nextTab = currentIdx < TABS.length - 1 ? TABS[currentIdx + 1] : null;

  return (
    <div className="flex flex-col gap-4">
      {/* Header */}
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(`/sar/${sar.id}`)}
            className="text-xs text-slate-500 hover:text-brand-700"
          >
            ← Back to detail
          </button>
          <span className="text-slate-300">|</span>
          <SarStatusBadge status={sar.status} />
          <span className="font-mono text-xs text-slate-500">{sar.reportNumber}</span>
          <span className="text-sm font-medium text-slate-700">{sar.filingInstitutionName}</span>
        </div>
        {sar.status === 'DRAFT' && (
          <Button size="sm" onClick={() => { void handleSubmitForReview(); }} disabled={submitForReview.isPending}>
            {submitForReview.isPending ? 'Submitting…' : 'Submit for Review'}
          </Button>
        )}
      </div>

      <ValidationPanel violations={violations} onDismiss={() => setViolations([])} />

      <div className="card overflow-hidden">
        {/* Tab Bar */}
        <div className="border-b border-slate-200 bg-slate-50">
          <nav className="flex overflow-x-auto">
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
                <span className={`
                  inline-flex items-center justify-center w-5 h-5 rounded-full text-xs font-bold
                  ${tab === t.id ? 'bg-brand-700 text-white' : 'bg-slate-200 text-slate-600'}
                `}>
                  {i + 1}
                </span>
                <span className="hidden lg:block">{t.label}</span>
                <span className="lg:hidden">{t.short}</span>
              </button>
            ))}
          </nav>
        </div>

        {/* Content */}
        <div className="p-6">
          {tab === 'part1' && <Part1SubjectTab sar={sar} />}
          {tab === 'part2' && <Part2ActivityTab sar={sar} />}
          {tab === 'part3' && <Part3BranchAccountTab sar={sar} />}
          {tab === 'part4' && <Part4FilingTab sar={sar} />}
          {tab === 'part5' && <Part5NarrativeTab sar={sar} />}
        </div>

        {/* Navigation Footer */}
        <div className="border-t border-slate-100 bg-slate-50 px-6 py-3 flex items-center justify-between">
          <div>
            {prevTab ? (
              <button
                onClick={() => setTab(prevTab.id)}
                className="text-sm text-slate-600 hover:text-brand-700 flex items-center gap-1"
              >
                ← {prevTab.short}
              </button>
            ) : (
              <span />
            )}
          </div>
          <Link
            to={`/sar/${sar.id}`}
            className="text-xs text-slate-400 hover:text-slate-600"
          >
            Return to overview
          </Link>
          <div>
            {nextTab ? (
              <button
                onClick={() => setTab(nextTab.id)}
                className="text-sm text-slate-600 hover:text-brand-700 flex items-center gap-1"
              >
                {nextTab.short} →
              </button>
            ) : (
              <span className="text-xs text-slate-400">All parts complete</span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
