/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ServiceRenameTest {

    private static ServiceShape service;
    private static OperationShape operation;

    @BeforeAll
    public static void before() {
        operation = OperationShape.builder()
                .id("smithy.example#O")
                .build();
        service = ServiceShape.builder()
                .id("smithy.example#S")
                .version("1")
                .addOperation(operation)
                .build();
    }

    @Test
    public void detectsRenameRemoved() {
        SourceLocation source = new SourceLocation("foo.smithy");
        ServiceShape service1 = service.toBuilder()
                .putRename(operation.getId(), "O1")
                .build();
        Model modelA = Model.builder()
                .addShapes(operation, service1)
                .build();

        ServiceShape service2 = service.toBuilder()
                .clearRename()
                .source(source)
                .build();
        Model modelB = modelA.toBuilder().addShape(service2).build();

        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);
        assertThat(TestHelper.findEvents(events, "ServiceRename").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, service2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
        assertThat(events.get(0).getSourceLocation(), equalTo(source));
    }

    @Test
    public void detectsRenameChange() {
        SourceLocation source = new SourceLocation("foo.smithy");
        ServiceShape service1 = service.toBuilder()
                .putRename(operation.getId(), "O1")
                .build();
        Model modelA = Model.builder()
                .addShapes(operation, service1)
                .build();

        ServiceShape service2 = service.toBuilder()
                .putRename(operation.getId(), "O2")
                .source(source)
                .build();
        Model modelB = modelA.toBuilder().addShape(service2).build();

        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);
        assertThat(TestHelper.findEvents(events, "ServiceRename").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, service2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
        assertThat(events.get(0).getSourceLocation(), equalTo(source));
    }

    @Test
    public void detectsRenameAdded() {
        SourceLocation source = new SourceLocation("foo.smithy");
        Model modelA = Model.builder()
                .addShapes(operation, service)
                .build();

        ServiceShape service2 = service.toBuilder()
                .putRename(operation.getId(), "O2")
                .source(source)
                .build();
        Model modelB = modelA.toBuilder().addShape(service2).build();

        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ServiceRename").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, service2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
        assertThat(events.get(0).getSourceLocation(), equalTo(source));
    }

    @Test
    public void ignoresSameRenames() {
        ServiceShape service1 = service.toBuilder()
                .putRename(operation.getId(), "O1")
                .build();
        Model model = Model.builder()
                .addShapes(operation, service1)
                .build();

        List<ValidationEvent> events = ModelDiff.compare(model, model);
        assertThat(TestHelper.findEvents(events, "ServiceRename"), empty());
    }

    @Test
    public void ignoresRenamesOfNewShapes() {
        ServiceShape service1 = service.toBuilder()
                .removeOperation(operation.getId())
                .build();
        Model modelA = Model.builder()
                .addShapes(operation, service1)
                .build();

        ServiceShape service2 = service.toBuilder()
                .putRename(operation.getId(), "Oo")
                .build();
        Model modelB = Model.builder()
                .addShapes(service2, service1)
                .build();

        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ServiceRename"), empty());
    }
}
