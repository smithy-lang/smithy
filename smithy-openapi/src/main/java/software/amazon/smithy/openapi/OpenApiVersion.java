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

package software.amazon.smithy.openapi;

import java.util.Set;
import software.amazon.smithy.jsonschema.JsonSchemaVersion;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * OpenAPI versions supported by the converter.
 */
public enum OpenApiVersion {
    VERSION_3_0_2("3.0.2", false, JsonSchemaVersion.DRAFT07,
            SetUtils.of("propertyNames", "contentMediaType")),
    VERSION_3_1_0("3.1.0", true, JsonSchemaVersion.DRAFT2020_12,
            SetUtils.of("contentMediaType"));

    private final String version;
    private final boolean supportsContentEncodingKeyword;
    private final JsonSchemaVersion jsonSchemaVersion;
    // See https://swagger.io/docs/specification/data-models/keywords/ for 3.0.2.
    private final Set<String> unsupportedKeywords;

    OpenApiVersion(String version, boolean supportsContentEncodingKeyword, JsonSchemaVersion jsonSchemaVersion,
                   Set<String> unsupportedKeywords) {
        this.version = version;
        this.supportsContentEncodingKeyword = supportsContentEncodingKeyword;
        this.jsonSchemaVersion = jsonSchemaVersion;
        this.unsupportedKeywords = unsupportedKeywords;
    }

    @Override
    public String toString() {
        return version;
    }

    @SmithyInternalApi
    public boolean supportsContentEncodingKeyword() {
        return supportsContentEncodingKeyword;
    }

    @SmithyInternalApi
    public Set<String> getUnsupportedKeywords() {
        return unsupportedKeywords;
    }

    JsonSchemaVersion getJsonSchemaVersion() {
        return jsonSchemaVersion;
    }
}
