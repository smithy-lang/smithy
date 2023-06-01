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

package software.amazon.smithy.rulesengine.language.syntax.rule;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import java.util.Objects;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * A rule-set rule that specifies a resolved endpoint.
 */
@SmithyUnstableApi
public final class EndpointRule extends Rule {
    private final Endpoint endpoint;

    public EndpointRule(Rule.Builder builder, Endpoint endpoint) {
        super(builder);
        this.endpoint = endpoint;
    }

    /**
     * Retrieves the resolved endpoint description.
     *
     * @return the endpoint.
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> visitor) {
        return visitor.visitEndpointRule(endpoint);
    }

    @Override
    protected Type typecheckValue(Scope<Type> scope) {
        return context("while typechecking the endpoint", endpoint, () -> endpoint.typeCheck(scope));
    }

    @Override
    void withValueNode(ObjectNode.Builder builder) {
        builder.withMember("endpoint", endpoint).withMember(TYPE, ENDPOINT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        EndpointRule that = (EndpointRule) o;
        return endpoint.equals(that.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), endpoint);
    }

    @Override
    public String toString() {
        return super.toString() + StringUtils.indent(endpoint.toString(), 2);
    }
}
