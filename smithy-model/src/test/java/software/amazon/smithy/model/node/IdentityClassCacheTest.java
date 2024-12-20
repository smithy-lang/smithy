/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Type;
import org.junit.jupiter.api.Test;

public class IdentityClassCacheTest {

    @Test
    public void doesNotInvalidateCacheForSameClassInstance() {
        IdentityClassCache<String, String> cache = new IdentityClassCache<>();

        String key = "key";
        Type associatedClass = new Type() {};
        String storedInstance = "value";

        cache.getForClass(key, associatedClass, () -> storedInstance);
        assertEquals(storedInstance, cache.getForClass(key, associatedClass, () -> "newValue"));

    }

    @Test
    public void invalidatesCacheForDifferentClassInstance() {
        IdentityClassCache<String, String> cache = new IdentityClassCache<>();

        String key = "key";
        Type associatedClass = new Type() {};
        String storedInstance = "value";
        cache.getForClass(key, associatedClass, () -> storedInstance);

        Type newAssociatedClass = new Type() {};
        String newStoredInstance = "newValue";
        assertEquals(newStoredInstance, cache.getForClass(key, newAssociatedClass, () -> newStoredInstance));

    }
}
