import client from './axiosClient';
import type {
  SarReportDto, CreateSarRequest, SarSubjectDto, SarAccountDto,
  SarTransactionDto, SarActivityTypeDto, SarBranchDto, PageResponse,
  AuditLogDto, EFilingBatch, XmlValidationResult, SarStatus, FilingStatus,
} from './types';
import axios from 'axios';
import { loadCredentials, toBasicAuth } from '@/lib/auth';

export const sarApi = {
  list: (params?: { status?: SarStatus; page?: number; size?: number; sort?: string }) =>
    client.get<PageResponse<SarReportDto>>('/sar-reports', { params }).then(r => r.data),

  get: (id: string) =>
    client.get<SarReportDto>(`/sar-reports/${id}`).then(r => r.data),

  create: (data: CreateSarRequest) =>
    client.post<SarReportDto>('/sar-reports', data).then(r => r.data),

  update: (id: string, data: Partial<SarReportDto>) =>
    client.put<SarReportDto>(`/sar-reports/${id}`, data).then(r => r.data),

  submitForReview: (id: string) =>
    client.post<SarReportDto>(`/sar-reports/${id}/submit-for-review`).then(r => r.data),

  approve: (id: string) =>
    client.post<SarReportDto>(`/sar-reports/${id}/approve`).then(r => r.data),

  reject: (id: string, reason: string) =>
    client.post<SarReportDto>(`/sar-reports/${id}/reject`, null, { params: { reason } }).then(r => r.data),

  populateFromCase: (id: string, caseId: string) =>
    client.post<SarReportDto>(`/sar-reports/${id}/populate-from-case`, null, { params: { caseId } }).then(r => r.data),

  addSubject: (id: string, subject: SarSubjectDto) =>
    client.post<SarSubjectDto>(`/sar-reports/${id}/subjects`, subject).then(r => r.data),

  updateSubject: (id: string, subjectId: string, subject: SarSubjectDto) =>
    client.put<SarSubjectDto>(`/sar-reports/${id}/subjects/${subjectId}`, subject).then(r => r.data),

  deleteSubject: (id: string, subjectId: string) =>
    client.delete(`/sar-reports/${id}/subjects/${subjectId}`),

  addAccount: (id: string, account: SarAccountDto) =>
    client.post<SarAccountDto>(`/sar-reports/${id}/accounts`, account).then(r => r.data),

  deleteAccount: (id: string, accountId: string) =>
    client.delete(`/sar-reports/${id}/accounts/${accountId}`),

  addTransaction: (id: string, tx: SarTransactionDto) =>
    client.post<SarTransactionDto>(`/sar-reports/${id}/transactions`, tx).then(r => r.data),

  deleteTransaction: (id: string, txId: string) =>
    client.delete(`/sar-reports/${id}/transactions/${txId}`),

  addActivityType: (id: string, dto: SarActivityTypeDto) =>
    client.post<SarActivityTypeDto>(`/sar-reports/${id}/activity-types`, dto).then(r => r.data),

  updateActivityType: (id: string, activityTypeId: string, dto: SarActivityTypeDto) =>
    client.put<SarActivityTypeDto>(`/sar-reports/${id}/activity-types/${activityTypeId}`, dto).then(r => r.data),

  deleteActivityType: (id: string, activityTypeId: string) =>
    client.delete(`/sar-reports/${id}/activity-types/${activityTypeId}`),

  addBranch: (id: string, dto: SarBranchDto) =>
    client.post<SarBranchDto>(`/sar-reports/${id}/branches`, dto).then(r => r.data),

  updateBranch: (id: string, branchId: string, dto: SarBranchDto) =>
    client.put<SarBranchDto>(`/sar-reports/${id}/branches/${branchId}`, dto).then(r => r.data),

  deleteBranch: (id: string, branchId: string) =>
    client.delete(`/sar-reports/${id}/branches/${branchId}`),

  auditHistory: (id: string, page = 0, size = 50) =>
    client.get<PageResponse<AuditLogDto>>(`/sar-reports/${id}/audit-history`, { params: { page, size } }).then(r => r.data),

  // eFiling
  efilingBatches: (status?: FilingStatus) =>
    client.get<EFilingBatch[]>('/efiling/batches', { params: status ? { status } : undefined }).then(r => r.data),

  efilingBatch: (batchId: string) =>
    client.get<EFilingBatch>(`/efiling/batches/${batchId}`).then(r => r.data),

  triggerBatch: () =>
    client.post<Record<string, string>>('/efiling/batches/trigger').then(r => r.data),

  resubmitBatch: (batchId: string) =>
    client.post<EFilingBatch>(`/efiling/batches/${batchId}/resubmit`).then(r => r.data),

  previewXml: (sarId: string) =>
    client.get<string>(`/efiling/reports/${sarId}/preview-xml`, { headers: { Accept: 'application/xml' } }).then(r => r.data),

  validateXml: (sarId: string) =>
    client.post<XmlValidationResult>(`/efiling/reports/${sarId}/validate-xml`).then(r => r.data),
};

export async function checkCredentials(username: string, password: string): Promise<boolean> {
  try {
    await axios.get('/actuator/health', {
      headers: { Authorization: toBasicAuth({ username, password }) },
    });
    return true;
  } catch (e: unknown) {
    if (axios.isAxiosError(e) && e.response?.status === 401) return false;
    return true;
  }
}
