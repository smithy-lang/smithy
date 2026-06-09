/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.ShapeMatcher;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ApplyResourceBasedTargetsTest {

    @Test
    public void detectsPrivateResourceTargetElisionForStructure() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("private-resource-elision/private-resource.smithy"))
                .addImport(getClass().getResource("private-resource-elision/cross-namespace-structure.smithy"))
                .assemble();

        assertThat(ShapeId.from("smithy.example#InvalidElidedStructure"),
                ShapeMatcher.hasEvent(result, "PrivateAccess", Severity.DANGER, "smithy.private#PrivateResource"));
    }

    @Test
    public void allowsPrivateResourceTargetElisionWithinSameNamespace() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("private-resource-elision/same-namespace-structure.smithy"))
                .assemble();

        assertThat(privateAccessEventIds(result), is(empty()));
    }

    @Test
    public void allowsCrossNamespaceTargetElisionForPublicResource() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("private-resource-elision/public-resource.smithy"))
                .addImport(getClass().getResource("private-resource-elision/cross-namespace-public.smithy"))
                .assemble();

        assertThat(privateAccessEventIds(result), is(empty()));
    }

    private static List<ValidationEvent> privateAccessEventIds(ValidatedResult<Model> result) {
        return result.getValidationEvents()
                .stream()
                .filter(e -> e.getId().equals("PrivateAccess"))
                .collect(Collectors.toList());
    }
}
