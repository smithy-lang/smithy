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

package software.amazon.smithy.jsonschema;

/**
 * Configures the schema version to use when converting Smithy shapes to JSON Schema.
 */
public enum JsonSchemaVersion {
    DRAFT07("draft07", "http://json-schema.org/draft-07/schema#", "#/definitions"),
    DRAFT2020_12("draft2020-12", "http://json-schema.org/draft/2020-12/schema#", "#/$defs");

    private final String versionName;
    private final String location;
    private final String defaultDefinitionPointer;

    JsonSchemaVersion(String versionName, String location, String defaultDefinitionPointer) {
        this.versionName = versionName;
        this.location = location;
        this.defaultDefinitionPointer = defaultDefinitionPointer;
    }

    public String toString() {
        return versionName;
    }

    public String getLocation() {
        return this.location;
    }

    String getDefaultDefinitionPointer() {
        return this.defaultDefinitionPointer;
    }
}
