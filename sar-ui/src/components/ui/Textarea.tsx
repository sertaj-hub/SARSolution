import { forwardRef, type TextareaHTMLAttributes } from 'react';

interface Props extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, Props>(({ label, error, id, ...props }, ref) => (
  <div>
    {label && <label htmlFor={id} className="label">{label}</label>}
    <textarea ref={ref} id={id} className={`input resize-none ${error ? 'border-red-400' : ''}`} {...props} />
    {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
  </div>
));
Textarea.displayName = 'Textarea';
