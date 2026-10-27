/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that CloudWatch metric namespaces follow conventions.
 */
public final class CloudWatchNamespaceValidator extends AbstractValidator {
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
                boolean isServicePascalCase = isServicePascalCase(prefixlessNamespace);

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

    /**
     * Returns true if the namespace is PascalCase. This method unrolls the
     * "^[A-Z][a-zA-Z0-9]*$" regex.
     */
    static boolean isServicePascalCase(String namespace) {
        int length = namespace.length();
        if (length == 0) {
            return false;
        }
        char ch = namespace.charAt(0);
        if (!isUpperAsciiLetter(ch)) {
            return false;
        }
        for (int i = 1; i < length; i++) {
            ch = namespace.charAt(i);
            if (isAsciiLetterOrDigit(ch)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Returns true if the given char is an upper case ASCII letter.
     */
    static boolean isUpperAsciiLetter(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    /**
     * Returns true if the given char is an ASCII letter or a digit.
     */
    static boolean isAsciiLetterOrDigit(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9');
    }
}
