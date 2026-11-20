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

import java.util.Collections;
import java.util.Locale;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class ExternalIpActivity extends HostScreenActivity {

    private static final String TAG = "hxNetAnalyzer";

    private static final String NOT_AVAILABLE = "N/A";

    private static final String COORDINATE_FORMAT = "%.2f";

    private static final int MAP_HEIGHT_DP = 250;

    private static Object stateHolder;

    private static boolean ipv6;

    private static Runnable reload;

    private TileMapView map;

    static void show(Object holder, boolean showIpv6, Runnable reloadAction) {
        stateHolder = holder;
        ipv6 = showIpv6;
        reload = reloadAction;
        HostActivity.open(ExternalIpActivity.class.getName(), null);
    }

    @Override
    String screenTitle() {
        return ipv6 ? "External IPv6" : "External IP";
    }

    @Override
    void buildContent() {
        final ExternalIp details;
        try {
            details = (stateHolder != null) ? ExternalIp.read(stateHolder) : null;
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "Cannot read the external IP state", ex);
            finish();
            return;
        }
        if (details == null) {
            finish();
            return;
        }
        section("Network");
        buttonRow("IP Address", (details.address != null) ? details.address.getHostAddress() : NOT_AVAILABLE, "Reload",
                (view) -> {
                    if (reload != null) {
                        reload.run();
                    }
                    finish();
                });
        onOffRow("Hostname", present(details.hostname));
        onOffRow("AS Number", present(details.asNumber));
        onOffRow("AS Name", present(details.asName));

        section("Location");
        onOffRow("Country Name", present(details.countryName));
        onOffRow("Country Code", present(details.countryCode));
        onOffRow("Time Zone", present(details.timeZone));
        onOffRow("Region", present(details.region));
        onOffRow("City", present(details.city));
        final boolean cityKnown = details.city != null && details.latitude != null && details.longitude != null
                && (details.latitude != 0 || details.longitude != 0);
        onOffRow("Latitude", cityKnown ? String.format(Locale.US, COORDINATE_FORMAT, details.latitude) : null);
        onOffRow("Longitude", cityKnown ? String.format(Locale.US, COORDINATE_FORMAT, details.longitude) : null);
        if (cityKnown) {
            this.map = new TileMapView(this, this.palette);
            this.map.setMarkers(Collections
                .singletonList(new MapMarker(details.latitude, details.longitude, MapMarker.TYPE_HOP, "", "")), false);
            this.list.addView(this.map,
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(MAP_HEIGHT_DP)));
        }
    }

    @Override
    protected void onDestroy() {
        if (this.map != null) {
            this.map.release();
        }
        super.onDestroy();
    }

    private static String present(String value) {
        return (value != null && !value.isEmpty()) ? value : null;
    }

}
