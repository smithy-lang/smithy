/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that SDK service IDs are correct and do not match any
 * prohibited patterns.
 *
 * <ul>
 *     <li>Must match the following regex: ^[a-zA-Z][a-zA-Z0-9]*( [a-zA-Z0-9]+)*$</li>
 *     <li>Must not contain "Amazon", "AWS", or "Aws"</li>
 *     <li>Must not case-insensitively end with "Service", "Client", or "API".</li>
 * </ul>
 */
@SmithyInternalApi
public final class SdkServiceIdValidator extends AbstractValidator {
    private static final Set<String> COMPANY_NAMES = SetUtils.of("AWS", "Aws", "Amazon");
    private static final Set<String> DISALLOWED_ENDINGS = SetUtils.of("service", "client", "api");
    private static final Pattern SERVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*( [a-zA-Z0-9]+)*$");

    /**
     * Service Id's that have already been shipped.
     *
     * No new serviceId's should be added to this list in the future.
     */
    private static final Set<String> PREEXISTING_SERVICE_IDS = SetUtils.of(
            "ACM PCA",
            "ApiGatewayManagementApi",
            "Config Service",
            "Cost and Usage Report Service",
            "Application Discovery Service",
            "Database Migration Service",
            "Directory Service",
            "Elasticsearch Service",
            "IoT 1Click Devices Service",
            "IoTAnalytics",
            "Lex Model Building Service",
            "Lex Runtime Service",
            "Marketplace Entitlement Service",
            "mq",
            "Resource Groups Tagging API");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(ServiceTrait.class)) {
            validateService(service, service.expectTrait(ServiceTrait.class)).ifPresent(events::add);
        }
        return events;
    }

    /**
     * Checks if the given value is a previously released but
     * invalid service ID.
     *
     * @param serviceId Service ID value to check.
     * @return Returns true if the service ID is approved but invalid.
     */
    public static boolean isPreviouslyReleasedInvalidServiceId(String serviceId) {
        return PREEXISTING_SERVICE_IDS.contains(serviceId);
    }

    /**
     * Validates a service ID value.
     *
     * @param serviceId Service ID to validate.
     * @throws IllegalArgumentException if the service ID is invalid.
     */
    public static void validateServiceId(String serviceId) {
        if (isPreviouslyReleasedInvalidServiceId(serviceId)) {
            return;
        }

        List<String> messages = new ArrayList<>();

        if (!validForPattern(serviceId)) {
            messages.add(String.format("Does not match the required pattern `%s`", SERVICE_ID_PATTERN.pattern()));
        }

        if (containsCompanyName(serviceId)) {
            messages.add(String.format(
                    "Must not contain any of the following company names: [%s]",
                    ValidationUtils.tickedList(COMPANY_NAMES)));
        }

        endsWithForbiddenWord(serviceId).ifPresent(suffix -> {
            messages.add(String.format("Must not case-insensitively end with `%s`", suffix));
        });

        if (serviceId.isEmpty() || serviceId.length() > 50) {
            messages.add("Must be between 1 and 50 characters long.");
        }

        if (!messages.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid SDK service ID value, `%s`: %s",
                    serviceId,
                    String.join(";", messages)));
        }
    }

    private Optional<ValidationEvent> validateService(ServiceShape service, ServiceTrait trait) {
        String value = trait.getSdkId();

        try {
            validateServiceId(value);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of(danger(service, trait, e.getMessage()));
        }
    }

    private static boolean containsCompanyName(String value) {
        return COMPANY_NAMES.stream().anyMatch(value::contains);
    }

    private static Optional<String> endsWithForbiddenWord(String value) {
        String lowercase = value.toLowerCase(Locale.US);

        return DISALLOWED_ENDINGS.stream()
                .filter(lowercase::endsWith)
                .findFirst();
    }

    private static boolean validForPattern(String value) {
        return SERVICE_ID_PATTERN.matcher(value).find();
    }
}
