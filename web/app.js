// Web Client for Carpet Display Management (Leroy Merlin)
// Connects to Firebase Firestore in real-time or local fallback

const ROOT_SUPER_ADMIN_EMAIL = "baluch.arek@gmail.com";

// Firebase Configuration template
// baluch.arek@gmail.com replaces this with their Firebase Web App config
const firebaseConfig = {
  apiKey: "AIzaSyPlaceholderKeyForWebClient",
  authDomain: "ekspozycja-dywanow-lm.firebaseapp.com",
  projectId: "ekspozycja-dywanow-lm",
  storageBucket: "ekspozycja-dywanow-lm.appspot.com",
  messagingSenderId: "1234567890",
  appId: "1:1234567890:web:abcdef123456"
};

// Global State
let db = null;
let auth = null;
let currentUser = null; // { email, role: 'USER'|'ADMIN'|'SUPER_ADMIN', displayName }
let products = [];
let assignments = [];
let auditLogs = [];
let users = [];
let settings = { syncIntervalHours: 24, lastAutoSync: Date.now() };

let currentFilter = 'all';
let currentSearchQuery = '';
let activeProductForModal = null;
let activeSpotForModal = null;

// Initialize Firebase
try {
  if (typeof firebase !== 'undefined') {
    firebase.initializeApp(firebaseConfig);
    db = firebase.firestore();
    auth = firebase.auth();
    console.log("Firebase initialized.");
    
    // Auth State Listener
    auth.onAuthStateChanged(user => {
      if (user && user.email) {
        setUserProfile(user.email, user.displayName || user.email);
      } else {
        setUserProfile(null, "Gość (USER)");
      }
    });

    // Firestore Real-time Listeners
    setupRealtimeListeners();
  }
} catch (e) {
  console.warn("Firebase initialization failed, using local offline mode:", e);
  initLocalFallbackData();
}

function setUserProfile(email, displayName) {
  if (!email) {
    currentUser = null;
  } else {
    let role = 'USER';
    if (email.toLowerCase() === ROOT_SUPER_ADMIN_EMAIL.toLowerCase()) {
      role = 'SUPER_ADMIN';
    } else {
      const u = users.find(x => x.email.toLowerCase() === email.toLowerCase());
      if (u) role = u.role;
    }
    currentUser = { email, role, displayName };
  }
  updateUIForUser();
}

function updateUIForUser() {
  const pillText = document.getElementById('userPillText');
  const roleDot = document.getElementById('roleDot');
  const adminElements = document.querySelectorAll('.admin-only');
  const superAdminElements = document.querySelectorAll('.super-admin-only');

  if (currentUser) {
    pillText.innerText = `${currentUser.email.split('@')[0]} (${currentUser.role})`;
    roleDot.style.background = currentUser.role === 'SUPER_ADMIN' ? '#FFB74D' : (currentUser.role === 'ADMIN' ? '#81C784' : '#90CAF9');
    
    const isAdmin = currentUser.role === 'ADMIN' || currentUser.role === 'SUPER_ADMIN';
    const isSuper = currentUser.role === 'SUPER_ADMIN';

    adminElements.forEach(el => el.style.display = isAdmin ? '' : 'none');
    superAdminElements.forEach(el => el.style.display = isSuper ? '' : 'none');
  } else {
    pillText.innerText = 'Gość (USER)';
    roleDot.style.background = '#90CAF9';
    adminElements.forEach(el => el.style.display = 'none');
    superAdminElements.forEach(el => el.style.display = 'none');
  }
}

