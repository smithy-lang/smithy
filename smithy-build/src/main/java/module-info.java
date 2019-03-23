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

import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.build.plugins.BuildInfoPlugin;
import software.amazon.smithy.build.plugins.ModelPlugin;
import software.amazon.smithy.build.transforms.ExcludeMetadata;
import software.amazon.smithy.build.transforms.ExcludeShapesByTag;
import software.amazon.smithy.build.transforms.ExcludeTags;
import software.amazon.smithy.build.transforms.ExcludeTraits;
import software.amazon.smithy.build.transforms.ExcludeTraitsByTag;
import software.amazon.smithy.build.transforms.IncludeAuthentication;
import software.amazon.smithy.build.transforms.IncludeMetadata;
import software.amazon.smithy.build.transforms.IncludeNamespaces;
import software.amazon.smithy.build.transforms.IncludeProtocols;
import software.amazon.smithy.build.transforms.IncludeServices;
import software.amazon.smithy.build.transforms.IncludeShapesByTag;
import software.amazon.smithy.build.transforms.IncludeTags;
import software.amazon.smithy.build.transforms.IncludeTraits;
import software.amazon.smithy.build.transforms.IncludeTraitsByTag;
import software.amazon.smithy.build.transforms.RemoveUnusedShapes;

module software.amazon.smithy.build {
    requires java.logging;
    requires software.amazon.smithy.model;

    exports software.amazon.smithy.build;

    uses SmithyBuildPlugin;
    uses ProjectionTransformer;

    provides SmithyBuildPlugin with
            BuildInfoPlugin,
            ModelPlugin;

    provides ProjectionTransformer with
            ExcludeMetadata,
            ExcludeShapesByTag,
            ExcludeTags,
            ExcludeTraits,
            ExcludeTraitsByTag,
            IncludeAuthentication,
            IncludeMetadata,
            IncludeNamespaces,
            IncludeProtocols,
            IncludeServices,
            IncludeShapesByTag,
            IncludeTags,
            IncludeTraits,
            IncludeTraitsByTag,
            RemoveUnusedShapes;
}
