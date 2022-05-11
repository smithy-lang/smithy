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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.MemberShape;
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
    private static final Set<String> EXCLUDED_NAMESPACES = new HashSet<>(Arrays.asList(
            Prelude.NAMESPACE,
            "aws.apigateway",
            "aws.cloudformation",
            "aws.iam",
            "aws.api",
            "aws.auth",
            "aws.customizations",
            "aws.protocols",
            "smithy.test"));

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
                return "consistent";
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
        int upperCamelMemberNamesCount = 0;
        int lowerCamelMemberNamesCount = 0;
        Set<MemberShape> nonUpperCamelMemberShapes = new HashSet<>();
        Set<MemberShape> nonLowerCamelMemberShapes = new HashSet<>();

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

        Stream<MemberShape> memberShapesToValidate = model.toSet(MemberShape.class).stream()
                .filter(memberShape -> !EXCLUDED_NAMESPACES.contains(memberShape.getContainer().getNamespace()));
        for (MemberShape memberShape : memberShapesToValidate.collect(Collectors.toList())) {
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

        // Member shapes are expected to be either upper or lower, depending on the config (and in AUTO mode, the model)
        Set<MemberShape> violatingMemberShapes = new HashSet<>();
        if (MemberNameHandling.AUTO.equals(config.getMemberNames())) {
            violatingMemberShapes = upperCamelMemberNamesCount > lowerCamelMemberNamesCount
                    ? nonUpperCamelMemberShapes : nonLowerCamelMemberShapes;
        } else if (MemberNameHandling.UPPER.equals(config.getMemberNames())) {
            violatingMemberShapes = nonUpperCamelMemberShapes;
        } else if (MemberNameHandling.LOWER.equals(config.getMemberNames())) {
            violatingMemberShapes = nonLowerCamelMemberShapes;
        }

        violatingMemberShapes.stream()
                .map(shape -> danger(shape, format(
                        "Member shape member name, `%s`, is not %s camel case. "
                                + "(Member names must all use the same type of camel case)",
                        shape.getMemberName(), config.getMemberNames())))
                .forEach(events::add);

        return events;
    }
}
