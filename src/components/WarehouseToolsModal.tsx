import {
  X,
  Upload,
  BarChart2,
  Download,
  FileText,
  History,
  HelpCircle,
  LogOut,
  Plus,
  Layers,
  Users,
  ShieldCheck,
  Package
} from 'lucide-react';
import { STORE_NUMBER, SUPER_ADMIN_EMAIL } from '../types';

interface WarehouseToolsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onOpenImport: () => void;
  onOpenStockExpo: () => void;
  onExportJSON: () => void;
  onExportXLSX: () => void;
  onOpenActivityLogs: () => void;
  onOpenHelp: () => void;
  onLogout: () => void;
  isSuperAdmin: boolean;
  onOpenQuickAdd: () => void;
  onOpenSuperAdminModal: (tab: 'admins' | 'warehouses') => void;
  totalBays: number;
  totalCarpets: number;
}

export function WarehouseToolsModal({
  isOpen,
  onClose,
  onOpenImport,
  onOpenStockExpo,
  onExportJSON,
  onExportXLSX,
  onOpenActivityLogs,
  onOpenHelp,
  onLogout,
  isSuperAdmin,
  onOpenQuickAdd,
  onOpenSuperAdminModal,
  totalBays,
  totalCarpets
}: WarehouseToolsModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-end sm:items-center justify-center p-0 sm:p-4 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden flex flex-col max-h-[85vh] animate-in slide-in-from-bottom-6 sm:zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-gray-50">
          <div>
            <h3 className="font-bold text-gray-900 text-sm sm:text-base flex items-center gap-2">
              <Package className="w-5 h-5 text-blue-600" />
              Narzędzia i akcje magazynu
            </h3>
            <span className="text-[11px] text-gray-500 font-medium">
              Sklep {STORE_NUMBER} • {totalBays} regałów • {totalCarpets} {totalCarpets === 1 ? 'dywan' : 'dywanów'}
            </span>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-200 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content list */}
        <div className="p-4 sm:p-5 overflow-y-auto flex-1 flex flex-col gap-4 text-xs sm:text-sm">
          {/* Dane i raporty */}
          <div>
            <div className="text-[10px] font-extrabold text-gray-400 uppercase tracking-wider mb-2">
              Dane i stany magazynowe
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <button
                onClick={() => {
                  onClose();
                  onOpenStockExpo();
                }}
                className="flex items-center gap-3 p-3 rounded-xl bg-amber-50 hover:bg-amber-100/80 text-amber-950 font-bold border border-amber-200/70 transition-all text-left cursor-pointer active:scale-98"
              >
                <div className="w-8 h-8 rounded-lg bg-amber-200 text-amber-900 flex items-center justify-center shrink-0">
                  <BarChart2 className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs sm:text-sm font-bold">Stock expo</div>
                  <div className="text-[10px] text-amber-800 font-normal">Zajętość i wolne miejsca</div>
                </div>
              </button>

              <button
                onClick={() => {
                  onClose();
                  onOpenActivityLogs();
                }}
                className="flex items-center gap-3 p-3 rounded-xl bg-purple-50 hover:bg-purple-100/80 text-purple-950 font-bold border border-purple-200/70 transition-all text-left cursor-pointer active:scale-98"
              >
                <div className="w-8 h-8 rounded-lg bg-purple-200 text-purple-900 flex items-center justify-center shrink-0">
                  <History className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs sm:text-sm font-bold">Historia zmian</div>
                  <div className="text-[10px] text-purple-800 font-normal">Kto dodał, zmienił, zdjął</div>
                </div>
              </button>

              <button
                onClick={() => {
                  onClose();
                  onOpenImport();
                }}
                className="flex items-center gap-3 p-3 rounded-xl bg-blue-50 hover:bg-blue-100/80 text-blue-950 font-bold border border-blue-200/70 transition-all text-left cursor-pointer active:scale-98"
              >
                <div className="w-8 h-8 rounded-lg bg-blue-200 text-blue-900 flex items-center justify-center shrink-0">
                  <Upload className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs sm:text-sm font-bold">Importuj bazę</div>
                  <div className="text-[10px] text-blue-800 font-normal">Wgraj z pliku Excel/JSON</div>
                </div>
              </button>

              <button
                onClick={() => {
                  onClose();
                  onExportXLSX();
                }}
                className="flex items-center gap-3 p-3 rounded-xl bg-emerald-50 hover:bg-emerald-100/80 text-emerald-950 font-bold border border-emerald-200/70 transition-all text-left cursor-pointer active:scale-98"
              >
                <div className="w-8 h-8 rounded-lg bg-emerald-200 text-emerald-900 flex items-center justify-center shrink-0">
                  <FileText className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs sm:text-sm font-bold">Eksport XLSX</div>
                  <div className="text-[10px] text-emerald-800 font-normal">Pobierz arkusz Excel</div>
                </div>
              </button>
            </div>
          </div>

          {/* Kopia zapasowa */}
          <div className="pt-1">
            <button
              onClick={() => {
                onClose();
                onExportJSON();
              }}
              className="w-full flex items-center justify-between p-2.5 rounded-xl bg-gray-50 hover:bg-gray-100 text-gray-800 font-medium border border-gray-200 transition-colors text-xs cursor-pointer"
            >
              <div className="flex items-center gap-2">
                <Download className="w-4 h-4 text-gray-500" />
                <span>Pobierz pełną kopię zapasową (JSON)</span>
              </div>
              <span className="text-[10px] text-gray-400 font-mono">backup.json</span>
            </button>
          </div>

          {/* Super Admin section */}
          {isSuperAdmin && (
            <div className="pt-2 border-t border-gray-100">
              <div className="text-[10px] font-extrabold text-purple-600 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5" />
                Opcje Super Administratora
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                <button
                  onClick={() => {
                    onClose();
                    onOpenQuickAdd();
                  }}
                  className="p-2.5 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-900 font-bold text-xs flex items-center justify-center gap-1.5 border border-purple-200 transition-colors cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Dodaj magazyn
                </button>

                <button
                  onClick={() => {
                    onClose();
                    onOpenSuperAdminModal('warehouses');
                  }}
                  className="p-2.5 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-900 font-bold text-xs flex items-center justify-center gap-1.5 border border-purple-200 transition-colors cursor-pointer"
                >
                  <Layers className="w-3.5 h-3.5" />
                  Lista magazynów
                </button>

                <button
                  onClick={() => {
                    onClose();
                    onOpenSuperAdminModal('admins');
                  }}
                  className="p-2.5 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-900 font-bold text-xs flex items-center justify-center gap-1.5 border border-purple-200 transition-colors cursor-pointer"
                >
                  <Users className="w-3.5 h-3.5" />
                  Admini sklepu
                </button>
              </div>
            </div>
          )}

          {/* Pomoc i Wylogowanie */}
          <div className="pt-2 border-t border-gray-100 grid grid-cols-2 gap-2">
            <button
              onClick={() => {
                onClose();
                onOpenHelp();
              }}
              className="py-2.5 px-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
            >
              <HelpCircle className="w-4 h-4 text-blue-600" />
              <span>Instrukcja i pomoc</span>
            </button>

            <button
              onClick={() => {
                onClose();
                onLogout();
              }}
              className="py-2.5 px-3 rounded-xl bg-red-50 hover:bg-red-100 text-red-700 font-bold text-xs flex items-center justify-center gap-1.5 border border-red-200 transition-colors cursor-pointer"
            >
              <LogOut className="w-4 h-4" />
              <span>Wyloguj</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
