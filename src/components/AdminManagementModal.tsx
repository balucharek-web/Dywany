import { useState } from 'react';
import { AdminUser, Carpet } from '../types';
import {
  X,
  UserPlus,
  Trash2,
  ShieldCheck,
  Mail,
  AlertCircle,
  Check,
  Layers,
  Plus,
  Search,
  RotateCcw,
  AlertTriangle,
  Crown,
  Shield,
  ArrowRightLeft,
  ChevronDown
} from 'lucide-react';

interface AdminManagementModalProps {
  isOpen: boolean;
  onClose: () => void;
  admins: AdminUser[];
  onAddAdmin: (email: string, role: 'admin' | 'superadmin') => Promise<void>;
  onUpdateAdminRole: (email: string, role: 'admin' | 'superadmin') => Promise<void>;
  onRemoveAdmin: (email: string) => Promise<void>;
  onTransferSuperAdmin: (targetEmail: string, removeSelf: boolean) => Promise<void>;
  currentUserEmail?: string;
  activeBays: number[];
  carpets: Carpet[];
  onAddWarehouses: (numbers: number[]) => Promise<void>;
  onDeleteWarehouse: (bayNumber: number, deleteCarpets: boolean) => Promise<void>;
  onInitializeWarehouses: (count: number) => Promise<void>;
  initialTab?: 'admins' | 'warehouses';
}

