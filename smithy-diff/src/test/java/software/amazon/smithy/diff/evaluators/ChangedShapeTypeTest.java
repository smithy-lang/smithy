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

package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedShapeTypeTest {
    @Test
    public void detectsTypeChanges() {
        Shape shapeA1 = StringShape.builder().id("foo.baz#Baz").build();
        Shape shapeA2 = TimestampShape.builder().id("foo.baz#Baz").build();
        Shape shapeB1 = StringShape.builder().id("foo.baz#Bam").build();
        Model modelA = Model.assembler().addShapes(shapeA1, shapeB1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, shapeB1).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedShapeType").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, shapeA1.getId()).size(), equalTo(1));
    }

    @Test
    public void ignoresExpectedSetToListMigration() {
        String rawModel = "$version: \"1.0\"\nnamespace smithy.example\nset Foo { member: String }\n";
        Model oldModel = Model.assembler().addUnparsedModel("example.smithy", rawModel)
                .assemble().unwrap();
        Node serialized = ModelSerializer.builder().build().serialize(oldModel);
        Model newModel = Model.assembler()
                .addDocumentNode(serialized)
                .assemble()
                .unwrap();

        List<ValidationEvent> events = ModelDiff.compare(oldModel, newModel);

        assertThat(TestHelper.findEvents(events, "ChangedShapeType"), empty());
    }
}
