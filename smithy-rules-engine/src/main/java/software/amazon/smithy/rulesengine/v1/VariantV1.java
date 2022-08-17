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
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public final class VariantV1 {
    public static final String DNS_SUFFIX = "dnsSuffix";
    public static final String HOSTNAME = "hostname";
    public static final String TAGS = "tags";

    private final String hostname;
    private final String dnsSuffix;
    private final Set<String> authSchemes;
    private final List<String> tags;

    private VariantV1(Builder b) {
        this.hostname = b.hostname;
        this.dnsSuffix = b.dnsSuffix;
        this.authSchemes = new HashSet<>(b.authSchemes);
        this.tags = new ArrayList<>(b.tags);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static VariantV1 fromNode(Node node) {
        ObjectNode on = node.expectObjectNode();

        Builder b = builder();

        on.getStringMember(DNS_SUFFIX).ifPresent(s -> b.dnsSuffix(s.getValue()));
        on.getStringMember(HOSTNAME).ifPresent(s -> b.hostname(s.getValue()));
        on.expectArrayMember(TAGS).forEach(e -> b.addTag(e.expectStringNode().getValue()));

        return b.build();
    }

    public String dnsSuffix() {
        return dnsSuffix;
    }

    public String hostname() {
        return hostname;
    }

    public List<String> tags() {
        return tags;
    }

    public Set<String> authSchemes() {
        return authSchemes;
    }

    public static class Builder {
        private String hostname;
        private String dnsSuffix;
        private Set<String> authSchemes = new HashSet<>();
        private Set<String> tags = new HashSet<>();

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder dnsSuffix(String dnsSuffix) {
            this.dnsSuffix = dnsSuffix;
            return this;
        }

        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder addAuthScheme(String authScheme) {
            this.authSchemes.add(authScheme);
            return this;
        }

        public VariantV1 build() {
            return new VariantV1(this);
        }
    }
}
