/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.Pair;

public class EndpointTest {
    private static final Pair<String, Map<Identifier, Literal>> SIGV4_AUTH_SCHEME = Pair.of(
            "sigv4",
            Collections.emptyMap());
    private static final Pair<String, Map<Identifier, Literal>> SIGV4A_AUTH_SCHEME = Pair.of(
            "sigv4a",
            MapUtils.of(
                    Identifier.of("signingRegionSet"),
                    Literal.tupleLiteral(ListUtils.of(Literal.of("*")))));

    @Test
    public void testGetEndpointAuthSchemes() {
        Endpoint endpoint = Endpoint.builder()
                .url(Literal.of("https://abc.service.com"))
                .addAuthScheme(Identifier.of(SIGV4A_AUTH_SCHEME.getLeft()), SIGV4A_AUTH_SCHEME.getRight())
                .addAuthScheme(Identifier.of(SIGV4_AUTH_SCHEME.getLeft()), SIGV4_AUTH_SCHEME.getRight())
                .build();
        List<Map<Identifier, Literal>> actual = endpoint.getEndpointAuthSchemes();
        List<Map<Identifier, Literal>> expected = ListUtils.of(
                convertAuthSchemeToMap(SIGV4A_AUTH_SCHEME),
                convertAuthSchemeToMap(SIGV4_AUTH_SCHEME));
        assertEquals(expected, actual);
    }

    @Test
    public void testEmptyGetEndpointAuthSchemes() {
        Endpoint endpoint = Endpoint.builder()
                .url(Literal.of("https://abc.service.com"))
                .build();
        List<Map<Identifier, Literal>> actual = endpoint.getEndpointAuthSchemes();
        List<Map<Identifier, Literal>> expected = Collections.emptyList();
        assertEquals(expected, actual);
    }

    private static Map<Identifier, Literal> convertAuthSchemeToMap(Pair<String, Map<Identifier, Literal>> authScheme) {
        Map<Identifier, Literal> base = new TreeMap<>(Comparator.comparing(Identifier::toString));
        base.put(Identifier.of("name"), Literal.of(authScheme.getLeft()));
        base.putAll(authScheme.getRight());
        return base;
    }
}
