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

public class ModelJmespathUtilities {

    public static final String JMESPATH_PROBLEM = "JmespathProblem";
    public static final String JMES_PATH_DANGER = "JmespathEventDanger";
    public static final String JMES_PATH_WARNING = "JmespathEventWarning";

    public static LinterResult lint(Model model, Shape shape, JmespathExpression expression) {
        return expression.lint(sampleShapeValue(model, shape));
    }

    public static LiteralExpression sampleShapeValue(Model model, Shape shape) {
        return shape == null
                ? LiteralExpression.ANY
                : new LiteralExpression(shape.accept(new ModelRuntimeTypeGenerator(model)));
    }
}
