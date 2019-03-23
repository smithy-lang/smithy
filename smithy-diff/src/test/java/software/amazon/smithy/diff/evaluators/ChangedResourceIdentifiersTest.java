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
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedResourceIdentifiersTest {
    @Test
    public void detectsChangedIdentifiers() {
        StringShape id = StringShape.builder().id("foo.baz#Id").build();
        ResourceShape r1 = ResourceShape.builder().id("foo.baz#R").addIdentifier("a", id).build();
        ResourceShape r2 = ResourceShape.builder().id("foo.baz#R").addIdentifier("b", id).build();
        Model modelA = Model.assembler().addShapes(r1, id).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(r2, id).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedResourceIdentifiers").size(), equalTo(1));
    }
}
