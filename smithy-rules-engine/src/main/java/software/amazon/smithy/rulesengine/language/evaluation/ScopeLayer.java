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

package software.amazon.smithy.rulesengine.language.evaluation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
final class ScopeLayer<T> {
    private final Map<Identifier, T> types = new HashMap<>();
    private final Set<Reference> nonNullReferences = new HashSet<>();

    ScopeLayer() {}

    public boolean containsType(Identifier identifier) {
        return types.containsKey(identifier);
    }

    public Map.Entry<Identifier, T> getTypeEntry(Identifier identifier) {
        // This needs to return the original entry, as the specific instance
        // of Identifier in the key is required for its source location.
        for (Map.Entry<Identifier, T> entry : types.entrySet()) {
            if (entry.getKey().equals(identifier)) {
                return entry;
            }
        }
        return null;
    }

    public T getType(Identifier identifier) {
        return types.get(identifier);
    }

    public T putType(Identifier identifier, T type) {
        return types.put(identifier, type);
    }

    public boolean containsNonNullReference(Reference reference) {
        return nonNullReferences.contains(reference);
    }

    public boolean addNonNullReference(Reference reference) {
        return nonNullReferences.add(reference);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScopeLayer<?> scopeLayer = (ScopeLayer<?>) o;
        return types.equals(scopeLayer.types) && nonNullReferences.equals(scopeLayer.nonNullReferences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types, nonNullReferences);
    }

    @Override
    public String toString() {
        return "ScopeLayer["
               + "types=" + types + ", "
               + "facts=" + nonNullReferences + ']';
    }
}
