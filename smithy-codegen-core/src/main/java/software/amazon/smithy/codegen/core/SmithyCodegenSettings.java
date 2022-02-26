/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Base interface used to represent the settings of a code generator.
 *
 * <p>This interface is currently unstable as more requirements
 * may be added in the future.
 */
@SmithyUnstableApi
public interface SmithyCodegenSettings {
    /**
     * Gets the service configured for the code generator.
     *
     * @return Returns the service shape ID to generate.
     */
    ShapeId service();
}
