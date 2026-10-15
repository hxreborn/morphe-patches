/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class VisualTracerouteActivity extends HostScreenActivity {

    private static final long REFRESH_MILLIS = 1000;

    private static final String CONTINENT_CODE_EUROPE = "EU";

    private static final String CURRENT_LOCATION = "Current Location";

    private static final String HOP_PREFIX = "#";

    private static final String RANGE_SEPARATOR = " - ";

    private static final String DELAY_FORMAT = "%.1fms";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable refresh = this::refresh;

    private final Map<MapMarker, Probe> markerProbes = new HashMap<>();

    private TileMapView map;

    private TextView toggle;

    private boolean running;

    private String shownPositions = "";

    @Override
    String screenTitle() {
        return "Visual Traceroute";
    }

    @Override
    void buildContent() {
        this.toggle = addToolbarText("Start", this.palette.startAction, (view) -> {
            if (this.running) {
                HostTools.stopTraceroute();
            } else {
                HostTools.restartTraceroute();
            }
            refresh();
        });
        this.map = new TileMapView(this, this.palette);
        this.map.setOnMarkerClickListener((marker) -> {
            final Probe probe = this.markerProbes.get(marker);
            if (probe != null) {
                ProbeDetailsActivity.show(probe);
            }
        });
        replaceBody(this.map);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.handler.removeCallbacks(this.refresh);
    }

    @Override
    protected void onDestroy() {
        this.map.release();
        super.onDestroy();
    }

    private void refresh() {
        final HostTools.Snapshot snapshot = HostTools.traceroute();
        this.running = snapshot.active;
        showRunState(this.toggle, this.running, HostTools.canRestartTraceroute());
        final List<MapMarker> markers = new ArrayList<>();
        final Map<MapMarker, Probe> probes = new HashMap<>();
        final MapMarker user = userMarker();
        if (user != null && !snapshot.probes.isEmpty()) {
            markers.add(user);
        }
        markers.addAll(hopMarkers(snapshot.probes, probes));
        final String positions = positions(markers);
        if (!positions.equals(this.shownPositions)) {
            this.shownPositions = positions;
            this.markerProbes.clear();
            this.markerProbes.putAll(probes);
            this.map.setMarkers(markers, !snapshot.active);
        }
        this.handler.removeCallbacks(this.refresh);
        this.handler.postDelayed(this.refresh, REFRESH_MILLIS);
    }

    static List<MapMarker> hopMarkers(List<Probe> hops, Map<MapMarker, Probe> probes) {
        final List<double[]> positions = new ArrayList<>();
        final List<Integer> pending = new ArrayList<>();
        String country = null;
        double[] previous = null;
        String previousCountry = null;
        for (Probe hop : hops) {
            final ExternalIp location = hop.location;
            String code = (location != null) ? location.countryCode : null;
            if (CONTINENT_CODE_EUROPE.equals(code) && country != null) {
                code = country;
            }
            if (code != null && !code.equals(country)) {
                pending.clear();
                country = code;
            }
            double[] position = null;
            if (location != null && location.city != null && location.latitude != null && location.longitude != null) {
                position = new double[] { location.latitude, location.longitude };
                for (int index : pending) {
                    positions.set(index, position);
                }
                pending.clear();
            } else if (previous != null && code != null && code.equals(previousCountry)) {
                position = previous;
            } else if (code != null && CountryCapitals.of(code) != null) {
                position = CountryCapitals.of(code);
                pending.add(positions.size());
            } else {
                position = previous;
            }
            positions.add(position);
            if (position != null) {
                previous = position;
                previousCountry = code;
            }
        }
        return group(hops, positions, probes);
    }

    private static List<MapMarker> group(List<Probe> hops, List<double[]> positions, Map<MapMarker, Probe> probes) {
        final List<MapMarker> markers = new ArrayList<>();
        int start = 0;
        while (start < hops.size()) {
            int end = start;
            while (end + 1 < hops.size() && samePosition(positions.get(end + 1), positions.get(start))) {
                end++;
            }
            final double[] position = positions.get(start);
            if (position != null && !(position[0] == 0 && position[1] == 0)) {
                boolean destination = false;
                double minDelay = Double.MAX_VALUE;
                double maxDelay = 0;
                for (int i = start; i <= end; i++) {
                    destination |= hops.get(i).destination;
                    minDelay = Math.min(minDelay, hops.get(i).delay);
                    maxDelay = Math.max(maxDelay, hops.get(i).delay);
                }
                final String title = (start == end) ? HOP_PREFIX + (start + 1)
                        : HOP_PREFIX + (start + 1) + RANGE_SEPARATOR + HOP_PREFIX + (end + 1);
                final String snippet = (minDelay == maxDelay) ? String.format(Locale.US, DELAY_FORMAT, minDelay)
                        : String.format(Locale.US, DELAY_FORMAT, minDelay) + RANGE_SEPARATOR
                                + String.format(Locale.US, DELAY_FORMAT, maxDelay);
                final MapMarker marker = new MapMarker(position[0], position[1],
                        destination ? MapMarker.TYPE_DESTINATION : MapMarker.TYPE_HOP, title, snippet);
                markers.add(marker);
                probes.put(marker, hops.get(start));
            }
            start = end + 1;
        }
        return markers;
    }

    private static boolean samePosition(double[] left, double[] right) {
        return left == right || (left != null && right != null && left[0] == right[0] && left[1] == right[1]);
    }

    @SuppressLint("MissingPermission")
    private MapMarker userMarker() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        final LocationManager locations = getSystemService(LocationManager.class);
        Location best = null;
        for (String provider : locations.getProviders(true)) {
            final Location location = locations.getLastKnownLocation(provider);
            if (location != null && (best == null || location.getTime() > best.getTime())) {
                best = location;
            }
        }
        return (best != null) ? new MapMarker(best.getLatitude(), best.getLongitude(), MapMarker.TYPE_USER_LOCATION,
                CURRENT_LOCATION, "") : null;
    }

    private static String positions(List<MapMarker> markers) {
        final StringBuilder key = new StringBuilder();
        for (MapMarker marker : markers) {
            key.append(marker.latitude)
                .append(',')
                .append(marker.longitude)
                .append(marker.title)
                .append(marker.snippet);
        }
        return key.toString();
    }

}
