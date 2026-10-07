import React, { useState, useEffect } from 'react';
import { CarpetSlot } from '../types';
import { X, ArrowRightLeft, Check } from 'lucide-react';

interface MoveCarpetModalProps {
  sourceSlot: CarpetSlot;
  onMove: (targetSlotId: string, targetRack: number, targetLetter: string) => void;
  onClose: () => void;
}

export const MoveCarpetModal: React.FC<MoveCarpetModalProps> = ({
  sourceSlot,
  onMove,
  onClose
}) => {
  const [targetRack, setTargetRack] = useState<string>('');
  const [targetLetter, setTargetLetter] = useState<'a' | 'b'>('a');

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const targetRackNum = parseInt(targetRack, 10);
  const isValidRack = !isNaN(targetRackNum) && targetRackNum > 0 && targetRackNum <= 500;
  const targetSlotId = isValidRack ? `${targetRackNum}${targetLetter}` : '';
  const isSameAsSource = targetSlotId === sourceSlot.slotId;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValidRack || isSameAsSource) return;
    onMove(targetSlotId, targetRackNum, targetLetter);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl space-y-4 border border-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-2">
          <h3 className="font-bold text-slate-900 text-sm flex items-center space-x-2">
            <ArrowRightLeft className="w-4 h-4 text-blue-700" />
            <span>Przenieś dywan</span>
          </h3>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Source item card */}
        <div className="text-xs space-y-1 bg-slate-50 p-3 rounded-xl border border-slate-200">
          <span className="text-slate-400 block text-[10px] font-semibold uppercase">
            Przenoszony dywan:
          </span>
          <div className="font-bold text-slate-800 leading-snug">
            {sourceSlot.productName || 'Dywan bez nazwy'}
          </div>
          <div className="text-blue-700 font-semibold pt-1">
            Z miejsca: {sourceSlot.slotId.toUpperCase()} (Stojak {sourceSlot.rackNumber}, miejsce {sourceSlot.slotLetter.toUpperCase()})
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-3.5 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              Docelowy stojak (numer magazynu):
            </label>
            <input
              type="number"
              min="1"
              max="500"
              required
              value={targetRack}
              onChange={(e) => setTargetRack(e.target.value)}
              placeholder="np. 5 lub 12"
              className="w-full px-3 py-2 rounded-lg border border-slate-200 focus:ring-2 focus:ring-blue-600 focus:outline-hidden text-sm"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              Docelowe miejsce na stojaku:
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setTargetLetter('a')}
                className={`py-2 rounded-lg font-bold text-xs transition cursor-pointer ${
                  targetLetter === 'a'
                    ? 'border-2 border-blue-600 bg-blue-50 text-blue-700'
                    : 'border border-slate-200 text-slate-600 bg-white hover:bg-slate-50'
                }`}
              >
                Miejsce A (lewe)
              </button>
              <button
                type="button"
                onClick={() => setTargetLetter('b')}
                className={`py-2 rounded-lg font-bold text-xs transition cursor-pointer ${
                  targetLetter === 'b'
                    ? 'border-2 border-blue-600 bg-blue-50 text-blue-700'
                    : 'border border-slate-200 text-slate-600 bg-white hover:bg-slate-50'
                }`}
              >
                Miejsce B (prawe)
              </button>
            </div>
          </div>

          {isValidRack && (
            <div className="p-2.5 rounded-lg bg-blue-50/70 border border-blue-100 text-[11px] text-blue-800 font-medium">
              Nowe miejsce: <span className="font-bold">{targetSlotId.toUpperCase()}</span> (Stojak {targetRackNum}, miejsce {targetLetter.toUpperCase()})
              {isSameAsSource && (
                <div className="text-rose-600 mt-1 font-semibold">
                  Miejsce docelowe jest takie samo jak źródłowe!
                </div>
              )}
            </div>
          )}

          <div className="flex justify-end space-x-2 pt-2 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-3.5 py-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 font-semibold text-xs cursor-pointer"
            >
              Anuluj
            </button>
            <button
              type="submit"
              disabled={!isValidRack || isSameAsSource}
              className="px-4 py-2 rounded-lg bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs shadow-xs flex items-center space-x-1 cursor-pointer disabled:opacity-50"
            >
              <Check className="w-3.5 h-3.5" />
              <span>Zatwierdź przeniesienie</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
