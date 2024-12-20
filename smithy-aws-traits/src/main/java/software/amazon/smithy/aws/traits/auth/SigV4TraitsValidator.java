/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates AWS Service, SigV4, and SigV4A traits.
 */
@SmithyInternalApi
public final class SigV4TraitsValidator extends AbstractValidator {
    private static final ShapeId SERVICE_ARN_NAMESPACE = ServiceTrait.ID.withMember("arnNamespace");
    private static final ShapeId SIGV4_NAME = SigV4Trait.ID.withMember("name");
    private static final ShapeId SIGV4A_NAME = SigV4ATrait.ID.withMember("name");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapes()) {
            events.addAll(validateService(model, service));
        }
        return events;
    }

    /**
     * Validates Service and SigV4 traits.
     *
     * - service$arnNamespace, sigv4$name, and sigv4a$name SHOULD be equal. Otherwise, emits warnings.
     */
    private List<ValidationEvent> validateService(Model model, ServiceShape service) {
        List<ValidationEvent> events = new ArrayList<>();
        Optional<ServiceTrait> serviceTraitOptional = service.getTrait(ServiceTrait.class);
        Optional<SigV4Trait> sigv4TraitOptional = service.getTrait(SigV4Trait.class);
        Optional<SigV4ATrait> sigv4aTraitOptional = service.getTrait(SigV4ATrait.class);
        if (serviceTraitOptional.isPresent()) {
            String serviceArnNamespace = serviceTraitOptional.get().getArnNamespace();
            // Check service$arnNamespace with sigv4$name
            if (sigv4TraitOptional.isPresent()) {
                String sigv4Name = sigv4TraitOptional.get().getName();
                if (!serviceArnNamespace.equals(sigv4Name)) {
                    events.add(createValuesShouldMatchWarning(
                            service,
                            SERVICE_ARN_NAMESPACE,
                            serviceArnNamespace,
                            SIGV4_NAME,
                            sigv4Name));
                }
            }
            // Check service$arnNamespace with sigv4a$name
            if (sigv4aTraitOptional.isPresent()) {
                String sigv4aName = sigv4aTraitOptional.get().getName();
                if (!serviceArnNamespace.equals(sigv4aName)) {
                    events.add(createValuesShouldMatchWarning(
                            service,
                            SERVICE_ARN_NAMESPACE,
                            serviceArnNamespace,
                            SIGV4A_NAME,
                            sigv4aName));
                }
            }
        }
        // Check sigv4$name with sigv4a$name
        if (sigv4TraitOptional.isPresent() && sigv4aTraitOptional.isPresent()) {
            String sigv4Name = sigv4TraitOptional.get().getName();
            String sigv4aName = sigv4aTraitOptional.get().getName();
            if (!sigv4Name.equals(sigv4aName)) {
                events.add(createValuesShouldMatchWarning(
                        service,
                        SIGV4_NAME,
                        sigv4Name,
                        SIGV4A_NAME,
                        sigv4aName));
            }
        }
        return events;
    }

    private ValidationEvent createValuesShouldMatchWarning(
            ServiceShape service,
            ShapeId member1,
            String value1,
            ShapeId member2,
            String value2
    ) {
        return warning(service,
                String.format(
                        "Value for `%s` \"%s\" and value for `%s` \"%s\" SHOULD match.",
                        member1.toString(),
                        value1,
                        member2.toString(),
                        value2));
    }
}
