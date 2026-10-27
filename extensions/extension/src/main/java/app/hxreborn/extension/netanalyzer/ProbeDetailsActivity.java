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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class ProbeDetailsActivity extends HostScreenActivity {

    private static final String RTT_SEPARATOR = "; ";

    private static final String RTT_FORMAT = "%.1f";

    private static final String COORDINATE_FORMAT = "%.2f";

    private static final String SEND_TIME_PATTERN = "HH:mm:ss";

    private static final int MAP_HEIGHT_DP = 250;

    private static Probe selected;

    private TileMapView map;

    static void show(Probe probe) {
        selected = probe;
        HostActivity.open(ProbeDetailsActivity.class.getName(), null);
    }

    @Override
    String screenTitle() {
        return "Host Details";
    }

    @Override
    void buildContent() {
        final Probe probe = selected;
        if (probe == null) {
            finish();
            return;
        }
        final ExternalIp location = probe.location;

        section("Network");
        onOffRow("IP Address", (probe.address != null) ? probe.address.getHostAddress() : null);
        onOffRow("Hostname", present(probe.hostname));
        onOffRow("RTT", rtt(probe));
        onOffRow("TTL", (probe.ttl != 0) ? String.valueOf(probe.ttl) : null);
        onOffRow("Probe Send Time", (probe.time != null)
                ? new SimpleDateFormat(SEND_TIME_PATTERN, Locale.getDefault()).format(probe.time) : null);
        onOffRow("AS Number", present((location != null) ? location.asNumber : null));
        onOffRow("AS Name", present((location != null) ? location.asName : null));

        if (location == null) {
            return;
        }
        section("Location");
        onOffRow("Country Name", present(location.countryName));
        onOffRow("Country Code", present(location.countryCode));
        onOffRow("Time Zone", present(location.timeZone));
        onOffRow("Region", present(location.region));
        onOffRow("City", present(location.city));
        final boolean cityKnown = location.city != null && location.latitude != null && location.longitude != null;
        onOffRow("Latitude", cityKnown ? String.format(Locale.US, COORDINATE_FORMAT, location.latitude) : null);
        onOffRow("Longitude", cityKnown ? String.format(Locale.US, COORDINATE_FORMAT, location.longitude) : null);
        if (cityKnown) {
            this.map = new TileMapView(this, this.palette);
            this.map.setMarkers(Collections.singletonList(new MapMarker(location.latitude, location.longitude,
                    probe.destination ? MapMarker.TYPE_DESTINATION : MapMarker.TYPE_HOP, "", "")), false);
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

    private static String rtt(Probe probe) {
        if (!probe.delays.isEmpty()) {
            final List<String> values = new ArrayList<>();
            for (double delay : probe.delays) {
                values.add(String.format(Locale.US, RTT_FORMAT, delay));
            }
            return TextUtils.join(RTT_SEPARATOR, values);
        }
        return probe.pingable ? String.format(Locale.US, RTT_FORMAT, probe.delay) : null;
    }

    private static String present(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }

}
