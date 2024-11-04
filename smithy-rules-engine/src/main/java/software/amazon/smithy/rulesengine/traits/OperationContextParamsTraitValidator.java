/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.CurrentExpression;
import software.amazon.smithy.jmespath.ast.ExpressionTypeExpression;
import software.amazon.smithy.jmespath.ast.FieldExpression;
import software.amazon.smithy.jmespath.ast.FilterProjectionExpression;
import software.amazon.smithy.jmespath.ast.FlattenExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.IndexExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectHashExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectListExpression;
import software.amazon.smithy.jmespath.ast.NotExpression;
import software.amazon.smithy.jmespath.ast.ObjectProjectionExpression;
import software.amazon.smithy.jmespath.ast.OrExpression;
import software.amazon.smithy.jmespath.ast.ProjectionExpression;
import software.amazon.smithy.jmespath.ast.SliceExpression;
import software.amazon.smithy.jmespath.ast.Subexpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.analysis.OperationContextParamsChecker;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Validates {@link OperationContextParamsTrait} traits.
 */
@SmithyUnstableApi
public final class OperationContextParamsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ContextIndex index = ContextIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operationShape : model.getOperationShapes()) {
            Map<String, OperationContextParamDefinition> definitionMap = index.getOperationContextParams(operationShape)
                    .map(OperationContextParamsTrait::getParameters)
                    .orElse(Collections.emptyMap());
            for (Map.Entry<String, OperationContextParamDefinition> entry : definitionMap.entrySet()) {

                try {
                    JmespathExpression path = JmespathExpression.parse(entry.getValue().getPath());
                    LinterResult linterResult = OperationContextParamsChecker.lint(
                            entry.getValue(),
                            operationShape,
                            model);

                    if (!linterResult.getProblems().isEmpty()) {
                        events.add(error(operationShape,
                                String.format("The operation `%s` is marked with `%s` which contains a "
                                        + "path `%s` with an invalid JMESPath path `%s`: %s.",
                                        operationShape.getId(),
                                        OperationContextParamsTrait.ID.toString(),
                                        entry.getKey(),
                                        entry.getValue().getPath(),
                                        linterResult.getProblems()
                                                .stream()
                                                .map(p -> "'" + p.message + "'")
                                                .collect(Collectors.joining(", ")))));
                    }

                    List<String> unsupportedExpressions = path.accept(new UnsupportedJmesPathVisitor());
                    if (!unsupportedExpressions.isEmpty()) {
                        events.add(error(operationShape,
                                String.format("The operation `%s` is marked with `%s` which contains a "
                                        + "key `%s` with a JMESPath path `%s` with "
                                        + "unsupported expressions: %s.",
                                        operationShape.getId(),
                                        OperationContextParamsTrait.ID.toString(),
                                        entry.getKey(),
                                        entry.getValue().getPath(),
                                        unsupportedExpressions.stream()
                                                .map(e -> "'" + e + "'")
                                                .collect(Collectors.joining(", ")))));
                    }
                } catch (JmespathException e) {
                    events.add(error(operationShape,
                            String.format("The operation `%s` is marked with `%s` which contains a "
                                    + "key `%s` with an unparseable JMESPath path `%s`: %s.",
                                    operationShape.getId(),
                                    OperationContextParamsTrait.ID.toString(),
                                    entry.getKey(),
                                    entry.getValue().getPath(),
                                    e.getMessage())));
                }
            }
        }
        return events;
    }

    private static final class UnsupportedJmesPathVisitor implements ExpressionVisitor<List<String>> {

        @Override
        public List<String> visitComparator(ComparatorExpression expression) {
            return ListUtils.of("comparator");
        }

        @Override
        public List<String> visitCurrentNode(CurrentExpression expression) {
            return Collections.emptyList();
        }

        @Override
        public List<String> visitExpressionType(ExpressionTypeExpression expression) {
            return expression.getExpression().accept(this);
        }

        @Override
        public List<String> visitFlatten(FlattenExpression expression) {
            return expression.getExpression().accept(this);
        }

        @Override
        public List<String> visitFunction(FunctionExpression expression) {
            if (expression.getName().equals("keys")) {
                return expression.getArguments()
                        .stream()
                        .map(e -> e.accept(this))
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            } else {
                return ListUtils.of("`" + expression.getName() + "` function");
            }
        }

        @Override
        public List<String> visitField(FieldExpression expression) {
            return Collections.emptyList();
        }

        @Override
        public List<String> visitIndex(IndexExpression expression) {
            return ListUtils.of("index");
        }

        @Override
        public List<String> visitLiteral(LiteralExpression expression) {
            return ListUtils.of("literal");
        }

        @Override
        public List<String> visitMultiSelectList(MultiSelectListExpression expression) {
            List<String> unsupported = new ArrayList<>();
            expression.getExpressions().forEach(e -> unsupported.addAll(e.accept(this)));
            return Collections.unmodifiableList(unsupported);
        }

        @Override
        public List<String> visitMultiSelectHash(MultiSelectHashExpression expression) {
            return ListUtils.of("multiselect hash");
        }

        @Override
        public List<String> visitAnd(AndExpression expression) {
            return ListUtils.of("and");
        }

        @Override
        public List<String> visitOr(OrExpression expression) {
            return ListUtils.of("or");
        }

        @Override
        public List<String> visitNot(NotExpression expression) {
            return ListUtils.of("not");
        }

        @Override
        public List<String> visitProjection(ProjectionExpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }

        @Override
        public List<String> visitFilterProjection(FilterProjectionExpression expression) {
            return ListUtils.of("filter projection");
        }

        @Override
        public List<String> visitObjectProjection(ObjectProjectionExpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }

        @Override
        public List<String> visitSlice(SliceExpression expression) {
            return ListUtils.of("slice");
        }

        @Override
        public List<String> visitSubexpression(Subexpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }
    }
}
