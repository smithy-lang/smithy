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

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class ServiceEndpointV1 {
    public static final String HOSTNAME = "hostname";
    public static final String VARIANTS = "variants";

    private final String hostname;
    private final List<VariantV1> variants;

    private ServiceEndpointV1(Builder b) {
        this.hostname = b.hostname;
        this.variants = b.variants;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ServiceEndpointV1 fromNode(Node node) {
        Builder b = new Builder();

        ObjectNode on = node.expectObjectNode();

        on.getStringMember(HOSTNAME).ifPresent(s -> b.hostname(s.getValue()));
        on.getArrayMember(VARIANTS).ifPresent(variantsArray ->
                variantsArray.forEach(n -> b.addVariant(VariantV1.fromNode(n))));

        return b.build();
    }

    public String hostname() {
        return hostname;
    }

    public List<VariantV1> variants() {
        return variants;
    }

    public static class Builder {
        private String hostname;
        private List<VariantV1> variants = new ArrayList<>();

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder addVariant(VariantV1 variant) {
            this.variants.add(variant);
            return this;
        }

        public ServiceEndpointV1 build() {
            return new ServiceEndpointV1(this);
        }
    }
}
