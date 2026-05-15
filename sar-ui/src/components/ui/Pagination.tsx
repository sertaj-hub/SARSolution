interface Props {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, totalElements, onChange }: Props) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
      <span>{totalElements} total</span>
      <div className="flex gap-1">
        <button className="btn-secondary py-1 px-2.5 text-xs" disabled={page === 0} onClick={() => onChange(page - 1)}>Prev</button>
        <span className="px-2 py-1">Page {page + 1} / {totalPages}</span>
        <button className="btn-secondary py-1 px-2.5 text-xs" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next</button>
      </div>
    </div>
  );
}
