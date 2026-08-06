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
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.Severity;
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
    public void detectsRemovedOperationsBeforeAndAfterContainedOperation() {
        // The retained middle ID exercises both early-exit and exhausted searches of the sorted closure.
        OperationShape first = OperationShape.builder().id("foo.baz#AOperation").build();
        OperationShape retained = OperationShape.builder().id("foo.baz#MOperation").build();
        OperationShape last = OperationShape.builder().id("foo.baz#ZOperation").build();
        ServiceShape service1 = ServiceShape.builder()
                .version("1")
                .id("foo.baz#Service")
                .addOperation(first.getId())
                .addOperation(retained.getId())
                .addOperation(last.getId())
                .build();
        ServiceShape service2 = service1.toBuilder()
                .clearOperations()
                .addOperation(retained.getId())
                .build();
        Model modelA = Model.assembler().addShapes(service1, first, retained, last).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(service2, first, retained, last).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);
        List<ValidationEvent> removedEvents =
                TestHelper.findEvents(events, "RemovedOperationBinding.FromService");

        assertThat(removedEvents.size(), equalTo(2));
        assertThat(TestHelper.findEvents(removedEvents, Severity.ERROR).size(), equalTo(2));
    }

    @Test
    public void warnsWhenOperationMovesFromServiceToNestedResource() {
        OperationShape op = OperationShape.builder().id("foo.baz#Operation").build();
        ResourceShape child = ResourceShape.builder()
                .id("foo.baz#Child")
                .addOperation(op.getId())
                .build();
        ResourceShape parent = ResourceShape.builder()
                .id("foo.baz#Parent")
                .addResource(child.getId())
                .build();
        ServiceShape service1 = ServiceShape.builder()
                .version("1")
                .id("foo.baz#Service")
                .addOperation(op.getId())
                .build();
        ServiceShape service2 = service1.toBuilder()
                .clearOperations()
                .addResource(parent.getId())
                .build();
        Model modelA = Model.assembler().addShapes(service1, op).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(service2, parent, child, op).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);
        List<ValidationEvent> movedEvents = TestHelper.findEvents(
                events,
                "RemovedOperationBinding.FromService.Operation");

        assertThat(movedEvents.size(), equalTo(1));
        assertThat(movedEvents.get(0).getSeverity(), equalTo(Severity.WARNING));
        assertThat(
                movedEvents.get(0).getMessage(),
                equalTo(
                        "Operation binding of `foo.baz#Operation` was removed from service shape, `foo.baz#Service`, "
                                + "but the operation remains in the service closure through a resource"));
    }

    @Test
    public void detectsWhenOperationMovesFromResourceToService() {
        OperationShape op = OperationShape.builder().id("foo.baz#Operation").build();
        ResourceShape resourceWithOperation = ResourceShape.builder()
                .id("foo.baz#Resource")
                .addOperation(op.getId())
                .build();
        ResourceShape resourceWithoutOperation = resourceWithOperation.toBuilder().clearOperations().build();
        ServiceShape serviceWithResource = ServiceShape.builder()
                .version("1")
                .id("foo.baz#Service")
                .addResource(resourceWithOperation.getId())
                .build();
        ServiceShape serviceWithOperation = serviceWithResource.toBuilder()
                .addOperation(op.getId())
                .build();
        Model modelA = Model.assembler().addShapes(serviceWithResource, resourceWithOperation, op).assemble().unwrap();
        Model modelB =
                Model.assembler().addShapes(serviceWithOperation, resourceWithoutOperation, op).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);
        List<ValidationEvent> removedEvents = TestHelper.findEvents(
                events,
                "RemovedOperationBinding.FromResource.Operation");

        assertThat(removedEvents.size(), equalTo(1));
        assertThat(removedEvents.get(0).getSeverity(), equalTo(Severity.ERROR));
    }

    @Test
    public void detectsRemovedOperationFromResource() {
        SourceLocation source = new SourceLocation("foo.smithy");
        OperationShape o = OperationShape.builder().id("foo.baz#Operation").build();
        ResourceShape r1 =
                ResourceShape.builder().id("foo.baz#Resource").addOperation(o.getId()).source(source).build();
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
