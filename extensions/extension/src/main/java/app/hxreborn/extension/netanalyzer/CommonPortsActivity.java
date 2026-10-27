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
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class CommonPortsActivity extends HostScreenActivity {

    private static final String TAG = "hxNetAnalyzer";

    private static final String PREFERENCES = "CommonPorts";

    private static final String PORTS_KEY = "ports";

    private static final String SEPARATOR_PATTERN = "[,;:\\-./\\s]+";

    private static final String DISPLAY_SEPARATOR = ", ";

    private static final int MIN_PORT = 1;

    private static final int MAX_PORT = 65535;

    private static final int INSET_DP = 16;

    private static final String DEFAULT_PORTS = """
            1, 7, 20, 21, 22, 23, 25, 42, 43, 53, 69, 79, 80, 88, 106, \
            110, 111, 113, 115, 119, 123, 135, \
            137, 138, 139, 143, 161, 177, 194, 311, 379, 389, 427, 443, 445, 464, 465, 497, 514, 515, 532, \
            548, 554, 587, 600, 625, 631, 636, 660, 687, 749, 985, 993, 995, 1080, 1085, 1194, 1099, 1220, \
            1433, 1434, 1521, 1522, 1525, 1529, 1640, 1649, 1723, 1990, 1998, 2049, 2195, 2196, 2336, 3004, \
            3031, 3128, 3283, 3306, 3389, 3689, 4111, 4488, 5000, 5001, 5003, 5009, 5010, 5060, 5100, 5190, \
            5200, 5222, 5223, 5269, 5298, 5432, 5500, 5632, 5800, 5900, 5988, 6000, 7070, 7777, 8005, 8008, \
            8043, 8080, 8085, 8086, 8087, 8088, 8089, 8096, 8170, 8171, 8175, 8200, 8443, 8800, 8821, 8826, \
            8843, 8880, 8891, 9006, 9100, 10000, 10001, 10002, 10010, 20005\
            """;

    private EditText ports;

    @Override
    String screenTitle() {
        return "Common Port List";
    }

    @Override
    void buildContent() {
        addToolbarIcon(MaterialIcons.MORE, "More",
                (view) -> new DropdownMenu(this).add("Reset", null, this::confirmReset).show(view));
        this.ports = new EditText(this);
        this.ports.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        this.ports.setGravity(Gravity.TOP | Gravity.START);
        BODY_LARGE.applyTo(this.ports);
        this.ports.setTextColor(this.palette.keyText);
        this.ports.setBackground(null);
        this.ports.setPaddingRelative(dp(INSET_DP), dp(INSET_DP), dp(INSET_DP), dp(INSET_DP));
        this.ports.setText(TextUtils.join(DISPLAY_SEPARATOR, load()));
        replaceBody(this.ports);
    }

    @Override
    protected void onPause() {
        super.onPause();
        save(parse(this.ports.getText().toString()));
    }

    private List<Integer> load() {
        final String json = preferences().getString(PORTS_KEY, null);
        if (json != null) {
            try {
                final JSONArray saved = new JSONObject(json).getJSONArray(PORTS_KEY);
                final List<Integer> result = new ArrayList<>();
                for (int i = 0; i < saved.length(); i++) {
                    result.add(saved.getInt(i));
                }
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (JSONException ex) {
                Log.e(TAG, "Cannot read the saved common ports", ex);
            }
        }
        return parse(DEFAULT_PORTS);
    }

    private void save(List<Integer> values) {
        final JSONArray array = new JSONArray();
        for (int value : values) {
            array.put(value);
        }
        try {
            preferences().edit().putString(PORTS_KEY, new JSONObject().put(PORTS_KEY, array).toString()).apply();
        } catch (JSONException ex) {
            Log.e(TAG, "Cannot save the common ports", ex);
        }
    }

    private static List<Integer> parse(String text) {
        final List<Integer> values = new ArrayList<>();
        for (String token : text.trim().split(SEPARATOR_PATTERN)) {
            try {
                final int port = Integer.parseInt(token);
                if (port >= MIN_PORT && port <= MAX_PORT) {
                    values.add(port);
                }
            } catch (NumberFormatException ex) {
                Log.d(TAG, "Skipping a non-numeric common port: " + token);
            }
        }
        return values;
    }

    private void confirmReset() {
        new AlertDialog.Builder(this).setTitle("Warning")
            .setMessage("Do you want to reset the port list to the default value?")
            .setPositiveButton("OK", (dialog, which) -> this.ports.setText(DEFAULT_PORTS))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private SharedPreferences preferences() {
        return getSharedPreferences(PREFERENCES, MODE_PRIVATE);
    }

}
