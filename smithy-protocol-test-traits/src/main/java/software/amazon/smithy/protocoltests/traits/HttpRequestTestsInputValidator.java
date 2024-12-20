/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.List;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Ensures that input parameters of protocol request test cases are
 * valid for the attached operation.
 */
@SmithyInternalApi
public final class HttpRequestTestsInputValidator extends ProtocolTestCaseValidator<HttpRequestTestsTrait> {

    public HttpRequestTestsInputValidator() {
        super(HttpRequestTestsTrait.ID, HttpRequestTestsTrait.class, "input");
    }

    @Override
    StructureShape getStructure(Shape shape, OperationIndex operationIndex) {
        return operationIndex.expectInputShape(shape);
    }

    @Override
    List<? extends HttpMessageTestCase> getTestCases(HttpRequestTestsTrait trait) {
        return trait.getTestCases();
    }
}
