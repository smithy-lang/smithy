/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Ite;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;

public class IteTest {

    @Test
    void testIteBothBranchesNonOptionalString() {
        Expression condition = Expression.getReference(Identifier.of("flag"));
        Expression trueValue = Literal.of("-fips");
        Expression falseValue = Literal.of("");
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("flag", Type.booleanType());

        Type resultType = ite.typeCheck(scope);

        // Both non-optional String => non-optional String
        assertEquals(Type.stringType(), resultType);
    }

    @Test
    void testIteBothBranchesNonOptionalInteger() {
        Expression condition = Expression.getReference(Identifier.of("useNewValue"));
        Expression trueValue = Literal.of(100);
        Expression falseValue = Literal.of(0);
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("useNewValue", Type.booleanType());

        Type resultType = ite.typeCheck(scope);

        assertEquals(Type.integerType(), resultType);
    }

    @Test
    void testIteTrueBranchOptional() {
        Expression condition = Expression.getReference(Identifier.of("flag"));
        Expression trueValue = Expression.getReference(Identifier.of("maybeValue"));
        Expression falseValue = Literal.of("default");
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("flag", Type.booleanType());
        scope.insert("maybeValue", Type.optionalType(Type.stringType()));

        Type resultType = ite.typeCheck(scope);

        // True branch optional => result is optional
        assertEquals(Type.optionalType(Type.stringType()), resultType);
    }

    @Test
    void testIteFalseBranchOptional() {
        Expression condition = Expression.getReference(Identifier.of("flag"));
        Expression trueValue = Literal.of("value");
        Expression falseValue = Expression.getReference(Identifier.of("maybeDefault"));
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("flag", Type.booleanType());
        scope.insert("maybeDefault", Type.optionalType(Type.stringType()));

        Type resultType = ite.typeCheck(scope);

        // False branch optional => result is optional
        assertEquals(Type.optionalType(Type.stringType()), resultType);
    }

    @Test
    void testIteBothBranchesOptional() {
        Expression condition = Expression.getReference(Identifier.of("flag"));
        Expression trueValue = Expression.getReference(Identifier.of("maybe1"));
        Expression falseValue = Expression.getReference(Identifier.of("maybe2"));
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("flag", Type.booleanType());
        scope.insert("maybe1", Type.optionalType(Type.stringType()));
        scope.insert("maybe2", Type.optionalType(Type.stringType()));

        Type resultType = ite.typeCheck(scope);

        // Both optional => result is optional
        assertEquals(Type.optionalType(Type.stringType()), resultType);
    }

    @Test
    void testIteWithOfStringsHelper() {
        Expression condition = Expression.getReference(Identifier.of("UseFIPS"));
        Ite ite = Ite.ofStrings(condition, "-fips", "");

        Scope<Type> scope = new Scope<>();
        scope.insert("UseFIPS", Type.booleanType());

        Type resultType = ite.typeCheck(scope);

        // Both literal strings => non-optional String
        assertEquals(Type.stringType(), resultType);
    }

    @Test
    void testIteTypeMismatchBetweenBranches() {
        Expression condition = Expression.getReference(Identifier.of("flag"));
        Expression trueValue = Literal.of("string");
        Expression falseValue = Literal.of(42);
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("flag", Type.booleanType());

        RuleError ex = assertThrows(RuleError.class, () -> ite.typeCheck(scope));
        assertTrue(ex.getMessage().contains("same base type"));
        assertTrue(ex.getMessage().contains("true branch"));
        assertTrue(ex.getMessage().contains("false branch"));
    }

    @Test
    void testIteConditionMustBeBoolean() {
        Expression condition = Literal.of("not a boolean");
        Expression trueValue = Literal.of("yes");
        Expression falseValue = Literal.of("no");
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();

        RuleError ex = assertThrows(RuleError.class, () -> ite.typeCheck(scope));
        assertTrue(ex.getMessage().contains("non-optional Boolean"));
    }

    @Test
    void testIteConditionCannotBeOptionalBoolean() {
        Expression condition = Expression.getReference(Identifier.of("maybeFlag"));
        Expression trueValue = Literal.of("yes");
        Expression falseValue = Literal.of("no");
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("maybeFlag", Type.optionalType(Type.booleanType()));

        RuleError ex = assertThrows(RuleError.class, () -> ite.typeCheck(scope));
        assertTrue(ex.getMessage().contains("non-optional Boolean"));
        assertTrue(ex.getMessage().contains("coalesce"));
    }

