import { useState, useEffect } from 'react';
import { Carpet } from '../types';
import { X, Save, PackagePlus, AlertCircle } from 'lucide-react';

interface EditCarpetModalProps {
  isOpen: boolean;
  onClose: () => void;
  carpetToEdit: Partial<Carpet> | null;
  onSave: (carpetData: Partial<Carpet>) => Promise<void>;
  existingMagazyny: number[];
}

export function EditCarpetModal({
  isOpen,
  onClose,
  carpetToEdit,
  onSave,
  existingMagazyny
}: EditCarpetModalProps) {
  const [magazyn, setMagazyn] = useState<number>(1);
  const [podmagazyn, setPodmagazyn] = useState<string>('A');
  const [nazwa, setNazwa] = useState<string>('');
  const [ref, setRef] = useState<string>('');
  const [ean, setEan] = useState<string>('');
  const [dodano, setDodano] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
  }, [carpetToEdit, isOpen]);

  if (!isOpen) return null;

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
            className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-200 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form body */}
        <form onSubmit={handleSubmit} className="p-5 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
              <span>{error}</span>
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
                <option value="C">Podmagazyn C</option>
                <option value="D">Podmagazyn D</option>
              </select>
            </div>

            <div className="col-span-2 text-center text-xs font-extrabold text-amber-800 pt-1">
              Symbol miejsca: <span className="font-mono text-base ml-1">{magazyn} {podmagazyn}</span>
            </div>
          </div>

          {/* Nazwa dywanu */}
          <div>
            <label className="text-xs font-bold text-gray-700 block mb-1">
              Nazwa dywanu i wymiary *
            </label>
            <input
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
              <label className="text-xs font-bold text-gray-700 block mb-1">
                REF. LM (numer referencyjny)
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
              <label className="text-xs font-bold text-gray-700 block mb-1">
                Kod kreskowy EAN *
              </label>
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
              disabled={loading}
              className="px-4 py-2 text-xs sm:text-sm font-semibold text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
            >
              Anuluj
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex items-center gap-1.5 px-5 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-lg text-xs sm:text-sm font-bold shadow-xs transition-colors"
            >
              <Save className="w-4 h-4" />
              {loading ? 'Zapisywanie...' : isEditing ? 'Zapisz zmiany' : 'Dodaj produkt'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
