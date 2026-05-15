import { useState } from 'react';
import { toast } from 'sonner';
import type { SarReportDto, SarAccountDto } from '@/api/types';
import { useAddAccount, useDeleteAccount } from '@/hooks/useSarReport';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { useForm } from 'react-hook-form';
import { formatAmount } from '@/lib/format';

export function AccountsTab({ sar }: { sar: SarReportDto }) {
  const [open, setOpen] = useState(false);
  const addAccount = useAddAccount(sar.id);
  const deleteAccount = useDeleteAccount(sar.id);
  const { register, handleSubmit, reset } = useForm<SarAccountDto>();

  const onAdd = async (data: SarAccountDto) => {
    try {
      await addAccount.mutateAsync({ ...data, balance: data.balance ? Number(data.balance) : undefined });
      toast.success('Account added');
      reset();
      setOpen(false);
    } catch (err: unknown) {
      toast.error((err as { detail?: string })?.detail ?? 'Failed to add account');
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-slate-700">Accounts ({sar.accounts.length})</h3>
        <Button size="sm" onClick={() => setOpen(true)}>+ Add Account</Button>
      </div>

      {sar.accounts.length === 0 ? (
        <p className="text-sm text-slate-500 py-8 text-center">No accounts added yet.</p>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50 text-xs font-medium text-slate-500 uppercase">
              <th className="px-3 py-2 text-left">Account #</th>
              <th className="px-3 py-2 text-left">Institution</th>
              <th className="px-3 py-2 text-left">Type</th>
              <th className="px-3 py-2 text-right">Balance</th>
              <th className="px-3 py-2"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {sar.accounts.map(a => (
              <tr key={a.id}>
                <td className="px-3 py-2 font-mono text-xs">{a.accountNumber ?? '—'}</td>
                <td className="px-3 py-2">{a.institutionName ?? '—'}</td>
                <td className="px-3 py-2">{a.accountType ?? '—'}</td>
                <td className="px-3 py-2 text-right">{formatAmount(a.balance, a.currencyCode)}</td>
                <td className="px-3 py-2 text-right">
                  <button onClick={() => { void (async () => { if (!confirm('Remove account?')) return; try { await deleteAccount.mutateAsync(a.id!); toast.success('Removed'); } catch { toast.error('Failed'); } })()} } className="text-xs text-red-500 hover:text-red-700">Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Modal open={open} title="Add Account" onClose={() => setOpen(false)}
        footer={<>
          <Button variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
          <Button onClick={() => { void handleSubmit(onAdd)(); }} disabled={addAccount.isPending}>{addAccount.isPending ? 'Adding…' : 'Add'}</Button>
        </>}
      >
        <form className="grid grid-cols-2 gap-3">
          <Input label="Account Number" {...register('accountNumber')} />
          <Input label="Account Type" {...register('accountType')} placeholder="Checking" />
          <Input label="Institution Name" {...register('institutionName')} />
          <Input label="Routing Number" {...register('routingNumber')} />
          <Input label="Balance" type="number" step="0.01" {...register('balance', { valueAsNumber: true })} />
          <Input label="Currency" {...register('currencyCode')} placeholder="USD" />
        </form>
      </Modal>
    </div>
  );
}
