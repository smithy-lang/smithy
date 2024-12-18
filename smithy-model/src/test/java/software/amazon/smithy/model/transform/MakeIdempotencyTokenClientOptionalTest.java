/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

public class MakeIdempotencyTokenClientOptionalTest {
    private static final ShapeId operationInput = ShapeId.from("smithy.example#IdempotencyTokenRequiredInput");

    @Test
    void compareTransform() {
        Model before = Model.assembler()
                .addImport(FlattenPaginationInfoTest.class.getResource("idempotency-token.smithy"))
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().makeIdempotencyTokensClientOptional(before);

        Shape input = result.expectShape(operationInput);
        Shape member = result.expectShape(input.getMember("token").get().getId());

        assertTrue(member.hasTrait(ClientOptionalTrait.class));
        assertTrue(member.hasTrait(RequiredTrait.class));
        assertTrue(member.hasTrait(IdempotencyTokenTrait.class));
    }
}
