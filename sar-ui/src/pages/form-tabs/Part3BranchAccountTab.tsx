import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import type { SarReportDto, SarBranchDto, SarAccountDto } from '@/api/types';
import { useAddBranch, useUpdateBranch, useDeleteBranch, useAddAccount, useDeleteAccount } from '@/hooks/useSarReport';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { ACCOUNT_TYPES, US_STATES } from '@/lib/sarEnums';

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h4 className="col-span-full text-xs font-semibold uppercase tracking-wide text-slate-500 border-b border-slate-100 pb-1 mt-2">
      {children}
    </h4>
  );
}

interface BranchFormProps {
  defaultValues?: SarBranchDto;
  onSave: (data: SarBranchDto) => Promise<void>;
  onCancel: () => void;
  saving: boolean;
}

function BranchForm({ defaultValues, onSave, onCancel, saving }: BranchFormProps) {
  const { register, handleSubmit } = useForm<SarBranchDto>({ defaultValues });
  return (
    <form onSubmit={e => { void handleSubmit(onSave)(e); }} className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <SectionTitle>Branch Information</SectionTitle>
        <div className="col-span-full">
          <Input label="Branch Name" {...register('branchName')} placeholder="Branch / Office name" />
        </div>
        <Input label="RSSD Number" {...register('rssdNumber')} placeholder="Federal Reserve RSSD ID" />
        <div className="flex items-end pb-1">
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('isPrimary')} className="accent-brand-700" /> Primary branch
          </label>
        </div>

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
      </div>
      <div className="flex gap-2 pt-2 border-t border-slate-100">
        <Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save Branch'}</Button>
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
      </div>
    </form>
  );
}

interface AccountFormProps {
  subjects: SarReportDto['subjects'];
  onSave: (data: SarAccountDto) => Promise<void>;
  onCancel: () => void;
  saving: boolean;
}

function AccountForm({ subjects, onSave, onCancel, saving }: AccountFormProps) {
  const { register, handleSubmit } = useForm<SarAccountDto>();
  return (
    <form onSubmit={e => { void handleSubmit(onSave)(e); }} className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <SectionTitle>Account Details</SectionTitle>
        <Input label="Account Number" {...register('accountNumber')} />
        <Select label="Account Type" {...register('accountType')}>
          <option value="">— Select —</option>
          {ACCOUNT_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
        </Select>
        <Input label="Product Type" {...register('productType')} placeholder="e.g. Checking, HELOC…" />
        <div className="flex items-end pb-1">
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('accountNumberClosed')} className="accent-brand-700" /> Account closed
          </label>
        </div>

        <SectionTitle>Institution</SectionTitle>
        <Input label="Institution Name" {...register('institutionName')} />
        <Input label="Institution EIN" {...register('institutionEin')} placeholder="XX-XXXXXXX" />
        <Input label="Routing Number" {...register('routingNumber')} placeholder="9-digit ABA" />

        <SectionTitle>Dates & Balance</SectionTitle>
        <Input label="Date Opened" type="date" {...register('openedDate')} />
        <Input label="Date Closed" type="date" {...register('closedDate')} />
        <Input label="Balance ($)" type="number" step="0.01" {...register('balance', { valueAsNumber: true })} placeholder="0.00" />
        <Input label="Currency Code" {...register('currencyCode')} placeholder="USD" maxLength={3} defaultValue="USD" />

        <SectionTitle>Foreign Account</SectionTitle>
        <div className="col-span-full flex gap-6">
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('isForeignAccount')} className="accent-brand-700" /> Foreign account
          </label>
        </div>
        <Input label="Foreign Country (ISO)" {...register('foreignCountry')} placeholder="MX" maxLength={2} />

        <SectionTitle>Actions Taken on Account</SectionTitle>
        <div className="col-span-full flex gap-6">
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('actionAccountClosed')} className="accent-brand-700" /> Account closed by FI
          </label>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('actionAccountFrozen')} className="accent-brand-700" /> Account frozen by FI
          </label>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" {...register('noActionTaken')} className="accent-brand-700" /> No action taken
          </label>
        </div>

        {subjects.length > 0 && (
          <>
            <SectionTitle>Link to Subject</SectionTitle>
            <Select label="Subject" {...register('subjectId')}>
              <option value="">— None —</option>
              {subjects.map((s, i) => (
                <option key={s.id} value={s.id}>
                  Subject {i + 1}: {s.isEntity ? s.entityName : [s.firstName, s.lastName].filter(Boolean).join(' ') || '(Unknown)'}
                </option>
              ))}
            </Select>
          </>
        )}
      </div>
      <div className="flex gap-2 pt-2 border-t border-slate-100">
        <Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save Account'}</Button>
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
      </div>
    </form>
  );
}

