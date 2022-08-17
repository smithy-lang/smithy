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

package software.amazon.smithy.rulesengine.reterminus.lang.fn;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.Into;
import software.amazon.smithy.rulesengine.reterminus.error.RuleError;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Expr;
import software.amazon.smithy.rulesengine.reterminus.lang.rule.Condition;
import software.amazon.smithy.rulesengine.reterminus.visit.ExprVisitor;
import software.amazon.smithy.rulesengine.reterminus.visit.FnVisitor;
import software.amazon.smithy.utils.Pair;

public abstract class Fn extends Expr implements Into<Condition> {
    protected FnNode fnNode;

    public Fn(FnNode fnNode) {
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

    public abstract <T> T acceptFnVisitor(FnVisitor<T> visitor);

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

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitFn(this);
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

    protected Pair<Expr, Expr> expectTwoArgs() {
        List<Expr> argv = this.fnNode.getArgv();
        if (argv.size() == 2) {
            return Pair.of(argv.get(0), argv.get(1));
        } else {
            throw new RuleError(
                    new SourceException("expected 2 arguments but found " + argv.size(),
                            this.fnNode));
        }

    }

    protected List<Expr> expectVariableArgs(int expectedNumberArgs) {
        List<Expr> argv = this.fnNode.getArgv();
        if (argv.size() == expectedNumberArgs) {
            return argv;
        } else {
            throw new RuleError(
                    new SourceException(String.format("expected %d arguments but found %d",
                            expectedNumberArgs, argv.size()), this.fnNode));
        }

    }

    @Override
    public Condition into() {
        return this.condition();
    }
}
