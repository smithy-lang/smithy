/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SyntheticShapeNamingTest {

    @Test
    public void listNameWithSimpleTarget() {
        assertEquals("_SyntheticListOfString", SyntheticShapeNaming.listName("String"));
    }

    @Test
    public void listNameWithQualifiedTarget() {
        assertEquals("_SyntheticListOfBar", SyntheticShapeNaming.listName("com.foo#Bar"));
    }

    @Test
    public void listNameWithSyntheticTarget() {
        assertEquals("_SyntheticListOf_SyntheticListOfString",
                SyntheticShapeNaming.listName("_SyntheticListOfString"));
    }

    @Test
    public void mapNameWithSimpleTargets() {
        assertEquals("_SyntheticMapOfStringToInteger",
                SyntheticShapeNaming.mapName("String", "Integer"));
    }

    @Test
    public void mapNameWithQualifiedTargets() {
        assertEquals("_SyntheticMapOfKeyToValue",
                SyntheticShapeNaming.mapName("com.foo#Key", "com.bar#Value"));
    }

    @Test
    public void mapNameWithMixedTargets() {
        assertEquals("_SyntheticMapOfStringToBar",
                SyntheticShapeNaming.mapName("String", "com.foo#Bar"));
    }
}
