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
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A description of a service operation and input used to verify an endpoint rule-set test case.
 */
@SmithyUnstableApi
public final class EndpointTestOperationInput implements FromSourceLocation,
        ToSmithyBuilder<EndpointTestOperationInput> {
    private final SourceLocation sourceLocation;
    private final String operationName;
    private final ObjectNode operationParams;
    private final ObjectNode builtInParams;
    private final ObjectNode clientParams;

    private EndpointTestOperationInput(Builder builder) {
        this.sourceLocation = builder.sourceLocation;
        this.operationName = SmithyBuilder.requiredState("operationName", builder.operationName);
        this.operationParams = builder.operationParams;
        this.builtInParams = builder.builtInParams;
        this.clientParams = builder.clientParams;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getOperationName() {
        return operationName;
    }

    public ObjectNode getOperationParams() {
        return operationParams;
    }

    public ObjectNode getBuiltInParams() {
        return builtInParams;
    }

    public ObjectNode getClientParams() {
        return clientParams;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointTestOperationInput that = (EndpointTestOperationInput) o;
        return getOperationName().equals(that.getOperationName())
               && Objects.equals(getOperationParams(), that.getOperationParams())
               && Objects.equals(getBuiltInParams(), that.getBuiltInParams())
               && Objects.equals(getClientParams(), that.getClientParams());
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationName, getOperationParams(), getBuiltInParams(), getClientParams());
    }

    @Override
    public SmithyBuilder<EndpointTestOperationInput> toBuilder() {
        return builder()
                .sourceLocation(sourceLocation)
                .operationName(operationName)
                .operationParams(operationParams)
                .builtInParams(builtInParams)
                .clientParams(clientParams);
    }

    public static final class Builder implements SmithyBuilder<EndpointTestOperationInput> {
        private SourceLocation sourceLocation = SourceLocation.none();
        private String operationName;
        private ObjectNode operationParams;
        private ObjectNode builtInParams;
        private ObjectNode clientParams;

        public Builder sourceLocation(FromSourceLocation fromSourceLocation) {
            this.sourceLocation = fromSourceLocation.getSourceLocation();
            return this;
        }

        public Builder operationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        public Builder operationParams(ObjectNode operationParams) {
            this.operationParams = operationParams;
            return this;
        }

        public Builder builtInParams(ObjectNode builtinParams) {
            this.builtInParams = builtinParams;
            return this;
        }

        public Builder clientParams(ObjectNode clientParams) {
            this.clientParams = clientParams;
            return this;
        }

        @Override
        public EndpointTestOperationInput build() {
            return new EndpointTestOperationInput(this);
        }
    }
}
