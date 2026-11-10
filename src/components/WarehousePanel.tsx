import { useState, useMemo, useEffect } from 'react';
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
  Clock,
  SlidersHorizontal,
  ArrowLeftRight,
  ExternalLink,
  BarChart3,
  LayoutList,
  LayoutGrid,
  Camera,
  ArrowUp
} from 'lucide-react';
import * as XLSX from 'xlsx';
import { formatDateTime, formatRelativeTime, getLeroyMerlinSearchUrl } from '../utils/formatters';
import { WarehouseToolsModal } from './WarehouseToolsModal';
import { Haptics } from '../utils/haptics';

interface BayTheme {
  id: string;
  cardBg: string;
  cardBorder: string;
  headerBg: string;
  headerBorder: string;
  titleText: string;
  accentBar: string;
  badge: string;
  subBayTag: string;
  slotHoverBorder: string;
  emptyHoverBg: string;
}

const BAY_THEMES: BayTheme[] = [
  {
    id: 'blue',
    cardBg: 'bg-blue-50/35',
    cardBorder: 'border-blue-200/90 hover:border-blue-300',
    headerBg: 'bg-gradient-to-r from-blue-100/60 to-blue-50/40',
    headerBorder: 'border-blue-200/70',
    titleText: 'text-blue-800',
    accentBar: 'bg-blue-600',
    badge: 'bg-blue-100/90 text-blue-800 border-blue-200',
    subBayTag: 'text-blue-800 bg-blue-50 border border-blue-200',
    slotHoverBorder: 'hover:border-blue-300',
    emptyHoverBg: 'hover:bg-blue-50/50'
  },
  {
    id: 'emerald',
    cardBg: 'bg-emerald-50/35',
    cardBorder: 'border-emerald-200/90 hover:border-emerald-300',
    headerBg: 'bg-gradient-to-r from-emerald-100/60 to-emerald-50/40',
    headerBorder: 'border-emerald-200/70',
    titleText: 'text-emerald-800',
    accentBar: 'bg-emerald-600',
    badge: 'bg-emerald-100/90 text-emerald-800 border-emerald-200',
    subBayTag: 'text-emerald-800 bg-emerald-50 border border-emerald-200',
    slotHoverBorder: 'hover:border-emerald-300',
    emptyHoverBg: 'hover:bg-emerald-50/50'
  },
  {
    id: 'amber',
    cardBg: 'bg-amber-50/35',
    cardBorder: 'border-amber-200/90 hover:border-amber-300',
    headerBg: 'bg-gradient-to-r from-amber-100/60 to-amber-50/40',
    headerBorder: 'border-amber-200/70',
    titleText: 'text-amber-900',
    accentBar: 'bg-amber-600',
    badge: 'bg-amber-100/90 text-amber-900 border-amber-200',
    subBayTag: 'text-amber-900 bg-amber-50 border border-amber-200',
    slotHoverBorder: 'hover:border-amber-300',
    emptyHoverBg: 'hover:bg-amber-50/50'
  },
  {
    id: 'purple',
    cardBg: 'bg-purple-50/35',
    cardBorder: 'border-purple-200/90 hover:border-purple-300',
    headerBg: 'bg-gradient-to-r from-purple-100/60 to-purple-50/40',
    headerBorder: 'border-purple-200/70',
    titleText: 'text-purple-800',
    accentBar: 'bg-purple-600',
    badge: 'bg-purple-100/90 text-purple-800 border-purple-200',
    subBayTag: 'text-purple-800 bg-purple-50 border border-purple-200',
    slotHoverBorder: 'hover:border-purple-300',
    emptyHoverBg: 'hover:bg-purple-50/50'
  },
  {
    id: 'rose',
    cardBg: 'bg-rose-50/35',
    cardBorder: 'border-rose-200/90 hover:border-rose-300',
    headerBg: 'bg-gradient-to-r from-rose-100/60 to-rose-50/40',
    headerBorder: 'border-rose-200/70',
    titleText: 'text-rose-800',
    accentBar: 'bg-rose-600',
    badge: 'bg-rose-100/90 text-rose-800 border-rose-200',
    subBayTag: 'text-rose-800 bg-rose-50 border border-rose-200',
    slotHoverBorder: 'hover:border-rose-300',
    emptyHoverBg: 'hover:bg-rose-50/50'
  },
  {
    id: 'teal',
    cardBg: 'bg-teal-50/35',
    cardBorder: 'border-teal-200/90 hover:border-teal-300',
    headerBg: 'bg-gradient-to-r from-teal-100/60 to-teal-50/40',
    headerBorder: 'border-teal-200/70',
    titleText: 'text-teal-900',
    accentBar: 'bg-teal-600',
    badge: 'bg-teal-100/90 text-teal-900 border-teal-200',
    subBayTag: 'text-teal-900 bg-teal-50 border border-teal-200',
    slotHoverBorder: 'hover:border-teal-300',
    emptyHoverBg: 'hover:bg-teal-50/50'
  }
];

