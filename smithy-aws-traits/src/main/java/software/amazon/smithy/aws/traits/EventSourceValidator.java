/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Validates that the aws.api#service/eventSource property matches
 * {@code aws.api#service/arnNamespace} + ".amazonaws.com" and does
 * not use incorrect formats.
 */
@SmithyInternalApi
public final class EventSourceValidator extends AbstractValidator {

    private static final Map<String, String> KNOWN_EXCEPTIONS = MapUtils.of(
            "cloudwatch.amazonaws.com",
            "monitoring.amazonaws.com");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(ServiceTrait.class)) {
            validateService(service, service.expectTrait(ServiceTrait.class)).ifPresent(events::add);
        }
        return events;
    }

    private Optional<ValidationEvent> validateService(ServiceShape service, ServiceTrait trait) {
        String message = null;
        String source = trait.getCloudTrailEventSource();
        String expectedEventSource = trait.getArnNamespace() + ".amazonaws.com";

        if (!Objects.equals(KNOWN_EXCEPTIONS.get(expectedEventSource), source)) {
            if (source.contains("REPLACE") || StringUtils.upperCase(source).equals(source)) {
                // TODO: ideally this can become a DANGER event in the future.
                message = "aws.api#service|cloudTrailEventSource must not use placeholders, but found: " + source;
            } else if (!source.equals(expectedEventSource)) {
                message = String.format("aws.api#service|cloudTrailEventSource does not match the expected value. "
                        + "Expected '%s', but found '%s'.", expectedEventSource, source);
            }
        }

        return message == null ? Optional.empty() : Optional.of(warning(service, trait, message));
    }
}
