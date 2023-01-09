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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public interface Type {
    static String string() {
        return new String();
    }

    static Endpoint endpoint() {
        return new Endpoint();
    }

    static Type empty() {
        return new Type.Empty();
    }

    static Array array(Type inner) {
        return new Type.Array(inner);
    }

    static Record record(Map<Identifier, Type> inner) {
        return new Record(inner);
    }

    static Type integer() {
        return new Integer();
    }

    static Option optional(Type t) {
        return new Option(t);
    }

    static Bool bool() {
        return new Bool();
    }

    default String expectString() throws InnerParseError {
        throw new InnerParseError("Expected string but found " + this);
    }

    default Record expectObject(java.lang.String message) throws InnerParseError {
        throw new InnerParseError(java.lang.String.format("Expected record but found %s%n == hint: %s", this, message));
    }

    default Bool expectBool() throws InnerParseError {
        throw new InnerParseError("Expected boolean but found " + this);
    }

    default Integer expectInt() throws InnerParseError {
        throw new InnerParseError("Expected int but found " + this);
    }

    default Option expectOptional() throws InnerParseError {
        throw new InnerParseError("Expected optional but found " + this);
    }

    default Array expectArray() throws InnerParseError {
        throw new InnerParseError("Expected array but found " + this);
    }

    default boolean isA(Type t) {
        if (t.equals(new Type.Any())) {
            return true;
        }
        return t.equals(this);
    }

    /**
     * When used in the context of a condition, the condition can only match if the value was truthful. This means
     * that a certain expression can be a different type, for example, {@code Option<T>} will become {@code T}.
     *
     * @return The type, given that it has been proven truthy
     */
    default Type provenTruthy() {
        return this;
    }

    final class Integer implements Type {
        @Override
        public int hashCode() {
            return 2;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj != null && obj.getClass() == this.getClass();
        }

        @Override
        public java.lang.String toString() {
            return "Int";
        }

        @Override
        public Integer expectInt() {
            return this;
        }
    }

    final class Any implements Type {
        public Any() {
        }

        @Override
        public boolean isA(Type t) {
            return true;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj != null && obj.getClass() == this.getClass();
        }

        @Override
        public java.lang.String toString() {
            return "Any[]";
        }

    }

    final class Empty implements Type {
        public Empty() {
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj != null && obj.getClass() == this.getClass();
        }

        @Override
        public java.lang.String toString() {
            return "Empty[]";
        }

    }

    final class Endpoint implements Type {
        public Endpoint() {
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj != null && obj.getClass() == this.getClass();
        }

        @Override
        public java.lang.String toString() {
            return "Endpoint[]";
        }

    }

    final class String implements Type {
        public String() {
        }

        @Override
        public String expectString() {
            return this;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj != null && obj.getClass() == this.getClass();
        }

        @Override
        public java.lang.String toString() {
            return "String";
        }

    }

    final class Bool implements Type {
        public Bool() {
        }

        public Bool expectBool() {
            return this;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj != null && obj.getClass() == this.getClass();
        }

        @Override
        public java.lang.String toString() {
            return "Bool";
        }

    }

    final class Option implements Type {
        private final Type inner;

        public Option(Type inner) {
            this.inner = inner;
        }

        @Override
        public String expectString() throws InnerParseError {
            throw new InnerParseError(java.lang.String
                    .format("Expected string but found %s. hint: use `assign` in a condition "
                            + "or `isSet` to prove that this value is non-null", this));
        }

        @Override
        public Bool expectBool() throws InnerParseError {
            throw new InnerParseError(java.lang.String
                    .format("Expected boolean but found %s. hint: use `isSet` to convert "
                            + "Option<Bool> to bool", this));
        }

        @Override
        public Option expectOptional() throws InnerParseError {
            return this;
        }

        @Override
        public boolean isA(Type t) {
            if (!(t instanceof Option)) {
                return false;
            }
            return ((Option) t).inner.isA(inner);
        }

        @Override
        public Type provenTruthy() {
            return inner;
        }

        public Type inner() {
            return inner;
        }

        @Override
        public int hashCode() {
            return Objects.hash(inner);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            Option that = (Option) obj;
            return Objects.equals(this.inner, that.inner);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("Option<%s>", inner);
        }

    }

    final class Tuple implements Type {
        private final List<Type> members;

        public Tuple(List<Type> members) {
            this.members = members;
        }

        public List<Type> members() {
            return members;
        }

        @Override
        public int hashCode() {
            return Objects.hash(members);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            Tuple that = (Tuple) obj;
            return Objects.equals(this.members, that.members);
        }

        @Override
        public java.lang.String toString() {
            return this.members.toString();
        }

    }

    final class Array implements Type {
        private final Type member;

        public Array(Type member) {
            this.member = member;
        }

        public Type getMember() {
            return member;
        }

        @Override
        public Array expectArray() throws InnerParseError {
            return this;
        }

        public Type member() {
            return member;
        }

        @Override
        public int hashCode() {
            return Objects.hash(member);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            Array that = (Array) obj;
            return Objects.equals(this.member, that.member);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("[%s]", this.member);
        }

    }

    final class Record implements Type {
        private final Map<Identifier, Type> shape;

        public Record(Map<Identifier, Type> shape) {
            this.shape = new LinkedHashMap<>(shape);
        }

        @Override
        public Record expectObject(java.lang.String message) {
            return this;
        }

        public Optional<Type> get(Identifier name) {
            if (shape.containsKey(name)) {
                return Optional.of(shape.get(name));
            } else {
                return Optional.empty();
            }
        }

        public Map<Identifier, Type> shape() {
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
            Record that = (Record) obj;
            return Objects.equals(this.shape, that.shape);
        }

        @Override
        public java.lang.String toString() {
            return shape.toString();
        }

    }
}
