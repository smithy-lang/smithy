/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
