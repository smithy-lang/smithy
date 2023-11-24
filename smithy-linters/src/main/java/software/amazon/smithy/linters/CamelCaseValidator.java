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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Emits a validation event if shapes at a specific location do not match the
 * desired camel casing format.
 */
public final class CamelCaseValidator extends AbstractValidator {

    /**
     * CamelCase configuration settings.
     */
    public static final class Config {
        private MemberNameHandling memberNames = MemberNameHandling.AUTO;

        /**
         * Gets how member names are to be cased.
         *
         * <p>One of "upper", "lower", or "auto" (default).
         *
         * @return returns member name casing.
         */
        public MemberNameHandling getMemberNames() {
            return memberNames;
        }

        public void setMemberNames(MemberNameHandling memberNames) {
            this.memberNames = memberNames;
        }
    }

    public enum MemberNameHandling {
        /**
         * Member names are serialized using upper camel case (e.g., FooBar).
         */
        UPPER {
            Pattern getRegex() {
                return UPPER_CAMEL_CASE;
            }

            @Override
            public String toString() {
                return "upper";
            }
        },

        /**
         * Member names are serialized using lower camel case (e.g., fooBar).
         */
        LOWER {
            Pattern getRegex() {
                return LOWER_CAMEL_CASE;
            }

            @Override
            public String toString() {
                return "lower";
            }
        },

        /**
         * Member names are serialized using either all lower camel or all upper camel, but not a mixture.
         */
        AUTO {
            Pattern getRegex() {
                // No Pattern associated with auto setting
                return null;
            }

            @Override
            public String toString() {
                return "auto";
            }
        };

        abstract Pattern getRegex();
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(CamelCaseValidator.class, configuration -> {
                NodeMapper mapper = new NodeMapper();
                return new CamelCaseValidator(mapper.deserialize(configuration, Config.class));
            });
        }
    }

    private static final Pattern UPPER_CAMEL_CASE = Pattern.compile("^[A-Z]+[A-Za-z0-9]*$");
    private static final Pattern LOWER_CAMEL_CASE = Pattern.compile("^[a-z]+[A-Za-z0-9]*$");

    private final Config config;

    private CamelCaseValidator(Config config) {
        this.config = config;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        // Normal shapes are expected to be upper camel.
        model.shapes()
                .filter(FunctionalUtils.not(Shape::isMemberShape))
                .filter(shape -> !shape.hasTrait(TraitDefinition.class))
                .filter(shape -> !MemberNameHandling.UPPER.getRegex().matcher(shape.getId().getName()).find())
                .map(shape -> danger(shape, format(
                        "%s shape name, `%s`, is not %s camel case",
                        shape.getType(), shape.getId().getName(), MemberNameHandling.UPPER)))
                .forEach(events::add);

        // Trait shapes are expected to be lower camel.
        model.shapes()
                .filter(shape -> shape.hasTrait(TraitDefinition.class))
                .filter(shape -> !shape.hasTrait(AuthDefinitionTrait.class))
                .filter(shape -> !shape.hasTrait(ProtocolDefinitionTrait.class))
                .filter(shape -> !MemberNameHandling.LOWER.getRegex().matcher(shape.getId().getName()).find())
                .map(shape -> danger(shape, format(
                        "%s trait definition, `%s`, is not lower camel case",
                        shape.getType(), shape.getId().getName())))
                .forEach(events::add);

        // First validate each service's closure's member shape member names
        Set<MemberShape> seenShapes = new HashSet<>();
        for (ServiceShape serviceShape : model.getServiceShapes()) {
            Set<Shape> serviceClosure = new HashSet<>();
            Walker walker = new Walker(model);
            walker.iterateShapes(serviceShape).forEachRemaining(serviceClosure::add);
            List<MemberShape> memberShapes = serviceClosure.stream()
                    .filter(Shape::isMemberShape)
                    .map(shape -> (MemberShape) shape)
                    .collect(Collectors.toList());
            events.addAll(validateCamelCasing(model, memberShapes, serviceShape.getId().getName()));
            seenShapes.addAll(memberShapes);
        }

        // Next get all other member shapes (ex. trait shape members) and validate per namespace grouping
        Map<String, List<MemberShape>> memberShapesByNamespace = model.toSet(MemberShape.class).stream()
                .filter(memberShape -> !seenShapes.contains(memberShape))
                .collect(Collectors.groupingBy(
                        memberShape -> memberShape.getContainer().getNamespace()));

        for (Map.Entry<String, List<MemberShape>> memberShapeGrouping : memberShapesByNamespace.entrySet()) {
            events.addAll(validateCamelCasing(model, memberShapeGrouping.getValue(),
                    memberShapeGrouping.getKey() + " namespace"));
        }

        return events;
    }

    private List<ValidationEvent> validateCamelCasing(Model model, List<MemberShape> memberShapes, String scope) {
        int upperCamelMemberNamesCount = 0;
        int lowerCamelMemberNamesCount = 0;
        Set<MemberShape> nonUpperCamelMemberShapes = new HashSet<>();
        Set<MemberShape> nonLowerCamelMemberShapes = new HashSet<>();

        for (MemberShape memberShape : memberShapes) {
            // Exclude members of enums from CamelCase validation,
            // as they're intended to be CAPS_SNAKE.
            // Also exclude list and map members as their names are constant.
            Shape container = model.expectShape(memberShape.getContainer());
            if (!container.isEnumShape() && !container.isIntEnumShape()
                    && !container.isListShape() && !container.isMapShape()) {
                if (MemberNameHandling.UPPER.getRegex().matcher(memberShape.getMemberName()).find()) {
                    upperCamelMemberNamesCount++;
                } else {
                    nonUpperCamelMemberShapes.add(memberShape);
                }
                if (MemberNameHandling.LOWER.getRegex().matcher(memberShape.getMemberName()).find()) {
                    lowerCamelMemberNamesCount++;
                } else {
                    nonLowerCamelMemberShapes.add(memberShape);
                }
            }
        }

        // Member shapes are expected to be either upper or lower, depending on the config (and in AUTO mode, the model)
        Set<MemberShape> violatingMemberShapes = new HashSet<>();
        String memberNameHandling = config.getMemberNames().toString();
        if (MemberNameHandling.AUTO.equals(config.getMemberNames())) {
            if (upperCamelMemberNamesCount > lowerCamelMemberNamesCount) {
                violatingMemberShapes = nonUpperCamelMemberShapes;
                memberNameHandling = MemberNameHandling.UPPER.toString();
            } else {
                violatingMemberShapes = nonLowerCamelMemberShapes;
                memberNameHandling = MemberNameHandling.LOWER.toString();
            }
        } else if (MemberNameHandling.UPPER.equals(config.getMemberNames())) {
            violatingMemberShapes = nonUpperCamelMemberShapes;
        } else if (MemberNameHandling.LOWER.equals(config.getMemberNames())) {
            violatingMemberShapes = nonLowerCamelMemberShapes;
        }

        String finalMemberNameHandling = memberNameHandling;
        return violatingMemberShapes.stream()
                .map(shape -> danger(shape, format(
                        "Member shape member name, `%s`, is not %s camel case;"
                                + " members in the %s must all use %s camel case.",
                        shape.getMemberName(), finalMemberNameHandling, scope, finalMemberNameHandling)))
                .collect(Collectors.toList());
    }
}
