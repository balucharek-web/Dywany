import { useState, useMemo } from 'react';
import { Carpet, STORE_NUMBER, STORE_CITY } from '../types';
import {
  Upload,
  BarChart2,
  Download,
  FileSpreadsheet,
  Printer,
  LogOut,
  HelpCircle,
  Plus,
  Pencil,
  Trash2,
  Search,
  Filter,
  FileText,
  Calendar,
  Layers,
  Users
} from 'lucide-react';
import * as XLSX from 'xlsx';

interface WarehousePanelProps {
  carpets: Carpet[];
  onAddProduct: (preselectedMagazyn?: number, preselectedPodmagazyn?: string) => void;
  onEditProduct: (carpet: Carpet) => void;
  onDeleteProduct: (carpetId: string, name: string) => void;
  onOpenImport: () => void;
  onOpenStockExpo: () => void;
  onOpenHelp: () => void;
  onLogout: () => void;
  onGenerateFiszka: (carpet: Carpet) => void;
  isSuperAdmin: boolean;
  onOpenSuperAdminModal: () => void;
}

export function WarehousePanel({
  carpets,
  onAddProduct,
  onEditProduct,
  onDeleteProduct,
  onOpenImport,
  onOpenStockExpo,
  onOpenHelp,
  onLogout,
  onGenerateFiszka,
  isSuperAdmin,
  onOpenSuperAdminModal
}: WarehousePanelProps) {
  const [filterQuery, setFilterQuery] = useState('');
  const [onlyEmpty, setOnlyEmpty] = useState(false);
  const [onlyNew, setOnlyNew] = useState(false);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc' | 'name' | 'date'>('asc');
  const [maxBaysCount, setMaxBaysCount] = useState<number>(() => {
    const highestInCarpets = carpets.reduce((max, c) => Math.max(max, c.magazyn || 0), 20);
    return Math.max(highestInCarpets, 20);
  });

  // Export JSON backup
  const handleExportJSON = () => {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(carpets, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    const dateStr = new Date().toISOString().split('T')[0];
    downloadAnchor.setAttribute('download', `baza_dywany_sklep038_${dateStr}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  // Export XLSX spreadsheet
  const handleExportXLSX = () => {
    const rows = carpets.map((c) => ({
      'Magazyn (Regał)': c.magazyn,
      'Podmagazyn (Slot)': c.podmagazyn,
      'Miejsce': c.miejsce || `${c.magazyn} ${c.podmagazyn}`,
      'Nazwa dywanu': c.nazwa,
      'REF. LM': c.ref,
      'Kod EAN': c.ean,
      'Data dodania': c.dodano || ''
    }));

    // Sort by Magazyn and Podmagazyn before exporting
    rows.sort((a, b) => {
      if (a['Magazyn (Regał)'] !== b['Magazyn (Regał)']) {
        return a['Magazyn (Regał)'] - b['Magazyn (Regał)'];
      }
      return a['Podmagazyn (Slot)'].localeCompare(b['Podmagazyn (Slot)']);
    });

    const worksheet = XLSX.utils.json_to_sheet(rows);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Dywany Sklep 038');

    // Auto column widths
    const maxCols = [15, 18, 12, 40, 15, 18, 15];
    worksheet['!cols'] = maxCols.map((w) => ({ wch: w }));

    const dateStr = new Date().toISOString().split('T')[0];
    XLSX.writeFile(workbook, `dywany_sklep038_${dateStr}.xlsx`);
  };

  // Print warehouse listing
  const handlePrintList = () => {
    window.print();
  };

  // Determine all bay numbers to show
  const baysList = useMemo(() => {
    const setOfBays = new Set<number>();
    for (let i = 1; i <= maxBaysCount; i++) {
      setOfBays.add(i);
    }
    carpets.forEach((c) => setOfBays.add(c.magazyn));
    const sorted = Array.from(setOfBays);

    if (sortOrder === 'desc') {
      sorted.sort((a, b) => b - a);
    } else {
      sorted.sort((a, b) => a - b);
    }
    return sorted;
  }, [carpets, maxBaysCount, sortOrder]);

  // Group carpets by bay
  const carpetsByBay = useMemo(() => {
    const map = new Map<number, Carpet[]>();
    carpets.forEach((c) => {
      const list = map.get(c.magazyn) || [];
      list.push(c);
      map.set(c.magazyn, list);
    });
    return map;
  }, [carpets]);

  // Thirty days ago threshold for "Tylko nowe"
  const newDateThreshold = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return d.toISOString().split('T')[0];
  }, []);

  return (
    <div className="max-w-2xl mx-auto px-3 sm:px-4 py-4 sm:py-6 flex flex-col gap-4">
      {/* Title matching Screenshot 3 & 4 */}
      <div className="flex items-center justify-between">
        <h1 className="text-lg sm:text-xl font-bold text-gray-900 tracking-tight">
          Panel magazynowy sklepu {STORE_NUMBER}
        </h1>
        {isSuperAdmin && (
          <button
            onClick={onOpenSuperAdminModal}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-purple-100 hover:bg-purple-200 text-purple-900 rounded-lg text-xs font-bold transition-colors"
          >
            <Users className="w-3.5 h-3.5" />
            Super Admin
          </button>
        )}
      </div>

      {/* Top action buttons matching Screenshot 3 */}
      <div className="flex flex-col gap-2.5 print:hidden">
        {/* Row 1: Importuj bazę (amber/yellow) */}
        <button
          onClick={onOpenImport}
          className="w-full py-3 px-4 rounded-xl bg-[#eab308] hover:bg-[#ca8a04] text-gray-950 font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98"
        >
          <Upload className="w-4 h-4" />
          Importuj bazę
        </button>

        {/* Row 2: Sprawdź stock expo (amber/yellow) */}
        <button
          onClick={onOpenStockExpo}
          className="w-full py-3 px-4 rounded-xl bg-[#eab308] hover:bg-[#ca8a04] text-gray-950 font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98"
        >
          <BarChart2 className="w-4 h-4" />
          Sprawdź stock expo
        </button>

        {/* Row 3: Eksportuj bazę (blue) */}
        <button
          onClick={handleExportJSON}
          className="w-full py-3 px-4 rounded-xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98"
        >
          <Download className="w-4 h-4" />
          Eksportuj bazę
        </button>

        {/* Row 4: Eksportuj XLSX (blue) */}
        <button
          onClick={handleExportXLSX}
          className="w-full py-3 px-4 rounded-xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98"
        >
          <FileSpreadsheet className="w-4 h-4" />
          Eksportuj XLSX
        </button>

        {/* Row 5: Drukuj listę (blue) */}
        <button
          onClick={handlePrintList}
          className="w-full py-3 px-4 rounded-xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98"
        >
          <Printer className="w-4 h-4" />
          Drukuj listę
        </button>

        {/* Row 6: Wyloguj (red) */}
        <button
          onClick={onLogout}
          className="w-full py-3 px-4 rounded-xl bg-[#ef4444] hover:bg-[#dc2626] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98"
        >
          <LogOut className="w-4 h-4" />
          Wyloguj
        </button>

        {/* Row 7: Pomoc (gray/white with red icon) */}
        <button
          onClick={onOpenHelp}
          className="w-full py-3 px-4 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-900 font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98"
        >
          <span className="text-red-500 font-black text-base">?</span>
          Pomoc
        </button>
      </div>

      {/* Filter and Search Bar matching Screenshot 4 */}
      <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs mt-2 print:hidden flex flex-col gap-3">
        {/* Search input */}
        <div className="relative">
          <input
            type="text"
            value={filterQuery}
            onChange={(e) => setFilterQuery(e.target.value)}
            placeholder="Szukaj magazynu / produktu..."
            className="w-full px-3.5 py-2.5 text-sm sm:text-base border border-gray-300 rounded-xl bg-gray-50 focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
          />
          {filterQuery && (
            <button
              onClick={() => setFilterQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-700 text-xs"
            >
              Wyczyść
            </button>
          )}
        </div>

        {/* Checkboxes and sort dropdown matching Screenshot 4 */}
        <div className="flex flex-wrap items-center justify-between gap-3 text-xs sm:text-sm">
          <div className="flex items-center gap-4 flex-wrap">
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={onlyEmpty}
                onChange={(e) => setOnlyEmpty(e.target.checked)}
                className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="font-semibold text-gray-700">Tylko puste</span>
            </label>

            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={onlyNew}
                onChange={(e) => setOnlyNew(e.target.checked)}
                className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="font-semibold text-gray-700">Tylko nowe</span>
            </label>
          </div>

          <div>
            <select
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value as any)}
              className="px-3 py-1.5 border border-gray-300 rounded-lg text-xs font-semibold bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
            >
              <option value="asc">Sortuj rosnąco</option>
              <option value="desc">Sortuj malejąco</option>
              <option value="name">Sortuj po nazwie</option>
              <option value="date">Sortuj po dacie</option>
            </select>
          </div>
        </div>
      </div>

      {/* Racks / Bays listing matching Screenshot 3 & 4 */}
      <div className="flex flex-col gap-5 mt-2">
        {baysList.map((bayNum) => {
          let bayCarpets = carpetsByBay.get(bayNum) || [];

          // Apply filters
          if (filterQuery.trim()) {
            const q = filterQuery.toLowerCase();
            const bayMatches = `magazyn ${bayNum}`.includes(q) || `magazyn${bayNum}`.includes(q) || String(bayNum) === q;
            bayCarpets = bayCarpets.filter((c) => {
              return (
                bayMatches ||
                c.nazwa?.toLowerCase().includes(q) ||
                c.ref?.toLowerCase().includes(q) ||
                c.ean?.toLowerCase().includes(q) ||
                c.miejsce?.toLowerCase().includes(q)
              );
            });
            if (!bayMatches && bayCarpets.length === 0) {
              return null;
            }
          }

          if (onlyNew) {
            bayCarpets = bayCarpets.filter((c) => c.dodano && c.dodano >= newDateThreshold);
          }

          const hasCarpets = bayCarpets.length > 0;
          if (onlyEmpty && hasCarpets) {
            return null;
          }

          // Slots: standard sub-bays A and B
          const carpetA = bayCarpets.find((c) => c.podmagazyn.toUpperCase() === 'A');
          const carpetB = bayCarpets.find((c) => c.podmagazyn.toUpperCase() === 'B');
          const otherCarpets = bayCarpets.filter(
            (c) => c.podmagazyn.toUpperCase() !== 'A' && c.podmagazyn.toUpperCase() !== 'B'
          );

          return (
            <div
              key={bayNum}
              className="bg-white border border-gray-200 rounded-2xl p-4 sm:p-5 shadow-xs flex flex-col gap-4"
            >
              {/* Bay Header: "Magazyn X" + [+ Dodaj produkt] button */}
              <div className="flex items-center justify-between gap-2 border-b border-gray-100 pb-3">
                <h2 className="text-lg sm:text-xl font-bold text-blue-600 tracking-tight">
                  Magazyn {bayNum}
                </h2>
                <button
                  onClick={() => onAddProduct(bayNum)}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-xs sm:text-sm shadow-xs transition-transform active:scale-95"
                >
                  <Plus className="w-4 h-4" />
                  Dodaj produkt
                </button>
              </div>

              {/* Sub-bay slots */}
              <div className="flex flex-col gap-3">
                {/* Podmagazyn A */}
                {carpetA ? (
                  <SlotCard
                    carpet={carpetA}
                    onEdit={() => onEditProduct(carpetA)}
                    onDelete={() => onDeleteProduct(carpetA.id, carpetA.nazwa)}
                    onGenerateFiszka={() => onGenerateFiszka(carpetA)}
                  />
                ) : (
                  <EmptySlotCard
                    bay={bayNum}
                    subBay="A"
                    onAdd={() => onAddProduct(bayNum, 'A')}
                  />
                )}

                {/* Podmagazyn B */}
                {carpetB ? (
                  <SlotCard
                    carpet={carpetB}
                    onEdit={() => onEditProduct(carpetB)}
                    onDelete={() => onDeleteProduct(carpetB.id, carpetB.nazwa)}
                    onGenerateFiszka={() => onGenerateFiszka(carpetB)}
                  />
                ) : (
                  <EmptySlotCard
                    bay={bayNum}
                    subBay="B"
                    onAdd={() => onAddProduct(bayNum, 'B')}
                  />
                )}

                {/* Other sub-bays if any (C, D...) */}
                {otherCarpets.map((c) => (
                  <SlotCard
                    key={c.id}
                    carpet={c}
                    onEdit={() => onEditProduct(c)}
                    onDelete={() => onDeleteProduct(c.id, c.nazwa)}
                    onGenerateFiszka={() => onGenerateFiszka(c)}
                  />
                ))}
              </div>
            </div>
          );
        })}
      </div>

      {/* Button to extend bays */}
      <div className="flex justify-center mt-4 print:hidden">
        <button
          onClick={() => setMaxBaysCount((prev) => prev + 10)}
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl border-2 border-dashed border-gray-300 hover:border-blue-500 text-gray-700 hover:text-blue-600 font-bold text-xs sm:text-sm transition-all"
        >
          <Plus className="w-4 h-4" />
          Pokaż kolejne 10 magazynów (regałów)
        </button>
      </div>
    </div>
  );
}

// Occupied slot card matching Screenshot 3 & 4
function SlotCard({
  carpet,
  onEdit,
  onDelete,
  onGenerateFiszka
}: {
  carpet: Carpet;
  onEdit: () => void;
  onDelete: () => void;
  onGenerateFiszka: () => void;
}) {
  return (
    <div className="bg-gray-50/70 border border-gray-200 rounded-xl p-3.5 sm:p-4 flex flex-col gap-2.5 hover:border-gray-300 transition-colors">
      <div className="flex items-center justify-between">
        <span className="font-black text-gray-900 text-sm sm:text-base">
          Podmagazyn {carpet.podmagazyn}
        </span>
        {carpet.dodano && (
          <span className="text-[11px] text-gray-400 font-medium">
            {carpet.dodano}
          </span>
        )}
      </div>

      <div className="font-bold text-gray-900 text-sm sm:text-base leading-snug">
        {carpet.nazwa}
      </div>

      <div className="text-xs text-gray-600 font-mono">
        <div>Ref: <strong className="text-gray-900">{carpet.ref || '—'}</strong></div>
        <div>EAN: <strong className="text-gray-900">{carpet.ean}</strong></div>
      </div>

      {/* Action buttons matching screenshot: [✏️ Edytuj] [🗑️ Usuń] */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 mt-1">
        <button
          onClick={onEdit}
          className="flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg bg-[#facc15] hover:bg-[#eab308] text-gray-950 font-bold text-xs shadow-2xs active:scale-95 transition-all"
        >
          <Pencil className="w-3.5 h-3.5" />
          Edytuj
        </button>

        <button
          onClick={onDelete}
          className="flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg bg-[#ef4444] hover:bg-[#dc2626] text-white font-bold text-xs shadow-2xs active:scale-95 transition-all"
        >
          <Trash2 className="w-3.5 h-3.5" />
          Usuń
        </button>

        <button
          onClick={onGenerateFiszka}
          className="col-span-2 sm:col-span-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold text-xs shadow-2xs active:scale-95 transition-all"
        >
          <FileText className="w-3.5 h-3.5" />
          Fiszka
        </button>
      </div>
    </div>
  );
}

// Empty slot card
function EmptySlotCard({
  bay,
  subBay,
  onAdd
}: {
  bay: number;
  subBay: string;
  onAdd: () => void;
}) {
  return (
    <div className="border border-dashed border-gray-200 bg-gray-50/30 rounded-xl p-3 flex items-center justify-between gap-2 text-xs">
      <div>
        <span className="font-bold text-gray-500">Podmagazyn {subBay}</span>
        <span className="text-gray-400 ml-2 italic">Wolne miejsce ({bay} {subBay})</span>
      </div>
      <button
        onClick={onAdd}
        className="flex items-center gap-1 px-3 py-1 rounded-md text-blue-600 hover:bg-blue-50 font-bold transition-colors"
      >
        <Plus className="w-3.5 h-3.5" />
        Przypisz dywan
      </button>
    </div>
  );
}