    @Test
    void testIteWithArrayTypes() {
        Expression condition = Expression.getReference(Identifier.of("useFirst"));
        Expression trueValue = Expression.getReference(Identifier.of("array1"));
        Expression falseValue = Expression.getReference(Identifier.of("array2"));
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("useFirst", Type.booleanType());
        scope.insert("array1", Type.arrayType(Type.stringType()));
        scope.insert("array2", Type.arrayType(Type.stringType()));

        Type resultType = ite.typeCheck(scope);

        assertEquals(Type.arrayType(Type.stringType()), resultType);
    }

    @Test
    void testIteWithOptionalArrayType() {
        Expression condition = Expression.getReference(Identifier.of("flag"));
        Expression trueValue = Expression.getReference(Identifier.of("maybeArray"));
        Expression falseValue = Expression.getReference(Identifier.of("definiteArray"));
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("flag", Type.booleanType());
        scope.insert("maybeArray", Type.optionalType(Type.arrayType(Type.integerType())));
        scope.insert("definiteArray", Type.arrayType(Type.integerType()));

        Type resultType = ite.typeCheck(scope);

        // One optional array => result is optional array
        assertEquals(Type.optionalType(Type.arrayType(Type.integerType())), resultType);
    }

    @Test
    void testIteWithBooleanValues() {
        Expression condition = Expression.getReference(Identifier.of("invertFlag"));
        Expression trueValue = Literal.of(false);
        Expression falseValue = Literal.of(true);
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("invertFlag", Type.booleanType());

        Type resultType = ite.typeCheck(scope);

        assertEquals(Type.booleanType(), resultType);
    }

    @Test
    void testIteTypeMismatchWithOptionalUnwrapping() {
        // Even with optional wrapping, base types must match
        Expression condition = Expression.getReference(Identifier.of("flag"));
        Expression trueValue = Expression.getReference(Identifier.of("maybeString"));
        Expression falseValue = Expression.getReference(Identifier.of("maybeInt"));
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("flag", Type.booleanType());
        scope.insert("maybeString", Type.optionalType(Type.stringType()));
        scope.insert("maybeInt", Type.optionalType(Type.integerType()));

        RuleError ex = assertThrows(RuleError.class, () -> ite.typeCheck(scope));
        assertTrue(ex.getMessage().contains("same base type"));
    }

    @Test
    void testIteReturnsCorrectId() {
        assertEquals("ite", Ite.ID);
        assertEquals("ite", Ite.getDefinition().getId());
    }

    @Test
    void testTypeMethodReturnsInferredTypeAfterTypeCheck() {
        // Verify that type() returns the correct inferred type after typeCheck()
        Expression condition = Expression.getReference(Identifier.of("flag"));
        Expression trueValue = Expression.getReference(Identifier.of("maybeValue"));
        Expression falseValue = Literal.of("default");
        Ite ite = Ite.ofExpressions(condition, trueValue, falseValue);

        Scope<Type> scope = new Scope<>();
        scope.insert("flag", Type.booleanType());
        scope.insert("maybeValue", Type.optionalType(Type.stringType()));

        // Call typeCheck to cache the type
        ite.typeCheck(scope);

        // Now type() should return the inferred type
        Type cachedType = ite.type();
        assertEquals(Type.optionalType(Type.stringType()), cachedType);
    }

    @Test
    void testNestedIteTypeInference() {
        // Test that nested Ite expressions have correct type inference
        Expression outerCondition = Expression.getReference(Identifier.of("outer"));
        Expression innerCondition = Expression.getReference(Identifier.of("inner"));

        // Inner ITE: ite(inner, "a", "b") => String
        Ite innerIte = Ite.ofExpressions(innerCondition, Literal.of("a"), Literal.of("b"));

        // Outer ITE: ite(outer, innerIte, "c") => String
        Ite outerIte = Ite.ofExpressions(outerCondition, innerIte, Literal.of("c"));

        Scope<Type> scope = new Scope<>();
        scope.insert("outer", Type.booleanType());
        scope.insert("inner", Type.booleanType());

        outerIte.typeCheck(scope);

        // Both inner and outer should have String type
        assertEquals(Type.stringType(), innerIte.type());
        assertEquals(Type.stringType(), outerIte.type());
    }
}
