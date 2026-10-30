import { useState, useMemo } from 'react';
import { Carpet, STORE_NUMBER, STORE_CITY } from '../types';
import {
  Upload,
  BarChart2,
  Download,
  Trash2,
  Search,
  FileText,
  Calendar,
  Layers,
  Users,
  Plus,
  Pencil,
  Copy,
  Check,
  AlertTriangle,
  LogOut,
  History,
  User,
  Clock
} from 'lucide-react';
import * as XLSX from 'xlsx';
import { formatDateTime, formatRelativeTime } from '../utils/formatters';

interface WarehousePanelProps {
  carpets: Carpet[];
  activeBays: number[];
  onAddProduct: (preselectedMagazyn?: number, preselectedPodmagazyn?: string) => void;
  onEditProduct: (carpet: Carpet) => void;
  onDeleteProduct: (carpetId: string, name: string) => void;
  onOpenImport: () => void;
  onOpenStockExpo: () => void;
  onOpenHelp: () => void;
  onOpenActivityLogs: () => void;
  onLogout: () => void;
  onGenerateFiszka: (carpet: Carpet) => void;
  isSuperAdmin: boolean;
  onOpenSuperAdminModal: (initialTab?: 'admins' | 'warehouses') => void;
  onDeleteWarehouse: (bayNumber: number, deleteCarpets: boolean) => Promise<void>;
  onAddWarehouses: (numbers: number[]) => Promise<void>;
}

