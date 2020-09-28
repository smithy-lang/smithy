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

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.schema.CfnException;
import software.amazon.smithy.jsonschema.JsonSchemaConfig;

public class CfnConfigTest {
    @Test
    public void throwsOnDifferentAlphanumericRefs() {
        CfnConfig config = new CfnConfig();

        assertTrue(config.getAlphanumericOnlyRefs());
        assertThrows(CfnException.class, () -> config.setAlphanumericOnlyRefs(false));
    }

    @Test
    public void throwsOnJsonName() {
        CfnConfig config = new CfnConfig();

        assertThrows(CfnException.class, () -> config.setUseJsonName(true));
        assertThrows(CfnException.class, () -> config.setUseJsonName(false));
    }

    @Test
    public void throwsOnDifferentMapStrategy() {
        CfnConfig config = new CfnConfig();

        assertEquals(config.getMapStrategy(), JsonSchemaConfig.MapStrategy.PATTERN_PROPERTIES);
        assertThrows(CfnException.class, () -> config.setMapStrategy(JsonSchemaConfig.MapStrategy.PROPERTY_NAMES));
    }

    @Test
    public void throwsOnDifferentUnionStrategy() {
        CfnConfig config = new CfnConfig();

        assertEquals(config.getUnionStrategy(), JsonSchemaConfig.UnionStrategy.ONE_OF);
        assertThrows(CfnException.class, () -> config.setUnionStrategy(JsonSchemaConfig.UnionStrategy.OBJECT));
        assertThrows(CfnException.class, () -> config.setUnionStrategy(JsonSchemaConfig.UnionStrategy.STRUCTURE));
    }
}
