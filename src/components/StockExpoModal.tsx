import { useMemo } from 'react';
import { Carpet, STORE_NUMBER, STORE_CITY } from '../types';
import { X, CheckCircle, AlertTriangle, Layers, BarChart2, Printer } from 'lucide-react';

interface StockExpoModalProps {
  isOpen: boolean;
  carpets: Carpet[];
  totalBays?: number;
  onClose: () => void;
  onSelectSlot?: (magazyn: number, podmagazyn: string) => void;
}

export function StockExpoModal({ isOpen, carpets, totalBays = 40, onClose, onSelectSlot }: StockExpoModalProps) {
  if (!isOpen) return null;
  // Compute analytics
  const stats = useMemo(() => {
    // Standard store 038 has bays 1..40, with sub-bays A and B
    const totalSlots = totalBays * 2;
    const occupiedCount = carpets.length;
    const emptyCount = Math.max(0, totalSlots - occupiedCount);
    const occupancyPercentage = Math.round((occupiedCount / totalSlots) * 100);

    // Find duplicates by EAN or Ref
    const eanMap = new Map<string, Carpet[]>();
    carpets.forEach((c) => {
      if (c.ean) {
        const list = eanMap.get(c.ean) || [];
        list.push(c);
        eanMap.set(c.ean, list);
      }
    });

    const duplicates = Array.from(eanMap.entries()).filter(([_, list]) => list.length > 1);

    // List empty slots
    const occupiedSet = new Set(carpets.map((c) => `${c.magazyn}_${c.podmagazyn.toUpperCase()}`));
    const emptySlots: { magazyn: number; podmagazyn: string; label: string }[] = [];

    for (let m = 1; m <= totalBays; m++) {
      ['A', 'B'].forEach((sub) => {
        if (!occupiedSet.has(`${m}_${sub}`)) {
          emptySlots.push({ magazyn: m, podmagazyn: sub, label: `${m} ${sub}` });
        }
      });
    }

    return {
      totalSlots,
      occupiedCount,
      emptyCount,
      occupancyPercentage,
      duplicates,
      emptySlots
    };
  }, [carpets, totalBays]);

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
      <div className="bg-white rounded-2xl max-w-2xl w-full shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-amber-50">
          <div className="flex items-center gap-2 font-bold text-amber-900 text-sm sm:text-base">
            <BarChart2 className="w-5 h-5 text-amber-600" />
            Stan Stock Expo • Sklep {STORE_NUMBER} {STORE_CITY}
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-amber-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-5">
          {/* Top KPI Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-gray-50 border border-gray-200 rounded-xl p-3 text-center">
              <span className="text-[10px] font-bold text-gray-500 uppercase block">Regały expo</span>
              <span className="text-xl sm:text-2xl font-black text-gray-900">{totalBays}</span>
              <span className="text-[10px] text-gray-400 block mt-0.5">({stats.totalSlots} miejsc)</span>
            </div>
            <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-3 text-center">
              <span className="text-[10px] font-bold text-emerald-700 uppercase block">Zajęte miejsca</span>
              <span className="text-xl sm:text-2xl font-black text-emerald-800">{stats.occupiedCount}</span>
              <span className="text-[10px] text-emerald-600 block mt-0.5">({stats.occupancyPercentage}%)</span>
            </div>
            <div className="bg-blue-50 border border-blue-200 rounded-xl p-3 text-center">
              <span className="text-[10px] font-bold text-blue-700 uppercase block">Wolne miejsca</span>
              <span className="text-xl sm:text-2xl font-black text-blue-800">{stats.emptyCount}</span>
              <span className="text-[10px] text-blue-600 block mt-0.5">gotowe do ekspozycji</span>
            </div>
            <div className={`border rounded-xl p-3 text-center ${stats.duplicates.length > 0 ? "bg-red-50 border-red-200" : "bg-gray-50 border-gray-200"}`}>
              <span className="text-[10px] font-bold uppercase block text-gray-500">Duplikaty EAN</span>
              <span className={`text-xl sm:text-2xl font-black ${stats.duplicates.length > 0 ? "text-red-700" : "text-gray-700"}`}>
                {stats.duplicates.length}
              </span>
              <span className="text-[10px] text-gray-400 block mt-0.5">
                {stats.duplicates.length > 0 ? "wymaga korekty" : "brak błędów"}
              </span>
            </div>
          </div>

          {/* Occupancy bar */}
          <div className="bg-gray-100 rounded-xl p-3">
            <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1.5">
              <span>Zapełnienie ekspozycji</span>
              <span>{stats.occupancyPercentage}% ({stats.occupiedCount} z {stats.totalSlots})</span>
            </div>
            <div className="w-full bg-gray-200 h-3 rounded-full overflow-hidden">
              <div
                className="bg-emerald-500 h-full rounded-full transition-all duration-500"
                style={{ width: `${Math.min(100, stats.occupancyPercentage)}%` }}
              ></div>
            </div>
          </div>

          {/* Duplicates warnings if any */}
          {stats.duplicates.length > 0 && (
            <div className="bg-red-50 border border-red-200 rounded-xl p-4">
              <div className="flex items-center gap-2 font-bold text-red-800 text-xs sm:text-sm mb-2">
                <AlertTriangle className="w-4 h-4 text-red-600" />
                Wykryto zduplikowane kody EAN na ekspozycji:
              </div>
              <div className="flex flex-col gap-2 max-h-36 overflow-y-auto text-xs">
                {stats.duplicates.map(([ean, items]) => (
                  <div key={ean} className="bg-white p-2 rounded-lg border border-red-100">
                    <span className="font-mono font-bold text-red-700">EAN: {ean}</span>
                    <span className="text-gray-600 ml-2">występuje w: </span>
                    <span className="font-bold text-gray-800">
                      {items.map((i) => i.miejsce).join(', ')}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Free slots list */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="font-bold text-gray-800 text-xs sm:text-sm flex items-center gap-1.5">
                <Layers className="w-4 h-4 text-blue-600" />
                Dostępne wolne miejsca na dywany ({stats.emptySlots.length})
              </span>
              <span className="text-[11px] text-gray-500">Kliknij miejsce, aby przypisać</span>
            </div>
            <div className="grid grid-cols-4 sm:grid-cols-6 md:grid-cols-8 gap-1.5 max-h-48 overflow-y-auto p-2 bg-gray-50 rounded-xl border border-gray-200">
              {stats.emptySlots.map((slot) => (
                <button
                  key={slot.label}
                  onClick={() => {
                    if (onSelectSlot) {
                      onSelectSlot(slot.magazyn, slot.podmagazyn);
                      onClose();
                    }
                  }}
                  className="px-2 py-1.5 bg-white hover:bg-blue-600 hover:text-white border border-gray-200 hover:border-blue-600 rounded-md text-xs font-bold font-mono text-gray-800 transition-colors text-center shadow-2xs"
                >
                  {slot.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t border-gray-200 bg-gray-50 flex items-center justify-between">
          <button
            onClick={() => window.print()}
            className="flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-200 rounded-lg transition-colors"
          >
            <Printer className="w-4 h-4" />
            Drukuj raport expo
          </button>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-800 hover:bg-gray-900 text-white rounded-lg text-xs sm:text-sm font-semibold transition-colors"
          >
            Zamknij
          </button>
        </div>
      </div>
    </div>
  );
}
