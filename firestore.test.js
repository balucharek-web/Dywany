const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const USER_UID = "worker_123";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user can read slots", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthedDb.collection("slots").doc("1a").get());
});

test("Unauthenticated user CANNOT create a slot", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthedDb.collection("slots").doc("1a").set({
      slotId: "1a",
      rackNumber: 1,
      slotLetter: "a",
      occupied: true,
      productName: "Dywan Isfahan",
      updatedAt: new Date(),
      createdAt: new Date(),
    })
  );
});

test("Authenticated worker can create and update a valid slot", async () => {
  const authedDb = testEnv.authenticatedContext(USER_UID).firestore();
  const slotRef = authedDb.collection("slots").doc("1a");
  const now = new Date();

  await assertSucceeds(
    slotRef.set({
      slotId: "1a",
      rackNumber: 1,
      slotLetter: "a",
      occupied: true,
      productName: "Dywan Komfort 160x230",
      ean: "5901234567890",
      referenceNumber: "82654321",
      eslCode: "ESL-001",
      price: "499,00 zł",
      updatedBy: "worker@example.com",
      updatedAt: now,
      createdAt: now,
    })
  );

  // Update carpet details
  await assertSucceeds(
    slotRef.update({
      productName: "Dywan Agnella 160x230",
      price: "449,00 zł",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated worker cannot violate slot schema", async () => {
  const authedDb = testEnv.authenticatedContext(USER_UID).firestore();
  const slotRef = authedDb.collection("slots").doc("1a");

  // Invalid rackNumber (0)
  await assertFails(
    slotRef.set({
      slotId: "1a",
      rackNumber: 0,
      slotLetter: "a",
      occupied: true,
      updatedAt: new Date(),
      createdAt: new Date(),
    })
  );
});
