import { useQuery } from '@tanstack/react-query';
import { sarApi } from '@/api/sarApi';
import type { SarStatus } from '@/api/types';

export function useSarReports(params: { status?: SarStatus; page?: number; size?: number; sort?: string }) {
  return useQuery({
    queryKey: ['sar-reports', params],
    queryFn: () => sarApi.list(params),
  });
}
