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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Validates that `http` traits applied to operation shapes use the most
 * semantically appropriate HTTP method according to RFC 7231.
 */
public final class HttpMethodSemanticsValidator extends AbstractValidator {
    private static final Map<String, HttpMethodSemantics> EXPECTED = MapUtils.of(
            "GET", new HttpMethodSemantics(true, false, false),
            "HEAD", new HttpMethodSemantics(true, false, false),
            "OPTIONS", new HttpMethodSemantics(true, false, false),
            "TRACE", new HttpMethodSemantics(true, false, false),
            "POST", new HttpMethodSemantics(false, false, true),
            "DELETE", new HttpMethodSemantics(false, true, true),
            "PUT", new HttpMethodSemantics(false, true, true),
            "PATCH", new HttpMethodSemantics(false, false, true));

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex index = model.getShapeIndex();
        HttpBindingIndex bindingIndex = model.getKnowledge(HttpBindingIndex.class);
        return index.shapes(OperationShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, HttpTrait.class))
                .flatMap(pair -> validateOperation(bindingIndex, pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateOperation(
            HttpBindingIndex bindingIndex,
            OperationShape shape,
            HttpTrait trait
    ) {
        String method = trait.getMethod().toUpperCase(Locale.US);
        List<ValidationEvent> events = new ArrayList<>();

        if (!EXPECTED.containsKey(method)) {
            return events;
        }

        HttpMethodSemantics semantics = EXPECTED.get(method);
        boolean isReadonly = shape.getTrait(ReadonlyTrait.class).isPresent();
        if (isReadonly != semantics.isReadonly) {
            events.add(warning(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but %s marked with the readonly trait",
                    method, isReadonly ? "is" : "is not")));
        }

        boolean isIdempotent = shape.getTrait(IdempotentTrait.class).isPresent();
        if (isIdempotent != semantics.isIdempotent) {
            events.add(warning(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but %s marked with the idempotent trait",
                    method, isIdempotent ? "is" : "is not")));
        }

        List<HttpBinding> payloadBindings = bindingIndex.getRequestBindings(shape, HttpBinding.Location.PAYLOAD);
        List<HttpBinding> documentBindings = bindingIndex.getRequestBindings(shape, HttpBinding.Location.DOCUMENT);
        if (!semantics.allowsRequestPayload && (!payloadBindings.isEmpty() || !documentBindings.isEmpty())) {
            // Detect location and combine to one list for messages
            String document = payloadBindings.isEmpty() ? "document" : "payload";
            payloadBindings.addAll(documentBindings);
            events.add(danger(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but "
                    + "has the following members bound to the %s: %s", method, document,
                    ValidationUtils.tickedList(payloadBindings.stream().map(HttpBinding::getMemberName)))));
        }

        return events;
    }

    private static final class HttpMethodSemantics {
        private final boolean isReadonly;
        private final boolean isIdempotent;
        private final boolean allowsRequestPayload;

        private HttpMethodSemantics(boolean isReadonly, boolean isIdempotent, boolean allowsRequestPayload) {
            this.isReadonly = isReadonly;
            this.isIdempotent = isIdempotent;
            this.allowsRequestPayload = allowsRequestPayload;
        }
    }
}
