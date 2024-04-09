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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Validates that `http` traits applied to operation shapes use the most
 * semantically appropriate HTTP method according to RFC 7231.
 */
public final class HttpMethodSemanticsValidator extends AbstractValidator {
    /**
     * Provides the configuration for each HTTP method name:
     *
     * <ul>
     *     <li>Key: (String) method</li>
     *     <li>(Boolean) isReadonly</li>
     *     <li>(Boolean) isIdempotent</li>
     *     <li>(Boolean) allowsRequestPayload</li>
     *     <li>(Boolean) warningMessageWhenModeled</li>
     * </ul>
     *
     * <p>Setting any of the Boolean properties to {@code null} means that
     * the validator will have no enforced semantic on a specific property.
     */
    private static final Map<String, HttpMethodSemantics> EXPECTED = MapUtils.of(
            "GET", new HttpMethodSemantics(true, false, false, null),
            "HEAD", new HttpMethodSemantics(true, false, false, null),
            "OPTIONS", new HttpMethodSemantics(true, false, true,
                    "OPTIONS requests are typically used as part of CORS and should not be modeled explicitly. They "
                    + "are an implementation detail that should not appear in generated client or server code. "
                    + "Instead, tooling should use the `cors` trait on a service to automatically configure CORS on "
                    + "clients and servers. It is the responsibility of service frameworks and API gateways to "
                    + "automatically manage OPTIONS requests. For example, OPTIONS requests are automatically "
                    + "created when using Smithy models with Amazon API Gateway."),
            "TRACE", new HttpMethodSemantics(true, false, false, null),
            "POST", new HttpMethodSemantics(false, null, true, null),
            "DELETE", new HttpMethodSemantics(false, true, false, null),
            "PUT", new HttpMethodSemantics(false, true, true, null),
            "PATCH", new HttpMethodSemantics(false, null, true, null));

    private static final String UNEXPECTED_PAYLOAD = "UnexpectedPayload";
    private static final String MISSING_READONLY_TRAIT = "MissingReadonlyTrait";
    private static final String MISSING_IDEMPOTENT_TRAIT = "MissingIdempotentTrait";
    private static final String UNNECESSARY_READONLY_TRAIT = "UnnecessaryReadonlyTrait";
    private static final String UNNECESSARY_IDEMPOTENT_TRAIT = "UnnecessaryIdempotentTrait";

    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(HttpTrait.class)) {
            return Collections.emptyList();
        }

        HttpBindingIndex bindingIndex = HttpBindingIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapesWithTrait(HttpTrait.class)) {
            events.addAll(validateOperation(bindingIndex, operation, operation.expectTrait(HttpTrait.class)));
        }

        return events;
    }

    private List<ValidationEvent> validateOperation(
            HttpBindingIndex bindingIndex,
            OperationShape shape,
            HttpTrait trait
    ) {
        String method = trait.getMethod().toUpperCase(Locale.US);

        // We don't have a standard method, so we can't validate it.
        if (!EXPECTED.containsKey(method)) {
            return ListUtils.of();
        }

        List<ValidationEvent> events = new ArrayList<>();

        // Emit a warning if the method is standard, but doesn't match standard casing.
        if (!EXPECTED.containsKey(trait.getMethod())) {
            events.add(warning(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but expected `%s`.",
                    trait.getMethod(), method)));
        }

        HttpMethodSemantics semantics = EXPECTED.get(method);

        if (semantics.warningWhenModeled != null) {
            events.add(warning(shape, trait, semantics.warningWhenModeled, method));
        }

        boolean isReadonly = shape.getTrait(ReadonlyTrait.class).isPresent();
        if (semantics.isReadonly != null && semantics.isReadonly != isReadonly) {
            events.add(warning(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but %s marked with the readonly trait",
                        method, isReadonly ? "is" : "is not"),
                    isReadonly ? UNNECESSARY_READONLY_TRAIT : MISSING_READONLY_TRAIT));
        }

        boolean isIdempotent = shape.getTrait(IdempotentTrait.class).isPresent();
        if (semantics.isIdempotent != null && semantics.isIdempotent != isIdempotent) {
            events.add(warning(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but %s marked with the idempotent trait",
                        method, isIdempotent ? "is" : "is not"),
                    isIdempotent ? UNNECESSARY_IDEMPOTENT_TRAIT : MISSING_IDEMPOTENT_TRAIT));
        }

        List<HttpBinding> payloadBindings = bindingIndex.getRequestBindings(shape, HttpBinding.Location.PAYLOAD);
        List<HttpBinding> documentBindings = bindingIndex.getRequestBindings(shape, HttpBinding.Location.DOCUMENT);
        if (semantics.allowsRequestPayload != null && !semantics.allowsRequestPayload) {
            if (!payloadBindings.isEmpty()) {
                events.add(danger(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but the `%s` member is sent as the "
                        + "payload of the request because it is marked with the `httpPayload` trait. Many HTTP "
                        + "clients do not support payloads with %1$s requests. Consider binding this member to "
                        + "other parts of the HTTP request such as a query string parameter using the `httpQuery` "
                        + "trait, a header using the `httpHeader` trait, or a path segment using the `httpLabel` "
                        + "trait.",
                        method, payloadBindings.get(0).getMemberName()),
                    UNEXPECTED_PAYLOAD)
                );
            } else if (!documentBindings.isEmpty()) {
                events.add(danger(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but the following members "
                        + "are sent as part of the payload of the request: %s. These members are sent as part "
                        + "of the payload because they are not explicitly configured to be sent in headers, in the "
                        + "query string, or in a URI segment. Many HTTP clients do not support payloads with %1$s "
                        + "requests. Consider binding these members to other parts of the HTTP request such as "
                        + "query string parameters using the `httpQuery` trait, headers using the `httpHeader` "
                        + "trait, or URI segments using the `httpLabel` trait.",
                        method, ValidationUtils.tickedList(documentBindings.stream().map(HttpBinding::getMemberName))),
                    UNEXPECTED_PAYLOAD)
                );
            }
        }

        return events;
    }

    private static final class HttpMethodSemantics {
        private final Boolean isReadonly;
        private final Boolean isIdempotent;
        private final Boolean allowsRequestPayload;
        private final String warningWhenModeled;

        private HttpMethodSemantics(
                Boolean isReadonly,
                Boolean isIdempotent,
                Boolean allowsRequestPayload,
                String warningWhenModeled
        ) {
            this.isReadonly = isReadonly;
            this.isIdempotent = isIdempotent;
            this.allowsRequestPayload = allowsRequestPayload;
            this.warningWhenModeled = warningWhenModeled;
        }
    }
}
