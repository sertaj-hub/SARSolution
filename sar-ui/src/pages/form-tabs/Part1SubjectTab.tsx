import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import type { SarReportDto, SarSubjectDto } from '@/api/types';
import { useAddSubject, useUpdateSubject, useDeleteSubject } from '@/hooks/useSarReport';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { IDENTIFICATION_TYPES, SUBJECT_ROLES, US_STATES } from '@/lib/sarEnums';

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <h4 className="col-span-full text-xs font-semibold uppercase tracking-wide text-slate-500 border-b border-slate-100 pb-1 mt-2">{children}</h4>;
}

interface SubjectFormProps {
  defaultValues?: SarSubjectDto;
  onSave: (data: SarSubjectDto) => Promise<void>;
  onCancel: () => void;
  saving: boolean;
}

function SubjectForm({ defaultValues, onSave, onCancel, saving }: SubjectFormProps) {
  const { register, watch, handleSubmit, formState: { errors } } = useForm<SarSubjectDto>({ defaultValues });
  const isEntity = watch('isEntity');
  const roleCode = watch('roleCode');

  return (
    <form onSubmit={e => { void handleSubmit(onSave)(e); }} className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <SectionTitle>Subject Type</SectionTitle>
        <div className="col-span-full flex gap-6">
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="radio" value="false" {...register('isEntity')} className="accent-brand-700" /> Individual
          </label>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="radio" value="true" {...register('isEntity')} className="accent-brand-700" /> Entity / Business
          </label>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('isUnknown')} className="accent-brand-700" /> Unknown subject
          </label>
        </div>

        {String(isEntity) !== 'true' ? (
          <>
            <SectionTitle>Individual Name</SectionTitle>
            <Input label="Last Name" {...register('lastName')} error={errors.lastName?.message} />
            <Input label="First Name" {...register('firstName')} />
            <Input label="Middle Name" {...register('middleName')} />
            <Input label="Suffix" {...register('suffix')} placeholder="Jr., Sr., III…" />
            <Input label="Date of Birth" type="date" {...register('dateOfBirth')} />
          </>
        ) : (
          <>
            <SectionTitle>Entity / Business Name</SectionTitle>
            <div className="col-span-full">
              <Input label="Entity / Business Name" {...register('entityName')} />
            </div>
            <Input label="Doing Business As (DBA)" {...register('doingBusinessAs')} />
          </>
        )}

        <SectionTitle>Identification</SectionTitle>
        <Select label="ID Type" {...register('idType')}>
          <option value="">— Select —</option>
          {IDENTIFICATION_TYPES.map(t => <option key={t.code} value={t.code}>{t.label}</option>)}
        </Select>
        <Input label="ID Number" {...register('idNumber')} />
        <Select label="ID Issue State" {...register('idIssueState')}>
          <option value="">— Select —</option>
          {US_STATES.map(s => <option key={s.code} value={s.code}>{s.name}</option>)}
        </Select>
        <Input label="ID Issue Country (ISO)" {...register('idIssueCountry')} placeholder="US" maxLength={2} />

        <SectionTitle>Address</SectionTitle>
        <div className="col-span-full">
          <Input label="Street Address" {...register('address')} placeholder="123 Main St" />
        </div>
        <Input label="City" {...register('city')} />
        <Select label="State" {...register('state')}>
          <option value="">— Select —</option>
          {US_STATES.map(s => <option key={s.code} value={s.code}>{s.name}</option>)}
        </Select>
        <Input label="ZIP Code" {...register('zipCode')} />
        <Input label="Country (ISO)" {...register('country')} placeholder="US" maxLength={2} defaultValue="US" />

        <SectionTitle>Contact</SectionTitle>
        <Input label="Phone Number" {...register('phoneNumber')} placeholder="+1 555-000-0000" />
        <Input label="Phone Extension" {...register('phoneExtension')} placeholder="123" />
        <div className="col-span-full">
          <Input label="Email" type="email" {...register('email')} />
        </div>

        <SectionTitle>Employment / Role</SectionTitle>
        <Input label="Occupation / Business Type" {...register('occupation')} />
        <Input label="NAICS Code" {...register('naicsCode')} placeholder="523110" />
        <Select label="Role in Activity" {...register('roleCode')}>
          <option value="">— Select —</option>
          {SUBJECT_ROLES.map(r => <option key={r.code} value={r.code}>{r.label}</option>)}
        </Select>
        {roleCode === 'OTHER' && (
          <Input label="Role Description (Other)" {...register('roleOtherDescription')} />
        )}
        <div className="col-span-full flex gap-6">
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('stillEmployed')} className="accent-brand-700" /> Still employed at institution
          </label>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('hasRelationshipToAccount')} className="accent-brand-700" /> Has relationship to reported account
          </label>
        </div>

        <SectionTitle>Corrective Action Taken</SectionTitle>
        <div className="col-span-full">
          <Textarea label="Corrective action taken by institution" rows={3} {...register('correctiveAction')} placeholder="Describe corrective action taken, if any…" />
        </div>
      </div>

      <div className="flex gap-2 pt-2 border-t border-slate-100">
        <Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save Subject'}</Button>
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
      </div>
    </form>
  );
}