export function WarehousePanel({
  carpets,
  activeBays,
  onAddProduct,
  onEditProduct,
  onDeleteProduct,
  onOpenImport,
  onOpenStockExpo,
  onOpenHelp,
  onOpenActivityLogs,
  onLogout,
  onGenerateFiszka,
  isSuperAdmin,
  onOpenSuperAdminModal,
  onDeleteWarehouse,
  onAddWarehouses
}: WarehousePanelProps) {
  const [filterQuery, setFilterQuery] = useState('');
  const [onlyEmpty, setOnlyEmpty] = useState(false);
  const [onlyNew, setOnlyNew] = useState(false);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc' | 'name' | 'date'>('asc');

  // Quick warehouse deletion state
  const [deletingBayModal, setDeletingBayModal] = useState<{
    bayNum: number;
    carpetsInBay: Carpet[];
  } | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Quick warehouse add modal state
  const [isQuickAddOpen, setIsQuickAddOpen] = useState(false);
  const [quickAddInput, setQuickAddInput] = useState('');
  const [quickAddError, setQuickAddError] = useState<string | null>(null);
  const [isAdding, setIsAdding] = useState(false);

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

  // Determine all bay numbers to show based on activeBays
  const baysList = useMemo(() => {
    const sorted = [...activeBays];
    if (sortOrder === 'desc') {
      sorted.sort((a, b) => b - a);
    } else {
      sorted.sort((a, b) => a - b);
    }
    return sorted;
  }, [activeBays, sortOrder]);

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

  // Confirm delete of warehouse
  const handleConfirmWarehouseDelete = async () => {
    if (!deletingBayModal) return;
    try {
      setIsDeleting(true);
      await onDeleteWarehouse(deletingBayModal.bayNum, true);
      setDeletingBayModal(null);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Błąd podczas usuwania magazynu');
    } finally {
      setIsDeleting(false);
    }
  };

  // Quick Add warehouse handler
  const handleQuickAddSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setQuickAddError(null);

    const parts = quickAddInput.split(/[\s,]+/);
    const numbers: number[] = [];

    for (const p of parts) {
      if (!p) continue;
      if (p.includes('-')) {
        const [startStr, endStr] = p.split('-');
        const s = parseInt(startStr, 10);
        const e = parseInt(endStr, 10);
        if (!isNaN(s) && !isNaN(e) && s > 0 && e >= s) {
          for (let i = s; i <= Math.min(e, s + 100); i++) {
            if (!numbers.includes(i)) numbers.push(i);
          }
        }
      } else {
        const num = parseInt(p, 10);
        if (!isNaN(num) && num > 0 && !numbers.includes(num)) {
          numbers.push(num);
        }
      }
    }

    if (numbers.length === 0) {
      setQuickAddError('Wpisz numer (np. 121) lub zakres (np. 121-125).');
      return;
    }

    const toAdd = numbers.filter((n) => !activeBays.includes(n));
    if (toAdd.length === 0) {
      setQuickAddError('Wpisane magazyny już znajdują się na liście.');
      return;
    }

    try {
      setIsAdding(true);
      await onAddWarehouses(toAdd);
      setQuickAddInput('');
      setIsQuickAddOpen(false);
    } catch (err) {
      setQuickAddError(err instanceof Error ? err.message : 'Błąd dodawania magazynu');
    } finally {
      setIsAdding(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-3 sm:px-4 py-4 sm:py-6 flex flex-col gap-4">
      {/* Title matching Screenshot 3 & 4 */}
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div>
          <h1 className="text-lg sm:text-xl font-bold text-gray-900 tracking-tight">
            Panel magazynowy sklepu {STORE_NUMBER}
          </h1>
          <span className="text-xs text-gray-500 font-medium">
            Aktywne magazyny: <strong className="text-gray-800">{activeBays.length}</strong> | Dywanów: <strong className="text-gray-800">{carpets.length}</strong>
          </span>
        </div>

        {isSuperAdmin && (
          <div className="flex items-center gap-1.5 flex-wrap">
            <button
              onClick={() => setIsQuickAddOpen(true)}
              className="flex items-center gap-1 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-bold transition-all shadow-xs active:scale-95 cursor-pointer"
              title="Szybkie dodanie nowego magazynu"
            >
              <Plus className="w-3.5 h-3.5" />
              Dodaj magazyn
            </button>

            <button
              onClick={() => onOpenSuperAdminModal('warehouses')}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-100 hover:bg-blue-200 text-blue-900 rounded-lg text-xs font-bold transition-colors cursor-pointer"
              title="Zarządzanie listą magazynów"
            >
              <Layers className="w-3.5 h-3.5 text-blue-700" />
              Magazyny ({activeBays.length})
            </button>

            <button
              onClick={() => onOpenSuperAdminModal('admins')}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-purple-100 hover:bg-purple-200 text-purple-900 rounded-lg text-xs font-bold transition-colors cursor-pointer"
              title="Zarządzanie administratorami"
            >
              <Users className="w-3.5 h-3.5 text-purple-700" />
              Super Admin
            </button>
          </div>
        )}
      </div>

      {/* Top action buttons matching Screenshot 3 */}
      <div className="flex flex-col gap-2.5 print:hidden">
        {/* Row 1: Importuj bazę (amber/yellow) */}
        <button
          onClick={onOpenImport}
          className="w-full py-3 px-4 rounded-xl bg-[#eab308] hover:bg-[#ca8a04] text-gray-950 font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98 cursor-pointer"
        >
          <Upload className="w-4 h-4" />
          Importuj bazę
        </button>

        {/* Row 2: Sprawdź stock expo (amber/yellow) */}
        <button
          onClick={onOpenStockExpo}
          className="w-full py-3 px-4 rounded-xl bg-[#eab308] hover:bg-[#ca8a04] text-gray-950 font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98 cursor-pointer"
        >
          <BarChart2 className="w-4 h-4" />
          Sprawdź stock expo
        </button>

        {/* Row 3: Eksportuj bazę (blue) */}
        <button
          onClick={handleExportJSON}
          className="w-full py-3 px-4 rounded-xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98 cursor-pointer"
        >
          <Download className="w-4 h-4" />
          Eksportuj bazę
        </button>

        {/* Row 4: Eksportuj XLSX (blue) */}
        <button
          onClick={handleExportXLSX}
          className="w-full py-3 px-4 rounded-xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98 cursor-pointer"
        >
          <FileText className="w-4 h-4" />
          Eksportuj bazę do XLSX
        </button>

        {/* Row 5: Historia zmian w bazie (Rejestr audytu: kto co zmienił) */}
        <button
          onClick={onOpenActivityLogs}
          className="w-full py-3 px-4 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98 cursor-pointer"
          title="Zobacz pełny rejestr kto dodał, edytował lub usunął pozycje w bazie"
        >
          <History className="w-4 h-4" />
          Historia zmian (Kto co zmienił)
        </button>

        {/* Row 6: Wyloguj (red) */}
        <button
          onClick={onLogout}
          className="w-full py-3 px-4 rounded-xl bg-[#ef4444] hover:bg-[#dc2626] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98 cursor-pointer"
        >
          <LogOut className="w-4 h-4" />
          Wyloguj
        </button>

        {/* Row 6: Pomoc (gray/white) */}
        <button
          onClick={onOpenHelp}
          className="w-full py-3 px-4 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-900 font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-xs transition-transform active:scale-98 cursor-pointer"
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
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-700 text-xs cursor-pointer"
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
              <span className="font-semibold text-gray-700">Tylko nowe (&lt;30 dni)</span>
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
            </select>
          </div>
        </div>
      </div>

      {/* Racks / Bays listing */}
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
              {/* Bay Header: "Magazyn X" + [+ Dodaj produkt] button + [Super Admin: Usuń Magazyn] */}
              <div className="flex items-center justify-between gap-2 border-b border-gray-100 pb-3">
                <div className="flex items-center gap-2">
                  <h2 className="text-lg sm:text-xl font-black text-blue-600 tracking-tight">
                    Magazyn {bayNum}
                  </h2>
                  {hasCarpets && (
                    <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 border border-blue-200">
                      {bayCarpets.length} {bayCarpets.length === 1 ? 'dywan' : 'dywany'}
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-1.5">
                  {isSuperAdmin && (
                    <button
                      onClick={() =>
                        setDeletingBayModal({
                          bayNum,
                          carpetsInBay: carpetsByBay.get(bayNum) || []
                        })
                      }
                      className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-colors cursor-pointer"
                      title={`Usuń Magazyn ${bayNum} (tylko Super Admin)`}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}

                  <button
                    onClick={() => onAddProduct(bayNum)}
                    className="flex items-center gap-1.5 px-3 sm:px-4 py-2 rounded-xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-xs sm:text-sm shadow-xs transition-transform active:scale-95 cursor-pointer"
                  >
                    <Plus className="w-4 h-4" />
                    Dodaj produkt
                  </button>
                </div>
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

                {/* Any extra custom sub-bays if present */}
                {otherCarpets.map((carpet) => (
                  <SlotCard
                    key={carpet.id}
                    carpet={carpet}
                    onEdit={() => onEditProduct(carpet)}
                    onDelete={() => onDeleteProduct(carpet.id, carpet.nazwa)}
                    onGenerateFiszka={() => onGenerateFiszka(carpet)}
                  />
                ))}
              </div>
            </div>
          );
        })}

        {baysList.length === 0 && (
          <div className="p-8 text-center bg-white rounded-2xl border border-gray-200 text-gray-500 text-sm">
            Brak zdefiniowanych magazynów. Jako Super Admin kliknij{' '}
            <strong className="text-blue-600">„Dodaj magazyn”</strong> lub{' '}
            <strong className="text-blue-600">„Magazyny”</strong>, aby zainicjalizować listę.
          </div>
        )}
      </div>

      {/* Confirmation Modal for Warehouse Deletion from Card */}
      {deletingBayModal && (
        <div className="fixed inset-0 z-60 bg-black/70 flex items-center justify-center p-4 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-md w-full p-5 sm:p-6 shadow-2xl flex flex-col gap-4 animate-in fade-in zoom-in-95">
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <div className="text-center">
              <h3 className="text-base sm:text-lg font-black text-gray-900">
                Usunąć Magazyn {deletingBayModal.bayNum}?
              </h3>

              {deletingBayModal.carpetsInBay.length > 0 ? (
                <div className="mt-2 text-xs text-gray-600 flex flex-col gap-2">
                  <p className="text-red-700 font-bold bg-red-50 p-2.5 rounded-xl border border-red-200 text-left">
                    Uwaga! Magazyn {deletingBayModal.bayNum} zawiera obecnie {deletingBayModal.carpetsInBay.length} {deletingBayModal.carpetsInBay.length === 1 ? 'dywan' : 'dywany/ów'}:
                  </p>
                  <div className="max-h-32 overflow-y-auto text-left bg-gray-50 p-2 rounded-lg border border-gray-200 flex flex-col gap-1">
                    {deletingBayModal.carpetsInBay.map((c) => (
                      <div key={c.id} className="text-[11px] truncate">
                        • <strong className="font-mono">{c.miejsce || `${c.magazyn}${c.podmagazyn}`}</strong>: {c.nazwa}
                      </div>
                    ))}
                  </div>
                  <p className="text-gray-500 font-medium text-left">
                    Usunięcie magazynu usunie również te dywany z bazy.
                  </p>
                </div>
              ) : (
                <p className="text-xs text-gray-600 mt-2 font-medium">
                  Ten magazyn jest obecnie pusty. Czy na pewno chcesz usunąć Magazyn {deletingBayModal.bayNum} z listy regałów?
                </p>
              )}
            </div>

            <div className="flex gap-2 pt-2">
              <button
                onClick={() => setDeletingBayModal(null)}
                className="flex-1 py-2.5 px-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs sm:text-sm transition-colors cursor-pointer"
              >
                Anuluj
              </button>
              <button
                onClick={handleConfirmWarehouseDelete}
                disabled={isDeleting}
                className="flex-1 py-2.5 px-3 rounded-xl bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-bold text-xs sm:text-sm transition-colors cursor-pointer shadow-xs"
              >
                {isDeleting ? 'Usuwanie...' : 'Tak, usuń magazyn'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Quick Add Warehouse Modal */}
      {isQuickAddOpen && (
        <div className="fixed inset-0 z-60 bg-black/70 flex items-center justify-center p-4 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 sm:p-6 shadow-2xl flex flex-col gap-4 animate-in fade-in zoom-in-95">
            <h3 className="text-base sm:text-lg font-black text-gray-900">
              Dodaj nowy magazyn
            </h3>

            <form onSubmit={handleQuickAddSubmit} className="flex flex-col gap-3">
              <div>
                <label className="text-xs font-bold text-gray-700 block mb-1">
                  Numer magazynu lub zakres:
                </label>
                <input
                  type="text"
                  value={quickAddInput}
                  onChange={(e) => setQuickAddInput(e.target.value)}
                  placeholder="np. 15 lub 121-125"
                  className="w-full px-3 py-2 border border-gray-300 rounded-xl text-sm font-semibold focus:ring-2 focus:ring-blue-600 focus:outline-hidden"
                  autoFocus
                />
                <span className="text-[11px] text-gray-500 block mt-1">
                  Możesz wpisać pojedynczy numer (np. 15, 121) lub zakres (np. 121-125).
                </span>
              </div>

              {quickAddError && (
                <div className="p-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700">
                  {quickAddError}
                </div>
              )}

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsQuickAddOpen(false)}
                  className="flex-1 py-2.5 px-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs sm:text-sm transition-colors cursor-pointer"
                >
                  Anuluj
                </button>
                <button
                  type="submit"
                  disabled={isAdding || !quickAddInput.trim()}
                  className="flex-1 py-2.5 px-3 rounded-xl bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-bold text-xs sm:text-sm transition-colors cursor-pointer shadow-xs"
                >
                  {isAdding ? 'Dodawanie...' : 'Dodaj'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

// Slot card displaying carpet in sub-bay
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
  const [copiedEan, setCopiedEan] = useState(false);
  const [copiedRef, setCopiedRef] = useState(false);

  const handleCopy = (text: string, isEan: boolean) => {
    navigator.clipboard.writeText(text);
    if (isEan) {
      setCopiedEan(true);
      setTimeout(() => setCopiedEan(false), 1500);
    } else {
      setCopiedRef(true);
      setTimeout(() => setCopiedRef(false), 1500);
    }
  };

  return (
    <div className="border border-gray-200 bg-white rounded-xl p-3.5 flex flex-col gap-2.5 shadow-2xs hover:border-blue-300 transition-colors">
      <div className="flex items-center justify-between border-b border-gray-100 pb-2">
        <span className="font-extrabold text-xs uppercase tracking-wide text-blue-700 bg-blue-50 px-2 py-0.5 rounded-md border border-blue-200">
          Podmagazyn {carpet.podmagazyn} ({carpet.miejsce || `${carpet.magazyn} ${carpet.podmagazyn}`})
        </span>

        {carpet.dodano && (
          <span className="text-[11px] text-gray-400 flex items-center gap-1">
            <Calendar className="w-3 h-3" />
            {carpet.dodano}
          </span>
        )}
      </div>

      <div>
        <div className="text-xs sm:text-sm font-bold text-gray-900 leading-snug">
          {carpet.nazwa}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2 text-xs">
        <div
          onClick={() => handleCopy(carpet.ref, false)}
          className="bg-gray-50 hover:bg-gray-100 p-2 rounded-lg cursor-pointer flex items-center justify-between transition-colors border border-gray-100"
          title="Kliknij, aby skopiować REF"
        >
          <div>
            <span className="text-[10px] text-gray-400 font-extrabold block uppercase">REF LM</span>
            <span className="font-mono font-bold text-gray-800">{carpet.ref}</span>
          </div>
          {copiedRef ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5 text-gray-400" />}
        </div>

        <div
          onClick={() => handleCopy(carpet.ean, true)}
          className="bg-gray-50 hover:bg-gray-100 p-2 rounded-lg cursor-pointer flex items-center justify-between transition-colors border border-gray-100"
          title="Kliknij, aby skopiować EAN"
        >
          <div>
            <span className="text-[10px] text-gray-400 font-extrabold block uppercase">KOD EAN</span>
            <span className="font-mono font-bold text-gray-800 truncate block max-w-[110px] sm:max-w-none">
              {carpet.ean}
            </span>
          </div>
          {copiedEan ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5 text-gray-400" />}
        </div>
      </div>

      {/* Modification audit info: WHO made changes and WHEN */}
      <div className="flex items-center justify-between text-[11px] text-gray-500 bg-gray-50/80 rounded-lg px-2.5 py-1.5 border border-gray-100">
        <div className="flex items-center gap-1.5 truncate">
          <User className="w-3.5 h-3.5 text-blue-600 shrink-0" />
          <span className="text-gray-400">Zmienił:</span>
          <span className="font-semibold text-gray-800 truncate" title={carpet.updatedBy || 'Brak danych'}>
            {carpet.updatedBy || 'Brak danych'}
          </span>
        </div>
        {carpet.updatedAt && (
          <span className="text-gray-400 shrink-0 ml-1.5 text-[10px]" title={formatDateTime(carpet.updatedAt)}>
            {formatRelativeTime(carpet.updatedAt)}
          </span>
        )}
      </div>

      <div className="grid grid-cols-3 gap-1.5 pt-1">
        <button
          onClick={onEdit}
          className="flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs shadow-2xs active:scale-95 transition-all cursor-pointer"
        >
          <Pencil className="w-3.5 h-3.5 text-gray-600" />
          Edytuj
        </button>

        <button
          onClick={onDelete}
          className="flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg bg-[#ef4444] hover:bg-[#dc2626] text-white font-bold text-xs shadow-2xs active:scale-95 transition-all cursor-pointer"
        >
          <Trash2 className="w-3.5 h-3.5" />
          Usuń
        </button>

        <button
          onClick={onGenerateFiszka}
          className="flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-2xs active:scale-95 transition-all cursor-pointer"
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
    <div className="border border-dashed border-gray-200 bg-gray-50/40 rounded-xl p-3 flex items-center justify-between gap-2 text-xs">
      <div>
        <span className="font-bold text-gray-600">Podmagazyn {subBay}</span>
        <span className="text-gray-400 ml-2 italic">Wolne miejsce ({bay} {subBay})</span>
      </div>
      <button
        onClick={onAdd}
        className="flex items-center gap-1 px-3 py-1 rounded-md text-blue-600 hover:bg-blue-50 font-bold transition-colors cursor-pointer"
      >
        <Plus className="w-3.5 h-3.5" />
        Przypisz dywan
      </button>
    </div>
  );
}
