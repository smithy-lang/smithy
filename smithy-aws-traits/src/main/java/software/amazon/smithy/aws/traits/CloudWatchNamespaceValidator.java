/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that CloudWatch metric namespaces follow conventions.
 */
public final class CloudWatchNamespaceValidator extends AbstractValidator {
    private static final Pattern PASCAL_CASE_PATTERN = Pattern.compile("^[A-Z][a-zA-Z0-9]*(?:[A-Z][a-zA-Z0-9]*)*$");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (ServiceShape service : model.getServiceShapesWithTrait(ServiceTrait.class)) {
            ServiceTrait trait = service.expectTrait(ServiceTrait.class);
            Optional<String> cloudWatchNamespaceOpt = trait.getCloudWatchNamespace();
            if (cloudWatchNamespaceOpt.isPresent()) {
                String cloudWatchNamespace = cloudWatchNamespaceOpt.get();
                boolean startsWithAws = cloudWatchNamespace.startsWith("AWS/");

                String prefixlessNamespace = cloudWatchNamespace.replace("AWS/", "");
                boolean isServicePascalCase = PASCAL_CASE_PATTERN.matcher(prefixlessNamespace).matches();

                if (!startsWithAws || !isServicePascalCase) {
                    String message = "The service's CloudWatch metric namespace `%s` does not follow convention.";
                    if (!startsWithAws) {
                        message += " The namespace should start with the `AWS/` prefix.";
                    }
                    if (!isServicePascalCase) {
                        message += " The namespace's service component `" + prefixlessNamespace
                                + "` should be PascalCase.";
                    }

                    events.add(danger(service, trait, format(message, cloudWatchNamespace)));
                }
            }
        }

        return events;
    }
}