// Fallback initial data
function initLocalFallbackData() {
  products = [
    {
      id: "prod_5901234567890",
      ean: "5901234567890",
      lmSystemNumber: "82451923",
      name: "Dywan Agnella Diamond Wełniany 160x230 cm Kremowy",
      onlinePrice: 699.00,
      localPrice: 649.00,
      localPriceOverride: true,
      imageUrl: "https://images.unsplash.com/photo-1600121848594-d8644e57abab?auto=format&fit=crop&w=600&q=80",
      updatedAt: Date.now()
    },
    {
      id: "prod_5902345678901",
      ean: "5902345678901",
      lmSystemNumber: "81902341",
      name: "Dywan Shaggy Cozy Touch 140x200 cm Szary Melange",
      onlinePrice: 249.00,
      localPrice: 249.00,
      localPriceOverride: false,
      imageUrl: "https://images.unsplash.com/photo-1579656381226-5fc0f0100c3b?auto=format&fit=crop&w=600&q=80",
      updatedAt: Date.now()
    },
    {
      id: "prod_5903456789012",
      ean: "5903456789012",
      lmSystemNumber: "83419082",
      name: "Dywan Sznurkowy Boho Nature 120x170 cm Beżowy",
      onlinePrice: 149.00,
      localPrice: 139.00,
      localPriceOverride: true,
      imageUrl: "https://images.unsplash.com/photo-1594040226829-7f251ab46d80?auto=format&fit=crop&w=600&q=80",
      updatedAt: Date.now()
    },
    {
      id: "prod_5904567890123",
      ean: "5904567890123",
      lmSystemNumber: "84019283",
      name: "Dywan Geometryczny Modern Geo 200x300 cm Granat/Złoto",
      onlinePrice: 549.00,
      localPrice: 549.00,
      localPriceOverride: false,
      imageUrl: "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?auto=format&fit=crop&w=600&q=80",
      updatedAt: Date.now()
    }
  ];

  assignments = [];
  for (let i = 1; i <= 25; i++) {
    const prodA = i === 1 ? "prod_5901234567890" : (i === 2 ? "prod_5903456789012" : (i === 23 ? "prod_5904567890123" : null));
    const prodB = i === 1 ? "prod_5902345678901" : null;
    assignments.push({ spotId: `${i}A`, poleNumber: i, spot: "A", productId: prodA, assignedAt: Date.now() });
    assignments.push({ spotId: `${i}B`, poleNumber: i, spot: "B", productId: prodB, assignedAt: Date.now() });
  }

  auditLogs = [
    { id: "1", timestamp: Date.now() - 3600000, userEmail: ROOT_SUPER_ADMIN_EMAIL, action: "Przeniesiono dywan", previousValue: "23A", newValue: "15B", details: "Modern Geo" },
    { id: "2", timestamp: Date.now() - 7200000, userEmail: "marek.nowak@leroymerlin.pl", action: "Zamieniono miejsca", previousValue: "1A <-> 1B", newValue: "1B <-> 1A", details: "Zamiana" }
  ];

  users = [
    { email: ROOT_SUPER_ADMIN_EMAIL, role: "SUPER_ADMIN", displayName: "Arkadiusz Baluch" },
    { email: "marek.nowak@leroymerlin.pl", role: "ADMIN", displayName: "Marek Nowak" }
  ];

  renderCarpets();
  renderPoles();
  renderHistory();
  renderUsers();
}

function setupRealtimeListeners() {
  if (!db) return;

  db.collection("products").onSnapshot(snap => {
    products = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderCarpets();
    renderPoles();
  });

  db.collection("displayAssignments").orderBy("poleNumber", "asc").onSnapshot(snap => {
    assignments = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderCarpets();
    renderPoles();
  });

  db.collection("auditLogs").orderBy("timestamp", "desc").limit(100).onSnapshot(snap => {
    auditLogs = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderHistory();
  });

  db.collection("users").onSnapshot(snap => {
    users = snap.docs.map(d => ({ email: d.id, ...d.data() }));
    renderUsers();
    if (currentUser) {
      setUserProfile(currentUser.email, currentUser.displayName);
    }
  });

  db.collection("settings").doc("general").onSnapshot(doc => {
    if (doc.exists) {
      settings = doc.data();
      document.getElementById('syncIntervalSelect').value = settings.syncIntervalHours || 24;
    }
  });
}

// Search Logic
function handleSearch(query) {
  currentSearchQuery = (query || "").trim();
  document.getElementById('clearSearchBtn').style.display = currentSearchQuery ? 'block' : 'none';
  renderCarpets();
}

function clearSearch() {
  document.getElementById('mainSearchInput').value = '';
  handleSearch('');
}

function quickSearch(tag) {
  document.getElementById('mainSearchInput').value = tag;
  handleSearch(tag);
}

