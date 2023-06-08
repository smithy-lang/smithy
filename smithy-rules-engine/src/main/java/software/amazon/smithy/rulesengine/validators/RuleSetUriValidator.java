/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.TraversingVisitor;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.LiteralVisitor;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Validate that URIs start with a scheme.
 */
@SmithyUnstableApi
public final class RuleSetUriValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            events.addAll(new UriSchemeVisitor(serviceShape)
                    .visitRuleset(serviceShape.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet())
                    .collect(Collectors.toList()));
        }
        return events;
    }

    private final class UriSchemeVisitor extends TraversingVisitor<ValidationEvent> {
        private final ServiceShape serviceShape;
        private boolean checkingEndpoint = false;

        UriSchemeVisitor(ServiceShape serviceShape) {
            this.serviceShape = serviceShape;
        }

        @Override
        public Stream<ValidationEvent> visitEndpoint(Endpoint endpoint) {
            checkingEndpoint = true;
            Stream<ValidationEvent> errors = endpoint.getUrl().accept(this);
            checkingEndpoint = false;
            return errors;
        }

        @Override
        public Stream<ValidationEvent> visitLiteral(Literal literal) {
            return literal.accept(new LiteralVisitor<Stream<ValidationEvent>>() {
                @Override
                public Stream<ValidationEvent> visitBoolean(boolean b) {
                    return Stream.empty();
                }

                @Override
                public Stream<ValidationEvent> visitString(Template value) {
                    return OptionalUtils.stream(validateTemplate(value));
                }

                @Override
                public Stream<ValidationEvent> visitRecord(Map<Identifier, Literal> members) {
                    return Stream.empty();
                }

                @Override
                public Stream<ValidationEvent> visitTuple(List<Literal> members) {
                    return Stream.empty();
                }

                @Override
                public Stream<ValidationEvent> visitInteger(int value) {
                    return Stream.empty();
                }
            });
        }

        private Optional<ValidationEvent> validateTemplate(Template template) {
            if (checkingEndpoint) {
                Template.Part part = template.getParts().get(0);
                if (part instanceof Template.Literal) {
                    String scheme = ((Template.Literal) part).getValue();
                    if (!(scheme.startsWith("http://") || scheme.startsWith("https://"))) {
                        return Optional.of(error(serviceShape, template,
                                "URI should start with `http://` or `https://` but the URI started with "
                                        + scheme));
                    }
                }
                // Allow dynamic URIs for now — we should lint that at looks like a scheme at some point
            }
            return Optional.empty();
        }
    }
}
