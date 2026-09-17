/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static java.lang.String.format;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

final class LoaderUtils {

    static final String PREFIX = "_Synthetic";
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
            ShapeId shape,
            Collection<String> properties
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

    static void loadServiceRenameIntoBuilder(ServiceShape.Builder builder, ObjectNode node) {
        node.getObjectMember("rename").ifPresent(rename -> {
            for (Map.Entry<StringNode, Node> entry : rename.getMembers().entrySet()) {
                ShapeId fromId = entry.getKey().expectShapeId();
                String toName = entry.getValue().expectStringNode().getValue();
                builder.putRename(fromId, toName);
            }
        });
    }

    static ModelSyntaxException idlSyntaxError(ShapeId shape, String message, SourceLocation location) {
        return ModelSyntaxException.builder()
                .message(format("Syntax error at line %d, column %d: %s",
                        location.getLine(),
                        location.getColumn(),
                        message))
                .sourceLocation(location)
                .shapeId(shape)
                .build();
    }

    /**
     * Generates a synthetic name for an inline list shape.
     *
     * <p>The name is derived from the fully-resolved element target (see
     * {@link #syntheticToken(ShapeId, String)}). It is deterministic and order-independent.
     * It is injective for the common cases; a small set of pathological targets can collide,
     * which the loader detects and reports as an error rather than silently reusing a shape.
     *
     * @param containingNamespace Namespace of the structure that declares the inline collection.
     * @param memberTarget The fully-resolved element target.
     * @return The synthetic shape name (without namespace).
     */
    static String listName(String containingNamespace, ShapeId memberTarget) {
        return PREFIX + "ListOf" + syntheticToken(memberTarget, containingNamespace);
    }

    /**
     * Generates a synthetic name for an inline map shape.
     *
     * <p>The key and value tokens are separated by {@code _To_}. As with {@link #listName},
     * the encoding is deterministic; rare collisions are detected and reported by the loader.
     *
     * @param containingNamespace Namespace of the structure that declares the inline collection.
     * @param keyTarget The fully-resolved key target.
     * @param valueTarget The fully-resolved value target.
     * @return The synthetic shape name (without namespace).
     */
    static String mapName(String containingNamespace, ShapeId keyTarget, ShapeId valueTarget) {
        return PREFIX + "MapOf"
                + syntheticToken(keyTarget, containingNamespace)
                + "_To_"
                + syntheticToken(valueTarget, containingNamespace);
    }

    /**
     * Encodes a resolved target shape ID into an identifier-safe token.
     *
     * <p>The encoding is decided purely from the resolved target's namespace and simple name
     * (independent of what else exists in the model or prelude, so a name does not change when,
     * for example, a shape is added to the prelude in a later version):
     *
     * <ul>
     *   <li>Same namespace and a synthetic target (name starts with {@code _Synthetic}):
     *       {@code _Name}. Nested inline collections hit this case; the short form is safe
     *       because the prelude never defines shapes whose names start with {@code _}.</li>
     *   <li>Other name starting with {@code _}: the flattened namespace form
     *       {@code _seg1_seg2_Name}. This keeps an underscore-prefixed user shape from fusing
     *       with the prelude ({@code __}) or same-namespace ({@code _}) markers.</li>
     *   <li>Prelude target ({@code smithy.api}): {@code __Name}.</li>
     *   <li>Same namespace as the declaring structure: {@code _Name}.</li>
     *   <li>Any other namespace: {@code _seg1_seg2_Name}.</li>
     * </ul>
     *
     * <p>The encoding is deterministic and injective for all but pathological namespace/underscore
     * constructions (e.g. {@code com.amazon#String} versus {@code com#amazon_String}). Those are
     * detected by the loader and reported as an error rather than silently reused.
     */
    private static String syntheticToken(ShapeId target, String containingNamespace) {
        boolean sameNamespace = target.getNamespace().equals(containingNamespace);
        if (sameNamespace && target.getName().startsWith(PREFIX)) {
            return "_" + target.getName();
        }
        if (!target.getName().startsWith("_")) {
            if (target.getNamespace().equals(Prelude.NAMESPACE)) {
                return "__" + target.getName();
            }
            if (sameNamespace) {
                return "_" + target.getName();
            }
        }
        return "_" + target.getNamespace().replace('.', '_') + "_" + target.getName();
    }
}
