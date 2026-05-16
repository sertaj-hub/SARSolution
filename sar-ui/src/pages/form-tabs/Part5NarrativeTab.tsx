import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import type { SarReportDto } from '@/api/types';
import { useUpdateSar } from '@/hooks/useSarReport';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';

interface NarrativeFields {
  narrative?: string;
  filingDate?: string;
  fiNotedSuspiciousActivity?: boolean;
}

const MAX_NARRATIVE = 20000;

export function Part5NarrativeTab({ sar }: { sar: SarReportDto }) {
  const updateSar = useUpdateSar(sar.id);

  const { register, handleSubmit, watch, formState: { errors, isDirty } } = useForm<NarrativeFields>({
    defaultValues: {
      narrative: sar.narrative ?? '',
      filingDate: sar.filingDate ?? '',
      fiNotedSuspiciousActivity: sar.fiNotedSuspiciousActivity ?? true,
    },
  });

  const narrative = watch('narrative') ?? '';
  const charCount = narrative.length;

  const handleSave = async (data: NarrativeFields) => {
    if (data.narrative && data.narrative.length > MAX_NARRATIVE) {
      toast.error(`Narrative exceeds ${MAX_NARRATIVE.toLocaleString()} character limit`);
      return;
    }
    try {
      await updateSar.mutateAsync(data as Partial<SarReportDto>);
      toast.success('Narrative saved');
    } catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  return (
    <form onSubmit={e => { void handleSubmit(handleSave)(e); }} className="space-y-5">
      <div>
        <h3 className="text-sm font-semibold text-slate-900">Part V – Suspicious Activity Narrative</h3>
        <p className="text-xs text-slate-500 mt-0.5">
          Describe the conduct giving rise to the suspicion. Include who, what, when, where, and how.
          Up to {MAX_NARRATIVE.toLocaleString()} characters.
        </p>
      </div>

      <div>
        <div className="flex items-center justify-between mb-1">
          <label className="label">Narrative *</label>
          <span className={`text-xs ${charCount > MAX_NARRATIVE ? 'text-red-600 font-semibold' : charCount > MAX_NARRATIVE * 0.9 ? 'text-orange-600' : 'text-slate-400'}`}>
            {charCount.toLocaleString()} / {MAX_NARRATIVE.toLocaleString()}
          </span>
        </div>
        <Textarea
          rows={20}
          {...register('narrative', {
            required: 'Narrative is required',
            minLength: { value: 17, message: 'Narrative must be at least 17 characters' },
            maxLength: { value: MAX_NARRATIVE, message: `Narrative exceeds ${MAX_NARRATIVE.toLocaleString()} character limit` },
          })}
          error={errors.narrative?.message}
          placeholder="Describe the suspicious activity in detail. Include:
• Who is involved (subjects, accounts, institutions)
• What type of suspicious activity occurred
• When the activity took place (specific dates/times)
• Where the activity occurred (locations, branches)
• How the activity was conducted (methods, instruments)
• Why it is suspicious (patterns, anomalies, red flags)
• Any corrective action taken"
        />
        {charCount > MAX_NARRATIVE && (
          <p className="mt-1 text-xs text-red-600">Narrative exceeds the maximum allowed length</p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3">
        <Input
          label="Filing Date"
          type="date"
          {...register('filingDate')}
        />
        <div className="flex items-end pb-2">
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('fiNotedSuspiciousActivity')} className="accent-brand-700" />
            Financial institution noted the suspicious activity
          </label>
        </div>
      </div>

      {sar.narrative && (
        <div className="rounded-lg bg-amber-50 border border-amber-200 p-3">
          <p className="text-xs font-medium text-amber-800 mb-1">Writing Guidance</p>
          <ul className="text-xs text-amber-700 space-y-0.5 list-disc list-inside">
            <li>Use clear, factual language — avoid legal conclusions</li>
            <li>Cite specific transaction dates, amounts, and account numbers</li>
            <li>Reference BSA red flags and typologies where applicable</li>
            <li>Document all attempts to contact or verify the subject</li>
            <li>Include any law enforcement contacts or referrals</li>
          </ul>
        </div>
      )}

      <div className="flex gap-2 pt-2 border-t border-slate-100">
        <Button type="submit" disabled={updateSar.isPending || !isDirty || charCount > MAX_NARRATIVE}>
          {updateSar.isPending ? 'Saving…' : 'Save Narrative'}
        </Button>
        <span className="text-xs text-slate-400 self-center">
          {sar.updatedAt ? `Last saved: ${new Date(sar.updatedAt).toLocaleString()}` : 'Not yet saved'}
        </span>
      </div>
    </form>
  );
}
