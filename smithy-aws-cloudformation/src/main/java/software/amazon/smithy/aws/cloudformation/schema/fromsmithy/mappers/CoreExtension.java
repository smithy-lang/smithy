/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
                new HandlerPermissionMapper(),
                new IdentifierMapper(),
                new JsonAddMapper(),
                new MutabilityMapper(),
                new RequiredMapper(),
                new TaggingMapper());
    }
}
