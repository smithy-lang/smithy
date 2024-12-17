/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class MapUtilsTest {
    @Test
    public void copyOfEmptyIsEmpty() {
        assertThat(MapUtils.copyOf(Collections.emptyMap()), anEmptyMap());
    }

    @Test
    public void copyOfIsSame() {
        assertThat(MapUtils.copyOf(Collections.singletonMap("1", "A")), hasEntry("1", "A"));
    }

    @Test
    public void orderedCopyOfEmptyIsEmpty() {
        assertThat(MapUtils.orderedCopyOf(Collections.emptyMap()), anEmptyMap());
    }

    @Test
    public void orderedCopyOfIsSame() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("a", "aa");
        input.put("b", "bb");
        Map<String, String> copy = MapUtils.copyOf(input);

        assertThat(ListUtils.copyOf(input.entrySet()), equalTo(ListUtils.copyOf(copy.entrySet())));
    }

    @Test
    public void ofEmptyIsEmpty() {
        assertThat(MapUtils.of(), anEmptyMap());
    }

    @Test
    public void ofOneIsOne() {
        Map<String, String> map = MapUtils.of("1", "A");
        assertThat(map, hasEntry("1", "A"));
    }

    @Test
    public void ofTwoIsTwo() {
        Map<String, String> map = MapUtils.of("1", "A", "2", "B");
        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
    }

    @Test
    public void ofThreeIsThree() {
        Map<String, String> map = MapUtils.of("1", "A", "2", "B", "3", "C");
        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
        assertThat(map, hasEntry("3", "C"));
    }

    @Test
    public void ofFourIsFour() {
        Map<String, String> map = MapUtils.of("1",
                "A",
                "2",
                "B",
                "3",
                "C",
                "4",
                "D");
        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
        assertThat(map, hasEntry("3", "C"));
        assertThat(map, hasEntry("4", "D"));
    }

    @Test
    public void ofFiveIsFive() {
        Map<String, String> map = MapUtils.of("1",
                "A",
                "2",
                "B",
                "3",
                "C",
                "4",
                "D",
                "5",
                "E");
        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
        assertThat(map, hasEntry("3", "C"));
        assertThat(map, hasEntry("4", "D"));
        assertThat(map, hasEntry("5", "E"));
    }

    @Test
    public void ofSixIsSix() {
        Map<String, String> map = MapUtils.of("1",
                "A",
                "2",
                "B",
                "3",
                "C",
                "4",
                "D",
                "5",
                "E",
                "6",
                "F");
        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
        assertThat(map, hasEntry("3", "C"));
        assertThat(map, hasEntry("4", "D"));
        assertThat(map, hasEntry("5", "E"));
        assertThat(map, hasEntry("6", "F"));
    }

    @Test
    public void ofSevenIsSeven() {
        Map<String, String> map = MapUtils.of("1",
                "A",
                "2",
                "B",
                "3",
                "C",
                "4",
                "D",
                "5",
                "E",
                "6",
                "F",
                "7",
                "G");
        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
        assertThat(map, hasEntry("3", "C"));
        assertThat(map, hasEntry("4", "D"));
        assertThat(map, hasEntry("5", "E"));
        assertThat(map, hasEntry("6", "F"));
        assertThat(map, hasEntry("7", "G"));
    }

    @Test
    public void ofEightIsEight() {
        Map<String, String> map = MapUtils.of("1",
                "A",
                "2",
                "B",
                "3",
                "C",
                "4",
                "D",
                "5",
                "E",
                "6",
                "F",
                "7",
                "G",
                "8",
                "H");
        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
        assertThat(map, hasEntry("3", "C"));
        assertThat(map, hasEntry("4", "D"));
        assertThat(map, hasEntry("5", "E"));
        assertThat(map, hasEntry("6", "F"));
        assertThat(map, hasEntry("7", "G"));
        assertThat(map, hasEntry("8", "H"));
    }

    @Test
    public void ofNineIsNine() {
        Map<String, String> map = MapUtils.of("1",
                "A",
                "2",
                "B",
                "3",
                "C",
                "4",
                "D",
                "5",
                "E",
                "6",
                "F",
                "7",
                "G",
                "8",
                "H",
                "9",
                "I");
        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
        assertThat(map, hasEntry("3", "C"));
        assertThat(map, hasEntry("4", "D"));
        assertThat(map, hasEntry("5", "E"));
        assertThat(map, hasEntry("6", "F"));
        assertThat(map, hasEntry("7", "G"));
        assertThat(map, hasEntry("8", "H"));
        assertThat(map, hasEntry("9", "I"));
    }

    @Test
    public void ofTenIsTen() {
        Map<String, String> map = MapUtils.of("1",
                "A",
                "2",
                "B",
                "3",
                "C",
                "4",
                "D",
                "5",
                "E",
                "6",
                "F",
                "7",
                "G",
                "8",
                "H",
                "9",
                "I",
                "10",
                "J");
        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
        assertThat(map, hasEntry("3", "C"));
        assertThat(map, hasEntry("4", "D"));
        assertThat(map, hasEntry("5", "E"));
        assertThat(map, hasEntry("6", "F"));
        assertThat(map, hasEntry("7", "G"));
        assertThat(map, hasEntry("8", "H"));
        assertThat(map, hasEntry("9", "I"));
        assertThat(map, hasEntry("10", "J"));
    }

    @Test
    public void suppliesEntry() {
        Map.Entry<String, String> entry = MapUtils.entry("1", "A");
        assertEquals("1", entry.getKey());
        assertEquals("A", entry.getValue());
    }

    @Test
    public void buildsFromTwoEntries() {
        Map<String, String> map = MapUtils.ofEntries(
                MapUtils.entry("1", "A"),
                MapUtils.entry("2", "B"));

        assertThat(map, hasEntry("1", "A"));
        assertThat(map, hasEntry("2", "B"));
        assertThat(map.entrySet(), hasSize(2));
    }

    @Test
    public void buildsFromEntriesSingleEntry() {
        Map<String, String> map = MapUtils.ofEntries(MapUtils.entry("1", "A"));

        assertThat(map, hasEntry("1", "A"));
        assertThat(map.entrySet(), hasSize(1));
    }

    @Test
    public void buildsFromEntriesEmpty() {
        Map<String, String> map = MapUtils.ofEntries();

        assertThat(map.entrySet(), empty());
    }

    @Test
    public void collectsToMap() {
        Map<String, String> map = Stream.of("Jason", "Michael", "Kevin")
                .collect(MapUtils.toUnmodifiableMap(i -> i.substring(0, 1), identity()));
        assertThat(map, hasEntry("J", "Jason"));
        assertThat(map, hasEntry("K", "Kevin"));
        assertThat(map, hasEntry("M", "Michael"));
    }
}
