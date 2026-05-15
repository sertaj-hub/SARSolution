import { useState } from 'react';
import { toast } from 'sonner';
import type { SarReportDto, SarTransactionDto } from '@/api/types';
import { useAddTransaction, useDeleteTransaction } from '@/hooks/useSarReport';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { useForm } from 'react-hook-form';
import { formatDate, formatAmount } from '@/lib/format';

export function TransactionsTab({ sar }: { sar: SarReportDto }) {
  const [open, setOpen] = useState(false);
  const addTx = useAddTransaction(sar.id);
  const deleteTx = useDeleteTransaction(sar.id);
  const { register, handleSubmit, reset } = useForm<SarTransactionDto>();

  const onAdd = async (data: SarTransactionDto) => {
    try {
      await addTx.mutateAsync({ ...data, amount: data.amount ? Number(data.amount) : undefined });
      toast.success('Transaction added');
      reset();
      setOpen(false);
    } catch (err: unknown) {
      toast.error((err as { detail?: string })?.detail ?? 'Failed to add transaction');
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-slate-700">Transactions ({sar.transactions.length})</h3>
        <Button size="sm" onClick={() => setOpen(true)}>+ Add Transaction</Button>
      </div>

      {sar.transactions.length === 0 ? (
        <p className="text-sm text-slate-500 py-8 text-center">No transactions added yet.</p>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50 text-xs font-medium text-slate-500 uppercase">
              <th className="px-3 py-2 text-left">Date</th>
              <th className="px-3 py-2 text-left">Type</th>
              <th className="px-3 py-2 text-left">Direction</th>
              <th className="px-3 py-2 text-right">Amount</th>
              <th className="px-3 py-2 text-left">Counterparty</th>
              <th className="px-3 py-2 text-left">Flags</th>
              <th className="px-3 py-2"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {sar.transactions.map(tx => (
              <tr key={tx.id}>
                <td className="px-3 py-2">{formatDate(tx.transactionDate)}</td>
                <td className="px-3 py-2">{tx.transactionType ?? '—'}</td>
                <td className="px-3 py-2">{tx.direction ?? '—'}</td>
                <td className="px-3 py-2 text-right">{formatAmount(tx.amount, tx.currencyCode)}</td>
                <td className="px-3 py-2">{tx.counterpartyName ?? '—'}</td>
                <td className="px-3 py-2">
                  <div className="flex gap-1 flex-wrap">
                    {tx.structuringFlag && <span className="rounded bg-red-100 px-1 text-xs text-red-700">Struct</span>}
                    {tx.rapidMovementFlag && <span className="rounded bg-orange-100 px-1 text-xs text-orange-700">Rapid</span>}
                    {tx.unusualPatternFlag && <span className="rounded bg-yellow-100 px-1 text-xs text-yellow-700">Unusual</span>}
                  </div>
                </td>
                <td className="px-3 py-2 text-right">
                  <button onClick={() => { void (async () => { if (!confirm('Remove?')) return; try { await deleteTx.mutateAsync(tx.id!); toast.success('Removed'); } catch { toast.error('Failed'); } })(); }} className="text-xs text-red-500 hover:text-red-700">Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Modal open={open} title="Add Transaction" onClose={() => setOpen(false)}
        footer={<>
          <Button variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
          <Button onClick={() => { void handleSubmit(onAdd)(); }} disabled={addTx.isPending}>{addTx.isPending ? 'Adding…' : 'Add'}</Button>
        </>}
      >
        <form className="grid grid-cols-2 gap-3">
          <Input label="Date" type="date" {...register('transactionDate')} />
          <Input label="Type" {...register('transactionType')} placeholder="Wire Transfer" />
          <Input label="Amount" type="number" step="0.01" {...register('amount', { valueAsNumber: true })} />
          <Input label="Currency" {...register('currencyCode')} placeholder="USD" />
          <Input label="Direction" {...register('direction')} placeholder="DEBIT / CREDIT" />
          <Input label="Counterparty Name" {...register('counterpartyName')} />
          <Input label="Counterparty Institution" {...register('counterpartyInstitution')} />
          <Input label="Reference #" {...register('referenceNumber')} />
        </form>
      </Modal>
    </div>
  );
}
