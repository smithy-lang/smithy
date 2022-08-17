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

package software.amazon.smithy.rulesengine.reterminus.synth;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EndpointView {
    private final String endpointPrefix;
    private final String dnsSuffix;
    private final List<String> hostname;
    private final Set<String> authSchemes;

    private EndpointView(Builder b) {
        this.endpointPrefix = b.endpointPrefix;
        this.dnsSuffix = b.dnsSuffix;
        this.hostname = b.hostname;
        this.authSchemes = b.authSchemes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String endpointPrefix() {
        return endpointPrefix;
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

    public String reifyHostName(Function<String, String> templateResolver) {
        return hostname.stream()
                .map(s -> {
                    if (!StringUtils.isTemplated(s)) {
                        return s;
                    }

                    String resolved = templateResolver.apply(s);
                    if (resolved != null) {
                        return resolved;
                    }

                    resolved = resolveTemplate(s);
                    if (resolved != null) {
                        return resolved;
                    }

                    throw new RuntimeException("Unresolved placeholder " + s);
                }).collect(Collectors.joining());
    }

    /**
     * Not strictly an equality check because this does not compare {@code endpointPrefix} and {@code dnsSuffix}.
     */
    public boolean differsFrom(EndpointView other) {
        return !hostname().equals(other.hostname());
//                && authSchemes().equals(other.authSchemes()));
    }

    public Builder toBuilder() {
        return builder()
                .endpointPrefix(endpointPrefix)
                .dnsSuffix(dnsSuffix)
                .hostname(hostname)
                .authSchemes(authSchemes);
    }

    private String resolveTemplate(String s) {
        switch (s) {
            case "{service}":
                return endpointPrefix;
            case "{dnsSuffix}":
                return dnsSuffix;
            default:
                return null;
        }
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

        EndpointView that = (EndpointView) o;

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

        public Builder endpointPrefix(String endpointPrefix) {
            this.endpointPrefix = endpointPrefix;
            return this;
        }

        public Builder dnsSuffix(String dnsSuffix) {
            this.dnsSuffix = dnsSuffix;
            return this;
        }

        public Builder hostname(List<String> hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder authSchemes(Set<String> authSchemes) {
            this.authSchemes = authSchemes;
            return this;
        }

        public EndpointView build() {
            return new EndpointView(this);
        }
    }
}
