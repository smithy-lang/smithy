/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * <p>Validates that shapes that have names that appear to be time values are
 * actually modeled with a timestamp shape.
 *
 * <p>This validator will check the name of string, integer, float, and long
 * shapes to see if they should have been {@link software.amazon.smithy.model.shapes.TimestampShape}.
 * It also scans structure members and union members. It uses the member name
 * and ensures the member targets a timestamp shape.
 *
 * <p>When considering if a name indicates a possible timestamp, the name must
 * have one of the following qualities:
 *
 * <ul>
 *     <li>Contains the string "timestamp"</li>
 *     <li>Begins or Ends with the word "time"</li>
 *     <li>Begins or Ends with the word "date"</li>
 *     <li>Ends with the word "at"</li>
 *     <li>Ends with the word "on"</li>
 * </ul>
 *
 * <p>When checking for one of the above words, the first character may be
 * upper or lower-cased. Words a separated by underscores or a pair of
 * lower-then-upper-cased characters, i.e. wordWord.</p>
 */
public final class ShouldHaveUsedTimestampValidator extends AbstractValidator {
    private static final List<String> ATTRIBUTES = List.of("additionalPatterns");
    private static final List<Pattern> DEFAULT_PATTERNS = List.of(
            Pattern.compile("^.*[Tt]imestamp.*$"), // contains the string "timestamp"
            Pattern.compile("^[Tt]ime([_A-Z].*)?$"), // begins with the word "time"
            Pattern.compile("^[Dd]ate([_A-Z].*)?$"), // begins with the word "date"
            Pattern.compile("^.*([a-z]T|_[Tt])ime$"), // ends with the word "time"
            Pattern.compile("^.*([a-z]D|_[Dd])ate$"), // ends with the word "date"
            Pattern.compile("^.*([a-z]A|_[Aa])t$"), // ends with the word "at"
            Pattern.compile("^.*([a-z]O|_[Oo])n$") // ends with the word "on"
    );

    private final List<Pattern> patterns;

    private ShouldHaveUsedTimestampValidator(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(ShouldHaveUsedTimestampValidator.class, configuration -> {
                List<Pattern> patterns = new ArrayList<>(DEFAULT_PATTERNS);
                configuration
                        .warnIfAdditionalProperties(ATTRIBUTES)
                        .getMember("additionalPatterns")
                        .orElse(Node.arrayNode())
                        .expectArrayNode()
                        .getElements()
                        .stream()
                        .map(node -> Pattern.compile(node.expectStringNode().getValue()))
                        .forEach(patterns::add);
                return new ShouldHaveUsedTimestampValidator(patterns);
            });
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        var index = model.getShapeIndex();
        var visitor = Shape.<List<ValidationEvent>>visitor()
                .when(StringShape.class, s -> validateSimpleShape(s, patterns))
                .when(ShortShape.class, s -> validateSimpleShape(s, patterns))
                .when(IntegerShape.class, s -> validateSimpleShape(s, patterns))
                .when(LongShape.class, s -> validateSimpleShape(s, patterns))
                .when(FloatShape.class, s -> validateSimpleShape(s, patterns))
                .when(StructureShape.class, shape -> validateStructure(shape, index, patterns))
                .when(UnionShape.class, shape -> validateUnion(shape, index, patterns))
                .orElse(List.of());
        return index.shapes().flatMap(shape -> shape.accept(visitor).stream()).collect(Collectors.toList());
    }

    private List<ValidationEvent> validateStructure(
            StructureShape structure,
            ShapeIndex shapeIndex,
            List<Pattern> patterns
    ) {
        return structure
                .getAllMembers()
                .entrySet()
                .stream()
                .flatMap(entry -> validateTargetShape(entry.getKey(), entry.getValue(), shapeIndex, patterns))
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateUnion(
            UnionShape union,
            ShapeIndex shapeIndex,
            List<Pattern> patterns
    ) {
        return union
                .getAllMembers()
                .entrySet()
                .stream()
                .flatMap(entry -> validateTargetShape(entry.getKey(), entry.getValue(), shapeIndex, patterns))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateTargetShape(
            String name,
            MemberShape target,
            ShapeIndex shapeIndex,
            List<Pattern> patterns
    ) {
        return shapeIndex.getShape(target.getTarget())
                .flatMap(shape -> validateName(name, shape.getType(), target, patterns))
                .stream();
    }

    private List<ValidationEvent> validateSimpleShape(
            Shape shape,
            List<Pattern> patterns
    ) {
        return validateName(shape.getId().getName(), shape.getType(), shape, patterns)
                .map(List::of)
                .orElse(List.of());
    }

    private Optional<ValidationEvent> validateName(
            String name,
            ShapeType type,
            Shape context,
            List<Pattern> patterns
    ) {
        if (type == ShapeType.TIMESTAMP) {
            return Optional.empty();
        }
        return patterns
                .stream()
                .filter(pattern -> pattern.matcher(name).matches())
                .map(matcher -> buildEvent(context, name, type))
                .findAny();
    }

    private ValidationEvent buildEvent(Shape context, String name, ShapeType type) {
        return danger(context, context.isMemberShape()
                ? String.format("Member `%s` is named like a timestamp but references a `%s` shape", name, type)
                : String.format("Shape `%s` is named like a timestamp but is a `%s` shape.", name, type));
    }
}
