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

package software.amazon.smithy.rulesengine.v1;

import java.util.HashMap;
import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public final class ServiceV1 {
    public static final String DEFAULTS = "defaults";
    public static final String ENDPOINTS = "endpoints";

    private final DefaultsV1 defaults;
    private final Map<String, DefaultsV1> endpoints;

    private ServiceV1(Builder b) {
        this.defaults = b.defaults;
        this.endpoints = b.endpoints;
    }

    public static ServiceV1 fromNode(Node node) {
        ObjectNode on = node.expectObjectNode();
        Builder b = builder();

        on.getObjectMember(DEFAULTS).ifPresent(n -> b.defaults(DefaultsV1.fromNode(n)));
        on.expectObjectMember(ENDPOINTS).getStringMap().forEach((region, endpointNode) ->
                b.putEndpoint(region, DefaultsV1.fromNode(endpointNode))
        );

        return b.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public DefaultsV1 defaults() {
        return defaults;
    }

    public Map<String, DefaultsV1> endpoints() {
        return endpoints;
    }

    public static class Builder {
        private DefaultsV1 defaults;
        private Map<String, DefaultsV1> endpoints = new HashMap<>();

        public Builder defaults(DefaultsV1 defaults) {
            this.defaults = defaults;
            return this;
        }

        public Builder putEndpoint(String region, DefaultsV1 regionDefaults) {
            endpoints.put(region, regionDefaults);
            return this;
        }

        public ServiceV1 build() {
            return new ServiceV1(this);
        }
    }
}
