/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import software.amazon.smithy.jmespath.RuntimeType;

/**
 * {@code requires(instance, predicate)} expresses a necessary precondition on an
 * operation instance: whenever the instance is a success (its {@code error} member
 * is null), {@code predicate} must be truthy. On a failure instance the requirement
 * is vacuously satisfied.
 *
 * <p>The predicate is evaluated eagerly. A necessary precondition reads only pre-call
 * state ({@code input} and {@code before}), which is present on both the success and
 * failure paths, so lazy evaluation buys nothing.
 */
class RequiresFunction implements Function {
    @Override
    public String name() {
        return "requires";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> arguments) {
        checkArgumentCount(2, arguments);
        T instance = arguments.get(0).expectObject();
        T predicate = arguments.get(1).expectValue();
        T error = runtime.value(instance, runtime.createString("error"));
        boolean succeeded = runtime.is(error, RuntimeType.NULL);
        return runtime.createBoolean(!succeeded || runtime.isTruthy(predicate));
    }
}
