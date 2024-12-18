/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
