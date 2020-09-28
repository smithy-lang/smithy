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

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import java.util.List;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.utils.ListUtils;

/**
 * An extension mechanism used to influence how CloudFormation resource schemas
 * are generated from Smithy models.
 *
 * <p>Implementations of this interface are discovered through Java SPI.
 */
public interface Smithy2CfnExtension {

    /**
     * Registers CloudFormation mappers, classes used to modify and extend the
     * process of converting a Smithy model to CloudFormation resource schemas.
     *
     * @return Returns the mappers to register.
     */
    default List<CfnMapper> getCfnMappers() {
        return ListUtils.of();
    }

    /**
     * Registers JsonSchema mappers that are used to modify JsonSchema
     * definitions created from a Smithy model.
     *
     * @return Returns the mappers to register.
     */
    default List<JsonSchemaMapper> getJsonSchemaMappers() {
        return ListUtils.of();
    }
}
