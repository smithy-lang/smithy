/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Coalesce;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;

public class CoalesceTest {

    @Test
    void testCoalesceWithSameTypes() {
        Expression left = Literal.of("default");
        Expression right = Literal.of("fallback");
        Coalesce coalesce = Coalesce.ofExpressions(left, right);

        Scope<Type> scope = new Scope<>();
        Type resultType = coalesce.typeCheck(scope);

        assertEquals(Type.stringType(), resultType);
    }

    @Test
    void testCoalesceWithOptionalLeft() {
        Expression optionalVar = Expression.getReference(Identifier.of("maybeValue"));
        Expression fallback = Literal.of("default");
        Coalesce coalesce = Coalesce.ofExpressions(optionalVar, fallback);

        Scope<Type> scope = new Scope<>();
        scope.insert("maybeValue", Type.optionalType(Type.stringType()));

        Type resultType = coalesce.typeCheck(scope);

        // Should unwrap optional and return non-optional String
        assertEquals(Type.stringType(), resultType);
    }

    @Test
    void testCoalesceWithBothOptional() {
        Expression var1 = Expression.getReference(Identifier.of("maybe1"));
        Expression var2 = Expression.getReference(Identifier.of("maybe2"));
        Coalesce coalesce = Coalesce.ofExpressions(var1, var2);

        Scope<Type> scope = new Scope<>();
        scope.insert("maybe1", Type.optionalType(Type.stringType()));
        scope.insert("maybe2", Type.optionalType(Type.stringType()));

        Type resultType = coalesce.typeCheck(scope);

        // Both optional means result is optional
        assertEquals(Type.optionalType(Type.stringType()), resultType);
    }

    @Test
    void testCoalesceWithCompatibleTypes() {
        // Test with optional types that should resolve to non-optional
        Expression optionalString = Expression.getReference(Identifier.of("optional"));
        Expression requiredString = Expression.getReference(Identifier.of("required"));
        Coalesce coalesce = Coalesce.ofExpressions(optionalString, requiredString);

        Scope<Type> scope = new Scope<>();
        scope.insert("optional", Type.optionalType(Type.stringType()));
        scope.insert("required", Type.stringType());

        Type resultType = coalesce.typeCheck(scope);

        // When coalescing Optional<String> with String, should return String
        assertEquals(Type.stringType(), resultType);
    }

    @Test
    void testCoalesceWithIncompatibleTypes() {
        Expression stringExpr = Literal.of("text");
        Expression intExpr = Literal.of(42);
        Coalesce coalesce = Coalesce.ofExpressions(stringExpr, intExpr);

        Scope<Type> scope = new Scope<>();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> coalesce.typeCheck(scope));
        assertTrue(ex.getMessage().contains("Type mismatch in coalesce"));
    }

    @Test
    void testCoalesceNonOptionalWithNonOptional() {
        Expression int1 = Literal.of(42);
        Expression int2 = Literal.of(100);
        Coalesce coalesce = Coalesce.ofExpressions(int1, int2);

        Scope<Type> scope = new Scope<>();
        Type resultType = coalesce.typeCheck(scope);

        // Two non-optionals of same type should return that type
        assertEquals(Type.integerType(), resultType);
    }

    @Test
    void testCoalesceOptionalWithNonOptional() {
        Expression optionalInt = Expression.getReference(Identifier.of("maybeInt"));
        Expression defaultInt = Literal.of(0);
        Coalesce coalesce = Coalesce.ofExpressions(optionalInt, defaultInt);

        Scope<Type> scope = new Scope<>();
        scope.insert("maybeInt", Type.optionalType(Type.integerType()));

        Type resultType = coalesce.typeCheck(scope);

        // Optional<Int> coalesced with Int should return Int
        assertEquals(Type.integerType(), resultType);
    }

    @Test
    void testCoalesceArrayTypes() {
        Expression arr1 = Expression.getReference(Identifier.of("array1"));
        Expression arr2 = Expression.getReference(Identifier.of("array2"));
        Coalesce coalesce = Coalesce.ofExpressions(arr1, arr2);

        Scope<Type> scope = new Scope<>();
        scope.insert("array1", Type.arrayType(Type.stringType()));
        scope.insert("array2", Type.arrayType(Type.stringType()));

        Type resultType = coalesce.typeCheck(scope);

        assertEquals(Type.arrayType(Type.stringType()), resultType);
    }
}
