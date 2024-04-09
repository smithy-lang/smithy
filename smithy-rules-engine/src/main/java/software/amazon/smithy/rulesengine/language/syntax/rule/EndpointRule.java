/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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

    EndpointRule(Rule.Builder builder, Endpoint endpoint) {
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
