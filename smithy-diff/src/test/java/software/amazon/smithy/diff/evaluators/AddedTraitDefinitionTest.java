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

public class AddedTraitDefinitionTest {
    @Test
    public void detectsAddedTraitDefinition() {
        SourceLocation source = new SourceLocation("main.smithy", 1, 2);
        Shape definition = StringShape.builder()
                .id("foo.baz#bam")
                .addTrait(TraitDefinition.builder().build())
                .source(source)
                .build();

        Model modelA = Model.assembler().assemble().unwrap();
        Model modelB = Model.assembler().addShape(definition).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedTraitDefinition").size(), equalTo(1));
        assertThat(events.get(0).getSourceLocation(), equalTo(source));
    }
}
