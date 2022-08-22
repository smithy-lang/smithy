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

package software.amazon.smithy.rulesengine.language.synth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum EndpointType {
    DEFAULT,
    FIPS,
    DUAL_STACK,
    FIPS_DUAL_STACK;

    public static EndpointType fromTagList(List<String> tagsList) {
        Set<String> tagSet = new HashSet<>(tagsList);

        if (tagSet.size() == 2 && tagSet.contains("fips") && tagSet.contains("dualstack")) {
            return FIPS_DUAL_STACK;
        } else if (tagSet.size() == 1 && tagSet.contains("fips")) {
            return FIPS;
        } else if (tagSet.size() == 1 && tagSet.contains("dualstack")) {
            return DUAL_STACK;
        } else {
            throw new RuntimeException("Unknown tag set: " + tagSet);
        }
    }
}
