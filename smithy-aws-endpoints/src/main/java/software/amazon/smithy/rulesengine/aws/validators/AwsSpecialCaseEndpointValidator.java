/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.validators;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.aws.traits.PartitionEndpointSpecialCase;
import software.amazon.smithy.rulesengine.aws.traits.PartitionSpecialCase;
import software.amazon.smithy.rulesengine.aws.traits.RegionSpecialCase;
import software.amazon.smithy.rulesengine.aws.traits.StandardPartitionalEndpointsTrait;
import software.amazon.smithy.rulesengine.aws.traits.StandardRegionalEndpointsTrait;
import software.amazon.smithy.utils.SetUtils;

/**
 * Validate special case endpoints from endpoint traits that are applied on a service.
 */
public final class AwsSpecialCaseEndpointValidator extends AbstractValidator {

    private static final Set<String> SUPPORTED_PATTERNS = SetUtils.of(
            "{region}", "{service}", "{dnsSuffix}", "{dualStackDnsSuffix}"
    );

    private static final Pattern PATTERN = Pattern.compile("\\{[^\\}]*\\}");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(StandardRegionalEndpointsTrait.class)) {
            events.addAll(validateRegionalEndpointPatterns(serviceShape,
                    serviceShape.expectTrait(StandardRegionalEndpointsTrait.class)));
        }
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(StandardPartitionalEndpointsTrait.class)) {
            events.addAll(validatePartitionalEndpointPatterns(serviceShape,
                    serviceShape.expectTrait(StandardPartitionalEndpointsTrait.class)));
        }
        return events;
    }

    private List<ValidationEvent> validateRegionalEndpointPatterns(
            ServiceShape serviceShape, StandardRegionalEndpointsTrait regionalEndpoints) {
        List<ValidationEvent> events = new ArrayList<>();

        for (List<RegionSpecialCase> specialCases : regionalEndpoints.getRegionSpecialCases().values()) {
            for (RegionSpecialCase specialCase : specialCases) {
                events.addAll(validateEndpointPatterns(
                        serviceShape, regionalEndpoints, specialCase.getEndpoint()));
            }
        }

        for (List<PartitionSpecialCase> specialCases : regionalEndpoints.getPartitionSpecialCases().values()) {
            for (PartitionSpecialCase specialCase : specialCases) {
                events.addAll(validateEndpointPatterns(
                        serviceShape, regionalEndpoints, specialCase.getEndpoint()));
            }
        }

        return events;
    }

    private List<ValidationEvent> validatePartitionalEndpointPatterns(
            ServiceShape serviceShape, StandardPartitionalEndpointsTrait partitionalEndpoints) {

        List<ValidationEvent> events = new ArrayList<>();

        for (List<PartitionEndpointSpecialCase> specialCases
                : partitionalEndpoints.getPartitionEndpointSpecialCases().values()) {
            for (PartitionEndpointSpecialCase specialCase : specialCases) {
                events.addAll(validateEndpointPatterns(
                        serviceShape, partitionalEndpoints, specialCase.getEndpoint()));
            }
        }

        return events;
    }

    private List<ValidationEvent> validateEndpointPatterns(
            ServiceShape serviceShape, FromSourceLocation location, String endpoint) {
        List<ValidationEvent> events = new ArrayList<>();

        Matcher m = PATTERN.matcher(endpoint);
        List<String> unsupportedPatterns = new ArrayList<>();
        while (m.find()) {
            if (!SUPPORTED_PATTERNS.contains(m.group())) {
                unsupportedPatterns.add(m.group());
            }
        }

        if (!unsupportedPatterns.isEmpty()) {
            events.add(danger(
                    serviceShape, location,
                    String.format("Endpoint `%s` contains unsupported patterns: %s",
                            endpoint, String.join(", ", unsupportedPatterns)),
                    "UnsupportedEndpointPattern"));
        }

        if (!(endpoint.startsWith("http://") || endpoint.startsWith("https://"))) {
            events.add(danger(
                    serviceShape, location,
                    String.format("Endpoint `%s` should start with scheme `http://` or `https://`",
                            endpoint),
                    "InvalidEndpointPatternScheme"));
        }

        if (!isValidURL(endpoint)) {
            events.add(error(
                    serviceShape, location,
                    String.format("Endpoint `%s` should be a valid URL.",
                            endpoint),
                    "InvalidEndpointPatternUrl"));
        }

        return events;
    }

    private boolean isValidURL(String endpointPattern) {
        String url = endpointPattern
                .replace("{", "")
                .replace("}", "");
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException e) {
            return false;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
