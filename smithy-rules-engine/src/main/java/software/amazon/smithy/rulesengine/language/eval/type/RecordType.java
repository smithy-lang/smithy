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

package software.amazon.smithy.rulesengine.language.eval.type;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;

public final class RecordType extends AbstractType {
    private final Map<Identifier, Type> shape;

    public RecordType(Map<Identifier, Type> shape) {
        this.shape = new LinkedHashMap<>(shape);
    }

    @Override
    public RecordType expectRecordType(String message) {
        return this;
    }

    public Optional<Type> get(Identifier name) {
        if (shape.containsKey(name)) {
            return Optional.of(shape.get(name));
        } else {
            return Optional.empty();
        }
    }

    public Map<Identifier, Type> getShape() {
        return shape;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shape);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        RecordType that = (RecordType) obj;
        return Objects.equals(this.shape, that.shape);
    }

    @Override
    public String toString() {
        return shape.toString();
    }
}
