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
import java.nio.charset.StandardCharsets;
import java.util.List;

import hx.mp4.Segment;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class DashManifestParserTest {

    private static final String MANIFEST_URL = "https://cdn.example.com/title/index.mpd";

    private static final String MANIFEST = "<?xml version=\"1.0\"?>"
            + "<MPD mediaPresentationDuration=\"PT10S\" xmlns=\"urn:mpeg:dash:schema:mpd:2011\">"
            + "<BaseURL>https://media.example.com/title/</BaseURL><Period>" + "<AdaptationSet contentType=\"video\">"
            + "<SegmentTemplate timescale=\"1000\" initialization=\"v/$RepresentationID$/init.mp4\" "
            + "media=\"v/$RepresentationID$/$Number%03d$.m4s\" startNumber=\"1\"><SegmentTimeline>"
            + "<S t=\"0\" d=\"4000\" r=\"1\"/><S d=\"2000\"/></SegmentTimeline></SegmentTemplate>"
            + "<Representation id=\"v720\" height=\"720\" bandwidth=\"1000000\"/>"
            + "<Representation id=\"v1080\" height=\"1080\" bandwidth=\"2000000\"/></AdaptationSet>"
            + "<AdaptationSet mimeType=\"audio/mp4\"><Representation id=\"a1\" bandwidth=\"128000\">"
            + "<SegmentTemplate timescale=\"48000\" initialization=\"a/init.mp4\" media=\"a/$Number$.m4s\" "
            + "duration=\"192000\"/></Representation></AdaptationSet></Period></MPD>";

    @Test
    public void readsThePresentationDuration() throws IOException {
        // when
        DashManifest manifest = parse(MANIFEST);

        // then
        Assertions.assertThat(manifest.presentationSeconds).isEqualTo(10.0);
    }

    @Test
    public void listsEveryRepresentationThatHasATemplate() throws IOException {
        // when
        DashManifest manifest = parse(MANIFEST);

        // then
        Assertions.assertThat(manifest.representations.size()).isEqualTo(3);
    }

    @Test
    public void picksTheTallestVideoWithinTheRequestedHeight() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        DashManifest.Representation video = manifest.select("video", 1080);

        // then
        Assertions.assertThat(video.id).isEqualTo("v1080");
    }

    @Test
    public void picksTheTallestVideoUnderACap() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        DashManifest.Representation capped = manifest.select("video", 900);

        // then
        Assertions.assertThat(capped.id).isEqualTo("v720");
    }

    @Test
    public void fallsBackToTheSmallestVideoWhenNoneFits() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        DashManifest.Representation video = manifest.select("video", 360);

        // then
        Assertions.assertThat(video.id).isEqualTo("v720");
    }

    @Test
    public void typesARepresentationFromTheMimeTypeOfItsSet() throws IOException {
        // when
        DashManifest.Representation audio = parse(MANIFEST).select("audio", Integer.MAX_VALUE);

        // then
        Assertions.assertThat(audio.id).isEqualTo("a1");
    }

    @Test
    public void givesNoRepresentationForAnAbsentType() throws IOException {
        // when
        DashManifest.Representation text = parse(MANIFEST).select("text", Integer.MAX_VALUE);

        // then
        Assertions.assertThat(text).isEqualTo(null);
    }

    @Test
    public void expandsATimelineIntoNumberedSegments() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        List<Segment> segments = manifest.segmentsOf(manifest.select("video", 720));

        // then
        Assertions.assertThat(segments.size()).isEqualTo(3);
        Assertions.assertThat(segments.get(0).url).isEqualTo("https://media.example.com/title/v/v720/001.m4s");
        Assertions.assertThat(segments.get(2).url).isEqualTo("https://media.example.com/title/v/v720/003.m4s");
        Assertions.assertThat(segments.get(0).startTime).isEqualTo(0);
        Assertions.assertThat(segments.get(1).startTime).isEqualTo(4000);
        Assertions.assertThat(segments.get(2).startTime).isEqualTo(8000);
        Assertions.assertThat(segments.get(2).duration).isEqualTo(2000);
    }

    @Test
    public void cutsFixedDurationSegmentsToThePresentationLength() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        List<Segment> segments = manifest.segmentsOf(manifest.select("audio", Integer.MAX_VALUE));

        // then
        Assertions.assertThat(segments.size()).isEqualTo(3);
        Assertions.assertThat(segments.get(0).url).isEqualTo("https://media.example.com/title/a/1.m4s");
        Assertions.assertThat(segments.get(1).startTime).isEqualTo(192000);
        Assertions.assertThat(segments.get(2).startTime).isEqualTo(384000);
        Assertions.assertThat(segments.get(2).duration).isEqualTo(96000);
    }

    @Test
    public void resolvesTheVideoInitializationUrl() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        String video = manifest.select("video", 720).initializationUrl();

        // then
        Assertions.assertThat(video).isEqualTo("https://media.example.com/title/v/v720/init.mp4");
    }

    @Test
    public void resolvesTheAudioInitializationUrl() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        String audio = manifest.select("audio", Integer.MAX_VALUE).initializationUrl();

        // then
        Assertions.assertThat(audio).isEqualTo("https://media.example.com/title/a/init.mp4");
    }

    @Test
    public void resolvesBaseUrlsAgainstTheManifestWhenThereIsNone() throws IOException {
        // given
        String xml = "<MPD><Period><AdaptationSet contentType=\"video\"><Representation id=\"v\" height=\"720\">"
                + "<SegmentTemplate timescale=\"1\" initialization=\"i.mp4\" media=\"$Number$.m4s\" duration=\"1\"/>"
                + "</Representation></AdaptationSet></Period></MPD>";

        // when
        DashManifest.Representation video = parse(xml).select("video", 720);

        // then
        Assertions.assertThat(video.initializationUrl()).isEqualTo("https://cdn.example.com/title/i.mp4");
    }

    @Test
    public void rejectsARepresentationWithoutAnInitialization() throws IOException {
        // given
        String xml = "<MPD><Period><AdaptationSet contentType=\"video\"><Representation id=\"v\" height=\"720\">"
                + "<SegmentTemplate timescale=\"1\" media=\"$Number$.m4s\" duration=\"1\"/></Representation>"
                + "</AdaptationSet></Period></MPD>";
        DashManifest.Representation video = parse(xml).select("video", 720);

        // when
        Throwable failure = Assertions.catchThrowable(() -> video.initializationUrl());

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class);
        Assertions.assertThat(failure.getMessage().contains("no initialization")).as(failure.getMessage()).isTrue();
    }

    @Test
    public void rejectsATemplateWithoutATimelineOrDuration() throws IOException {
        // given
        String xml = "<MPD><Period><AdaptationSet contentType=\"video\"><Representation id=\"v\" height=\"720\">"
                + "<SegmentTemplate timescale=\"1\" initialization=\"i.mp4\" media=\"$Number$.m4s\"/>"
                + "</Representation></AdaptationSet></Period></MPD>";
        DashManifest manifest = parse(xml);

        // when
        Throwable failure = Assertions.catchThrowable(() -> manifest.segmentsOf(manifest.select("video", 720)));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class);
        Assertions.assertThat(failure.getMessage().contains("lacks a segment timeline"))
            .as(failure.getMessage())
            .isTrue();
    }

    @Test
    public void rejectsATimelineWithAnEmptySegment() throws IOException {
        // given
        String xml = "<MPD><Period><AdaptationSet contentType=\"video\"><Representation id=\"v\" height=\"720\">"
                + "<SegmentTemplate timescale=\"1\" initialization=\"i.mp4\" media=\"$Number$.m4s\"><SegmentTimeline>"
                + "<S t=\"0\" d=\"0\"/></SegmentTimeline></SegmentTemplate></Representation></AdaptationSet>"
                + "</Period></MPD>";
        DashManifest manifest = parse(xml);

        // when
        Throwable failure = Assertions.catchThrowable(() -> manifest.segmentsOf(manifest.select("video", 720)));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class);
        Assertions.assertThat(failure.getMessage().contains("invalid segment timeline"))
            .as(failure.getMessage())
            .isTrue();
    }

    @Test
    public void rejectsManifestsThatAreNotXml() {
        // when
        Throwable failure = Assertions
            .catchThrowable(() -> DashManifestParser.parse("<MPD".getBytes(StandardCharsets.UTF_8), MANIFEST_URL));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class);
        Assertions.assertThat(failure.getMessage().startsWith("cannot parse manifest"))
            .as(failure.getMessage())
            .isTrue();
    }

    @Test
    public void rejectsAMalformedPresentationDuration() {
        // when
        Throwable failure = Assertions.catchThrowable(() -> parse("<MPD mediaPresentationDuration=\"ten seconds\"/>"));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class);
        Assertions.assertThat(failure.getMessage().startsWith("cannot parse manifest"))
            .as(failure.getMessage())
            .isTrue();
    }

    @Test
    public void rejectsManifestsNestedTooDeep() {
        // given
        StringBuilder xml = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            xml.append("<a>");
        }

        // when
        Throwable failure = Assertions.catchThrowable(() -> parse(xml.toString()));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class);
        Assertions.assertThat(failure.getMessage().contains("nested deeper than 32")).as(failure.getMessage()).isTrue();
    }

    @Test
    public void addsUpIsoDurations() throws IOException {
        // when
        DashManifest manifest = parse("<MPD mediaPresentationDuration=\"P1DT2H3M4.5S\"/>");

        // then
        Assertions.assertThat(manifest.presentationSeconds).isEqualTo(86400 + 7200 + 180 + 4.5);
    }

    @Test
    public void readsAMissingDurationAsZero() throws IOException {
        // when
        DashManifest manifest = parse("<MPD/>");

        // then
        Assertions.assertThat(manifest.presentationSeconds).isEqualTo(0.0);
    }

    private static DashManifest parse(String xml) throws IOException {
        return DashManifestParser.parse(xml.getBytes(StandardCharsets.UTF_8), MANIFEST_URL);
    }

}
