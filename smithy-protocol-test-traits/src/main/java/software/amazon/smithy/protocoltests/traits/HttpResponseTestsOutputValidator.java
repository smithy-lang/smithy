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
 * Validates that protocol tests on output use the correct params.
 */
@SmithyInternalApi
public class HttpResponseTestsOutputValidator extends ProtocolTestCaseValidator<HttpResponseTestsTrait> {

    public HttpResponseTestsOutputValidator() {
        this("output");
    }

    HttpResponseTestsOutputValidator(String descriptor) {
        super(HttpResponseTestsTrait.ID, HttpResponseTestsTrait.class, descriptor);
    }

    @Override
    StructureShape getStructure(Shape shape, OperationIndex operationIndex) {
        return operationIndex.getOutputShape(shape).orElse(null);
    }

    @Override
    final List<? extends HttpMessageTestCase> getTestCases(HttpResponseTestsTrait trait) {
        return trait.getTestCases();
    }
}
