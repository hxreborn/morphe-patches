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
package app.hxreborn.extension.ultimea.equalizer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.text.InputFilter;
import android.view.ContextThemeWrapper;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONException;

import app.hxreborn.extension.shared.ActivityCallbacksAdapter;
import app.hxreborn.extension.ultimea.EqualizerStyle;

@SuppressWarnings("unused")
public final class LocalPresets extends ActivityCallbacksAdapter {

    private static final int IMPORT_REQUEST = 21841;

    private static final int EXPORT_REQUEST = 21842;

    private static final int BUFFER_BYTES = 4096;

    private static final int FILE_READ_TIMEOUT_MS = 30000;

    private static final int LIST_ACTIONS = 2;

    private static final int MAX_FILE_TRANSFERS = 2;

    private static final int FILE_WORKER_IDLE_SECONDS = 60;

    private static final String MIME_TYPE = "application/json";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final ExecutorService FILES = new ThreadPoolExecutor(0, MAX_FILE_TRANSFERS, FILE_WORKER_IDLE_SECONDS,
            TimeUnit.SECONDS, new SynchronousQueue<>());

    private static WeakReference<LocalPresets> current = new WeakReference<>(null);

    private final Activity activity;

    private final EqualizerStyle style;

    private final EqualizerPresetStore store;

    private final EqualizerCurve curve;

    private final PresetSelection selection;

    private AlertDialog dialog;

    private boolean fileRequest;

    private int transfers;

    private LocalPresets(Activity activity, EqualizerCurve curve, PresetSelection selection) {
        this.activity = activity;
        this.style = new EqualizerStyle(activity);
        this.store = new EqualizerPresetStore(activity);
        this.curve = curve;
        this.selection = selection;
        current = new WeakReference<>(this);
        activity.getApplication().registerActivityLifecycleCallbacks(this);
    }

    static void open(Activity activity, EqualizerCurve curve, PresetSelection selection) {
        LocalPresets previous = current.get();
        if (previous != null && previous.fileRequest) {
            previous.message("Finish choosing the preset file first");
            return;
        }
        if (previous != null) {
            previous.dispose();
        }
        new LocalPresets(activity, curve, selection).showList();
    }

    static boolean isOpen(Activity activity) {
        LocalPresets presets = current.get();
        return presets != null && presets.activity == activity && (presets.fileRequest || presets.transfers > 0
                || presets.dialog != null && presets.dialog.isShowing());
    }

    static void close(Activity activity) {
        LocalPresets presets = current.get();
        if (presets != null && presets.activity == activity) {
            presets.dispose();
        }
    }

    static boolean hasFileRequest(Activity activity) {
        LocalPresets presets = current.get();
        return presets != null && presets.activity == activity && presets.fileRequest;
    }

    public static void onActivityResult(Activity activity, int request, int result, Intent data) {
        if (request != IMPORT_REQUEST && request != EXPORT_REQUEST) {
            return;
        }
        LocalPresets presets = current.get();
        if (presets == null || presets.activity != activity) {
            presets = new LocalPresets(activity, null, null);
        }
        presets.fileRequest = false;
        if (result != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        if (request == IMPORT_REQUEST) {
            presets.importFile(data.getData());
        } else {
            presets.exportFile(data.getData());
        }
    }

    private void showList() {
        try {
            List<EqualizerPreset> presets = this.store.load();
            String[] labels = new String[presets.size() + LIST_ACTIONS];
            labels[0] = "Save current curve";
            labels[1] = "Import";
            for (int index = 0; index < presets.size(); index++) {
                labels[index + LIST_ACTIONS] = presets.get(index).name;
            }
            this.dialog = new AlertDialog.Builder(this.activity, this.style.dialogTheme).setTitle("Local presets")
                .setItems(labels, (dialog, which) -> {
                    if (which == 0 && this.curve != null) {
                        name("Save preset", "Untitled", null);
                    } else if (which == 1) {
                        chooseImport();
                    } else if (which >= LIST_ACTIONS) {
                        actions(presets.get(which - LIST_ACTIONS));
                    }
                })
                .setNegativeButton("Close", (dialog, which) -> dispose())
                .show();
        } catch (JSONException | IllegalArgumentException ex) {
            message("Local presets could not be read");
        }
    }

    private void actions(EqualizerPreset preset) {
        PresetAction[] actions = PresetAction.values();
        String[] labels = new String[actions.length];
        for (int index = 0; index < actions.length; index++) {
            labels[index] = actions[index].title;
        }
        this.dialog = new AlertDialog.Builder(this.activity, this.style.dialogTheme).setTitle(preset.name)
            .setItems(labels, (dialog, which) -> {
                switch (actions[which]) {
                    case USE:
                        if (this.selection != null) {
                            this.selection.selected(preset.curve);
                        }
                        dispose();
                        break;
                    case DUPLICATE:
                        name("Duplicate preset", preset.name, preset);
                        break;
                    case RENAME:
                        rename(preset);
                        break;
                    case EXPORT:
                        chooseExport(preset);
                        break;
                    case DELETE:
                        confirmDelete(preset);
                        break;
                }
            })
            .setNegativeButton("Cancel", (dialog, which) -> showList())
            .show();
    }

    private void confirmDelete(EqualizerPreset preset) {
        this.dialog = new AlertDialog.Builder(this.activity, this.style.dialogTheme).setTitle("Delete preset")
            .setMessage("Delete " + preset.name + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                try {
                    this.store.remove(preset.id);
                    showList();
                } catch (IOException | JSONException ex) {
                    message("Preset could not be deleted");
                }
            })
            .setNegativeButton("Cancel", (dialog, which) -> showList())
            .show();
    }

