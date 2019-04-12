/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.aws.traits.iam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.ValidatedResult;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ConditionKeysIndexTest {
    @Test
    public void successfullyLoadsConditionKeys() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("successful-condition-keys.smithy"))
                .discoverModels(getClass().getClassLoader())
                .assemble()
                .unwrap();
        ShapeId service = ShapeId.from("smithy.example#MyService");

        ConditionKeysIndex index = model.getKnowledge(ConditionKeysIndex.class);
        assertThat(index.getConditionKeyNames(service), containsInAnyOrder(
                "aws:accountId", "foo:baz", "myservice:Resource1Id1", "myservice:Resource2Id2"));
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Operation1")),
                   containsInAnyOrder("aws:accountId", "foo:baz"));
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource1")),
                   containsInAnyOrder("aws:accountId", "foo:baz", "myservice:Resource1Id1"));
        // Note that ID1 is not duplicated but rather reused on the child operation.
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#Resource2")),
                   containsInAnyOrder("aws:accountId", "foo:baz",
                                      "myservice:Resource1Id1", "myservice:Resource2Id2"));
        // Note that this operation does bind all identifiers, so it includes both ID1 and 2.
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#GetResource2")),
                   containsInAnyOrder("aws:accountId", "foo:baz",
                                      "myservice:Resource1Id1", "myservice:Resource2Id2"));
        // Note that this operation does not bind all identifiers, so it does not include ID2.
        assertThat(index.getConditionKeyNames(service, ShapeId.from("smithy.example#ListResource2")),
                   containsInAnyOrder("aws:accountId", "foo:baz", "myservice:Resource1Id1"));

        // Defined context keys are assembled from the names and mapped with the definitions.
        assertThat(index.getDefinedConditionKeys(service).get("myservice:Resource1Id1").getDocumentation(),
                   not(Optional.empty()));
        assertThat(index.getDefinedConditionKeys(service, ShapeId.from("smithy.example#GetResource2")).keySet(),
                   containsInAnyOrder("foo:baz", "myservice:Resource1Id1", "myservice:Resource2Id2"));
    }

    @Test
    public void detectsUnknownConditionKeys() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("invalid-condition-keys.smithy"))
                .discoverModels(getClass().getClassLoader())
                .assemble();

        assertTrue(result.isBroken());
        assertThat(result.getValidationEvents(Severity.ERROR).stream()
                           .map(ValidationEvent::getEventId)
                           .collect(Collectors.toSet()),
                contains("ConditionKeys"));
    }
}
