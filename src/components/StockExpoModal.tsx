import { useMemo } from 'react';
import { Carpet, STORE_NUMBER, STORE_CITY } from '../types';
import {
  X,
  AlertTriangle,
  Layers,
  BarChart2,
  Printer
} from 'lucide-react';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';
import { PrintableBarcode } from './PrintableBarcode';

interface StockExpoModalProps {
  isOpen: boolean;
  carpets: Carpet[];
  totalBays?: number;
  activeBays?: number[];
  onClose: () => void;
  onSelectSlot?: (magazyn: number, podmagazyn: string) => void;
}

export function StockExpoModal({
  isOpen,
  carpets,
  totalBays = 120,
  activeBays,
  onClose,
  onSelectSlot
}: StockExpoModalProps) {
  // Quick lookup map: "bay_slot" -> Carpet
  const carpetsBySlot = useMemo(() => {
    const map = new Map<string, Carpet>();
    carpets.forEach((c) => {
      const key = `${c.magazyn}_${String(c.podmagazyn || '').toUpperCase()}`;
      map.set(key, c);
    });
    return map;
  }, [carpets]);

  // Bays list (sorted 1..120 or active bays)
  const baysList = useMemo(() => {
    if (activeBays && activeBays.length > 0) {
      return [...activeBays].sort((a, b) => a - b);
    }
    return Array.from({ length: totalBays }, (_, i) => i + 1);
  }, [activeBays, totalBays]);

  // Compute analytics
  const stats = useMemo(() => {
    const totalSlots = baysList.length * 2;
    const occupiedCount = carpets.length;
    const emptyCount = Math.max(0, totalSlots - occupiedCount);
    const occupancyPercentage =
      totalSlots > 0 ? Math.round((occupiedCount / totalSlots) * 100) : 0;

    // Find duplicates by EAN
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
    const emptySlots: { magazyn: number; podmagazyn: string; label: string }[] = [];
    baysList.forEach((m) => {
      ['A', 'B'].forEach((sub) => {
        if (!carpetsBySlot.has(`${m}_${sub}`)) {
          emptySlots.push({ magazyn: m, podmagazyn: sub, label: `${m} ${sub}` });
        }
      });
    });

    return {
      totalSlots,
      occupiedCount,
      emptyCount,
      occupancyPercentage,
      duplicates,
      emptySlots
    };
  }, [carpets, baysList, carpetsBySlot]);

  if (!isOpen) return null;

  return (
    <div className="stock-expo-modal-backdrop fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
      <div className="stock-expo-modal-container bg-white rounded-2xl max-w-2xl w-full shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* =========================================================================
            1. ON-SCREEN MODAL UI (HIDDEN ON PRINT via stock-expo-screen-only)
           ========================================================================= */}
        <div className="stock-expo-screen-only flex flex-col h-full overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-amber-50 shrink-0">
            <div className="flex items-center gap-2 font-bold text-amber-900 text-sm sm:text-base">
              <BarChart2 className="w-5 h-5 text-amber-600 shrink-0" />
              <span>Stan Stock Expo • Sklep {STORE_NUMBER} {STORE_CITY}</span>
            </div>
            <button
              onClick={onClose}
              className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-amber-100 transition-colors cursor-pointer"
              title="Zamknij"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Content */}
          <div className="p-4 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-4 sm:gap-5">
            {/* Top KPI Cards */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 sm:gap-3">
              <div className="bg-gray-50 border border-gray-200 rounded-xl p-3 text-center">
                <span className="text-[10px] font-bold text-gray-500 uppercase block">Regały expo</span>
                <span className="text-xl sm:text-2xl font-black text-gray-900">{baysList.length}</span>
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
              <div className={`border rounded-xl p-3 text-center ${stats.duplicates.length > 0 ? 'bg-red-50 border-red-200' : 'bg-gray-50 border-gray-200'}`}>
                <span className="text-[10px] font-bold uppercase block text-gray-500">Duplikaty EAN</span>
                <span className={`text-xl sm:text-2xl font-black ${stats.duplicates.length > 0 ? 'text-red-700' : 'text-gray-700'}`}>
                  {stats.duplicates.length}
                </span>
                <span className="text-[10px] text-gray-400 block mt-0.5">
                  {stats.duplicates.length > 0 ? 'wymaga korekty' : 'brak błędów'}
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
                    className="px-2 py-1.5 bg-white hover:bg-blue-600 hover:text-white border border-gray-200 hover:border-blue-600 rounded-md text-xs font-bold font-mono text-gray-800 transition-colors text-center shadow-2xs cursor-pointer"
                  >
                    {slot.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Footer with immediate Print button */}
          <div className="px-5 py-3 border-t border-gray-200 bg-gray-50 flex items-center justify-between shrink-0">
            <button
              onClick={() => window.print()}
              className="flex items-center gap-2 px-4 py-2.5 text-xs sm:text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl transition-all shadow-xs active:scale-95 cursor-pointer"
              title="Drukuj pełny arkusz inwentaryzacyjny magazynów (regały, wiersze A i B, kody kreskowe, notatki)"
            >
              <Printer className="w-4 h-4" />
              <span>Drukuj raport expo</span>
            </button>
            <button
              onClick={onClose}
              className="px-4 py-2 bg-gray-800 hover:bg-gray-900 text-white rounded-xl text-xs sm:text-sm font-semibold transition-colors cursor-pointer"
            >
              Zamknij
            </button>
          </div>
        </div>

        {/* =========================================================================
            2. THE PRINTABLE TABLE DOCUMENT (ONLY VISIBLE ON PRINT)
               Contains Magazyn 1..N, rows A and B, carpet info, scannable barcode,
               and handwriting notes column!
           ========================================================================= */}
        <div id="stock-expo-report-printable">
          {/* Document Header */}
          <div className="flex items-center justify-between border-b-2 border-black pb-3 mb-3">
            <div className="flex items-center gap-3">
              <LeroyMerlinLogo className="h-9" />
              <div>
                <h1 className="text-base font-black text-black uppercase tracking-tight">
                  Arkusz inwentaryzacji ekspozycji dywanów
                </h1>
                <div className="text-[11px] text-gray-700 font-bold">
                  LEROY MERLIN • Sklep {STORE_NUMBER} {STORE_CITY} • Dział Dywany
                </div>
              </div>
            </div>

            <div className="text-right text-[11px] leading-tight">
              <div>Data: <strong className="font-mono">{new Date().toISOString().split('T')[0]}</strong></div>
              <div>Regałów: <strong>{baysList.length}</strong> | Dywanów: <strong>{carpets.length}</strong> ({stats.occupancyPercentage}%)</div>
            </div>
          </div>

          {/* Table */}
          <table className="w-full border-collapse border border-black text-[11px]">
            <thead>
              <tr className="bg-gray-100 border-b-2 border-black text-black uppercase text-[10px] tracking-wider font-black">
                <th className="p-2 border border-black w-[95px] text-center">Magazyn</th>
                <th className="p-2 border border-black w-[45px] text-center">Slot</th>
                <th className="p-2 border border-black">Dane dywanu</th>
                <th className="p-2 border border-black w-[150px] text-center">Kod kreskowy (EAN)</th>
                <th className="p-2 border border-black w-[165px]">Notatki / Weryfikacja</th>
              </tr>
            </thead>

            {baysList.map((bayNum) => {
              const carpetA = carpetsBySlot.get(`${bayNum}_A`);
              const carpetB = carpetsBySlot.get(`${bayNum}_B`);

              return (
                <tbody key={`bay-${bayNum}`} className="print-rack-group border-b-2 border-black">
                  {/* Row A */}
                  <tr className="border-b border-gray-400">
                    {/* Magazyn cell spanning slots A and B */}
                    <td
                      rowSpan={2}
                      className="p-2 border border-black font-black text-center align-middle bg-gray-50"
                    >
                      <div className="text-[9px] uppercase tracking-wider text-gray-500 font-bold">Regał</div>
                      <div className="text-xl font-black text-black leading-none my-0.5">{bayNum}</div>
                      <div className="text-[9px] text-gray-600 font-mono">Magazyn {bayNum}</div>
                    </td>

                    {/* Slot A */}
                    <td className="p-1.5 border border-black font-black text-center align-middle bg-blue-50/50">
                      <div className="text-xs font-black text-black">A</div>
                      <div className="text-[8px] text-gray-500 font-mono">{bayNum} A</div>
                    </td>

                    {/* Carpet data A */}
                    <td className="p-2 border border-black align-top">
                      {carpetA ? (
                        <div>
                          <div className="font-bold text-xs text-black leading-snug">
                            {carpetA.nazwa}
                          </div>
                          <div className="flex flex-wrap items-center gap-2 mt-1 text-[10px] text-gray-700">
                            <span className="font-mono font-bold bg-gray-100 px-1 py-0.5 border border-gray-300 rounded-xs">
                              REF: {carpetA.ref || '—'}
                            </span>
                            <span>Dodano: {carpetA.dodano || '—'}</span>
                            {carpetA.updatedBy && (
                              <span className="text-gray-500">({carpetA.updatedBy.split('@')[0]})</span>
                            )}
                          </div>
                        </div>
                      ) : (
                        <div className="py-1 text-[11px] text-gray-500 italic">
                          — WOLNE MIEJSCE —
                        </div>
                      )}
                    </td>

                    {/* Barcode A */}
                    <td className="p-1.5 border border-black text-center align-middle bg-white">
                      {carpetA && carpetA.ean ? (
                        <div className="flex flex-col items-center justify-center">
                          <PrintableBarcode value={carpetA.ean} />
                        </div>
                      ) : carpetA ? (
                        <span className="text-[9px] text-gray-400 italic">Brak EAN</span>
                      ) : (
                        <span className="text-gray-300 font-mono">—</span>
                      )}
                    </td>

                    {/* Notes A */}
                    <td className="p-1.5 border border-black text-[9px] align-top bg-white">
                      <div className="flex items-center gap-2 text-gray-700 mb-1">
                        <span className="flex items-center gap-1">
                          <span className="inline-block w-2.5 h-2.5 border border-black"></span>
                          <span>OK</span>
                        </span>
                        <span className="flex items-center gap-1">
                          <span className="inline-block w-2.5 h-2.5 border border-black"></span>
                          <span>Brak fiszki</span>
                        </span>
                        <span className="flex items-center gap-1">
                          <span className="inline-block w-2.5 h-2.5 border border-black"></span>
                          <span>Wymiana</span>
                        </span>
                      </div>
                      <div className="text-[8px] text-gray-500">Notatka:</div>
                      <div className="border-b border-dotted border-gray-500 h-2.5"></div>
                      <div className="border-b border-dotted border-gray-500 h-2.5 mt-0.5"></div>
                    </td>
                  </tr>

                  {/* Row B */}
                  <tr>
                    {/* Slot B */}
                    <td className="p-1.5 border border-black font-black text-center align-middle bg-blue-50/50">
                      <div className="text-xs font-black text-black">B</div>
                      <div className="text-[8px] text-gray-500 font-mono">{bayNum} B</div>
                    </td>

                    {/* Carpet data B */}
                    <td className="p-2 border border-black align-top">
                      {carpetB ? (
                        <div>
                          <div className="font-bold text-xs text-black leading-snug">
                            {carpetB.nazwa}
                          </div>
                          <div className="flex flex-wrap items-center gap-2 mt-1 text-[10px] text-gray-700">
                            <span className="font-mono font-bold bg-gray-100 px-1 py-0.5 border border-gray-300 rounded-xs">
                              REF: {carpetB.ref || '—'}
                            </span>
                            <span>Dodano: {carpetB.dodano || '—'}</span>
                            {carpetB.updatedBy && (
                              <span className="text-gray-500">({carpetB.updatedBy.split('@')[0]})</span>
                            )}
                          </div>
                        </div>
                      ) : (
                        <div className="py-1 text-[11px] text-gray-500 italic">
                          — WOLNE MIEJSCE —
                        </div>
                      )}
                    </td>

                    {/* Barcode B */}
                    <td className="p-1.5 border border-black text-center align-middle bg-white">
                      {carpetB && carpetB.ean ? (
                        <div className="flex flex-col items-center justify-center">
                          <PrintableBarcode value={carpetB.ean} />
                        </div>
                      ) : carpetB ? (
                        <span className="text-[9px] text-gray-400 italic">Brak EAN</span>
                      ) : (
                        <span className="text-gray-300 font-mono">—</span>
                      )}
                    </td>

                    {/* Notes B */}
                    <td className="p-1.5 border border-black text-[9px] align-top bg-white">
                      <div className="flex items-center gap-2 text-gray-700 mb-1">
                        <span className="flex items-center gap-1">
                          <span className="inline-block w-2.5 h-2.5 border border-black"></span>
                          <span>OK</span>
                        </span>
                        <span className="flex items-center gap-1">
                          <span className="inline-block w-2.5 h-2.5 border border-black"></span>
                          <span>Brak fiszki</span>
                        </span>
                        <span className="flex items-center gap-1">
                          <span className="inline-block w-2.5 h-2.5 border border-black"></span>
                          <span>Wymiana</span>
                        </span>
                      </div>
                      <div className="text-[8px] text-gray-500">Notatka:</div>
                      <div className="border-b border-dotted border-gray-500 h-2.5"></div>
                      <div className="border-b border-dotted border-gray-500 h-2.5 mt-0.5"></div>
                    </td>
                  </tr>
                </tbody>
              );
            })}
          </table>

          {/* Document Signatures */}
          <div className="mt-4 pt-3 border-t-2 border-black grid grid-cols-3 gap-3 text-[10px] text-gray-800">
            <div>
              <div className="font-bold">Data przeprowadzenia audytu:</div>
              <div className="mt-4 border-b border-black w-32"></div>
            </div>
            <div>
              <div className="font-bold">Podpis kontrolera (magazynier):</div>
              <div className="mt-4 border-b border-black w-44"></div>
            </div>
            <div>
              <div className="font-bold">Podpis kierownika sektora:</div>
              <div className="mt-4 border-b border-black w-44"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
