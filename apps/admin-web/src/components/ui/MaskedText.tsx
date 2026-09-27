import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { maskEmail, maskPhone } from '@/utils/mask';

interface MaskedTextProps {
  value?: string | null;
  type: 'email' | 'phone';
  canReveal?: boolean;
  onRevealAudit?: () => void;
  className?: string;
}

export const MaskedText: React.FC<MaskedTextProps> = ({
  value,
  type,
  canReveal = false,
  onRevealAudit,
  className = '',
}) => {
  const [isRevealed, setIsRevealed] = useState(false);

  if (!value) {
    return <span className={`text-slate-500 italic ${className}`}>N/A</span>;
  }

  const maskedValue = type === 'email' ? maskEmail(value) : maskPhone(value);
  const displayValue = isRevealed ? value : maskedValue;

  const handleToggleReveal = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!canReveal) return;

    const nextState = !isRevealed;
    setIsRevealed(nextState);

    if (nextState && onRevealAudit) {
      onRevealAudit();
    }
  };

  return (
    <span className={`inline-flex items-center gap-1.5 font-mono text-xs ${className}`}>
      <span>{displayValue}</span>
      {canReveal && (
        <button
          type="button"
          onClick={handleToggleReveal}
          title={isRevealed ? 'Hide PII' : 'Audit Reveal (SUPER_ADMIN only)'}
          className="p-0.5 text-slate-500 hover:text-indigo-400 rounded transition"
        >
          {isRevealed ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
        </button>
      )}
    </span>
  );
};
