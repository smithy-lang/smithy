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

package software.amazon.smithy.rulesengine.language.syntax.fn;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.Into;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public abstract class Fn extends Expr implements Into<Condition> {
    protected final FnNode fnNode;

    public Fn(FnNode fnNode) {
        super(fnNode.getSourceLocation());
        this.fnNode = fnNode;
    }

    /**
     * Convert this fn into a condition.
     */
    public Condition condition() {
        return new Condition.Builder().fn(this).build();
    }

    public Condition condition(String result) {
        return new Condition.Builder().fn(this).result(result).build();
    }

    /**
     * Returns the name of this function, eg. {@code isSet}, {@code parseUrl}
     *
     * @return The name
     */
    public String getName() {
        return fnNode.getId();
    }

    /**
     * @return The arguments to this function
     */
    public List<Expr> getArgv() {
        return fnNode.getArgv();
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", fnNode.getId(), fnNode.getArgv().stream()
                .map(Expr::toString)
                .collect(Collectors.joining(", ")));
    }

    protected Expr expectOneArg() {
        List<Expr> argv = this.fnNode.getArgv();
        if (argv.size() == 1) {
            return argv.get(0);
        } else {
            throw new RuleError(
                    new SourceException("expected 1 argument but found " + argv.size(),
                            this.fnNode));
        }
    }

    @Override
    public SourceLocation getSourceLocation() {
        return fnNode.getSourceLocation();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Fn) {
            return ((Fn) obj).fnNode.equals(this.fnNode);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fnNode);
    }

    @Override
    public Node toNode() {
        return fnNode.toNode();
    }

    @Override
    public Condition into() {
        return this.condition();
    }
}
