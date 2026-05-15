import { useMutation, useQueryClient } from '@tanstack/react-query';
import { sarApi } from '@/api/sarApi';
import type { CreateSarRequest } from '@/api/types';

export function useCreateSar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateSarRequest) => sarApi.create(data),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sar-reports'] }); },
  });
}
