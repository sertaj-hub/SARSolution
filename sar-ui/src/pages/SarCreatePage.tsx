import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { useCreateSar } from '@/hooks/useCreateSar';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import type { CreateSarRequest } from '@/api/types';

export function SarCreatePage() {
  const navigate = useNavigate();
  const { mutateAsync, isPending } = useCreateSar();
  const { register, handleSubmit, formState: { errors } } = useForm<CreateSarRequest>();

  const onSubmit = async (data: CreateSarRequest) => {
    try {
      const created = await mutateAsync(data);
      toast.success(`SAR ${created.reportNumber} created`);
      navigate(`/sar/${created.id}`);
    } catch (err: unknown) {
      toast.error((err as { detail?: string })?.detail ?? 'Failed to create SAR');
    }
  };

  return (
    <div className="max-w-2xl">
      <div className="mb-6">
        <button onClick={() => navigate('/sar')} className="text-xs text-slate-500 hover:text-brand-700">← Back</button>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">New SAR Report</h1>
        <p className="text-sm text-slate-500">Create a new Suspicious Activity Report (DRAFT)</p>
      </div>

      <form onSubmit={e => { void handleSubmit(onSubmit)(e); }} className="card p-6 space-y-5">
        <div className="space-y-1">
          <h2 className="text-sm font-semibold text-slate-700 border-b border-slate-100 pb-2">Filing Institution</h2>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div className="col-span-2">
            <Input
              label="Institution Name *"
              {...register('filingInstitutionName', { required: 'Required' })}
              error={errors.filingInstitutionName?.message}
              placeholder="First National Bank"
            />
          </div>
          <Input label="EIN" {...register('filingInstitutionEin')} placeholder="12-3456789" />
          <Input label="Institution Type" {...register('filingInstitutionType')} placeholder="Bank" />
        </div>

        <div className="space-y-1 pt-2">
          <h2 className="text-sm font-semibold text-slate-700 border-b border-slate-100 pb-2">Contact</h2>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Input label="Contact Office" {...register('contactOfficeName')} placeholder="BSA Compliance" />
          <Input label="Phone" {...register('contactPhone')} placeholder="5551234567" />
          <div className="col-span-2">
            <Input label="Email" type="email" {...register('contactEmail')} placeholder="bsa@bank.com" />
          </div>
        </div>

        <div className="space-y-1 pt-2">
          <h2 className="text-sm font-semibold text-slate-700 border-b border-slate-100 pb-2">Activity</h2>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Input label="Case ID" {...register('caseId')} placeholder="CASE-2026-000001" />
          <Input label="Total Suspicious Amount ($)" type="number" step="0.01" {...register('totalSuspiciousAmount', { valueAsNumber: true })} />
          <Input label="Activity From" type="date" {...register('activityFromDate')} />
          <Input label="Activity To" type="date" {...register('activityToDate')} />
        </div>
        <div>
          <Textarea label="Narrative" rows={5} {...register('narrative')} placeholder="Describe the suspicious activity…" />
        </div>

        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={isPending}>{isPending ? 'Creating…' : 'Create SAR'}</Button>
          <Button type="button" variant="secondary" onClick={() => navigate('/sar')}>Cancel</Button>
        </div>
      </form>
    </div>
  );
}
