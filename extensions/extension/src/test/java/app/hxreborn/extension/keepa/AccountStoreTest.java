/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class AccountStoreTest {

    private static final long NOW = 1_000_000_000_000L;

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private File file;

    private AccountStore store;

    @Before
    public void openStore() {
        this.file = new File(this.folder.getRoot(), AccountStore.FILE_NAME);
        this.store = new AccountStore(this.file);
    }

    private void writeRaw(String content) throws IOException {
        Files.write(this.file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private JSONObject readRaw() throws IOException, JSONException {
        return new JSONObject(new String(Files.readAllBytes(this.file.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void usesAFixedFileName() {
        assertEquals("hx_keepa_accounts.json", AccountStore.FILE_NAME);
    }

    @Test
    public void keepsTheFileInTheNoBackupDirectoryOfTheContext() throws JSONException {
        // given
        final Context context = RuntimeEnvironment.getApplication();

        // when
        AccountStore.of(context).writePending("pending");

        // then
        assertTrue(new File(context.getNoBackupFilesDir(), "hx_keepa_accounts.json").exists());
        assertEquals("pending", AccountStore.of(context).pending());
    }

    @Test
    public void loadsAnEmptySetWhenNothingWasSaved() throws JSONException {
        // when
        final Accounts accounts = this.store.load();

        // then
        assertTrue(accounts.all().isEmpty());
        assertEquals("", accounts.primaryId());
        assertEquals("", accounts.summary());
        assertEquals("{}", accounts.owners().toString());
        assertFalse(this.file.exists());
    }

    @Test
    public void readsNoPendingOperationWhenNothingWasSaved() throws JSONException {
        assertEquals("", this.store.pending());
    }

    @Test
    public void roundTripsTheAccountSet() throws JSONException {
        // given
        final Accounts accounts = this.store.load();
        accounts.upsertByUsername("t1", "alice", "a@x", NOW);
        accounts.upsertByUsername("t2", "bob", "b@x", NOW + 1);
        accounts.applyAddResult("B01", accounts.find(accounts.primaryId()).id, true, null);
        this.store.save(accounts);

        // when
        final Accounts reloaded = this.store.load();

        // then
        assertEquals(2, reloaded.all().size());
        assertEquals("alice", reloaded.all().get(0).username);
        assertEquals("t2", reloaded.all().get(1).token);
        assertEquals(accounts.primaryId(), reloaded.primaryId());
        assertEquals(accounts.owners().toString(), reloaded.owners().toString());
        assertEquals(accounts.summary(), reloaded.summary());
    }

    @Test
    public void replacesThePreviousSnapshotOnSave() throws JSONException {
        // given
        final Accounts accounts = this.store.load();
        accounts.upsertByUsername("t1", "alice", "a@x", NOW);
        this.store.save(accounts);
        accounts.remove(accounts.primaryId());

        // when
        this.store.save(accounts);

        // then
        assertTrue(this.store.load().all().isEmpty());
    }

    @Test
    public void keepsTheDocumentStructureThePageReads() throws IOException, JSONException {
        // given
        final Accounts accounts = this.store.load();
        accounts.upsertByUsername("t1", "alice", "a@x", NOW);

        // when
        this.store.save(accounts);

        // then
        final JSONObject document = readRaw();
        assertEquals(1, new JSONObject(document.getString("accounts")).getJSONArray("accounts").length());
        assertEquals("{}", document.getString("owners"));
    }

    @Test
    public void leavesNoTemporaryFileBehind() throws IOException, JSONException {
        this.store.save(this.store.load());
        this.store.writePending("{\"op\":\"switch\"}");
        this.store.clearPending();

        assertTrue(this.file.exists());
        assertEquals(1, this.folder.getRoot().list().length);
        assertFalse(new File(this.file.getPath() + ".tmp").exists());
    }

    @Test
    public void readsBackTheWrittenPendingOperation() throws JSONException {
        // given
        this.store.writePending("{\"op\":\"switch\",\"id\":\"a\"}");

        // when
        final String pending = this.store.pending();

        // then
        assertEquals("{\"op\":\"switch\",\"id\":\"a\"}", pending);
    }

    @Test
    public void keepsOnlyTheLastPendingOperationUntilCleared() throws JSONException {
        this.store.writePending("first");
        this.store.writePending("second");
        assertEquals("second", this.store.pending());

        this.store.clearPending();
        assertEquals("", this.store.pending());
    }

    @Test
    public void clearingThePendingOperationRemovesItsKey() throws IOException, JSONException {
        // given
        this.store.writePending("pending");
        assertTrue(readRaw().has("pending"));

        // when
        this.store.clearPending();

        // then
        assertFalse(readRaw().has("pending"));
    }

    @Test
    public void clearingNothingPendingIsHarmless() throws JSONException {
        this.store.clearPending();
        assertEquals("", this.store.pending());
        this.store.clearPending();
        assertEquals("", this.store.pending());
    }

    @Test
    public void savingAccountsKeepsThePendingOperation() throws JSONException {
        // given
        this.store.writePending("pending");
        final Accounts accounts = this.store.load();
        accounts.upsertByUsername("t1", "alice", "a@x", NOW);

        // when
        this.store.save(accounts);

        // then
        assertEquals("pending", this.store.pending());
        assertEquals(1, this.store.load().all().size());
    }

    @Test
    public void writingThePendingOperationKeepsTheAccounts() throws JSONException {
        final Accounts accounts = this.store.load();
        accounts.upsertByUsername("t1", "alice", "a@x", NOW);
        this.store.save(accounts);

        this.store.writePending("pending");
        this.store.clearPending();

        assertEquals(1, this.store.load().all().size());
    }

    @Test
    public void keepsKeysItDoesNotKnow() throws IOException, JSONException {
        writeRaw("{\"futureKey\":\"kept\"}");
        this.store.save(this.store.load());
        this.store.writePending("pending");

        assertEquals("kept", readRaw().getString("futureKey"));
    }

    @Test
    public void resetDeletesTheFile() throws JSONException {
        // given
        this.store.writePending("pending");
        assertTrue(this.file.exists());

        // when
        this.store.reset();

        // then
        assertFalse(this.file.exists());
        assertEquals("", this.store.pending());
        assertTrue(this.store.load().all().isEmpty());
    }

    @Test
    public void resetWithoutAFileIsHarmless() {
        // when
        this.store.reset();

        // then
        assertFalse(this.file.exists());
    }

    @Test
    public void resetFailsLoudlyWhenTheFileCannotBeDeleted() throws IOException {
        // given
        assertTrue(this.file.mkdir());
        assertTrue(new File(this.file, "child").createNewFile());

        // when
        try {
            this.store.reset();
            throw new AssertionError("expected a failure");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains(this.file.getPath()));
        }
    }

    @Test
    public void reportsACorruptFileInsteadOfLoadingNothing() throws IOException {
        writeRaw("{not json");
        try {
            this.store.load();
            throw new AssertionError("expected a failure");
        } catch (JSONException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            this.store.pending();
            throw new AssertionError("expected a failure");
        } catch (JSONException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void doesNotOverwriteACorruptFileWhenSaving() throws IOException, JSONException {
        // given
        writeRaw("{not json");

        // when
        try {
            this.store.writePending("pending");
            throw new AssertionError("expected a failure");
        } catch (JSONException expected) {
            assertEquals("{not json", new String(Files.readAllBytes(this.file.toPath()), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void failsToWriteIntoAMissingDirectory() {
        // given
        final AccountStore orphan = new AccountStore(new File(new File(this.folder.getRoot(), "gone"), "f.json"));

        // when
        try {
            orphan.writePending("pending");
            throw new AssertionError("expected a failure");
        } catch (JSONException expected) {
            assertTrue(expected.getMessage().startsWith("Unable to write"));
        }
    }

    @Test
    public void readsMultiByteTextBackIntact() throws JSONException {
        // given
        this.store.writePending("{\"name\":\"é中文\"}");

        // when
        final String pending = this.store.pending();

        // then
        assertEquals("{\"name\":\"é中文\"}", pending);
    }

}
