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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Describes an endpoint rule-set test case.
 */
@SmithyUnstableApi
public final class EndpointTestCase implements FromSourceLocation, ToSmithyBuilder<EndpointTestCase> {
    private final SourceLocation sourceLocation;
    private final String documentation;
    private final ObjectNode params;
    private final List<EndpointTestOperationInput> operationInputs;
    private final EndpointTestExpectation expect;

    public EndpointTestCase(Builder builder) {
        this.sourceLocation = builder.sourceLocation;
        this.documentation = builder.documentation;
        this.params = builder.params;
        this.operationInputs = builder.operationInputs.copy();
        this.expect = builder.expect;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    public ObjectNode getParams() {
        return params;
    }

    public List<EndpointTestOperationInput> getOperationInputs() {
        return operationInputs;
    }

    public EndpointTestExpectation getExpect() {
        return expect;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(sourceLocation)
                .documentation(documentation)
                .params(params)
                .operationInputs(operationInputs)
                .expect(expect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDocumentation(), getParams(), getOperationInputs(), getExpect());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointTestCase that = (EndpointTestCase) o;
        return Objects.equals(getDocumentation(), that.getDocumentation())
               && Objects.equals(getParams(), that.getParams())
               && Objects.equals(getOperationInputs(), that.getOperationInputs())
               && Objects.equals(getExpect(), that.getExpect());
    }

    public static final class Builder implements SmithyBuilder<EndpointTestCase> {
        private final BuilderRef<List<EndpointTestOperationInput>> operationInputs = BuilderRef.forList();
        private SourceLocation sourceLocation = SourceLocation.none();
        private String documentation;
        private ObjectNode params;
        private EndpointTestExpectation expect;

        private Builder() {
        }

        public Builder sourceLocation(FromSourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation.getSourceLocation();
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder params(ObjectNode params) {
            this.params = params;
            return this;
        }

        public Builder operationInputs(List<EndpointTestOperationInput> operationInputs) {
            this.operationInputs.clear();
            this.operationInputs.get().addAll(operationInputs);
            return this;
        }

        public Builder addOperationInput(EndpointTestOperationInput operationInput) {
            this.operationInputs.get().add(operationInput);
            return this;
        }

        public Builder removeOperationInput(EndpointTestOperationInput operationInput) {
            this.operationInputs.get().remove(operationInput);
            return this;
        }

        public Builder expect(EndpointTestExpectation expect) {
            this.expect = expect;
            return this;
        }

        @Override
        public EndpointTestCase build() {
            return new EndpointTestCase(this);
        }
    }
}