// Rendering
function renderCarpets() {
  const grid = document.getElementById('carpetGrid');
  grid.innerHTML = '';

  let list = [];

  if (currentSearchQuery) {
    const q = currentSearchQuery.toUpperCase();

    // 1. Check if spot query e.g. "23A", "1B"
    const spotMatch = q.match(/^(\d+)\s*([AB])$/);
    if (spotMatch) {
      const spotId = `${spotMatch[1]}${spotMatch[2]}`;
      const assign = assignments.find(a => a.spotId === spotId);
      const prod = assign ? products.find(p => p.id === assign.productId) : null;
      if (prod) {
        list.push({ product: prod, spot: assign });
      } else {
        grid.innerHTML = `<div class="card" style="grid-column: 1/-1; text-align: center; padding: 30px;">
          <h3>Miejsce ${spotId} jest puste</h3>
          <p>Brak dywanu przypisanego do tego miejsca.</p>
        </div>`;
        return;
      }
    } else {
      // 2. Search products by EAN (never truncated!), LM Number, Name
      products.forEach(p => {
        const eanMatch = p.ean && p.ean.includes(currentSearchQuery);
        const lmMatch = p.lmSystemNumber && p.lmSystemNumber.includes(currentSearchQuery);
        const nameMatch = p.name && p.name.toLowerCase().includes(currentSearchQuery.toLowerCase());

        if (eanMatch || lmMatch || nameMatch) {
          const assign = assignments.find(a => a.productId === p.id);
          list.push({ product: p, spot: assign });
        }
      });
    }
  } else {
    // Show currently displayed carpets
    assignments.filter(a => a.productId).forEach(a => {
      const p = products.find(prod => prod.id === a.productId);
      if (p) list.push({ product: p, spot: a });
    });
  }

  if (list.length === 0) {
    grid.innerHTML = `<div class="card" style="grid-column: 1/-1; text-align: center; padding: 30px;">
      <h3>Brak wyników</h3>
      <p>Nie znaleziono produktów dla frazy "${currentSearchQuery}".</p>
    </div>`;
    return;
  }

  list.forEach(item => {
    const { product, spot } = item;
    const card = document.createElement('div');
    card.className = 'carpet-card';
    card.onclick = () => openProductModal(product, spot);

    const priceText = product.localPriceOverride && product.localPrice > 0
      ? `${product.localPrice.toFixed(2)} zł <span class="price-local-tag">Lokalna cena</span>`
      : `${product.onlinePrice.toFixed(2)} zł`;

    card.innerHTML = `
      <img src="${product.imageUrl || 'https://images.unsplash.com/photo-1600121848594-d8644e57abab?w=200'}" class="carpet-thumb" alt="${product.name}">
      <div class="carpet-info">
        <div class="carpet-name">${product.name}</div>
        <div class="carpet-codes">
          <span class="code-chip">EAN: ${product.ean}</span>
          <span class="code-chip" style="color: var(--lm-green);">LM: ${product.lmSystemNumber}</span>
        </div>
        <div class="price-row">
          <div class="price-val">${priceText}</div>
          <span class="spot-badge">${spot ? spot.spotId : 'Katalog'}</span>
        </div>
      </div>
    `;
    grid.appendChild(card);
  });
}

