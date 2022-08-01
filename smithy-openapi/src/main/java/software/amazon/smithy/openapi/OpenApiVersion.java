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

public enum OpenApiVersion {
    /** OpenAPI versions supported by the converter. */
    VERSION_3_0_2("3.0.2", true, false),
    VERSION_3_1_0("3.1.0", false, true);

    private final String version;
    private final boolean supportsNullableKeyword;
    private final boolean supportsContentEncodingKeyword;

    OpenApiVersion(String version, boolean supportsNullableKeyword, boolean supportsContentEncodingKeyword) {
        this.version = version;
        this.supportsNullableKeyword = supportsNullableKeyword;
        this.supportsContentEncodingKeyword = supportsContentEncodingKeyword;
    }

    @Override
    public String toString() {
        return version;
    }

    public boolean supportsNullableKeyword() {
        return supportsNullableKeyword;
    }

    public boolean supportsContentEncodingKeyword() {
        return supportsContentEncodingKeyword;
    }
}
