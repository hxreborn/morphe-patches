/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.zarchiver.passwords;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

final class ArchivePasswordStore {

    private static final String KEY_STORE = "AndroidKeyStore";

    private static final String KEY_ALIAS = "hx_zarchiver_archive_passwords";

    private static final String CIPHER = "AES/GCM/NoPadding";

    private static final int NONCE_BYTES = 12;

    private static final int TAG_BITS = 128;

    private final SharedPreferences preferences;

    ArchivePasswordStore(Context context) {
        this.preferences = context.getSharedPreferences(KEY_ALIAS, Context.MODE_PRIVATE);
    }

    List<String> names() {
        List<String> labels = new ArrayList<>(this.preferences.getAll().keySet());
        Collections.sort(labels, String.CASE_INSENSITIVE_ORDER);
        return labels;
    }

    boolean contains(String name) {
        return this.preferences.contains(name);
    }

    String read(String name) throws IOException, GeneralSecurityException {
        String encoded = this.preferences.getString(name, null);
        if (encoded == null) {
            throw new IOException("Password entry missing");
        }
        byte[] stored = Base64.decode(encoded, Base64.NO_WRAP);
        if (stored.length <= NONCE_BYTES) {
            throw new IOException("Password entry truncated");
        }
        ByteBuffer entry = ByteBuffer.wrap(stored);
        byte[] nonce = new byte[NONCE_BYTES];
        entry.get(nonce);
        byte[] encrypted = new byte[entry.remaining()];
        entry.get(encrypted);
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, key(false), new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(name.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    boolean save(String name, String password) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, key(true));
        cipher.updateAAD(name.getBytes(StandardCharsets.UTF_8));
        byte[] nonce = cipher.getIV();
        if (nonce.length != NONCE_BYTES) {
            throw new GeneralSecurityException("Unexpected password nonce size");
        }
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        byte[] entry = ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array();
        return this.preferences.edit().putString(name, Base64.encodeToString(entry, Base64.NO_WRAP)).commit();
    }

    boolean delete(String name) {
        return this.preferences.edit().remove(name).commit();
    }

    private static synchronized SecretKey key(boolean create) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE);
        keyStore.load(null);
        if (keyStore.getKey(KEY_ALIAS, null) instanceof SecretKey key) {
            return key;
        }
        if (!create) {
            throw new GeneralSecurityException("Password key missing");
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build());
        return generator.generateKey();
    }

}
