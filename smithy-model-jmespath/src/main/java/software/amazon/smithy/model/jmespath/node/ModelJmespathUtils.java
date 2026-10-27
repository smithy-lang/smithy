/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmespath.node;

import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.model.Model;
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
     * Creates a sample {@link LiteralExpression} value that is a valid value
     * of the given shape, or {@link LiteralExpression#ANY} if the shape is null.
     */
    public static LiteralExpression sampleShapeValue(Model model, Shape shape) {
        if (shape == null) {
            return LiteralExpression.ANY;
        }
        ModelRuntimeTypeGenerator generator = new ModelRuntimeTypeGenerator(model);
        // An operation call is not an instance of the operation shape, so build the
        // call tuple directly rather than visiting the operation shape.
        Object value = shape.asOperationShape().isPresent()
                ? generator.operationCall(shape.asOperationShape().get())
                : shape.accept(generator);
        return new LiteralExpression(value);
    }
}
