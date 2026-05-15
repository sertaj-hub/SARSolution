import { forwardRef, type SelectHTMLAttributes } from 'react';

interface Props extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
}

export const Select = forwardRef<HTMLSelectElement, Props>(({ label, error, id, children, ...props }, ref) => (
  <div>
    {label && <label htmlFor={id} className="label">{label}</label>}
    <select ref={ref} id={id} className={`input ${error ? 'border-red-400' : ''}`} {...props}>
      {children}
    </select>
    {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
  </div>
));
Select.displayName = 'Select';