export function Part1SubjectTab({ sar }: { sar: SarReportDto }) {
  const [editing, setEditing] = useState<SarSubjectDto | null>(null);
  const [adding, setAdding] = useState(false);
  const addSubject = useAddSubject(sar.id);
  const updateSubject = useUpdateSubject(sar.id);
  const deleteSubject = useDeleteSubject(sar.id);

  const handleAdd = async (data: SarSubjectDto) => {
    try {
      await addSubject.mutateAsync(data);
      toast.success('Subject added');
      setAdding(false);
    } catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  const handleUpdate = async (data: SarSubjectDto) => {
    if (!editing?.id) return;
    try {
      await updateSubject.mutateAsync({ subjectId: editing.id, subject: data });
      toast.success('Subject updated');
      setEditing(null);
    } catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Remove this subject?')) return;
    try { await deleteSubject.mutateAsync(id); toast.success('Subject removed'); }
    catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  if (adding) return (
    <div>
      <h3 className="text-sm font-semibold text-slate-700 mb-4">Add Subject</h3>
      <SubjectForm onSave={handleAdd} onCancel={() => setAdding(false)} saving={addSubject.isPending} />
    </div>
  );

  if (editing) return (
    <div>
      <h3 className="text-sm font-semibold text-slate-700 mb-4">Edit Subject</h3>
      <SubjectForm defaultValues={editing} onSave={handleUpdate} onCancel={() => setEditing(null)} saving={updateSubject.isPending} />
    </div>
  );

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">Part I – Subject Information</h3>
          <p className="text-xs text-slate-500">Individuals or entities engaged in suspicious activity (up to 999)</p>
        </div>
        <Button size="sm" onClick={() => setAdding(true)}>+ Add Subject</Button>
      </div>

      {sar.subjects.length === 0 ? (
        <div className="rounded-lg border-2 border-dashed border-slate-200 p-10 text-center">
          <p className="text-sm font-medium text-slate-500">No subjects added yet</p>
          <p className="text-xs text-slate-400 mt-1">Click "+ Add Subject" to add the person or entity under investigation</p>
        </div>
      ) : (
        <div className="space-y-3">
          {sar.subjects.map((s, i) => (
            <div key={s.id} className="rounded-lg border border-slate-200 bg-white p-4">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-medium text-brand-700">Subject {i + 1}</span>
                    {s.isEntity && <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">Entity</span>}
                    {s.isUnknown && <span className="rounded-full bg-yellow-100 px-2 py-0.5 text-xs text-yellow-700">Unknown</span>}
                  </div>
                  <p className="font-medium text-slate-900">
                    {s.isEntity ? (s.entityName ?? '(No entity name)') : [s.firstName, s.middleName, s.lastName].filter(Boolean).join(' ') || '(No name)'}
                  </p>
                  {s.isEntity && s.doingBusinessAs && <p className="text-xs text-slate-500">DBA: {s.doingBusinessAs}</p>}
                  <div className="mt-2 grid grid-cols-2 gap-x-6 gap-y-0.5 text-xs text-slate-500">
                    {s.idType && <span>ID: {s.idType} {s.idNumber}</span>}
                    {s.dateOfBirth && <span>DOB: {s.dateOfBirth}</span>}
                    {s.address && <span>{[s.address, s.city, s.state, s.zipCode, s.country].filter(Boolean).join(', ')}</span>}
                    {s.phoneNumber && <span>Phone: {s.phoneNumber}{s.phoneExtension ? ` x${s.phoneExtension}` : ''}</span>}
                    {s.email && <span>Email: {s.email}</span>}
                    {s.occupation && <span>Occupation: {s.occupation}</span>}
                    {s.roleCode && <span>Role: {s.roleCode}{s.roleOtherDescription ? ` — ${s.roleOtherDescription}` : ''}</span>}
                    {s.naicsCode && <span>NAICS: {s.naicsCode}</span>}
                    {s.stillEmployed && <span className="text-orange-600">Still employed</span>}
                    {s.hasRelationshipToAccount && <span className="text-blue-600">Has account relationship</span>}
                  </div>
                  {s.correctiveAction && (
                    <p className="mt-2 text-xs text-slate-500 italic">Corrective action: {s.correctiveAction}</p>
                  )}
                </div>
                <div className="flex gap-2 ml-4">
                  <button onClick={() => setEditing(s)} className="text-xs text-brand-700 hover:underline">Edit</button>
                  <button onClick={() => { void handleDelete(s.id!); }} className="text-xs text-red-500 hover:underline">Remove</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
