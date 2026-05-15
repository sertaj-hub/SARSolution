import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sarApi } from '@/api/sarApi';
import type { SarReportDto, SarSubjectDto, SarAccountDto, SarTransactionDto, SarActivityTypeDto, SarBranchDto } from '@/api/types';

export function useSarReport(id: string | undefined) {
  return useQuery({
    queryKey: ['sar-report', id],
    queryFn: () => sarApi.get(id!),
    enabled: !!id,
  });
}

export function useUpdateSar(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<SarReportDto>) => sarApi.update(id, data),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); void qc.invalidateQueries({ queryKey: ['sar-reports'] }); },
  });
}

export function useSubmitForReview(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => sarApi.submitForReview(id),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); void qc.invalidateQueries({ queryKey: ['sar-reports'] }); },
  });
}

export function useApproveSar(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => sarApi.approve(id),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); void qc.invalidateQueries({ queryKey: ['sar-reports'] }); },
  });
}

export function useRejectSar(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reason: string) => sarApi.reject(id, reason),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); void qc.invalidateQueries({ queryKey: ['sar-reports'] }); },
  });
}

export function useAddSubject(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (s: SarSubjectDto) => sarApi.addSubject(id, s),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useUpdateSubject(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ subjectId, subject }: { subjectId: string; subject: SarSubjectDto }) =>
      sarApi.updateSubject(id, subjectId, subject),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useDeleteSubject(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (subjectId: string) => sarApi.deleteSubject(id, subjectId),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useAddAccount(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (a: SarAccountDto) => sarApi.addAccount(id, a),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useDeleteAccount(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (accountId: string) => sarApi.deleteAccount(id, accountId),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useAddTransaction(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (tx: SarTransactionDto) => sarApi.addTransaction(id, tx),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useDeleteTransaction(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (txId: string) => sarApi.deleteTransaction(id, txId),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useAddActivityType(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto: SarActivityTypeDto) => sarApi.addActivityType(id, dto),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useUpdateActivityType(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ activityTypeId, dto }: { activityTypeId: string; dto: SarActivityTypeDto }) =>
      sarApi.updateActivityType(id, activityTypeId, dto),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useDeleteActivityType(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (activityTypeId: string) => sarApi.deleteActivityType(id, activityTypeId),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useAddBranch(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto: SarBranchDto) => sarApi.addBranch(id, dto),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useUpdateBranch(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ branchId, dto }: { branchId: string; dto: SarBranchDto }) =>
      sarApi.updateBranch(id, branchId, dto),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useDeleteBranch(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (branchId: string) => sarApi.deleteBranch(id, branchId),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}

export function useAuditHistory(id: string, page = 0) {
  return useQuery({
    queryKey: ['sar-audit', id, page],
    queryFn: () => sarApi.auditHistory(id, page),
  });
}

export function useValidateXml(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => sarApi.validateXml(id),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-report', id] }); },
  });
}
