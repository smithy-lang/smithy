/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import static java.lang.String.format;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

final class LoaderUtils {

    static final String BAD_DOCUMENTATION_COMMENT = "Model.BadDocumentationComment";

    private LoaderUtils() {}

    /**
     * Checks if additional properties are in an object, and if so, emits a warning event.
     *
     * @param node Node to check.
     * @param shape Shape to associate with the error.
     * @param properties Properties to allow.
     * @return Returns an optionally created event.
     */
    static Optional<ValidationEvent> checkForAdditionalProperties(
            ObjectNode node,
            ShapeId shape, Collection<String> properties
    ) {
        try {
            node.expectNoAdditionalProperties(properties);
            return Optional.empty();
        } catch (ExpectationNotMetException e) {
            ValidationEvent event = ValidationEvent.fromSourceException(e)
                    .toBuilder()
                    .shapeId(shape)
                    .severity(Severity.WARNING)
                    .build();
            return Optional.of(event);
        }
    }

    /**
     * Create a {@link ValidationEvent} for a shape conflict.
     *
     * @param id Shape ID in conflict.
     * @param a The first location of this shape.
     * @param b The second location of this shape.
     * @param message Message to append.
     * @return Returns the created validation event.
     */
    static ValidationEvent onShapeConflict(ShapeId id, SourceLocation a, SourceLocation b, String message) {
        String formatted = String.format("Conflicting shape definition for `%s` found at `%s` and `%s`", id, a, b);
        if (message != null) {
            formatted += ". " + message;
        }
        return ValidationEvent.builder()
                .id(Validator.MODEL_ERROR)
                .severity(Severity.ERROR)
                .sourceLocation(b)
                .shapeId(id)
                .message(formatted)
                .build();
    }

    /**
     * Checks if the given values are defined at the same source location,
     * and the source location is not {@link SourceLocation#NONE}.
     *
     * @param a First value to check.
     * @param b Second value to check.
     * @return Returns true if they are the same.
     */
    static boolean isSameLocation(FromSourceLocation a, FromSourceLocation b) {
        SourceLocation sa = a.getSourceLocation();
        SourceLocation sb = b.getSourceLocation();
        return sa != SourceLocation.NONE && sa.equals(sb);
    }

    /**
     * Checks if a list of validation events contains an ERROR severity.
     *
     * @param events Events to check.
     * @return Returns true if an ERROR event is present.
     */
    static boolean containsErrorEvents(List<ValidationEvent> events) {
        for (ValidationEvent event : events) {
            if (event.getSeverity() == Severity.ERROR) {
                return true;
            }
        }
        return false;
    }

    static ValidationEvent emitBadDocComment(SourceLocation location, String comments) {
        String message = "Found documentation comments ('///') attached to nothing. Documentation comments must "
                         + "appear on their own lines, directly before shapes and members, and before any traits.";
        if (comments != null) {
            message += " The invalid comments were: " + comments;
        }
        return ValidationEvent.builder()
                .id(BAD_DOCUMENTATION_COMMENT)
                .severity(Severity.WARNING)
                .message(message)
                .sourceLocation(location)
                .build();
    }

    static String idlExpectMessage(IdlTokenizer tokenizer, IdlToken... tokens) {
        StringBuilder result = new StringBuilder();
        IdlToken current = tokenizer.getCurrentToken();
        if (current == IdlToken.ERROR) {
            result.append(tokenizer.getCurrentTokenError());
        } else if (tokens.length == 1) {
            result.append("Expected ")
                    .append(tokens[0].getDebug())
                    .append(" but found ")
                    .append(current.getDebug(tokenizer.getCurrentTokenLexeme()));
        } else {
            result.append("Expected one of ");
            for (IdlToken token : tokens) {
                result.append(token.getDebug()).append(", ");
            }
            result.delete(result.length() - 2, result.length());
            result.append("; but found ").append(current.getDebug(tokenizer.getCurrentTokenLexeme()));
        }
        return result.toString();
    }

    static ModelSyntaxException idlSyntaxError(String message, SourceLocation location) {
        return idlSyntaxError(null, message, location);
    }

    static ModelSyntaxException idlSyntaxError(ShapeId shape, String message, SourceLocation location) {
        return ModelSyntaxException.builder()
                .message(format("Syntax error at line %d, column %d: %s",
                                location.getLine(), location.getColumn(), message))
                .sourceLocation(location)
                .shapeId(shape)
                .build();
    }
}
