/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.validators.ServiceValidator;

public class ServiceValidatorTest {
    @Test
    public void allowsValidRenames() {
        StructureShape baz1 = StructureShape.builder()
                .id("smithy.example#Baz")
                .build();
        StructureShape baz2 = StructureShape.builder()
                .id("foo#Baz")
                .build();
        StructureShape output = StructureShape.builder()
                .id("smithy.example#Ouput")
                .addMember("a", baz1.getId())
                .addMember("b", baz2.getId())
                .build();
        OperationShape operation = OperationShape.builder()
                .id("smithy.example#Operation")
                .output(output)
                .build();
        ServiceShape service = ServiceShape.builder()
                .version("1")
                .id("smithy.example#Service")
                .addOperation(operation)
                .putRename(ShapeId.from("foo#Baz"), "FooBaz")
                .build();
        Model model = Model.builder()
                .addShapes(service, operation, output, baz2, baz1)
                .build();

        ServiceValidator validator = new ServiceValidator();

        assertThat(validator.validate(model), empty());
    }
}
