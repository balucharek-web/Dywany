import { useState, useEffect, useMemo, useCallback, lazy, Suspense } from 'react';
import {
  collection,
  onSnapshot,
  doc,
  setDoc,
  updateDoc,
  deleteDoc,
  writeBatch,
  runTransaction,
  getDoc,
  getDocs,
  query,
  where
} from 'firebase/firestore';
import {
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  User
} from 'firebase/auth';
import { db, auth, googleProvider, handleFirestoreError, OperationType } from './firebase';
import { Carpet, AdminUser, Warehouse, ActivityLog, ActivityAction, SUPER_ADMIN_EMAIL, STORE_NUMBER, STORE_CITY, PowerBiStockExpoItem } from './types';
import { initialCarpets } from './data/seedData';
import { initProductCatalog, saveProductToCatalog, batchSaveToCatalog } from './services/catalogService';
import { Header } from './components/Header';
import { SearchView } from './components/SearchView';
import { WarehousePanel } from './components/WarehousePanel';
import { LoginModal } from './components/LoginModal';
import { FiszkaModal } from './components/FiszkaModal';
import type { FiszkaItem } from './components/FiszkiBatchModal';
import { OfflineIndicator } from './components/OfflineIndicator';
import { ShieldAlert, LogOut, CheckCircle2 } from 'lucide-react';
import { useKeyboardShortcuts } from './hooks/useKeyboardShortcuts';

// Code-split heavy modals to reduce initial JS payload by up to ~85%
const BarcodeScannerModal = lazy(() =>
  import('./components/BarcodeScannerModal').then((m) => ({ default: m.BarcodeScannerModal }))
);
const StockExpoModal = lazy(() =>
  import('./components/StockExpoModal').then((m) => ({ default: m.StockExpoModal }))
);
const PowerBiAuditModal = lazy(() =>
  import('./components/PowerBiAuditModal').then((m) => ({ default: m.PowerBiAuditModal }))
);
const ImportModal = lazy(() =>
  import('./components/ImportModal').then((m) => ({ default: m.ImportModal }))
);
const EditCarpetModal = lazy(() =>
  import('./components/EditCarpetModal').then((m) => ({ default: m.EditCarpetModal }))
);
const AdminManagementModal = lazy(() =>
  import('./components/AdminManagementModal').then((m) => ({ default: m.AdminManagementModal }))
);
const HelpModal = lazy(() =>
  import('./components/HelpModal').then((m) => ({ default: m.HelpModal }))
);
const ActivityLogModal = lazy(() =>
  import('./components/ActivityLogModal').then((m) => ({ default: m.ActivityLogModal }))
);
const FiszkiBatchModal = lazy(() =>
  import('./components/FiszkiBatchModal').then((m) => ({ default: m.FiszkiBatchModal }))
);
const MoveCarpetModal = lazy(() =>
  import('./components/MoveCarpetModal').then((m) => ({ default: m.MoveCarpetModal }))
);
const CBAInstallTransitionModal = lazy(() =>
  import('./components/CBAInstallTransitionModal').then((m) => ({ default: m.CBAInstallTransitionModal }))
);

