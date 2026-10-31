import { useState, useEffect, useMemo } from 'react';
import {
  collection,
  onSnapshot,
  doc,
  setDoc,
  updateDoc,
  deleteDoc,
  writeBatch
} from 'firebase/firestore';
import {
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  User
} from 'firebase/auth';
import { db, auth, googleProvider, handleFirestoreError, OperationType } from './firebase';
import { Carpet, AdminUser, Warehouse, ActivityLog, ActivityAction, SUPER_ADMIN_EMAIL, STORE_NUMBER, STORE_CITY } from './types';
import { initialCarpets } from './data/seedData';
import { Header } from './components/Header';
import { SearchView } from './components/SearchView';
import { WarehousePanel } from './components/WarehousePanel';
import { LoginModal } from './components/LoginModal';
import { BarcodeScannerModal } from './components/BarcodeScannerModal';
import { FiszkaModal } from './components/FiszkaModal';
import { FiszkiBatchModal, FiszkaItem } from './components/FiszkiBatchModal';
import { StockExpoModal } from './components/StockExpoModal';
import { ImportModal } from './components/ImportModal';
import { EditCarpetModal } from './components/EditCarpetModal';
import { AdminManagementModal } from './components/AdminManagementModal';
import { HelpModal } from './components/HelpModal';
import { ActivityLogModal } from './components/ActivityLogModal';
import { OfflineIndicator } from './components/OfflineIndicator';
import { ShieldAlert, LogOut, CheckCircle2 } from 'lucide-react';

