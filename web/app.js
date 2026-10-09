// Firebase configuration matching project google-services.json
const firebaseConfig = {
  apiKey: "AIzaSyBfmbYlKbCZu81TsjSoCuHixI0Bdxpeh8Y",
  authDomain: "gen-lang-client-0789992790.firebaseapp.com",
  projectId: "gen-lang-client-0789992790",
  storageBucket: "gen-lang-client-0789992790.firebasestorage.app",
  messagingSenderId: "448858732402",
  appId: "1:448858732402:android:73c5582bd06a766a0a0feb"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
// Connect to the specific named Firestore database
const db = firebase.app().firestore("ai-studio-android-ekspozyc-faa4fd35-3317-4477-b9ae-5449adb70e3d");

// Application state
let currentUser = null;
let userRole = "USER";
let poles = [];
let displayAssignments = [];
let products = [];
let currentFilter = "ALL";
let currentSearchQuery = "";
let currentJumpPole = "";

// Auth listener
auth.onAuthStateChanged(async (user) => {
  if (user) {
    currentUser = user;
    const isMasterEmail = user.email.toLowerCase() === "baluch.arek@gmail.com";
    
    // Fetch user profile from Firestore
    try {
      const userDoc = await db.collection("users").doc(user.uid).get();
      if (userDoc.exists) {
        userRole = isMasterEmail ? "SUPER_ADMIN" : (userDoc.data().role || "USER");
      } else {
        userRole = isMasterEmail ? "SUPER_ADMIN" : "USER";
        // Create user document
        await db.collection("users").doc(user.uid).set({
          userId: user.uid,
          email: user.email,
          displayName: user.displayName || "",
          role: userRole,
          active: true,
          createdAt: firebase.firestore.FieldValue.serverTimestamp()
        });
      }
    } catch (e) {
      console.warn("Błąd pobierania roli, używam domyślnej:", e);
      userRole = isMasterEmail ? "SUPER_ADMIN" : "USER";
    }

    renderAuthUI();
  } else {
    currentUser = null;
    userRole = "USER";
    renderAuthUI();
  }
});

function renderAuthUI() {
  const loginBtn = document.getElementById("loginBtn");
  const userInfo = document.getElementById("userInfo");
  const userEmail = document.getElementById("userEmail");
  const userRoleSpan = document.getElementById("userRole");
  const adminToolbar = document.getElementById("adminToolbar");
  const usersBtn = document.getElementById("usersBtn");

  if (currentUser) {
    loginBtn.classList.add("hidden");
    userInfo.classList.remove("hidden");
    userEmail.textContent = currentUser.email;
    userRoleSpan.textContent = userRole;

    const isAdmin = userRole === "ADMIN" || userRole === "SUPER_ADMIN";
    if (isAdmin) {
      adminToolbar.classList.remove("hidden");
      if (userRole === "SUPER_ADMIN") {
        usersBtn.classList.remove("hidden");
      } else {
        usersBtn.classList.add("hidden");
      }
    } else {
      adminToolbar.classList.add("hidden");
    }
  } else {
    loginBtn.classList.remove("hidden");
    userInfo.classList.add("hidden");
    adminToolbar.classList.add("hidden");
  }

  renderPoles();
  if (currentSearchQuery) handleSearch();
}

// Google Sign-In
async function loginWithGoogle() {
  const provider = new firebase.auth.GoogleAuthProvider();
  try {
    await auth.signInWithPopup(provider);
  } catch (err) {
    alert("Błąd logowania: " + err.message);
  }
}

async function logout() {
  await auth.signOut();
}

// Real-time Firestore Listeners
db.collection("poles").orderBy("number").onSnapshot((snapshot) => {
  poles = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
  renderPoles();
});

db.collection("displayAssignments").onSnapshot((snapshot) => {
  displayAssignments = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
  renderPoles();
  if (currentSearchQuery) handleSearch();
});

db.collection("products").onSnapshot((snapshot) => {
  products = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
  if (currentSearchQuery) handleSearch();
});

// Render Poles Board
function renderPoles() {
  const grid = document.getElementById("polesGrid");
  grid.innerHTML = "";

  const filteredPoles = poles.filter(pole => {
    if (currentJumpPole && pole.number != parseInt(currentJumpPole, 10)) {
      return false;
    }
    const spotA = displayAssignments.find(a => a.poleNumber === pole.number && a.position === "A");
    const spotB = displayAssignments.find(a => a.poleNumber === pole.number && a.position === "B");
    const occA = spotA && spotA.productId;
    const occB = spotB && spotB.productId;

    if (currentFilter === "OCCUPIED") return occA || occB;
    if (currentFilter === "FREE") return !occA || !occB;
    return true;
  });

  if (filteredPoles.length === 0) {
    grid.innerHTML = `<p style="padding: 20px; color: var(--text-muted);">Brak pałąków spełniających kryteria.</p>`;
    return;
  }

  filteredPoles.forEach(pole => {
    const spotA = displayAssignments.find(a => a.poleNumber === pole.number && a.position === "A") || { poleNumber: pole.number, position: "A" };
    const spotB = displayAssignments.find(a => a.poleNumber === pole.number && a.position === "B") || { poleNumber: pole.number, position: "B" };

    const occA = spotA.productId ? true : false;
    const occB = spotB.productId ? true : false;

    const card = document.createElement("div");
    card.className = "pole-card";
    card.innerHTML = `
      <div class="pole-header">PAŁĄK ${pole.number}</div>
      <div class="spots-container">
        <div class="spot-box ${occA ? 'occupied' : ''}" onclick="selectSpot(${pole.number}, 'A')">
          <div class="spot-header">
            <span>${pole.number}A</span>
            <span class="status-tag ${occA ? 'occupied' : 'empty'}">${occA ? 'Zajęte' : 'Puste'}</span>
          </div>
          ${occA ? `
            <div class="spot-details">
              <strong>${spotA.productName}</strong><br>
              LM: ${spotA.lmSystemNumber}<br>
              <div class="spot-price">${Number(spotA.price).toFixed(2)} zł</div>
            </div>
          ` : `<div class="spot-details" style="color: gray;">+ Kliknij, aby przypisać</div>`}
        </div>

        <div class="spot-box ${occB ? 'occupied' : ''}" onclick="selectSpot(${pole.number}, 'B')">
          <div class="spot-header">
            <span>${pole.number}B</span>
            <span class="status-tag ${occB ? 'occupied' : 'empty'}">${occB ? 'Zajęte' : 'Puste'}</span>
          </div>
          ${occB ? `
            <div class="spot-details">
              <strong>${spotB.productName}</strong><br>
              LM: ${spotB.lmSystemNumber}<br>
              <div class="spot-price">${Number(spotB.price).toFixed(2)} zł</div>
            </div>
          ` : `<div class="spot-details" style="color: gray;">+ Kliknij, aby przypisać</div>`}
        </div>
      </div>
    `;
    grid.appendChild(card);
  });
}

function selectSpot(poleNumber, position) {
  const spotKey = `${poleNumber}${position}`;
  document.getElementById("searchInput").value = spotKey;
  handleSearch();
}

function setFilter(filter, btn) {
  currentFilter = filter;
  document.querySelectorAll(".btn-filter").forEach(b => b.classList.remove("active"));
  btn.classList.add("active");
  renderPoles();
}

function handleJumpPole(val) {
  currentJumpPole = val.trim();
  renderPoles();
}

// Search Logic
function handleSearch() {
  const input = document.getElementById("searchInput");
  const query = input.value.trim();
  currentSearchQuery = query;

  const resultsSection = document.getElementById("searchResultsSection");
  const resultsList = document.getElementById("searchResultsList");
  const clearBtn = document.getElementById("clearSearchBtn");

  if (!query) {
    resultsSection.classList.add("hidden");
    clearBtn.classList.add("hidden");
    return;
  }

  clearBtn.classList.remove("hidden");
  resultsSection.classList.remove("hidden");
  resultsList.innerHTML = "";

  const q = query.toLowerCase();
  const isAdmin = userRole === "ADMIN" || userRole === "SUPER_ADMIN";

  // 1. Check if spot pattern e.g. "23A", "1B"
  const spotRegex = /^(\d+)([a-b])$/i;
  const match = q.match(spotRegex);
  const matchedSpots = [];

  if (match) {
    const pNum = parseInt(match[1], 10);
    const pPos = match[2].toUpperCase();
    const ass = displayAssignments.find(a => a.poleNumber === pNum && a.position === pPos);
    if (ass && ass.productId) {
      matchedSpots.push(ass);
    }
  }

  // 2. Search products by EAN, LM Number, Name
  const matchedProducts = products.filter(p => {
    const eanMatch = p.ean && p.ean.toLowerCase() === q; // EAN is full
    const lmMatch = p.lmSystemNumber && p.lmSystemNumber.toLowerCase() === q;
    const nameMatch = p.name && p.name.toLowerCase().includes(q);
    return eanMatch || lmMatch || nameMatch;
  });

  const allItemsToDisplay = [];

  matchedSpots.forEach(ass => {
    allItemsToDisplay.push({
      product: {
        productId: ass.productId,
        name: ass.productName,
        ean: ass.ean,
        lmSystemNumber: ass.lmSystemNumber,
        onlinePrice: ass.price,
        localPrice: ass.price,
        localPriceOverride: ass.localPriceOverride,
        imageUrl: ass.imageUrl
      },
      assignment: ass,
      location: `${ass.poleNumber}${ass.position}`
    });
  });

  matchedProducts.forEach(prod => {
    if (!allItemsToDisplay.some(item => item.product.productId === prod.productId)) {
      const ass = displayAssignments.find(a => a.productId === prod.productId);
      allItemsToDisplay.push({
        product: prod,
        assignment: ass,
        location: ass ? `${ass.poleNumber}${ass.position}` : "Brak na ekspozycji"
      });
    }
  });

  if (allItemsToDisplay.length === 0) {
    resultsList.innerHTML = `
      <div style="padding: 20px; background: white; border-radius: 8px;">
        <h3>Nie znaleziono produktu</h3>
        <p style="color: gray; margin-top: 4px;">Brak pasującego dywanu dla zapytania: "${query}".</p>
      </div>
    `;
    return;
  }

  allItemsToDisplay.forEach(item => {
    const prod = item.product;
    const ass = item.assignment;
    const effectivePrice = (prod.localPriceOverride && prod.localPrice > 0) ? prod.localPrice : (prod.onlinePrice || 0);

    const card = document.createElement("div");
    card.className = "product-result-card";
    card.innerHTML = `
      <img src="${prod.imageUrl || 'https://via.placeholder.com/150?text=Dywan'}" class="product-img" alt="${prod.name}">
      <div class="product-info">
        <div class="product-title">${prod.name}</div>
        <div class="product-meta">
          <strong>Numer LM:</strong> ${prod.lmSystemNumber || 'Brak'} | <strong>EAN:</strong> ${prod.ean || 'Brak'}
        </div>
        <span class="product-location-badge">${item.location}</span>
        <div class="product-price-tag">
          ${Number(effectivePrice).toFixed(2)} zł
          <span style="font-size: 0.8rem; font-weight: normal; margin-left: 8px; color: ${prod.localPriceOverride ? '#D32F2F' : '#2E7D32'}">
            (${prod.localPriceOverride ? 'Cena lokalna' : 'Cena internetowa'})
          </span>
        </div>
        ${isAdmin ? `
          <div class="product-actions">
            ${ass && ass.productId ? `
              <button class="btn btn-sm btn-secondary" onclick="openMoveModal(${ass.poleNumber}, '${ass.position}', '${prod.name.replace(/'/g, "\\'")}', false)">Przenieś</button>
              <button class="btn btn-sm btn-secondary" onclick="openMoveModal(${ass.poleNumber}, '${ass.position}', '${prod.name.replace(/'/g, "\\'")}', true)">Zamień</button>
              <button class="btn btn-sm btn-secondary" style="color: red;" onclick="removeFromDisplay(${ass.poleNumber}, '${ass.position}')">Zdejmij</button>
            ` : `
              <button class="btn btn-sm btn-primary" onclick="quickAssignProduct('${prod.productId}')">+ Przypisz do pałąka</button>
            `}
          </div>
        ` : ''}
      </div>
    `;
    resultsList.appendChild(card);
  });
}

function clearSearch() {
  document.getElementById("searchInput").value = "";
  handleSearch();
}

// Admin Actions
async function submitAddRug(e) {
  e.preventDefault();
  const ean = document.getElementById("addEan").value.trim(); // NEVER truncate!
  const lmNumber = document.getElementById("addLmNumber").value.trim();
  const name = document.getElementById("addName").value.trim();
  const onlinePrice = parseFloat(document.getElementById("addOnlinePrice").value) || 0;
  const localPrice = parseFloat(document.getElementById("addLocalPrice").value) || 0;
  const localPriceOverride = document.getElementById("addLocalOverride").checked;
  const imageUrl = document.getElementById("addImageUrl").value.trim();
  const poleNum = parseInt(document.getElementById("addPoleNum").value, 10);
  const spotPos = document.getElementById("addSpotPos").value;

  const prodId = lmNumber ? `lm_${lmNumber}` : `ean_${ean}`;
  const effectivePrice = (localPriceOverride && localPrice > 0) ? localPrice : onlinePrice;

  try {
    // 1. Save product
    await db.collection("products").doc(prodId).set({
      productId: prodId,
      name,
      ean,
      lmSystemNumber: lmNumber,
      onlinePrice,
      localPrice,
      localPriceOverride,
      imageUrl,
      lastUpdated: firebase.firestore.FieldValue.serverTimestamp(),
      updatedBy: currentUser.email
    }, { merge: true });

    // 2. Assign to spot
    const assId = `pos_${poleNum}_${spotPos}`;
    await db.collection("displayAssignments").doc(assId).set({
      assignmentId: assId,
      poleNumber: poleNum,
      position: spotPos,
      productId: prodId,
      productName: name,
      ean,
      lmSystemNumber: lmNumber,
      price: effectivePrice,
      localPriceOverride,
      imageUrl,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
      updatedBy: currentUser.email
    }, { merge: true });

    // 3. Record audit log
    await db.collection("auditLogs").add({
      logId: Date.now().toString(),
      userId: currentUser.uid,
      userEmail: currentUser.email,
      operationType: "DODANIE_DYWANU",
      details: `Dodano ${name} na miejsce ${poleNum}${spotPos}`,
      oldValue: "Brak",
      newValue: `${poleNum}${spotPos}`,
      timestamp: firebase.firestore.FieldValue.serverTimestamp()
    });

    closeModal("addRugModal");
    alert(`Pomyślnie przypisano ${name} do miejsca ${poleNum}${spotPos}!`);
  } catch (err) {
    alert("Błąd: " + err.message);
  }
}

let activeMoveSource = null;
let isSwapMode = false;

function openMoveModal(poleNum, pos, name, isSwap) {
  isSwapMode = isSwap;
  activeMoveSource = { poleNum, pos, name };
  document.getElementById("moveModalTitle").textContent = isSwap ? "Zamień miejsca" : "Przenieś dywan";
  document.getElementById("moveSourceInfo").innerHTML = `
    <p style="margin-bottom: 12px;"><strong>Źródło:</strong> ${poleNum}${pos} (${name})</p>
  `;
  document.getElementById("moveSubmitBtn").textContent = isSwap ? "Zamień" : "Przenieś";
  openModal("moveModal");
}

async function submitMoveOrSwap(e) {
  e.preventDefault();
  const targetPole = parseInt(document.getElementById("targetPoleInput").value, 10);
  const targetPos = document.getElementById("targetPosInput").value;

  if (targetPole === activeMoveSource.poleNum && targetPos === activeMoveSource.pos) {
    alert("Miejsce docelowe jest takie samo jak źródłowe!");
    return;
  }

  const srcId = `pos_${activeMoveSource.poleNum}_${activeMoveSource.pos}`;
  const tgtId = `pos_${targetPole}_${targetPos}`;

  const srcRef = db.collection("displayAssignments").doc(srcId);
  const tgtRef = db.collection("displayAssignments").doc(tgtId);

  try {
    await db.runTransaction(async (transaction) => {
      const srcDoc = await transaction.get(srcRef);
      const tgtDoc = await transaction.get(tgtRef);

      const srcData = srcDoc.data() || {};
      const tgtData = tgtDoc.data() || {};

      if (isSwapMode) {
        // Swap
        transaction.set(tgtRef, {
          ...srcData,
          assignmentId: tgtId,
          poleNumber: targetPole,
          position: targetPos,
          updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
          updatedBy: currentUser.email
        });
        transaction.set(srcRef, {
          ...tgtData,
          assignmentId: srcId,
          poleNumber: activeMoveSource.poleNum,
          position: activeMoveSource.pos,
          updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
          updatedBy: currentUser.email
        });
      } else {
        // Move
        if (tgtData.productId) {
          throw new Error(`Miejsce docelowe ${targetPole}${targetPos} jest zajęte! Użyj opcji 'Zamień' lub zwolnij miejsce.`);
        }
        transaction.set(tgtRef, {
          ...srcData,
          assignmentId: tgtId,
          poleNumber: targetPole,
          position: targetPos,
          updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
          updatedBy: currentUser.email
        });
        transaction.set(srcRef, {
          assignmentId: srcId,
          poleNumber: activeMoveSource.poleNum,
          position: activeMoveSource.pos,
          productId: "",
          productName: "",
          ean: "",
          lmSystemNumber: "",
          price: 0,
          localPriceOverride: false,
          imageUrl: "",
          updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
          updatedBy: currentUser.email
        });
      }
    });

    closeModal("moveModal");
    alert("Operacja wykonana pomyślnie!");
  } catch (err) {
    alert("Błąd: " + err.message);
  }
}

async function removeFromDisplay(poleNum, pos) {
  if (!confirm(`Czy na pewno zdjąć dywan z miejsca ${poleNum}${pos}? Produkt pozostanie w katalogu.`)) return;
  const assId = `pos_${poleNum}_${pos}`;
  try {
    await db.collection("displayAssignments").doc(assId).set({
      assignmentId: assId,
      poleNumber: poleNum,
      position: pos,
      productId: "",
      productName: "",
      ean: "",
      lmSystemNumber: "",
      price: 0,
      localPriceOverride: false,
      imageUrl: "",
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
      updatedBy: currentUser.email
    }, { merge: true });
    alert(`Dywan został zdjęty z miejsca ${poleNum}${pos}.`);
  } catch (err) {
    alert("Błąd: " + err.message);
  }
}

// Poles Management
async function addNewPole() {
  const num = parseInt(document.getElementById("newPoleInput").value, 10);
  if (!num || num < 1) return alert("Podaj poprawny numer pałąka.");
  const pId = `pole_${num}`;
  try {
    await db.collection("poles").doc(pId).set({
      poleId: pId,
      number: num,
      createdAt: firebase.firestore.FieldValue.serverTimestamp()
    });
    // Init spots
    await db.collection("displayAssignments").doc(`pos_${num}_A`).set({
      assignmentId: `pos_${num}_A`, poleNumber: num, position: "A", productId: "", price: 0
    }, { merge: true });
    await db.collection("displayAssignments").doc(`pos_${num}_B`).set({
      assignmentId: `pos_${num}_B`, poleNumber: num, position: "B", productId: "", price: 0
    }, { merge: true });
    document.getElementById("newPoleInput").value = num + 1;
    loadManagePolesList();
  } catch (e) {
    alert("Błąd: " + e.message);
  }
}

function openManagePolesModal() {
  loadManagePolesList();
  openModal("managePolesModal");
}

function loadManagePolesList() {
  const container = document.getElementById("polesListContainer");
  container.innerHTML = "";
  poles.sort((a,b) => a.number - b.number).forEach(p => {
    const item = document.createElement("div");
    item.style = "display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--border);";
    item.innerHTML = `
      <span>Pałąk ${p.number} (miejsca A i B)</span>
      <button class="btn btn-sm btn-secondary" style="color: red;" onclick="deletePole(${p.number})">Usuń</button>
    `;
    container.appendChild(item);
  });
}

async function deletePole(num) {
  const spotA = displayAssignments.find(a => a.poleNumber === num && a.position === "A");
  const spotB = displayAssignments.find(a => a.poleNumber === num && a.position === "B");
  if ((spotA && spotA.productId) || (spotB && spotB.productId)) {
    return alert(`Pałąk ${num} zawiera produkty! Przed usunięciem zdejmij lub przenieś produkty.`);
  }
  if (!confirm(`Czy usunąć pałąk ${num}?`)) return;
  try {
    await db.collection("poles").doc(`pole_${num}`).delete();
    await db.collection("displayAssignments").doc(`pos_${num}_A`).delete();
    await db.collection("displayAssignments").doc(`pos_${num}_B`).delete();
    loadManagePolesList();
  } catch (e) {
    alert("Błąd: " + e.message);
  }
}

// User Management (SUPER_ADMIN)
async function openUsersModal() {
  const container = document.getElementById("usersListContainer");
  container.innerHTML = "Ładowanie...";
  openModal("usersModal");
  const snap = await db.collection("users").get();
  container.innerHTML = "";
  snap.docs.forEach(doc => {
    const u = doc.data();
    const item = document.createElement("div");
    item.style = "display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border);";
    item.innerHTML = `
      <div>
        <strong>${u.email || u.displayName}</strong><br>
        <span class="badge" style="background: ${u.role === 'SUPER_ADMIN' ? 'green' : (u.role === 'ADMIN' ? 'orange' : 'gray')}">${u.role}</span>
      </div>
      <div>
        ${u.role === 'ADMIN' && currentUser.uid !== doc.id ? `
          <button class="btn btn-sm btn-outline" style="color: red; border-color: red;" onclick="changeRole('${doc.id}', 'USER')">Odbierz ADMIN</button>
          <button class="btn btn-sm btn-primary" onclick="transferSuperAdmin('${doc.id}', '${u.email}')">Przekaż SUPER_ADMIN</button>
        ` : (u.role === 'USER' ? `
          <button class="btn btn-sm btn-primary" onclick="changeRole('${doc.id}', 'ADMIN')">Uczyń ADMIN</button>
        ` : '')}
      </div>
    `;
    container.appendChild(item);
  });
}

async function addNewAdmin() {
  const email = document.getElementById("newAdminEmail").value.trim().toLowerCase();
  if (!email) return alert("Podaj adres email.");
  const query = await db.collection("users").where("email", "==", email).get();
  if (!query.empty) {
    await query.docs[0].ref.update({ role: "ADMIN" });
  } else {
    await db.collection("users").doc(`invited_${Date.now()}`).set({
      email,
      role: "ADMIN",
      active: true,
      createdAt: firebase.firestore.FieldValue.serverTimestamp()
    });
  }
  document.getElementById("newAdminEmail").value = "";
  openUsersModal();
  alert(`Dodano administratora ${email}.`);
}

async function changeRole(userId, newRole) {
  await db.collection("users").doc(userId).update({ role: newRole });
  openUsersModal();
}

async function transferSuperAdmin(targetUid, targetEmail) {
  if (!confirm(`Czy na pewno przekazać rolę Głównego Administratora (SUPER_ADMIN) dla ${targetEmail}? Twoja rola zmieni się na ADMIN.`)) return;
  try {
    const batch = db.batch();
    batch.update(db.collection("users").doc(targetUid), { role: "SUPER_ADMIN" });
    batch.update(db.collection("users").doc(currentUser.uid), { role: "ADMIN" });
    await batch.commit();
    userRole = "ADMIN";
    renderAuthUI();
    openUsersModal();
    alert(`Przekazano rolę SUPER_ADMIN do ${targetEmail}.`);
  } catch (e) {
    alert("Błąd: " + e.message);
  }
}

// Audit Modal
async function openAuditModal() {
  const container = document.getElementById("auditLogsContainer");
  container.innerHTML = "Ładowanie...";
  openModal("auditModal");
  const snap = await db.collection("auditLogs").orderBy("timestamp", "desc").limit(50).get();
  container.innerHTML = "";
  snap.docs.forEach(doc => {
    const l = doc.data();
    const dateStr = l.timestamp?.toDate() ? l.timestamp.toDate().toLocaleString('pl-PL') : 'Przed chwilą';
    const item = document.createElement("div");
    item.style = "padding: 8px; border-bottom: 1px solid var(--border);";
    item.innerHTML = `
      <div style="font-size: 0.8rem; color: gray;">${dateStr} | ${l.userEmail || 'System'}</div>
      <div><strong>${l.operationType}</strong>: ${l.details}</div>
      ${l.oldValue ? `<div style="font-size: 0.85rem; color: #555;">Zmiana: ${l.oldValue} → ${l.newValue}</div>` : ''}
    `;
    container.appendChild(item);
  });
}

function openAddRugModal() {
  const nextNum = poles.length > 0 ? (Math.max(...poles.map(p => p.number)) || 1) : 1;
  document.getElementById("addPoleNum").value = nextNum;
  openModal("addRugModal");
}

function openModal(id) {
  document.getElementById(id).classList.remove("hidden");
}

function closeModal(id) {
  document.getElementById(id).classList.add("hidden");
}

// Web Scanner using HTML5 Camera
let scannerStream = null;
let scannerInterval = null;

async function startWebScanner() {
  openModal("scannerModal");
  const video = document.getElementById("scannerVideo");

  try {
    scannerStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "environment" }
    });
    video.srcObject = scannerStream;
    await video.play();

    // Check for native BarcodeDetector API in modern Chromium / Android Chrome
    if ('BarcodeDetector' in window) {
      const barcodeDetector = new BarcodeDetector({
        formats: ['ean_13', 'ean_8', 'upc_a', 'upc_e', 'code_128', 'code_39', 'qr_code']
      });
      scannerInterval = setInterval(async () => {
        try {
          const barcodes = await barcodeDetector.detect(video);
          if (barcodes.length > 0) {
            const raw = barcodes[0].rawValue.trim(); // NEVER truncate!
            stopWebScanner();
            document.getElementById("searchInput").value = raw;
            handleSearch();
          }
        } catch (e) {
          // ignore detection frame errors
        }
      }, 300);
    }
  } catch (err) {
    alert("Nie udało się uruchomić kamery w przeglądarce: " + err.message);
    stopWebScanner();
  }
}

function stopWebScanner() {
  if (scannerStream) {
    scannerStream.getTracks().forEach(track => track.stop());
    scannerStream = null;
  }
  if (scannerInterval) {
    clearInterval(scannerInterval);
    scannerInterval = null;
  }
  closeModal("scannerModal");
}
