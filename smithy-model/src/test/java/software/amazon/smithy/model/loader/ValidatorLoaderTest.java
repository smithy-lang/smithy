/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ValidatorLoaderTest {
    @Test
    public void loadsAppropriateSourceLocations() {
        List<ValidationEvent> events = Model.assembler()
                .addImport(getClass().getResource("invalid-validation-selector.json"))
                .assemble()
                .getValidationEvents();

        assertThat(events, not(empty()));
        Assertions.assertTrue(events.stream().anyMatch(e -> e.getMessage().contains("Syntax error")));
    }
}
