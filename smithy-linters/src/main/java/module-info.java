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

import software.amazon.smithy.model.validation.ValidatorService;
import software.smithy.linters.AbbreviationNameValidator;
import software.smithy.linters.CamelCaseValidator;
import software.smithy.linters.EmitEachSelectorValidator;
import software.smithy.linters.EmitNoneSelectorValidator;
import software.smithy.linters.InputOutputStructureReuseValidator;
import software.smithy.linters.MissingPaginatedTraitValidator;
import software.smithy.linters.ReservedWordsValidator;
import software.smithy.linters.ShouldHaveUsedTimestampValidator;
import software.smithy.linters.StandardOperationVerbValidator;
import software.smithy.linters.StutteredShapeNameValidator;
import software.smithy.linters.UnreferencedShapeValidator;

module software.amazon.smithy.linters {
    requires java.logging;
    requires software.amazon.smithy.model;

    uses ValidatorService;

    // Configurable validators.
    provides ValidatorService with
            AbbreviationNameValidator,
            CamelCaseValidator,
            EmitEachSelectorValidator,
            EmitNoneSelectorValidator,
            InputOutputStructureReuseValidator,
            MissingPaginatedTraitValidator,
            ReservedWordsValidator,
            ShouldHaveUsedTimestampValidator,
            StandardOperationVerbValidator,
            StutteredShapeNameValidator,
            UnreferencedShapeValidator;
}
