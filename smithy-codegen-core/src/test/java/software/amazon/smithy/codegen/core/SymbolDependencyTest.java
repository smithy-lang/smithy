/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.Pair;

public class SymbolDependencyTest {
    @Test
    public void setsDefaultTypeToEmptyString() {
        SymbolDependency dependency = SymbolDependency.builder()
                .packageName("foo")
                .version("10")
                .build();

        assertThat(dependency.getPackageName(), equalTo("foo"));
        assertThat(dependency.getVersion(), equalTo("10"));
        assertThat(dependency.getDependencyType(), equalTo(""));
    }

    @Test
    public void convertsToBuilder() {
        SymbolDependency dependency = SymbolDependency.builder()
                .dependencyType("dev")
                .packageName("foo")
                .version("10.0.1")
                .build();

        assertThat(dependency.toBuilder().build(), equalTo(dependency));
    }

    @Test
    public void hasProperties() {
        SymbolDependency dependency = SymbolDependency.builder()
                .dependencyType("dev")
                .packageName("foo")
                .version("10.0.1")
                .putProperty("foo", "baz!")
                .build();

        assertThat(dependency.expectProperty("foo", String.class), equalTo("baz!"));
    }

    @Test
    public void canBeSorted() {
        SymbolDependency a = SymbolDependency.builder().dependencyType("a").packageName("a").version("1").build();
        SymbolDependency a2 = SymbolDependency.builder().dependencyType("a").packageName("a2").version("1").build();
        SymbolDependency a3 = SymbolDependency.builder().dependencyType("a2").packageName("a").version("1").build();
        SymbolDependency b = SymbolDependency.builder().dependencyType("b").packageName("b").version("1").build();
        SymbolDependency b2 = SymbolDependency.builder().dependencyType("b").packageName("b").version("2").build();
        SymbolDependency c = SymbolDependency.builder().dependencyType("b").packageName("c").version("1").build();
        List<SymbolDependency> dependencies = Arrays.asList(c, b2, b, a3, a2, a);
        Collections.sort(dependencies);

        assertThat(dependencies, contains(a, a2, a3, b, b2, c));
    }

    @Test
    public void gathersDependencies() {
        SymbolDependency a = SymbolDependency.builder().dependencyType("a").packageName("a").version("1").build();
        SymbolDependency aDuplicate = a.toBuilder().build();
        SymbolDependency a2 = SymbolDependency.builder().dependencyType("a").packageName("a2").version("1").build();
        SymbolDependency a3 = SymbolDependency.builder().dependencyType("a2").packageName("a").version("1").build();
        SymbolDependency b = SymbolDependency.builder().dependencyType("b").packageName("b").version("1").build();
        SymbolDependency b2 = SymbolDependency.builder().dependencyType("b").packageName("b").version("2").build();
        SymbolDependency c = SymbolDependency.builder().dependencyType("b").packageName("c").version("1").build();

        Assertions.assertThrows(CodegenException.class, () -> {
            SymbolDependency.gatherDependencies(Stream.of(a, aDuplicate, a2, a3, b, b2, c));
        });

        List<Pair<SymbolDependency, SymbolDependency>> conflicts = new ArrayList<>();
        Map<String, Map<String, SymbolDependency>> result = SymbolDependency.gatherDependencies(
                Stream.of(a, aDuplicate, a2, a3, b, b2, c),
                (sa, sb) -> {
                    conflicts.add(Pair.of(sa, sb));
                    return sb;
                });

        assertThat(conflicts, contains(Pair.of(b, b2)));
        assertThat(result, hasKey("a"));
        assertThat(result, hasKey("a2"));
        assertThat(result, hasKey("b"));
        assertThat(result.get("a"), hasKey("a"));
        assertThat(result.get("a"), hasKey("a2"));
        assertThat(result.get("a").get("a"), equalTo(a));
        assertThat(result.get("a").get("a2"), equalTo(a2));
        assertThat(result.get("a2"), hasKey("a"));
        assertThat(result.get("a2").get("a"), equalTo(a3));
        assertThat(result.get("b"), hasKey("b"));
        assertThat(result.get("b").get("b"), equalTo(b2));
        assertThat(result.get("b"), hasKey("c"));
        assertThat(result.get("b").get("c"), equalTo(c));
    }
}
