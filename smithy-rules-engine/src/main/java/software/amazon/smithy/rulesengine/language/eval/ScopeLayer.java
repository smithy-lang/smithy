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

package software.amazon.smithy.rulesengine.language.eval;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
// TODO The internal containers of this class are mutated.
final class ScopeLayer<T> {
    private final Map<Identifier, T> types;
    private final Set<Reference> nonNullReferences;

    ScopeLayer(Map<Identifier, T> types, Set<Reference> nonNullReferences) {
        this.types = types;
        this.nonNullReferences = nonNullReferences;
    }

    ScopeLayer() {
        this(new HashMap<>(), new HashSet<>());
    }

    public Map<Identifier, T> getTypes() {
        return types;
    }

    public Set<Reference> getNonNullReferences() {
        return nonNullReferences;
    }

    @Override
    public int hashCode() {
        return Objects.hash(types, nonNullReferences);
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
    public String toString() {
        return "ScopeLayer["
               + "types=" + types + ", "
               + "facts=" + nonNullReferences + ']';
    }

}