export default function App() {
  // Always start on the search screen by default upon launching the app
  const [currentView, setCurrentView] = useState<'search' | 'warehouse'>('search');

  // Clean up any legacy persisted view from previous sessions
  useEffect(() => {
    try {
      localStorage.removeItem('lm_current_view');
    } catch {
      // ignore
    }
  }, []);

  const handleSetCurrentView = (view: 'search' | 'warehouse') => {
    setCurrentView(view);
  };

  // Pure OLED Black Dark Mode (Opcja 4) - default is always 'light'
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    try {
      const saved = localStorage.getItem('lm_theme');
      if (saved === 'dark') return 'dark';
    } catch (_) {}
    return 'light';
  });

  useEffect(() => {
    try {
      const root = document.documentElement;
      if (theme === 'dark') {
        root.classList.add('dark');
      } else {
        root.classList.remove('dark');
      }
      localStorage.setItem('lm_theme', theme);

      // Dynamically update mobile browser address bar / status bar color
      let metaThemeColor = document.querySelector('meta[name="theme-color"]');
      if (!metaThemeColor) {
        metaThemeColor = document.createElement('meta');
        metaThemeColor.setAttribute('name', 'theme-color');
        document.head.appendChild(metaThemeColor);
      }
      metaThemeColor.setAttribute('content', theme === 'dark' ? '#000000' : '#22c55e');
    } catch (_) {}
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
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
  const [isPowerBiAuditOpen, setIsPowerBiAuditOpen] = useState(false);
  const [powerBiItems, setPowerBiItems] = useState<PowerBiStockExpoItem[]>([]);
  const [powerBiUpdatedAt, setPowerBiUpdatedAt] = useState<string | undefined>(undefined);
  const [powerBiUpdatedBy, setPowerBiUpdatedBy] = useState<string | undefined>(undefined);
  const [isImportOpen, setIsImportOpen] = useState(false);
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isAdminManagementOpen, setIsAdminManagementOpen] = useState(false);
  const [isActivityLogModalOpen, setIsActivityLogModalOpen] = useState(false);
  const [activityLogs, setActivityLogs] = useState<ActivityLog[]>([]);

  // Edit carpet modal state
  const [isEditCarpetOpen, setIsEditCarpetOpen] = useState(false);
  const [carpetToEdit, setCarpetToEdit] = useState<Partial<Carpet> | null>(null);

  // Move / Swap carpet modal state (Admins only)
  const [isMoveModalOpen, setIsMoveModalOpen] = useState(false);
  const [carpetToMove, setCarpetToMove] = useState<Carpet | null>(null);

  // Unauthorized login rejection notice
  const [authRejectionError, setAuthRejectionError] = useState<string | null>(null);

  // Helper to verify if an email is authorized as Admin or SuperAdmin
  const checkIsEmailAuthorized = useCallback(
    async (email: string): Promise<boolean> => {
      const normalized = email.trim().toLowerCase();
      if (normalized === SUPER_ADMIN_EMAIL.toLowerCase()) {
        return true;
      }
      if (admins.some((a) => a.email.toLowerCase() === normalized)) {
        return true;
      }
      try {
        const docSnap = await getDoc(doc(db, 'admins', normalized));
        if (docSnap.exists()) {
          return true;
        }
        const qSnap = await getDocs(
          query(collection(db, 'admins'), where('email', '==', normalized))
        );
        if (!qSnap.empty) {
          return true;
        }
      } catch (err) {
        console.warn('Admins verification query notice:', err);
      }
      return false;
    },
    [admins]
  );

  // Auth observer - immediately rejects non-admins and returns to search view
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user && user.email) {
        const isAuthorized = await checkIsEmailAuthorized(user.email);
        if (!isAuthorized) {
          console.warn('Session has unauthorized email, signing out immediately:', user.email);
          await signOut(auth);
          setCurrentUser(null);
          handleSetCurrentView('search');
          setAuthRejectionError(
            `Konto ${user.email} nie posiada uprawnień administratora w Sklepie ${STORE_NUMBER}. Logowanie zostało zablokowane.`
          );
          setAuthLoading(false);
          return;
        }
      }
      setCurrentUser(user);
      setAuthLoading(false);
    });
    return () => unsubscribe();
  }, [checkIsEmailAuthorized]);

  // Initialize persistent store product catalog (memory retained across rack deletions)
  useEffect(() => {
    const unsubscribeCatalog = initProductCatalog();
    return () => unsubscribeCatalog();
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

  // Real-time synchronization of Admins from Firestore (requires authentication)
  useEffect(() => {
    if (!currentUser) {
      setAdmins([]);
      return;
    }
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

  // Real-time synchronization of Activity Logs from Firestore (Audit Trail - requires authentication)
  useEffect(() => {
    if (!currentUser) {
      setActivityLogs([]);
      return;
    }
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
  }, [currentUser]);

  // Real-time synchronization of Power BI Stock Expo from Firestore
  useEffect(() => {
    const docRef = doc(db, 'powerbi_stock_expo', `store_${STORE_NUMBER}`);
    const unsubscribePbi = onSnapshot(
      docRef,
      (docSnap) => {
        if (docSnap.exists()) {
          const data = docSnap.data();
          setPowerBiItems(Array.isArray(data.items) ? data.items : []);
          setPowerBiUpdatedAt(data.updatedAt);
          setPowerBiUpdatedBy(data.updatedBy);
        } else {
          setPowerBiItems([]);
          setPowerBiUpdatedAt(undefined);
          setPowerBiUpdatedBy(undefined);
        }
      },
      (error) => {
        console.warn('Power BI stock expo listener notice:', error.message);
      }
    );

    return () => unsubscribePbi();
  }, []);

  // Security: Auto-logout after 20 minutes of inactivity to protect shared store terminals
  useEffect(() => {
    if (!currentUser) return;

    const INACTIVITY_TIMEOUT_MS = 20 * 60 * 1000; // 20 minutes
    let timeoutId: NodeJS.Timeout;

    const resetTimer = () => {
      if (timeoutId) clearTimeout(timeoutId);
      timeoutId = setTimeout(async () => {
        try {
          await signOut(auth);
          setCurrentUser(null);
          handleSetCurrentView('search');
          setAuthRejectionError(
            `Zostałeś automatycznie wylogowany z powodu 20 minut braku aktywności (ochrona sesji pracownika Sklepu ${STORE_NUMBER}).`
          );
        } catch {
          // ignore
        }
      }, INACTIVITY_TIMEOUT_MS);
    };

    const events = ['mousedown', 'mousemove', 'keydown', 'touchstart', 'scroll'];
    events.forEach((event) => window.addEventListener(event, resetTimer, { passive: true }));
    resetTimer();

    return () => {
      if (timeoutId) clearTimeout(timeoutId);
      events.forEach((event) => window.removeEventListener(event, resetTimer));
    };
  }, [currentUser, handleSetCurrentView]);

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

  // Active store Super Admin email derived from admins collection
  const superAdminEmail = useMemo(() => {
    const sa = admins.find((a) => a.role === 'superadmin');
    return sa?.email;
  }, [admins]);

  // View protection guard: ensure non-admins can never remain on the warehouse panel
  useEffect(() => {
    if (!authLoading && currentView === 'warehouse' && (!currentUser || !isAdmin)) {
      handleSetCurrentView('search');
    }
  }, [authLoading, currentView, currentUser, isAdmin]);

  // Google Sign-In
  const handleGoogleSignIn = async () => {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      if (!user || !user.email) {
        await signOut(auth);
        setCurrentUser(null);
        handleSetCurrentView('search');
        throw new Error('Nie udało się pobrać adresu e-mail konta Google.');
      }

      const isAuthorized = await checkIsEmailAuthorized(user.email);
      if (!isAuthorized) {
        await signOut(auth);
        setCurrentUser(null);
        handleSetCurrentView('search');
        const rejectMsg = `Brak uprawnień. Konto ${user.email} nie jest administratorem ani Super Adminem Sklepu ${STORE_NUMBER}. Logowanie odrzucone — powrót do wyszukiwarki.`;
        setAuthRejectionError(rejectMsg);
        throw new Error(rejectMsg);
      }

      setAuthRejectionError(null);
      setCurrentUser(user);
      handleSetCurrentView('warehouse');
    } catch (err: unknown) {
      const fbErr = err as { code?: string; message?: string };
      const errCode = fbErr?.code || '';
      const errMsg = fbErr?.message || String(err);
      if (
        errCode === 'auth/popup-closed-by-user' ||
        errCode === 'auth/cancelled-popup-request' ||
        errMsg.includes('popup-closed-by-user') ||
        errMsg.includes('cancelled-popup-request')
      ) {
        // User closed or dismissed popup - graceful cancellation, no error
        return;
      }
      console.error('Google Sign In Error:', err);
      throw err;
    }
  };

  // Logout
  const handleLogout = async () => {
    await signOut(auth);
    setCurrentUser(null);
    setAuthRejectionError(null);
    handleSetCurrentView('search');
  };

  // Switch account (sign out current and prompt account chooser immediately)
  const handleSwitchAccount = async () => {
    try {
      await signOut(auth);
      setCurrentUser(null);
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      if (!user || !user.email) {
        handleSetCurrentView('search');
        return;
      }

      const isAuthorized = await checkIsEmailAuthorized(user.email);
      if (!isAuthorized) {
        await signOut(auth);
        setCurrentUser(null);
        handleSetCurrentView('search');
        setAuthRejectionError(
          `Brak uprawnień. Konto ${user.email} nie jest administratorem Sklepu ${STORE_NUMBER}. Logowanie odrzucone — powrót do wyszukiwarki.`
        );
        return;
      }

      setAuthRejectionError(null);
      setCurrentUser(user);
      handleSetCurrentView('warehouse');
    } catch (err: unknown) {
      const fbErr = err as { code?: string; message?: string };
      const errCode = fbErr?.code || '';
      const errMsg = fbErr?.message || String(err);
      if (
        errCode === 'auth/popup-closed-by-user' ||
        errCode === 'auth/cancelled-popup-request' ||
        errMsg.includes('popup-closed-by-user') ||
        errMsg.includes('cancelled-popup-request')
      ) {
        // User closed popup peacefully
        return;
      }
      console.warn('Switch account issue:', err);
      handleSetCurrentView('search');
    }
  };

  // Global Keyboard Shortcuts (Ctrl+K to focus search, Ctrl+M to toggle view, Esc to close modal)
  const isAnyModalOpen =
    isLoginModalOpen ||
    isScannerModalOpen ||
    isFiszkiBatchModalOpen ||
    !!fiszkaCarpet ||
    isStockExpoOpen ||
    isPowerBiAuditOpen ||
    isImportOpen ||
    isEditCarpetOpen ||
    isAdminManagementOpen ||
    isHelpOpen ||
    isActivityLogModalOpen ||
    isMoveModalOpen;

  const handleCloseAllModals = useCallback(() => {
    setIsLoginModalOpen(false);
    setIsScannerModalOpen(false);
    setIsFiszkiBatchModalOpen(false);
    setFiszkaCarpet(null);
    setIsStockExpoOpen(false);
    setIsPowerBiAuditOpen(false);
    setIsImportOpen(false);
    setIsEditCarpetOpen(false);
    setIsAdminManagementOpen(false);
    setIsHelpOpen(false);
    setIsActivityLogModalOpen(false);
    setIsMoveModalOpen(false);
  }, []);

  useKeyboardShortcuts({
    onSearchFocus: useCallback(() => {
      if (currentView !== 'search') {
        setCurrentView('search');
      }
      setTimeout(() => {
        const input = document.getElementById('search-input-field');
        input?.focus();
        if (input instanceof HTMLInputElement) {
          input.select();
        }
      }, 60);
    }, [currentView]),
    onToggleView: useCallback(() => {
      if (currentView === 'search') {
        if (!currentUser || !isAdmin) {
          setIsLoginModalOpen(true);
        } else {
          setCurrentView('warehouse');
        }
      } else {
        setCurrentView('search');
      }
    }, [currentView, currentUser, isAdmin]),
    onEscape: handleCloseAllModals,
    isAnyModalOpen
  });

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
      alert(`Tylko uprawnieni administratorzy sklepu ${STORE_NUMBER} mogą edytować magazyn.`);
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
      // Transactional integrity: prevents race conditions between multiple store associates
      const carpetRef = doc(db, 'carpets', docId);
      await runTransaction(db, async (transaction) => {
        await transaction.get(carpetRef);
        transaction.set(carpetRef, payload, { merge: true });
      });

      // Permanently remember carpet product in Master Catalog (retained even after shelf deletion)
      if (payload.nazwa && (payload.ean || payload.ref)) {
        saveProductToCatalog({
          nazwa: payload.nazwa,
          ref: payload.ref,
          ean: payload.ean,
          source: 'user',
          updatedBy: userEmail
        }).catch((err) => console.warn('Catalog save notice:', err));
      }

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

  // Update carpet specifications (sklad, wysokoscRuna, etc.) in Firestore
  const handleUpdateCarpetSpecs = async (carpetId: string, updates: Partial<Carpet>) => {
    try {
      if (isAdmin) {
        await setDoc(doc(db, 'carpets', carpetId), updates, { merge: true });
      }
      setCarpets((prev) =>
        prev.map((c) => (c.id === carpetId ? { ...c, ...updates } : c))
      );
      if (fiszkaCarpet && fiszkaCarpet.id === carpetId) {
        setFiszkaCarpet((prev) => (prev ? { ...prev, ...updates } : null));
      }
    } catch (err) {
      console.warn('Błąd aktualizacji parametrów w Firestore:', err);
    }
  };

  // Delete Carpet from shelf location (Master Catalog memory is permanently retained)
  const handleDeleteCarpet = async (carpetId: string, name: string) => {
    if (!isAdmin) {
      alert('Brak uprawnień do usuwania pozycji z magazynu.');
      return;
    }

    if (window.confirm(`Czy na pewno chcesz usunąć pozycję:\n"${name}"?\n\nUwaga: Pozycja zniknie z regału magazynowego, lecz dane produktu (nazwa, REF, EAN) pozostaną zapamiętane w pamięci sklepu.`)) {
      try {
        const carpetToDelete = carpets.find((c) => c.id === carpetId);
        await deleteDoc(doc(db, 'carpets', carpetId));
        await logActivity(
          'delete',
          name || `Dywan ${carpetId}`,
          `Zdjął dywan "${name}" z miejsca ${carpetToDelete?.miejsce || carpetId} (REF: ${carpetToDelete?.ref || '-'}, EAN: ${carpetToDelete?.ean || '-'}). Dane produktu pozostają zapamiętane w katalogu sklepu.`,
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

  // Open Move / Swap modal (Admins only)
  const handleOpenMoveModal = (carpet: Carpet) => {
    if (!isAdmin) {
      setIsLoginModalOpen(true);
      return;
    }
    setCarpetToMove(carpet);
    setIsMoveModalOpen(true);
  };

  // Move / Swap carpet execution
  const handleMoveOrSwapCarpet = async (
    sourceCarpet: Carpet,
    targetBay: number,
    targetSub: string,
    mode: 'move' | 'swap' | 'overwrite'
  ) => {
    if (!isAdmin) {
      alert(`Tylko uprawnieni administratorzy sklepu ${STORE_NUMBER} mogą przenosić dywany.`);
      return;
    }

    const userEmail = currentUser?.email || 'admin@leroymerlin.pl';
    const timestamp = new Date().toISOString();

    const sourcePlace = sourceCarpet.miejsce || `${sourceCarpet.magazyn} ${sourceCarpet.podmagazyn}`;
    const targetPlace = `${targetBay} ${targetSub}`;

    // Find any carpet occupying target slot
    const targetCarpet = carpets.find(
      (c) =>
        c.magazyn === targetBay &&
        (c.podmagazyn || '').trim().toUpperCase() === targetSub.trim().toUpperCase()
    );

    const targetDocId = `mag_${targetBay}_${targetSub.trim().toUpperCase()}`;
    const sourceDocId = `mag_${sourceCarpet.magazyn}_${sourceCarpet.podmagazyn.trim().toUpperCase()}`;

    try {
      await runTransaction(db, async (transaction) => {
        if (!targetCarpet || mode === 'move') {
          // 1. Move to empty slot
          const targetRef = doc(db, 'carpets', targetDocId);
          await transaction.get(targetRef);
          transaction.set(targetRef, {
            magazyn: targetBay,
            podmagazyn: targetSub.trim().toUpperCase(),
            miejsce: targetPlace,
            nazwa: sourceCarpet.nazwa || '',
            ref: sourceCarpet.ref || '',
            ean: sourceCarpet.ean || '',
            dodano: sourceCarpet.dodano || timestamp.split('T')[0],
            updatedAt: timestamp,
            updatedBy: userEmail
          });

          // Delete old location document
          transaction.delete(doc(db, 'carpets', sourceCarpet.id));
          if (sourceCarpet.id !== sourceDocId) {
            transaction.delete(doc(db, 'carpets', sourceDocId));
          }
        } else if (mode === 'swap') {
          // 2. SWAP: Both carpets exchange places
          const targetRef = doc(db, 'carpets', targetDocId);
          const sourceRef = doc(db, 'carpets', sourceDocId);
          await transaction.get(targetRef);
          await transaction.get(sourceRef);

          // Put source carpet in target slot
          transaction.set(targetRef, {
            magazyn: targetBay,
            podmagazyn: targetSub.trim().toUpperCase(),
            miejsce: targetPlace,
            nazwa: sourceCarpet.nazwa || '',
            ref: sourceCarpet.ref || '',
            ean: sourceCarpet.ean || '',
            dodano: sourceCarpet.dodano || timestamp.split('T')[0],
            updatedAt: timestamp,
            updatedBy: userEmail
          });

          // Put target carpet in source slot
          transaction.set(sourceRef, {
            magazyn: sourceCarpet.magazyn,
            podmagazyn: sourceCarpet.podmagazyn.trim().toUpperCase(),
            miejsce: sourcePlace,
            nazwa: targetCarpet.nazwa || '',
            ref: targetCarpet.ref || '',
            ean: targetCarpet.ean || '',
            dodano: targetCarpet.dodano || timestamp.split('T')[0],
            updatedAt: timestamp,
            updatedBy: userEmail
          });

          // Delete old non-standard doc IDs if different
          if (sourceCarpet.id !== targetDocId && sourceCarpet.id !== sourceDocId) {
            transaction.delete(doc(db, 'carpets', sourceCarpet.id));
          }
          if (targetCarpet.id !== targetDocId && targetCarpet.id !== sourceDocId) {
            transaction.delete(doc(db, 'carpets', targetCarpet.id));
          }
        } else if (mode === 'overwrite') {
          // 3. Overwrite
          const targetRef = doc(db, 'carpets', targetDocId);
          await transaction.get(targetRef);
          transaction.set(targetRef, {
            magazyn: targetBay,
            podmagazyn: targetSub.trim().toUpperCase(),
            miejsce: targetPlace,
            nazwa: sourceCarpet.nazwa || '',
            ref: sourceCarpet.ref || '',
            ean: sourceCarpet.ean || '',
            dodano: sourceCarpet.dodano || timestamp.split('T')[0],
            updatedAt: timestamp,
            updatedBy: userEmail
          });

          transaction.delete(doc(db, 'carpets', sourceCarpet.id));
          if (targetCarpet && targetCarpet.id !== targetDocId) {
            transaction.delete(doc(db, 'carpets', targetCarpet.id));
          }
        }
      });

      if (!targetCarpet || mode === 'move') {
        await logActivity(
          'edit',
          sourceCarpet.nazwa,
          `Przeniósł dywan "${sourceCarpet.nazwa}" z miejsca ${sourcePlace} na ${targetPlace} (REF: ${sourceCarpet.ref || '-'}, EAN: ${sourceCarpet.ean || '-'})`,
          { miejsce: targetPlace, ref: sourceCarpet.ref, ean: sourceCarpet.ean }
        );
      } else if (mode === 'swap') {
        await logActivity(
          'edit',
          `${sourceCarpet.nazwa} ⇄ ${targetCarpet.nazwa}`,
          `Zamienił miejscami dywany: "${sourceCarpet.nazwa}" (${sourcePlace} ➔ ${targetPlace}) oraz "${targetCarpet.nazwa}" (${targetPlace} ➔ ${sourcePlace})`,
          { miejsce: `${sourcePlace} ⇄ ${targetPlace}` }
        );
      } else if (mode === 'overwrite') {
        await logActivity(
          'edit',
          sourceCarpet.nazwa,
          `Przeniósł dywan "${sourceCarpet.nazwa}" na miejsce ${targetPlace}, zastępując i zdejmując z ekspozycji "${targetCarpet?.nazwa}"`,
          { miejsce: targetPlace, ref: sourceCarpet.ref, ean: sourceCarpet.ean }
        );
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, 'carpets/move-swap');
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

      // Update persistent product catalog with all imported carpets
      batchSaveToCatalog(
        incoming.map((c) => ({
          nazwa: c.nazwa,
          ref: c.ref,
          ean: c.ean,
          source: 'import',
          updatedBy: currentUser?.email || 'import'
        }))
      ).catch((err) => console.warn('Batch catalog save notice:', err));

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

  // Save or clear Power BI Stock Expo items in Firestore
  const handleSavePowerBiItems = async (items: PowerBiStockExpoItem[]) => {
    try {
      const docRef = doc(db, 'powerbi_stock_expo', `store_${STORE_NUMBER}`);
      const userEmail = currentUser?.email || 'admin@leroymerlin.pl';
      const timestamp = new Date().toISOString();

      if (items.length === 0) {
        await deleteDoc(docRef);
        await logActivity(
          'delete',
          'Power BI Stock Expo',
          `Wyczyścił bazę Power BI Stock Expo dla Sklepu ${STORE_NUMBER}`
        );
      } else {
        await setDoc(docRef, {
          items,
          totalCount: items.length,
          updatedAt: timestamp,
          updatedBy: userEmail,
          store: STORE_NUMBER
        });
        await logActivity(
          'edit',
          `Power BI Stock Expo (${items.length} pozycji)`,
          `Zaktualizował bazę Stock Expo z Power BI: ${items.length} pozycji dla Sklepu ${STORE_NUMBER}`
        );
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `powerbi_stock_expo/store_${STORE_NUMBER}`);
    }
  };

  // Quick assign a missing Power BI carpet to a free rack
  const handleAssignPowerBiItemToRack = (ref: string, nazwa: string) => {
    const occupiedKeys = new Set(
      carpets.map((c) => `${c.magazyn}_${(c.podmagazyn || 'A').toUpperCase()}`)
    );
    let suggestedBay = activeBays[0] || 1;
    let suggestedSub = 'A';

    for (const b of activeBays) {
      if (!occupiedKeys.has(`${b}_A`)) {
        suggestedBay = b;
        suggestedSub = 'A';
        break;
      }
      if (!occupiedKeys.has(`${b}_B`)) {
        suggestedBay = b;
        suggestedSub = 'B';
        break;
      }
    }

    setCarpetToEdit({
      magazyn: suggestedBay,
      podmagazyn: suggestedSub,
      miejsce: `${suggestedBay} ${suggestedSub}`,
      nazwa: nazwa || `Dywan ${ref}`,
      ref: ref,
      dodano: new Date().toISOString().split('T')[0]
    });
    setIsEditCarpetOpen(true);
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
      {/* CBA PWA Migration & Automatic Transition Assistant */}
      <Suspense fallback={null}>
        <CBAInstallTransitionModal />
      </Suspense>

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
        theme={theme}
        toggleTheme={toggleTheme}
      />

      {/* Unauthorized Warning Banner slot - persistent container keeps root DOM hierarchy unchanged */}
      <div key="app-banner-slot">
        {authRejectionError && (
          <div className="bg-red-50 border-b border-red-200 px-4 py-3 text-xs sm:text-sm text-red-950 flex flex-wrap items-center justify-between gap-3 shadow-2xs animate-in fade-in slide-in-from-top-2">
            <div className="flex items-center gap-2">
              <ShieldAlert className="w-5 h-5 text-red-600 shrink-0" />
              <div>{authRejectionError}</div>
            </div>
            <button
              onClick={() => setAuthRejectionError(null)}
              className="px-2.5 py-1 text-xs font-bold text-red-700 hover:bg-red-100 rounded-lg transition-colors cursor-pointer"
            >
              Zamknij
            </button>
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
              isAdmin={isAdmin}
              onMoveCarpet={handleOpenMoveModal}
            />
          </div>
        ) : authLoading ? (
          <div key="view-loading-wrapper" className="max-w-md mx-auto py-24 px-4 text-center flex flex-col items-center justify-center">
            <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4" />
            <h3 className="text-base font-bold text-gray-800">Sprawdzanie uprawnień administratora...</h3>
            <p className="text-xs text-gray-500 mt-1">Łączenie z bazą sklepu {STORE_NUMBER} w czasie rzeczywistym</p>
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
              onMoveProduct={handleOpenMoveModal}
              onOpenPowerBiAudit={() => setIsPowerBiAuditOpen(true)}
              pbiCount={powerBiItems.length}
            />
          </div>
        )}
      </main>

      {/* Offline connectivity indicator */}
      <OfflineIndicator />

      {/* Modals in a stable persistent container with Suspense for code-splitting */}
      <Suspense fallback={null}>
        <div key="app-modals">
      <LoginModal
        isOpen={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onGoogleSignIn={handleGoogleSignIn}
        currentUser={currentUser}
        isAdmin={isAdmin}
        isSuperAdmin={isSuperAdmin}
        superAdminEmail={superAdminEmail}
        onSwitchAccount={handleSwitchAccount}
        onLogout={handleLogout}
      />

      {isScannerModalOpen && (
        <BarcodeScannerModal
          isOpen={isScannerModalOpen}
          onClose={() => setIsScannerModalOpen(false)}
          onScanSuccess={handleScanSuccess}
        />
      )}

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
        onUpdateCarpet={handleUpdateCarpetSpecs}
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

      <PowerBiAuditModal
        isOpen={isPowerBiAuditOpen}
        onClose={() => setIsPowerBiAuditOpen(false)}
        carpets={carpets}
        pbiItems={powerBiItems}
        pbiUpdatedAt={powerBiUpdatedAt}
        pbiUpdatedBy={powerBiUpdatedBy}
        onSavePbiItems={handleSavePowerBiItems}
        onAssignToRack={handleAssignPowerBiItemToRack}
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
        superAdminEmail={superAdminEmail}
      />

      <ActivityLogModal
        isOpen={isActivityLogModalOpen}
        onClose={() => setIsActivityLogModalOpen(false)}
        logs={activityLogs}
        superAdminEmail={superAdminEmail}
      />

      <MoveCarpetModal
        isOpen={isMoveModalOpen}
        onClose={() => {
          setIsMoveModalOpen(false);
          setCarpetToMove(null);
        }}
        carpet={carpetToMove}
        activeBays={activeBays}
        allCarpets={carpets}
        onMoveOrSwap={handleMoveOrSwapCarpet}
      />
        </div>
      </Suspense>
    </div>
  );
}
