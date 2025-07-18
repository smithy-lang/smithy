/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

class ToObjectTest {
    @Test
    void testStringValueToObject() {
        Value value = Value.stringValue("hello");

        assertEquals("hello", value.toObject());
    }

    @Test
    void testIntegerValueToObject() {
        Value value = Value.integerValue(42);

        assertEquals(42, value.toObject());
    }

    @Test
    void testBooleanValueToObject() {
        assertEquals(Boolean.TRUE, Value.booleanValue(true).toObject());
        assertEquals(Boolean.FALSE, Value.booleanValue(false).toObject());
    }

    @Test
    void testEmptyValueToObject() {
        Value value = Value.emptyValue();

        assertNull(value.toObject());
    }

    @Test
    void testArrayValueToObject() {
        Value arrayValue = Value.arrayValue(ListUtils.of(
                Value.stringValue("a"),
                Value.integerValue(1),
                Value.booleanValue(true)));

        Object result = arrayValue.toObject();
        assertInstanceOf(List.class, result);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals(1, list.get(1));
        assertEquals(true, list.get(2));
    }

    @Test
    void testRecordValueToObject() {
        Map<Identifier, Value> map = new LinkedHashMap<>();
        map.put(Identifier.of("name"), Value.stringValue("test"));
        map.put(Identifier.of("count"), Value.integerValue(5));
        map.put(Identifier.of("enabled"), Value.booleanValue(true));

        Value recordValue = Value.recordValue(map);
        Object result = recordValue.toObject();

        assertInstanceOf(Map.class, result);
        Map<?, ?> resultMap = (Map<?, ?>) result;
        assertEquals("test", resultMap.get("name"));
        assertEquals(5, resultMap.get("count"));
        assertEquals(true, resultMap.get("enabled"));
    }

    @Test
    void testEmptyCollections() {
        assertEquals(ListUtils.of(), Value.arrayValue(ListUtils.of()).toObject());
        assertEquals(MapUtils.of(), Value.recordValue(MapUtils.of()).toObject());
    }
}
