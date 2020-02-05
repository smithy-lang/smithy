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

package software.amazon.smithy.openapi.fromsmithy;

import java.util.List;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

/**
 * An extension mechanism used to influence how Smithy models are converted
 * to OpenAPI models.
 *
 * <p>Implementations of this interface are discovered through Java SPI.
 */
public interface Smithy2OpenApiExtension {
    /**
     * Registers additional security scheme converters.
     *
     * @return Returns the converters to register.
     */
    default List<SecuritySchemeConverter<? extends Trait>> getSecuritySchemeConverters() {
        return ListUtils.of();
    }

    /**
     * Registers additional protocols that handle serialization and
     * deserialization.
     *
     * @return Returns the protocols to register.
     */
    default List<OpenApiProtocol<? extends Trait>> getProtocols() {
        return ListUtils.of();
    }

    /**
     * Registers OpenAPI mappers, classes used to modify and extend the
     * process of converting a Smithy model to OpenAPI.
     *
     * @return Returns the mappers to register.
     */
    default List<OpenApiMapper> getOpenApiMappers() {
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
