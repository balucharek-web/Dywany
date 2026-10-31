import { useState, useMemo } from 'react';
import { Carpet, STORE_NUMBER, STORE_CITY } from '../types';
import {
  X,
  AlertTriangle,
  Layers,
  BarChart2,
  Printer,
  Table,
  ArrowLeft,
  CheckCircle2,
  Filter
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
  // Mode: 'stats' (KPI analytics) | 'print_table' (Audit printable table)
  const [viewMode, setViewMode] = useState<'stats' | 'print_table'>('stats');

  // Print table filters
  const [filterType, setFilterType] = useState<'all' | 'occupied' | 'empty' | 'range'>('all');
  const [rangeStart, setRangeStart] = useState<number>(1);
  const [rangeEnd, setRangeEnd] = useState<number>(30);

  // Quick lookup map: "bay_slot" -> Carpet
  const carpetsBySlot = useMemo(() => {
    const map = new Map<string, Carpet>();
    carpets.forEach((c) => {
      const key = `${c.magazyn}_${String(c.podmagazyn || '').toUpperCase()}`;
      map.set(key, c);
    });
    return map;
  }, [carpets]);

  // Bays list
  const baysToCount = useMemo(() => {
    if (activeBays && activeBays.length > 0) {
      return [...activeBays].sort((a, b) => a - b);
    }
    return Array.from({ length: totalBays }, (_, i) => i + 1);
  }, [activeBays, totalBays]);

  // Compute analytics
  const stats = useMemo(() => {
    const totalSlots = baysToCount.length * 2;
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
    baysToCount.forEach((m) => {
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
  }, [carpets, baysToCount, carpetsBySlot]);

  // Filtered bays for the print table
  const displayedBays = useMemo(() => {
    return baysToCount.filter((bayNum) => {
      const hasA = carpetsBySlot.has(`${bayNum}_A`);
      const hasB = carpetsBySlot.has(`${bayNum}_B`);

      if (filterType === 'occupied') {
        return hasA || hasB;
      }
      if (filterType === 'empty') {
        return !hasA || !hasB;
      }
      if (filterType === 'range') {
        return bayNum >= rangeStart && bayNum <= rangeEnd;
      }
      return true; // 'all'
    });
  }, [baysToCount, carpetsBySlot, filterType, rangeStart, rangeEnd]);

  if (!isOpen) return null;

  const handlePrint = () => {
    window.print();
  };

  const handleOpenPrintView = () => {
    setViewMode('print_table');
  };

  return (
    <div className="stock-expo-modal-backdrop fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-2 sm:p-4 backdrop-blur-xs">
      <div
        className={`stock-expo-modal-container bg-white rounded-2xl shadow-2xl overflow-hidden flex flex-col transition-all duration-200 ${
          viewMode === 'print_table'
            ? 'max-w-5xl w-full max-h-[95vh]'
            : 'max-w-2xl w-full max-h-[90vh]'
        }`}
      >
        {/* Top Header (hidden when printing) */}
        <div className="flex items-center justify-between px-4 sm:px-5 py-3 border-b border-gray-200 bg-amber-50 print:hidden shrink-0">
          <div className="flex items-center gap-2 font-bold text-amber-950 text-xs sm:text-base">
            <BarChart2 className="w-5 h-5 text-amber-600 shrink-0" />
            <span>
              {viewMode === 'print_table'
                ? `Arkusz inwentaryzacji ekspozycji • Sklep ${STORE_NUMBER}`
                : `Stan Stock Expo • Sklep ${STORE_NUMBER} ${STORE_CITY}`}
            </span>
          </div>

          <div className="flex items-center gap-2">
            {viewMode === 'print_table' && (
              <button
                onClick={() => setViewMode('stats')}
                className="flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-gray-700 bg-white hover:bg-gray-100 border border-gray-300 rounded-lg transition-colors cursor-pointer"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Statystyki</span>
              </button>
            )}

            <button
              onClick={onClose}
              className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-amber-100 transition-colors cursor-pointer"
              title="Zamknij"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* View Mode 1: Stats & Occupancy Dashboard */}
        {viewMode === 'stats' && (
          <>
            <div className="p-4 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-4 sm:gap-5 print:hidden">
              {/* Top KPI Cards */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 sm:gap-3">
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-3 text-center">
                  <span className="text-[10px] font-bold text-gray-500 uppercase block">
                    Regały expo
                  </span>
                  <span className="text-xl sm:text-2xl font-black text-gray-900">
                    {baysToCount.length}
                  </span>
                  <span className="text-[10px] text-gray-400 block mt-0.5">
                    ({stats.totalSlots} miejsc)
                  </span>
                </div>
                <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-3 text-center">
                  <span className="text-[10px] font-bold text-emerald-700 uppercase block">
                    Zajęte miejsca
                  </span>
                  <span className="text-xl sm:text-2xl font-black text-emerald-800">
                    {stats.occupiedCount}
                  </span>
                  <span className="text-[10px] text-emerald-600 block mt-0.5">
                    ({stats.occupancyPercentage}%)
                  </span>
                </div>
                <div className="bg-blue-50 border border-blue-200 rounded-xl p-3 text-center">
                  <span className="text-[10px] font-bold text-blue-700 uppercase block">
                    Wolne miejsca
                  </span>
                  <span className="text-xl sm:text-2xl font-black text-blue-800">
                    {stats.emptyCount}
                  </span>
                  <span className="text-[10px] text-blue-600 block mt-0.5">
                    gotowe do ekspozycji
                  </span>
                </div>
                <div
                  className={`border rounded-xl p-3 text-center ${
                    stats.duplicates.length > 0
                      ? 'bg-red-50 border-red-200'
                      : 'bg-gray-50 border-gray-200'
                  }`}
                >
                  <span className="text-[10px] font-bold uppercase block text-gray-500">
                    Duplikaty EAN
                  </span>
                  <span
                    className={`text-xl sm:text-2xl font-black ${
                      stats.duplicates.length > 0 ? 'text-red-700' : 'text-gray-700'
                    }`}
                  >
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
                  <span>
                    {stats.occupancyPercentage}% ({stats.occupiedCount} z {stats.totalSlots})
                  </span>
                </div>
                <div className="w-full bg-gray-200 h-3 rounded-full overflow-hidden">
                  <div
                    className="bg-emerald-500 h-full rounded-full transition-all duration-500"
                    style={{ width: `${Math.min(100, stats.occupancyPercentage)}%` }}
                  ></div>
                </div>
              </div>

              {/* Action Banner: Druk tabeli magazynów A i B */}
              <div className="bg-linear-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-xl p-3.5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-2xs">
                <div>
                  <div className="text-xs sm:text-sm font-bold text-blue-950 flex items-center gap-1.5">
                    <Table className="w-4 h-4 text-blue-600" />
                    Wydruk inwentaryzacyjny (tabela magazynów i wiersze A/B)
                  </div>
                  <div className="text-[11px] text-blue-800 mt-0.5">
                    Wykaz z komórkami dla każdego dywanu, skanowalnymi kodami kreskowymi i miejscem na notatki
                  </div>
                </div>
                <button
                  onClick={handleOpenPrintView}
                  className="px-3.5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-bold transition-all shadow-xs active:scale-95 cursor-pointer shrink-0 flex items-center gap-1.5"
                >
                  <Printer className="w-3.5 h-3.5" />
                  Otwórz tabelę do druku
                </button>
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

            {/* Footer */}
            <div className="px-4 sm:px-5 py-3 border-t border-gray-200 bg-gray-50 flex items-center justify-between print:hidden shrink-0">
              <button
                onClick={handleOpenPrintView}
                className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-blue-700 bg-blue-50 hover:bg-blue-100 border border-blue-200 rounded-lg transition-colors cursor-pointer"
              >
                <Printer className="w-4 h-4 text-blue-600" />
                Drukuj raport expo (tabela A/B)
              </button>
              <button
                onClick={onClose}
                className="px-4 py-2 bg-gray-800 hover:bg-gray-900 text-white rounded-lg text-xs sm:text-sm font-semibold transition-colors cursor-pointer"
              >
                Zamknij
              </button>
            </div>
          </>
        )}

        {/* View Mode 2: Printable Table (Magazyn 1..N, wiersze A i B, barcode, notatki) */}
        {viewMode === 'print_table' && (
          <div className="flex-1 overflow-y-auto flex flex-col">
            {/* Filter controls toolbar (hidden on print) */}
            <div className="p-3 sm:p-4 bg-gray-50 border-b border-gray-200 print:hidden flex flex-col gap-2.5">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 flex-wrap">
                  <span className="text-xs font-bold text-gray-600 flex items-center gap-1 mr-1">
                    <Filter className="w-3.5 h-3.5" /> Zakres:
                  </span>
                  <button
                    onClick={() => setFilterType('all')}
                    className={`px-2.5 py-1 rounded-md text-xs font-semibold transition-colors cursor-pointer border ${
                      filterType === 'all'
                        ? 'bg-blue-600 text-white border-blue-600'
                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-100'
                    }`}
                  >
                    Wszystkie ({baysToCount.length})
                  </button>
                  <button
                    onClick={() => setFilterType('occupied')}
                    className={`px-2.5 py-1 rounded-md text-xs font-semibold transition-colors cursor-pointer border ${
                      filterType === 'occupied'
                        ? 'bg-blue-600 text-white border-blue-600'
                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-100'
                    }`}
                  >
                    Tylko z dywanami
                  </button>
                  <button
                    onClick={() => setFilterType('empty')}
                    className={`px-2.5 py-1 rounded-md text-xs font-semibold transition-colors cursor-pointer border ${
                      filterType === 'empty'
                        ? 'bg-blue-600 text-white border-blue-600'
                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-100'
                    }`}
                  >
                    Z wolnymi miejscami
                  </button>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={handlePrint}
                    className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs sm:text-sm font-bold shadow-xs active:scale-95 transition-all cursor-pointer"
                  >
                    <Printer className="w-4 h-4" />
                    <span>Drukuj teraz (A4)</span>
                  </button>
                </div>
              </div>

              {/* Quick range pills */}
              <div className="flex items-center gap-1.5 flex-wrap text-xs pt-1 border-t border-gray-200">
                <span className="text-[11px] text-gray-500 font-medium">Szybkie przedziały:</span>
                {[
                  { label: 'Regały 1-30', start: 1, end: 30 },
                  { label: 'Regały 31-60', start: 31, end: 60 },
                  { label: 'Regały 61-90', start: 61, end: 90 },
                  { label: 'Regały 91-120', start: 91, end: 120 }
                ].map((rng) => (
                  <button
                    key={rng.label}
                    onClick={() => {
                      setFilterType('range');
                      setRangeStart(rng.start);
                      setRangeEnd(rng.end);
                    }}
                    className={`px-2 py-0.5 rounded text-[11px] font-medium border cursor-pointer ${
                      filterType === 'range' && rangeStart === rng.start && rangeEnd === rng.end
                        ? 'bg-gray-900 text-white border-gray-900'
                        : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-100'
                    }`}
                  >
                    {rng.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Printable Document Area */}
            <div id="stock-expo-report-printable" className="p-3 sm:p-6 bg-white text-gray-950">
              {/* Document Header */}
              <div className="flex items-center justify-between border-b-2 border-gray-900 pb-3 mb-4">
                <div className="flex items-center gap-3">
                  <LeroyMerlinLogo className="h-9" />
                  <div>
                    <h2 className="text-base sm:text-lg font-black text-gray-900 uppercase tracking-tight">
                      Arkusz inwentaryzacji ekspozycji dywanów
                    </h2>
                    <div className="text-xs text-gray-600 font-semibold">
                      LEROY MERLIN • Sklep {STORE_NUMBER} {STORE_CITY} • Sektor Dywany
                    </div>
                  </div>
                </div>

                <div className="text-right text-xs">
                  <div className="font-bold text-gray-900">
                    Data:{' '}
                    <span className="font-mono">
                      {new Date().toISOString().split('T')[0]}
                    </span>
                  </div>
                  <div className="text-[11px] text-gray-600">
                    Regałów w raporcie: <strong>{displayedBays.length}</strong> | Dywanów:{' '}
                    <strong>{carpets.length}</strong>
                  </div>
                </div>
              </div>

              {/* The Inventory Table */}
              <div className="overflow-x-auto border border-gray-400 rounded-lg">
                <table className="w-full border-collapse text-left text-xs">
                  <thead>
                    <tr className="bg-gray-100 border-b-2 border-gray-900 text-gray-800 uppercase text-[10px] tracking-wider font-extrabold">
                      <th className="p-2.5 border-r border-gray-300 w-[90px] sm:w-[110px] text-center">
                        Magazyn (Regał)
                      </th>
                      <th className="p-2.5 border-r border-gray-300 w-[50px] text-center">
                        Slot
                      </th>
                      <th className="p-2.5 border-r border-gray-300">
                        Dane dywanu (Nazwa, REF LM, Wymiary)
                      </th>
                      <th className="p-2.5 border-r border-gray-300 w-[140px] sm:w-[160px] text-center">
                        Kod kreskowy (do zeskanowania)
                      </th>
                      <th className="p-2.5 w-[160px] sm:w-[200px]">
                        Notatki / Weryfikacja fizyczna
                      </th>
                    </tr>
                  </thead>

                  {displayedBays.map((bayNum) => {
                    const carpetA = carpetsBySlot.get(`${bayNum}_A`);
                    const carpetB = carpetsBySlot.get(`${bayNum}_B`);

                    return (
                      <tbody
                        key={`bay-group-${bayNum}`}
                        className="print-rack-group border-b-2 border-gray-400 hover:bg-gray-50/50"
                      >
                        {/* Wiersz A */}
                        <tr className="border-b border-gray-200">
                          {/* Komórka Magazynu - wspólna dla A i B */}
                          <td
                            rowSpan={2}
                            className="p-2.5 border-r-2 border-gray-400 font-bold text-center bg-gray-50/80 align-middle"
                          >
                            <span className="text-[10px] text-gray-500 font-semibold block uppercase">
                              Magazyn
                            </span>
                            <span className="text-xl sm:text-2xl font-black text-gray-900 block leading-tight">
                              {bayNum}
                            </span>
                            <span className="text-[10px] text-gray-500 font-mono block mt-0.5">
                              Regał {bayNum}
                            </span>
                          </td>

                          {/* Slot A */}
                          <td className="p-2 border-r border-gray-300 font-bold text-center bg-blue-50/40 align-middle">
                            <span className="inline-block px-1.5 py-0.5 rounded text-xs font-black text-blue-900 bg-blue-100 border border-blue-200">
                              A
                            </span>
                            <span className="text-[9px] text-gray-500 block font-mono mt-0.5">
                              {bayNum} A
                            </span>
                          </td>

                          {/* Dane dywanu A */}
                          <td className="p-2.5 border-r border-gray-300 align-top">
                            {carpetA ? (
                              <div>
                                <div className="font-bold text-xs sm:text-sm text-gray-950 leading-snug">
                                  {carpetA.nazwa}
                                </div>
                                <div className="flex flex-wrap items-center gap-2 mt-1 text-[11px] text-gray-700">
                                  <span className="font-mono font-bold bg-gray-100 px-1.5 py-0.5 rounded border border-gray-200">
                                    REF: {carpetA.ref || '—'}
                                  </span>
                                  <span className="text-gray-500">
                                    Dodano: {carpetA.dodano || '—'}
                                  </span>
                                  {carpetA.updatedBy && (
                                    <span className="text-[10px] text-gray-400">
                                      ({carpetA.updatedBy.split('@')[0]})
                                    </span>
                                  )}
                                </div>
                              </div>
                            ) : (
                              <div className="py-2 text-xs text-gray-400 italic flex items-center gap-1.5">
                                <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0"></span>
                                <span>WOLNE MIEJSCE (gotowe do powieszenia dywanu)</span>
                              </div>
                            )}
                          </td>

                          {/* Kod kreskowy A */}
                          <td className="p-2 border-r border-gray-300 text-center align-middle bg-white">
                            {carpetA && carpetA.ean ? (
                              <div className="flex flex-col items-center justify-center">
                                <PrintableBarcode value={carpetA.ean} />
                              </div>
                            ) : carpetA ? (
                              <span className="text-[10px] text-gray-400 italic">Brak EAN</span>
                            ) : (
                              <span className="text-gray-300 font-mono text-xs">—</span>
                            )}
                          </td>

                          {/* Notatki w wierszu A */}
                          <td className="p-2 text-[10px] align-top bg-white">
                            <div className="flex items-center gap-2.5 text-[9px] text-gray-600 mb-1">
                              <span className="flex items-center gap-1">
                                <span className="inline-block w-3 h-3 border border-gray-400 rounded-2xs"></span>
                                <span>Zgodny</span>
                              </span>
                              <span className="flex items-center gap-1">
                                <span className="inline-block w-3 h-3 border border-gray-400 rounded-2xs"></span>
                                <span>Brak fiszki</span>
                              </span>
                              <span className="flex items-center gap-1">
                                <span className="inline-block w-3 h-3 border border-gray-400 rounded-2xs"></span>
                                <span>Wymiana</span>
                              </span>
                            </div>
                            <div className="text-[9px] text-gray-400 mt-1">Notatka:</div>
                            <div className="border-b border-dotted border-gray-400 h-3"></div>
                            <div className="border-b border-dotted border-gray-400 h-3 mt-1"></div>
                          </td>
                        </tr>

                        {/* Wiersz B */}
                        <tr>
                          {/* Slot B */}
                          <td className="p-2 border-r border-gray-300 font-bold text-center bg-blue-50/40 align-middle">
                            <span className="inline-block px-1.5 py-0.5 rounded text-xs font-black text-blue-900 bg-blue-100 border border-blue-200">
                              B
                            </span>
                            <span className="text-[9px] text-gray-500 block font-mono mt-0.5">
                              {bayNum} B
                            </span>
                          </td>

                          {/* Dane dywanu B */}
                          <td className="p-2.5 border-r border-gray-300 align-top">
                            {carpetB ? (
                              <div>
                                <div className="font-bold text-xs sm:text-sm text-gray-950 leading-snug">
                                  {carpetB.nazwa}
                                </div>
                                <div className="flex flex-wrap items-center gap-2 mt-1 text-[11px] text-gray-700">
                                  <span className="font-mono font-bold bg-gray-100 px-1.5 py-0.5 rounded border border-gray-200">
                                    REF: {carpetB.ref || '—'}
                                  </span>
                                  <span className="text-gray-500">
                                    Dodano: {carpetB.dodano || '—'}
                                  </span>
                                  {carpetB.updatedBy && (
                                    <span className="text-[10px] text-gray-400">
                                      ({carpetB.updatedBy.split('@')[0]})
                                    </span>
                                  )}
                                </div>
                              </div>
                            ) : (
                              <div className="py-2 text-xs text-gray-400 italic flex items-center gap-1.5">
                                <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0"></span>
                                <span>WOLNE MIEJSCE (gotowe do powieszenia dywanu)</span>
                              </div>
                            )}
                          </td>

                          {/* Kod kreskowy B */}
                          <td className="p-2 border-r border-gray-300 text-center align-middle bg-white">
                            {carpetB && carpetB.ean ? (
                              <div className="flex flex-col items-center justify-center">
                                <PrintableBarcode value={carpetB.ean} />
                              </div>
                            ) : carpetB ? (
                              <span className="text-[10px] text-gray-400 italic">Brak EAN</span>
                            ) : (
                              <span className="text-gray-300 font-mono text-xs">—</span>
                            )}
                          </td>

                          {/* Notatki w wierszu B */}
                          <td className="p-2 text-[10px] align-top bg-white">
                            <div className="flex items-center gap-2.5 text-[9px] text-gray-600 mb-1">
                              <span className="flex items-center gap-1">
                                <span className="inline-block w-3 h-3 border border-gray-400 rounded-2xs"></span>
                                <span>Zgodny</span>
                              </span>
                              <span className="flex items-center gap-1">
                                <span className="inline-block w-3 h-3 border border-gray-400 rounded-2xs"></span>
                                <span>Brak fiszki</span>
                              </span>
                              <span className="flex items-center gap-1">
                                <span className="inline-block w-3 h-3 border border-gray-400 rounded-2xs"></span>
                                <span>Wymiana</span>
                              </span>
                            </div>
                            <div className="text-[9px] text-gray-400 mt-1">Notatka:</div>
                            <div className="border-b border-dotted border-gray-400 h-3"></div>
                            <div className="border-b border-dotted border-gray-400 h-3 mt-1"></div>
                          </td>
                        </tr>
                      </tbody>
                    );
                  })}
                </table>
              </div>

              {/* Document Sign-off Footer */}
              <div className="mt-6 pt-4 border-t-2 border-gray-300 grid grid-cols-2 sm:grid-cols-3 gap-4 text-xs text-gray-700">
                <div>
                  <div className="font-semibold text-gray-600">Data kontroli:</div>
                  <div className="mt-3 border-b border-gray-400 w-36"></div>
                </div>
                <div>
                  <div className="font-semibold text-gray-600">Podpis pracownika (kontrola):</div>
                  <div className="mt-3 border-b border-gray-400 w-48"></div>
                </div>
                <div className="col-span-2 sm:col-span-1">
                  <div className="font-semibold text-gray-600">Podpis kierownika sektora:</div>
                  <div className="mt-3 border-b border-gray-400 w-48"></div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
