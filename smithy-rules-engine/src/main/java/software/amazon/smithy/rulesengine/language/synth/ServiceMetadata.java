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

import java.util.Optional;

public final class ServiceMetadata {
    private final String serviceId;
    private final String endpointPrefix;
    private final String signingName;

    private ServiceMetadata(Builder b) {
        this.serviceId = b.serviceId;
        this.endpointPrefix = b.endpointPrefix;
        this.signingName = b.signingName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String serviceId() {
        return serviceId;
    }

    public String endpointPrefix() {
        return endpointPrefix;
    }

    public Optional<String> signingName() {
        return Optional.ofNullable(signingName);
    }

    public static class Builder {
        private String serviceId;
        private String endpointPrefix;
        private String signingName;

        public Builder serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder endpointPrefix(String endpointPrefix) {
            this.endpointPrefix = endpointPrefix;
            return this;
        }

        public Builder signingName(String signingName) {
            this.signingName = signingName;
            return this;
        }

        public ServiceMetadata build() {
            return new ServiceMetadata(this);
        }
    }
}
