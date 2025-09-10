/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Coalesce;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;

public class CoalesceTest {

    @Test
    void testCoalesceWithTwoSameTypes() {
        Expression left = Literal.of("default");
        Expression right = Literal.of("fallback");
        Coalesce coalesce = Coalesce.ofExpressions(left, right);

        Scope<Type> scope = new Scope<>();
        Type resultType = coalesce.typeCheck(scope);

        assertEquals(Type.stringType(), resultType);
    }

    @Test
    void testCoalesceWithThreeSameTypes() {
        Expression first = Literal.of("first");
        Expression second = Literal.of("second");
        Expression third = Literal.of("third");
        Coalesce coalesce = Coalesce.ofExpressions(first, second, third);

        Scope<Type> scope = new Scope<>();
        Type resultType = coalesce.typeCheck(scope);

        assertEquals(Type.stringType(), resultType);
    }

    @Test
    void testCoalesceVariadicWithList() {
        Expression first = Literal.of(1);
        Expression second = Literal.of(2);
        Expression third = Literal.of(3);
        Expression fourth = Literal.of(4);

        Coalesce coalesce = Coalesce.ofExpressions(Arrays.asList(first, second, third, fourth));

        Scope<Type> scope = new Scope<>();
        Type resultType = coalesce.typeCheck(scope);

        assertEquals(Type.integerType(), resultType);
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
    void testCoalesceWithAllOptional() {
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
    void testCoalesceThreeWithAllOptional() {
        Expression var1 = Expression.getReference(Identifier.of("maybe1"));
        Expression var2 = Expression.getReference(Identifier.of("maybe2"));
        Expression var3 = Expression.getReference(Identifier.of("maybe3"));
        Coalesce coalesce = Coalesce.ofExpressions(var1, var2, var3);

        Scope<Type> scope = new Scope<>();
        scope.insert("maybe1", Type.optionalType(Type.integerType()));
        scope.insert("maybe2", Type.optionalType(Type.integerType()));
        scope.insert("maybe3", Type.optionalType(Type.integerType()));

        Type resultType = coalesce.typeCheck(scope);

        // All optional means result is optional
        assertEquals(Type.optionalType(Type.integerType()), resultType);
    }

    @Test
    void testCoalesceMixedOptionalAndNonOptional() {
        Expression optional1 = Expression.getReference(Identifier.of("optional1"));
        Expression required = Expression.getReference(Identifier.of("required"));
        Expression optional2 = Expression.getReference(Identifier.of("optional2"));

        Coalesce coalesce = Coalesce.ofExpressions(optional1, required, optional2);

        Scope<Type> scope = new Scope<>();
        scope.insert("optional1", Type.optionalType(Type.stringType()));
        scope.insert("required", Type.stringType());
        scope.insert("optional2", Type.optionalType(Type.stringType()));

        Type resultType = coalesce.typeCheck(scope);

        // Any non-optional in the chain makes result non-optional
        assertEquals(Type.stringType(), resultType);
    }

    @Test
    void testCoalesceWithIncompatibleTypes() {
        Expression stringExpr = Literal.of("text");
        Expression intExpr = Literal.of(42);
        Coalesce coalesce = Coalesce.ofExpressions(stringExpr, intExpr);

        Scope<Type> scope = new Scope<>();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> coalesce.typeCheck(scope));
        assertTrue(ex.getMessage().contains("Type mismatch in coalesce"));
        assertTrue(ex.getMessage().contains("argument 2"));
    }

    @Test
    void testCoalesceWithIncompatibleTypesInMiddle() {
        Expression int1 = Literal.of(1);
        Expression int2 = Literal.of(2);
        Expression string = Literal.of("oops");
        Expression int3 = Literal.of(3);

        Coalesce coalesce = Coalesce.ofExpressions(int1, int2, string, int3);

        Scope<Type> scope = new Scope<>();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> coalesce.typeCheck(scope));
        assertTrue(ex.getMessage().contains("Type mismatch in coalesce"));
        assertTrue(ex.getMessage().contains("argument 3"));
    }

    @Test
    void testCoalesceWithLessThanTwoArguments() {
        Expression single = Literal.of("only");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Coalesce.ofExpressions(single).typeCheck(new Scope<>()));
        assertTrue(ex.getMessage().contains("at least 2 arguments"));
    }

    @Test
    void testCoalesceArrayTypes() {
        Expression arr1 = Expression.getReference(Identifier.of("array1"));
        Expression arr2 = Expression.getReference(Identifier.of("array2"));
        Expression arr3 = Expression.getReference(Identifier.of("array3"));
        Coalesce coalesce = Coalesce.ofExpressions(arr1, arr2, arr3);

        Scope<Type> scope = new Scope<>();
        scope.insert("array1", Type.arrayType(Type.stringType()));
        scope.insert("array2", Type.arrayType(Type.stringType()));
        scope.insert("array3", Type.arrayType(Type.stringType()));

        Type resultType = coalesce.typeCheck(scope);

        assertEquals(Type.arrayType(Type.stringType()), resultType);
    }

    @Test
    void testCoalesceOptionalArrayTypes() {
        Expression arr1 = Expression.getReference(Identifier.of("array1"));
        Expression arr2 = Expression.getReference(Identifier.of("array2"));
        Coalesce coalesce = Coalesce.ofExpressions(arr1, arr2);

        Scope<Type> scope = new Scope<>();
        scope.insert("array1", Type.optionalType(Type.arrayType(Type.integerType())));
        scope.insert("array2", Type.arrayType(Type.integerType()));

        Type resultType = coalesce.typeCheck(scope);

        // One non-optional makes result non-optional
        assertEquals(Type.arrayType(Type.integerType()), resultType);
    }

    @Test
    void testCoalesceWithBooleanTypes() {
        Expression bool1 = Expression.getReference(Identifier.of("bool1"));
        Expression bool2 = Expression.getReference(Identifier.of("bool2"));
        Expression bool3 = Expression.getReference(Identifier.of("bool3"));

        Coalesce coalesce = Coalesce.ofExpressions(bool1, bool2, bool3);

        Scope<Type> scope = new Scope<>();
        scope.insert("bool1", Type.optionalType(Type.booleanType()));
        scope.insert("bool2", Type.optionalType(Type.booleanType()));
        scope.insert("bool3", Type.booleanType());

        Type resultType = coalesce.typeCheck(scope);

        assertEquals(Type.booleanType(), resultType);
    }
}
