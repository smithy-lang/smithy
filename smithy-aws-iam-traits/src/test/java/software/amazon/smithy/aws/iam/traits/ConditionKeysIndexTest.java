/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class ConditionKeysIndexTest {
    @Test
    public void successfullyLoadsConditionKeys() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("successful-condition-keys.smithy"))
                .discoverModels(getClass().getClassLoader())
                .assemble()
                .unwrap();
        ShapeId service = ShapeId.from("smithy.example#MyService");

        ConditionKeysIndex index = ConditionKeysIndex.of(model);
        assertThat(index.getConditionKeyNames(service),
                containsInAnyOrder(
                        "aws:accountId",
                        "foo:baz",
                        "myservice:Resource1Id1",
                        "myservice:ResourceTwoId2",
                        "myservice:bar"));
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Operation1")),
                containsInAnyOrder("aws:accountId", "myservice:bar"));
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource1")),
                containsInAnyOrder("aws:accountId", "foo:baz", "myservice:Resource1Id1"));
        // Note that ID1 is not duplicated but rather reused on the child operation.
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource2")),
                containsInAnyOrder("aws:accountId",
                        "foo:baz",
                        "myservice:Resource1Id1",
                        "myservice:ResourceTwoId2"));
        // This resource has inheritance disabled.
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource3")), empty());
        // This resource has inheritance disabled and an explicit list provided.
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource4")),
                contains("foo:baz"));
        // Note that while this operation binds identifiers, it contains no unique ConditionKeys to bind.
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#GetResource2")), is(empty()));

        // Defined context keys are assembled from the names and mapped with the definitions.
        assertEquals(index.getDefinedConditionKeys(service).get("foo:baz").getDocumentation().get(), "Foo baz");
        assertEquals(index.getDefinedConditionKeys(service).get("foo:baz").getRelativeDocumentation().get(),
                "condition-keys.html");
        assertThat(index.getDefinedConditionKeys(service).get("myservice:Resource1Id1").getDocumentation(),
                not(Optional.empty()));
        assertEquals(index.getDefinedConditionKeys(service).get("myservice:ResourceTwoId2").getDocumentation().get(),
                "This is Foo");
        assertThat(index.getDefinedConditionKeys(service, ShapeId.from("smithy.example#GetResource2")).keySet(),
                is(empty()));
    }

    @Test
    public void disableConditionKeyInferenceForResources() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("disable-condition-key-inference-for-resources.smithy"))
                .discoverModels(getClass().getClassLoader())
                .assemble()
                .unwrap();

        ShapeId service = ShapeId.from("smithy.example#MyService");

        ConditionKeysIndex index = ConditionKeysIndex.of(model);

        assertThat(index.getConditionKeyNames(service),
                containsInAnyOrder("my:service", "aws:operation1", "resource:1", "myservice:Resource1Id1"));

        // Verify inference key myservice:Resource2Id2 does not exist
        assertThat(index.getConditionKeyNames(service), not(contains("myservice:Resource2Id2")));

        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource1")),
                containsInAnyOrder("resource:1", "myservice:Resource1Id1"));

        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource1")),
                not(contains("myservice:Resource2Id2")));

        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource2")),
                containsInAnyOrder("resource:1", "myservice:Resource1Id1"));

        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource2")),
                not(contains("myservice:Resource2Id2")));
    }

    @Test
    public void disableConditionKeyInferenceForService() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("disable-condition-key-inference-for-service.smithy"))
                .discoverModels(getClass().getClassLoader())
                .assemble()
                .unwrap();

        ShapeId service = ShapeId.from("smithy.example#MyService");

        ConditionKeysIndex index = ConditionKeysIndex.of(model);

        assertThat(index.getConditionKeyNames(service),
                containsInAnyOrder("my:service", "aws:operation1", "resource:1"));

        // Verify inference key myservice:Resource1Id1 AND myservice:Resource2Id2 do not exist
        assertThat(index.getConditionKeyNames(service),
                not(contains("myservice:Resource1Id1", "myservice:Resource2Id2")));

        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource1")),
                not(contains("myservice:Resource1Id1", "myservice:Resource2Id2")));

        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource1")),
                contains("resource:1"));

        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource2")),
                contains("resource:1"));

        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource2")),
                not(contains("myservice:Resource1Id1", "myservice:Resource2Id2")));
    }
}
