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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a variation for a service endpoint. A variation can be at all level, i.e. it can appear at the
 * partition level, service, and a specific service for a region.
 */
@SmithyUnstableApi
public final class VariantModel implements ToSmithyBuilder<VariantModel> {
    private final String dnsSuffix;
    private final List<String> hostname;
    private final Set<String> authSchemes;

    private VariantModel(Builder b) {
        this.dnsSuffix = b.dnsSuffix;
        this.hostname = b.hostname;
        this.authSchemes = b.authSchemes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String dnsSuffix() {
        return dnsSuffix;
    }

    public List<String> hostname() {
        return hostname;
    }

    public Set<String> authSchemes() {
        return authSchemes;
    }

    public VariantModel merge(VariantModel other) {
        if (this.equals(other)) {
            return this;
        }

        Builder merged = toBuilder();

        if (this.hostname == null) {
            merged.hostname(other.hostname);
        }

        if (this.dnsSuffix == null) {
            merged.dnsSuffix(other.dnsSuffix);
        }

        if (this.authSchemes.isEmpty()) {
            merged.authSchemes(other.authSchemes);
        }

        return merged.build();
    }

    @Override
    public Builder toBuilder() {
        return builder().hostname(hostname).dnsSuffix(dnsSuffix).authSchemes(authSchemes);
    }

    public static class Builder implements SmithyBuilder<VariantModel> {
        private String dnsSuffix;
        private List<String> hostname = new ArrayList<>();
        private Set<String> authSchemes = new HashSet<>();

        public Builder dnsSuffix(String dnsSuffix) {
            this.dnsSuffix = dnsSuffix;
            return this;
        }

        public Builder hostname(List<String> splitHostname) {
            this.hostname.clear();
            this.hostname.addAll(splitHostname);
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname.clear();
            if (hostname != null) {
                this.hostname.addAll(StringUtils.splitTemplatedString(hostname));
            }
            return this;
        }

        public Builder authSchemes(Set<String> authSchemes) {
            this.authSchemes.clear();
            this.authSchemes.addAll(authSchemes);
            return this;
        }

        public Builder addAuthScheme(String authScheme) {
            this.authSchemes.add(authScheme);
            return this;
        }

        public VariantModel build() {
            return new VariantModel(this);
        }
    }
}
