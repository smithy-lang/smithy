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

package software.amazon.smithy.rulesengine.traits;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An endpoint test case expectation.
 */
@SmithyUnstableApi
public final class EndpointTestExpectation implements FromSourceLocation, ToSmithyBuilder<EndpointTestExpectation> {
    private final SourceLocation sourceLocation;
    private final String error;
    private final ExpectedEndpoint endpoint;

    EndpointTestExpectation(Builder builder) {
        this.sourceLocation = builder.sourceLocation;
        this.error = builder.error;
        this.endpoint = builder.endpoint;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }

    public Optional<ExpectedEndpoint> getEndpoint() {
        return Optional.ofNullable(endpoint);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(sourceLocation)
                .endpoint(endpoint)
                .error(error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getError(), getEndpoint());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointTestExpectation that = (EndpointTestExpectation) o;
        return Objects.equals(getError(), that.getError()) && Objects.equals(getEndpoint(), that.getEndpoint());
    }

    public static final class Builder implements SmithyBuilder<EndpointTestExpectation> {
        private SourceLocation sourceLocation = SourceLocation.none();
        private String error;
        private ExpectedEndpoint endpoint;

        private Builder() {
        }

        public Builder sourceLocation(FromSourceLocation fromSourceLocation) {
            this.sourceLocation = fromSourceLocation.getSourceLocation();
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder endpoint(ExpectedEndpoint endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public EndpointTestExpectation build() {
            return new EndpointTestExpectation(this);
        }
    }
}
