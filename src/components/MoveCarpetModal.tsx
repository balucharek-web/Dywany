import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Carpet } from '../types';
import {
  ArrowRight,
  CheckCircle2,
  AlertTriangle,
  X,
  MapPin,
  Search,
  Sparkles,
  RefreshCw,
  Layers
} from 'lucide-react';

interface MoveCarpetModalProps {
  isOpen: boolean;
  onClose: () => void;
  carpet: Carpet | null;
  activeBays: number[];
  allCarpets: Carpet[];
  onMoveOrSwap: (
    sourceCarpet: Carpet,
    targetBay: number,
    targetSub: string,
    mode: 'move' | 'swap' | 'overwrite'
  ) => Promise<void>;
}

/**
 * Universal place parser: parses "140B", "140 b", "12a", "30B", "5A", "140"
 */
function parsePlace(input: string): { bay: number; sub: 'A' | 'B' } | null {
  if (!input) return null;
  const clean = input.trim().toUpperCase();

  // Pattern with letter: "140B", "140 B", "12A", "30B", "5A"
  const matchWithSub = clean.match(/^(\d+)\s*[-/_]?\s*([AB])$/);
  if (matchWithSub) {
    const bay = parseInt(matchWithSub[1], 10);
    const sub = matchWithSub[2] as 'A' | 'B';
    if (!isNaN(bay) && bay > 0) {
      return { bay, sub };
    }
  }

  // Pattern bay only: "140" -> defaults to A
  const matchBayOnly = clean.match(/^(\d+)$/);
  if (matchBayOnly) {
    const bay = parseInt(matchBayOnly[1], 10);
    if (!isNaN(bay) && bay > 0) {
      return { bay, sub: 'A' };
    }
  }

  return null;
}

