/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class AuthorizerIndexTest {
    @Test
    public void computesAuthorizers() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("effective-authorizers.smithy"))
                .assemble()
                .unwrap();

        AuthorizerIndex index = AuthorizerIndex.of(model);
        ShapeId serviceA = ShapeId.from("smithy.example#ServiceA");
        ShapeId serviceB = ShapeId.from("smithy.example#ServiceB");
        ShapeId resourceA = ShapeId.from("smithy.example#ResourceA");
        ShapeId resourceB = ShapeId.from("smithy.example#ResourceB");
        ShapeId operationA = ShapeId.from("smithy.example#OperationA");
        ShapeId operationB = ShapeId.from("smithy.example#OperationB");
        ShapeId operationC = ShapeId.from("smithy.example#OperationC");
        ShapeId operationD = ShapeId.from("smithy.example#OperationD");
        ShapeId operationE = ShapeId.from("smithy.example#OperationE");
        ShapeId operationF = ShapeId.from("smithy.example#OperationF");

        // Resolves service value.
        assertThat(index.getAuthorizer(serviceA).get(), equalTo("foo"));
        assertThat(index.getAuthorizerValue(serviceA).map(AuthorizerDefinition::getScheme),
                equalTo(Optional.of(SigV4Trait.ID)));
        assertThat(index.getAuthorizer(serviceB), equalTo(Optional.empty()));
        assertThat(index.getAuthorizerValue(serviceB), equalTo(Optional.empty()));

        // Resolves top-level operations.
        assertThat(index.getAuthorizer(serviceA, operationA).get(), equalTo("foo"));
        assertThat(index.getAuthorizer(serviceA, operationB).get(), equalTo("baz"));
        assertThat(index.getAuthorizer(serviceB, operationA), equalTo(Optional.empty()));
        assertThat(index.getAuthorizer(serviceA, operationB).get(), equalTo("baz"));

        // Resolves top-level resources.
        assertThat(index.getAuthorizer(serviceA, resourceA).get(), equalTo("foo"));
        assertThat(index.getAuthorizer(serviceB, resourceA), equalTo(Optional.empty()));
        assertThat(index.getAuthorizer(serviceA, resourceB).get(), equalTo("baz"));
        assertThat(index.getAuthorizer(serviceB, resourceB).get(), equalTo("baz"));

        // Resolves nested operations.
        assertThat(index.getAuthorizer(serviceA, operationC).get(), equalTo("foo"));
        assertThat(index.getAuthorizer(serviceA, operationD).get(), equalTo("baz"));
        assertThat(index.getAuthorizer(serviceA, operationE).get(), equalTo("baz"));
        assertThat(index.getAuthorizer(serviceA, operationF).get(), equalTo("foo"));

        assertThat(index.getAuthorizer(serviceB, operationC), equalTo(Optional.empty()));
        assertThat(index.getAuthorizer(serviceB, operationD).get(), equalTo("baz"));
        assertThat(index.getAuthorizer(serviceB, operationE).get(), equalTo("baz"));
        assertThat(index.getAuthorizer(serviceB, operationF).get(), equalTo("foo"));
    }
}
