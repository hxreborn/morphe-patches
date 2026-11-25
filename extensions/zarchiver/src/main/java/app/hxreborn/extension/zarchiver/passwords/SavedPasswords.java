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
import java.security.GeneralSecurityException;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class SavedPasswords {

    private static final String SAVED_PASSWORDS = "Saved passwords";

    private static final int MAX_NAME_LENGTH = 80;

    private final Context context;

    private final View content;

    private final int inputId;

    private final ArchivePasswordStore store;

    private SavedPasswords(View content, int inputId) {
        this.context = content.getContext();
        this.content = content;
        this.inputId = inputId;
        this.store = new ArchivePasswordStore(this.context);
    }

    public static AlertDialog.Builder setPasswordPrompt(AlertDialog.Builder builder, View content) {
        return builder.setView(addAction(content, passwordPromptInputId()));
    }

    public static AlertDialog.Builder setArchiveOptions(AlertDialog.Builder builder, View content) {
        return builder.setView(addAction(content, archiveOptionsInputId()));
    }

    public static AlertDialog.Builder setArchiveFileOptions(AlertDialog.Builder builder, View content,
            boolean encryptionAvailable) {
        return encryptionAvailable ? setArchiveOptions(builder, content) : builder.setView(content);
    }

    private static int passwordPromptInputId() {
        return 0;
    }

    private static int archiveOptionsInputId() {
        return 0;
    }

    private static View addAction(View content, int inputId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return content;
        }
        if (!(content.findViewById(inputId) instanceof EditText)) {
            return content;
        }
        SavedPasswords passwords = new SavedPasswords(content, inputId);
        LinearLayout layout = new LinearLayout(content.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        Button action = new Button(content.getContext(), null, android.R.attr.borderlessButtonStyle);
        action.setText(SAVED_PASSWORDS);
        action.setOnClickListener((view) -> passwords.show());
        layout.addView(action, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return layout;
    }

    private void show() {
        List<String> names = this.store.names();
        AlertDialog.Builder dialog = new AlertDialog.Builder(this.context).setTitle(SAVED_PASSWORDS)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton("Save entered password", (window, which) -> this.promptForName());
        if (names.isEmpty()) {
            dialog.setMessage("No saved passwords");
        } else {
            dialog.setItems(names.toArray(new String[0]), (window, index) -> this.showEntry(names.get(index)));
        }
        dialog.show();
    }

    private void showEntry(String name) {
        new AlertDialog.Builder(this.context).setTitle(name)
            .setPositiveButton("Use", (dialog, which) -> this.fillPassword(name))
            .setNeutralButton("Delete", (dialog, which) -> this.confirmDelete(name))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void fillPassword(String name) {
        if (!(this.content.findViewById(this.inputId) instanceof EditText password)) {
            return;
        }
        try {
            String secret = this.store.read(name);
            password.setText(secret);
            password.setSelection(password.length());
        } catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
            Toast.makeText(this.context, "Could not read saved password", Toast.LENGTH_SHORT).show();
        }
    }

    private void promptForName() {
        if (!(this.content.findViewById(this.inputId) instanceof EditText password)) {
            return;
        }
        String secret = password.getText().toString();
        if (secret.isEmpty()) {
            Toast.makeText(this.context, "Enter a password first", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText name = new EditText(this.context);
        name.setSingleLine(true);
        name.setHint("Name");
        name.setFilters(new InputFilter[] { new InputFilter.LengthFilter(MAX_NAME_LENGTH) });
        AlertDialog dialog = new AlertDialog.Builder(this.context).setTitle("Save password")
            .setView(name)
            .setPositiveButton("Save", null)
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> {
            String label = name.getText().toString().trim();
            if (label.isEmpty()) {
                name.setError("Enter a name");
                return;
            }
            if (this.store.contains(label)) {
                name.setError("Name already used");
                return;
            }
            try {
                if (this.store.save(label, secret)) {
                    dialog.dismiss();
                } else {
                    name.setError("Could not save password");
                }
            } catch (GeneralSecurityException | IOException ex) {
                name.setError("Could not save password");
            }
        });
    }

    private void confirmDelete(String name) {
        new AlertDialog.Builder(this.context).setTitle("Delete saved password?")
            .setMessage(name)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Delete", (dialog, which) -> {
                if (!this.store.delete(name)) {
                    Toast.makeText(this.context, "Could not delete saved password", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

}
