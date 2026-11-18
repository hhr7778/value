const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_vault_123";
const BOB_UID = "bob_vault_456";

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

function validConfig(uid, ts) {
  return {
    userId: uid,
    email: "alice@gmail.com",
    displayName: "Alice",
    masterPinHash: "salt123:abcdef123456",
    decoyPinHash: "",
    panicPinHash: "",
    calculatorCode: "7788",
    fingerprintEnabled: true,
    faceLockEnabled: false,
    doubleSecurityMode: false,
    fastMode: true,
    calculatorModeEnabled: false,
    autoBackupEnabled: false,
    autoLockSeconds: 60,
    wrongAttempts: 0,
    planTier: "FREE",
    proPlanCycle: "NONE",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated users cannot read or write any vault path", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).set(validConfig(ALICE_UID, ts)));
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("files").get());
});

test("Owner can create and partially update UserSecurityConfig; ghost fields and immutable mutations fail", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(validConfig(ALICE_UID, created)));

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      planTier: "PRO",
      proPlanCycle: "YEARLY_799",
      calculatorModeEnabled: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immutable createdAt change must fail
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Ghost field must fail
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Owner can create, list, update, and delete folders and encrypted file metadata", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 3000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("folders").doc("folder_photos").set({
      userId: ALICE_UID,
      name: "Photos",
      category: "PHOTOS",
      colorHex: "#8B5CF6",
      iconKey: "photo",
      systemFolder: true,
      locked: false,
      folderPinHash: "",
      decoyOnly: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("files").doc("file_01").set({
      userId: ALICE_UID,
      folderId: "folder_photos",
      name: "passport_scan.jpg",
      extension: "jpg",
      mimeType: "image/jpeg",
      category: "PHOTOS",
      sizeBytes: 245760,
      durationMs: 0,
      encryptedBlobPath: "vault_blobs/file_01.enc",
      sha256Checksum: "abc123def456",
      backedUp: false,
      decoyItem: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List subcollections (matches Kotlin observeFolders / observeFiles)
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("folders").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("files").get());

  // Partial update on file (e.g., rename / mark backedUp)
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("files").doc("file_01").update({
      backedUp: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject writing explicit @DocumentId "id" field
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("files").doc("file_02").set({
      id: "file_02",
      userId: ALICE_UID,
      folderId: "folder_photos",
      name: "bad.jpg",
      extension: "jpg",
      mimeType: "image/jpeg",
      category: "PHOTOS",
      sizeBytes: 100,
      durationMs: 0,
      encryptedBlobPath: "vault_blobs/file_02.enc",
      sha256Checksum: "abc",
      backedUp: false,
      decoyItem: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Owner can create security alerts and encrypted cloud backups; cross-user access is denied", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("securityAlerts").doc("alert_1").set({
      userId: ALICE_UID,
      alertType: "BREAK_IN_SELFIE",
      attemptCount: 3,
      detail: "3 consecutive wrong PIN attempts captured front-camera selfie.",
      locationText: "23.8103, 90.4125",
      selfieLocalPath: "intruders/selfie_1.jpg",
      emailAlertTarget: "alice@gmail.com",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("backups").doc("bak_1").set({
      userId: ALICE_UID,
      label: "Auto Backup #1",
      itemCount: 4,
      totalBytes: 1024000,
      encryptionAlgo: "AES-256-GCM-PBKDF2",
      saltBase64: "c2FsdDEyMzQ1Njc4",
      ivBase64: "aXYxMjM0NTY3ODkw",
      encryptedManifestBase64: "ZW5jcnlwdGVkX21hbmlmZXN0X2RhdGE=",
      createdAt: ts,
    })
  );

  // Bob cannot read or write Alice's vault
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("files").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("securityAlerts").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("backups").get());
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("folders").doc("hack").set({
      userId: ALICE_UID,
      name: "Hack",
      category: "CUSTOM",
      colorHex: "#FF0000",
      iconKey: "folder",
      systemFolder: false,
      locked: false,
      folderPinHash: "",
      decoyOnly: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
