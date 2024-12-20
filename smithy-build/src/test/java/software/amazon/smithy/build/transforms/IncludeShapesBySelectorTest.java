/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SetUtils;

public class IncludeShapesBySelectorTest {

    @Test
    public void includesByServiceFoo() {
        Model model = testModel();
        String selector = "[id=smithy.example#FooService] :is(*, ~> *)";
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("selector", selector))
                .build();
        Model result = new IncludeShapesBySelector().transform(context);
        assertThat(exampleIds(result),
                is(SetUtils.of(
                        "smithy.example#FooEnum",
                        "smithy.example#FooEnum$BAR",
                        "smithy.example#FooEnum$BAZ",
                        "smithy.example#FooEnum$FOO",
                        "smithy.example#FooInteger",
                        "smithy.example#FooService",
                        "smithy.example#FooStructInput",
                        "smithy.example#FooStructInput$intVal",
                        "smithy.example#FooStructInput$stringVal",
                        "smithy.example#FooStructOutput",
                        "smithy.example#FooStructOutput$intVal",
                        "smithy.example#FooStructOutput$stringVal",
                        "smithy.example#FooStructOutput$unionVal",
                        "smithy.example#FooUnion",
                        "smithy.example#FooUnion$fooEnum",
                        "smithy.example#FooUnion$fooInteger",
                        "smithy.example#FooUnion$fooString",
                        "smithy.example#GetFoo",
                        "smithy.example#LeaveEvent",
                        "smithy.example#Message",
                        "smithy.example#Message$message",
                        "smithy.example#PublishEvents",
                        "smithy.example#PublishEvents$leave",
                        "smithy.example#PublishEvents$message",
                        "smithy.example#PublishMessages",
                        "smithy.example#PublishMessagesInput",
                        "smithy.example#PublishMessagesInput$messages",
                        "smithy.example#PublishMessagesInput$room")));
    }

    @Test
    public void includesByServiceBar() {
        Model model = testModel();
        String selector = "[id=smithy.example#BarService] :is(*, ~> *)";
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("selector", selector))
                .build();
        Model result = new IncludeShapesBySelector().transform(context);
        assertThat(exampleIds(result),
                is(SetUtils.of(
                        "smithy.example#BarEnum",
                        "smithy.example#BarEnum$BAR",
                        "smithy.example#BarEnum$BAZ",
                        "smithy.example#BarEnum$FOO",
                        "smithy.example#BarInteger",
                        "smithy.example#BarService",
                        "smithy.example#BarStructInput",
                        "smithy.example#BarStructInput$intVal",
                        "smithy.example#BarStructInput$stringVal",
                        "smithy.example#BarStructOutput",
                        "smithy.example#BarStructOutput$intVal",
                        "smithy.example#BarStructOutput$stringVal",
                        "smithy.example#BarStructOutput$unionVal",
                        "smithy.example#BarUnion",
                        "smithy.example#BarUnion$fooEnum",
                        "smithy.example#BarUnion$fooInteger",
                        "smithy.example#BarUnion$fooString",
                        "smithy.example#GetBar")));
    }

    @Test
    public void includesNotByService() {
        Model model = testModel();
        String selector = ":not([id=smithy.example#BarService] ~> *)";
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("selector", selector))
                .build();
        Model result = new IncludeShapesBySelector().transform(context);
        assertThat(exampleIds(result),
                is(SetUtils.of(
                        "smithy.example#BarEnum",
                        "smithy.example#BarEnum$BAR",
                        "smithy.example#BarEnum$BAZ",
                        "smithy.example#BarEnum$FOO",
                        "smithy.example#BarInteger",
                        "smithy.example#BarStruct",
                        "smithy.example#BarStruct$intVal",
                        "smithy.example#BarStruct$stringVal",
                        "smithy.example#BarStruct$unionVal",
                        "smithy.example#BarStructInput",
                        "smithy.example#BarStructInput$intVal",
                        "smithy.example#BarStructInput$stringVal",
                        "smithy.example#BarStructOutput",
                        "smithy.example#BarStructOutput$intVal",
                        "smithy.example#BarStructOutput$stringVal",
                        "smithy.example#BarStructOutput$unionVal",
                        "smithy.example#BarUnion",
                        "smithy.example#BarUnion$fooEnum",
                        "smithy.example#BarUnion$fooInteger",
                        "smithy.example#BarUnion$fooString",
                        "smithy.example#FooEnum",
                        "smithy.example#FooEnum$BAR",
                        "smithy.example#FooEnum$BAZ",
                        "smithy.example#FooEnum$FOO",
                        "smithy.example#FooInteger",
                        "smithy.example#FooService",
                        "smithy.example#FooStruct",
                        "smithy.example#FooStruct$intVal",
                        "smithy.example#FooStruct$stringVal",
                        "smithy.example#FooStruct$unionVal",
                        "smithy.example#FooStructInput",
                        "smithy.example#FooStructInput$intVal",
                        "smithy.example#FooStructInput$stringVal",
                        "smithy.example#FooStructOutput",
                        "smithy.example#FooStructOutput$intVal",
                        "smithy.example#FooStructOutput$stringVal",
                        "smithy.example#FooStructOutput$unionVal",
                        "smithy.example#FooUnion",
                        "smithy.example#FooUnion$fooEnum",
                        "smithy.example#FooUnion$fooInteger",
                        "smithy.example#FooUnion$fooString",
                        "smithy.example#GetBar",
                        "smithy.example#GetFoo",
                        "smithy.example#LeaveEvent",
                        "smithy.example#Message",
                        "smithy.example#Message$message",
                        "smithy.example#PublishEvents",
                        "smithy.example#PublishEvents$leave",
                        "smithy.example#PublishEvents$message",
                        "smithy.example#PublishMessages",
                        "smithy.example#PublishMessagesInput",
                        "smithy.example#PublishMessagesInput$messages",
                        "smithy.example#PublishMessagesInput$room")));
    }

    @Test
    public void includesOnlyEnumMembersWithTags() {
        Model model = testModel();
        String selector = ":is(enum, enum > member:test([trait|tags|(values) = \"alpha\",\"beta\"]))";
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("selector", selector))
                .build();
        Model result = new IncludeShapesBySelector().transform(context);
        assertThat(exampleIds(result),
                is(SetUtils.of(
                        "smithy.example#BarEnum",
                        "smithy.example#BarEnum$BAR",
                        "smithy.example#BarEnum$FOO",
                        "smithy.example#FooEnum",
                        "smithy.example#FooEnum$BAR",
                        "smithy.example#FooEnum$FOO")));
    }

    @Test
    public void includesOnlyEnums() {
        Model model = testModel();
        String selector = ":is(enum, enum > member)";
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("selector", selector))
                .build();
        Model result = new IncludeShapesBySelector().transform(context);
        assertThat(exampleIds(result),
                is(SetUtils.of(
                        "smithy.example#BarEnum",
                        "smithy.example#BarEnum$BAR",
                        "smithy.example#BarEnum$BAZ",
                        "smithy.example#BarEnum$FOO",
                        "smithy.example#FooEnum",
                        "smithy.example#FooEnum$BAR",
                        "smithy.example#FooEnum$BAZ",
                        "smithy.example#FooEnum$FOO")));
    }

    @Test
    public void includesStructsAndForwardRecursiveNeighbors() {
        Model model = testModel();
        String selector = ":is(structure, structure ~> *)";
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("selector", selector))
                .build();
        Model result = new IncludeShapesBySelector().transform(context);
        assertThat(exampleIds(result),
                is(SetUtils.of(
                        "smithy.example#BarEnum",
                        "smithy.example#BarEnum$BAR",
                        "smithy.example#BarEnum$BAZ",
                        "smithy.example#BarEnum$FOO",
                        "smithy.example#BarInteger",
                        "smithy.example#BarStruct",
                        "smithy.example#BarStruct$intVal",
                        "smithy.example#BarStruct$stringVal",
                        "smithy.example#BarStruct$unionVal",
                        "smithy.example#BarStructInput",
                        "smithy.example#BarStructInput$intVal",
                        "smithy.example#BarStructInput$stringVal",
                        "smithy.example#BarStructOutput",
                        "smithy.example#BarStructOutput$intVal",
                        "smithy.example#BarStructOutput$stringVal",
                        "smithy.example#BarStructOutput$unionVal",
                        "smithy.example#BarUnion",
                        "smithy.example#BarUnion$fooEnum",
                        "smithy.example#BarUnion$fooInteger",
                        "smithy.example#BarUnion$fooString",
                        "smithy.example#FooEnum",
                        "smithy.example#FooEnum$BAR",
                        "smithy.example#FooEnum$BAZ",
                        "smithy.example#FooEnum$FOO",
                        "smithy.example#FooInteger",
                        "smithy.example#FooStruct",
                        "smithy.example#FooStruct$intVal",
                        "smithy.example#FooStruct$stringVal",
                        "smithy.example#FooStruct$unionVal",
                        "smithy.example#FooStructInput",
                        "smithy.example#FooStructInput$intVal",
                        "smithy.example#FooStructInput$stringVal",
                        "smithy.example#FooStructOutput",
                        "smithy.example#FooStructOutput$intVal",
                        "smithy.example#FooStructOutput$stringVal",
                        "smithy.example#FooStructOutput$unionVal",
                        "smithy.example#FooUnion",
                        "smithy.example#FooUnion$fooEnum",
                        "smithy.example#FooUnion$fooInteger",
                        "smithy.example#FooUnion$fooString",
                        "smithy.example#LeaveEvent",
                        "smithy.example#Message",
                        "smithy.example#Message$message",
                        "smithy.example#PublishEvents",
                        "smithy.example#PublishEvents$leave",
                        "smithy.example#PublishEvents$message",
                        "smithy.example#PublishMessagesInput",
                        "smithy.example#PublishMessagesInput$messages",
                        "smithy.example#PublishMessagesInput$room")));
    }

    Model testModel() {
        try {
            return Model.assembler()
                    .addImport(Paths.get(getClass().getResource("transform-by-selector.smithy").toURI()))
                    .assemble()
                    .unwrap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Set<String> exampleIds(Model model) {
        return model.getShapeIds()
                .stream()
                .filter(id -> id.getNamespace().equals("smithy.example"))
                .map(ShapeId::toString)
                .collect(Collectors.toSet());
    }

}
