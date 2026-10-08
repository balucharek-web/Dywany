const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SUPER_ADMIN_UID = "super_admin_uid";
const SUPER_ADMIN_EMAIL = "abaluch@leroymerlin.pl";
const ADMIN_UID = "admin_uid";
const ADMIN_EMAIL = "admin.test@leroymerlin.pl";
const USER_UID = "user_uid";
const USER_EMAIL = "user.test@leroymerlin.pl";

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

test("Unauthenticated user: cannot read palki", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("palki").get());
});

test("Authenticated user: can read palki", async () => {
  const userDb = testEnv.authenticatedContext(USER_UID, { email: USER_EMAIL }).firestore();
  await assertSucceeds(userDb.collection("palki").get());
});

test("Regular user: cannot create palek", async () => {
  const userDb = testEnv.authenticatedContext(USER_UID, { email: USER_EMAIL }).firestore();
  await assertFails(userDb.collection("palki").doc("palek_1").set({
    id: "palek_1",
    numer: 1,
    slots: { A: null, B: null }
  }));
});

test("Super Admin (abaluch@leroymerlin.pl): can create palek and admin", async () => {
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL }).firestore();
  await assertSucceeds(superDb.collection("palki").doc("palek_1").set({
    id: "palek_1",
    numer: 1,
    slots: { A: null, B: null }
  }));

  await assertSucceeds(superDb.collection("admins").doc(ADMIN_EMAIL).set({
    email: ADMIN_EMAIL,
    addedBy: SUPER_ADMIN_EMAIL
  }));
});

test("Admin: can create palek and dywan with valid 8-digit KM", async () => {
  // First seed admin entry
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(ADMIN_EMAIL).set({
      email: ADMIN_EMAIL,
      addedBy: SUPER_ADMIN_EMAIL
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(adminDb.collection("palki").doc("palek_2").set({
    id: "palek_2",
    numer: 2,
    slots: { A: null, B: null }
  }));

  // Valid 8-digit KM: 45657894
  await assertSucceeds(adminDb.collection("dywany").doc("45657894").set({
    km: "45657894",
    nazwa: "Dywan Testowy",
    palekNumer: 2,
    miejsce: "2A",
    slot: "A"
  }));

  // Invalid KM (7 digits or letters) must FAIL
  await assertFails(adminDb.collection("dywany").doc("1234567").set({
    km: "1234567",
    nazwa: "Błędny Dywan",
    palekNumer: 2,
    miejsce: "2A",
    slot: "A"
  }));
});
