import { useState, useEffect, useRef } from 'react';
import { Carpet } from '../types';
import {
  X,
  Save,
  PackagePlus,
  AlertCircle,
  Sparkles,
  Camera,
  Upload,
  CheckCircle2,
  Loader2,
  Tag,
  Barcode,
  Keyboard,
  Info,
  ScanText
} from 'lucide-react';
import { EslScannerModal } from './EslScannerModal';
import { BarcodeScannerModal } from './BarcodeScannerModal';
import { scanEslLabelWithoutAi, LocalOcrResult } from '../services/localOcrService';
import { initialCarpets } from '../data/seedData';

interface EditCarpetModalProps {
  isOpen: boolean;
  onClose: () => void;
  carpetToEdit: Partial<Carpet> | null;
  onSave: (carpetData: Partial<Carpet>) => Promise<void>;
  existingMagazyny: number[];
  existingCarpets?: Carpet[];
}

export function EditCarpetModal({
  isOpen,
  onClose,
  carpetToEdit,
  onSave,
  existingMagazyny,
  existingCarpets = []
}: EditCarpetModalProps) {
  const [magazyn, setMagazyn] = useState<number>(1);
  const [podmagazyn, setPodmagazyn] = useState<string>('A');
  const [nazwa, setNazwa] = useState<string>('');
  const [ref, setRef] = useState<string>('');
  const [ean, setEan] = useState<string>('');
  const [dodano, setDodano] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // ESL OCR State (100% Local non-AI)
  const [isEslScannerOpen, setIsEslScannerOpen] = useState(false);
  const [isBarcodeScannerOpen, setIsBarcodeScannerOpen] = useState(false);
  const [quickScanning, setQuickScanning] = useState(false);
  const [ocrSuccessNotice, setOcrSuccessNotice] = useState<string | null>(null);
  const quickGalleryInputRef = useRef<HTMLInputElement | null>(null);
  const quickCameraInputRef = useRef<HTMLInputElement | null>(null);
  const nazwaInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (carpetToEdit) {
      setMagazyn(carpetToEdit.magazyn || 1);
      setPodmagazyn(carpetToEdit.podmagazyn || 'A');
      setNazwa(carpetToEdit.nazwa || '');
      setRef(carpetToEdit.ref || '');
      setEan(carpetToEdit.ean || '');
      setDodano(carpetToEdit.dodano || new Date().toISOString().split('T')[0]);
    } else {
      setMagazyn(1);
      setPodmagazyn('A');
      setNazwa('');
      setRef('');
      setEan('');
      setDodano(new Date().toISOString().split('T')[0]);
    }
    setError(null);
    setOcrSuccessNotice(null);
  }, [carpetToEdit, isOpen]);

  if (!isOpen) return null;

  // Apply OCR scan result from modal or direct photo
  const handleApplyEslData = (data: LocalOcrResult) => {
    if (data.nazwa) setNazwa(data.nazwa);
    if (data.ref) setRef(data.ref);
    if (data.ean) setEan(data.ean);

    setError(null);
    setOcrSuccessNotice(
      `Odczytano z cenówki: ${data.nazwa || data.ean || ''} (REF: ${data.ref || '-'}, EAN: ${data.ean || '-'})`
    );
    setTimeout(() => {
      setOcrSuccessNotice(null);
    }, 6000);
  };

  // Direct quick file/camera snapshot using local non-AI OCR
  const handleQuickFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setQuickScanning(true);
      setError(null);
      const reader = new FileReader();
      const dataUrl = await new Promise<string>((resolve, reject) => {
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });
      const result = await scanEslLabelWithoutAi(dataUrl);
      handleApplyEslData(result);
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? `Błąd odczytu etykiety: ${err.message}`
          : 'Nie udało się odczytać etykiety ze zdjęcia'
      );
    } finally {
      setQuickScanning(false);
      if (quickGalleryInputRef.current) {
        quickGalleryInputRef.current.value = '';
      }
      if (quickCameraInputRef.current) {
        quickCameraInputRef.current.value = '';
      }
    }
  };

  const handleBarcodeScanned = (scannedCode: string) => {
    const code = scannedCode.trim();
    if (code.length === 13) {
      setEan(code);
    } else if (code.length === 8) {
      setRef(code);
    } else {
      setEan(code);
    }

    // Auto-match if this product code exists in the store catalog (100% exact)
    const allCarpets = [...existingCarpets, ...initialCarpets];
    const match = allCarpets.find(
      (c) => c.ean === code || c.ref === code || (code.length === 8 && c.ref === code)
    );
    if (match) {
      if (!nazwa) setNazwa(match.nazwa);
      if (!ref && match.ref) setRef(match.ref);
      if (!ean && match.ean) setEan(match.ean);
      setOcrSuccessNotice(`Rozpoznano dywan ze sklepu: ${match.nazwa} (REF: ${match.ref})`);
      setTimeout(() => setOcrSuccessNotice(null), 5000);
      setError(null);
      return;
    }

    setError(null);
    setOcrSuccessNotice(`Zeskanowano kod kreskowy: ${code}`);
    setTimeout(() => setOcrSuccessNotice(null), 4000);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!nazwa.trim()) {
      setError('Wpisz nazwę dywanu');
      return;
    }

    if (!ean.trim()) {
      setError('Wpisz kod kreskowy EAN');
      return;
    }

    const miejsce = `${magazyn} ${podmagazyn.trim().toUpperCase()}`;

    try {
      setLoading(true);
      await onSave({
        ...(carpetToEdit?.id ? { id: carpetToEdit.id } : {}),
        magazyn,
        podmagazyn: podmagazyn.trim().toUpperCase(),
        miejsce,
        nazwa: nazwa.trim(),
        ref: ref.trim(),
        ean: ean.trim(),
        dodano: dodano || new Date().toISOString().split('T')[0]
      });
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Wystąpił błąd podczas zapisywania');
    } finally {
      setLoading(false);
    }
  };

  const isEditing = Boolean(carpetToEdit?.id);

  return (
    <>
      <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
        <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
          {/* Header */}
          <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-gray-50">
            <div className="flex items-center gap-2 font-bold text-gray-800 text-sm sm:text-base">
              <PackagePlus className="w-5 h-5 text-blue-600" />
              {isEditing ? 'Edytuj pozycję dywanu' : 'Dodaj produkt do magazynu'}
            </div>
            <button
              onClick={onClose}
              className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-200 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form body */}
          <form onSubmit={handleSubmit} className="p-4 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-4">
            {/* Error Notice */}
            {error && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-start justify-between gap-2">
                <div className="flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0 text-red-600 mt-0.5" />
                  <span>{error}</span>
                </div>
                <button
                  type="button"
                  onClick={() => setError(null)}
                  className="text-red-500 hover:text-red-800 font-bold px-1"
                >
                  ✕
                </button>
              </div>
            )}

            {/* ESL Scanner Action Banner (100% Local non-AI) */}
            <div className="bg-linear-to-r from-emerald-50 via-teal-50 to-blue-50 border border-emerald-200 rounded-2xl p-3.5 flex flex-col gap-2.5 shadow-2xs">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-start gap-2">
                  <div className="w-7 h-7 rounded-lg bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-2xs">
                    <ScanText className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="font-extrabold text-xs text-emerald-950 block">
                      Skanuj cenówkę ESL (Lokalny odczyt bez AI)
                    </span>
                    <span className="text-[11px] text-emerald-800 leading-tight block">
                      Odczytuje kod kreskowy na żywo oraz nazwę i REF LM ze zdjęcia bez limitów.
                    </span>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-1.5">
                <button
                  type="button"
                  onClick={() => setIsEslScannerOpen(true)}
                  disabled={quickScanning || loading}
                  className="py-2.5 px-2 bg-emerald-600 hover:bg-emerald-700 active:scale-98 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1 shadow-xs transition-all cursor-pointer truncate"
                  title="Otwórz skaner na żywo z podglądem kamery"
                >
                  <Camera className="w-3.5 h-3.5 shrink-0" />
                  <span className="truncate">Skaner aparatem</span>
                </button>

                <button
                  type="button"
                  onClick={() => quickGalleryInputRef.current?.click()}
                  disabled={quickScanning || loading}
                  className="py-2 px-2 bg-white border border-emerald-300 hover:border-emerald-500 hover:bg-emerald-50/60 text-emerald-900 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition-all cursor-pointer shadow-2xs truncate"
                  title="Wybierz zapisane zdjęcie z galerii"
                >
                  {quickScanning ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin text-emerald-600 shrink-0" />
                  ) : (
                    <Upload className="w-3.5 h-3.5 text-blue-600 shrink-0" />
                  )}
                  <span className="truncate">Z galerii</span>
                </button>

                <button
                  type="button"
                  onClick={() => quickCameraInputRef.current?.click()}
                  disabled={quickScanning || loading}
                  className="py-2 px-2 bg-white border border-emerald-300 hover:border-emerald-500 hover:bg-emerald-50/60 text-emerald-900 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition-all cursor-pointer shadow-2xs truncate"
                  title="Zrób zdjęcie aparatem telefonu"
                >
                  <Camera className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                  <span className="truncate">Zrób zdjęcie</span>
                </button>

                {/* 1. Pure gallery input (NO capture attribute = opens photo gallery / file manager) */}
                <input
                  ref={quickGalleryInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleQuickFileChange}
                  className="hidden"
                />

                {/* 2. Direct camera input (with capture="environment" = opens native camera directly) */}
                <input
                  ref={quickCameraInputRef}
                  type="file"
                  accept="image/*"
                  capture="environment"
                  onChange={handleQuickFileChange}
                  className="hidden"
                />
              </div>
            </div>

            {/* OCR Success Banner */}
            {ocrSuccessNotice && (
              <div className="p-3 bg-emerald-50 border border-emerald-300 rounded-xl text-xs text-emerald-900 flex items-start gap-2 animate-in fade-in">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <div className="flex-1 font-semibold">
                  {ocrSuccessNotice}
                </div>
              </div>
            )}

            {isEditing && (carpetToEdit?.updatedBy || carpetToEdit?.updatedAt) && (
              <div className="p-2.5 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-900 flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 truncate">
                  <span className="text-gray-500">Zmieniono przez:</span>
                  <strong className="font-bold truncate text-blue-950">
                    {carpetToEdit.updatedBy || 'Brak danych'}
                  </strong>
                </div>
                {carpetToEdit.updatedAt && (
                  <span className="text-[11px] text-blue-700 shrink-0 font-medium">
                    {new Date(carpetToEdit.updatedAt).toLocaleDateString('pl-PL')}{' '}
                    {new Date(carpetToEdit.updatedAt).toLocaleTimeString('pl-PL', {
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </span>
                )}
              </div>
            )}

            {/* Location row: Magazyn & Podmagazyn */}
            <div className="grid grid-cols-2 gap-3 bg-amber-50/70 p-3.5 rounded-xl border border-amber-200">
              <div>
                <label className="text-xs font-bold text-amber-900 block mb-1">
                  Numer Magazynu (Regał)
                </label>
                <input
                  type="number"
                  min={1}
                  max={999}
                  value={magazyn}
                  onChange={(e) => setMagazyn(Number(e.target.value) || 1)}
                  className="w-full px-3 py-2 bg-white border border-amber-300 rounded-lg text-sm font-bold text-gray-900 focus:ring-2 focus:ring-amber-500 focus:outline-hidden"
                  required
                />
              </div>

              <div>
                <label className="text-xs font-bold text-amber-900 block mb-1">
                  Podmagazyn (Slot)
                </label>
                <select
                  value={podmagazyn}
                  onChange={(e) => setPodmagazyn(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-amber-300 rounded-lg text-sm font-bold text-gray-900 focus:ring-2 focus:ring-amber-500 focus:outline-hidden"
                >
                  <option value="A">Podmagazyn A</option>
                  <option value="B">Podmagazyn B</option>
                </select>
              </div>

              <div className="col-span-2 text-center text-xs font-extrabold text-amber-800 pt-1">
                Symbol miejsca: <span className="font-mono text-base ml-1">{magazyn} {podmagazyn}</span>
              </div>
            </div>

            {/* Nazwa dywanu */}
            <div>
              <label className="text-xs font-bold text-gray-700 block mb-1 flex items-center justify-between">
                <span>Nazwa dywanu i wymiary *</span>
                <span className="text-[11px] text-gray-400 font-normal">Z etykiety cenowej</span>
              </label>
              <input
                ref={nazwaInputRef}
                type="text"
                value={nazwa}
                onChange={(e) => setNazwa(e.target.value)}
                placeholder="np. DYWAN LAGOON BALTA 130X230 MORSKI"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
                required
              />
            </div>

            {/* REF & EAN */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="text-xs font-bold text-gray-700 block mb-1 flex items-center justify-between">
                  <span>REF. LM (numer ref)</span>
                  <span className="text-[10px] text-gray-400 font-mono">8 cyfr</span>
                </label>
                <input
                  type="text"
                  value={ref}
                  onChange={(e) => setRef(e.target.value)}
                  placeholder="np. 92116369"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-bold text-gray-700">
                    Kod kreskowy EAN *
                  </label>
                  <button
                    type="button"
                    onClick={() => setIsBarcodeScannerOpen(true)}
                    className="text-[11px] text-blue-600 hover:text-blue-800 font-bold flex items-center gap-1 cursor-pointer bg-blue-50 hover:bg-blue-100 px-2 py-0.5 rounded-md transition-colors"
                    title="Uruchom skaner kodów aparatem"
                  >
                    <Barcode className="w-3.5 h-3.5" />
                    <span>Skanuj aparat</span>
                  </button>
                </div>
                <input
                  type="text"
                  value={ean}
                  onChange={(e) => setEan(e.target.value)}
                  placeholder="np. 3276007888454"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
                  required
                />
              </div>
            </div>

            {/* Data dodania */}
            <div>
              <label className="text-xs font-bold text-gray-700 block mb-1">
                Data dodania
              </label>
              <input
                type="date"
                value={dodano}
                onChange={(e) => setDodano(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-hidden"
              />
            </div>

            {/* Buttons */}
            <div className="pt-3 border-t border-gray-200 flex justify-end gap-2.5">
              <button
                type="button"
                onClick={onClose}
                disabled={loading || quickScanning}
                className="px-4 py-2 text-xs sm:text-sm font-semibold text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
              >
                Anuluj
              </button>
              <button
                type="submit"
                disabled={loading || quickScanning}
                className="flex items-center gap-1.5 px-5 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-lg text-xs sm:text-sm font-bold shadow-xs transition-colors cursor-pointer"
              >
                <Save className="w-4 h-4" />
                {loading ? 'Zapisywanie...' : isEditing ? 'Zapisz zmiany' : 'Dodaj produkt'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* ESL Scanner Modal */}
      <EslScannerModal
        isOpen={isEslScannerOpen}
        onClose={() => setIsEslScannerOpen(false)}
        onApplyData={handleApplyEslData}
        existingCarpets={existingCarpets}
      />

      {/* Direct EAN Barcode Scanner Modal (Zero AI / Zero Quota) */}
      <BarcodeScannerModal
        isOpen={isBarcodeScannerOpen}
        onClose={() => setIsBarcodeScannerOpen(false)}
        onScanSuccess={handleBarcodeScanned}
        title="Zeskanuj kod kreskowy EAN"
      />
    </>
  );
}
