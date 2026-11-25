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
package app.hxreborn.extension.outlook.theme;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import app.hxreborn.extension.outlook.settings.PatchSettings;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.R)
public final class PicturePickerFragment extends Fragment {

    private static final String FRAGMENT_TAG = "hx_custom_picture_picker";

    private static final int PICK_PICTURE_REQUEST = 0x4858;

    private static final String IMAGE_TYPE = "image/*";

    private static final String WORKER_NAME = "hxOutlookCustomPicture";

    private static final Executor PICTURE_SAVES = Executors
        .newSingleThreadExecutor((task) -> new Thread(task, WORKER_NAME));

    private Object dynamicViewModel;

    private Object appearanceViewModel;

    static void pick(Activity activity, Object dynamicViewModel, Object appearanceViewModel) {
        PicturePickerFragment fragment = new PicturePickerFragment();
        fragment.dynamicViewModel = dynamicViewModel;
        fragment.appearanceViewModel = appearanceViewModel;
        fragment.setRetainInstance(true);
        activity.getFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commitNow();
        fragment.startPicker();
    }

    private void startPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                startActivityForResult(new Intent(MediaStore.ACTION_PICK_IMAGES).setType(IMAGE_TYPE),
                        PICK_PICTURE_REQUEST);
                return;
            } catch (ActivityNotFoundException ex) {
                Log.w(PatchSettings.LOG_TAG, "No photo picker, opening the document picker", ex);
            }
        }
        try {
            startActivityForResult(
                    new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(IMAGE_TYPE),
                    PICK_PICTURE_REQUEST);
        } catch (ActivityNotFoundException ex) {
            Log.e(PatchSettings.LOG_TAG, "Cannot open a picture picker", ex);
            remove();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_PICTURE_REQUEST) {
            return;
        }
        Uri picture = (resultCode == Activity.RESULT_OK && data != null) ? data.getData() : null;
        if (picture == null) {
            remove();
            return;
        }
        Context application = getActivity().getApplicationContext();
        PICTURE_SAVES.execute(() -> save(application, picture));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.dynamicViewModel = null;
        this.appearanceViewModel = null;
    }

    private void save(Context application, Uri picture) {
        Handler main = new Handler(Looper.getMainLooper());
        SavedPicture saved;
        try {
            saved = PictureStore.save(application, picture);
        } catch (IOException | RuntimeException ex) {
            Log.e(PatchSettings.LOG_TAG, "Cannot decode or copy the picked picture", ex);
            main.post(this::remove);
            return;
        }
        main.post(() -> {
            Activity current = getActivity();
            try {
                CustomTheme.applyPicture((current != null) ? current : application, this.dynamicViewModel,
                        this.appearanceViewModel, saved);
            } finally {
                PictureStore.markApplied(saved);
            }
            remove();
        });
    }

    private void remove() {
        if (isAdded()) {
            getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }
    }

}