export function Part3BranchAccountTab({ sar }: { sar: SarReportDto }) {
  const [addingBranch, setAddingBranch] = useState(false);
  const [editingBranch, setEditingBranch] = useState<SarBranchDto | null>(null);
  const [addingAccount, setAddingAccount] = useState(false);

  const addBranch = useAddBranch(sar.id);
  const updateBranch = useUpdateBranch(sar.id);
  const deleteBranch = useDeleteBranch(sar.id);
  const addAccount = useAddAccount(sar.id);
  const deleteAccount = useDeleteAccount(sar.id);

  const handleAddBranch = async (data: SarBranchDto) => {
    try {
      await addBranch.mutateAsync(data);
      toast.success('Branch added');
      setAddingBranch(false);
    } catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  const handleUpdateBranch = async (data: SarBranchDto) => {
    if (!editingBranch?.id) return;
    try {
      await updateBranch.mutateAsync({ branchId: editingBranch.id, dto: data });
      toast.success('Branch updated');
      setEditingBranch(null);
    } catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  const handleDeleteBranch = async (id: string) => {
    if (!confirm('Remove this branch?')) return;
    try { await deleteBranch.mutateAsync(id); toast.success('Branch removed'); }
    catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  const handleAddAccount = async (data: SarAccountDto) => {
    try {
      await addAccount.mutateAsync(data);
      toast.success('Account added');
      setAddingAccount(false);
    } catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  const handleDeleteAccount = async (id: string) => {
    if (!confirm('Remove this account?')) return;
    try { await deleteAccount.mutateAsync(id); toast.success('Account removed'); }
    catch (err: unknown) { toast.error((err as { detail?: string })?.detail ?? 'Failed'); }
  };

  if (addingBranch) return (
    <div>
      <h3 className="text-sm font-semibold text-slate-700 mb-4">Add Branch</h3>
      <BranchForm onSave={handleAddBranch} onCancel={() => setAddingBranch(false)} saving={addBranch.isPending} />
    </div>
  );

  if (editingBranch) return (
    <div>
      <h3 className="text-sm font-semibold text-slate-700 mb-4">Edit Branch</h3>
      <BranchForm defaultValues={editingBranch} onSave={handleUpdateBranch} onCancel={() => setEditingBranch(null)} saving={updateBranch.isPending} />
    </div>
  );

  if (addingAccount) return (
    <div>
      <h3 className="text-sm font-semibold text-slate-700 mb-4">Add Account</h3>
      <AccountForm subjects={sar.subjects} onSave={handleAddAccount} onCancel={() => setAddingAccount(false)} saving={addAccount.isPending} />
    </div>
  );

  return (
    <div className="space-y-8">
      {/* Branches Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-sm font-semibold text-slate-900">Part III – Branch Offices Where Activity Occurred</h3>
            <p className="text-xs text-slate-500">Branches of the filing institution where suspicious activity was identified</p>
          </div>
          <Button size="sm" onClick={() => setAddingBranch(true)}>+ Add Branch</Button>
        </div>

        {sar.branches.length === 0 ? (
          <div className="rounded-lg border-2 border-dashed border-slate-200 p-8 text-center">
            <p className="text-sm font-medium text-slate-500">No branches added</p>
            <p className="text-xs text-slate-400 mt-1">Add the branch where the suspicious activity occurred</p>
          </div>
        ) : (
          <div className="space-y-3">
            {sar.branches.map((b, i) => (
              <div key={b.id} className="rounded-lg border border-slate-200 bg-white p-4">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">Branch {i + 1}</span>
                      {b.isPrimary && <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-700">Primary</span>}
                    </div>
                    <p className="font-medium text-slate-900">{b.branchName ?? '(No name)'}</p>
                    {b.rssdNumber && <p className="text-xs text-slate-500">RSSD: {b.rssdNumber}</p>}
                    {b.address && (
                      <p className="text-xs text-slate-500 mt-1">
                        {[b.address, b.city, b.state, b.zipCode, b.country].filter(Boolean).join(', ')}
                      </p>
                    )}
                  </div>
                  <div className="flex gap-2 ml-4">
                    <button onClick={() => setEditingBranch(b)} className="text-xs text-brand-700 hover:underline">Edit</button>
                    <button onClick={() => { void handleDeleteBranch(b.id!); }} className="text-xs text-red-500 hover:underline">Remove</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Accounts Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-sm font-semibold text-slate-900">Accounts Involved in Activity</h3>
            <p className="text-xs text-slate-500">Financial accounts involved in the suspicious activity</p>
          </div>
          <Button size="sm" onClick={() => setAddingAccount(true)}>+ Add Account</Button>
        </div>

        {sar.accounts.length === 0 ? (
          <div className="rounded-lg border-2 border-dashed border-slate-200 p-8 text-center">
            <p className="text-sm font-medium text-slate-500">No accounts added</p>
            <p className="text-xs text-slate-400 mt-1">Add the financial accounts involved in suspicious activity</p>
          </div>
        ) : (
          <div className="space-y-3">
            {sar.accounts.map((a, i) => (
              <div key={a.id} className="rounded-lg border border-slate-200 bg-white p-4">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="rounded-full bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-700">Account {i + 1}</span>
                      {a.accountType && <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{a.accountType}</span>}
                      {a.isForeignAccount && <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs text-orange-700">Foreign</span>}
                      {a.accountNumberClosed && <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs text-red-700">Closed</span>}
                    </div>
                    <p className="font-medium text-slate-900">
                      {a.accountNumber ? `•••• ${a.accountNumber.slice(-4)}` : '(No account number)'}
                    </p>
                    <div className="mt-1 grid grid-cols-2 gap-x-6 gap-y-0.5 text-xs text-slate-500">
                      {a.institutionName && <span>{a.institutionName}</span>}
                      {a.routingNumber && <span>Routing: {a.routingNumber}</span>}
                      {a.balance != null && <span>Balance: ${a.balance.toLocaleString()}</span>}
                      {a.openedDate && <span>Opened: {a.openedDate}</span>}
                    </div>
                  </div>
                  <div className="flex gap-2 ml-4">
                    <button onClick={() => { void handleDeleteAccount(a.id!); }} className="text-xs text-red-500 hover:underline">Remove</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
