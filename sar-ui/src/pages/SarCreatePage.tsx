import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { useCreateSar } from '@/hooks/useCreateSar';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import type { CreateSarRequest } from '@/api/types';

export function SarCreatePage() {
  const navigate = useNavigate();
  const { mutateAsync, isPending } = useCreateSar();
  const { register, handleSubmit, formState: { errors } } = useForm<CreateSarRequest>();

  const onSubmit = async (data: CreateSarRequest) => {
    try {
      const created = await mutateAsync(data);
      toast.success(`SAR ${created.reportNumber} created`);
      navigate(`/sar/${created.id}/form`);
    } catch (err: unknown) {
      toast.error((err as { detail?: string })?.detail ?? 'Failed to create SAR');
    }
  };

  return (
    <div className="max-w-lg">
      <div className="mb-6">
        <button onClick={() => navigate('/sar')} className="text-xs text-slate-500 hover:text-brand-700">← Back</button>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">New SAR Report</h1>
        <p className="text-sm text-slate-500">Enter the filing institution name to start a new draft. You will complete all 5 parts of the FinCEN form on the next screen.</p>
      </div>

      <form onSubmit={e => { void handleSubmit(onSubmit)(e); }} className="card p-6 space-y-5">
        <Input
          label="Filing Institution Name *"
          {...register('filingInstitutionName', { required: 'Required' })}
          error={errors.filingInstitutionName?.message}
          placeholder="First National Bank"
        />

        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={isPending}>{isPending ? 'Creating…' : 'Create & Open Form'}</Button>
          <Button type="button" variant="secondary" onClick={() => navigate('/sar')}>Cancel</Button>
        </div>
      </form>
    </div>
  );
}
