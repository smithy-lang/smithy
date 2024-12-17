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
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.ValidationEvent;

public class AddedEntityBindingTest {
    @Test
    public void detectsAddedOperationToService() {
        OperationShape o = OperationShape.builder().id("foo.baz#Operation").build();
        ServiceShape service1 = ServiceShape.builder()
                .version("1")
                .id("foo.baz#Service")
                .addOperation(o.getId())
                .build();
        ServiceShape service2 = service1.toBuilder().clearOperations().build();
        Model modelA = Model.assembler().addShapes(service2, o).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(service1, o).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedOperationBinding.ToService.Operation").size(), equalTo(1));
    }

    @Test
    public void detectsAddedOperationToResource() {
        OperationShape o = OperationShape.builder().id("foo.baz#Operation").build();
        ResourceShape r1 = ResourceShape.builder().id("foo.baz#Resource").addOperation(o.getId()).build();
        ResourceShape r2 = r1.toBuilder().clearOperations().build();
        Model modelA = Model.assembler().addShapes(r2, o).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(r1, o).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedOperationBinding.ToResource.Operation").size(), equalTo(1));
    }

    @Test
    public void detectsAddedResourceToService() {
        ResourceShape r = ResourceShape.builder().id("foo.baz#Resource").build();
        ServiceShape service1 = ServiceShape.builder()
                .id("foo.baz#Service")
                .version("1")
                .addResource(r.getId())
                .build();
        ServiceShape service2 = service1.toBuilder().clearResources().build();
        Model modelA = Model.assembler().addShapes(service2, r).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(service1, r).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedResourceBinding.ToService.Resource").size(), equalTo(1));
    }

    @Test
    public void detectsAddedResourceToResource() {
        ResourceShape child = ResourceShape.builder().id("foo.baz#C").build();
        ResourceShape p1 = ResourceShape.builder().id("foo.baz#P").addResource(child.getId()).build();
        ResourceShape p2 = p1.toBuilder().clearResources().build();
        Model modelA = Model.assembler().addShapes(p2, child).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(p1, child).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedResourceBinding.ToResource.C").size(), equalTo(1));
    }
}