    private void name(String title, String initial, EqualizerPreset source) {
        EditText input = nameInput(initial);
        AlertDialog dialog = new AlertDialog.Builder(this.activity, this.style.dialogTheme).setTitle(title)
            .setView(input)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", (ignored, which) -> showList())
            .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> {
            try {
                this.store.add(input.getText().toString(), (source != null) ? source.curve : this.curve);
                dialog.dismiss();
                showList();
            } catch (IllegalArgumentException ex) {
                input.setError(ex.getMessage());
            } catch (JSONException | IOException ex) {
                message("Preset could not be saved");
            }
        });
        this.dialog = dialog;
    }

    private void rename(EqualizerPreset preset) {
        EditText input = nameInput(preset.name);
        AlertDialog dialog = new AlertDialog.Builder(this.activity, this.style.dialogTheme).setTitle("Rename preset")
            .setView(input)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", (ignored, which) -> showList())
            .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> {
            try {
                this.store.rename(preset.id, input.getText().toString());
                dialog.dismiss();
                showList();
            } catch (IllegalArgumentException ex) {
                input.setError(ex.getMessage());
            } catch (JSONException | IOException ex) {
                message("Preset could not be renamed");
            }
        });
        this.dialog = dialog;
    }

    private EditText nameInput(String initial) {
        EditText input = new EditText(new ContextThemeWrapper(this.activity, this.style.dialogTheme));
        input.setSingleLine(true);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(EqualizerPreset.MAX_NAME_LENGTH) });
        input.setText(initial);
        input.selectAll();
        return input;
    }

    @SuppressWarnings("deprecation")
    private void chooseImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType(MIME_TYPE)
            .addCategory(Intent.CATEGORY_OPENABLE);
        initialFolder(intent);
        try {
            this.fileRequest = true;
            this.activity.startActivityForResult(intent, IMPORT_REQUEST);
        } catch (ActivityNotFoundException ex) {
            this.fileRequest = false;
            message("No document picker found");
        }
    }

    @SuppressWarnings("deprecation")
    private void chooseExport(EqualizerPreset preset) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(MIME_TYPE)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE, preset.name.replace('/', '_').replace('\\', '_') + ".json");
        initialFolder(intent);
        try {
            this.store.prepareExport(preset);
            this.fileRequest = true;
            this.activity.startActivityForResult(intent, EXPORT_REQUEST);
        } catch (ActivityNotFoundException ex) {
            this.fileRequest = false;
            message("No document picker found");
        } catch (IOException | JSONException ex) {
            this.fileRequest = false;
            message("Preset could not be prepared for export");
        }
    }

    private static void initialFolder(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"));
        }
    }

    private void importFile(Uri uri) {
        Context context = this.activity.getApplicationContext();
        WeakReference<LocalPresets> owner = new WeakReference<>(this);
        transfer(() -> importFile(context, uri, owner));
    }

    private static void importFile(Context context, Uri uri, WeakReference<LocalPresets> owner) {
        try {
            EqualizerPresetFile file;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (!(input instanceof FileInputStream)) {
                    throw new IOException("Preset file descriptor could not be opened");
                }
                StructPollfd descriptor = new StructPollfd();
                descriptor.fd = ((FileInputStream) input).getFD();
                descriptor.events = (short) OsConstants.POLLIN;
                StructPollfd[] descriptors = { descriptor };
                long deadline = SystemClock.elapsedRealtime() + FILE_READ_TIMEOUT_MS;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[BUFFER_BYTES];
                while (true) {
                    long remaining = deadline - SystemClock.elapsedRealtime();
                    if (remaining <= 0 || Os.poll(descriptors, (int) remaining) == 0) {
                        throw new IOException("Preset file read timed out");
                    }
                    int count = input.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    if (bytes.size() + count > EqualizerPresetFile.MAX_BYTES) {
                        throw new IOException("Preset file exceeds size limit");
                    }
                    bytes.write(buffer, 0, count);
                }
                String document = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray()))
                    .toString();
                file = EqualizerPresetFile.parse(document);
            }
            new EqualizerPresetStore(context).add(file.name, file.curve);
            MAIN.post(() -> {
                LocalPresets presets = owner.get();
                if (presets == null) {
                    return;
                }
                presets.message((file.ignoredFields > 0)
                        ? "Preset imported. Unknown fields ignored: " + file.ignoredFields : "Preset imported");
                if (presets.selection != null && current.get() == presets && !presets.activity.isDestroyed()
                        && !presets.activity.isFinishing()) {
                    presets.showList();
                }
            });
        } catch (IOException | ErrnoException | JSONException | SecurityException | IllegalArgumentException ex) {
            postMessage(owner, "Import failed. Check local presets.");
        }
    }

    private void exportFile(Uri uri) {
        EqualizerPresetFile preset;
        try {
            preset = this.store.takeExport();
        } catch (IOException | JSONException ex) {
            message("Preset could not be prepared for export");
            return;
        }
        if (preset == null) {
            message("Export interrupted. Choose Export again.");
            return;
        }
        Context context = this.activity.getApplicationContext();
        WeakReference<LocalPresets> owner = new WeakReference<>(this);
        transfer(() -> exportFile(context, uri, preset, owner));
    }

    private static void exportFile(Context context, Uri uri, EqualizerPresetFile preset,
            WeakReference<LocalPresets> owner) {
        try (OutputStream output = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) {
                throw new IOException("Preset file could not be opened");
            }
            output.write(
                    EqualizerPresetFile.json(preset.name, preset.curve).toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException | SecurityException ex) {
            postMessage(owner, "Preset could not be exported");
            return;
        }
        MAIN.post(() -> {
            LocalPresets presets = owner.get();
            if (presets != null) {
                presets.message("Preset exported");
                presets.dispose();
            }
        });
    }

    private void transfer(Runnable operation) {
        this.transfers++;
        WeakReference<LocalPresets> owner = new WeakReference<>(this);
        try {
            FILES.execute(() -> {
                try {
                    operation.run();
                } finally {
                    MAIN.post(() -> {
                        LocalPresets presets = owner.get();
                        if (presets != null) {
                            presets.transfers--;
                        }
                    });
                }
            });
        } catch (RejectedExecutionException ex) {
            this.transfers--;
            message("Too many preset transfers. Try again when one finishes.");
        }
    }

    private static void postMessage(WeakReference<LocalPresets> owner, String text) {
        MAIN.post(() -> {
            LocalPresets presets = owner.get();
            if (presets != null) {
                presets.message(text);
            }
        });
    }

    private void message(String text) {
        if (!this.activity.isDestroyed() && !this.activity.isFinishing()) {
            Toast.makeText(this.activity, text, Toast.LENGTH_LONG).show();
        }
    }

    private void dispose() {
        this.activity.getApplication().unregisterActivityLifecycleCallbacks(this);
        if (this.dialog != null) {
            this.dialog.dismiss();
            this.dialog = null;
        }
        if (current.get() == this) {
            current = new WeakReference<>(null);
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity == this.activity) {
            dispose();
        }
    }

}
