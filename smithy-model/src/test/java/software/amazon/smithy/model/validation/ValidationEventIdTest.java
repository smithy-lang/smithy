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

import java.util.Collections;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.ValidatorLoadException;
import software.amazon.smithy.model.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationEventIdTest {

    private static final ObjectNode TEST_OBJECT_NODE =
            new ObjectNode(Collections.emptyMap(), SourceLocation.none());

    @Test
    public void checksIfValidValidatorId() {
        assertFalse(ValidationEventId.isValidValidationEventId("foo.bar."));
        assertFalse(ValidationEventId.isValidValidationEventId(".foo.bar"));
        assertFalse(ValidationEventId.isValidValidationEventId("foo.bar#baz"));
        assertFalse(ValidationEventId.isValidValidationEventId("1foo.bar"));
        assertFalse(ValidationEventId.isValidValidationEventId("foo.1bar"));
        assertFalse(ValidationEventId.isValidValidationEventId("foo.bar.1"));
        assertFalse(ValidationEventId.isValidValidationEventId("foo.bar.*"));
        assertFalse(ValidationEventId.isValidValidationEventId("foo.bar.."));
        assertFalse(ValidationEventId.isValidValidationEventId(""));
        assertFalse(ValidationEventId.isValidValidationEventId("___"));
        assertFalse(ValidationEventId.isValidValidationEventId("a.___"));
        assertFalse(ValidationEventId.isValidValidationEventId("___1"));
        assertFalse(ValidationEventId.isValidValidationEventId("1"));
        assertFalse(ValidationEventId.isValidValidationEventId("a.___.b"));
        assertFalse(ValidationEventId.isValidValidationEventId("_._._"));

        assertTrue(ValidationEventId.isValidValidationEventId("foo"));
        assertTrue(ValidationEventId.isValidValidationEventId("Foo.bar"));
        assertTrue(ValidationEventId.isValidValidationEventId("foo._bar"));
        assertTrue(ValidationEventId.isValidValidationEventId("foo.bar"));
        assertTrue(ValidationEventId.isValidValidationEventId("foo.bar1"));
        assertTrue(ValidationEventId.isValidValidationEventId("_foo.bar"));
        assertTrue(ValidationEventId.isValidValidationEventId("f.b"));
        assertTrue(ValidationEventId.isValidValidationEventId("f.b1.c_d"));
        assertTrue(ValidationEventId.isValidValidationEventId("f.b1.c_d_.e"));
        assertTrue(ValidationEventId.isValidValidationEventId("f.b1.c_d_.e"));
        assertTrue(ValidationEventId.isValidValidationEventId("f.b1.c_d_1234.e"));
        assertTrue(ValidationEventId.isValidValidationEventId("____f"));
    }

    @Test
    public void test_validateValidationEventId_idValid() {
        ValidationEventId.validateValidationEventId(
                "valid.id",
                TEST_OBJECT_NODE);
    }

    @Test
    public void test_validateValidationEventId_idInvalid_ValidatorLoadException() {
        assertThrows(ValidatorLoadException.class, () -> {
            ValidationEventId.validateValidationEventId(
                    "invalid.id.",
                    TEST_OBJECT_NODE);
        });
    }

    @Test
    public void test_validateValidationEventId_idNull_ValidatorLoadException() {
        assertThrows(ValidatorLoadException.class, () -> {
            ValidationEventId.validateValidationEventId(
                    null,
                    TEST_OBJECT_NODE);
        });
    }

    @Test
    public void test_validateValidationEventId_idValid_nodeNull() {
        ValidationEventId.validateValidationEventId(
                "valid.id",
                null);
    }

    @Test
    public void test_validateValidationEventId_idInvalid_nodeNull_NullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            ValidationEventId.validateValidationEventId(
                    "invalid.id.",
                    null);
        });
    }

    @Test
    public void test_validateValidationEventId_idNull_nodeNull_NullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            ValidationEventId.validateValidationEventId(
                    null,
                    null);
        });
    }

}
