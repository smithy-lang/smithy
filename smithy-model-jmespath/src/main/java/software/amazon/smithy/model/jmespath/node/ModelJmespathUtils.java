/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmespath.node;

import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Provides utilities for working with JMESPath expressions in the context of a Smithy model.
 */
public final class ModelJmespathUtils {

    /**
     * Constant for "JmespathProblem", a default validation event ID segment.
     */
    public static final String JMES_PATH_PROBLEM = "JmespathProblem";

    /**
     * Constant for "JmespathEventDanger", a default validation event ID segment.
     */
    public static final String JMES_PATH_DANGER = "JmespathEventDanger";

    /**
     * Constant for "JmespathEventWarning", a default validation event ID segment.
     */
    public static final String JMES_PATH_WARNING = "JmespathEventWarning";

    /**
     * Lint the expression using static analysis.
     *
     * @param shape The shape the current node will be a value of.
     * @return Returns the problems that were detected.
     */
    public static LinterResult lint(Model model, Shape shape, JmespathExpression expression) {
        return expression.lint(sampleShapeValue(model, shape));
    }

    /**
     * Lint the expression against the static type of an operation call.
     *
     * @param model The model the operation belongs to.
     * @param operation The operation whose call the expression evaluates over.
     * @param expression The expression to lint.
     * @return Returns the problems that were detected.
     */
    public static LinterResult lintOperationCall(Model model, OperationShape operation, JmespathExpression expression) {
        return expression.lint(sampleOperationCall(model, operation));
    }

    /**
     * Creates a sample {@link LiteralExpression} value that is a valid value
     * of the given shape, or {@link LiteralExpression#ANY} if the shape is null.
     */
    public static LiteralExpression sampleShapeValue(Model model, Shape shape) {
        return shape == null
                ? LiteralExpression.ANY
                : new LiteralExpression(shape.accept(new ModelRuntimeTypeGenerator(model)));
    }

    /**
     * Creates a sample {@link LiteralExpression} value that is a valid operation call: the
     * {@code {input, output, error, before, after}} tuple observed for a single call.
     *
     * <p>An operation call is not an instance of the operation shape, so this is distinct
     * from {@link #sampleShapeValue} and is built directly rather than by visiting the shape.
     */
    public static LiteralExpression sampleOperationCall(Model model, OperationShape operation) {
        return new LiteralExpression(new ModelRuntimeTypeGenerator(model).operationCall(operation));
    }
}
