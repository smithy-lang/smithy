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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public final class DefaultsV1 {
    public static final String VARIANTS = "variants";
    public static final String HOSTNAME = "hostname";
    public static final String SIGNATURE_VERSIONS = "signatureVersions";
    public static final String DEPRECATED = "deprecated";

    private final String dnsSuffix;
    private final String hostname;
    private final List<VariantV1> variants;
    private final Set<String> signatureVersions;
    private final boolean deprecated;

    private DefaultsV1(Builder b) {
        this.dnsSuffix = b.dnsSuffix;
        this.hostname = b.hostname;
        this.variants = b.variants;
        this.signatureVersions = b.signatureVersions;
        this.deprecated = b.deprecated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DefaultsV1 fromNode(Node node) {
        ObjectNode on = node.expectObjectNode();

        Builder b = builder();

        on.getStringMember(HOSTNAME).ifPresent(s -> b.hostname(s.getValue()));
        Optional<ArrayNode> variantsNode = on.getArrayMember(VARIANTS);

        variantsNode.ifPresent(n -> {
            List<Node> elements = n.getElements();
            elements.forEach(e ->
                    b.addVariant(VariantV1.fromNode(e))
            );
        });

        Optional<ArrayNode> signatureVersions = on.getArrayMember(SIGNATURE_VERSIONS);
        signatureVersions.ifPresent(n -> {
            List<Node> elements = n.getElements();
            elements.forEach(e -> {
                b.addSignatureVersion(e.expectStringNode().getValue());
            });
        });

        on.getBooleanMember(DEPRECATED).ifPresent(d -> b.deprecated(d.getValue()));

        return b.build();
    }

    public String dnsSuffix() {
        return dnsSuffix;
    }

    public String hostname() {
        return hostname;
    }

    public List<VariantV1> variants() {
        return variants;
    }

    public Set<String> signatureVersions() {
        return signatureVersions;
    }

    public boolean deprecated() {
        return deprecated;
    }

    public static class Builder {
        private String dnsSuffix;
        private String hostname;
        private List<VariantV1> variants = new ArrayList<>();
        private Set<String> signatureVersions = new HashSet<>();
        private boolean deprecated;

        public Builder dnsSuffix(String dnsSuffix) {
            this.dnsSuffix = dnsSuffix;
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder addVariant(VariantV1 variant) {
            this.variants.add(variant);
            return this;
        }

        public Builder addSignatureVersion(String signatureVersion) {
            this.signatureVersions.add(signatureVersion);
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public DefaultsV1 build() {
            return new DefaultsV1(this);
        }
    }
}