const getBayTheme = (bayNum: number): BayTheme => {
  const index = Math.abs((bayNum - 1) % BAY_THEMES.length);
  return BAY_THEMES[index];
};

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
  onMoveProduct?: (carpet: Carpet) => void;
  onOpenPowerBiAudit?: () => void;
  pbiCount?: number;
  onOpenScanner?: () => void;
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
  onAddWarehouses,
  onMoveProduct,
  onOpenPowerBiAudit,
  pbiCount,
  onOpenScanner
}: WarehousePanelProps) {
  const [filterQuery, setFilterQuery] = useState('');
  const [onlyEmpty, setOnlyEmpty] = useState(false);
  const [onlyNew, setOnlyNew] = useState(false);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc' | 'name' | 'date'>('asc');
  const [isToolsOpen, setIsToolsOpen] = useState(false);
  const [showScrollTop, setShowScrollTop] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setShowScrollTop(window.scrollY > 350);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Desktop view mode: 'horizontal' (slim wide row with Slot A and B side-by-side) or 'cards' (2-column bays)
  const [desktopViewMode, setDesktopViewMode] = useState<'horizontal' | 'cards'>(() => {
    try {
      const saved = localStorage.getItem('lm_warehouse_desktop_view');
      if (saved === 'cards') return 'cards';
    } catch {
      // ignore
    }
    return 'horizontal';
  });

  const handleSetDesktopViewMode = (mode: 'horizontal' | 'cards') => {
    setDesktopViewMode(mode);
    try {
      localStorage.setItem('lm_warehouse_desktop_view', mode);
    } catch {
      // ignore
    }
  };

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
    <div className="max-w-6xl xl:max-w-7xl mx-auto px-3 sm:px-6 py-4 sm:py-6 flex flex-col gap-4 pb-20 sm:pb-12">
      {/* Sleek, Modern Control Bar: Works seamlessly on mobile and desktop without wrapping */}
      <div className="flex flex-col gap-2.5 print:hidden">
        {/* Row 1: Header + Action group */}
        <div className="flex items-center justify-between gap-2">
          {/* Title & metadata */}
          <div className="min-w-0">
            <h1 className="text-base sm:text-lg font-black text-gray-900 tracking-tight flex items-center gap-1.5 truncate">
              <span>Magazyn {STORE_NUMBER}</span>
              <span className="text-[11px] font-semibold text-gray-500 font-sans hidden xs:inline">
                • {activeBays.length} reg. • {carpets.length} {carpets.length === 1 ? 'dywan' : 'dywanów'}
              </span>
            </h1>
            <div className="text-[10px] text-gray-500 font-medium xs:hidden">
              {activeBays.length} regałów • {carpets.length} dywanów
            </div>
          </div>

          {/* Quick action buttons */}
          <div className="flex items-center gap-1 sm:gap-1.5 shrink-0">
            {/* Quick action: Audyt Power BI */}
            {onOpenPowerBiAudit && (
              <button
                onClick={onOpenPowerBiAudit}
                className="inline-flex items-center gap-1 sm:gap-1.5 h-8 sm:h-9 px-2 sm:px-3 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 text-amber-950 border border-amber-300 text-xs font-bold transition-all shadow-2xs active:scale-95 cursor-pointer shrink-0"
                title="Porównaj ekspozycję sklepu z oficjalnym raportem Stock Expo w Power BI"
              >
                <BarChart3 className="w-3.5 h-3.5 text-amber-700 shrink-0" />
                <span className="hidden sm:inline">Audyt Power BI</span>
                <span className="sm:hidden font-extrabold text-[11px]">PBI</span>
                {typeof pbiCount === 'number' && pbiCount > 0 && (
                  <span className="px-1.5 py-0.2 rounded-full text-[9px] bg-amber-200 text-amber-900 font-mono font-bold">
                    {pbiCount}
                  </span>
                )}
              </button>
            )}

            {/* Quick action: Stock expo */}
            <button
              onClick={onOpenStockExpo}
              className="inline-flex items-center gap-1 sm:gap-1.5 h-8 sm:h-9 px-2 sm:px-3 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-900 border border-amber-200 text-xs font-bold transition-all shadow-2xs active:scale-95 cursor-pointer shrink-0"
              title="Zajętość i wolne miejsca na regałach"
            >
              <BarChart2 className="w-3.5 h-3.5 text-amber-700 shrink-0" />
              <span className="hidden sm:inline">Stock expo</span>
            </button>

            {/* Quick action: Historia zmian (audyt - visible on sm+) */}
            <button
              onClick={onOpenActivityLogs}
              className="hidden sm:inline-flex items-center gap-1.5 h-9 px-3 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-900 border border-purple-200 text-xs font-bold transition-all shadow-2xs active:scale-95 cursor-pointer shrink-0"
              title="Zobacz historię zmian magazynu"
            >
              <History className="w-3.5 h-3.5 text-purple-700 shrink-0" />
              <span>Historia</span>
            </button>

            {/* Main Tools button: opens WarehouseToolsModal */}
            <button
              onClick={() => setIsToolsOpen(true)}
              className="inline-flex items-center gap-1 sm:gap-1.5 h-8 sm:h-9 px-2.5 sm:px-3 rounded-xl bg-gray-900 hover:bg-black text-white text-xs font-bold transition-all shadow-xs active:scale-95 cursor-pointer shrink-0"
              title="Wszystkie opcje magazynu (import, eksport, kopia, zarządzanie)"
            >
              <SlidersHorizontal className="w-3.5 h-3.5 text-amber-400 shrink-0" />
              <span className="hidden xs:inline">Narzędzia</span>
            </button>
          </div>
        </div>

        {/* Row 2: Search & Filter Toolbar */}
        <div className="bg-white border border-gray-200 rounded-2xl p-2 sm:p-2.5 shadow-2xs flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2 text-xs">
          {/* Search box */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={filterQuery}
              onChange={(e) => setFilterQuery(e.target.value)}
              placeholder="Szukaj regału, ref, EAN, nazwy..."
              className="w-full pl-9 pr-8 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-hidden"
            />
            {filterQuery && (
              <button
                onClick={() => setFilterQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-700 text-xs font-bold p-1 cursor-pointer"
              >
                ✕
              </button>
            )}
          </div>

          {/* Filter chips & Sort dropdown */}
          <div className="flex items-center justify-between sm:justify-end gap-2 shrink-0">
            <div className="flex items-center gap-1 bg-gray-100 p-0.5 rounded-xl border border-gray-200">
              <button
                type="button"
                onClick={() => {
                  setOnlyEmpty(false);
                  setOnlyNew(false);
                }}
                className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  !onlyEmpty && !onlyNew
                    ? 'bg-white text-gray-900 shadow-2xs'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                Wszystkie
              </button>
              <button
                type="button"
                onClick={() => {
                  setOnlyEmpty(!onlyEmpty);
                  if (!onlyEmpty) setOnlyNew(false);
                }}
                className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  onlyEmpty
                    ? 'bg-blue-600 text-white shadow-2xs'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                Puste
              </button>
              <button
                type="button"
                onClick={() => {
                  setOnlyNew(!onlyNew);
                  if (!onlyNew) setOnlyEmpty(false);
                }}
                className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  onlyNew
                    ? 'bg-blue-600 text-white shadow-2xs'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                Nowe &lt;30d
              </button>
            </div>

            <select
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value as any)}
              className="h-8 px-2 border border-gray-200 rounded-xl text-xs font-bold bg-gray-50 text-gray-700 hover:bg-white focus:outline-hidden cursor-pointer shrink-0"
            >
              <option value="asc">1 → {activeBays.length}</option>
              <option value="desc">{activeBays.length} → 1</option>
            </select>

            {/* Desktop View Mode Switcher: Poziomy (paski) vs Kafelki */}
            <div className="hidden lg:flex items-center gap-1 bg-gray-100 p-0.5 rounded-xl border border-gray-200">
              <button
                type="button"
                onClick={() => handleSetDesktopViewMode('horizontal')}
                className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  desktopViewMode === 'horizontal'
                    ? 'bg-white text-blue-700 shadow-2xs'
                    : 'text-gray-500 hover:text-gray-900'
                }`}
                title="Widok poziomy (wąskie pasy, Slot A i B obok siebie, minimalne przewijanie na PC)"
              >
                <LayoutList className="w-3.5 h-3.5" />
                <span>Pasy (A | B)</span>
              </button>
              <button
                type="button"
                onClick={() => handleSetDesktopViewMode('cards')}
                className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  desktopViewMode === 'cards'
                    ? 'bg-white text-gray-900 shadow-2xs'
                    : 'text-gray-500 hover:text-gray-900'
                }`}
                title="Tradycyjny widok kafelkowy (kolumny)"
              >
                <LayoutGrid className="w-3.5 h-3.5" />
                <span>Kafelki</span>
              </button>
            </div>
          </div>
        </div>

        {/* Row 3: Quick Rack Navigator (Jump to rack) */}
        {activeBays.length > 0 && (
          <div className="flex items-center gap-1 overflow-x-auto py-1 px-0.5 text-xs">
            <span className="text-[10px] font-black text-gray-400 uppercase tracking-wider shrink-0 pr-1">
              Regał:
            </span>
            {baysList.map((bayNum) => {
              const count = carpetsByBay.get(bayNum)?.length || 0;
              return (
                <button
                  key={`jump-${bayNum}`}
                  onClick={() => {
                    const el = document.getElementById(`bay-card-${bayNum}`);
                    if (el) {
                      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                      el.classList.add('ring-4', 'ring-blue-400');
                      setTimeout(() => el.classList.remove('ring-4', 'ring-blue-400'), 1500);
                    }
                  }}
                  className={`min-w-[28px] h-7 px-1.5 rounded-lg font-mono font-bold text-xs shrink-0 transition-all cursor-pointer flex items-center justify-center border ${
                    count === 2
                      ? 'bg-emerald-50 text-emerald-800 border-emerald-200 hover:bg-emerald-100'
                      : count === 1
                      ? 'bg-amber-50 text-amber-800 border-amber-200 hover:bg-amber-100'
                      : 'bg-gray-50 text-gray-500 border-gray-200 hover:bg-gray-100'
                  }`}
                  title={`Magazyn ${bayNum} (${count}/2 dywany)`}
                >
                  {bayNum}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Warehouse Tools Modal */}
      <WarehouseToolsModal
        isOpen={isToolsOpen}
        onClose={() => setIsToolsOpen(false)}
        onOpenImport={onOpenImport}
        onOpenStockExpo={onOpenStockExpo}
        onOpenPowerBiAudit={onOpenPowerBiAudit || (() => {})}
        pbiCount={pbiCount}
        onExportJSON={handleExportJSON}
        onExportXLSX={handleExportXLSX}
        onOpenActivityLogs={onOpenActivityLogs}
        onOpenHelp={onOpenHelp}
        onLogout={onLogout}
        isSuperAdmin={isSuperAdmin}
        onOpenQuickAdd={() => setIsQuickAddOpen(true)}
        onOpenSuperAdminModal={onOpenSuperAdminModal}
        totalBays={activeBays.length}
        totalCarpets={carpets.length}
      />

      {/* Racks / Bays listing */}
      <div
        className={
          desktopViewMode === 'horizontal'
            ? 'flex flex-col gap-3 sm:gap-4 mt-2'
            : 'grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-5 mt-2'
        }
      >
        {baysList.map((bayNum) => {
          let bayCarpets = carpetsByBay.get(bayNum) || [];

          // Apply filters
          if (filterQuery.trim()) {
            const rawQ = filterQuery.trim().toLowerCase();
            const cleanQ = rawQ.replace(/\s+/g, '');

            const placeWithLetter = rawQ.match(/^(\d+)\s*[-/]?\s*([a-zA-Z])$/i);
            const isPureNum = /^\d+$/.test(cleanQ);

            if (placeWithLetter) {
              const targetBay = parseInt(placeWithLetter[1], 10);
              const targetSub = placeWithLetter[2].toUpperCase();
              if (bayNum !== targetBay) return null;
              bayCarpets = bayCarpets.filter((c) => (c.podmagazyn || '').trim().toUpperCase() === targetSub);
              if (bayCarpets.length === 0) return null;
            } else if (isPureNum) {
              const targetNum = parseInt(cleanQ, 10);
              const isExactBay = bayNum === targetNum;
              const hasMatchingCode = bayCarpets.some(
                (c) => c.ref?.trim().toLowerCase() === rawQ || c.ean?.replace(/\s+/g, '') === cleanQ
              );
              if (!isExactBay && !hasMatchingCode) {
                return null;
              }
            } else {
              const bayMatches = `magazyn ${bayNum}`.includes(rawQ) || `magazyn${bayNum}`.includes(cleanQ);
              bayCarpets = bayCarpets.filter((c) => {
                return (
                  bayMatches ||
                  c.nazwa?.toLowerCase().includes(rawQ) ||
                  c.ref?.toLowerCase().includes(rawQ) ||
                  c.ean?.toLowerCase().includes(cleanQ) ||
                  c.miejsce?.toLowerCase().includes(rawQ)
                );
              });
              if (!bayMatches && bayCarpets.length === 0) {
                return null;
              }
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
          const safeBayCarpets = bayCarpets || [];
          const carpetA = safeBayCarpets.find(
            (c) => String(c?.podmagazyn || '').trim().toUpperCase() === 'A'
          );
          const carpetB = safeBayCarpets.find(
            (c) => String(c?.podmagazyn || '').trim().toUpperCase() === 'B'
          );
          const otherCarpets = safeBayCarpets.filter((c) => {
            const p = String(c?.podmagazyn || '').trim().toUpperCase();
            return p !== 'A' && p !== 'B';
          });

          const theme = getBayTheme(bayNum);

          return (
            <div
              key={bayNum}
              id={`bay-card-${bayNum}`}
              className={`${theme.cardBg} border ${theme.cardBorder} rounded-2xl shadow-xs overflow-hidden flex flex-col transition-all hover:shadow-md scroll-mt-18`}
            >
              {/* Bay Header: "Magazyn X" with distinct color strip + badges + [Super Admin: Usuń Magazyn] */}
              <div
                className={`px-4 sm:px-5 ${
                  desktopViewMode === 'horizontal' ? 'py-2 sm:py-2.5' : 'py-3'
                } ${theme.headerBg} border-b ${theme.headerBorder} flex items-center justify-between gap-2`}
              >
                <div className="flex items-center gap-2 sm:gap-2.5 flex-wrap">
                  <div className={`w-2.5 h-6 rounded-full ${theme.accentBar}`} />
                  <h2 className={`text-lg sm:text-xl font-black ${theme.titleText} tracking-tight`}>
                    Magazyn {bayNum}
                  </h2>
                  {hasCarpets ? (
                    <span className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${theme.badge}`}>
                      {bayCarpets.length} {bayCarpets.length === 1 ? 'dywan' : 'dywany'}
                    </span>
                  ) : (
                    <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-gray-100 text-gray-500 border border-gray-200">
                      pusty
                    </span>
                  )}

                  {/* Visual Capacity Gauge */}
                  {bayCarpets.length >= 2 ? (
                    <span
                      className="hidden xs:inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-emerald-100/90 text-emerald-800 border border-emerald-200 text-[10px] font-bold tracking-tight shadow-2xs"
                      title="Regał w 100% zapełniony (oba ramiona A i B zajęte)"
                    >
                      <span className="flex items-center gap-0.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 inline-block" />
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 inline-block" />
                      </span>
                      <span>2/2 Pełny</span>
                    </span>
                  ) : bayCarpets.length === 1 ? (
                    <span
                      className="hidden xs:inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-amber-100/90 text-amber-900 border border-amber-300 text-[10px] font-bold tracking-tight shadow-2xs"
                      title={`Wolny slot ${carpetA ? 'B' : 'A'} do zagospodarowania`}
                    >
                      <span className="flex items-center gap-0.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-500 inline-block" />
                        <span className="w-1.5 h-1.5 rounded-full border border-amber-500 bg-white inline-block" />
                      </span>
                      <span>1/2 Wolny {carpetA ? 'B' : 'A'}</span>
                    </span>
                  ) : (
                    <span
                      className="hidden xs:inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-gray-100 text-gray-600 border border-gray-300 text-[10px] font-semibold tracking-tight shadow-2xs"
                      title="Regał całkowicie pusty (wolne A i B)"
                    >
                      <span className="flex items-center gap-0.5">
                        <span className="w-1.5 h-1.5 rounded-full border border-gray-400 bg-white inline-block" />
                        <span className="w-1.5 h-1.5 rounded-full border border-gray-400 bg-white inline-block" />
                      </span>
                      <span>0/2 Pusty</span>
                    </span>
                  )}
                </div>

                {isSuperAdmin && (
                  <button
                    onClick={() =>
                      setDeletingBayModal({
                        bayNum,
                        carpetsInBay: carpetsByBay.get(bayNum) || []
                      })
                    }
                    className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-white/80 rounded-xl transition-colors cursor-pointer"
                    title={`Usuń Magazyn ${bayNum} (tylko Super Admin)`}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>

              {/* Sub-bay slots: strictly 2 places per bay - Podmagazyn A and Podmagazyn B */}
              <div
                className={
                  desktopViewMode === 'horizontal'
                    ? 'p-3 sm:p-4 grid grid-cols-1 lg:grid-cols-2 gap-3 items-stretch'
                    : 'p-4 sm:p-5 flex flex-col gap-3'
                }
              >
                {/* Podmagazyn A */}
                {carpetA ? (
                  <SlotCard
                    key={`slot-${bayNum}-A-${carpetA.id}`}
                    carpet={carpetA}
                    theme={theme}
                    onEdit={() => onEditProduct(carpetA)}
                    onMove={onMoveProduct ? () => onMoveProduct(carpetA) : undefined}
                    onDelete={() => onDeleteProduct(carpetA.id, carpetA.nazwa)}
                    onGenerateFiszka={() => onGenerateFiszka(carpetA)}
                  />
                ) : (
                  <EmptySlotCard
                    key={`empty-${bayNum}-A`}
                    bay={bayNum}
                    subBay="A"
                    theme={theme}
                    onAdd={() => onAddProduct(bayNum, 'A')}
                  />
                )}

                {/* Podmagazyn B */}
                {carpetB ? (
                  <SlotCard
                    key={`slot-${bayNum}-B-${carpetB.id}`}
                    carpet={carpetB}
                    theme={theme}
                    onEdit={() => onEditProduct(carpetB)}
                    onMove={onMoveProduct ? () => onMoveProduct(carpetB) : undefined}
                    onDelete={() => onDeleteProduct(carpetB.id, carpetB.nazwa)}
                    onGenerateFiszka={() => onGenerateFiszka(carpetB)}
                  />
                ) : (
                  <EmptySlotCard
                    key={`empty-${bayNum}-B`}
                    bay={bayNum}
                    subBay="B"
                    theme={theme}
                    onAdd={() => onAddProduct(bayNum, 'B')}
                  />
                )}
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

      {/* Mobile Floating Action Button: Quick Barcode / Ref Scanner */}
      {onOpenScanner && (
        <button
          onClick={onOpenScanner}
          className="sm:hidden fixed bottom-5 right-4 z-30 flex items-center gap-2 px-4 py-3 rounded-full bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-black text-sm shadow-xl shadow-blue-600/35 active:scale-95 transition-all border-2 border-white cursor-pointer"
          title="Szybki skaner kodów kreskowych"
        >
          <Camera className="w-5 h-5 text-white" />
          <span>Skanuj</span>
        </button>
      )}

      {/* Floating Back to Top Button */}
      {showScrollTop && (
        <button
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          className={`fixed z-30 flex items-center justify-center rounded-full bg-gray-900/90 hover:bg-black text-white shadow-lg backdrop-blur-xs transition-all active:scale-95 cursor-pointer border border-gray-700 ${
            onOpenScanner ? 'bottom-20 right-4 sm:bottom-6 sm:right-6 w-10 h-10' : 'bottom-6 right-6 w-10 h-10'
          }`}
          title="Przewiń do góry"
          aria-label="Przewiń do góry"
        >
          <ArrowUp className="w-5 h-5" />
        </button>
      )}
    </div>
  );
}

// Slot card displaying carpet in sub-bay
function SlotCard({
  carpet,
  theme,
  onEdit,
  onMove,
  onDelete,
  onGenerateFiszka
}: {
  carpet: Carpet;
  theme?: BayTheme;
  onEdit: () => void;
  onMove?: () => void;
  onDelete: () => void;
  onGenerateFiszka: () => void;
}) {
  const [copiedEan, setCopiedEan] = useState(false);
  const [copiedRef, setCopiedRef] = useState(false);

  const handleCopy = (text: string, isEan: boolean) => {
    navigator.clipboard.writeText(text);
    Haptics.copy();
    if (isEan) {
      setCopiedEan(true);
      setTimeout(() => setCopiedEan(false), 1500);
    } else {
      setCopiedRef(true);
      setTimeout(() => setCopiedRef(false), 1500);
    }
  };

  return (
    <div className={`border border-gray-200 bg-white rounded-xl p-3.5 flex flex-col gap-2.5 shadow-2xs ${theme?.slotHoverBorder || 'hover:border-blue-300'} transition-colors`}>
      <div className="flex items-center justify-between border-b border-gray-100 pb-2">
        <span className={`font-extrabold text-xs uppercase tracking-wide px-2 py-0.5 rounded-md ${theme?.subBayTag || 'text-blue-700 bg-blue-50 border border-blue-200'}`}>
          Podmagazyn {carpet.podmagazyn} ({carpet.miejsce || `${carpet.magazyn} ${carpet.podmagazyn}`})
        </span>

        {carpet.dodano && (
          <span className="text-[11px] text-gray-400 flex items-center gap-1">
            <Calendar className="w-3 h-3" />
            {carpet.dodano}
          </span>
        )}
      </div>

      <div className="pt-0.5">
        <span className="text-[10px] font-black uppercase tracking-wider text-gray-400 block mb-0.5">
          NAZWA DYWANU
        </span>
        <a
          href={getLeroyMerlinSearchUrl(carpet)}
          target="_blank"
          rel="noopener noreferrer"
          className="group inline-flex items-center gap-1.5 text-base sm:text-lg font-black text-gray-900 hover:text-blue-600 transition-colors cursor-pointer leading-snug"
          title={`Otwórz w Leroy Merlin (REF: ${carpet.ref || carpet.nazwa})`}
        >
          <span className="group-hover:underline">{carpet.nazwa}</span>
          <ExternalLink className="w-3.5 h-3.5 text-gray-400 group-hover:text-blue-600 shrink-0 transition-colors" />
        </a>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
        <div
          onClick={() => handleCopy(carpet.ref, false)}
          className="bg-gray-50 hover:bg-gray-100 p-2.5 rounded-xl cursor-pointer flex items-center justify-between transition-colors border border-gray-200 shadow-2xs"
          title="Kliknij, aby skopiować REF"
        >
          <div>
            <span className="text-[11px] text-gray-500 font-black block uppercase tracking-wider">REF LM</span>
            <span className="font-mono font-black text-base sm:text-lg text-gray-900 tracking-wide">{carpet.ref}</span>
          </div>
          {copiedRef ? <Check className="w-4 h-4 text-emerald-600 shrink-0" /> : <Copy className="w-4 h-4 text-gray-400 shrink-0" />}
        </div>

        <div
          onClick={() => handleCopy(carpet.ean, true)}
          className="bg-gray-50 hover:bg-gray-100 p-2.5 rounded-xl cursor-pointer flex items-center justify-between transition-colors border border-gray-200 shadow-2xs"
          title="Kliknij, aby skopiować EAN"
        >
          <div>
            <span className="text-[11px] text-gray-500 font-black block uppercase tracking-wider">KOD EAN</span>
            <span className="font-mono font-black text-base sm:text-lg text-gray-900 tracking-wider truncate block max-w-[170px] sm:max-w-none">
              {carpet.ean}
            </span>
          </div>
          {copiedEan ? <Check className="w-4 h-4 text-emerald-600 shrink-0" /> : <Copy className="w-4 h-4 text-gray-400 shrink-0" />}
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

      <div className={`grid ${onMove ? 'grid-cols-2 sm:grid-cols-4' : 'grid-cols-3'} gap-1.5 pt-1`}>
        <button
          onClick={onEdit}
          className="flex items-center justify-center gap-1.5 py-2 px-2.5 rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs shadow-2xs active:scale-95 transition-all cursor-pointer"
        >
          <Pencil className="w-3.5 h-3.5 text-gray-600" />
          Edytuj
        </button>

        {onMove && (
          <button
            onClick={onMove}
            className="flex items-center justify-center gap-1.5 py-2 px-2.5 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-indigo-700 border border-indigo-200 font-bold text-xs shadow-2xs active:scale-95 transition-all cursor-pointer"
            title="Przenieś na inny regał lub zamień miejscami (Swap)"
          >
            <ArrowLeftRight className="w-3.5 h-3.5" />
            Przenieś
          </button>
        )}

        <button
          onClick={onGenerateFiszka}
          className="flex items-center justify-center gap-1.5 py-2 px-2.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-2xs active:scale-95 transition-all cursor-pointer"
        >
          <FileText className="w-3.5 h-3.5" />
          Fiszka
        </button>

        <button
          onClick={onDelete}
          className="flex items-center justify-center gap-1.5 py-2 px-2.5 rounded-lg bg-[#ef4444] hover:bg-[#dc2626] text-white font-bold text-xs shadow-2xs active:scale-95 transition-all cursor-pointer"
        >
          <Trash2 className="w-3.5 h-3.5" />
          Usuń
        </button>
      </div>
    </div>
  );
}

// Empty slot card
function EmptySlotCard({
  bay,
  subBay,
  theme,
  onAdd
}: {
  bay: number;
  subBay: string;
  theme?: BayTheme;
  onAdd: () => void;
}) {
  return (
    <div
      className={`border-2 border-dashed border-gray-300 bg-white/80 ${
        theme?.emptyHoverBg || 'hover:bg-blue-50/20'
      } rounded-xl p-4 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs sm:text-sm transition-all h-full min-h-[120px]`}
    >
      <div className="flex items-center gap-2 flex-wrap text-center sm:text-left">
        <span className="font-extrabold text-xs text-gray-700 bg-gray-100 px-2.5 py-1 rounded-md border border-gray-200">
          Podmagazyn {subBay}
        </span>
        <span className="text-gray-400 italic text-xs">
          Wolne ramię ekspozytora ({bay} {subBay})
        </span>
      </div>
      <button
        onClick={onAdd}
        className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-xs sm:text-sm shadow-xs transition-transform active:scale-95 cursor-pointer shrink-0"
      >
        <Plus className="w-3.5 h-3.5" />
        <span>Przypisz dywan</span>
      </button>
    </div>
  );
}
