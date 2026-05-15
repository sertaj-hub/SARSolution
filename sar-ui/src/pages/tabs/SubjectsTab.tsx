import { useState } from 'react';
import { toast } from 'sonner';
import type { SarReportDto, SarSubjectDto } from '@/api/types';
import { useAddSubject, useDeleteSubject } from '@/hooks/useSarReport';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { useForm } from 'react-hook-form';
import { formatDate } from '@/lib/format';

export function SubjectsTab({ sar }: { sar: SarReportDto }) {
  const [open, setOpen] = useState(false);
  const addSubject = useAddSubject(sar.id);
  const deleteSubject = useDeleteSubject(sar.id);
  const { register, handleSubmit, reset } = useForm<SarSubjectDto>();

  const onAdd = async (data: SarSubjectDto) => {
    try {
      await addSubject.mutateAsync(data);
      toast.success('Subject added');
      reset();
      setOpen(false);
    } catch (err: unknown) {
      toast.error((err as { detail?: string })?.detail ?? 'Failed to add subject');
    }
  };

  const onDelete = async (subjectId: string) => {
    if (!confirm('Remove this subject?')) return;
    try {
      await deleteSubject.mutateAsync(subjectId);
      toast.success('Subject removed');
    } catch (err: unknown) {
      toast.error((err as { detail?: string })?.detail ?? 'Failed to remove subject');
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-slate-700">Subjects ({sar.subjects.length})</h3>
        <Button size="sm" onClick={() => setOpen(true)}>+ Add Subject</Button>
      </div>

      {sar.subjects.length === 0 ? (
        <p className="text-sm text-slate-500 py-8 text-center">No subjects added yet.</p>
      ) : (
        <div className="space-y-3">
          {sar.subjects.map(s => (
            <div key={s.id} className="rounded-lg border border-slate-200 p-4 text-sm">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-slate-900">
                    {s.isEntity ? s.entityName : [s.firstName, s.middleName, s.lastName].filter(Boolean).join(' ') || '(Unknown)'}
                  </p>
                  {s.isEntity && s.doingBusinessAs && <p className="text-slate-500 text-xs">DBA: {s.doingBusinessAs}</p>}
                  <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
                    {s.idType && <span>ID: {s.idType} {s.idNumber}</span>}
                    {s.dateOfBirth && <span>DOB: {formatDate(s.dateOfBirth)}</span>}
                    {s.address && <span>{[s.address, s.city, s.state, s.zipCode].filter(Boolean).join(', ')}</span>}
                    {s.occupation && <span>Occupation: {s.occupation}</span>}
                  </div>
                </div>
                <button onClick={() => { void onDelete(s.id!); }} className="text-xs text-red-500 hover:text-red-700 ml-4">Remove</button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={open}
        title="Add Subject"
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
            <Button onClick={() => { void handleSubmit(onAdd)(); }} disabled={addSubject.isPending}>
              {addSubject.isPending ? 'Adding…' : 'Add Subject'}
            </Button>
          </>
        }
      >
        <form className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <Input label="First Name" {...register('firstName')} />
            <Input label="Last Name" {...register('lastName')} />
            <Input label="Entity Name" {...register('entityName')} />
            <Input label="Date of Birth" type="date" {...register('dateOfBirth')} />
            <Input label="ID Type" {...register('idType')} placeholder="PASSPORT" />
            <Input label="ID Number" {...register('idNumber')} />
            <Input label="Address" {...register('address')} />
            <Input label="City" {...register('city')} />
            <Input label="State" {...register('state')} />
            <Input label="ZIP" {...register('zipCode')} />
          </div>
          <Input label="Occupation" {...register('occupation')} />
          <Input label="Email" type="email" {...register('email')} />
          <Input label="Phone" {...register('phoneNumber')} />
        </form>
      </Modal>
    </div>
  );
}
