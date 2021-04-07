/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import java.util.List;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Smithy2CfnExtension;
import software.amazon.smithy.utils.ListUtils;

/**
 * Registers the core Smithy2CloudFormation functionality.
 */
public final class CoreExtension implements Smithy2CfnExtension {
    @Override
    public List<CfnMapper> getCfnMappers() {
        return ListUtils.of(
                new AdditionalPropertiesMapper(),
                new DeprecatedMapper(),
                new DocumentationMapper(),
                new IdentifierMapper(),
                new JsonAddMapper(),
                new MutabilityMapper());
    }
}
