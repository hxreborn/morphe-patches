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
package hx.dash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hx.mp4.Segment;

final class DashManifest {

    private static final String REPRESENTATION_ID = "$RepresentationID$";

    private static final Pattern NUMBER = Pattern.compile("\\$Number(?:%0(\\d+)d)?\\$");

    final List<Representation> representations = new ArrayList<>();

    double presentationSeconds;

    Representation select(String type, int maxHeight) {
        Representation best = null;
        Representation smallest = null;
        for (Representation candidate : this.representations) {
            if (!type.equals(candidate.type)) {
                continue;
            }
            if (smallest == null || candidate.height < smallest.height) {
                smallest = candidate;
            }
            if (candidate.height <= maxHeight && isBetter(candidate, best)) {
                best = candidate;
            }
        }
        return (best != null) ? best : smallest;
    }

    List<Segment> segmentsOf(Representation representation) throws IOException {
        Template template = representation.template;
        if (template.timescale <= 0) {
            throw new IOException("representation " + representation.id + " has no timescale");
        }
        List<Interval> timeline = timelineOf(template);
        if (timeline.isEmpty()) {
            throw new IOException("manifest lacks a segment timeline or segment duration");
        }
        List<Segment> segments = new ArrayList<>();
        long time = 0;
        int number = template.startNumber;
        for (Interval interval : timeline) {
            if (interval.duration <= 0 || interval.repeats < 0) {
                throw new IOException("representation " + representation.id + " has an invalid segment timeline");
            }
            if (interval.startTime >= 0) {
                time = interval.startTime;
            }
            for (long repeat = 0; repeat <= interval.repeats; repeat++) {
                String media = resolveMediaUrl(template.media, representation.id, number++);
                segments.add(new Segment(representation.baseUrl + media, time, interval.duration));
                time += interval.duration;
            }
        }
        return segments;
    }

    private static String resolveMediaUrl(String media, String representationId, int number) {
        Matcher matcher = NUMBER.matcher(media.replace(REPRESENTATION_ID, representationId));
        StringBuffer url = new StringBuffer();
        while (matcher.find()) {
            String width = matcher.group(1);
            String value = (width != null) ? String.format(Locale.US, "%0" + width + "d", number)
                    : Integer.toString(number);
            matcher.appendReplacement(url, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(url);
        return url.toString();
    }

    private static boolean isBetter(Representation candidate, Representation best) {
        return best == null || candidate.height > best.height
                || (candidate.height == best.height && candidate.bandwidth > best.bandwidth);
    }

    private List<Interval> timelineOf(Template template) {
        if (!template.timeline.isEmpty() || template.duration <= 0 || this.presentationSeconds <= 0) {
            return template.timeline;
        }
        long total = Math.round(this.presentationSeconds * template.timescale);
        long fullSegments = (total - 1) / template.duration;
        List<Interval> timeline = new ArrayList<>();
        if (fullSegments > 0) {
            timeline.add(new Interval(0, template.duration, fullSegments - 1));
        }
        timeline.add(new Interval(fullSegments * template.duration, total - fullSegments * template.duration, 0));
        return timeline;
    }

    static final class Representation {

        String id;

        String type;

        int height;

        int bandwidth;

        String baseUrl;

        Template template;

        String initializationUrl() throws IOException {
            String initialization = this.template.initialization;
            if (initialization == null) {
                throw new IOException("representation " + this.id + " has no initialization");
            }
            return this.baseUrl + initialization.replace(REPRESENTATION_ID, this.id);
        }

    }

    static final class Template {

        final List<Interval> timeline = new ArrayList<>();

        long timescale = 1;

        long duration;

        String initialization;

        String media;

        int startNumber = 1;

    }

    static final class Interval {

        final long startTime;

        final long duration;

        final long repeats;

        Interval(long startTime, long duration, long repeats) {
            this.startTime = startTime;
            this.duration = duration;
            this.repeats = repeats;
        }

    }

}
