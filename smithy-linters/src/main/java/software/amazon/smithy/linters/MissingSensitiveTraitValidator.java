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

package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * <p>Validates that shapes and members that possibly contain sensitive data are marked with the sensitive trait.
 */
public final class MissingSensitiveTraitValidator extends AbstractValidator {
    static final Set<String> DEFAULT_SENSITIVE_TERMS = SetUtils.of(
            "account number",
            "bank",
            "billing address",
            "birth",
            "birth day",
            "citizenship",
            "credentials",
            "credit card",
            "csc",
            "driver license",
            "drivers license",
            "email",
            "ethnicity",
            "first name",
            "gender",
            "insurance",
            "ip address",
            "last name",
            "maiden name",
            "mailing address",
            "pass phrase",
            "pass word",
            "passport",
            "phone",
            "religion",
            "secret",
            "sexual orientation",
            "social security",
            "ssn",
            "tax payer",
            "telephone",
            "zip code"
    );

    private final WordBoundaryMatcher wordMatcher;

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(MissingSensitiveTraitValidator.class, node -> {
                NodeMapper mapper = new NodeMapper();
                return new MissingSensitiveTraitValidator(
                        mapper.deserialize(node, MissingSensitiveTraitValidator.Config.class));
            });
        }
    }

    /**
     * MissingSensitiveTrait configuration.
     */
    public static final class Config {
        private List<String> terms = ListUtils.of();
        private boolean excludeDefaults;

        public List<String> getTerms() {
            return terms;
        }

        public void setTerms(List<String> terms) {
            this.terms = terms;
        }

        public boolean getExcludeDefaults() {
            return excludeDefaults;
        }

        public void setExcludeDefaults(boolean excludeDefaults) {
            this.excludeDefaults = excludeDefaults;
        }
    }

    private MissingSensitiveTraitValidator(Config config) {
        wordMatcher = new WordBoundaryMatcher();
        if (config.getExcludeDefaults() && config.getTerms().isEmpty()) {
            //This configuration combination makes the validator a no-op.
            throw new IllegalArgumentException("Cannot set 'excludeDefaults' to true and leave "
                    + "'terms' unspecified.");
        }

        config.getTerms().forEach(wordMatcher::addSearch);

        if (!config.getExcludeDefaults()) {
            DEFAULT_SENSITIVE_TERMS.forEach(wordMatcher::addSearch);
        }
    }

    /**
     * Finds shapes without the sensitive trait that possibly contain sensitive data,
     * based on the shape/member name and the list of key words and phrases.
     *
     * @param model Model to validate.
     * @return list of violation events
     */
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> validationEvents = new ArrayList<>();
        validationEvents.addAll(scanShapes(model));
        return validationEvents;
    }

    private List<ValidationEvent> scanShapes(Model model) {
        List<ValidationEvent> validationEvents = new ArrayList<>();

        for (Shape shape : model.toSet()) {
            if (shape.isMemberShape()) {
                MemberShape memberShape = (MemberShape) shape;
                Shape containingShape = model.expectShape(memberShape.getContainer());
                Shape targetShape = model.expectShape(memberShape.getTarget());

                if (!containingShape.hasTrait(SensitiveTrait.class)
                        && !containingShape.isEnumShape()
                        && !targetShape.hasTrait(SensitiveTrait.class)) {
                    Optional<ValidationEvent> optionalValidationEvent =
                            detectSensitiveTerms(memberShape.getMemberName(), memberShape);
                    optionalValidationEvent.ifPresent(validationEvents::add);
                }
            } else if (!shape.isOperationShape()
                    && !shape.isServiceShape()
                    && !shape.isResourceShape()
                    && !shape.hasTrait(SensitiveTrait.class)) {
                Optional<ValidationEvent> optionalValidationEvent =
                        detectSensitiveTerms(shape.toShapeId().getName(), shape);
                optionalValidationEvent.ifPresent(validationEvents::add);
            }
        }

        return validationEvents;
    }

    private Optional<ValidationEvent> detectSensitiveTerms(String name, Shape shape) {
        Optional<String> matchedTerm = wordMatcher.getFirstMatch(name);

        if (matchedTerm.isPresent()) {
            String message = shape.isMemberShape()
                    ? String.format("This member possibly contains sensitive data but neither the enclosing nor target"
                    + " shape are marked with the sensitive trait (based on the presence of '%s')", matchedTerm.get())
                    : String.format("This shape possibly contains sensitive data but is not marked "
                    + "with the sensitive trait (based on the presence of '%s')", matchedTerm.get());
            return Optional.of(warning(shape, message));
        } else {
            return Optional.empty();
        }
    }
}
