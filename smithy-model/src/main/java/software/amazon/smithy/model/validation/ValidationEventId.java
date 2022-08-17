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

package software.amazon.smithy.model.validation;

import java.util.Optional;
import software.amazon.smithy.model.loader.ParserUtils;
import software.amazon.smithy.model.loader.ValidatorLoadException;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Custom Identifier for Validators.
 *
 * <p>A validation event ID is used to identify specific instances of a
 * validator so that suppressions can be applied to those instances.
 */
public final class ValidationEventId {

    private static final String VALIDATION_EVENT_ID_REGEX_PATTERN =
            "^_*[A-Za-z][A-Za-z0-9_]*(\\._*[A-Za-z][A-Za-z0-9_]*)*$";

    private ValidationEventId() {}

    /**
     * Checks if the given string is a valid validation event ID.
     *
     * @param validationEventId Validation Event ID value to check.
     * @return Returns true if this is a valid validationEventId.
     */
    public static boolean isValidValidationEventId(CharSequence validationEventId) {
        if (validationEventId == null) {
            return false;
        }

        int length = validationEventId.length();
        if (length == 0) {
            return false;
        }

        int position = 0;
        while (true) {
            position = parseIdentifier(validationEventId, position);
            if (position == -1) { // Bad: did not parse a valid identifier.
                return false;
            } else if (position == length) { // Good: parsed and reached the end.
                return true;
            } else if (validationEventId.charAt(position) != '.') { // Bad: invalid character.
                return false;
            } else if (++position >= length) { // Bad: trailing '.'
                return false;
            } // continue parsing after '.', expecting an identifier.
        }
    }

    private static int parseIdentifier(CharSequence identifier, int offset) {
        if (identifier == null || identifier.length() <= offset) {
            return -1;
        }

        // Parse the required IdentifierStart production.
        char startingChar = identifier.charAt(offset);
        if (startingChar == '_') {
            while (offset < identifier.length() && identifier.charAt(offset) == '_') {
                offset++;
            }
            if (offset >= identifier.length()) {
                return -1;
            }
            if (!ParserUtils.isAlphabetic(identifier.charAt(offset))) {
                return -1;
            }
            offset++;
        } else if (!ParserUtils.isAlphabetic(startingChar)) {
            return -1;
        }

        // Parse the optional IdentifierChars production.
        while (offset < identifier.length()) {
            if (!ParserUtils.isValidIdentifierCharacter(identifier.charAt(offset))) {
                // Return the position of the character that stops the identifier.
                // The marker is needed for isValidValidationEventId to find '.'.
                return offset;
            }
            offset++;
        }

        return offset;
    }

    public static void validateValidationEventId(String validationEventId, ObjectNode node)
            throws ValidatorLoadException {
        Optional.ofNullable(validationEventId)
                .filter(ValidationEventId::isValidValidationEventId)
                .orElseThrow(() -> new ValidatorLoadException(String.format(
                        "Validation Event ID `%s` must match regular expression: %s",
                        validationEventId, VALIDATION_EVENT_ID_REGEX_PATTERN), node.getSourceLocation()));
    }
}
