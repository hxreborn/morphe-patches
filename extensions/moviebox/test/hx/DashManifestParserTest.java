/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        assertEquals(10.0, manifest.presentationSeconds, 0.0);
    }

    @Test
    public void listsEveryRepresentationThatHasATemplate() throws IOException {
        // when
        DashManifest manifest = parse(MANIFEST);

        // then
        assertEquals(3, manifest.representations.size());
    }

    @Test
    public void picksTheTallestVideoWithinTheRequestedHeight() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        DashManifest.Representation video = manifest.select("video", 1080);
        DashManifest.Representation capped = manifest.select("video", 900);

        // then
        assertEquals("v1080", video.id);
        assertEquals("v720", capped.id);
    }

    @Test
    public void fallsBackToTheSmallestVideoWhenNoneFits() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        DashManifest.Representation video = manifest.select("video", 360);

        // then
        assertEquals("v720", video.id);
    }

    @Test
    public void typesARepresentationFromTheMimeTypeOfItsSet() throws IOException {
        // when
        DashManifest.Representation audio = parse(MANIFEST).select("audio", Integer.MAX_VALUE);

        // then
        assertEquals("a1", audio.id);
    }

    @Test
    public void givesNoRepresentationForAnAbsentType() throws IOException {
        // when
        DashManifest.Representation text = parse(MANIFEST).select("text", Integer.MAX_VALUE);

        // then
        assertEquals(null, text);
    }

    @Test
    public void expandsATimelineIntoNumberedSegments() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        List<Segment> segments = manifest.segmentsOf(manifest.select("video", 720));

        // then
        assertEquals(3, segments.size());
        assertEquals("https://media.example.com/title/v/v720/001.m4s", segments.get(0).url);
        assertEquals("https://media.example.com/title/v/v720/003.m4s", segments.get(2).url);
        assertEquals(0, segments.get(0).startTime);
        assertEquals(4000, segments.get(1).startTime);
        assertEquals(8000, segments.get(2).startTime);
        assertEquals(2000, segments.get(2).duration);
    }

    @Test
    public void cutsFixedDurationSegmentsToThePresentationLength() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        List<Segment> segments = manifest.segmentsOf(manifest.select("audio", Integer.MAX_VALUE));

        // then
        assertEquals(3, segments.size());
        assertEquals("https://media.example.com/title/a/1.m4s", segments.get(0).url);
        assertEquals(192000, segments.get(1).startTime);
        assertEquals(384000, segments.get(2).startTime);
        assertEquals(96000, segments.get(2).duration);
    }

    @Test
    public void resolvesTheInitializationUrl() throws IOException {
        // given
        DashManifest manifest = parse(MANIFEST);

        // when
        String video = manifest.select("video", 720).initializationUrl();
        String audio = manifest.select("audio", Integer.MAX_VALUE).initializationUrl();

        // then
        assertEquals("https://media.example.com/title/v/v720/init.mp4", video);
        assertEquals("https://media.example.com/title/a/init.mp4", audio);
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
        assertEquals("https://cdn.example.com/title/i.mp4", video.initializationUrl());
    }

    @Test
    public void rejectsARepresentationWithoutAnInitialization() throws IOException {
        // given
        String xml = "<MPD><Period><AdaptationSet contentType=\"video\"><Representation id=\"v\" height=\"720\">"
                + "<SegmentTemplate timescale=\"1\" media=\"$Number$.m4s\" duration=\"1\"/></Representation>"
                + "</AdaptationSet></Period></MPD>";
        DashManifest.Representation video = parse(xml).select("video", 720);

        // when
        try {
            video.initializationUrl();
            fail("expected a missing initialization");
        } catch (IOException ex) {

            // then
            assertTrue(ex.getMessage(), ex.getMessage().contains("no initialization"));
        }
    }

    @Test
    public void rejectsATemplateWithoutATimelineOrDuration() throws IOException {
        // given
        String xml = "<MPD><Period><AdaptationSet contentType=\"video\"><Representation id=\"v\" height=\"720\">"
                + "<SegmentTemplate timescale=\"1\" initialization=\"i.mp4\" media=\"$Number$.m4s\"/>"
                + "</Representation></AdaptationSet></Period></MPD>";
        DashManifest manifest = parse(xml);

        // when
        try {
            manifest.segmentsOf(manifest.select("video", 720));
            fail("expected a missing timeline");
        } catch (IOException ex) {

            // then
            assertTrue(ex.getMessage(), ex.getMessage().contains("lacks a segment timeline"));
        }
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
        try {
            manifest.segmentsOf(manifest.select("video", 720));
            fail("expected an invalid timeline");
        } catch (IOException ex) {

            // then
            assertTrue(ex.getMessage(), ex.getMessage().contains("invalid segment timeline"));
        }
    }

    @Test
    public void rejectsManifestsThatAreNotXml() {
        // when
        try {
            DashManifestParser.parse("<MPD".getBytes(StandardCharsets.UTF_8), MANIFEST_URL);
            fail("expected a parse failure");
        } catch (IOException ex) {

            // then
            assertTrue(ex.getMessage(), ex.getMessage().startsWith("cannot parse manifest"));
        }
    }

    @Test
    public void rejectsAMalformedPresentationDuration() {
        // when
        try {
            parse("<MPD mediaPresentationDuration=\"ten seconds\"/>");
            fail("expected a parse failure");
        } catch (IOException ex) {

            // then
            assertTrue(ex.getMessage(), ex.getMessage().startsWith("cannot parse manifest"));
        }
    }

    @Test
    public void rejectsManifestsNestedTooDeep() {
        // given
        StringBuilder xml = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            xml.append("<a>");
        }

        // when
        try {
            parse(xml.toString());
            fail("expected a depth failure");
        } catch (IOException ex) {

            // then
            assertTrue(ex.getMessage(), ex.getMessage().contains("nested deeper than 32"));
        }
    }

    @Test
    public void addsUpIsoDurations() throws IOException {
        // when
        DashManifest manifest = parse("<MPD mediaPresentationDuration=\"P1DT2H3M4.5S\"/>");

        // then
        assertEquals(86400 + 7200 + 180 + 4.5, manifest.presentationSeconds, 0.0);
    }

    @Test
    public void readsAMissingDurationAsZero() throws IOException {
        // when
        DashManifest manifest = parse("<MPD/>");

        // then
        assertEquals(0.0, manifest.presentationSeconds, 0.0);
    }

    private static DashManifest parse(String xml) throws IOException {
        return DashManifestParser.parse(xml.getBytes(StandardCharsets.UTF_8), MANIFEST_URL);
    }

}
