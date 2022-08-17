/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.validators;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import software.amazon.smithy.rulesengine.reterminus.Endpoint;
import software.amazon.smithy.rulesengine.reterminus.lang.Identifier;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Literal;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Template;
import software.amazon.smithy.rulesengine.reterminus.visit.TraversingVisitor;

/**
 * Validate that URIs start with a scheme.
 */
public class ValidateUriScheme extends TraversingVisitor<ValidationError> {
    boolean checkingEndpoint = false;

    @Override
    public Stream<ValidationError> visitEndpoint(Endpoint endpoint) {
        checkingEndpoint = true;
        Stream<ValidationError> errors = endpoint.getUrl().accept(this);
        checkingEndpoint = false;
        return errors;
    }

    @Override
    public Stream<ValidationError> visitLiteral(Literal literal) {
        return literal.accept(new Literal.Vistor<Stream<ValidationError>>() {
            @Override
            public Stream<ValidationError> visitBool(boolean b) {
                return Stream.empty();
            }

            @Override
            public Stream<ValidationError> visitStr(Template value) {
                return validateTemplate(value);
            }

            @Override
            public Stream<ValidationError> visitObject(Map<Identifier, Literal> members) {
                return Stream.empty();
            }

            @Override
            public Stream<ValidationError> visitTuple(List<Literal> members) {
                return Stream.empty();
            }

            @Override
            public Stream<ValidationError> visitInt(int value) {
                return Stream.empty();
            }
        });
    }

    private Stream<ValidationError> validateTemplate(Template template) {
        if (checkingEndpoint) {
            Template.Part head = template.getParts().get(0);
            if (head instanceof Template.Literal) {
                String templateStart = ((Template.Literal) head).getValue();
                if (!(templateStart.startsWith("http://") || templateStart.startsWith("https://"))) {
                    return Stream.of(new ValidationError(
                            ValidationErrorType.INVALID_URI,
                            "URI should start with `http://` or `https://` but the URI started with " + templateStart,
                            template.getSourceLocation())
                    );
                }
            }
            /* Allow dynamic URIs for now—we should lint that at looks like a scheme at some point */
        }
        return Stream.empty();
    }
}
