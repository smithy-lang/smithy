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
        assertEquals("_SyntheticListOfString", LoaderUtils.listName("String"));
    }

    @Test
    public void listNameWithQualifiedTarget() {
        assertEquals("_SyntheticListOfBar", LoaderUtils.listName("com.foo#Bar"));
    }

    @Test
    public void listNameWithSyntheticTarget() {
        assertEquals("_SyntheticListOf_SyntheticListOfString",
                LoaderUtils.listName("_SyntheticListOfString"));
    }

    @Test
    public void mapNameWithSimpleTargets() {
        assertEquals("_SyntheticMapOfStringToInteger",
                LoaderUtils.mapName("String", "Integer"));
    }

    @Test
    public void mapNameWithQualifiedTargets() {
        assertEquals("_SyntheticMapOfKeyToValue",
                LoaderUtils.mapName("com.foo#Key", "com.bar#Value"));
    }

    @Test
    public void mapNameWithMixedTargets() {
        assertEquals("_SyntheticMapOfStringToBar",
                LoaderUtils.mapName("String", "com.foo#Bar"));
    }
}
