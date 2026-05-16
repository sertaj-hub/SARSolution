import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import type { SarReportDto, SarActivityTypeDto } from '@/api/types';
import { useUpdateSar, useAddActivityType, useUpdateActivityType, useDeleteActivityType } from '@/hooks/useSarReport';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { ACTIVITY_TYPE_GROUPS } from '@/lib/sarEnums';

interface ActivityFields {
  activityFromDate?: string;
  activityToDate?: string;
  totalSuspiciousAmount?: number;
  noAmountInvolved?: boolean;
  continuingActivity?: boolean;
  fiNotedSuspiciousActivity?: boolean;
}

interface ActivityTypeRow {
  code: string;
  label: string;
  checked: boolean;
  existingId?: string;
  amount?: string;
  productType?: string;
  activityTypeOther?: string;
}

export function Part2ActivityTab({ sar }: { sar: SarReportDto }) {
  const updateSar = useUpdateSar(sar.id);
  const addActivityType = useAddActivityType(sar.id);
  const updateActivityType = useUpdateActivityType(sar.id);
  const deleteActivityType = useDeleteActivityType(sar.id);

  const { register, handleSubmit, watch, formState: { errors, isDirty } } = useForm<ActivityFields>({
    defaultValues: {
      activityFromDate: sar.activityFromDate ?? '',
      activityToDate: sar.activityToDate ?? '',
      totalSuspiciousAmount: sar.totalSuspiciousAmount ?? undefined,
      noAmountInvolved: sar.noAmountInvolved ?? false,
      continuingActivity: sar.continuingActivity ?? false,
      fiNotedSuspiciousActivity: sar.fiNotedSuspiciousActivity ?? true,
    },
  });

  const noAmountInvolved = watch('noAmountInvolved');
  const activityFromDate = watch('activityFromDate');

  const [rows, setRows] = useState<ActivityTypeRow[]>(() =>
    ACTIVITY_TYPE_GROUPS.flatMap(g => g.types).map(t => {
      const existing = sar.activityTypes.find(at => at.activityTypeCode === t.code);
      return {
        code: t.code,
        label: t.label,
        checked: !!existing,
        existingId: existing?.id,
        amount: existing?.amount?.toString() ?? '',
        productType: existing?.productType ?? '',
        activityTypeOther: existing?.activityTypeOther ?? '',
      };
    })
  );

  const [savingTypes, setSavingTypes] = useState(false);

  useEffect(() => {
    setRows(
      ACTIVITY_TYPE_GROUPS.flatMap(g => g.types).map(t => {
        const existing = sar.activityTypes.find(at => at.activityTypeCode === t.code);
        return {
          code: t.code,
          label: t.label,
          checked: !!existing,
          existingId: existing?.id,
          amount: existing?.amount?.toString() ?? '',
          productType: existing?.productType ?? '',
          activityTypeOther: existing?.activityTypeOther ?? '',
        };
      })
    );
  }, [sar.activityTypes]);

  const handleSaveFields = async (data: ActivityFields) => {
    try {
      await updateSar.mutateAsync(data as Partial<SarReportDto>);
      toast.success('Activity fields saved');
    } catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  const handleSaveActivityTypes = async () => {
    setSavingTypes(true);
    try {
      const checkedRows = rows.filter(r => r.checked);
      const uncheckedWithId = rows.filter(r => !r.checked && r.existingId);

      for (const r of uncheckedWithId) {
        await deleteActivityType.mutateAsync(r.existingId!);
      }

      for (const r of checkedRows) {
        const dto: SarActivityTypeDto = {
          activityTypeCode: r.code,
          activityTypeOther: r.activityTypeOther || undefined,
          amount: r.amount ? parseFloat(r.amount) : undefined,
          productType: r.productType || undefined,
        };
        if (r.existingId) {
          await updateActivityType.mutateAsync({ activityTypeId: r.existingId, dto });
        } else {
          await addActivityType.mutateAsync(dto);
        }
      }

      toast.success('Activity types saved');
    } catch (err: unknown) {
      toast.error((err as { detail?: string })?.detail ?? 'Failed to save activity types');
    } finally {
      setSavingTypes(false);
    }
  };

  const toggleRow = (code: string) => {
    setRows(prev => prev.map(r => r.code === code ? { ...r, checked: !r.checked } : r));
  };

  const updateRowField = (code: string, field: 'amount' | 'productType' | 'activityTypeOther', value: string) => {
    setRows(prev => prev.map(r => r.code === code ? { ...r, [field]: value } : r));
  };

  const checkedCount = rows.filter(r => r.checked).length;

  return (
    <div className="space-y-6">
      {/* Activity Date Range & Amounts */}
      <form onSubmit={e => { void handleSubmit(handleSaveFields)(e); }} className="space-y-4">
        <h3 className="text-sm font-semibold text-slate-900">Part II – Suspicious Activity Information</h3>

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Activity From Date *"
            type="date"
            {...register('activityFromDate', { required: 'Activity from date is required' })}
            error={errors.activityFromDate?.message}
          />
          <Input
            label="Activity To Date *"
            type="date"
            {...register('activityToDate', {
              required: 'Activity to date is required',
              validate: v => !activityFromDate || !v || v >= activityFromDate || 'To date must not be before from date',
            })}
            error={errors.activityToDate?.message}
          />
          <Input
            label="Total Suspicious Amount ($)"
            type="number"
            step="0.01"
            min="0"
            {...register('totalSuspiciousAmount', {
              valueAsNumber: true,
              validate: v => noAmountInvolved || (v != null && !isNaN(v) && v > 0) || 'Enter an amount or check "No amount involved"',
            })}
            error={errors.totalSuspiciousAmount?.message}
            placeholder="0.00"
          />
          <div className="flex flex-col justify-end gap-2 pb-1">
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" {...register('noAmountInvolved')} className="accent-brand-700" />
              No amount involved
            </label>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" {...register('continuingActivity')} className="accent-brand-700" />
              Continuing activity
            </label>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" {...register('fiNotedSuspiciousActivity')} className="accent-brand-700" />
              FI noted suspicious activity
            </label>
          </div>
        </div>

        <div className="flex gap-2">
          <Button type="submit" size="sm" disabled={updateSar.isPending || !isDirty}>
            {updateSar.isPending ? 'Saving…' : 'Save Fields'}
          </Button>
        </div>
      </form>

      {/* Activity Type Checkboxes */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <div>
            <h4 className="text-sm font-semibold text-slate-900">Suspicious Activity Types</h4>
            <p className="text-xs text-slate-500">{checkedCount} type(s) selected</p>
          </div>
          <Button size="sm" onClick={() => { void handleSaveActivityTypes(); }} disabled={savingTypes}>
            {savingTypes ? 'Saving…' : 'Save Activity Types'}
          </Button>
        </div>

        <div className="space-y-4">
          {ACTIVITY_TYPE_GROUPS.map(group => (
            <div key={group.label} className="rounded-lg border border-slate-200 overflow-hidden">
              <div className="bg-slate-50 px-4 py-2 border-b border-slate-200">
                <span className="text-xs font-semibold uppercase tracking-wide text-slate-600">{group.label}</span>
              </div>
              <div className="p-3 space-y-2">
                {group.types.map(type => {
                  const row = rows.find(r => r.code === type.code)!;
                  return (
                    <div key={type.code}>
                      <label className="flex items-start gap-2 text-sm cursor-pointer">
                        <input
                          type="checkbox"
                          checked={row.checked}
                          onChange={() => toggleRow(type.code)}
                          className="mt-0.5 accent-brand-700"
                        />
                        <span className={row.checked ? 'text-slate-900 font-medium' : 'text-slate-600'}>
                          {type.label}
                        </span>
                      </label>
                      {row.checked && (
                        <div className="ml-6 mt-2 grid grid-cols-3 gap-2">
                          <Input
                            label="Amount ($)"
                            type="number"
                            step="0.01"
                            min="0"
                            value={row.amount}
                            onChange={e => updateRowField(type.code, 'amount', e.target.value)}
                            placeholder="0.00"
                          />
                          <Input
                            label="Product Type"
                            value={row.productType}
                            onChange={e => updateRowField(type.code, 'productType', e.target.value)}
                            placeholder="e.g. Wire transfer, Cash…"
                          />
                          {type.code.endsWith('_OTHER') && (
                            <Input
                              label="Other description"
                              value={row.activityTypeOther}
                              onChange={e => updateRowField(type.code, 'activityTypeOther', e.target.value)}
                              placeholder="Describe…"
                            />
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
