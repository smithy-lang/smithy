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
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.ValidationEvent;

public class RemovedEntityBindingTest {
    @Test
    public void detectsRemovedOperationFromService() {
        SourceLocation source = new SourceLocation("foo.smithy");
        OperationShape o = OperationShape.builder().id("foo.baz#Operation").build();
        ServiceShape service1 = ServiceShape.builder()
                .version("1")
                .id("foo.baz#Service")
                .addOperation(o.getId())
                .source(source)
                .build();
        ServiceShape service2 = service1.toBuilder().clearOperations().build();
        Model modelA = Model.assembler().addShapes(service1, o).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(service2, o).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedOperationBinding.FromService.Operation").size(), equalTo(1));
        assertThat(events.get(0).getSourceLocation(), equalTo(source));
    }

    @Test
    public void detectsRemovedOperationFromResource() {
        SourceLocation source = new SourceLocation("foo.smithy");
        OperationShape o = OperationShape.builder().id("foo.baz#Operation").build();
        ResourceShape r1 = ResourceShape.builder().id("foo.baz#Resource").addOperation(o.getId()).source(source).build();
        ResourceShape r2 = r1.toBuilder().clearOperations().build();
        Model modelA = Model.assembler().addShapes(r1, o).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(r2, o).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedOperationBinding.FromResource.Operation").size(), equalTo(1));
        assertThat(events.get(0).getSourceLocation(), equalTo(source));
    }

    @Test
    public void detectsRemovedResourceFromService() {
        SourceLocation source = new SourceLocation("foo.smithy");
        ResourceShape r = ResourceShape.builder().id("foo.baz#Resource").build();
        ServiceShape service1 = ServiceShape.builder()
                .id("foo.baz#Service")
                .version("1")
                .addResource(r.getId())
                .source(source)
                .build();
        ServiceShape service2 = service1.toBuilder().clearResources().build();
        Model modelA = Model.assembler().addShapes(service1, r).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(service2, r).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedResourceBinding.FromService.Resource").size(), equalTo(1));
        assertThat(events.get(0).getSourceLocation(), equalTo(source));
    }

    @Test
    public void detectsRemovedResourceFromResource() {
        SourceLocation source = new SourceLocation("foo.smithy");
        ResourceShape child = ResourceShape.builder().id("foo.baz#C").build();
        ResourceShape p1 = ResourceShape.builder().id("foo.baz#P").addResource(child.getId()).source(source).build();
        ResourceShape p2 = p1.toBuilder().clearResources().build();
        Model modelA = Model.assembler().addShapes(p1, child).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(p2, child).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedResourceBinding.FromResource.C").size(), equalTo(1));
        assertThat(events.get(0).getSourceLocation(), equalTo(source));
    }
}
