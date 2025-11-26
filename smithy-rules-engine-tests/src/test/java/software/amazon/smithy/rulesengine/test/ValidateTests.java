/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.test;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.rulesengine.validators.EndpointTestsTraitValidator;

/**
 * Validates that we can load the models in the JAR. The {@link EndpointTestsTraitValidator}
 * will validate that the tests are correct.
 */
public class ValidateTests {

    @Test
    public void validateTests() {
        Model.assembler()
                .discoverModels(ValidateTests.class.getClassLoader())
                .assemble()
                .unwrap();
    }
}
