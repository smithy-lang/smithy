/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.model.Model;

public class VersionTest {

    public static Stream<Arguments> resolvedVersions() {
        return Stream.of(
                // Fully qualified versions resolve to themselves.
                Arguments.of("1.0", Version.VERSION_1_0),
                Arguments.of("2.0", Version.VERSION_2_0),
                Arguments.of("2.1", Version.VERSION_2_1),
                // A version that omits the minor version resolves to the latest known minor version.
                Arguments.of("1", Version.VERSION_1_0),
                Arguments.of("2", Version.VERSION_2_1));
    }

    @ParameterizedTest
    @MethodSource("resolvedVersions")
    public void resolvesVersionStrings(String value, Version expected) {
        assertThat(Version.fromString(value), is(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "02", "2.", "2.2", "3", "999", "2.0.0", "two"})
    public void returnsNullForUnknownVersions(String value) {
        assertThat(Version.fromString(value), is(nullValue()));
    }

    @Test
    public void majorVersionOmissionTracksTheLatestMinor() {
        // Guards against adding a new minor version without updating what "2" resolves to. The minor
        // version is read back out of each label rather than relying on enum declaration order.
        Version latestV2 = null;
        for (Version version : Version.values()) {
            if (version.isV2() && (latestV2 == null || minorOf(version) > minorOf(latestV2))) {
                latestV2 = version;
            }
        }

        assertThat(Version.fromString("2"), is(latestV2));
    }

    @Test
    public void modelConstantsTrackTheLatestSupportedVersion() {
        // Model.MODEL_VERSION is public API documented as the highest supported version, and
        // MODEL_MAJOR_VERSION is what both serializers emit by default. Adding a minor version
        // without updating these constants would leave MODEL_VERSION claiming a stale version and
        // could leave MODEL_MAJOR_VERSION pointing at a major version that no longer exists.
        Version latest = null;
        for (Version version : Version.values()) {
            if (version != Version.UNKNOWN && (latest == null || isNewerThan(version, latest))) {
                latest = version;
            }
        }

        assertThat(Version.fromString(Model.MODEL_VERSION), is(latest));
        assertThat(Version.fromString(Model.MODEL_MAJOR_VERSION), is(latest));
    }

    private static boolean isNewerThan(Version left, Version right) {
        return majorOf(left) != majorOf(right)
                ? majorOf(left) > majorOf(right)
                : minorOf(left) > minorOf(right);
    }

    private static int majorOf(Version version) {
        String label = version.toString();
        return Integer.parseInt(label.substring(0, label.indexOf('.')));
    }

    private static int minorOf(Version version) {
        String label = version.toString();
        return Integer.parseInt(label.substring(label.indexOf('.') + 1));
    }
}