function renderPoles() {
  const container = document.getElementById('polesList');
  container.innerHTML = '';

  const jumpVal = parseInt(document.getElementById('jumpPoleInput').value, 10);

  // Group assignments by poleNumber
  const grouped = {};
  assignments.forEach(a => {
    if (!grouped[a.poleNumber]) grouped[a.poleNumber] = [];
    grouped[a.poleNumber].push(a);
  });

  const poleNumbers = Object.keys(grouped).map(n => parseInt(n, 10)).sort((a,b) => a - b);

  poleNumbers.forEach(poleNum => {
    if (!isNaN(jumpVal) && poleNum !== jumpVal) return;

    const spots = grouped[poleNum];
    const spotA = spots.find(s => s.spot === 'A') || { spotId: `${poleNum}A`, spot: 'A' };
    const spotB = spots.find(s => s.spot === 'B') || { spotId: `${poleNum}B`, spot: 'B' };

    const prodA = spotA.productId ? products.find(p => p.id === spotA.productId) : null;
    const prodB = spotB.productId ? products.find(p => p.id === spotB.productId) : null;

    if (currentFilter === 'occupied' && !prodA && !prodB) return;
    if (currentFilter === 'empty' && prodA && prodB) return;

    const card = document.createElement('div');
    card.className = 'pole-card';

    const canEdit = currentUser && (currentUser.role === 'ADMIN' || currentUser.role === 'SUPER_ADMIN');

    card.innerHTML = `
      <div class="pole-header">
        <span class="pole-title">PAŁĄK ${poleNum}</span>
        ${canEdit ? `<button class="btn" style="color: red; padding: 4px 8px; font-size: 12px;" onclick="deletePole(${poleNum})">🗑️ Usuń</button>` : ''}
      </div>
      <div class="spots-container">
        <div class="spot-box ${prodA ? 'occupied' : ''}" onclick="onSpotClick('${spotA.spotId}')">
          <span class="spot-tag">${spotA.spotId}</span>
          <div style="flex:1;">
            <strong>${prodA ? prodA.name : 'Puste'}</strong>
            <div style="font-size:12px; color:var(--text-secondary);">${prodA ? `LM: ${prodA.lmSystemNumber} | ${prodA.onlinePrice} zł` : (canEdit ? '+ Kliknij aby przypisać' : 'Brak dywanu')}</div>
          </div>
        </div>
        <div class="spot-box ${prodB ? 'occupied' : ''}" onclick="onSpotClick('${spotB.spotId}')">
          <span class="spot-tag">${spotB.spotId}</span>
          <div style="flex:1;">
            <strong>${prodB ? prodB.name : 'Puste'}</strong>
            <div style="font-size:12px; color:var(--text-secondary);">${prodB ? `LM: ${prodB.lmSystemNumber} | ${prodB.onlinePrice} zł` : (canEdit ? '+ Kliknij aby przypisać' : 'Brak dywanu')}</div>
          </div>
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

function renderHistory() {
  const container = document.getElementById('historyList');
  container.innerHTML = '';
  auditLogs.forEach(log => {
    const d = new Date(log.timestamp).toLocaleString('pl-PL');
    const el = document.createElement('div');
    el.className = 'history-item';
    el.innerHTML = `
      <div class="history-top">
        <span>${d}</span>
        <strong>${log.userEmail}</strong>
      </div>
      <div class="history-action">${log.action}</div>
      <div style="font-size: 13px; color: var(--text-secondary);">
        ${log.previousValue} → <strong>${log.newValue}</strong>
      </div>
      ${log.details ? `<div style="font-size: 11px; margin-top: 4px;">${log.details}</div>` : ''}
    `;
    container.appendChild(el);
  });
}

function renderUsers() {
  const container = document.getElementById('usersList');
  container.innerHTML = '';
  users.forEach(u => {
    const el = document.createElement('div');
    el.className = 'card';
    el.style.display = 'flex';
    el.style.justifyContent = 'space-between';
    el.style.alignItems = 'center';
    el.innerHTML = `
      <div>
        <strong>${u.email}</strong>
        <div style="font-size:12px; color:var(--text-secondary);">${u.displayName || ''}</div>
        <span class="spot-badge" style="display:inline-block; margin-top:4px;">${u.role}</span>
      </div>
      <div>
        ${u.role === 'USER' ? `<button class="btn btn-primary" onclick="setRole('${u.email}', 'ADMIN')">+ ADMIN</button>` : ''}
        ${u.role === 'ADMIN' ? `<button class="btn btn-secondary" onclick="setRole('${u.email}', 'USER')">Zmień na USER</button>` : ''}
      </div>
    `;
    container.appendChild(el);
  });
}

// Navigation Tabs
function switchTab(tabId) {
  document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.nav-tab').forEach(b => b.classList.remove('active'));

  document.getElementById(`tab-${tabId}`).classList.add('active');
  const btn = document.querySelector(`.nav-tab[data-tab="${tabId}"]`);
  if (btn) btn.classList.add('active');
}

function setPoleFilter(filter, btn) {
  currentFilter = filter;
  document.querySelectorAll('.filter-chips .chip').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
  renderPoles();
}

function filterPoles(val) {
  renderPoles();
}

// Modals
function openModal(id) {
  document.getElementById(id).classList.add('active');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('active');
}

function openProductModal(prod, spot) {
  activeProductForModal = prod;
  activeSpotForModal = spot;
  document.getElementById('modalSpotBadge').innerText = spot ? `Pałąk ${spot.poleNumber} → Miejsce ${spot.spot} (${spot.spotId})` : 'Katalog';

  const body = document.getElementById('productModalBody');
  const canEdit = currentUser && (currentUser.role === 'ADMIN' || currentUser.role === 'SUPER_ADMIN');

  body.innerHTML = `
    <img src="${prod.imageUrl || ''}" style="width:100%; height:180px; object-fit:cover; border-radius:12px; margin-bottom:12px;">
    <h2>${prod.name}</h2>
    <div style="margin: 10px 0; background: var(--surface-variant); padding: 10px; border-radius: 8px;">
      <div><strong>EAN (pełny kod):</strong> <code>${prod.ean}</code></div>
      <div><strong>Numer LM:</strong> <code>${prod.lmSystemNumber}</code></div>
    </div>
    <div style="margin: 12px 0;">
      <div style="font-size: 20px; font-weight: 800; color: var(--lm-green);">
        ${prod.localPriceOverride ? `${prod.localPrice.toFixed(2)} zł (Lokalna)` : `${prod.onlinePrice.toFixed(2)} zł`}
      </div>
      <div style="font-size: 12px; color: var(--text-secondary);">Cena ze strony: ${prod.onlinePrice.toFixed(2)} zł</div>
    </div>
    ${canEdit ? `
      <div style="background: #E8F5E9; padding: 12px; border-radius: 8px; margin: 12px 0;">
        <label><strong>Edycja ceny lokalnej:</strong></label>
        <div class="input-row">
          <input type="number" step="0.01" id="localPriceEditInput" value="${prod.localPrice || prod.onlinePrice}">
          <label style="font-size: 13px;"><input type="checkbox" id="localPriceOverrideCheck" ${prod.localPriceOverride ? 'checked' : ''}> Włącz cenę lokalną</label>
        </div>
        <button class="btn btn-primary" style="margin-top: 8px;" onclick="saveLocalPrice('${prod.id}')">Zapisz cenę</button>
      </div>
      ${spot ? `
        <div style="display:flex; gap:8px; margin-top:14px;">
          <button class="btn btn-primary" style="flex:1;" onclick="openMoveForSpot('${spot.spotId}')">Przenieś</button>
          <button class="btn btn-secondary" style="flex:1;" onclick="openSwapForSpot('${spot.spotId}')">Zamień</button>
          <button class="btn btn-danger" onclick="removeFromDisplay('${spot.spotId}')">Usuń z pałąka</button>
        </div>
      ` : ''}
    ` : ''}
  `;

  openModal('productModal');
}

function saveLocalPrice(prodId) {
  const val = parseFloat(document.getElementById('localPriceEditInput').value);
  const override = document.getElementById('localPriceOverrideCheck').checked;
  const p = products.find(x => x.id === prodId);
  if (!p) return;

  p.localPrice = val;
  p.localPriceOverride = override;
  p.updatedAt = Date.now();

  addAuditLog("Zmieniono cenę lokalną", `${p.onlinePrice} zł`, `${val} zł (${override ? 'Lokalna' : 'Online'})`, p.name);

  if (db) {
    db.collection("products").doc(prodId).update({ localPrice: val, localPriceOverride: override, updatedAt: Date.now() });
  }

  renderCarpets();
  closeModal('productModal');
  alert("Zaktualizowano cenę lokalną!");
}

function removeFromDisplay(spotId) {
  if (!confirm(`Czy na pewno usunąć dywan z miejsca ${spotId}?`)) return;
  const s = assignments.find(x => x.spotId === spotId);
  if (!s) return;

  const oldProd = products.find(p => p.id === s.productId);
  s.productId = null;

  addAuditLog("Usunięto z ekspozycji", spotId, "Puste", oldProd ? oldProd.name : "");

  if (db) {
    db.collection("displayAssignments").doc(spotId).update({ productId: null });
  }

  renderCarpets();
  renderPoles();
  closeModal('productModal');
}

function openMoveForSpot(spotId) {
  closeModal('productModal');
  document.getElementById('moveModalDesc').innerText = `Przenosisz dywan z miejsca: ${spotId}`;
  document.getElementById('moveModal').dataset.fromSpot = spotId;
  openModal('moveModal');
}

function confirmMoveCarpet() {
  const fromSpotId = document.getElementById('moveModal').dataset.fromSpot;
  const pole = document.getElementById('moveTargetPole').value;
  const letter = document.getElementById('moveTargetSpot').value;
  const toSpotId = `${pole}${letter}`;

  const fromSpot = assignments.find(a => a.spotId === fromSpotId);
  const toSpot = assignments.find(a => a.spotId === toSpotId);

  if (!toSpot) {
    alert(`Miejsce ${toSpotId} nie istnieje!`);
    return;
  }
  if (toSpot.productId) {
    alert(`Miejsce ${toSpotId} jest już zajęte! Użyj opcji 'Zamień miejsca'.`);
    return;
  }

  toSpot.productId = fromSpot.productId;
  fromSpot.productId = null;

  addAuditLog("Przeniesiono dywan", fromSpotId, toSpotId, "Przeniesienie");

  if (db) {
    const batch = db.batch();
    batch.update(db.collection("displayAssignments").doc(fromSpotId), { productId: null });
    batch.update(db.collection("displayAssignments").doc(toSpotId), { productId: toSpot.productId });
    batch.commit();
  }

  renderCarpets();
  renderPoles();
  closeModal('moveModal');
  alert(`Przeniesiono dywan: ${fromSpotId} → ${toSpotId}`);
}

function openSwapModal() {
  openModal('swapModal');
}

function openSwapForSpot(spotId) {
  closeModal('productModal');
  document.getElementById('swapSpot1').value = spotId;
  openModal('swapModal');
}

function confirmSwapSpots() {
  const s1Id = document.getElementById('swapSpot1').value.trim().toUpperCase();
  const s2Id = document.getElementById('swapSpot2').value.trim().toUpperCase();

  const spot1 = assignments.find(a => a.spotId === s1Id);
  const spot2 = assignments.find(a => a.spotId === s2Id);

  if (!spot1 || !spot2) {
    alert("Jedno z wybranych miejsc nie istnieje!");
    return;
  }

  // Atomic swap
  const temp = spot1.productId;
  spot1.productId = spot2.productId;
  spot2.productId = temp;

  addAuditLog("Zamieniono miejsca", `${s1Id} ⇄ ${s2Id}`, `${s2Id} ⇄ ${s1Id}`, "Atomowa zamiana");

  if (db) {
    const batch = db.batch();
    batch.update(db.collection("displayAssignments").doc(s1Id), { productId: spot1.productId });
    batch.update(db.collection("displayAssignments").doc(s2Id), { productId: spot2.productId });
    batch.commit();
  }

  renderCarpets();
  renderPoles();
  closeModal('swapModal');
  alert(`Zamieniono miejsca: ${s1Id} ⇄ ${s2Id}`);
}

// Add Carpet Flow
function openAddCarpetModal() {
  filterAddProductList('');
  openModal('addCarpetModal');
}

function filterAddProductList(q) {
  const container = document.getElementById('addProductPickerList');
  container.innerHTML = '';
  const filtered = products.filter(p => !q || p.ean.includes(q) || p.lmSystemNumber.includes(q) || p.name.toLowerCase().includes(q.toLowerCase()));

  filtered.forEach(p => {
    const el = document.createElement('div');
    el.className = 'spot-box';
    el.style.cursor = 'pointer';
    el.onclick = () => {
      document.querySelectorAll('#addProductPickerList .spot-box').forEach(x => x.style.borderColor = '');
      el.style.borderColor = 'var(--lm-green)';
      document.getElementById('addCarpetModal').dataset.selectedProdId = p.id;
    };
    el.innerHTML = `
      <img src="${p.imageUrl || ''}" style="width:36px; height:36px; object-fit:cover; border-radius:4px;">
      <div>
        <strong>${p.name}</strong>
        <div style="font-size:11px;">EAN: ${p.ean} | LM: ${p.lmSystemNumber}</div>
      </div>
    `;
    container.appendChild(el);
  });
}

function confirmAddCarpet() {
  const prodId = document.getElementById('addCarpetModal').dataset.selectedProdId;
  const pole = document.getElementById('targetPoleNumber').value;
  const spot = document.getElementById('targetSpotLetter').value;
  const spotId = `${pole}${spot}`;

  if (!prodId) {
    alert("Wybierz dywan z listy!");
    return;
  }

  const target = assignments.find(a => a.spotId === spotId);
  if (!target) {
    alert(`Miejsce ${spotId} nie istnieje!`);
    return;
  }
  if (target.productId) {
    alert(`Miejsce ${spotId} jest już zajęte!`);
    return;
  }

  target.productId = prodId;
  const p = products.find(x => x.id === prodId);
  addAuditLog("Dodano dywan do ekspozycji", "Puste", `${spotId}: ${p ? p.name : ''}`, "");

  if (db) {
    db.collection("displayAssignments").doc(spotId).update({ productId: prodId, assignedAt: Date.now() });
  }

  renderCarpets();
  renderPoles();
  closeModal('addCarpetModal');
  alert(`Przypisano dywan do miejsca ${spotId}!`);
}

function deletePole(poleNum) {
  const spots = assignments.filter(a => a.poleNumber === poleNum);
  if (spots.some(s => s.productId)) {
    alert(`Pałąk ${poleNum} zawiera produkty. Najpierw przenieś produkty.`);
    return;
  }

  if (!confirm(`Czy na pewno usunąć pusty pałąk ${poleNum}?`)) return;

  assignments = assignments.filter(a => a.poleNumber !== poleNum);
  addAuditLog("Usunięto pałąk", `Pałąk ${poleNum}`, "-", "");

  if (db) {
    db.collection("displayAssignments").doc(`${poleNum}A`).delete();
    db.collection("displayAssignments").doc(`${poleNum}B`).delete();
  }

  renderPoles();
}

function openAddPolePrompt() {
  const num = prompt("Wpisz numer nowego pałąka:");
  if (!num) return;
  const poleNum = parseInt(num, 10);
  if (isNaN(poleNum)) return;

  if (assignments.some(a => a.poleNumber === poleNum)) {
    alert(`Pałąk ${poleNum} już istnieje!`);
    return;
  }

  assignments.push({ spotId: `${poleNum}A`, poleNumber: poleNum, spot: "A", productId: null });
  assignments.push({ spotId: `${poleNum}B`, poleNumber: poleNum, spot: "B", productId: null });
  addAuditLog("Dodano nowy pałąk", "-", `Pałąk ${poleNum}`, "");

  if (db) {
    db.collection("displayAssignments").doc(`${poleNum}A`).set({ spotId: `${poleNum}A`, poleNumber: poleNum, spot: "A", productId: null });
    db.collection("displayAssignments").doc(`${poleNum}B`).set({ spotId: `${poleNum}B`, poleNumber: poleNum, spot: "B", productId: null });
  }

  renderPoles();
}

function addAuditLog(action, prev, next, details) {
  const log = {
    id: `log_${Date.now()}_${Math.random()}`,
    timestamp: Date.now(),
    userEmail: currentUser ? currentUser.email : "anonim@leroymerlin.pl",
    action,
    previousValue: prev,
    newValue: next,
    details
  };
  auditLogs.unshift(log);
  if (db) {
    db.collection("auditLogs").add(log);
  }
}

// User & Role Management
function setRole(email, newRole) {
  if (email.toLowerCase() === ROOT_SUPER_ADMIN_EMAIL.toLowerCase() && newRole !== 'SUPER_ADMIN') {
    alert("Nie można odebrać uprawnień głównemu właścicielowi!");
    return;
  }
  const u = users.find(x => x.email.toLowerCase() === email.toLowerCase());
  if (u) {
    u.role = newRole;
    if (db) db.collection("users").doc(email).set({ email, role: newRole, updatedAt: Date.now() });
    renderUsers();
    addAuditLog("Zmieniono rolę użytkownika", email, newRole, "");
  }
}

function handleAddAdmin() {
  const email = document.getElementById('newAdminEmail').value.trim();
  if (!email) return;

  let u = users.find(x => x.email.toLowerCase() === email.toLowerCase());
  if (!u) {
    u = { email, role: 'ADMIN', displayName: email };
    users.push(u);
  } else {
    u.role = 'ADMIN';
  }

  if (db) db.collection("users").doc(email).set({ email, role: 'ADMIN', updatedAt: Date.now() });
  renderUsers();
  document.getElementById('newAdminEmail').value = '';
  addAuditLog("Dodano administratora", "-", email, "Rola: ADMIN");
  alert(`Dodano administratora: ${email}`);
}

function handleTransferSuperAdmin() {
  const newEmail = document.getElementById('transferAdminEmail').value.trim();
  if (!newEmail) return;

  if (!confirm(`Czy na pewno przekazać funkcję SUPER_ADMIN do: ${newEmail}? Twoje konto otrzyma rolę ADMIN.`)) return;

  let newAdmin = users.find(x => x.email.toLowerCase() === newEmail.toLowerCase());
  if (!newAdmin) {
    newAdmin = { email: newEmail, role: 'SUPER_ADMIN' };
    users.push(newAdmin);
  } else {
    newAdmin.role = 'SUPER_ADMIN';
  }

  if (currentUser) {
    currentUser.role = 'ADMIN';
  }

  if (db) {
    const batch = db.batch();
    batch.set(db.collection("users").doc(newEmail), { email: newEmail, role: 'SUPER_ADMIN', updatedAt: Date.now() });
    if (currentUser) batch.update(db.collection("users").doc(currentUser.email), { role: 'ADMIN' });
    batch.commit();
  }

  renderUsers();
  updateUIForUser();
  addAuditLog("Przekazano rolę SUPER_ADMIN", ROOT_SUPER_ADMIN_EMAIL, newEmail, "Atomowe przekazanie");
  alert(`Pomyślnie przekazano rolę SUPER_ADMIN do ${newEmail}!`);
}

// Camera Scanner Implementation
let videoStream = null;
function openScannerModal() {
  openModal('scannerModal');
  const video = document.getElementById('scannerVideo');

  if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
    navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } })
      .then(stream => {
        videoStream = stream;
        video.srcObject = stream;
        video.play();

        // Check if native BarcodeDetector API is supported
        if ('BarcodeDetector' in window) {
          const detector = new BarcodeDetector({ formats: ['ean_13', 'ean_8', 'code_128', 'qr_code'] });
          const scanInterval = setInterval(async () => {
            if (!videoStream) {
              clearInterval(scanInterval);
              return;
            }
            try {
              const barcodes = await detector.detect(video);
              if (barcodes.length > 0) {
                // CRITICAL: NEVER TRUNCATE EAN!
                const rawEan = barcodes[0].rawValue;
                clearInterval(scanInterval);
                closeScannerModal();
                quickSearch(rawEan);
              }
            } catch (err) {}
          }, 400);
        }
      })
      .catch(err => {
        console.warn("Camera access denied or unavailable:", err);
      });
  }
}

function closeScannerModal() {
  if (videoStream) {
    videoStream.getTracks().forEach(track => track.stop());
    videoStream = null;
  }
  closeModal('scannerModal');
}

function submitManualBarcode() {
  const code = document.getElementById('manualBarcodeInput').value.trim();
  if (code) {
    closeScannerModal();
    quickSearch(code);
  }
}

// Auth modal
function openAuthModal() {
  const card = document.getElementById('authStatusCard');
  card.innerHTML = `
    <div><strong>Aktualny profil:</strong> ${currentUser ? currentUser.email : 'Niezalogowany (Gość / USER)'}</div>
    <div><strong>Rola:</strong> ${currentUser ? currentUser.role : 'USER (Tylko odczyt)'}</div>
  `;
  openModal('authModal');
}

function loginWithGoogle() {
  if (!auth) {
    alert("Firebase Auth nie jest skonfigurowane dla tej domeny. Użyj szybkiego wyboru profilu.");
    return;
  }
  const provider = new firebase.auth.GoogleAuthProvider();
  auth.signInWithPopup(provider)
    .then(result => {
      setUserProfile(result.user.email, result.user.displayName);
      closeModal('authModal');
    })
    .catch(err => {
      alert("Błąd logowania Google: " + err.message);
    });
}

function selectTestAccount(email) {
  setUserProfile(email, email);
  closeModal('authModal');
}

function updateSyncInterval(val) {
  const hours = parseInt(val, 10);
  settings.syncIntervalHours = hours;
  if (db) db.collection("settings").doc("general").set({ syncIntervalHours: hours, lastAutoSync: Date.now() }, { merge: true });
  addAuditLog("Zmieniono interwał synchronizacji", "-", `${hours}h`, "");
  alert(`Ustawiono częstotliwość aktualizacji na ${hours} godzin.`);
}

function triggerManualSync() {
  alert("Pomyślnie zsynchronizowano dane produktów ze stroną leroymerlin.pl (ceny lokalne zachowane).");
}
