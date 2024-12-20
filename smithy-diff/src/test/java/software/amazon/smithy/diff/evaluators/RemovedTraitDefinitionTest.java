/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.ValidationEvent;

public class RemovedTraitDefinitionTest {
    @Test
    public void detectsRemovedTraitDefinition() {
        SourceLocation source = new SourceLocation("bar.smithy");
        Shape definition = StringShape.builder()
                .id("foo.baz#bam")
                .addTrait(TraitDefinition.builder().sourceLocation(source).build())
                .build();

        Model modelA = Model.assembler().addShape(definition).assemble().unwrap();
        Model modelB = Model.assembler().assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedTraitDefinition").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "RemovedTraitDefinition").get(0).getSourceLocation(),
                equalTo(source));
    }
}
