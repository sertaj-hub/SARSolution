import { forwardRef, type InputHTMLAttributes } from 'react';

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, Props>(({ label, error, id, ...props }, ref) => (
  <div>
    {label && <label htmlFor={id} className="label">{label}</label>}
    <input ref={ref} id={id} className={`input ${error ? 'border-red-400' : ''}`} {...props} />
    {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
  </div>
));
Input.displayName = 'Input';
