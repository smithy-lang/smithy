/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.OptionalUtils;

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

    public static final class Config {
        private List<Pattern> additionalPatterns = new ArrayList<>();

        /**
         * Sets a list of additional regular expression patterns that indicate a
         * value carries a timestamp.
         *
         * @param additionalPatterns Additional patterns to add.
         */
        public void setAdditionalPatterns(List<Pattern> additionalPatterns) {
            this.additionalPatterns = additionalPatterns;
        }

        public List<Pattern> getAdditionalPatterns() {
            return additionalPatterns;
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(ShouldHaveUsedTimestampValidator.class, configuration -> {
                Config config = new NodeMapper().deserialize(configuration, Config.class);
                return new ShouldHaveUsedTimestampValidator(config);
            });
        }
    }

    private static final List<Pattern> DEFAULT_PATTERNS = ListUtils.of(
            Pattern.compile("^.*[Tt]imestamp.*$"), // contains the string "timestamp"
            Pattern.compile("^[Tt]ime([_A-Z].*)?$"), // begins with the word "time"
            Pattern.compile("^[Dd]ate([_A-Z].*)?$"), // begins with the word "date"
            Pattern.compile("^.*([a-z]T|_[Tt])ime$"), // ends with the word "time"
            Pattern.compile("^.*([a-z]D|_[Dd])ate$"), // ends with the word "date"
            Pattern.compile("^.*([a-z]A|_[Aa])t$"), // ends with the word "at"
            Pattern.compile("^.*([a-z]O|_[Oo])n$") // ends with the word "on"
    );

    private final List<Pattern> patterns = new ArrayList<>(DEFAULT_PATTERNS);

    private ShouldHaveUsedTimestampValidator(Config config) {
        patterns.addAll(config.getAdditionalPatterns());
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeVisitor<List<ValidationEvent>> visitor = new ShapeVisitor.Default<List<ValidationEvent>>() {
            @Override
            protected List<ValidationEvent> getDefault(Shape shape) {
                if (shape.isStringShape() || shape instanceof NumberShape) {
                    return validateSimpleShape(shape);
                } else {
                    return Collections.emptyList();
                }
            }

            @Override
            public List<ValidationEvent> structureShape(StructureShape shape) {
                return validateStructure(shape, model);
            }

            @Override
            public List<ValidationEvent> unionShape(UnionShape shape) {
                return validateUnion(shape, model);
            }

            @Override
            public List<ValidationEvent> enumShape(EnumShape shape) {
                return Collections.emptyList();
            }
        };

        return model.shapes().flatMap(shape -> shape.accept(visitor).stream()).collect(Collectors.toList());
    }

    private List<ValidationEvent> validateStructure(
            StructureShape structure,
            Model model
    ) {
        return structure
                .getAllMembers()
                .entrySet()
                .stream()
                .flatMap(entry -> validateTargetShape(entry.getKey(), entry.getValue(), model))
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateUnion(
            UnionShape union,
            Model model
    ) {
        return union
                .getAllMembers()
                .entrySet()
                .stream()
                .flatMap(entry -> validateTargetShape(entry.getKey(), entry.getValue(), model))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateTargetShape(
            String name,
            MemberShape memberShape,
            Model model
    ) {
        return OptionalUtils.stream(model.getShape(memberShape.getTarget())
                .flatMap(targetShape -> validateName(name, targetShape, memberShape, patterns, model)));
    }

    private List<ValidationEvent> validateSimpleShape(
            Shape shape
    ) {
        String name = shape.getId().getName();

        return patterns
                .stream()
                .filter(pattern -> pattern.matcher(name).matches())
                .map(matcher -> buildEvent(shape, name, shape.getType()))
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> validateName(
            String name,
            Shape targetShape,
            Shape context,
            List<Pattern> patterns,
            Model model
    ) {
        ShapeType type = targetShape.getType();
        if (type == ShapeType.TIMESTAMP || type == ShapeType.ENUM) {
            return Optional.empty();
        } else if (type == ShapeType.STRUCTURE || type == ShapeType.LIST) {
            if (this.onlyContainsTimestamps(targetShape, model)) {
                return Optional.empty();
            }
        }
        return patterns
                .stream()
                .filter(pattern -> pattern.matcher(name).matches())
                .map(matcher -> buildEvent(context, name, type))
                .findAny();
    }

    private boolean onlyContainsTimestamps(Shape shape, Model model) {
        return shape.getAllMembers()
                .values()
                .stream()
                .map(memberShape -> model.getShape(memberShape.getTarget()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .allMatch(Shape::isTimestampShape);
    }

    private ValidationEvent buildEvent(Shape context, String name, ShapeType type) {
        return danger(context,
                context.isMemberShape()
                        ? String.format("Member `%s` is named like a timestamp but references a `%s` shape", name, type)
                        : String.format("Shape `%s` is named like a timestamp but is a `%s` shape.", name, type));
    }
}
