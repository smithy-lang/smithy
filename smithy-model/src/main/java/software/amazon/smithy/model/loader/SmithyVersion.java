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

package software.amazon.smithy.model.loader;

enum SmithyVersion {

    VERSION_0_5_0("0.5.0");

    public final String value;

    SmithyVersion(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static boolean isSupported(String value) {
        for (SmithyVersion version : SmithyVersion.values()) {
            if (version.value.equals(value)) {
                return true;
            }
        }

        return false;
    }
}
