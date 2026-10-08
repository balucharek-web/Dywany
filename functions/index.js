const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const { resolveProduct } = require("./leroyProductService");

admin.initializeApp();

const DATABASE_ID = "ai-studio-android-ekspozyc-1a74d62b-b3ae-4eeb-8cc7-c35020e9f415";
const db = admin.firestore(DATABASE_ID);

/**
 * Endpoint Callable: Wyszukiwanie i pobieranie danych produktu Leroy Merlin (EAN lub KM)
 */
exports.fetchProductData = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Wymagane zalogowanie.");
  }

  const identifier = (request.data.identifier || "").trim();
  if (!identifier) {
    throw new HttpsError("invalid-argument", "Brak identyfikatora produktu.");
  }

  // 1. Sprawdź pamięć podręczną w Firestore
  const isKm = /^[0-9]{8}$/.test(identifier);
  if (isKm) {
    const docSnap = await db.collection("dywany").doc(identifier).get();
    if (docSnap.exists) {
      const data = docSnap.data();
      const updatedAt = data.productDataUpdatedAt?.toDate?.() || 0;
      const hoursSince = (Date.now() - updatedAt) / (1000 * 60 * 60);

      // Jeśli dane są świeże (mniej niż 24h), zwróć z Firestore
      if (hoursSince < 24 && data.nazwa) {
        return {
          km: data.km,
          ean: data.ean || "",
          nazwa: data.nazwa,
          rozmiar: data.rozmiar || "",
          cena: data.cena || null,
          waluta: data.waluta || "PLN",
          productUrl: data.productUrl || "",
          status: data.productDataStatus || "ACTIVE",
          source: data.productDataSource || "leroy_merlin",
          cached: true
        };
      }
    }
  }

  // 2. Pobierz z katalogu Leroy Merlin
  const productData = await resolveProduct(identifier);
  return productData;
});

/**
 * Endpoint Callable: Wymuszone odświeżenie danych produktu (ADMIN/SUPER_ADMIN)
 */
exports.refreshProductData = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Wymagane zalogowanie.");
  }

  const userEmail = (request.auth.token.email || "").toLowerCase();
  const isAdmin = userEmail === "abaluch@leroymerlin.pl" ||
    (await db.collection("admins").doc(userEmail).get()).exists;

  if (!isAdmin) {
    throw new HttpsError("permission-denied", "Tylko administrator może wymusić odświeżenie danych.");
  }

  const km = (request.data.km || "").trim();
  if (!km) {
    throw new HttpsError("invalid-argument", "Brak numeru KM.");
  }

  const dywanRef = db.collection("dywany").doc(km);
  const existingSnap = await dywanRef.get();
  if (!existingSnap.exists) {
    throw new HttpsError("not-found", "Produkt nie istnieje w bazie.");
  }

  const existingData = existingSnap.data();
  const freshData = await resolveProduct(km);

  if (freshData.status === "ACTIVE") {
    const oldPrice = existingData.cena;
    const newPrice = freshData.cena;
    const priceChanged = newPrice && oldPrice && Math.abs(newPrice - oldPrice) > 0.001;

    const batch = db.batch();
    batch.update(dywanRef, {
      nazwa: freshData.nazwa || existingData.nazwa,
      rozmiar: freshData.rozmiar || existingData.rozmiar,
      cena: newPrice || existingData.cena,
      ean: freshData.ean || existingData.ean,
      productDataStatus: "ACTIVE",
      productDataUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    if (priceChanged) {
      const priceLogId = `${Date.now()}_price_${km}`;
      batch.set(db.collection("productPriceHistory").doc(priceLogId), {
        id: priceLogId,
        km,
        oldPrice,
        newPrice,
        changedAt: admin.firestore.FieldValue.serverTimestamp(),
        source: "leroy_merlin",
        changedBy: userEmail
      });
    }

    await batch.commit();
    return { success: true, priceChanged, oldPrice, newPrice };
  } else {
    await dywanRef.update({
      productDataStatus: freshData.status,
      productDataUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    return { success: false, status: freshData.status };
  }
});

/**
 * Zadanie Cron / Scheduled: Okresowa aktualizacja cen i dostępności produktów (raz na dobę o 04:00)
 */
exports.scheduledProductRefresh = onSchedule("every 24 hours", async (event) => {
  console.log("Rozpoczynanie zaplanowanego odświeżania produktów Leroy Merlin...");

  const snapshot = await db.collection("dywany").limit(50).get();
  for (const docSnap of snapshot.docs) {
    const data = docSnap.data();
    const km = data.km;
    if (!km) continue;

    try {
      const freshData = await resolveProduct(km);
      if (freshData.status === "ACTIVE") {
        const oldPrice = data.cena;
        const newPrice = freshData.cena;
        const priceChanged = newPrice && oldPrice && Math.abs(newPrice - oldPrice) > 0.001;

        const batch = db.batch();
        batch.update(docSnap.ref, {
          nazwa: freshData.nazwa || data.nazwa,
          rozmiar: freshData.rozmiar || data.rozmiar,
          cena: newPrice || data.cena,
          productDataStatus: "ACTIVE",
          productDataUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        if (priceChanged) {
          const priceLogId = `${Date.now()}_cron_${km}`;
          batch.set(db.collection("productPriceHistory").doc(priceLogId), {
            id: priceLogId,
            km,
            oldPrice,
            newPrice,
            changedAt: admin.firestore.FieldValue.serverTimestamp(),
            source: "scheduled_cron",
            changedBy: "system_cron"
          });
        }
        await batch.commit();
      }
    } catch (err) {
      console.error(`Błąd aktualizacji produktu ${km}:`, err);
    }
  }
});
