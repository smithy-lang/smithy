/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that protocol tests on errors use the correct params.
 */
@SmithyInternalApi
public final class HttpResponseTestsErrorValidator extends HttpResponseTestsOutputValidator {

    public HttpResponseTestsErrorValidator() {
        super("error");
    }

    @Override
    StructureShape getStructure(Shape shape, OperationIndex operationIndex) {
        return shape.asStructureShape().orElse(null);
    }

    @Override
    boolean isValidatedBy(Shape shape) {
        return shape instanceof StructureShape;
    }
}
