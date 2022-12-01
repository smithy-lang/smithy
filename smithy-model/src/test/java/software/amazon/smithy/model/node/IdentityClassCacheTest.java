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

package software.amazon.smithy.model.node;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

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
        cache.getForClass("key", associatedClass, () -> storedInstance);

        Type newAssociatedClass = new Type() {};
        String newStoredInstance = "newValue";
        assertEquals(newStoredInstance, cache.getForClass(key, newAssociatedClass, () -> newStoredInstance));

    }
}
