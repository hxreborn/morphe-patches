/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class GalleryPickerActivity extends Activity {

    private static final String TAG = "GalleryPickerActivity";

    private static final int PICK = 1;

    private static final int MAX_PICKS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            startActivityForResult(pickerIntent(), PICK);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final List<Uri> picked = (resultCode == RESULT_OK && data != null) ? uris(data) : new ArrayList<>();
        if (picked.isEmpty()) {
            finish();
            return;
        }
        stage(picked);
    }

    private static Intent pickerIntent() {
        if (Build.VERSION.SDK_INT >= 33) {
            final Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, MAX_PICKS);
            return intent;
        }
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        return intent;
    }

    private static List<Uri> uris(Intent data) {
        final List<Uri> result = new ArrayList<>();
        final ClipData clip = data.getClipData();
        if (clip == null) {
            if (data.getData() != null) {
                result.add(data.getData());
            }
            return result;
        }
        for (int index = 0; index < clip.getItemCount() && result.size() < MAX_PICKS; index++) {
            result.add(clip.getItemAt(index).getUri());
        }
        return result;
    }

    private void stage(List<Uri> picked) {
        final Context context = getApplicationContext();
        final long stamp = System.currentTimeMillis();
        new Thread(() -> {
            final List<Uri> staged = new ArrayList<>();
            try {
                for (int index = 0; index < picked.size(); index++) {
                    final File target = new File(GalleryPhotos.directory(context),
                            "picked-" + stamp + "-" + index + ".jpg");
                    GalleryPhotos.store(context, picked.get(index), target);
                    staged.add(Uri.fromFile(target));
                }
            } catch (IOException | RuntimeException ex) {
                Log.e(TAG, "Picked photos not read", ex);
                staged.clear();
                runOnUiThread(() -> Toast.makeText(this, "Couldn't read these photos", Toast.LENGTH_SHORT).show());
            }
            runOnUiThread(() -> {
                GalleryButton.picked(staged);
                finish();
            });
        }).start();
    }

}
