/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class ChannelUsage {

    static final int SERIES_ALPHA = 183;

    static final int[] SERIES_PALETTE = { 0xE91E63, 0x2196F3, 0x009688, 0xFF9800, 0x673AB7, 0x8BC34A, 0xFF5722,
            0x00BCD4, 0x795548, 0xFFC107, 0x3F51B5, 0x4CAF50, 0xF44336, 0x03A9F4, 0xCDDC39 };

    static final int Y_AXIS_MAX = 100;

    private static final int ALPHA_SHIFT = 24;

    private static final String SERIES_KEY_SEPARATOR = "@@@@";

    private static final String HIDDEN_SSID = "<hidden>";

    private static final int WEIGHT_OFFSET_DBM = 100;

    private static final int[] WIDTHS_MHZ = { 20, 40, 80, 160, 80, 320 };

    private static final int DEFAULT_WIDTH_MHZ = 20;

    private static final int WIDTH_INDEX_40_MHZ = 1;

    private static final int WIDTH_INDEX_80_PLUS_80 = 4;

    private static final int WIDTH_INDEX_320_MHZ = 5;

    private static final int BUCKET_MHZ = 10;

    private static final int CHANNEL_SPACING_MHZ = 5;

    private static final int BAND_2GHZ_BASE_MHZ = 2407;

    private static final int JAPAN_CHANNEL = 14;

    private static final int JAPAN_CHANNEL_MHZ = 2484;

    private static final int BAND_5GHZ_START_MHZ = 3000;

    private static final int BAND_5GHZ_BASE_MHZ = 5000;

    private static final int BAND_6GHZ_START_MHZ = 5925;

    private static final int BAND_6GHZ_BASE_MHZ = 5950;

    private static final int BAND_6GHZ_LOW_CHANNEL = 2;

    private static final int BAND_6GHZ_LOW_CHANNEL_MHZ = 5935;

    private static final int HT40_CENTER_OFFSET = 2;

    private static final int HT40_LOW_PRIMARY_MAX = 4;

    private static final int HT40_HIGH_PRIMARY_MIN = 10;

    private static final int[] CENTERS_320_MHZ = { 31, 63, 95, 127, 159, 191 };

    private static final int JAPAN_BUCKET_CENTER = 15;

    private static final int BAND_5GHZ_UPPER_FIRST_CHANNEL = 100;

    private static final int BAND_5GHZ_UPPER_GAP = 28;

    private static final int BAND_6GHZ_BUCKET_SHIFT = 1;

    private static final int LAST_2GHZ_LABEL = 13;

    private static final int[] LOW_5GHZ_LABELS = { 36, 44, 52, 60 };

    private static final int UPPER_5GHZ_LABEL_STEP = 8;

    private static final int UPPER_5GHZ_LABEL_MAX = 172;

    private static final int LABEL_6GHZ_STEP = 20;

    private static final int LABEL_6GHZ_LAST = 221;

    private final Band band;

    private final List<Network> networks = new ArrayList<>();

    private final Map<Integer, List<Network>> stacks = new TreeMap<>();

    private int maxStack;

    ChannelUsage(Band band, List<Network> scanned) {
        this.band = band;
        for (Network network : scanned) {
            if (band.accepts(network)) {
                this.networks.add(network);
            }
        }
        Collections.sort(this.networks, (left, right) -> right.level - left.level);
        for (Network network : this.networks) {
            spread(network, network.centerChannel);
            if (network.secondCenterChannel != 0) {
                spread(network, network.secondCenterChannel);
            }
        }
        for (List<Network> stack : this.stacks.values()) {
            int total = 0;
            for (Network network : stack) {
                total += network.weight();
            }
            this.maxStack = Math.max(this.maxStack, total);
        }
    }

    Band band() {
        return this.band;
    }

    Map<Integer, List<Network>> stacks() {
        return this.stacks;
    }

    float segmentHeight(Network network) {
        return (this.maxStack == 0) ? 0f : (float) network.weight() / this.maxStack * Y_AXIS_MAX;
    }

    static int channelOf(int frequency) {
        if (frequency <= 0) {
            return 0;
        }
        if (frequency == JAPAN_CHANNEL_MHZ) {
            return JAPAN_CHANNEL;
        }
        if (frequency < BAND_5GHZ_START_MHZ) {
            return (frequency - BAND_2GHZ_BASE_MHZ) / CHANNEL_SPACING_MHZ;
        }
        if (frequency < BAND_6GHZ_START_MHZ) {
            return (frequency - BAND_5GHZ_BASE_MHZ) / CHANNEL_SPACING_MHZ;
        }
        if (frequency == BAND_6GHZ_LOW_CHANNEL_MHZ) {
            return BAND_6GHZ_LOW_CHANNEL;
        }
        return (frequency - BAND_6GHZ_BASE_MHZ) / CHANNEL_SPACING_MHZ;
    }

    private void spread(Network network, int center) {
        final int middle = this.band.bucketCenter(center);
        final int half = network.width / BUCKET_MHZ;
        for (int bucket = middle - half; bucket < middle + half; bucket++) {
            List<Network> stack = this.stacks.get(bucket);
            if (stack == null) {
                stack = new ArrayList<>();
                this.stacks.put(bucket, stack);
            }
            stack.add(network);
        }
    }

    private static int centerOf(int primary, int frequency, int widthIndex, int centerFrequency0) {
        if (widthIndex == WIDTH_INDEX_40_MHZ && frequency < BAND_5GHZ_START_MHZ) {
            if (centerFrequency0 > frequency) {
                return primary + HT40_CENTER_OFFSET;
            }
            if (centerFrequency0 > 0 && centerFrequency0 < frequency) {
                return primary - HT40_CENTER_OFFSET;
            }
            if (primary <= HT40_LOW_PRIMARY_MAX) {
                return primary + HT40_CENTER_OFFSET;
            }
            return (primary >= HT40_HIGH_PRIMARY_MIN) ? primary - HT40_CENTER_OFFSET : 0;
        }
        final int center = channelOf(centerFrequency0);
        if (widthIndex != WIDTH_INDEX_320_MHZ || center == 0) {
            return center;
        }
        int nearest = CENTERS_320_MHZ[0];
        for (int candidate : CENTERS_320_MHZ) {
            if (Math.abs(candidate - center) < Math.abs(nearest - center)) {
                nearest = candidate;
            }
        }
        return nearest;
    }

    enum Band {

        GHZ_2_4(1, 15, 0, BAND_5GHZ_START_MHZ, -1, 15, -2, 16, 18),

        GHZ_5(32, 178, BAND_5GHZ_BASE_MHZ, BAND_6GHZ_START_MHZ - 1, 32, 178, 32, 148, 29),

        GHZ_6(1, 234, BAND_6GHZ_START_MHZ, Integer.MAX_VALUE, -5, 234, -5, 230, 47);

        final int axisMin;

        final int axisMax;

        final int labelCount;

        final int firstBucket;

        final int endBucket;

        private final int minChannel;

        private final int endChannel;

        private final int minFrequency;

        private final int maxFrequency;

        Band(int minChannel, int endChannel, int minFrequency, int maxFrequency, int firstBucket, int endBucket,
                int axisMin, int axisMax, int labelCount) {
            this.minChannel = minChannel;
            this.endChannel = endChannel;
            this.minFrequency = minFrequency;
            this.maxFrequency = maxFrequency;
            this.firstBucket = firstBucket;
            this.endBucket = endBucket;
            this.axisMin = axisMin;
            this.axisMax = axisMax;
            this.labelCount = labelCount;
        }

        boolean accepts(Network network) {
            return network.primaryChannel >= this.minChannel && network.primaryChannel < this.endChannel
                    && network.frequency >= this.minFrequency && network.frequency <= this.maxFrequency;
        }

        int bucketCenter(int center) {
            if (this == GHZ_2_4) {
                return (center == JAPAN_CHANNEL) ? JAPAN_BUCKET_CENTER : center;
            }
            if (this == GHZ_5) {
                return (center >= BAND_5GHZ_UPPER_FIRST_CHANNEL) ? center - BAND_5GHZ_UPPER_GAP : center;
            }
            return center - BAND_6GHZ_BUCKET_SHIFT;
        }

        String tickLabel(int position) {
            if (this == GHZ_2_4) {
                return (position >= 1 && position <= LAST_2GHZ_LABEL) ? String.valueOf(position) : "";
            }
            if (this == GHZ_5) {
                for (int label : LOW_5GHZ_LABELS) {
                    if (label == position) {
                        return String.valueOf(position);
                    }
                }
                final int channel = position + BAND_5GHZ_UPPER_GAP;
                final boolean upper = channel >= BAND_5GHZ_UPPER_FIRST_CHANNEL && channel <= UPPER_5GHZ_LABEL_MAX
                        && (channel - BAND_5GHZ_UPPER_FIRST_CHANNEL) % UPPER_5GHZ_LABEL_STEP == 0;
                return upper ? String.valueOf(channel) : "";
            }
            final int channel = position + BAND_6GHZ_BUCKET_SHIFT;
            final boolean labelled = position >= 0 && channel <= LABEL_6GHZ_LAST && position % LABEL_6GHZ_STEP == 0;
            return labelled ? String.valueOf(channel) : "";
        }

    }

    static final class Network {

        final String ssid;

        final String bssid;

        final int frequency;

        final int level;

        final int primaryChannel;

        final int centerChannel;

        final int secondCenterChannel;

        final int width;

        private Network(String ssid, String bssid, int frequency, int level, int primaryChannel, int centerChannel,
                int secondCenterChannel, int width) {
            this.ssid = ssid;
            this.bssid = bssid;
            this.frequency = frequency;
            this.level = level;
            this.primaryChannel = primaryChannel;
            this.centerChannel = centerChannel;
            this.secondCenterChannel = secondCenterChannel;
            this.width = width;
        }

        static Network of(String ssid, String bssid, int frequency, int level, int widthIndex, int centerFrequency0,
                int centerFrequency1) {
            final String name = (ssid == null || ssid.isEmpty()) ? HIDDEN_SSID : ssid;
            final int primary = channelOf(frequency);
            final int index = (widthIndex >= 0 && widthIndex < WIDTHS_MHZ.length) ? widthIndex : 0;
            int center = primary;
            int second = 0;
            if (index > 0) {
                center = centerOf(primary, frequency, index, centerFrequency0);
            }
            if (index == WIDTH_INDEX_80_PLUS_80) {
                second = channelOf(centerFrequency1);
            }
            if (center <= 0) {
                return new Network(name, bssid, frequency, level, primary, primary, 0, DEFAULT_WIDTH_MHZ);
            }
            return new Network(name, bssid, frequency, level, primary, center, second, WIDTHS_MHZ[index]);
        }

        int weight() {
            return Math.max(0, this.level + WEIGHT_OFFSET_DBM);
        }

        String seriesKey() {
            return this.ssid + SERIES_KEY_SEPARATOR + this.bssid;
        }

    }

    static final class Colors {

        private final Map<String, Integer> assigned = new HashMap<>();

        private final Map<Band, Integer> counters = new HashMap<>();

        int of(Band band, Network network) {
            final String key = band.name() + network.seriesKey();
            Integer color = this.assigned.get(key);
            if (color == null) {
                final Integer counter = this.counters.get(band);
                final int next = (counter != null) ? counter : 0;
                this.counters.put(band, next + 1);
                color = (SERIES_ALPHA << ALPHA_SHIFT) | SERIES_PALETTE[next % SERIES_PALETTE.length];
                this.assigned.put(key, color);
            }
            return color;
        }

    }

}
