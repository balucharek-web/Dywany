import React from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

interface ToastProps {
  message: string;
  type?: 'success' | 'error' | 'info';
  onClose: () => void;
}

export const Toast: React.FC<ToastProps> = ({
  message,
  type = 'success',
  onClose
}) => {
  return (
    <div className="fixed bottom-5 right-5 z-50 flex items-center space-x-2.5 px-4 py-3 rounded-xl bg-slate-900 text-white text-xs font-semibold shadow-2xl transition transform animate-slide-up">
      {type === 'success' && <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />}
      {type === 'error' && <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />}
      {type === 'info' && <Info className="w-4 h-4 text-blue-400 shrink-0" />}
      <span>{message}</span>
      <button
        onClick={onClose}
        className="ml-2 text-slate-400 hover:text-white p-0.5 cursor-pointer"
      >
        <X className="w-3.5 h-3.5" />
      </button>
    </div>
  );
};
