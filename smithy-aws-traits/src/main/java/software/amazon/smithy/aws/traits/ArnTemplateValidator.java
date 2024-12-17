/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static java.util.stream.Collectors.toList;
import static software.amazon.smithy.model.validation.ValidationUtils.tickedList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Ensures that all arn traits for a service are valid and that their templates
 * only reference valid resource identifiers.
 */
@SmithyInternalApi
public final class ArnTemplateValidator extends AbstractValidator {
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("^[!|+]?[a-zA-Z_][a-zA-Z0-9_]*$");

    @Override
    public List<ValidationEvent> validate(Model model) {
        ArnIndex arnIndex = ArnIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(ServiceTrait.class)) {
            events.addAll(validateService(model, arnIndex, service));
        }
        return events;
    }

    private List<ValidationEvent> validateService(Model model, ArnIndex arnIndex, ServiceShape service) {
        // Make sure each ARN template contains relevant identifiers.
        List<ValidationEvent> events = new ArrayList<>();
        for (Map.Entry<ShapeId, ArnTrait> entry : arnIndex.getServiceResourceArns(service.getId()).entrySet()) {
            model.getShape(entry.getKey()).flatMap(Shape::asResourceShape).ifPresent(resource -> {
                events.addAll(validateResourceArn(resource, entry.getValue()));
            });
        }
        return events;
    }

    private List<ValidationEvent> validateResourceArn(ResourceShape resource, ArnTrait template) {
        // Fail early on syntax error, otherwise, validate that the
        // template correspond to identifiers.
        return syntax(resource, template).map(Collections::singletonList).orElseGet(() -> {
            List<ValidationEvent> events = new ArrayList<>();
            enough(resource.getIdentifiers().keySet(), resource, template).ifPresent(events::add);
            tooMuch(resource.getIdentifiers().keySet(), resource, template).ifPresent(events::add);
            return events;
        });
    }

    // Validates the syntax of each template.
    private Optional<ValidationEvent> syntax(Shape shape, ArnTrait trait) {
        List<String> invalid = trait.getLabels()
                .stream()
                .filter(expr -> !EXPRESSION_PATTERN.matcher(expr).find())
                .collect(toList());

        if (invalid.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(error(shape,
                trait,
                String.format(
                        "aws.api#arn trait contains invalid template labels: %s. Template labels must match the "
                                + "following regular expression: %s",
                        tickedList(invalid),
                        EXPRESSION_PATTERN.pattern())));
    }

    // Ensures that a template does not contain extraneous resource identifiers.
    private Optional<ValidationEvent> tooMuch(Collection<String> names, Shape shape, ArnTrait trait) {
        Set<String> templateCheck = new HashSet<>(trait.getLabels());
        templateCheck.removeAll(names);
        if (!templateCheck.isEmpty()) {
            return Optional.of(error(shape,
                    trait,
                    String.format(
                            "Invalid aws.api#arn trait resource, `%s`. Found template labels in the trait "
                                    + "that are not the names of the identifiers of the resource: %s. Extraneous identifiers: [%s]",
                            trait.getTemplate(),
                            names,
                            tickedList(templateCheck))));
        }
        return Optional.empty();
    }

    // Ensures that a template references all resource identifiers.
    private Optional<ValidationEvent> enough(Collection<String> names, Shape shape, ArnTrait trait) {
        Set<String> identifierVars = new HashSet<>(names);
        identifierVars.removeAll(trait.getLabels());
        if (!identifierVars.isEmpty()) {
            return Optional.of(error(shape,
                    trait,
                    String.format(
                            "Invalid aws.api#arn trait resource, `%s`. The following resource identifier names "
                                    + "were missing from the `arn` template: %s",
                            trait.getTemplate(),
                            tickedList(identifierVars))));
        }
        return Optional.empty();
    }
}
