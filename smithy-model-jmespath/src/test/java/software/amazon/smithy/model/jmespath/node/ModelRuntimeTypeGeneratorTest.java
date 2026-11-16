/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmespath.node;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

    private static final int STRING_CAP = 16;
    private static final int COLLECTION_CAP = 1000;
    private static final long NODE_BUDGET = 100_000;
    private static final int DEPTH_CAP = 256;

    private static Object sample(Model m, String shapeName) {
        Shape shape = m.expectShape(ShapeId.fromOptionalNamespace("smithy.example", shapeName));
        return shape.accept(new ModelRuntimeTypeGenerator(m));
    }

    private static Model parse(String idl) {
        return Model.assembler()
                .disableValidation()
                .addUnparsedModel("test.smithy", "$version: \"2.0\"\nnamespace smithy.example\n" + idl)
                .assemble()
                .unwrap();
    }

    // Counts the values of a sample as a tree, walking shared references once per use.
    private static long expandedCount(Object value) {
        long count = 1;
        if (value instanceof List) {
            for (Object element : (List<?>) value) {
                count += expandedCount(element);
            }
        } else if (value instanceof Map) {
            for (Object element : ((Map<?, ?>) value).values()) {
                count += expandedCount(element);
            }
        }
        return count;
    }

    @Test
    public void capsHugeLengths() {
        assertThat(((List<?>) sample(model, "HugeList")).size(), equalTo(COLLECTION_CAP));
        assertThat(((List<?>) sample(model, "OverflowList")).size(), equalTo(COLLECTION_CAP));
        assertThat(((Map<?, ?>) sample(model, "HugeMap")).size(), equalTo(COLLECTION_CAP));
        assertThat(((String) sample(model, "HugeString")).length(), equalTo(STRING_CAP));
    }

    @Test
    public void doesNotTruncateListsBelowCap() {
        assertThat(((List<?>) sample(model, "Min20List")).size(), equalTo(20));
    }

    @Test
    public void boundsNestedHugeLists() {
        Object value = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> sample(model, "HugeNestedL1"));
        List<?> outer = (List<?>) value;
        assertThat(outer.isEmpty(), is(false));
        List<?> inner = (List<?>) ((List<?>) outer.get(0)).get(0);
        assertThat(inner.size(), equalTo(COLLECTION_CAP));
        assertThat(expandedCount(value), lessThanOrEqualTo(2 * NODE_BUDGET));
    }

    @Test
    public void boundsFanOutWithoutLengths() {
        StringBuilder idl = new StringBuilder();
        int depth = 30;
        for (int i = 0; i < depth; i++) {
            idl.append("structure S")
                    .append(i)
                    .append(" { a: S")
                    .append(i + 1)
                    .append(", b: S")
                    .append(i + 1)
                    .append(" }\n");
        }
        idl.append("structure S").append(depth).append(" { s: String }\n");
        Model m = parse(idl.toString());

        Object value = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> sample(m, "S0"));
        assertThat(expandedCount(value), lessThanOrEqualTo(2 * NODE_BUDGET));
    }

    @Test
    public void chargesRecursionLeaves() {
        StringBuilder idl = new StringBuilder("structure RS {\n");
        for (int i = 1; i <= 50; i++) {
            idl.append("    m").append(i).append(": RS\n");
        }
        idl.append("    l: RSList\n}\n");
        idl.append("@length(min: 1000)\nlist RSList { member: RS }\n");
        idl.append("@length(min: 1000)\nlist RSOuter { member: RS }\n");
        Model m = parse(idl.toString());

        Object value = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> sample(m, "RSOuter"));
        assertThat(expandedCount(value), lessThanOrEqualTo(2 * NODE_BUDGET));
    }

    @Test
    public void boundsDeepChains() {
        StringBuilder idl = new StringBuilder();
        int depth = 10000;
        for (int i = 0; i < depth; i++) {
            idl.append("structure S").append(i).append(" { a: S").append(i + 1).append(" }\n");
        }
        idl.append("structure S").append(depth).append(" {}\n");
        Model m = parse(idl.toString());

        Object value = sample(m, "S0");
        int steps = 0;
        while (value instanceof Map) {
            value = ((Map<?, ?>) value).get("a");
            steps++;
        }
        assertThat(value, sameInstance(LiteralExpression.ANY));
        assertThat(steps, equalTo(DEPTH_CAP + 1));
    }

    @Test
    public void handlesNegativeMaxOnUnvalidatedModels() {
        Model m = parse("@length(max: -1)\nlist NegList { member: String }\n"
                + "@length(max: -1)\nstring NegString\n");

        assertThat(sample(m, "NegList"), equalTo(Collections.emptyList()));
        assertThat(sample(m, "NegString"), equalTo(""));
    }
}
