import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import type { SarReportDto } from '@/api/types';
import { useUpdateSar } from '@/hooks/useSarReport';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { FILING_INSTITUTION_TYPES } from '@/lib/sarEnums';

interface FilingFields {
  filingInstitutionName?: string;
  filingInstitutionEin?: string;
  filingInstitutionType?: string;
  contactOfficeName?: string;
  contactPhone?: string;
  contactEmail?: string;
  jointReport?: boolean;
  correctsAmendsPrior?: boolean;
  priorBsaIdentifier?: string;
  caseId?: string;
  caseSystem?: string;
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h4 className="col-span-full text-xs font-semibold uppercase tracking-wide text-slate-500 border-b border-slate-100 pb-1 mt-2">
      {children}
    </h4>
  );
}

export function Part4FilingTab({ sar }: { sar: SarReportDto }) {
  const updateSar = useUpdateSar(sar.id);

  const { register, handleSubmit, watch, formState: { isDirty } } = useForm<FilingFields>({
    defaultValues: {
      filingInstitutionName: sar.filingInstitutionName ?? '',
      filingInstitutionEin: sar.filingInstitutionEin ?? '',
      filingInstitutionType: sar.filingInstitutionType ?? '',
      contactOfficeName: sar.contactOfficeName ?? '',
      contactPhone: sar.contactPhone ?? '',
      contactEmail: sar.contactEmail ?? '',
      jointReport: sar.jointReport ?? false,
      correctsAmendsPrior: sar.correctsAmendsPrior ?? false,
      priorBsaIdentifier: sar.priorBsaIdentifier ?? '',
      caseId: sar.caseId ?? '',
      caseSystem: sar.caseSystem ?? '',
    },
  });

  const correctsAmendsPrior = watch('correctsAmendsPrior');

  const handleSave = async (data: FilingFields) => {
    try {
      await updateSar.mutateAsync(data as Partial<SarReportDto>);
      toast.success('Filing institution information saved');
    } catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  return (
    <form onSubmit={e => { void handleSubmit(handleSave)(e); }} className="space-y-4">
      <h3 className="text-sm font-semibold text-slate-900">Part IV – Filing Institution Information</h3>

      <div className="grid grid-cols-2 gap-3">
        <SectionTitle>Filing Institution</SectionTitle>
        <div className="col-span-full">
          <Input
            label="Financial Institution Name *"
            {...register('filingInstitutionName')}
            placeholder="Name of the institution filing this SAR"
          />
        </div>
        <Input
          label="EIN / Tax ID"
          {...register('filingInstitutionEin')}
          placeholder="XX-XXXXXXX"
        />
        <Select label="Institution Type" {...register('filingInstitutionType')}>
          <option value="">— Select —</option>
          {FILING_INSTITUTION_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
        </Select>

        <SectionTitle>Contact Information</SectionTitle>
        <Input
          label="Contact Office / Division Name"
          {...register('contactOfficeName')}
          placeholder="BSA Compliance, AML Department…"
        />
        <Input
          label="Contact Phone"
          {...register('contactPhone')}
          placeholder="+1 555-000-0000"
        />
        <div className="col-span-full">
          <Input
            label="Contact Email"
            type="email"
            {...register('contactEmail')}
            placeholder="bsa@institution.com"
          />
        </div>

        <SectionTitle>Filing Flags</SectionTitle>
        <div className="col-span-full flex flex-wrap gap-6">
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('jointReport')} className="accent-brand-700" />
            Joint report (multiple filers)
          </label>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('correctsAmendsPrior')} className="accent-brand-700" />
            Corrects / amends prior SAR
          </label>
        </div>

        {correctsAmendsPrior && (
          <>
            <SectionTitle>Amendment Information</SectionTitle>
            <div className="col-span-full">
              <Input
                label="Prior BSA Identifier (being corrected)"
                {...register('priorBsaIdentifier')}
                placeholder="Prior FinCEN BSA filing ID"
              />
            </div>
          </>
        )}

        <SectionTitle>Case Linkage (Optional)</SectionTitle>
        <Input
          label="AML Case ID"
          {...register('caseId')}
          placeholder="Case number in case management system"
        />
        <Input
          label="Case System"
          {...register('caseSystem')}
          placeholder="e.g. AML-Workbench, Actimize…"
        />

        <SectionTitle>FinCEN Identifiers (Read Only)</SectionTitle>
        <div className="col-span-full">
          <div className="rounded-lg bg-slate-50 border border-slate-200 p-4 grid grid-cols-2 gap-3 text-sm">
            <div>
              <p className="text-xs font-medium text-slate-500 mb-0.5">Report Number</p>
              <p className="font-mono text-slate-900">{sar.reportNumber}</p>
            </div>
            {sar.bsaIdentifier && (
              <div>
                <p className="text-xs font-medium text-slate-500 mb-0.5">BSA Identifier</p>
                <p className="font-mono text-slate-900">{sar.bsaIdentifier}</p>
              </div>
            )}
            {sar.fincenTrackingNumber && (
              <div>
                <p className="text-xs font-medium text-slate-500 mb-0.5">FinCEN Tracking Number</p>
                <p className="font-mono text-slate-900">{sar.fincenTrackingNumber}</p>
              </div>
            )}
            {sar.submittedAt && (
              <div>
                <p className="text-xs font-medium text-slate-500 mb-0.5">Submitted At</p>
                <p className="text-slate-900">{new Date(sar.submittedAt).toLocaleString()}</p>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="flex gap-2 pt-2 border-t border-slate-100">
        <Button type="submit" disabled={updateSar.isPending || !isDirty}>
          {updateSar.isPending ? 'Saving…' : 'Save Filing Information'}
        </Button>
      </div>
    </form>
  );
}
