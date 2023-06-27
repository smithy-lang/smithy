/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.validators;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;

/**
 * Validates an authentication scheme after passing a predicate check.
 */
public interface AuthSchemeValidator extends Predicate<String> {
    /**
     * Validates that the provided {@code authScheme} matches required modeling behavior,
     * emitting events for any failures.
     *
     * @param authScheme an authorization scheme parameter set.
     * @param sourceLocation the location of the authorization scheme to generate events from.
     * @param emitter a function to emit {@link ValidationEvent}s for validation failures.
     * @return a list of validation events.
     */
    List<ValidationEvent> validateScheme(
            Map<Identifier, Literal> authScheme,
            SourceLocation sourceLocation,
            BiFunction<FromSourceLocation, String, ValidationEvent> emitter);
}