export function MoveCarpetModal({
  isOpen,
  onClose,
  carpet,
  activeBays,
  allCarpets,
  onMoveOrSwap
}: MoveCarpetModalProps) {
  // Hooks unconditionally at the top
  const [targetInput, setTargetInput] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [filterFreeSearch, setFilterFreeSearch] = useState<string>('');

  const targetInputRef = useRef<HTMLInputElement | null>(null);

  // Compute all currently empty places across the warehouse
  const allEmptySlots = useMemo(() => {
    const occupiedSet = new Set(
      allCarpets.map((c) => `${c.magazyn}${String(c.podmagazyn || 'A').toUpperCase().trim()}`)
    );
    const empty: { bay: number; sub: 'A' | 'B'; label: string }[] = [];

    // Sort bays numerically
    const sortedBays = [...activeBays].sort((a, b) => a - b);
    for (const bay of sortedBays) {
      if (!occupiedSet.has(`${bay}A`)) {
        empty.push({ bay, sub: 'A', label: `${bay}A` });
      }
      if (!occupiedSet.has(`${bay}B`)) {
        empty.push({ bay, sub: 'B', label: `${bay}B` });
      }
    }
    return empty;
  }, [activeBays, allCarpets]);

  // Filtered empty slots if user searches for suggestions
  const suggestedEmptySlots = useMemo(() => {
    if (!filterFreeSearch.trim()) {
      return allEmptySlots.slice(0, 14);
    }
    const q = filterFreeSearch.trim().toLowerCase();
    return allEmptySlots.filter((slot) => slot.label.toLowerCase().includes(q)).slice(0, 14);
  }, [allEmptySlots, filterFreeSearch]);

  // When modal opens: initialize cleanly
  useEffect(() => {
    if (isOpen) {
      setTargetInput('');
      setErrorMsg(null);
      setFilterFreeSearch('');

      // Auto-focus target input
      setTimeout(() => {
        targetInputRef.current?.focus();
      }, 100);
    }
  }, [isOpen, carpet]);

  // Parse destination place
  const parsedTarget = useMemo(() => parsePlace(targetInput), [targetInput]);

  // Check if target place is occupied by any carpet
  const occupyingCarpet = useMemo(() => {
    if (!parsedTarget) return null;
    return allCarpets.find(
      (c) =>
        c.magazyn === parsedTarget.bay &&
        (c.podmagazyn || '').toUpperCase() === parsedTarget.sub
    ) || null;
  }, [parsedTarget, allCarpets]);

  // Safe early return after hooks are evaluated
  if (!isOpen || !carpet) return null;

  const sourcePlace = carpet.miejsce || `${carpet.magazyn}${carpet.podmagazyn}`;
  const targetPlaceFormatted = parsedTarget ? `${parsedTarget.bay}${parsedTarget.sub}` : '';

  const isSameLocation =
    parsedTarget &&
    parsedTarget.bay === carpet.magazyn &&
    parsedTarget.sub === (carpet.podmagazyn || 'A').toUpperCase();

  const isTargetOccupied = Boolean(occupyingCarpet);
  const isTargetValidAndFree = Boolean(parsedTarget && !isSameLocation && !isTargetOccupied);

  const handleExecuteMove = async () => {
    if (!parsedTarget) return;

    if (isSameLocation) {
      setErrorMsg('Wybierz inne miejsce docelowe – dywan już znajduje się na tym regale.');
      return;
    }

    if (isTargetOccupied) {
      setErrorMsg(`Miejsce ${targetPlaceFormatted} jest już zajęte przez inny dywan! Wybierz wolne miejsce.`);
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMsg(null);
      await onMoveOrSwap(carpet, parsedTarget.bay, parsedTarget.sub, 'move');
      onClose();
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'Wystąpił błąd podczas przenoszenia dywanu');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/75 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-gray-200 overflow-hidden flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="px-5 py-4 bg-linear-to-r from-blue-700 via-indigo-700 to-indigo-900 text-white flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-white/15 flex items-center justify-center backdrop-blur-xs shadow-inner">
              <ArrowRight className="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-black tracking-tight leading-tight">
                Przenieś dywan na wolne miejsce
              </h3>
              <p className="text-xs text-blue-100 font-medium">
                Zapis w historii zmian magazynu Sklepu 038
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-white/80 hover:text-white hover:bg-white/10 rounded-xl transition-colors cursor-pointer"
            aria-label="Zamknij"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-4 sm:p-6 overflow-y-auto flex flex-col gap-4 text-xs sm:text-sm">
          {/* Source Carpet Info */}
          <div className="bg-blue-50/70 border border-blue-200 rounded-2xl p-4 flex flex-col gap-2">
            <span className="text-[11px] font-black uppercase tracking-wider text-blue-700 flex items-center gap-1.5">
              <MapPin className="w-3.5 h-3.5" />
              Wybrany dywan do przeniesienia:
            </span>
            <div className="text-base sm:text-lg font-black text-gray-900 leading-snug">
              {carpet.nazwa}
            </div>

            <div className="mt-1 flex flex-wrap items-center gap-2 text-xs sm:text-sm">
              <span className="inline-flex items-center gap-1 font-mono font-black px-3 py-1.5 rounded-lg bg-blue-600 text-white shadow-xs">
                Obecne miejsce: {sourcePlace}
              </span>
              {carpet.ref && (
                <span className="font-mono font-bold bg-white px-2.5 py-1 rounded-lg border border-gray-300 text-gray-800 shadow-2xs">
                  REF: {carpet.ref}
                </span>
              )}
              {carpet.ean && (
                <span className="font-mono font-bold bg-white px-2.5 py-1 rounded-lg border border-gray-300 text-gray-800 shadow-2xs">
                  EAN: {carpet.ean}
                </span>
              )}
            </div>
          </div>

          {/* Destination Place Input */}
          <div className="flex flex-col gap-2.5 bg-gray-50/90 p-4 rounded-2xl border border-gray-200">
            <label className="text-xs font-black text-gray-900 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <ArrowRight className="w-4 h-4 text-blue-600" />
                Wpisz wolne miejsce docelowe:
              </span>
              {targetPlaceFormatted && (
                <span className={`px-2.5 py-0.5 rounded-lg font-mono font-bold text-xs shadow-xs ${
                  isTargetOccupied
                    ? 'bg-red-100 text-red-800 border border-red-300'
                    : 'bg-emerald-600 text-white'
                }`}>
                  {targetPlaceFormatted}
                </span>
              )}
            </label>

            {/* Direct Input Field */}
            <div className="relative">
              <input
                ref={targetInputRef}
                type="text"
                value={targetInput}
                onChange={(e) => {
                  setTargetInput(e.target.value);
                  setErrorMsg(null);
                }}
                placeholder="np. 140B, 30B, 20A, 5A..."
                className="w-full px-3.5 py-3 text-base sm:text-lg font-black text-gray-900 bg-white border-2 border-blue-300 rounded-xl focus:border-blue-600 focus:ring-2 focus:ring-blue-600 focus:outline-hidden placeholder:text-gray-400 placeholder:font-normal"
              />
            </div>
            <span className="text-[11px] text-gray-500">
              Wpisz dowolne miejsce w sklepie (np. <strong className="font-mono text-gray-800">140B</strong> lub <strong className="font-mono text-gray-800">30B</strong>). System od razu sprawdzi, czy jest wolne.
            </span>

            {/* Quick Picker of Free Slots in the store */}
            <div className="mt-2 pt-3 border-t border-gray-200">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-[11px] font-bold text-gray-700 flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                  Szybki wybór wolnego miejsca ({allEmptySlots.length} wolnych):
                </span>
              </div>

              {allEmptySlots.length > 0 ? (
                <div className="flex flex-wrap gap-1.5 max-h-28 overflow-y-auto p-1 bg-white rounded-xl border border-gray-200">
                  {suggestedEmptySlots.map((slot) => (
                    <button
                      key={slot.label}
                      type="button"
                      onClick={() => {
                        setTargetInput(slot.label);
                        setErrorMsg(null);
                      }}
                      className={`px-2.5 py-1 rounded-lg text-xs font-mono font-bold transition-all cursor-pointer border ${
                        targetPlaceFormatted === slot.label
                          ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs'
                          : 'bg-emerald-50 text-emerald-900 border-emerald-200 hover:bg-emerald-100'
                      }`}
                    >
                      {slot.label}
                    </button>
                  ))}
                </div>
              ) : (
                <div className="text-xs text-gray-500 italic">
                  Wszystkie zdefiniowane regały są obecnie zajęte.
                </div>
              )}
            </div>
          </div>

          {/* Validation Feedback & Action Button */}
          {isSameLocation ? (
            <div className="p-3 bg-amber-50 border border-amber-300 rounded-xl text-xs text-amber-900 font-medium flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
              <span>Dywan już znajduje się na miejscu {sourcePlace}. Wpisz inne wolne miejsce.</span>
            </div>
          ) : isTargetOccupied ? (
            /* TARGET OCCUPIED WARNING */
            <div className="p-3.5 bg-red-50 border-2 border-red-300 rounded-2xl flex flex-col gap-1 text-xs text-red-950">
              <div className="font-extrabold flex items-center gap-1.5 text-red-700">
                <X className="w-4 h-4 text-red-600 shrink-0" />
                <span>Miejsce {targetPlaceFormatted} jest już zajęte!</span>
              </div>
              <div className="text-[11px] text-gray-700 mt-0.5">
                Wisi tam obecnie: <strong className="text-gray-900">{occupyingCarpet?.nazwa}</strong>
              </div>
              <p className="text-[11px] text-red-800 font-medium mt-1">
                Wybierz inne, wolne miejsce z listy powyżej lub wpisz inny numer regału.
              </p>
            </div>
          ) : parsedTarget ? (
            /* TARGET FREE: READY TO MOVE */
            <div className="p-3.5 bg-emerald-50 border-2 border-emerald-400 rounded-2xl flex flex-col gap-3 text-xs text-emerald-950 shadow-xs">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                <span className="font-extrabold text-emerald-900 text-sm">
                  Miejsce {targetPlaceFormatted} jest wolne!
                </span>
              </div>
              <div className="text-[11px] text-emerald-800">
                Dywan <strong>{carpet.nazwa}</strong> zostanie przeniesiony z <strong>{sourcePlace}</strong> na <strong>{targetPlaceFormatted}</strong>, a zmiana zostanie natychmiast zapisana w historii.
              </div>

              <button
                type="button"
                disabled={isSubmitting || !isTargetValidAndFree}
                onClick={handleExecuteMove}
                className="w-full py-3.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-black text-sm sm:text-base shadow-md transition-all active:scale-98 flex items-center justify-center gap-2 cursor-pointer"
              >
                {isSubmitting ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Przenoszenie i zapis w historii...</span>
                  </>
                ) : (
                  <>
                    <ArrowRight className="w-5 h-5" />
                    <span>Przenieś dywan na {targetPlaceFormatted}</span>
                  </>
                )}
              </button>
            </div>
          ) : null}

          {errorMsg && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 font-medium">
              {errorMsg}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="px-5 py-3.5 bg-gray-50 border-t border-gray-200 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 font-bold text-xs rounded-xl transition-colors cursor-pointer"
          >
            Anuluj
          </button>
        </div>
      </div>
    </div>
  );
}
