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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class EndpointModel {
    private final String endpointPrefix;
    private final String dnsSuffix;

    private final List<String> hostname;
    private final Set<String> authSchemes;

    private final VariantModel fips;
    private final VariantModel dualStack;
    private final VariantModel fipsDualStack;

    private EndpointModel(Builder b) {
        this.endpointPrefix = b.endpointPrefix;
        this.dnsSuffix = b.dnsSuffix;
        this.hostname = b.hostname;
        this.authSchemes = b.authSchemes;

        this.fips = b.fips;
        this.dualStack = b.dualStack;
        this.fipsDualStack = b.fipsDualStack;
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

    public boolean hasFips() {
        return fips != null;
    }

    public boolean hasDualStack() {
        return dualStack != null;
    }

    public boolean hasFipsDualStack() {
        return fipsDualStack != null;
    }

    public EndpointModel merge(EndpointModel other) {
        Builder merged = toBuilder();

        if (merged.dnsSuffix == null) {
            merged.dnsSuffix(other.dnsSuffix);
        }

        if (merged.hostname == null || merged.hostname.isEmpty()) {
            merged.hostname(other.hostname);
        }

        if (merged.authSchemes == null || merged.authSchemes.isEmpty()) {
            merged.authSchemes(other.authSchemes);
        }

        merged.fips(mergeVariants(this.fips, other.fips));
        merged.dualStack(mergeVariants(this.dualStack, other.dualStack));
        merged.fipsDualStack(mergeVariants(this.fipsDualStack, other.fipsDualStack));

        return merged.build();
    }

    public EndpointView defaultView() {
        return EndpointView.builder()
                .endpointPrefix(endpointPrefix)
                .dnsSuffix(dnsSuffix)
                .hostname(hostname)
                .authSchemes(authSchemes)
                .build();
    }

    public Optional<EndpointView> fipsView() {
        if (!hasFips()) {
            return Optional.empty();
        }

        return Optional.of(applyVariant(fips));
    }

    public Optional<EndpointView> dualStackView() {
        if (!hasDualStack()) {
            return Optional.empty();
        }

        return Optional.of(applyVariant(dualStack));
    }

    public Optional<EndpointView> fipsDualStackView() {
        if (!hasFipsDualStack()) {
            return Optional.empty();
        }

        return Optional.of(applyVariant(fipsDualStack));
    }

    private EndpointView applyVariant(VariantModel variant) {
        EndpointView.Builder builder = defaultView()
                .toBuilder()
                .hostname(variant.hostname())
                .dnsSuffix(variant.dnsSuffix());

        if (!variant.authSchemes().isEmpty()) {
            builder.authSchemes(variant.authSchemes());
        }

        return builder.build();
    }

    private VariantModel mergeVariants(VariantModel higher, VariantModel lower) {
        if (higher != null && lower != null) {
            return higher.merge(lower);
        }

        if (higher != null) {
            return higher;
        }

        return lower;
    }

    public Builder toBuilder() {
        return builder()
                .endpointPrefix(endpointPrefix)
                .dnsSuffix(dnsSuffix)
                .hostname(hostname)
                .authSchemes(authSchemes)
                .fips(fips)
                .dualStack(dualStack)
                .fipsDualStack(fipsDualStack);
    }

    @Override
    public int hashCode() {
        int result = hostname != null ? hostname.hashCode() : 0;
        result = 31 * result + (authSchemes != null ? authSchemes.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EndpointModel that = (EndpointModel) o;

        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) {
            return false;
        }
        return authSchemes != null ? authSchemes.equals(that.authSchemes) : that.authSchemes == null;
    }

    public static class Builder {
        private String endpointPrefix;
        private String dnsSuffix;
        private List<String> hostname;
        private Set<String> authSchemes;

        private VariantModel fips;
        private VariantModel dualStack;
        private VariantModel fipsDualStack;

        public Builder endpointPrefix(String endpointPrefix) {
            this.endpointPrefix = endpointPrefix;
            return this;
        }

        public Builder dnsSuffix(String dnsSuffix) {
            this.dnsSuffix = dnsSuffix;
            return this;
        }

        public Builder hostname(String hostname) {
            if (hostname != null) {
                this.hostname = StringUtils.splitTemplatedString(hostname);
            } else {
                this.hostname = Collections.emptyList();
            }
            return this;
        }

        public Builder hostname(List<String> hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder authSchemes(Set<String> authParams) {
            this.authSchemes = authParams;
            return this;
        }

        public Builder fips(VariantModel fips) {
            this.fips = fips;
            return this;
        }

        public Builder dualStack(VariantModel dualStack) {
            this.dualStack = dualStack;
            return this;
        }

        public Builder fipsDualStack(VariantModel fipsDualStack) {
            this.fipsDualStack = fipsDualStack;
            return this;
        }

        public EndpointModel build() {
            return new EndpointModel(this);
        }
    }
}
