import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sarApi } from '@/api/sarApi';
import type { FilingStatus } from '@/api/types';

export function useEfilingBatches(status?: FilingStatus) {
  return useQuery({
    queryKey: ['efiling-batches', status],
    queryFn: () => sarApi.efilingBatches(status),
    refetchInterval: 30_000,
  });
}

export function useTriggerBatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => sarApi.triggerBatch(),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['efiling-batches'] }); void qc.invalidateQueries({ queryKey: ['sar-reports'] }); },
  });
}

export function useResubmitBatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (batchId: string) => sarApi.resubmitBatch(batchId),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['efiling-batches'] }); },
  });
}