export default function App() {
  const [currentView, setCurrentView] = useState<'search' | 'warehouse'>(() => {
    try {
      const saved = localStorage.getItem('lm_current_view');
      return saved === 'warehouse' ? 'warehouse' : 'search';
    } catch {
      return 'search';
    }
  });

  const handleSetCurrentView = (view: 'search' | 'warehouse') => {
    setCurrentView(view);
    try {
      localStorage.setItem('lm_current_view', view);
    } catch {
      // ignore
    }
  };

  const [carpets, setCarpets] = useState<Carpet[]>([]);
  const [admins, setAdmins] = useState<AdminUser[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [superAdminTab, setSuperAdminTab] = useState<'admins' | 'warehouses'>('admins');
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);

  // Search state
  const [searchQuery, setSearchQuery] = useState('');

  // Modals state
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isScannerModalOpen, setIsScannerModalOpen] = useState(false);
  const [fiszkaCarpet, setFiszkaCarpet] = useState<Carpet | null>(null);
  const [isFiszkiBatchModalOpen, setIsFiszkiBatchModalOpen] = useState(false);
  const [batchFiszkiItems, setBatchFiszkiItems] = useState<FiszkaItem[]>([]);
  const [isStockExpoOpen, setIsStockExpoOpen] = useState(false);
  const [isImportOpen, setIsImportOpen] = useState(false);
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isAdminManagementOpen, setIsAdminManagementOpen] = useState(false);
  const [isActivityLogModalOpen, setIsActivityLogModalOpen] = useState(false);
  const [activityLogs, setActivityLogs] = useState<ActivityLog[]>([]);

  // Edit carpet modal state
  const [isEditCarpetOpen, setIsEditCarpetOpen] = useState(false);
  const [carpetToEdit, setCarpetToEdit] = useState<Partial<Carpet> | null>(null);

  // Auth observer
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user);
      setAuthLoading(false);
    });
    return () => unsubscribe();
  }, []);

  // Real-time synchronization of Carpets from Firestore
  useEffect(() => {
    setIsSyncing(true);
    const unsubscribeCarpets = onSnapshot(
      collection(db, 'carpets'),
      (snapshot) => {
        const loadedCarpets: Carpet[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          loadedCarpets.push({
            id: docSnap.id,
            magazyn: Number(data.magazyn) || 1,
            podmagazyn: String(data.podmagazyn || 'A').toUpperCase().trim() || 'A',
            miejsce: data.miejsce || `${Number(data.magazyn) || 1} ${String(data.podmagazyn || 'A').toUpperCase().trim() || 'A'}`,
            nazwa: data.nazwa || '',
            ref: data.ref || '',
            ean: data.ean || '',
            dodano: data.dodano || '',
            updatedAt: data.updatedAt,
            updatedBy: data.updatedBy
          });
        });

        // If Firestore is empty on first ever run, seed with sample data
        if (loadedCarpets.length === 0) {
          setCarpets(initialCarpets);
        } else {
          setCarpets(loadedCarpets);
        }
        setIsSyncing(false);
      },
      (error) => {
        setIsSyncing(false);
        handleFirestoreError(error, OperationType.LIST, 'carpets');
      }
    );

    return () => unsubscribeCarpets();
  }, []);

  // Real-time synchronization of Admins from Firestore
  useEffect(() => {
    const unsubscribeAdmins = onSnapshot(
      collection(db, 'admins'),
      (snapshot) => {
        const loadedAdmins: AdminUser[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          loadedAdmins.push({
            email: data.email || docSnap.id,
            role: (data.role as 'superadmin' | 'admin') || 'admin',
            addedBy: data.addedBy,
            createdAt: data.createdAt || ''
          });
        });

        // If the database collection does not yet contain baluch.arek@gmail.com
        // AND no other superadmin exists, provide the bootstrap entry and auto-seed it
        const hasAnySuperAdmin = loadedAdmins.some((a) => a.role === 'superadmin');
        const hasBootstrap = loadedAdmins.some(
          (a) => a.email.toLowerCase() === SUPER_ADMIN_EMAIL.toLowerCase()
        );

        if (!hasBootstrap && !hasAnySuperAdmin) {
          loadedAdmins.unshift({
            email: SUPER_ADMIN_EMAIL,
            role: 'superadmin',
            createdAt: '2026-10-30',
            addedBy: 'Inicjalizacja systemu'
          });

          // If current user is baluch.arek@gmail.com, quietly write document to Firestore
          if (currentUser?.email?.toLowerCase() === SUPER_ADMIN_EMAIL.toLowerCase()) {
            setDoc(
              doc(db, 'admins', SUPER_ADMIN_EMAIL.toLowerCase()),
              {
                email: SUPER_ADMIN_EMAIL.toLowerCase(),
                role: 'superadmin',
                createdAt: new Date().toISOString().split('T')[0],
                addedBy: 'Główny Super Administrator'
              },
              { merge: true }
            ).catch((err) => console.warn('Auto-seed superadmin note:', err));
          }
        }

        setAdmins(loadedAdmins);
      },
      (error) => {
        // Unauthenticated users or non-admins might not read admins; log gently
        console.warn('Admins collection snapshot notice:', error.message);
      }
    );

    return () => unsubscribeAdmins();
  }, [currentUser]);

  // Real-time synchronization of Warehouses from Firestore
  useEffect(() => {
    const unsubscribeWarehouses = onSnapshot(
      collection(db, 'warehouses'),
      (snapshot) => {
        const loaded: Warehouse[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          loaded.push({
            id: docSnap.id,
            number: Number(data.number) || Number(docSnap.id),
            name: data.name || '',
            createdAt: data.createdAt,
            createdBy: data.createdBy
          });
        });
        loaded.sort((a, b) => a.number - b.number);
        setWarehouses(loaded);
      },
      (error) => {
        console.warn('Warehouses collection snapshot notice:', error.message);
      }
    );

    return () => unsubscribeWarehouses();
  }, []);

  // Real-time synchronization of Activity Logs from Firestore (Audit Trail)
  useEffect(() => {
    const unsubscribeLogs = onSnapshot(
      collection(db, 'activity_logs'),
      (snapshot) => {
        const loaded: ActivityLog[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          loaded.push({
            id: docSnap.id,
            timestamp: data.timestamp || new Date().toISOString(),
            userEmail: data.userEmail || 'Nieznany',
            userName: data.userName,
            action: (data.action as ActivityAction) || 'edit',
            target: data.target || '',
            details: data.details || '',
            miejsce: data.miejsce,
            ref: data.ref,
            ean: data.ean
          });
        });
        // Sort newest first
        loaded.sort(
          (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
        );
        setActivityLogs(loaded);
      },
      (error) => {
        console.warn('Activity logs listener notice:', error.message);
      }
    );

    return () => unsubscribeLogs();
  }, []);

  // Compute active warehouse bays (from Firestore, fallback to 1..120 default + carpets)
  const activeBays = useMemo(() => {
    if (warehouses.length > 0) {
      return warehouses.map((w) => w.number);
    }
    const defaultList: number[] = [];
    for (let i = 1; i <= 120; i++) {
      defaultList.push(i);
    }
    carpets.forEach((c) => {
      if (c.magazyn && !defaultList.includes(c.magazyn)) {
        defaultList.push(c.magazyn);
      }
    });
    return defaultList.sort((a, b) => a - b);
  }, [warehouses, carpets]);

  // Authorization checks
  const isSuperAdmin = useMemo(() => {
    if (!currentUser?.email) return false;
    const userEmail = currentUser.email.toLowerCase();

    // 1. Direct role check in loaded admins
    const currentAdminDoc = admins.find(
      (a) => a.email.toLowerCase() === userEmail
    );
    if (currentAdminDoc) {
      return currentAdminDoc.role === 'superadmin';
    }

    // 2. Initial bootstrap fallback check (only if no other superadmin has been assigned)
    const hasAnyOtherSuperAdmin = admins.some(
      (a) => a.role === 'superadmin' && a.email.toLowerCase() !== SUPER_ADMIN_EMAIL.toLowerCase()
    );
    if (userEmail === SUPER_ADMIN_EMAIL.toLowerCase() && !hasAnyOtherSuperAdmin) {
      return true;
    }

    return false;
  }, [currentUser, admins]);

  const isAdmin = Boolean(
    isSuperAdmin ||
      (currentUser?.email &&
        admins.some((a) => a.email.toLowerCase() === currentUser.email?.toLowerCase()))
  );

  // Google Sign-In
  const handleGoogleSignIn = async () => {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      if (result.user) {
        setCurrentUser(result.user);
        handleSetCurrentView('warehouse');
      }
    } catch (err) {
      console.error('Google Sign In Error:', err);
      throw err;
    }
  };

  // Logout
  const handleLogout = async () => {
    await signOut(auth);
    handleSetCurrentView('search');
  };

  // Switch account (sign out current and prompt account chooser immediately)
  const handleSwitchAccount = async () => {
    try {
      await signOut(auth);
      const result = await signInWithPopup(auth, googleProvider);
      if (result.user) {
        setCurrentUser(result.user);
        handleSetCurrentView('warehouse');
      }
    } catch (err: unknown) {
      console.log('Switch account closed or cancelled:', err);
    }
  };

  // Audit logger to record who made changes in Firestore
  const logActivity = async (
    action: ActivityAction,
    target: string,
    details: string,
    extra?: { miejsce?: string; ref?: string; ean?: string }
  ) => {
    try {
      const userEmail = currentUser?.email || 'admin@leroymerlin.pl';
      const logId = `log_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
      const payload: ActivityLog = {
        id: logId,
        timestamp: new Date().toISOString(),
        userEmail,
        action,
        target,
        details,
        miejsce: extra?.miejsce || '',
        ref: extra?.ref || '',
        ean: extra?.ean || ''
      };
      await setDoc(doc(db, 'activity_logs', logId), payload);
    } catch (err) {
      console.warn('Activity log write notice:', err);
    }
  };

  // Add / Edit Carpet
  const handleSaveCarpet = async (data: Partial<Carpet>) => {
    if (!isAdmin) {
      alert('Tylko uprawnieni administratorzy sklepu 038 mogą edytować magazyn.');
      return;
    }

    const docId = data.id || `mag_${data.magazyn}_${data.podmagazyn?.toUpperCase()}`;
    const isExisting = carpets.some((c) => c.id === docId);
    const userEmail = currentUser?.email || 'admin@leroymerlin.pl';
    const timestamp = new Date().toISOString();

    const payload = {
      magazyn: Number(data.magazyn),
      podmagazyn: String(data.podmagazyn).toUpperCase(),
      miejsce: data.miejsce || `${data.magazyn} ${data.podmagazyn}`,
      nazwa: data.nazwa || '',
      ref: data.ref || '',
      ean: data.ean || '',
      dodano: data.dodano || timestamp.split('T')[0],
      updatedAt: timestamp,
      updatedBy: userEmail
    };

    try {
      await setDoc(doc(db, 'carpets', docId), payload, { merge: true });
      await logActivity(
        isExisting ? 'edit' : 'add',
        payload.nazwa || `Dywan ${payload.miejsce}`,
        isExisting
          ? `Zmienił dywan "${payload.nazwa}" (Miejsce: ${payload.miejsce}, REF: ${payload.ref || '-'}, EAN: ${payload.ean || '-'})`
          : `Dodał dywan "${payload.nazwa}" do magazynu (Miejsce: ${payload.miejsce}, REF: ${payload.ref || '-'}, EAN: ${payload.ean || '-'})`,
        {
          miejsce: payload.miejsce,
          ref: payload.ref,
          ean: payload.ean
        }
      );
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `carpets/${docId}`);
    }
  };

  // Delete Carpet
  const handleDeleteCarpet = async (carpetId: string, name: string) => {
    if (!isAdmin) {
      alert('Brak uprawnień do usuwania pozycji z magazynu.');
      return;
    }

    if (window.confirm(`Czy na pewno chcesz usunąć pozycję:\n"${name}"?`)) {
      try {
        const carpetToDelete = carpets.find((c) => c.id === carpetId);
        await deleteDoc(doc(db, 'carpets', carpetId));
        await logActivity(
          'delete',
          name || `Dywan ${carpetId}`,
          `Zdjął dywan "${name}" z miejsca ${carpetToDelete?.miejsce || carpetId} (REF: ${carpetToDelete?.ref || '-'}, EAN: ${carpetToDelete?.ean || '-'})`,
          {
            miejsce: carpetToDelete?.miejsce,
            ref: carpetToDelete?.ref,
            ean: carpetToDelete?.ean
          }
        );
      } catch (error) {
        handleFirestoreError(error, OperationType.DELETE, `carpets/${carpetId}`);
      }
    }
  };

  // Bulk Import
  const handleImportCarpets = async (incoming: Carpet[], mode: 'merge' | 'replace') => {
    if (!isAdmin) {
      throw new Error('Brak uprawnień do importowania bazy.');
    }

    try {
      // If replace mode, delete existing carpets in safe batches
      if (mode === 'replace') {
        for (let i = 0; i < carpets.length; i += 300) {
          const b = writeBatch(db);
          const chunk = carpets.slice(i, i + 300);
          chunk.forEach((c) => {
            b.delete(doc(db, 'carpets', c.id));
          });
          await b.commit();
        }
      }

      // Write incoming carpets in safe batches
      for (let i = 0; i < incoming.length; i += 300) {
        const b = writeBatch(db);
        const chunk = incoming.slice(i, i + 300);
        chunk.forEach((c) => {
          const docId = c.id || `mag_${c.magazyn}_${c.podmagazyn?.toUpperCase() || 'A'}`;
          const ref = doc(db, 'carpets', docId);
          b.set(
            ref,
            {
              magazyn: Number(c.magazyn) || 1,
              podmagazyn: String(c.podmagazyn || 'A').toUpperCase(),
              miejsce: c.miejsce || `${c.magazyn} ${c.podmagazyn}`,
              nazwa: c.nazwa || '',
              ref: c.ref || '',
              ean: c.ean || '',
              dodano: c.dodano || new Date().toISOString().split('T')[0],
              updatedAt: new Date().toISOString(),
              updatedBy: currentUser?.email || 'import'
            },
            { merge: true }
          );
        });
        await b.commit();
      }

      // Log import in activity audit
      await logActivity(
        'add',
        `Import bazy (${incoming.length} pozycji)`,
        `Zaimportował bazę dywanów: ${incoming.length} pozycji (Tryb: ${mode === 'replace' ? 'Zastąpienie całej bazy' : 'Scalenie z obecną bazą'})`
      );
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, 'carpets/batch');
    }
  };

  // Super Admin: Add Admin Email with selected role
  const handleAddAdminEmail = async (email: string, role: 'admin' | 'superadmin' = 'admin') => {
    if (!isSuperAdmin) {
      throw new Error('Tylko Super Admin może dodawać administratorów.');
    }
    const normalized = email.trim().toLowerCase();
    const docId = normalized;
    const payload: AdminUser = {
      email: normalized,
      role,
      addedBy: currentUser?.email || 'Super Admin',
      createdAt: new Date().toISOString().split('T')[0]
    };

    try {
      await setDoc(doc(db, 'admins', docId), payload);
      await logActivity(
        'add',
        `Administrator ${normalized}`,
        `Nadano uprawnienia: ${role === 'superadmin' ? 'Super Administrator' : 'Administrator sklepu'} dla ${normalized}`
      );
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `admins/${docId}`);
    }
  };

  // Super Admin: Update Admin Role
  const handleUpdateAdminRole = async (email: string, newRole: 'admin' | 'superadmin') => {
    if (!isSuperAdmin) {
      throw new Error('Tylko Super Admin może modyfikować uprawnienia administratorów.');
    }
    const normalized = email.trim().toLowerCase();

    // Prevent demoting the last super admin
    if (newRole === 'admin') {
      const superAdmins = admins.filter((a) => a.role === 'superadmin');
      if (superAdmins.length <= 1 && superAdmins.some((a) => a.email.toLowerCase() === normalized)) {
        throw new Error('Nie można zdegradować jedynego Super Administratora. W systemie musi pozostać co najmniej jeden Super Admin.');
      }
    }

    try {
      await updateDoc(doc(db, 'admins', normalized), { role: newRole });
      await logActivity(
        'edit',
        `Rola ${normalized}`,
        `Zmieniono rolę użytkownika ${normalized} na: ${newRole === 'superadmin' ? 'Super Administrator' : 'Administrator sklepu'}`
      );
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `admins/${normalized}`);
    }
  };

  // Super Admin: Remove Admin Email
  const handleRemoveAdminEmail = async (email: string) => {
    if (!isSuperAdmin) {
      throw new Error('Tylko Super Admin może usuwać administratorów.');
    }
    const normalized = email.trim().toLowerCase();

    // Prevent deleting the last super admin
    const targetAdmin = admins.find((a) => a.email.toLowerCase() === normalized);
    if (targetAdmin?.role === 'superadmin') {
      const remainingSuperAdmins = admins.filter(
        (a) => a.role === 'superadmin' && a.email.toLowerCase() !== normalized
      );
      if (remainingSuperAdmins.length === 0) {
        throw new Error('Nie można usunąć jedynego Super Administratora. W systemie musi pozostać co najmniej jeden Super Admin.');
      }
    }

    try {
      await deleteDoc(doc(db, 'admins', normalized));
      await logActivity(
        'delete',
        `Administrator ${normalized}`,
        `Odebrano uprawnienia administratora dla ${normalized}`
      );
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, `admins/${normalized}`);
    }
  };

  // Super Admin: Transfer Super Admin & optional self-removal
  const handleTransferSuperAdmin = async (targetEmail: string, removeSelf: boolean) => {
    if (!isSuperAdmin) {
      throw new Error('Tylko Super Admin może przekazać zarządzanie aplikacją.');
    }
    const normalizedTarget = targetEmail.trim().toLowerCase();
    const currentEmail = currentUser?.email?.toLowerCase();

    if (normalizedTarget === currentEmail) {
      throw new Error('Nie możesz przekazać uprawnień samemu sobie.');
    }

    try {
      const batch = writeBatch(db);

      // 1. Promote or add target user as superadmin
      batch.set(
        doc(db, 'admins', normalizedTarget),
        {
          email: normalizedTarget,
          role: 'superadmin',
          addedBy: currentEmail || 'Przekazanie Super Admina',
          createdAt: new Date().toISOString().split('T')[0]
        },
        { merge: true }
      );

      // 2. If removeSelf, delete current user from admins collection
      if (removeSelf && currentEmail) {
        batch.delete(doc(db, 'admins', currentEmail));
      }

      await batch.commit();

      await logActivity(
        'edit',
        'Przekazanie aplikacji',
        `Super Admin (${currentEmail}) przekazał rolę Super Administratora dla ${normalizedTarget}${
          removeSelf ? ' i usunął własne uprawnienia.' : '.'
        }`
      );
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `admins/${normalizedTarget}`);
    }
  };

  // Super Admin: Add Warehouses
  const handleAddWarehouses = async (numbers: number[]) => {
    if (!isSuperAdmin) {
      throw new Error('Tylko Super Admin może dodawać magazyny.');
    }

    try {
      const batch = writeBatch(db);

      // If warehouses collection is currently empty, seed all current activeBays first
      if (warehouses.length === 0) {
        activeBays.forEach((b) => {
          batch.set(doc(db, 'warehouses', String(b)), {
            number: b,
            createdAt: new Date().toISOString().split('T')[0],
            createdBy: currentUser?.email || SUPER_ADMIN_EMAIL
          });
        });
      }

      numbers.forEach((num) => {
        batch.set(doc(db, 'warehouses', String(num)), {
          number: num,
          createdAt: new Date().toISOString().split('T')[0],
          createdBy: currentUser?.email || SUPER_ADMIN_EMAIL
        });
      });

      await batch.commit();
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, 'warehouses');
    }
  };

  // Super Admin: Delete Warehouse
  const handleDeleteWarehouse = async (bayNumber: number, deleteCarpets: boolean) => {
    if (!isSuperAdmin) {
      throw new Error('Tylko Super Admin może usuwać magazyny.');
    }

    try {
      const batch = writeBatch(db);

      if (warehouses.length === 0) {
        // First initialize all current activeBays EXCEPT bayNumber
        activeBays
          .filter((b) => b !== bayNumber)
          .forEach((b) => {
            batch.set(doc(db, 'warehouses', String(b)), {
              number: b,
              createdAt: new Date().toISOString().split('T')[0],
              createdBy: currentUser?.email || SUPER_ADMIN_EMAIL
            });
          });
      } else {
        batch.delete(doc(db, 'warehouses', String(bayNumber)));
      }

      // If deleteCarpets, remove any carpets in this bay
      if (deleteCarpets) {
        const toDelete = carpets.filter((c) => c.magazyn === bayNumber);
        toDelete.forEach((c) => {
          batch.delete(doc(db, 'carpets', c.id));
        });
      }

      await batch.commit();
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, `warehouses/${bayNumber}`);
    }
  };

  // Super Admin: Initialize default 1-120 in Firestore
  const handleInitializeWarehouses = async (count: number = 120) => {
    if (!isSuperAdmin) {
      throw new Error('Tylko Super Admin może inicjalizować magazyny.');
    }

    try {
      const batch = writeBatch(db);
      for (let i = 1; i <= count; i++) {
        batch.set(doc(db, 'warehouses', String(i)), {
          number: i,
          createdAt: new Date().toISOString().split('T')[0],
          createdBy: currentUser?.email || SUPER_ADMIN_EMAIL
        });
      }
      await batch.commit();
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, 'warehouses/init');
    }
  };

  // Scanner callback
  const handleScanSuccess = (decodedText: string) => {
    setSearchQuery(decodedText);
    setCurrentView('search');
  };

  // Open Add carpet dialog for specific slot
  const handleOpenAdd = (bay?: number, subBay?: string) => {
    if (!isAdmin) {
      setIsLoginModalOpen(true);
      return;
    }
    setCarpetToEdit({
      magazyn: bay || 1,
      podmagazyn: subBay || 'A',
      miejsce: bay ? `${bay} ${subBay || 'A'}` : '1 A',
      dodano: new Date().toISOString().split('T')[0]
    });
    setIsEditCarpetOpen(true);
  };

  const handleOpenEdit = (carpet: Carpet) => {
    if (!isAdmin) {
      setIsLoginModalOpen(true);
      return;
    }
    setCarpetToEdit(carpet);
    setIsEditCarpetOpen(true);
  };

  const handleOpenFiszkiBatch = (carpetToAdd?: Carpet) => {
    if (carpetToAdd && carpetToAdd.ref) {
      const cleanRef = carpetToAdd.ref.trim();
      setBatchFiszkiItems((prev) => {
        if (prev.some((i) => i.ref === cleanRef)) return prev;
        return [
          ...prev,
          {
            id: `${cleanRef}-${Date.now()}`,
            ref: cleanRef,
            nazwa: carpetToAdd.nazwa,
            miejsce: carpetToAdd.miejsce || `${carpetToAdd.magazyn}${carpetToAdd.podmagazyn}`,
            ean: carpetToAdd.ean,
            addedAt: Date.now()
          }
        ];
      });
    }
    setIsFiszkiBatchModalOpen(true);
  };

  return (
    <div className="min-h-screen bg-[#f3f4f6] text-gray-900 flex flex-col font-sans notranslate" translate="no">
      {/* Top Header */}
      <Header
        key="app-header"
        currentView={currentView}
        setCurrentView={(view) => {
          if (view === 'warehouse' && (!currentUser || !isAdmin)) {
            setIsLoginModalOpen(true);
          } else {
            handleSetCurrentView(view);
          }
        }}
        user={currentUser}
        isAdmin={isAdmin}
        isSuperAdmin={isSuperAdmin}
        onLoginClick={() => setIsLoginModalOpen(true)}
        onLogoutClick={handleLogout}
        onSwitchAccount={handleSwitchAccount}
        onOpenSuperAdminModal={() => setIsAdminManagementOpen(true)}
        isSyncing={isSyncing}
      />

      {/* Unauthorized Warning Banner slot - persistent container keeps root DOM hierarchy unchanged */}
      <div key="app-banner-slot">
        {currentUser && !isAdmin && (
          <div className="bg-amber-100 border-b border-amber-300 px-4 py-3 text-xs sm:text-sm text-amber-950 flex flex-wrap items-center justify-between gap-3 shadow-2xs">
            <div className="flex items-center gap-2">
              <ShieldAlert className="w-5 h-5 text-amber-700 shrink-0" />
              <div>
                Zalogowano jako: <strong className="font-mono">{currentUser.email}</strong>. Twoje konto nie zostało jeszcze dodane do listy administratorów Sklepu {STORE_NUMBER}. Skontaktuj się z Super Administratorem (<span className="font-mono font-bold">{SUPER_ADMIN_EMAIL}</span>).
              </div>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={handleSwitchAccount}
                className="px-3 py-1 bg-amber-600 hover:bg-amber-700 font-bold rounded-lg text-white transition-colors cursor-pointer"
              >
                Zmień konto Google
              </button>
              <button
                onClick={handleLogout}
                className="px-3 py-1 bg-amber-200 hover:bg-amber-300 font-bold rounded-lg text-amber-900 transition-colors cursor-pointer"
              >
                Wyloguj
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Main View Area */}
      <main key="app-main" className="flex-1 pb-16">
        {currentView === 'search' ? (
          <div key="view-search-wrapper">
            <SearchView
              carpets={carpets}
              searchQuery={searchQuery}
              setSearchQuery={setSearchQuery}
              onOpenScanner={() => setIsScannerModalOpen(true)}
              onOpenFiszkiModal={handleOpenFiszkiBatch}
              onPrintSingleFiszka={(c) => setFiszkaCarpet(c)}
              onOpenHelp={() => setIsHelpOpen(true)}
            />
          </div>
        ) : authLoading ? (
          <div key="view-loading-wrapper" className="max-w-md mx-auto py-24 px-4 text-center flex flex-col items-center justify-center">
            <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4" />
            <h3 className="text-base font-bold text-gray-800">Sprawdzanie uprawnień administratora...</h3>
            <p className="text-xs text-gray-500 mt-1">Łączenie z bazą sklepu 038 w czasie rzeczywistym</p>
          </div>
        ) : (
          <div key="view-warehouse-wrapper">
            <WarehousePanel
              carpets={carpets}
              activeBays={activeBays}
              onAddProduct={(bay, subBay) => handleOpenAdd(bay, subBay)}
              onEditProduct={handleOpenEdit}
              onDeleteProduct={handleDeleteCarpet}
              onOpenImport={() => setIsImportOpen(true)}
              onOpenStockExpo={() => setIsStockExpoOpen(true)}
              onOpenHelp={() => setIsHelpOpen(true)}
              onOpenActivityLogs={() => setIsActivityLogModalOpen(true)}
              onLogout={handleLogout}
              onGenerateFiszka={(c) => handleOpenFiszkiBatch(c)}
              isSuperAdmin={isSuperAdmin}
              onOpenSuperAdminModal={(tab) => {
                setSuperAdminTab(tab || 'admins');
                setIsAdminManagementOpen(true);
              }}
              onDeleteWarehouse={handleDeleteWarehouse}
              onAddWarehouses={handleAddWarehouses}
            />
          </div>
        )}
      </main>

      {/* Offline connectivity indicator */}
      <OfflineIndicator />

      {/* Modals in a stable persistent container */}
      <div key="app-modals">
      <LoginModal
        isOpen={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onGoogleSignIn={handleGoogleSignIn}
        currentUser={currentUser}
        isAdmin={isAdmin}
        isSuperAdmin={isSuperAdmin}
        onSwitchAccount={handleSwitchAccount}
        onLogout={handleLogout}
      />

      <BarcodeScannerModal
        isOpen={isScannerModalOpen}
        onClose={() => setIsScannerModalOpen(false)}
        onScanSuccess={handleScanSuccess}
      />

      <FiszkiBatchModal
        isOpen={isFiszkiBatchModalOpen}
        onClose={() => setIsFiszkiBatchModalOpen(false)}
        carpets={carpets}
        initialItems={batchFiszkiItems}
        onItemsChange={setBatchFiszkiItems}
      />

      <FiszkaModal
        carpet={fiszkaCarpet}
        onClose={() => setFiszkaCarpet(null)}
      />

      <StockExpoModal
        carpets={carpets}
        activeBays={activeBays}
        isOpen={isStockExpoOpen}
        onClose={() => setIsStockExpoOpen(false)}
        onSelectSlot={(bay, sub) => {
          setCurrentView('warehouse');
          handleOpenAdd(bay, sub);
        }}
      />

      <ImportModal
        isOpen={isImportOpen}
        onClose={() => setIsImportOpen(false)}
        onImport={handleImportCarpets}
      />

      <EditCarpetModal
        isOpen={isEditCarpetOpen}
        onClose={() => {
          setIsEditCarpetOpen(false);
          setCarpetToEdit(null);
        }}
        carpetToEdit={carpetToEdit}
        onSave={handleSaveCarpet}
        existingMagazyny={activeBays}
        existingCarpets={carpets}
      />

      <AdminManagementModal
        isOpen={isAdminManagementOpen}
        onClose={() => setIsAdminManagementOpen(false)}
        admins={admins}
        onAddAdmin={handleAddAdminEmail}
        onUpdateAdminRole={handleUpdateAdminRole}
        onRemoveAdmin={handleRemoveAdminEmail}
        onTransferSuperAdmin={handleTransferSuperAdmin}
        currentUserEmail={currentUser?.email || undefined}
        activeBays={activeBays}
        carpets={carpets}
        onAddWarehouses={handleAddWarehouses}
        onDeleteWarehouse={handleDeleteWarehouse}
        onInitializeWarehouses={handleInitializeWarehouses}
        initialTab={superAdminTab}
      />

      <HelpModal
        isOpen={isHelpOpen}
        onClose={() => setIsHelpOpen(false)}
      />

      <ActivityLogModal
        isOpen={isActivityLogModalOpen}
        onClose={() => setIsActivityLogModalOpen(false)}
        logs={activityLogs}
      />
      </div>
    </div>
  );
}