export function AdminManagementModal({
  isOpen,
  onClose,
  admins,
  onAddAdmin,
  onUpdateAdminRole,
  onRemoveAdmin,
  onTransferSuperAdmin,
  currentUserEmail,
  activeBays,
  carpets,
  onAddWarehouses,
  onDeleteWarehouse,
  onInitializeWarehouses,
  initialTab = 'admins'
}: AdminManagementModalProps) {
  const [activeTab, setActiveTab] = useState<'admins' | 'warehouses'>(initialTab);

  // Admins state
  const [newEmail, setNewEmail] = useState('');
  const [newRole, setNewRole] = useState<'admin' | 'superadmin'>('admin');
  const [loadingAdmin, setLoadingAdmin] = useState(false);
  const [adminError, setAdminError] = useState<string | null>(null);
  const [adminSuccess, setAdminSuccess] = useState<string | null>(null);

  // Handover state
  const [isHandoverOpen, setIsHandoverOpen] = useState(false);
  const [handoverTarget, setHandoverTarget] = useState('');
  const [handoverRemoveSelf, setHandoverRemoveSelf] = useState(true);

  // Warehouses state
  const [newBayInput, setNewBayInput] = useState('');
  const [warehouseSearch, setWarehouseSearch] = useState('');
  const [loadingWarehouse, setLoadingWarehouse] = useState(false);
  const [warehouseError, setWarehouseError] = useState<string | null>(null);
  const [warehouseSuccess, setWarehouseSuccess] = useState<string | null>(null);

  // Delete warehouse confirmation modal
  const [pendingDeleteBay, setPendingDeleteBay] = useState<{
    bayNum: number;
    carpetsInBay: Carpet[];
  } | null>(null);

  // Self deletion confirmation modal
  const [pendingSelfDelete, setPendingSelfDelete] = useState<string | null>(null);

  if (!isOpen) return null;

  const superAdminsCount = admins.filter((a) => a.role === 'superadmin').length;

  // Handle Admin Add
  const handleAddAdminSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const email = newEmail.trim().toLowerCase();
    setAdminError(null);
    setAdminSuccess(null);

    if (!email || !email.includes('@')) {
      setAdminError('Wpisz poprawny adres e-mail pracownika.');
      return;
    }

    if (admins.some((a) => a.email.toLowerCase() === email)) {
      setAdminError('Użytkownik z tym adresem e-mail jest już na liście administratorów.');
      return;
    }

    try {
      setLoadingAdmin(true);
      await onAddAdmin(email, newRole);
      setNewEmail('');
      setAdminSuccess(
        `Pomyślnie dodano użytkownika ${email} jako ${
          newRole === 'superadmin' ? 'Super Administrator' : 'Administrator sklepu'
        }.`
      );
      setTimeout(() => setAdminSuccess(null), 3500);
    } catch (err: unknown) {
      setAdminError(err instanceof Error ? err.message : 'Wystąpił błąd podczas dodawania');
    } finally {
      setLoadingAdmin(false);
    }
  };

  // Handle changing role
  const handleChangeRole = async (email: string, targetRole: 'admin' | 'superadmin') => {
    setAdminError(null);
    setAdminSuccess(null);

    if (targetRole === 'admin' && superAdminsCount <= 1) {
      const targetUser = admins.find((a) => a.email.toLowerCase() === email.toLowerCase());
      if (targetUser?.role === 'superadmin') {
        setAdminError('W systemie musi pozostać co najmniej jeden Super Administrator.');
        return;
      }
    }

    try {
      setLoadingAdmin(true);
      await onUpdateAdminRole(email, targetRole);
      setAdminSuccess(
        `Zmieniono uprawnienia dla ${email} na ${
          targetRole === 'superadmin' ? 'Super Administrator' : 'Administrator sklepu'
        }.`
      );
      setTimeout(() => setAdminSuccess(null), 3000);
    } catch (err: unknown) {
      setAdminError(err instanceof Error ? err.message : 'Błąd zmiany roli');
    } finally {
      setLoadingAdmin(false);
    }
  };

  // Handle Remove Admin click
  const handleRemoveAdminClick = async (email: string) => {
    const isSelf = currentUserEmail && email.toLowerCase() === currentUserEmail.toLowerCase();
    const targetUser = admins.find((a) => a.email.toLowerCase() === email.toLowerCase());

    if (targetUser?.role === 'superadmin' && superAdminsCount <= 1) {
      setAdminError('Nie można usunąć jedynego Super Administratora. Najpierw wyznacz innego Super Admina.');
      return;
    }

    if (isSelf) {
      setPendingSelfDelete(email);
      return;
    }

    if (window.confirm(`Czy na pewno chcesz odebrać uprawnienia dla ${email}?`)) {
      try {
        setLoadingAdmin(true);
        await onRemoveAdmin(email);
        setAdminSuccess(`Odebrano uprawnienia dla ${email}`);
        setTimeout(() => setAdminSuccess(null), 3000);
      } catch (err: unknown) {
        setAdminError(err instanceof Error ? err.message : 'Wystąpił błąd podczas usuwania');
      } finally {
        setLoadingAdmin(false);
      }
    }
  };

  // Confirm Self Removal
  const handleConfirmSelfDelete = async () => {
    if (!pendingSelfDelete) return;
    try {
      setLoadingAdmin(true);
      await onRemoveAdmin(pendingSelfDelete);
      setPendingSelfDelete(null);
      onClose();
    } catch (err: unknown) {
      setAdminError(err instanceof Error ? err.message : 'Błąd podczas usuwania własnego konta');
      setPendingSelfDelete(null);
    } finally {
      setLoadingAdmin(false);
    }
  };

  // Handle Handover Submit
  const handleHandoverSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const target = handoverTarget.trim().toLowerCase();
    setAdminError(null);
    setAdminSuccess(null);

    if (!target || !target.includes('@')) {
      setAdminError('Wpisz poprawny adres e-mail nowego Super Administratora.');
      return;
    }

    if (currentUserEmail && target === currentUserEmail.toLowerCase()) {
      setAdminError('Nie możesz przekazać roli samemu sobie.');
      return;
    }

    try {
      setLoadingAdmin(true);
      await onTransferSuperAdmin(target, handoverRemoveSelf);
      setIsHandoverOpen(false);
      setHandoverTarget('');
      setAdminSuccess(`Pomyślnie mianowano ${target} nowym Super Administratorem!`);
      setTimeout(() => setAdminSuccess(null), 4000);
      if (handoverRemoveSelf) {
        onClose();
      }
    } catch (err: unknown) {
      setAdminError(err instanceof Error ? err.message : 'Błąd przekazywania uprawnień');
    } finally {
      setLoadingAdmin(false);
    }
  };

  // Parse input for warehouses: e.g. "15", "121", "121-130", "15, 20, 25"
  const parseBayNumbers = (raw: string): number[] => {
    const parts = raw.split(/[\s,]+/);
    const result: number[] = [];

    for (const part of parts) {
      if (!part) continue;
      if (part.includes('-')) {
        const [startStr, endStr] = part.split('-');
        const start = parseInt(startStr, 10);
        const end = parseInt(endStr, 10);
        if (!isNaN(start) && !isNaN(end) && start > 0 && end >= start) {
          const limit = Math.min(end, start + 200);
          for (let i = start; i <= limit; i++) {
            if (!result.includes(i)) result.push(i);
          }
        }
      } else {
        const num = parseInt(part, 10);
        if (!isNaN(num) && num > 0 && !result.includes(num)) {
          result.push(num);
        }
      }
    }
    return result;
  };

  // Handle Add Warehouse
  const handleAddWarehousesSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setWarehouseError(null);
    setWarehouseSuccess(null);

    const parsed = parseBayNumbers(newBayInput);
    if (parsed.length === 0) {
      setWarehouseError('Wpisz poprawny numer magazynu (np. 121) lub zakres (np. 121-125).');
      return;
    }

    const alreadyExist = parsed.filter((num) => activeBays.includes(num));
    const toAdd = parsed.filter((num) => !activeBays.includes(num));

    if (toAdd.length === 0) {
      setWarehouseError(
        `Wszystkie wpisane magazyny (${alreadyExist.join(', ')}) już istnieją na liście.`
      );
      return;
    }

    try {
      setLoadingWarehouse(true);
      await onAddWarehouses(toAdd);
      setNewBayInput('');
      setWarehouseSuccess(
        `Pomyślnie dodano ${toAdd.length} ${
          toAdd.length === 1 ? 'magazyn' : 'magazynów'
        }: ${toAdd.join(', ')}`
      );
      setTimeout(() => setWarehouseSuccess(null), 3500);
    } catch (err: unknown) {
      setWarehouseError(err instanceof Error ? err.message : 'Wystąpił błąd podczas dodawania magazynu');
    } finally {
      setLoadingWarehouse(false);
    }
  };

  // Trigger warehouse deletion check
  const handleRequestDeleteBay = (bayNum: number) => {
    const carpetsInBay = carpets.filter((c) => c.magazyn === bayNum);
    setPendingDeleteBay({ bayNum, carpetsInBay });
  };

  // Confirm warehouse deletion
  const handleConfirmDelete = async () => {
    if (!pendingDeleteBay) return;
    const { bayNum } = pendingDeleteBay;

    try {
      setLoadingWarehouse(true);
      await onDeleteWarehouse(bayNum, true);
      setWarehouseSuccess(`Pomyślnie usunięto Magazyn ${bayNum}.`);
      setTimeout(() => setWarehouseSuccess(null), 3000);
      setPendingDeleteBay(null);
    } catch (err: unknown) {
      setWarehouseError(err instanceof Error ? err.message : 'Wystąpił błąd podczas usuwania magazynu');
    } finally {
      setLoadingWarehouse(false);
    }
  };

  // Initialize 1-120
  const handleInit120 = async () => {
    if (
      window.confirm(
        'Czy na pewno chcesz zainicjalizować listę magazynów 1-120 w bazie Firestore? Ta operacja utworzy brakujące magazyny od 1 do 120.'
      )
    ) {
      try {
        setLoadingWarehouse(true);
        await onInitializeWarehouses(120);
        setWarehouseSuccess('Pomyślnie zainicjalizowano magazyny 1-120 w bazie.');
        setTimeout(() => setWarehouseSuccess(null), 3500);
      } catch (err: unknown) {
        setWarehouseError(err instanceof Error ? err.message : 'Błąd inicjalizacji magazynów');
      } finally {
        setLoadingWarehouse(false);
      }
    }
  };

  // Filtered warehouses
  const filteredBays = activeBays.filter((b) => {
    if (!warehouseSearch.trim()) return true;
    const q = warehouseSearch.trim().toLowerCase();
    return String(b).includes(q) || `magazyn ${b}`.includes(q);
  });

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
      <div className="bg-white rounded-2xl max-w-xl w-full shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-purple-50">
          <div className="flex items-center gap-2 font-bold text-purple-900 text-sm sm:text-base">
            <Crown className="w-5 h-5 text-purple-600" />
            Panel Super Admina – Sklep 038
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-purple-100 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab selection */}
        <div className="flex border-b border-gray-200 bg-gray-50/80 px-4 pt-2 gap-2 text-xs sm:text-sm font-bold">
          <button
            onClick={() => setActiveTab('admins')}
            className={`py-2 px-3.5 rounded-t-xl transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'admins'
                ? 'bg-white text-purple-900 border-t-2 border-purple-600 shadow-xs'
                : 'text-gray-500 hover:text-gray-800'
            }`}
          >
            <ShieldCheck className="w-4 h-4 text-purple-600" />
            <span>Administratorzy ({admins.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('warehouses')}
            className={`py-2 px-3.5 rounded-t-xl transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'warehouses'
                ? 'bg-white text-blue-900 border-t-2 border-blue-600 shadow-xs'
                : 'text-gray-500 hover:text-gray-800'
            }`}
          >
            <Layers className="w-4 h-4 text-blue-600" />
            <span>Magazyny / Regały ({activeBays.length})</span>
          </button>
        </div>

        {/* Tab 1: Admins */}
        {activeTab === 'admins' && (
          <div className="p-4 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-4">
            {/* Super Admin Notice & Handover Button */}
            <div className="bg-purple-50 border border-purple-200 rounded-xl p-3.5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
              <div className="flex items-start gap-2.5">
                <Crown className="w-5 h-5 text-purple-600 shrink-0 mt-0.5" />
                <div className="text-xs">
                  <span className="font-bold text-purple-950 block">Zarządzanie rolami i uprawnieniami</span>
                  <p className="text-purple-800 mt-0.5">
                    Możesz mianować innych Super Adminów, zarządzać pracownikami lub w bezpieczny sposób przekazać aplikację innej osobie.
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setIsHandoverOpen(true)}
                className="shrink-0 flex items-center gap-1.5 px-3 py-1.5 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-xs font-bold transition-colors shadow-xs cursor-pointer"
              >
                <ArrowRightLeft className="w-3.5 h-3.5" />
                <span>Przekaż aplikację</span>
              </button>
            </div>

            {/* Handover Dialog/Box if open */}
            {isHandoverOpen && (
              <div className="bg-amber-50 border-2 border-amber-300 rounded-xl p-4 flex flex-col gap-3 animate-in fade-in">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 font-bold text-amber-900 text-xs sm:text-sm">
                    <ArrowRightLeft className="w-4 h-4 text-amber-700" />
                    Przekazanie roli Super Administratora
                  </div>
                  <button
                    onClick={() => setIsHandoverOpen(false)}
                    className="text-amber-600 hover:text-amber-900 cursor-pointer text-xs font-bold"
                  >
                    Anuluj
                  </button>
                </div>

                <p className="text-xs text-amber-800">
                  Wybierz osobę, której przekazujesz zarządzanie aplikacją. Nowy Super Admin otrzyma pełne uprawnienia do bazy sklepu 038.
                </p>

                <form onSubmit={handleHandoverSubmit} className="flex flex-col gap-2.5">
                  <div>
                    <label className="text-[11px] font-bold text-gray-700 block mb-1">
                      Wybierz z listy lub wpisz nowy adres e-mail:
                    </label>
                    <input
                      type="email"
                      value={handoverTarget}
                      onChange={(e) => setHandoverTarget(e.target.value)}
                      placeholder="np. nowy.kierownik@leroymerlin.pl"
                      className="w-full px-3 py-2 text-xs sm:text-sm border border-gray-300 rounded-lg bg-white focus:ring-2 focus:ring-amber-500 focus:outline-hidden"
                      disabled={loadingAdmin}
                      list="existing-admins-list"
                    />
                    <datalist id="existing-admins-list">
                      {admins
                        .filter((a) => !currentUserEmail || a.email.toLowerCase() !== currentUserEmail.toLowerCase())
                        .map((a) => (
                          <option key={a.email} value={a.email}>
                            {a.email} ({a.role === 'superadmin' ? 'Super Admin' : 'Admin sklepu'})
                          </option>
                        ))}
                    </datalist>
                  </div>

                  <label className="flex items-center gap-2 text-xs text-gray-800 cursor-pointer select-none py-1">
                    <input
                      type="checkbox"
                      checked={handoverRemoveSelf}
                      onChange={(e) => setHandoverRemoveSelf(e.target.checked)}
                      className="w-4 h-4 text-purple-600 rounded-sm focus:ring-purple-500"
                    />
                    <span className="font-semibold">
                      Usuń moje konto z listy administratorów po przekazaniu (całkowite oddanie aplikacji)
                    </span>
                  </label>

                  <div className="flex justify-end gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() => setIsHandoverOpen(false)}
                      className="px-3 py-1.5 text-xs text-gray-600 hover:bg-amber-100 rounded-lg transition-colors cursor-pointer"
                    >
                      Anuluj
                    </button>
                    <button
                      type="submit"
                      disabled={loadingAdmin || !handoverTarget.trim()}
                      className="px-4 py-2 bg-amber-600 hover:bg-amber-700 disabled:opacity-50 text-white rounded-lg text-xs font-bold transition-colors shadow-xs cursor-pointer flex items-center gap-1.5"
                    >
                      <Check className="w-3.5 h-3.5" />
                      Potwierdź przekazanie
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* Add new administrator form */}
            <div className="bg-gray-50 border border-gray-200 rounded-xl p-3.5 flex flex-col gap-2.5">
              <span className="text-xs font-bold text-gray-800 block">
                Dodaj nowego administratora (konto Google)
              </span>

              <form onSubmit={handleAddAdminSubmit} className="flex flex-col gap-2.5">
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Mail className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="email"
                      value={newEmail}
                      onChange={(e) => setNewEmail(e.target.value)}
                      placeholder="np. pracownik@leroymerlin.pl lub gmail.com"
                      className="w-full pl-9 pr-3 py-2 text-xs sm:text-sm border border-gray-300 rounded-lg bg-white focus:ring-2 focus:ring-purple-500 focus:outline-hidden"
                      disabled={loadingAdmin}
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={loadingAdmin || !newEmail.trim()}
                    className="px-4 py-2 bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white rounded-lg text-xs sm:text-sm font-bold flex items-center gap-1.5 transition-colors shadow-xs cursor-pointer shrink-0"
                  >
                    <UserPlus className="w-4 h-4" />
                    Dodaj
                  </button>
                </div>

                {/* Role selector pills */}
                <div className="flex items-center gap-2 pt-0.5">
                  <span className="text-[11px] font-bold text-gray-500">Rola:</span>
                  <button
                    type="button"
                    onClick={() => setNewRole('admin')}
                    className={`px-2.5 py-1 rounded-lg text-xs font-bold flex items-center gap-1 transition-all cursor-pointer ${
                      newRole === 'admin'
                        ? 'bg-blue-600 text-white shadow-2xs'
                        : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-100'
                    }`}
                  >
                    <Shield className="w-3.5 h-3.5" />
                    Admin sklepu
                  </button>
                  <button
                    type="button"
                    onClick={() => setNewRole('superadmin')}
                    className={`px-2.5 py-1 rounded-lg text-xs font-bold flex items-center gap-1 transition-all cursor-pointer ${
                      newRole === 'superadmin'
                        ? 'bg-purple-600 text-white shadow-2xs'
                        : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-100'
                    }`}
                  >
                    <Crown className="w-3.5 h-3.5" />
                    Super Administrator
                  </button>
                </div>
              </form>
            </div>

            {adminError && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-2.5 flex items-center gap-2 text-xs text-red-700">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{adminError}</span>
              </div>
            )}

            {adminSuccess && (
              <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-2.5 flex items-center gap-2 text-xs text-emerald-700">
                <Check className="w-4 h-4 shrink-0" />
                <span>{adminSuccess}</span>
              </div>
            )}

            {/* Active Admins List */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold text-gray-700">
                  Lista administratorów ({admins.length})
                </span>
                <span className="text-[11px] text-gray-400">
                  Super Admini: {superAdminsCount}
                </span>
              </div>

              <div className="flex flex-col gap-2">
                {admins.map((adm) => {
                  const isCurrent =
                    currentUserEmail && adm.email.toLowerCase() === currentUserEmail.toLowerCase();
                  const isSuper = adm.role === 'superadmin';
                  const isSoleSuperAdmin = isSuper && superAdminsCount <= 1;

                  return (
                    <div
                      key={adm.email}
                      className={`flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-xl border gap-2 transition-colors ${
                        isSuper
                          ? 'border-purple-200 bg-purple-50/40 hover:border-purple-300'
                          : 'border-gray-200 bg-white hover:border-gray-300'
                      }`}
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div
                          className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
                            isSuper
                              ? 'bg-purple-600 text-white'
                              : 'bg-blue-100 text-blue-700'
                          }`}
                        >
                          {isSuper ? <Crown className="w-4 h-4" /> : <Shield className="w-4 h-4" />}
                        </div>
                        <div className="min-w-0">
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <span className="text-xs sm:text-sm font-bold text-gray-900 font-mono truncate">
                              {adm.email}
                            </span>
                            {isCurrent && (
                              <span className="px-1.5 py-0.2 rounded text-[10px] font-bold bg-amber-100 text-amber-800">
                                TY
                              </span>
                            )}
                          </div>
                          <div className="text-[11px] text-gray-400 flex items-center gap-2">
                            {adm.createdAt && <span>Dodano: {adm.createdAt}</span>}
                            {adm.addedBy && <span>przez: {adm.addedBy}</span>}
                          </div>
                        </div>
                      </div>

                      {/* Role & actions */}
                      <div className="flex items-center justify-between sm:justify-end gap-2 pt-1 sm:pt-0 border-t sm:border-t-0 border-gray-100">
                        {/* Role switcher */}
                        <div className="flex items-center gap-1">
                          {isSuper ? (
                            <button
                              type="button"
                              onClick={() => handleChangeRole(adm.email, 'admin')}
                              disabled={loadingAdmin || isSoleSuperAdmin}
                              title={
                                isSoleSuperAdmin
                                  ? 'W systemie musi pozostać co najmniej jeden Super Admin'
                                  : 'Zmień na zwykłego Administratora'
                              }
                              className={`px-2 py-1 rounded-md text-[11px] font-bold flex items-center gap-1 transition-colors cursor-pointer ${
                                isSoleSuperAdmin
                                  ? 'bg-purple-200 text-purple-900 cursor-not-allowed opacity-90'
                                  : 'bg-purple-100 text-purple-800 hover:bg-purple-200'
                              }`}
                            >
                              <Crown className="w-3 h-3 text-purple-700" />
                              <span>SUPER ADMIN</span>
                              {!isSoleSuperAdmin && <ChevronDown className="w-3 h-3 ml-0.5 opacity-60" />}
                            </button>
                          ) : (
                            <button
                              type="button"
                              onClick={() => handleChangeRole(adm.email, 'superadmin')}
                              disabled={loadingAdmin}
                              title="Awansuj na Super Administratora"
                              className="px-2 py-1 rounded-md text-[11px] font-bold bg-blue-100 text-blue-800 hover:bg-blue-200 flex items-center gap-1 transition-colors cursor-pointer"
                            >
                              <Shield className="w-3 h-3 text-blue-700" />
                              <span>ADMIN SKLEPU</span>
                              <ChevronDown className="w-3 h-3 ml-0.5 opacity-60" />
                            </button>
                          )}
                        </div>

                        {/* Remove admin button */}
                        <button
                          type="button"
                          onClick={() => handleRemoveAdminClick(adm.email)}
                          disabled={loadingAdmin || isSoleSuperAdmin}
                          className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                            isSoleSuperAdmin
                              ? 'text-gray-300 cursor-not-allowed'
                              : 'text-gray-400 hover:text-red-600 hover:bg-red-50'
                          }`}
                          title={
                            isSoleSuperAdmin
                              ? 'Nie można usunąć jedynego Super Administratora'
                              : isCurrent
                              ? 'Zrzeknij się i usuń swoje konto'
                              : 'Usuń uprawnienia'
                          }
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  );
                })}

                {admins.length === 0 && (
                  <div className="p-4 text-center border border-dashed border-gray-200 rounded-xl text-gray-400 text-xs">
                    Brak administratorów na liście.
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Warehouses Management */}
        {activeTab === 'warehouses' && (
          <div className="p-4 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-4">
            {/* Quick explanation & stats */}
            <div className="bg-blue-50 border border-blue-200 rounded-xl p-3.5 flex items-start justify-between gap-3">
              <div className="flex items-start gap-2.5">
                <Layers className="w-5 h-5 text-blue-700 shrink-0 mt-0.5" />
                <div className="text-xs">
                  <span className="font-bold text-blue-950 block">Zarządzanie magazynami sklepu:</span>
                  <p className="text-blue-800 mt-0.5">
                    Możesz swobodnie dodawać nowe magazyny (np. 121) lub usuwać zbędne (np. 15). Zmiany są natychmiast synchronizowane w bazie.
                  </p>
                </div>
              </div>

              <button
                onClick={handleInit120}
                disabled={loadingWarehouse}
                className="shrink-0 flex items-center gap-1 px-2.5 py-1.5 bg-blue-100 hover:bg-blue-200 text-blue-800 rounded-lg text-xs font-bold transition-colors cursor-pointer"
                title="Utwórz lub przywróć magazyny 1-120"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                Reset 1-120
              </button>
            </div>

            {/* Add warehouse form */}
            <div className="bg-gray-50 border border-gray-200 rounded-xl p-3.5 flex flex-col gap-2">
              <label className="text-xs font-bold text-gray-800 block">
                Dodaj magazyn lub zakres magazynów
              </label>
              <form onSubmit={handleAddWarehousesSubmit} className="flex gap-2">
                <input
                  type="text"
                  value={newBayInput}
                  onChange={(e) => setNewBayInput(e.target.value)}
                  placeholder="np. 15 lub 121-130"
                  className="flex-1 px-3 py-2 text-xs sm:text-sm border border-gray-300 rounded-lg bg-white focus:ring-2 focus:ring-blue-500 focus:outline-hidden font-medium"
                  disabled={loadingWarehouse}
                />
                <button
                  type="submit"
                  disabled={loadingWarehouse || !newBayInput.trim()}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-lg text-xs sm:text-sm font-bold flex items-center gap-1.5 transition-colors shadow-xs cursor-pointer shrink-0"
                >
                  <Plus className="w-4 h-4" />
                  Dodaj
                </button>
              </form>
              <span className="text-[11px] text-gray-500">
                Możesz wpisać pojedynczy numer (np. <strong className="font-mono">15</strong>, <strong className="font-mono">121</strong>) lub zakres oddzielony myślnikiem (np. <strong className="font-mono">121-125</strong>).
              </span>
            </div>

            {warehouseError && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-2.5 flex items-center gap-2 text-xs text-red-700">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{warehouseError}</span>
              </div>
            )}

            {warehouseSuccess && (
              <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-2.5 flex items-center gap-2 text-xs text-emerald-700">
                <Check className="w-4 h-4 shrink-0" />
                <span>{warehouseSuccess}</span>
              </div>
            )}

            {/* Search / filter in warehouses */}
            <div className="flex items-center justify-between gap-3 pt-1">
              <span className="text-xs font-bold text-gray-700">
                Aktywne magazyny ({activeBays.length})
              </span>

              <div className="relative max-w-[160px] w-full">
                <Search className="w-3.5 h-3.5 text-gray-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={warehouseSearch}
                  onChange={(e) => setWarehouseSearch(e.target.value)}
                  placeholder="Filtruj..."
                  className="w-full pl-7 pr-2 py-1 text-xs border border-gray-300 rounded-lg bg-white focus:outline-hidden focus:ring-1 focus:ring-blue-500"
                />
              </div>
            </div>

            {/* Warehouses Grid */}
            <div className="border border-gray-200 rounded-xl p-3 bg-gray-50/50 max-h-72 overflow-y-auto grid grid-cols-2 sm:grid-cols-3 gap-2">
              {filteredBays.length === 0 ? (
                <div className="col-span-full py-6 text-center text-xs text-gray-400">
                  Nie znaleziono magazynów pasujących do filtra.
                </div>
              ) : (
                filteredBays.map((bayNum) => {
                  const bayCarpets = carpets.filter((c) => c.magazyn === bayNum);
                  const count = bayCarpets.length;

                  return (
                    <div
                      key={bayNum}
                      className="flex items-center justify-between p-2.5 bg-white border border-gray-200 rounded-xl shadow-2xs hover:border-blue-300 transition-colors"
                    >
                      <div className="min-w-0 pr-1">
                        <span className="text-xs sm:text-sm font-black text-gray-900 block truncate">
                          Magazyn {bayNum}
                        </span>
                        <span
                          className={`text-[10px] font-semibold block truncate ${
                            count > 0 ? 'text-blue-700' : 'text-gray-400'
                          }`}
                        >
                          {count > 0 ? `${count} ${count === 1 ? 'dywan' : 'dywany'}` : 'Pusty'}
                        </span>
                      </div>

                      <button
                        onClick={() => handleRequestDeleteBay(bayNum)}
                        disabled={loadingWarehouse}
                        className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer shrink-0"
                        title={`Usuń Magazyn ${bayNum}`}
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="px-5 py-3 border-t border-gray-200 bg-gray-50 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-800 hover:bg-gray-900 text-white rounded-lg text-xs sm:text-sm font-semibold transition-colors cursor-pointer"
          >
            Zamknij
          </button>
        </div>
      </div>

      {/* Confirmation Modal for Self Removal */}
      {pendingSelfDelete && (
        <div className="fixed inset-0 z-60 bg-black/70 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-5 sm:p-6 shadow-2xl flex flex-col gap-4 animate-in fade-in zoom-in-95">
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <div className="text-center">
              <h3 className="text-base sm:text-lg font-black text-gray-900">
                Usunąć własne uprawnienia?
              </h3>
              <p className="mt-2 text-xs text-gray-600">
                Czy na pewno chcesz usunąć swoje konto (<strong className="font-mono">{pendingSelfDelete}</strong>) z listy administratorów? Po tej operacji utracisz dostęp do panelu edycji magazynu i zarządzania aplikacją.
              </p>
            </div>

            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => setPendingSelfDelete(null)}
                className="flex-1 py-2.5 px-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs sm:text-sm transition-colors cursor-pointer"
              >
                Anuluj
              </button>
              <button
                type="button"
                onClick={handleConfirmSelfDelete}
                disabled={loadingAdmin}
                className="flex-1 py-2.5 px-3 rounded-xl bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-bold text-xs sm:text-sm transition-colors cursor-pointer shadow-xs"
              >
                {loadingAdmin ? 'Usuwanie...' : 'Tak, usuń moje konto'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal for Warehouse Deletion */}
      {pendingDeleteBay && (
        <div className="fixed inset-0 z-60 bg-black/70 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-5 sm:p-6 shadow-2xl flex flex-col gap-4 animate-in fade-in zoom-in-95">
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <div className="text-center">
              <h3 className="text-base sm:text-lg font-black text-gray-900">
                Usunąć Magazyn {pendingDeleteBay.bayNum}?
              </h3>

              {pendingDeleteBay.carpetsInBay.length > 0 ? (
                <div className="mt-2 text-xs text-gray-600 flex flex-col gap-2">
                  <p className="text-red-700 font-bold bg-red-50 p-2.5 rounded-xl border border-red-200 text-left">
                    Uwaga! Magazyn {pendingDeleteBay.bayNum} zawiera obecnie {pendingDeleteBay.carpetsInBay.length} {pendingDeleteBay.carpetsInBay.length === 1 ? 'dywan' : 'dywany/ów'}:
                  </p>
                  <div className="max-h-32 overflow-y-auto text-left bg-gray-50 p-2.5 rounded-lg border border-gray-200 flex flex-col gap-1">
                    {pendingDeleteBay.carpetsInBay.map((c) => (
                      <div key={c.id} className="text-[11px] truncate">
                        • <strong className="font-mono">{c.miejsce || `${c.magazyn}${c.podmagazyn}`}</strong>: {c.nazwa} ({c.ref})
                      </div>
                    ))}
                  </div>
                  <p className="text-gray-500 font-medium text-left">
                    Usunięcie magazynu spowoduje również usunięcie tych dywanów z bazy danych.
                  </p>
                </div>
              ) : (
                <p className="text-xs text-gray-600 mt-2 font-medium">
                  Ten magazyn jest obecnie pusty. Czy na pewno chcesz go usunąć z listy regałów?
                </p>
              )}
            </div>

            <div className="flex gap-2 pt-2">
              <button
                onClick={() => setPendingDeleteBay(null)}
                className="flex-1 py-2.5 px-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs sm:text-sm transition-colors cursor-pointer"
              >
                Anuluj
              </button>
              <button
                onClick={handleConfirmDelete}
                disabled={loadingWarehouse}
                className="flex-1 py-2.5 px-3 rounded-xl bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-bold text-xs sm:text-sm transition-colors cursor-pointer shadow-xs"
              >
                {loadingWarehouse ? 'Usuwanie...' : 'Tak, usuń magazyn'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
