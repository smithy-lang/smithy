/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.waiters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ModelRuntimeTypeGeneratorTest {

    private static Model model;

    @BeforeAll
    static void before() {
        model = Model.assembler()
                .addImport(ModelRuntimeTypeGenerator.class.getResource("model-runtime-types.smithy"))
                .assemble()
                .unwrap();
    }

    @ParameterizedTest
    @MethodSource("shapeSource")
    public void convertsShapeToExpectedValue(String shapeName, Object expected) {
        ShapeId id = ShapeId.fromOptionalNamespace("smithy.example", shapeName);
        Shape shape = model.expectShape(id);
        ModelRuntimeTypeGenerator generator = new ModelRuntimeTypeGenerator(model);
        Object actual = shape.accept(generator);
        assertThat(expected, equalTo(actual));
    }

    public static Collection<Object[]> shapeSource() {
        Map<String, Object> stringListMap = new LinkedHashMap<>();
        stringListMap.put("aa0", Arrays.asList("aa", "aa"));
        stringListMap.put("aa1", Arrays.asList("aa", "aa"));

        Map<String, Object> sizedStringListMap = new LinkedHashMap<>();
        sizedStringListMap.put("aa0", Arrays.asList("aa", "aa"));
        sizedStringListMap.put("aa1", Arrays.asList("aa", "aa"));
        sizedStringListMap.put("aa2", Arrays.asList("aa", "aa"));
        sizedStringListMap.put("aa3", Arrays.asList("aa", "aa"));
        sizedStringListMap.put("aa4", Arrays.asList("aa", "aa"));

        Map<String, Object> myUnionOrStruct = new LinkedHashMap<>();
        myUnionOrStruct.put("foo", "aa");

        Map<String, Object> recursiveStruct = new LinkedHashMap<>();
        recursiveStruct.put("foo", Arrays.asList("aa", "aa"));
        Map<String, Object> recursiveStructAny = new LinkedHashMap<>();
        recursiveStructAny.put("foo", LiteralExpression.ANY);
        recursiveStructAny.put("bar", LiteralExpression.ANY);
        recursiveStruct.put("bar", Collections.singletonList(recursiveStructAny));

        return Arrays.asList(new Object[][] {
                {"StringList", Arrays.asList("aa", "aa")},
                {"SizedStringList", Arrays.asList("aa", "aa", "aa", "aa", "aa")},
                {"StringListMap", stringListMap},
                {"SizedStringListMap", sizedStringListMap},
                {"SizedString1", "aaaa"},
                {"SizedString2", "a"},
                {"SizedString3", "aaaaaaaa"},
                {"SizedInteger1", 100.0},
                {"SizedInteger2", 2.0},
                {"SizedInteger3", 8.0},
                {"MyUnion", myUnionOrStruct},
                {"MyStruct", myUnionOrStruct},
                {"smithy.api#Blob", "blob"},
                {"smithy.api#Document", LiteralExpression.ANY},
                {"smithy.api#Boolean", true},
                {"smithy.api#Byte", 8.0},
                {"smithy.api#Short", 8.0},
                {"smithy.api#Integer", 8.0},
                {"smithy.api#Long", 8.0},
                {"smithy.api#Float", 8.0},
                {"smithy.api#Double", 8.0},
                {"smithy.api#BigInteger", 8.0},
                {"smithy.api#BigDecimal", 8.0},
                {"smithy.api#Timestamp", LiteralExpression.NUMBER},
                {"RecursiveStruct", recursiveStruct}
        });
    }
}
