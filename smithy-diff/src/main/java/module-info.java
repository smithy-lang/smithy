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

import software.amazon.smithy.diff.DiffEvaluator;
import software.amazon.smithy.diff.evaluators.AddedEntityBinding;
import software.amazon.smithy.diff.evaluators.AddedMetadata;
import software.amazon.smithy.diff.evaluators.AddedOperationError;
import software.amazon.smithy.diff.evaluators.AddedOperationInputOutput;
import software.amazon.smithy.diff.evaluators.AddedShape;
import software.amazon.smithy.diff.evaluators.AddedTraitDefinition;
import software.amazon.smithy.diff.evaluators.ChangedEnumTrait;
import software.amazon.smithy.diff.evaluators.ChangedLengthTrait;
import software.amazon.smithy.diff.evaluators.ChangedMemberTarget;
import software.amazon.smithy.diff.evaluators.ChangedMetadata;
import software.amazon.smithy.diff.evaluators.ChangedOperationInput;
import software.amazon.smithy.diff.evaluators.ChangedOperationOutput;
import software.amazon.smithy.diff.evaluators.ChangedRangeTrait;
import software.amazon.smithy.diff.evaluators.ChangedResourceIdentifiers;
import software.amazon.smithy.diff.evaluators.ChangedShapeType;
import software.amazon.smithy.diff.evaluators.ModifiedTrait;
import software.amazon.smithy.diff.evaluators.RemovedAuthenticationScheme;
import software.amazon.smithy.diff.evaluators.RemovedEntityBinding;
import software.amazon.smithy.diff.evaluators.RemovedMetadata;
import software.amazon.smithy.diff.evaluators.RemovedOperationError;
import software.amazon.smithy.diff.evaluators.RemovedOperationInput;
import software.amazon.smithy.diff.evaluators.RemovedOperationOutput;
import software.amazon.smithy.diff.evaluators.RemovedProtocolAuthentication;
import software.amazon.smithy.diff.evaluators.RemovedShape;
import software.amazon.smithy.diff.evaluators.RemovedTraitDefinition;

module software.amazon.smithy.diff {
    requires software.amazon.smithy.model;

    exports software.amazon.smithy.diff;
    exports software.amazon.smithy.diff.evaluators;

    uses DiffEvaluator;

    provides DiffEvaluator with
            AddedEntityBinding,
            AddedMetadata,
            AddedOperationError,
            AddedOperationInputOutput,
            AddedShape,
            AddedTraitDefinition,
            ChangedEnumTrait,
            ChangedLengthTrait,
            ChangedMemberTarget,
            ChangedMetadata,
            ChangedOperationInput,
            ChangedOperationOutput,
            ChangedRangeTrait,
            ChangedResourceIdentifiers,
            ChangedShapeType,
            ModifiedTrait,
            RemovedAuthenticationScheme,
            RemovedEntityBinding,
            RemovedMetadata,
            RemovedOperationError,
            RemovedOperationInput,
            RemovedOperationOutput,
            RemovedProtocolAuthentication,
            RemovedShape,
            RemovedTraitDefinition;
}
