/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.RecordLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.TupleLiteral;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

class ReferenceRewriterTest {

    @Test
    void testSimpleReferenceReplacement() {
        // Create a rewriter that replaces "x" with "y"
        Map<String, Expression> replacements = new HashMap<>();
        replacements.put("x", Expression.getReference(Identifier.of("y")));

        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);

        // Test rewriting a simple reference
        Reference original = Expression.getReference(Identifier.of("x"));
        Expression rewritten = rewriter.rewrite(original);

        assertEquals("y", ((Reference) rewritten).getName().toString());
    }

    @Test
    void testNoRewriteNeeded() {
        // Create a rewriter with no relevant replacements
        Map<String, Expression> replacements = new HashMap<>();
        replacements.put("x", Expression.getReference(Identifier.of("y")));

        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);

        // Reference to "z" should not be rewritten
        Reference original = Expression.getReference(Identifier.of("z"));
        Expression rewritten = rewriter.rewrite(original);

        assertEquals(original, rewritten);
    }

    @Test
    void testRewriteInStringLiteral() {
        // Create a string literal with template variable
        Template template = Template.fromString("Value is {x}");
        Literal original = Literal.stringLiteral(template);

        Map<String, Expression> replacements = new HashMap<>();
        replacements.put("x", Expression.getReference(Identifier.of("newVar")));

        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);
        Expression rewritten = rewriter.rewrite(original);

        assertInstanceOf(StringLiteral.class, rewritten);
        StringLiteral rewrittenStr = (StringLiteral) rewritten;
        assertTrue(rewrittenStr.toString().contains("newVar"));
        assertNotEquals(original, rewritten);
    }

    @Test
    void testRewriteInTupleLiteral() {
        // Create a tuple with references
        Literal original = Literal.tupleLiteral(ListUtils.of(Literal.of("constant"), Literal.of("{x}")));

        Map<String, Expression> replacements = new HashMap<>();
        replacements.put("x", Expression.getReference(Identifier.of("replaced")));

        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);
        Expression rewritten = rewriter.rewrite(original);

        assertInstanceOf(TupleLiteral.class, rewritten);
        TupleLiteral rewrittenTuple = (TupleLiteral) rewritten;
        assertEquals(2, rewrittenTuple.members().size());
        assertTrue(rewrittenTuple.members().get(1).toString().contains("replaced"));
    }

    @Test
    void testRewriteInRecordLiteral() {
        // Create a record with references
        Literal original = Literal.recordLiteral(MapUtils.of(
                Identifier.of("field1"),
                Literal.of("value1"),
                Identifier.of("field2"),
                Literal.of("{x}")));

        Map<String, Expression> replacements = new HashMap<>();
        replacements.put("x", Expression.getReference(Identifier.of("newX")));

        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);
        Expression rewritten = rewriter.rewrite(original);

        assertInstanceOf(RecordLiteral.class, rewritten);
        RecordLiteral rewrittenRecord = (RecordLiteral) rewritten;
        assertEquals(2, rewrittenRecord.members().size());
        assertTrue(rewrittenRecord.members().get(Identifier.of("field2")).toString().contains("newX"));
    }

    @Test
    void testRewriteInLibraryFunction() {
        // Create a function that uses references
        Expression original = StringEquals.ofExpressions(
                Expression.getReference(Identifier.of("x")),
                Literal.of("test"));

        Map<String, Expression> replacements = new HashMap<>();
        replacements.put("x", Expression.getReference(Identifier.of("replacedVar")));

        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);
        Expression rewritten = rewriter.rewrite(original);

        assertTrue(rewritten.toString().contains("replacedVar"));
        assertNotEquals(original, rewritten);
    }

    @Test
    void testMultipleReplacements() {
        // Create a function with multiple references
        Expression original = BooleanEquals.ofExpressions(
                Expression.getReference(Identifier.of("a")),
                Expression.getReference(Identifier.of("b")));

        Map<String, Expression> replacements = new HashMap<>();
        replacements.put("a", Expression.getReference(Identifier.of("x")));
        replacements.put("b", Expression.getReference(Identifier.of("y")));

        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);
        Expression rewritten = rewriter.rewrite(original);

        assertTrue(rewritten.toString().contains("x"));
        assertTrue(rewritten.toString().contains("y"));
    }

    @Test
    void testNestedRewriting() {
        // Create nested functions with references
        Expression inner = IsSet.ofExpressions(Expression.getReference(Identifier.of("x")));
        Expression original = BooleanEquals.ofExpressions(inner, Literal.of(true));

        Map<String, Expression> replacements = new HashMap<>();
        replacements.put("x", Expression.getReference(Identifier.of("newVar")));

        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);
        Expression rewritten = rewriter.rewrite(original);

        assertTrue(rewritten.toString().contains("newVar"));
        assertNotEquals(original, rewritten);
    }

    @Test
    void testStaticStringNotRewritten() {
        // Static strings without templates should not be rewritten
        Literal original = Literal.of("static string");

        Map<String, Expression> replacements = new HashMap<>();
        replacements.put("x", Expression.getReference(Identifier.of("y")));

        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);
        Expression rewritten = rewriter.rewrite(original);

        assertEquals(original, rewritten);
    }
}
