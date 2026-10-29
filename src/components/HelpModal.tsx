import { X, HelpCircle, Smartphone, Search, Database, ShieldCheck, Printer, RefreshCw } from 'lucide-react';
import { STORE_NUMBER, STORE_CITY, SUPER_ADMIN_EMAIL } from '../types';

interface HelpModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function HelpModal({ isOpen, onClose }: HelpModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
      <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-gray-50">
          <div className="flex items-center gap-2 font-bold text-gray-800 text-sm sm:text-base">
            <HelpCircle className="w-5 h-5 text-blue-600" />
            Pomoc i instrukcja obsługi • Sklep {STORE_NUMBER} {STORE_CITY}
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-200 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-4 text-xs sm:text-sm text-gray-700 leading-relaxed">
          <div className="flex items-start gap-3 bg-blue-50/70 p-3 rounded-xl border border-blue-100">
            <Search className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-gray-900 block">Szybkie wyszukiwanie bez logowania</span>
              Każdy pracownik lub doradca klienta w sklepie 038 może bez logowania wyszukać dywan po:
              <ul className="list-disc ml-4 mt-1 space-y-0.5 text-gray-600">
                <li>Numerze miejsca (np. <span className="font-mono font-bold text-gray-800">1A</span>, <span className="font-mono font-bold text-gray-800">20A</span>)</li>
                <li>Kodzie kreskowym EAN (np. <span className="font-mono font-bold text-gray-800">3276007888454</span>)</li>
                <li>Numerze referencyjnym REF (np. <span className="font-mono font-bold text-gray-800">92116369</span>)</li>
                <li>Nazwie lub kolekcji (np. <span className="font-semibold text-gray-800">Lagoon, Jimma, Kashmir</span>)</li>
              </ul>
            </div>
          </div>

          <div className="flex items-start gap-3 bg-emerald-50/70 p-3 rounded-xl border border-emerald-100">
            <Smartphone className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-gray-900 block">Skaner kodów EAN w aparacie</span>
              Kliknij przycisk <span className="font-bold text-emerald-800">📷 Skanuj</span> na telefonie lub terminalu, aby skierować aparat na kod kreskowy dywanu i natychmiast sprawdzić jego przypisane miejsce ekspozycyjne na regale.
            </div>
          </div>

          <div className="flex items-start gap-3 bg-amber-50/70 p-3 rounded-xl border border-amber-100">
            <Printer className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-gray-900 block">Drukowanie fiszki ekspozycyjnej</span>
              Przycisk <span className="font-bold text-amber-800">📝 Generuj fiszkę</span> tworzy profesjonalną zawieszkę z dużym oznaczeniem miejsca, nazwą, kodem kreskowym i numerem LM gotową do wydruku na drukarce etykiet lub papierze A4.
            </div>
          </div>

          <div className="flex items-start gap-3 bg-purple-50/70 p-3 rounded-xl border border-purple-100">
            <ShieldCheck className="w-5 h-5 text-purple-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-gray-900 block">Zarządzanie magazynem i uprawnienia</span>
              Edycja danych (dodawanie dywanów, edycja, usuwanie, import z pliku) wymaga zalogowania kontem Google.
              <p className="mt-1">
                Głównym Super Administratorem jest <span className="font-mono font-bold text-purple-900">{SUPER_ADMIN_EMAIL}</span>. Super Admin może autoryzować adresy Google innych pracowników sklepu.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3 bg-cyan-50/70 p-3 rounded-xl border border-cyan-100">
            <RefreshCw className="w-5 h-5 text-cyan-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold text-gray-900 block">Synchronizacja w czasie rzeczywistym</span>
              Baza jest na żywo połączona z chmurą sklepu 038. Każda zmiana wprowadzona na jednym telefonie natychmiast wyświetla się wszystkim innym użytkownikom bez potrzeby odświeżania strony.
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t border-gray-200 bg-gray-50 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-800 hover:bg-gray-900 text-white rounded-lg text-xs sm:text-sm font-semibold transition-colors"
          >
            Rozumiem
          </button>
        </div>
      </div>
    </div>
  );
}
